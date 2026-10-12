"""Procedural sound effects for Fallen Roses 0.9 (numpy + ffmpeg/libvorbis).

Every sound is synthesised from a fixed seed, so a rerun gives the same files.
Usage: python3 tools/sfx.py res/assets/fallenroses/sounds [name ...]
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 44100


# ---------------------------------------------------------------- primitives

def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def rng(seed):
    return np.random.default_rng(seed)


def noise(dur, r):
    return r.standard_normal(int(dur * SR))


def env_adsr(n, a, d, s, rel, hold=None):
    """Attack/decay/sustain/release envelope over n samples (times in seconds)."""
    a, d, rel = int(a * SR), int(d * SR), int(rel * SR)
    hold = n - a - d - rel if hold is None else int(hold * SR)
    hold = max(0, hold)
    e = np.concatenate([
        np.linspace(0, 1, max(1, a), endpoint=False),
        np.linspace(1, s, max(1, d), endpoint=False),
        np.full(hold, s),
        np.linspace(s, 0, max(1, rel)),
    ])
    if len(e) < n:
        e = np.concatenate([e, np.zeros(n - len(e))])
    return e[:n]


def env_exp(n, attack, decay):
    """Fast attack then exponential decay (decay = time constant in seconds)."""
    t = np.arange(n) / SR
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    return a * np.exp(-np.maximum(0, t - attack) / decay)


def fade_out(x, dur):
    k = min(len(x), int(dur * SR))
    x = x.copy()
    x[-k:] *= np.linspace(1, 0, k) ** 2
    return x


def osc(freq, dur, shape="sine", phase=0.0):
    """freq may be a scalar or an array (per-sample frequency)."""
    n = int(dur * SR)
    f = np.full(n, float(freq)) if np.isscalar(freq) else np.asarray(freq)[:n]
    ph = 2 * np.pi * np.cumsum(f) / SR + phase
    if shape == "sine":
        return np.sin(ph)
    if shape == "saw":
        return 2 * ((ph / (2 * np.pi)) % 1.0) - 1
    if shape == "square":
        return np.sign(np.sin(ph))
    if shape == "tri":
        return 2 * np.abs(2 * ((ph / (2 * np.pi)) % 1.0) - 1) - 1
    raise ValueError(shape)


def sweep(f0, f1, dur, curve=1.0):
    t = np.linspace(0, 1, int(dur * SR)) ** curve
    return f0 * (f1 / f0) ** t


def band(x, lo, hi, soft=0.25):
    """Zero-phase band-pass with smooth (log-domain) skirts (zero-padded: no wrap-around)."""
    n = len(x)
    size = 1 << (2 * n - 1).bit_length()
    X = np.fft.rfft(x, size)
    f = np.fft.rfftfreq(size, 1 / SR) + 1e-3
    lf = np.log2(f)
    g = np.ones_like(f)
    if lo > 0:
        g *= 1 / (1 + np.exp(-(lf - np.log2(lo)) / soft))
    if hi < SR / 2:
        g *= 1 / (1 + np.exp((lf - np.log2(hi)) / soft))
    return np.fft.irfft(X * g, size)[:n]


def lowpass(x, hi, soft=0.25):
    return band(x, 0, hi, soft)


def highpass(x, lo, soft=0.25):
    return band(x, lo, SR / 2, soft)


def tv_band(x, lo_fn, hi_fn, frame=2048, hop=512, soft=0.3):
    """Time-varying band-pass (STFT); lo_fn/hi_fn map time 0..1 to Hz."""
    n = len(x)
    win = np.hanning(frame)
    pad = np.concatenate([np.zeros(frame), x, np.zeros(frame)])
    out = np.zeros_like(pad)
    norm = np.zeros_like(pad)
    f = np.fft.rfftfreq(frame, 1 / SR) + 1e-3
    lf = np.log2(f)
    for start in range(0, len(pad) - frame, hop):
        u = np.clip((start - frame) / max(1, n), 0, 1)
        lo, hi = lo_fn(u), hi_fn(u)
        g = 1 / (1 + np.exp(-(lf - np.log2(max(lo, 1))) / soft)) * 1 / (1 + np.exp((lf - np.log2(hi)) / soft))
        seg = np.fft.irfft(np.fft.rfft(pad[start:start + frame] * win) * g, frame)
        out[start:start + frame] += seg * win
        norm[start:start + frame] += win ** 2
    out = out / np.maximum(norm, 1e-6)
    return out[frame:frame + n]


def modal(dur, partials, r=None, strike=0.002):
    """Sum of exponentially decaying sinusoids: (freq, amp, decay) tuples. Metal, wood, glass."""
    t = t_axis(dur)
    y = np.zeros_like(t)
    for f, a, d in partials:
        ph = 0 if r is None else r.uniform(0, 2 * np.pi)
        y += a * np.sin(2 * np.pi * f * t + ph) * np.exp(-t / d)
    y *= np.clip(t / strike, 0, 1)
    return y


def reverb(x, seconds=1.2, wet=0.3, predelay=0.012, bright=4000, seed=7):
    """Convolution with a synthetic decaying noise tail."""
    r = rng(seed)
    n = int(seconds * SR)
    ir = r.standard_normal(n) * np.exp(-np.arange(n) / (seconds * SR / 6.9))
    ir = lowpass(ir, bright)
    ir = np.concatenate([np.zeros(int(predelay * SR)), ir])
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    L = len(x) + len(ir)
    size = 1 << (L - 1).bit_length()
    wet_sig = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:L]
    dry = np.concatenate([x, np.zeros(len(ir))])
    return dry * (1 - wet) + wet_sig * wet * 1.6


def echo(x, delay, fb=0.35, taps=3, damp=3000):
    out = np.concatenate([x, np.zeros(int(delay * SR * taps) + 1)])
    d = int(delay * SR)
    cur = x
    for k in range(1, taps + 1):
        cur = lowpass(cur, damp) * fb
        out[k * d:k * d + len(cur)] += cur
    return out


def sat(x, drive=2.0):
    return np.tanh(x * drive) / np.tanh(drive)


def place(dst, src, at):
    i = int(at * SR)
    if i >= len(dst):
        return dst
    k = min(len(src), len(dst) - i)
    dst[i:i + k] += src[:k]
    return dst


def silence(dur):
    return np.zeros(int(dur * SR))


def norm(x, peak=0.89):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * peak


def grains(dur, r, rate, lo, hi, glen=(0.003, 0.02), amp=(0.2, 1.0), env=None):
    """Sparse short noise grains (debris, crackle, glass)."""
    y = silence(dur)
    count = int(rate * dur)
    for _ in range(count):
        at = r.uniform(0, dur) if env is None else env(r)
        L = r.uniform(*glen)
        g = band(noise(L + 0.01, r), lo, hi)[: int(L * SR)]
        g *= env_exp(len(g), 0.0005, L / 3)
        place(y, g * r.uniform(*amp), at)
    return y


def boom(dur, f0=70, f1=32, decay=0.35, r=None):
    """Sub-bass drop with a click on top."""
    n = int(dur * SR)
    body = osc(sweep(f0, f1, dur, 0.5), dur) * env_exp(n, 0.002, decay)
    click = highpass(noise(dur, r or rng(1)), 1500) * env_exp(n, 0.0003, 0.006) * 0.5
    return body + click


def whoosh(dur, r, f_lo=300, f_hi=3000, up=True, q=0.9):
    n = int(dur * SR)
    x = noise(dur, r)
    c0, c1 = (f_lo, f_hi) if up else (f_hi, f_lo)

    def center(u):
        return c0 * (c1 / c0) ** u
    y = tv_band(x, lambda u: center(u) / (1 + q), lambda u: center(u) * (1 + q))
    e = np.sin(np.linspace(0, np.pi, n)) ** 1.5
    return y * e


def crackle(dur, r, density=120, lo=2000, hi=12000):
    return grains(dur, r, density, lo, hi, glen=(0.0008, 0.004), amp=(0.1, 1.0))


# ---------------------------------------------------------------- sounds

SOUNDS = {}


def sound(name, seeds=(1,)):
    def deco(fn):
        for i, s in enumerate(seeds):
            key = name if len(seeds) == 1 else f"{name}_{i + 1}"
            SOUNDS[key] = (fn, s)
        return fn
    return deco


@sound("fx/cast", seeds=(11, 12, 13))
def cast(r):
    """Cursed energy release: thump, rising air rush and a crackling tail."""
    d = 0.9
    y = silence(d)
    place(y, boom(0.5, 110 + r.uniform(-15, 15), 45, 0.09, r) * 0.9, 0)
    w = whoosh(0.55, r, 350, 4200, up=True, q=0.7) * 0.7
    place(y, w, 0.0)
    place(y, crackle(0.7, r, 90, 3000, 11000) * 0.25 * env_exp(int(0.7 * SR), 0.01, 0.25), 0.05)
    hum = osc(sweep(220, 440, 0.4), 0.4, "saw") * env_exp(int(0.4 * SR), 0.01, 0.12)
    place(y, lowpass(hum, 1600) * 0.18, 0.0)
    return reverb(y, 0.8, 0.22)


@sound("fx/cast_ultimate", seeds=(21,))
def cast_ultimate(r):
    d = 3.2
    y = silence(d)
    rise = 1.4
    sw = whoosh(rise, r, 120, 2500, up=True, q=0.6) * np.linspace(0.2, 1, int(rise * SR)) ** 2
    place(y, sw * 0.8, 0)
    chord = sum(osc(sweep(f, f * 1.5, rise, 2.0), rise, "saw") for f in (55, 82.4, 110.5, 164.8))
    chord = lowpass(chord, 900) * np.linspace(0, 1, int(rise * SR)) ** 3 * 0.25
    place(y, chord, 0)
    place(y, boom(1.6, 90, 28, 0.5, r) * 1.2, rise)
    place(y, band(noise(1.2, r), 150, 3000) * env_exp(int(1.2 * SR), 0.002, 0.18) * 0.9, rise)
    place(y, crackle(1.4, r, 160, 2500, 9000) * 0.3 * env_exp(int(1.4 * SR), 0.01, 0.4), rise)
    return reverb(sat(y, 1.6), 2.2, 0.35)


@sound("fx/impact", seeds=(31, 32))
def impact(r):
    d = 1.8
    y = silence(d)
    place(y, boom(1.2, 85, 30, 0.32, r) * 1.1, 0)
    crunch = lowpass(sat(noise(0.4, r) * 3, 3), 2500) * env_exp(int(0.4 * SR), 0.001, 0.07)
    place(y, crunch * 0.8, 0)
    deb = grains(1.2, r, 60, 800, 6000, glen=(0.004, 0.03), amp=(0.05, 0.4),
                 env=lambda rr: 0.05 + abs(rr.normal(0, 0.35)))
    place(y, deb, 0.02)
    return reverb(y, 1.6, 0.3)


@sound("fx/slash", seeds=(41, 42, 43))
def slash(r):
    d = 0.6
    y = silence(d)
    w = whoosh(0.22, r, 7000, 900, up=False, q=0.5)
    place(y, w * 0.9, 0)
    ting = modal(0.5, [(r.uniform(2600, 3400), 0.25, 0.12), (r.uniform(5200, 6800), 0.12, 0.06)], r)
    place(y, ting, 0.17)
    place(y, highpass(noise(0.05, r), 3000) * env_exp(int(0.05 * SR), 0.0005, 0.008) * 0.6, 0.18)
    return reverb(y, 0.6, 0.18)


@sound("fx/charge", seeds=(51,))
def charge(r):
    d = 1.4
    n = int(d * SR)
    f = sweep(180, 1400, d, 1.6)
    tone = (osc(f, d, "saw") * 0.3 + osc(f * 1.01, d, "sine") * 0.4)
    tone = lowpass(tone, 4000) * np.linspace(0.05, 1, n) ** 1.5
    air = tv_band(noise(d, r), lambda u: 400 + 3000 * u, lambda u: 1500 + 9000 * u) * np.linspace(0, 0.6, n)
    sparks = crackle(d, r, 70, 4000, 12000) * np.linspace(0, 0.5, n)
    return fade_out(reverb(tone * 0.6 + air * 0.5 + sparks, 0.7, 0.2), 0.05)


@sound("fx/aura", seeds=(61,))
def aura(r):
    """Gas-flame ignition: the aura flaring up around the body."""
    d = 1.3
    n = int(d * SR)
    body = lowpass(noise(d, r), 900) * env_adsr(n, 0.04, 0.2, 0.5, 0.7)
    flutter = 1 + 0.35 * np.sin(2 * np.pi * 13 * t_axis(d)) * np.exp(-t_axis(d) * 2)
    top = band(noise(d, r), 1500, 6000) * env_exp(n, 0.01, 0.12) * 0.5
    y = body * flutter * 1.2 + top
    place(y, boom(0.4, 70, 40, 0.08, r) * 0.7, 0)
    return reverb(y, 0.9, 0.2)


# --- domain

@sound("domain/sign", seeds=(71,))
def domain_sign(r):
    d = 2.6
    y = silence(d)
    clap = band(noise(0.08, r), 900, 5000) * env_exp(int(0.08 * SR), 0.0005, 0.012)
    place(y, clap * 1.2, 0)
    wood = modal(0.4, [(410, 0.6, 0.05), (1130, 0.3, 0.03), (2380, 0.2, 0.02)], r)
    place(y, wood * 0.6, 0)
    drone = sum(osc(f, 2.4, "saw") for f in (36.7, 55.0, 55.4, 73.4)) * 0.2
    drone = lowpass(drone, 380) * env_adsr(int(2.4 * SR), 0.6, 0.4, 0.8, 1.0)
    place(y, drone, 0.1)
    return reverb(y, 3.0, 0.45, bright=3000)


@sound("domain/barrier", seeds=(72,))
def domain_barrier(r):
    """The black barrier rising and sealing: viscous rush, choir pad, sealing boom."""
    d = 4.6
    y = silence(d)
    rise = 2.2
    n = int(rise * SR)
    rush = tv_band(noise(rise, r), lambda u: 60 + 200 * u, lambda u: 350 + 1800 * u ** 2)
    rush *= np.linspace(0.1, 1, n) ** 1.3
    place(y, rush * 1.1, 0)
    rumble = osc(sweep(28, 48, rise), rise) * np.linspace(0, 1, n) * 0.8
    place(y, rumble, 0)
    t = t_axis(d - 0.4)
    pad = np.zeros_like(t)
    for f in (110, 164.8, 220.5, 277.2, 329.6):
        vib = 1 + 0.004 * np.sin(2 * np.pi * r.uniform(4, 6) * t + r.uniform(0, 6))
        pad += osc(f * vib, d - 0.4, "saw")
    pad = band(pad, 300, 2400) * 0.12 * env_adsr(len(pad), 1.6, 0.3, 0.8, 1.8)
    place(y, pad, 0.4)
    place(y, boom(2.2, 75, 24, 0.7, r) * 1.4, rise)
    place(y, lowpass(sat(noise(0.5, r) * 2, 2), 1800) * env_exp(int(0.5 * SR), 0.001, 0.09) * 0.8, rise)
    return fade_out(reverb(y, 3.2, 0.4, bright=2500), 0.4)


@sound("domain/shatter", seeds=(73,))
def domain_shatter(r):
    d = 3.4
    y = silence(d)
    place(y, boom(1.6, 70, 30, 0.4, r), 0)
    crack = band(noise(0.12, r), 800, 9000) * env_exp(int(0.12 * SR), 0.0005, 0.03)
    place(y, crack * 1.2, 0)
    for _ in range(140):
        at = abs(r.normal(0, 0.5)) + 0.02
        f = r.uniform(1800, 7500)
        g = modal(0.35, [(f, 1, r.uniform(0.03, 0.12)), (f * r.uniform(1.4, 2.7), 0.5, 0.04)], r)
        place(y, g * r.uniform(0.05, 0.35), at)
    place(y, grains(2.5, r, 90, 3000, 12000, glen=(0.002, 0.01), amp=(0.03, 0.2),
                    env=lambda rr: 0.2 + abs(rr.normal(0, 0.8))), 0)
    return fade_out(reverb(y, 2.6, 0.35, bright=6000), 0.3)


def ambient(r, d, layers):
    y = silence(d)
    for layer in layers:
        y += layer
    y = reverb(y, 1.6, 0.35)[: int(d * SR)]
    k = int(0.25 * SR)
    y[:k] *= np.linspace(0, 1, k)
    y[-k:] *= np.linspace(1, 0, k)
    return y


@sound("domain/ambient_flames", seeds=(81,))
def amb_flames(r):
    d = 3.2
    n = int(d * SR)
    roar = lowpass(noise(d, r), 700) * (1 + 0.4 * band(noise(d, r), 1, 6))
    pops = grains(d, r, 40, 1500, 8000, glen=(0.002, 0.01), amp=(0.1, 0.6))
    low = osc(41, d) * 0.3
    return ambient(r, d, [roar * 0.9, pops * 0.4, low])


@sound("domain/ambient_flesh", seeds=(82,))
def amb_flesh(r):
    d = 3.2
    t = t_axis(d)
    hum = osc(55 * (1 + 0.02 * np.sin(2 * np.pi * 0.7 * t)), d, "saw")
    hum = band(hum, 60, 600) * 0.4
    wet = tv_band(noise(d, r), lambda u: 300 + 200 * np.sin(u * 9), lambda u: 900 + 400 * np.sin(u * 7)) * 0.4
    beat = sum(boom(0.5, 60, 40, 0.1, r) * 0.6 for _ in range(1))
    y = silence(d)
    for at in (0.2, 0.55, 1.8, 2.15):
        place(y, beat, at)
    return ambient(r, d, [hum, wet, y])


@sound("domain/ambient_sea", seeds=(83,))
def amb_sea(r):
    d = 3.2
    t = t_axis(d)
    swell = 0.5 + 0.5 * np.sin(2 * np.pi * t / d - np.pi / 2)
    waves = tv_band(noise(d, r), lambda u: 200 + 300 * u, lambda u: 2500 + 2000 * np.sin(np.pi * u)) * swell
    fizz = band(noise(d, r), 3000, 10000) * swell ** 3 * 0.25
    return ambient(r, d, [waves * 0.9, fizz])


@sound("domain/ambient_cemetery", seeds=(84,))
def amb_cemetery(r):
    d = 3.2
    t = t_axis(d)
    wind = tv_band(noise(d, r), lambda u: 350 + 250 * np.sin(u * 5), lambda u: 700 + 500 * np.sin(u * 5 + 1), soft=0.15) * 0.8
    bell = modal(3.0, [(196, 0.5, 1.2), (392.6, 0.3, 0.8), (466, 0.25, 0.9), (587, 0.18, 0.6), (989, 0.1, 0.3)], r)
    y = silence(d)
    place(y, bell * 0.35, 0.3)
    return ambient(r, d, [wind, y])


@sound("domain/ambient_pachinko", seeds=(85,))
def amb_pachinko(r):
    d = 3.2
    clatter = silence(d)
    for _ in range(220):
        f = r.uniform(3000, 6000)
        place(clatter, modal(0.06, [(f, 1, 0.01), (f * 1.6, 0.5, 0.008)], r) * r.uniform(0.05, 0.3), r.uniform(0, d))
    t = t_axis(d)
    hum = osc(60, d, "square") * 0.04 + osc(120, d) * 0.05
    jingle = silence(d)
    for i, f in enumerate((1318.5, 1568, 2093, 1568)):
        place(jingle, osc(f, 0.18, "square") * env_exp(int(0.18 * SR), 0.002, 0.05) * 0.06, 0.4 + i * 0.12)
    return ambient(r, d, [clatter, lowpass(hum, 800), jingle])


@sound("domain/ambient_court", seeds=(86,))
def amb_court(r):
    d = 3.2
    t = t_axis(d)
    choir = np.zeros_like(t)
    for f in (98, 146.8, 196, 233):
        choir += osc(f * (1 + 0.003 * np.sin(2 * np.pi * 5 * t + f)), d, "saw")
    choir = band(choir, 250, 1800) * 0.1
    tick = silence(d)
    for k in range(int(d / 0.8)):
        place(tick, modal(0.1, [(1800, 1, 0.01), (3500, 0.4, 0.006)], r) * 0.25, 0.1 + k * 0.8)
    return ambient(r, d, [choir, tick])


@sound("domain/ambient_film", seeds=(87,))
def amb_film(r):
    d = 3.2
    proj = silence(d)
    for k in range(int(d * 24)):
        place(proj, band(noise(0.006, r), 1500, 6000) * 0.12, k / 24)
    t = t_axis(d)
    pad = sum(osc(f * (1 + 0.002 * np.sin(2 * np.pi * 3 * t)), d) for f in (523.3, 659.3, 784)) * 0.06
    return ambient(r, d, [proj, pad, lowpass(noise(d, r), 300) * 0.15])


@sound("domain/ambient_station", seeds=(88,))
def amb_station(r):
    d = 3.2
    t = t_axis(d)
    rumble = lowpass(noise(d, r), 200) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.5 * t)) * 1.2
    clack = silence(d)
    for at in (0.3, 0.42, 1.1, 1.22, 1.9, 2.02, 2.7, 2.82):
        place(clack, modal(0.15, [(240, 1, 0.04), (620, 0.5, 0.03)], r) * 0.4, at)
    chime = silence(d)
    for i, f in enumerate((659.3, 523.3, 587.3, 392)):
        place(chime, modal(0.9, [(f, 0.5, 0.4), (f * 2, 0.15, 0.2)], r) * 0.25, 0.5 + i * 0.32)
    return ambient(r, d, [rumble, clack, chime])


# --- weapons

def gunshot(r, size=1.0):
    d = 1.6
    y = silence(d)
    crack = highpass(noise(0.03, r), 1200) * env_exp(int(0.03 * SR), 0.0002, 0.004)
    place(y, crack * 1.4, 0)
    body = lowpass(sat(noise(0.35, r) * 2, 2.5), 2400 * size) * env_exp(int(0.35 * SR), 0.0005, 0.045 * size)
    place(y, body * 1.1, 0.001)
    place(y, boom(0.5, 120, 50, 0.06 * size, r) * 0.9 * size, 0)
    ring = modal(0.4, [(2950, 0.08, 0.08), (4720, 0.05, 0.05)], r)
    place(y, ring, 0.004)
    tail = echo(lowpass(y[: int(0.3 * SR)], 1500) * 0.5, 0.21, 0.4, 3, 1200)
    place(y, tail[int(0.2 * SR):] * 0.6, 0.2)
    return reverb(y, 1.3, 0.22)


@sound("weapon/revolver", seeds=(91, 92))
def revolver(r):
    return gunshot(r)


@sound("weapon/revolver_construct", seeds=(93,))
def revolver_construct(r):
    """Mai's last bullet: a constructed round, a heavier shot with an energy crack."""
    d = 2.2
    y = silence(d)
    whine = osc(sweep(900, 2600, 0.35, 2), 0.35) * np.linspace(0, 1, int(0.35 * SR)) ** 2 * 0.25
    place(y, whine, 0)
    place(y, gunshot(r, 1.6) * 1.0, 0.33)
    place(y, crackle(0.8, r, 140, 2500, 10000) * 0.35 * env_exp(int(0.8 * SR), 0.005, 0.25), 0.34)
    return y


@sound("weapon/revolver_reload", seeds=(94,))
def revolver_reload(r):
    d = 1.5
    y = silence(d)
    place(y, modal(0.12, [(1900, 1, 0.02), (4100, 0.5, 0.01)], r) * 0.5, 0.0)  # swing out
    for k in range(6):
        f = r.uniform(3200, 3800)
        place(y, modal(0.08, [(f, 1, 0.012), (f * 1.7, 0.4, 0.008)], r) * 0.35, 0.18 + k * 0.13)
    snap = modal(0.2, [(1300, 1, 0.03), (2700, 0.6, 0.02), (5200, 0.3, 0.01)], r)
    place(y, snap * 0.9, 1.05)
    place(y, highpass(noise(0.02, r), 2000) * 0.3, 1.05)
    return reverb(y, 0.4, 0.12)


@sound("weapon/revolver_empty", seeds=(95,))
def revolver_empty(r):
    y = silence(0.3)
    place(y, modal(0.15, [(2400, 1, 0.015), (5100, 0.5, 0.008)], r) * 0.6, 0)
    return y


@sound("weapon/rope_whip", seeds=(101, 102))
def rope_whip(r):
    d = 0.9
    y = silence(d)
    w = whoosh(0.32, r, 250, 1800, up=True, q=0.8)
    place(y, w * 0.8, 0)
    crack = band(noise(0.015, r), 1500, 12000) * env_exp(int(0.015 * SR), 0.0002, 0.0025)
    place(y, crack * 1.5, 0.3)
    place(y, boom(0.2, 160, 80, 0.03, r) * 0.4, 0.3)
    return reverb(y, 0.9, 0.25)


@sound("weapon/rope_spin", seeds=(103,))
def rope_spin(r):
    d = 1.6
    y = silence(d)
    for k in range(7):
        w = whoosh(0.22, r, 200, 900, up=k % 2 == 0, q=1.0)
        place(y, w * 0.6, k * 0.22)
    return fade_out(y, 0.1)


@sound("weapon/rope_unravel", seeds=(104,))
def rope_unravel(r):
    """Black Rope tearing a technique apart: glassy descending shimmer and an air hiss."""
    d = 1.6
    n = int(d * SR)
    shimmer = sum(osc(sweep(f, f * 0.35, d, 0.6), d) * a for f, a in ((2400, 0.3), (3100, 0.2), (4300, 0.15)))
    shimmer *= env_exp(n, 0.005, 0.4) * (0.6 + 0.4 * np.sin(2 * np.pi * 23 * t_axis(d)))
    hiss = highpass(noise(d, r), 3000) * env_exp(n, 0.01, 0.3) * 0.35
    snap = band(noise(0.04, r), 800, 6000) * env_exp(int(0.04 * SR), 0.0003, 0.008)
    y = shimmer * 0.6 + hiss
    place(y, snap, 0)
    return reverb(y, 1.2, 0.3)


@sound("weapon/broom", seeds=(105,))
def broom(r):
    d = 2.0
    t = t_axis(d)
    wind = tv_band(noise(d, r), lambda u: 250 + 150 * np.sin(u * 6), lambda u: 1100 + 500 * np.sin(u * 4 + 1), soft=0.3)
    wind *= 0.7 + 0.3 * np.sin(2 * np.pi * 1.5 * t)
    k = int(0.2 * SR)
    wind[:k] *= np.linspace(0, 1, k)
    wind[-k:] *= np.linspace(1, 0, k)
    return wind


@sound("weapon/chain", seeds=(106,))
def chain(r):
    d = 1.0
    y = silence(d)
    for _ in range(26):
        f = r.uniform(2200, 5200)
        place(y, modal(0.12, [(f, 1, 0.02), (f * 1.41, 0.6, 0.015), (f * 2.3, 0.3, 0.01)], r) * r.uniform(0.1, 0.4),
              abs(r.normal(0.12, 0.12)))
    place(y, whoosh(0.3, r, 400, 2500) * 0.4, 0)
    return reverb(y, 0.6, 0.18)


# --- technique families

@sound("lightning/zap", seeds=(111, 112))
def zap(r):
    d = 0.8
    n = int(d * SR)
    t = t_axis(d)
    buzz = osc(np.clip(90 + 40 * r.standard_normal(n).cumsum() / np.sqrt(n), 45, 300), d, "saw")
    buzz = band(buzz, 200, 6000) * env_exp(n, 0.001, 0.08)
    crack = highpass(noise(0.05, r), 1500) * env_exp(int(0.05 * SR), 0.0002, 0.006) * 1.5
    y = sat(buzz * 1.5, 2) * 0.6 + crackle(d, r, 160, 2500, 14000) * env_exp(n, 0.001, 0.12) * 0.7
    place(y, crack, 0)
    place(y, boom(0.4, 100, 40, 0.05, r) * 0.6, 0)
    return reverb(y, 0.8, 0.2)


@sound("lightning/field", seeds=(113,))
def field(r):
    d = 2.4
    n = int(d * SR)
    hum = osc(100, d, "saw") * 0.15 + osc(150, d, "square") * 0.05
    hum = band(hum, 90, 3000) * (0.7 + 0.3 * np.sin(2 * np.pi * 7 * t_axis(d)))
    y = hum + crackle(d, r, 110, 2000, 12000) * 0.6
    y *= env_adsr(n, 0.1, 0.1, 0.9, 0.4)
    return y


@sound("star/impact", seeds=(121,))
def star_impact(r):
    """Star Rage: virtual mass landing — huge sub, crunch and rolling debris."""
    d = 2.6
    y = silence(d)
    place(y, boom(2.0, 60, 22, 0.6, r) * 1.4, 0)
    place(y, lowpass(sat(noise(0.6, r) * 4, 4), 1500) * env_exp(int(0.6 * SR), 0.001, 0.1) * 0.9, 0)
    place(y, grains(2.0, r, 70, 300, 3500, glen=(0.01, 0.05), amp=(0.05, 0.4),
                    env=lambda rr: 0.05 + abs(rr.normal(0, 0.5))), 0)
    return reverb(sat(y, 1.3), 2.0, 0.3, bright=2500)


@sound("star/gravity", seeds=(122,))
def star_gravity(r):
    d = 3.0
    n = int(d * SR)
    t = t_axis(d)
    hum = osc(38 * (1 + 0.1 * np.sin(2 * np.pi * 0.8 * t)), d) * 0.7 + osc(57, d, "saw") * 0.1
    whine = osc(sweep(300, 1200, d, 2), d) * 0.08 * np.linspace(0, 1, n)
    pull = tv_band(noise(d, r), lambda u: 900 - 700 * u, lambda u: 2500 - 1800 * u) * 0.35
    return fade_out(lowpass(hum, 400) + whine + pull, 0.3) * env_adsr(n, 0.3, 0.1, 1, 0.3)


@sound("star/wing", seeds=(123,))
def star_wing(r):
    d = 1.0
    y = silence(d)
    for k, at in enumerate((0.0, 0.32)):
        flap = lowpass(noise(0.25, r), 700) * env_exp(int(0.25 * SR), 0.03, 0.06)
        place(y, flap * (1.2 - 0.3 * k), at)
    return reverb(y, 0.7, 0.2)


@sound("granite/charge", seeds=(131,))
def granite_charge(r):
    d = 1.6
    n = int(d * SR)
    f = sweep(70, 520, d, 1.4)
    core = lowpass(osc(f, d, "saw") + osc(f * 1.5, d, "saw") * 0.5, 2200) * np.linspace(0.05, 1, n) ** 2
    grit = lowpass(noise(d, r), 400) * np.linspace(0.1, 1, n) * 0.6
    return fade_out(core * 0.35 + grit + crackle(d, r, 60, 2000, 8000) * np.linspace(0, 0.4, n), 0.05)


@sound("granite/beam", seeds=(132,))
def granite_beam(r):
    d = 2.6
    n = int(d * SR)
    roar = sat(lowpass(noise(d, r), 1800) * 3, 3) * 0.6
    tone = lowpass(osc(73.4, d, "saw") + osc(110, d, "saw") * 0.6, 1200) * 0.4
    y = (roar + tone) * env_adsr(n, 0.01, 0.2, 0.7, 1.0)
    place(y, boom(1.0, 90, 35, 0.25, r) * 1.2, 0)
    return reverb(y, 1.5, 0.3)


@sound("construct/form", seeds=(141,))
def construct_form(r):
    d = 1.6
    y = silence(d)
    sh = sum(osc(f, d) * a * np.exp(-t_axis(d) / dd) for f, a, dd in
             ((1760, 0.15, 0.6), (2637, 0.1, 0.4), (3520, 0.08, 0.3)))
    sh *= np.clip(t_axis(d) / 0.15, 0, 1)
    y += sh
    clink = modal(1.2, [(1047, 0.5, 0.3), (2830, 0.35, 0.2), (4460, 0.2, 0.12), (6890, 0.1, 0.06)], r)
    place(y, clink, 0.18)
    return reverb(y, 1.2, 0.3)


@sound("construct/clang", seeds=(142,))
def construct_clang(r):
    y = silence(1.2)
    place(y, modal(1.1, [(523, 0.6, 0.35), (1390, 0.4, 0.25), (2690, 0.3, 0.15), (4300, 0.2, 0.08)], r), 0)
    place(y, highpass(noise(0.02, r), 2000) * 0.5, 0)
    return reverb(y, 0.8, 0.2)


@sound("sky/ripple", seeds=(151,))
def sky_ripple(r):
    d = 1.4
    n = int(d * SR)
    t = t_axis(d)
    mod = 1 + 0.03 * np.sin(2 * np.pi * 9 * t)
    glass = sum(osc(f * mod, d) * a for f, a in ((880, 0.3), (1318.5, 0.2), (1760, 0.12)))
    glass *= env_exp(n, 0.02, 0.35)
    air = whoosh(0.6, r, 600, 3500, up=True, q=0.5) * 0.5
    y = glass * 0.7
    place(y, air, 0)
    return reverb(y, 1.4, 0.35, bright=7000)


@sound("sky/fold", seeds=(152,))
def sky_fold(r):
    d = 1.6
    n = int(d * SR)
    y = tv_band(noise(d, r), lambda u: 120 + 600 * np.sin(np.pi * u), lambda u: 400 + 2600 * np.sin(np.pi * u), soft=0.12)
    y *= np.sin(np.linspace(0, np.pi, n)) * 1.2
    y += osc(sweep(60, 30, d), d) * env_exp(n, 0.2, 0.4) * 0.5
    return reverb(y, 1.2, 0.3)


@sound("rika/claw", seeds=(161, 162))
def rika_claw(r):
    d = 0.9
    y = silence(d)
    place(y, whoosh(0.25, r, 1200, 300, up=False, q=0.9) * 1.0, 0)
    hit = lowpass(sat(noise(0.25, r) * 3, 3), 1500) * env_exp(int(0.25 * SR), 0.001, 0.05)
    place(y, hit * 0.9, 0.2)
    place(y, boom(0.4, 90, 45, 0.08, r) * 0.8, 0.2)
    for k in range(3):
        place(y, band(noise(0.12, r), 2000, 7000) * env_exp(int(0.12 * SR), 0.001, 0.03) * 0.4, 0.2 + k * 0.03)
    return reverb(y, 0.8, 0.22)


@sound("rika/roar", seeds=(163,))
def rika_roar(r):
    """A monstrous, broken scream: jittering formant-filtered growls."""
    d = 2.8
    n = int(d * SR)
    t = t_axis(d)
    f0 = 85 * (1 + 0.25 * np.sin(2 * np.pi * 0.4 * t)) * np.clip(1 + 0.12 * r.standard_normal(n).cumsum() / np.sqrt(n), 0.7, 1.3)
    src = osc(f0, d, "saw") + osc(f0 * 1.51, d, "saw") * 0.5 + noise(d, r) * 0.3
    vox = band(src, 400, 900, 0.15) * 1.0 + band(src, 1100, 1700, 0.15) * 0.7 + band(src, 2400, 3200, 0.15) * 0.4
    vox = sat(vox * 3, 2.5) * env_adsr(n, 0.15, 0.4, 0.75, 1.2)
    rasp = band(noise(d, r), 1500, 6000) * env_adsr(n, 0.1, 0.3, 0.4, 1.0) * 0.3
    return reverb(vox + rasp, 1.8, 0.35)


@sound("swarm/buzz", seeds=(171,))
def swarm_buzz(r):
    d = 2.2
    n = int(d * SR)
    t = t_axis(d)
    y = np.zeros(n)
    for _ in range(14):
        f = r.uniform(140, 260)
        y += osc(f * (1 + 0.02 * np.sin(2 * np.pi * r.uniform(3, 9) * t + r.uniform(0, 6))), d, "saw") * r.uniform(0.3, 1)
    y = band(y, 150, 5000) * 0.12 * env_adsr(n, 0.15, 0.1, 1, 0.5)
    return y


@sound("rot/hiss", seeds=(181,))
def rot_hiss(r):
    d = 1.6
    n = int(d * SR)
    hiss = band(noise(d, r), 2500, 9000) * env_adsr(n, 0.05, 0.2, 0.5, 0.8) * 0.5
    bub = silence(d)
    for _ in range(40):
        f = r.uniform(200, 700)
        b = osc(sweep(f, f * 2.2, 0.06), 0.06) * env_exp(int(0.06 * SR), 0.002, 0.015)
        place(bub, b * r.uniform(0.1, 0.4), r.uniform(0, d - 0.1))
    return reverb(hiss + bub, 0.8, 0.25)


@sound("judge/gavel", seeds=(191,))
def gavel(r):
    y = silence(2.2)
    knock = modal(0.5, [(320, 1, 0.06), (890, 0.6, 0.04), (1750, 0.3, 0.025), (2900, 0.15, 0.015)], r)
    place(y, knock, 0)
    place(y, boom(0.6, 90, 50, 0.08, r) * 0.6, 0)
    return reverb(y, 2.4, 0.4, bright=3500)


@sound("gamble/reel", seeds=(201,))
def gamble_reel(r):
    d = 1.4
    y = silence(d)
    k, at = 0, 0.0
    while at < d - 0.1:
        f = r.uniform(2600, 3200)
        place(y, modal(0.05, [(f, 1, 0.008)], r) * 0.35, at)
        at += 0.035 + 0.06 * (at / d) ** 2
        k += 1
    place(y, modal(0.4, [(1568, 0.6, 0.12), (3136, 0.3, 0.06)], r), d - 0.25)
    return y


@sound("gamble/jackpot", seeds=(202,))
def gamble_jackpot(r):
    d = 2.6
    y = silence(d)
    notes = (1046.5, 1318.5, 1568, 2093, 1568, 2093, 2637)
    for i, f in enumerate(notes):
        tone = (osc(f, 0.35, "square") * 0.15 + osc(f * 2, 0.35) * 0.1) * env_exp(int(0.35 * SR), 0.002, 0.12)
        place(y, tone, i * 0.09)
    for _ in range(30):
        f = r.uniform(3000, 7000)
        place(y, modal(0.3, [(f, 1, 0.08)], r) * 0.08, r.uniform(0.6, 2.0))
    return reverb(y, 1.2, 0.25, bright=8000)


# ---------------------------------------------------------------- output

def trim(x, floor_db=-58, margin=0.06):
    """Cut the silent end of the reverb tail."""
    a = np.abs(x) / (np.max(np.abs(x)) + 1e-9)
    loud = np.nonzero(a > 10 ** (floor_db / 20))[0]
    end = min(len(x), (loud[-1] if len(loud) else 0) + int(margin * SR))
    x = x[:end].copy()
    k = min(len(x), int(margin * SR))
    x[-k:] *= np.linspace(1, 0, k)
    return x


def write_ogg(path, x):
    x = trim(norm(x))
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", wav, "-c:a", "libvorbis", "-q:a", "5", path], check=True)
    os.remove(wav)


def main():
    out = sys.argv[1]
    only = set(sys.argv[2:])
    for key, (fn, seed) in SOUNDS.items():
        if only and key not in only and key.rsplit("_", 1)[0] not in only:
            continue
        x = fn(rng(seed))
        write_ogg(os.path.join(out, key + ".ogg"), x)
        print(f"{key}.ogg  {len(x) / SR:.2f}s")


if __name__ == "__main__":
    main()
