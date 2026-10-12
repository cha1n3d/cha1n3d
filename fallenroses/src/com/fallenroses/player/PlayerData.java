package com.fallenroses.player;

import com.fallenroses.config.FRConfig;
import com.fallenroses.technique.Technique;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class PlayerData {
    @Nullable
    private PlayerClass playerClass;
    private int level = 1;
    private int xp;
    private int talentPoints;
    private final int[] stats = new int[Stat.values().length];
    private final int[] talents = new int[Talent.values().length];
    private float energy = -1.0F;
    private float killBonus;
    private int fingers;
    private int paintings;
    @Nullable
    private Technique technique;
    private boolean reinforced;
    private boolean infinity;
    private final int[] cooldowns = new int[5];
    private int absorbed;
    public int zoneTicks;
    public int strikeReadyTicks;
    public int berserkTicks;
    public int instinctTicks;
    public int overtimeTicks;
    public int scaleTicks;
    public int stationTicks;
    public int projectionStacks;
    public int projectionDecay;
    public int confiscatedTicks;
    public int sinceCast = 999;
    private boolean dirty = true;
    private boolean urgent = true;

    public PlayerData() {
        this.resetTalentRanks();
    }

    @Nullable
    public PlayerClass getPlayerClass() {
        return this.playerClass;
    }

    public void chooseClass(PlayerClass c, @Nullable Technique chosenTechnique) {
        this.playerClass = c;

        for (Stat s : Stat.values()) {
            this.stats[s.ordinal()] = c.startStat(s);
        }

        this.resetTalentRanks();
        this.talentPoints = Progression.talentPointsForLevel(this.level);
        Technique fixed = c.fixedTechnique();
        this.technique = fixed != null ? fixed : chosenTechnique;
        if (c == PlayerClass.VESSEL && this.fingers == 0) {
            this.fingers = 1;
        }

        this.reinforced = false;
        this.infinity = false;
        this.clearCooldowns();
        this.energy = this.getMax();
        this.markUrgent();
    }

    public void resetClass() {
        this.playerClass = null;
        this.technique = null;
        this.reinforced = false;
        this.infinity = false;
        this.level = 1;
        this.xp = 0;
        this.talentPoints = 0;
        this.resetTalentRanks();

        for (int i = 0; i < this.stats.length; i++) {
            this.stats[i] = 0;
        }

        this.energy = -1.0F;
        this.markUrgent();
    }

    public boolean usesStamina() {
        return this.playerClass != null && this.playerClass.usesStamina();
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = Mth.clamp(level, 1, 50);
        this.markUrgent();
    }

    public int getXp() {
        return this.xp;
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
        this.markUrgent();
    }

    public int getTalentPoints() {
        return this.talentPoints;
    }

    public void setTalentPoints(int points) {
        this.talentPoints = Math.max(0, points);
        this.markUrgent();
    }

    public int getStat(Stat stat) {
        return this.stats[stat.ordinal()] + talentStatBonus(this.talents, stat);
    }

    public int baseStat(Stat stat) {
        return this.stats[stat.ordinal()];
    }

    public static int talentStatBonus(int[] ranks, Stat stat) {
        int bonus = 0;

        for (Talent t : Talent.values()) {
            if (t.stat == stat) {
                bonus += ranks[t.ordinal()] * t.statPerRank;
            }
        }

        return bonus;
    }

    public int talentRank(Talent t) {
        return this.talents[t.ordinal()];
    }

    public boolean hasTalent(Talent t) {
        return this.talents[t.ordinal()] > 0;
    }

    public int[] talentRanks() {
        return this.talents;
    }

    public void setTalentRank(Talent t, int rank) {
        this.talents[t.ordinal()] = Mth.clamp(rank, 0, t.maxRank);
        this.setEnergy(this.getEnergy());
        this.markUrgent();
    }

    public int spentTalentPoints() {
        int spent = 0;

        for (Talent t : Talent.values()) {
            spent += this.talents[t.ordinal()] * t.cost;
        }

        return spent;
    }

    private void resetTalentRanks() {
        for (int i = 0; i < this.talents.length; i++) {
            this.talents[i] = 0;
        }

        this.talents[Talent.CORE.ordinal()] = 1;
    }

    public void refundTalents() {
        this.talentPoints = this.talentPoints + this.spentTalentPoints();
        this.resetTalentRanks();
        this.setEnergy(this.getEnergy());
        this.markUrgent();
    }

    public boolean isSlotUnlocked(int slot) {
        return isSlotUnlocked(slot, this.talents, this.technique, this.fingers, this.paintings);
    }

    public static boolean isSlotUnlocked(int slot, int[] ranks, @Nullable Technique technique, int fingers, int paintings) {
        if (technique != null && paintings < technique.ability(slot).paintings()) {
            return false;
        } else if (technique != null && fingers < technique.ability(slot).fingers()) {
            return false;
        } else {
            Talent need = Talent.forSlot(slot);
            return need == null || ranks[need.ordinal()] > 0;
        }
    }

    public int getAbsorbed() {
        return this.absorbed;
    }

    public void setAbsorbed(int value) {
        this.absorbed = Mth.clamp(value, 0, this.maxAbsorbed());
        this.markUrgent();
    }

    public int maxAbsorbed() {
        return 12 + (this.hasTalent(Talent.CURSE_BOND) ? 6 : 0) + this.getLevel() / 5;
    }

    public float getMax() {
        if (this.playerClass == null) {
            return FRConfig.BASE_MAX_ENERGY.get().floatValue();
        } else {
            float peak = (this.hasTalent(Talent.ENERGY_PEAK) ? 1.25F : 1.0F)
                + (float)this.talentRank(Talent.DEEP_RESERVE) * 0.05F
                + (float)this.talentRank(Talent.INNER_WORLD) * 0.04F
                + (this.hasTalent(Talent.BOUNDLESS_SPRING) ? 0.1F : 0.0F);
            if (this.playerClass.usesStamina()) {
                return (this.playerClass.baseEnergy() + (float)this.getStat(Stat.ENERGY) * 8.0F) * peak;
            } else {
                float fingerBonus = FRConfig.FINGER_BONUS.get().floatValue() * (this.playerClass == PlayerClass.VESSEL ? 2.0F : 1.0F);
                return (this.playerClass.baseEnergy() + (float)this.getStat(Stat.ENERGY) * 12.0F + this.killBonus + (float)this.fingers * fingerBonus) * peak;
            }
        }
    }

    public float regenPerTick() {
        float percent = this.usesStamina()
            ? 6.0F + (float)this.getStat(Stat.CONTROL) * 0.2F + (float)this.talentRank(Talent.FLOW) * 1.0F + (float)this.talentRank(Talent.REGEN_SURGE) * 0.5F
            : FRConfig.REGEN_PERCENT.get().floatValue()
                + (float)this.getStat(Stat.CONTROL) * 0.1F
                + (float)this.talentRank(Talent.FLOW) * 0.5F
                + (float)this.talentRank(Talent.REGEN_SURGE) * 0.25F;
        if (this.hasTalent(Talent.BOUNDLESS_SPRING)) {
            percent *= 1.5F;
        }

        return this.getMax() * percent / 100.0F / 20.0F;
    }

    public static float mastery(int level) {
        return 0.3F + 0.7F * (float)Math.pow((double)Mth.clamp((float)(level - 1) / 49.0F, 0.0F, 1.0F), 0.85);
    }

    public float mastery() {
        return mastery(this.level);
    }

    public float power() {
        float p = 1.0F + (float)this.getStat(Stat.CONTROL) * 0.04F + Math.max(0.0F, this.getMax() - 120.0F) / 800.0F + techPowerBonus(this.talents);
        p *= this.mastery();
        if (this.technique != null) {
            p *= this.technique.balance();
        }

        if (this.zoneTicks > 0) {
            p *= 1.2F;
        }

        if (this.berserkTicks > 0) {
            p *= 1.5F;
        }

        if (this.overtimeTicks > 0) {
            p *= 1.3F;
        }

        if (this.scaleTicks > 0) {
            p *= 1.15F;
        }

        return p;
    }

    public float power(int slot) {
        float p = this.power();
        if (slot == 4) {
            if (this.hasTalent(Talent.ULT_MASTERY)) {
                p *= 1.25F;
            }

            p *= 1.0F + (float)this.talentRank(Talent.ULT_HEART) * 0.06F;
        }

        return p;
    }

    public static float techPowerBonus(int[] r) {
        return (float)r[Talent.TECH_INSIGHT.ordinal()] * 0.02F
            + (float)r[Talent.TECH_FOCUS.ordinal()] * 0.02F
            + (float)r[Talent.TECH_POWER.ordinal()] * 0.06F
            + (float)r[Talent.TECH_PRECISION.ordinal()] * 0.03F
            + (float)r[Talent.TECH_DEPTH.ordinal()] * 0.04F
            + (float)r[Talent.TECH_SURGE.ordinal()] * 0.03F
            + (float)r[Talent.TECH_EXPANSION.ordinal()] * 0.03F
            + (float)r[Talent.TRANSCEND.ordinal()] * 0.04F
            + (r[Talent.TECH_PEAK.ordinal()] > 0 ? 0.1F : 0.0F);
    }

    public float costMultiplier(int slot) {
        float m = this.costMultiplier();
        if (slot == 4) {
            if (this.hasTalent(Talent.ULT_MASTERY)) {
                m *= 0.8F;
            }

            m *= 1.0F - (float)this.talentRank(Talent.THRESHOLD) * 0.05F;
        }

        return m;
    }

    public float costMultiplier() {
        return costMultiplier(this.talents);
    }

    public static float costMultiplier(int[] ranks) {
        float m = ranks[Talent.EFFICIENCY.ordinal()] > 0 ? 0.85F : 1.0F;
        return m * (1.0F - (float)(ranks[Talent.TECH_FLOW.ordinal()] + ranks[Talent.TECH_RHYTHM.ordinal()] + ranks[Talent.TECH_INSTINCT.ordinal()]) * 0.03F);
    }

    public float cooldownMultiplier(int slot) {
        float m = 1.0F
            - (float)this.talentRank(Talent.TECH_COOLDOWN) * 0.07F
            - (float)this.talentRank(Talent.TECH_RESONANCE) * 0.04F
            - (float)this.talentRank(Talent.TECH_ENDURANCE) * 0.03F;
        if (slot == 4) {
            if (this.hasTalent(Talent.TECH_PEAK)) {
                m *= 0.75F;
            }

            m *= 1.0F - (float)this.talentRank(Talent.ULT_SEAL) * 0.08F - (float)this.talentRank(Talent.ULT_ECHO) * 0.1F;
        }

        return Math.max(0.3F, m);
    }

    public float getEnergy() {
        if (this.energy < 0.0F) {
            this.energy = this.getMax();
        }

        return this.energy;
    }

    public void setEnergy(float value) {
        float clamped = Mth.clamp(value, 0.0F, this.getMax());
        if (clamped != this.getEnergy()) {
            this.energy = clamped;
            this.dirty = true;
        }
    }

    public boolean consume(float amount) {
        if (this.getEnergy() < amount) {
            return false;
        } else {
            this.setEnergy(this.energy - amount);
            this.urgent = true;
            return true;
        }
    }

    public void regen(float amount) {
        if (this.getEnergy() < this.getMax()) {
            this.setEnergy(this.energy + amount);
        }
    }

    public float getKillBonus() {
        return this.killBonus;
    }

    public void addKillBonus(float amount) {
        this.killBonus = (float)Math.min(FRConfig.KILL_BONUS_CAP.get(), (double)(this.killBonus + amount));
        this.markUrgent();
    }

    public int getFingers() {
        return this.fingers;
    }

    public int getPaintings() {
        return this.paintings;
    }

    public void setPaintings(int value) {
        this.paintings = Mth.clamp(value, 0, 9);
        this.markUrgent();
    }

    public void prepareRebirth() {
        this.playerClass = null;
        this.technique = null;
        this.reinforced = false;
        this.infinity = false;
        this.resetTalentRanks();
        this.talentPoints = 0;

        for (int i = 0; i < this.stats.length; i++) {
            this.stats[i] = 0;
        }

        this.clearCooldowns();
        this.markUrgent();
    }

    public void setFingers(int fingers) {
        this.fingers = fingers;
        this.setEnergy(this.getEnergy());
        this.markUrgent();
    }

    @Nullable
    public Technique getTechnique() {
        return this.technique;
    }

    public void setTechnique(@Nullable Technique technique) {
        this.technique = technique;
        this.infinity = false;
        this.clearCooldowns();
    }

    public boolean isReinforced() {
        return this.reinforced;
    }

    public void setReinforced(boolean reinforced) {
        this.reinforced = reinforced;
        this.markUrgent();
    }

    public boolean isInfinity() {
        return this.infinity;
    }

    public void setInfinity(boolean infinity) {
        this.infinity = infinity;
        this.markUrgent();
    }

    public int getCooldown(int slot) {
        return this.cooldowns[slot];
    }

    public void setCooldown(int slot, int ticks) {
        this.cooldowns[slot] = ticks;
        this.markUrgent();
    }

    public void tick() {
        for (int i = 0; i < this.cooldowns.length; i++) {
            if (this.cooldowns[i] > 0) {
                this.cooldowns[i]--;
            }
        }

        if (this.zoneTicks > 0 && --this.zoneTicks == 0) {
            this.markUrgent();
        }

        if (this.strikeReadyTicks > 0) {
            this.strikeReadyTicks--;
        }

        if (this.berserkTicks > 0 && --this.berserkTicks == 0) {
            this.markUrgent();
        }

        if (this.instinctTicks > 0) {
            this.instinctTicks--;
        }

        if (this.overtimeTicks > 0 && --this.overtimeTicks == 0) {
            this.markUrgent();
        }

        if (this.scaleTicks > 0) {
            this.scaleTicks--;
        }

        if (this.stationTicks > 0) {
            this.stationTicks--;
        }

        if (this.projectionDecay > 0 && --this.projectionDecay == 0) {
            this.projectionStacks = 0;
        }

        if (this.confiscatedTicks > 0) {
            this.confiscatedTicks--;
        }

        if (this.sinceCast < 999) {
            this.sinceCast++;
        }
    }

    public void clearCooldowns() {
        for (int i = 0; i < this.cooldowns.length; i++) {
            this.cooldowns[i] = 0;
        }

        this.markUrgent();
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isUrgent() {
        return this.urgent;
    }

    public void markUrgent() {
        this.dirty = true;
        this.urgent = true;
    }

    public void clean() {
        this.dirty = false;
        this.urgent = false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (this.playerClass != null) {
            tag.putString("Class", this.playerClass.id());
        }

        tag.putInt("Level", this.level);
        tag.putInt("Xp", this.xp);
        tag.putInt("TalentPoints", this.talentPoints);
        tag.putIntArray("Stats", (int[])this.stats.clone());
        CompoundTag tal = new CompoundTag();

        for (Talent t : Talent.values()) {
            if (this.talents[t.ordinal()] > 0) {
                tal.putInt(t.id(), this.talents[t.ordinal()]);
            }
        }

        tag.put("Talents", tal);
        tag.putInt("TalentLayout", 2);
        tag.putFloat("Energy", this.getEnergy());
        tag.putFloat("KillBonus", this.killBonus);
        tag.putInt("Fingers", this.fingers);
        tag.putInt("Paintings", this.paintings);
        if (this.technique != null) {
            tag.putString("Technique", this.technique.id());
        }

        tag.putBoolean("Reinforced", this.reinforced);
        tag.putBoolean("Infinity", this.infinity);
        tag.putIntArray("Cooldowns", (int[])this.cooldowns.clone());
        tag.putInt("Absorbed", this.absorbed);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.playerClass = tag.contains("Class") ? PlayerClass.byId(tag.getString("Class")) : null;
        this.level = Math.max(1, tag.getInt("Level"));
        this.xp = tag.getInt("Xp");
        int[] s = tag.getIntArray("Stats");

        for (int i = 0; i < this.stats.length; i++) {
            this.stats[i] = i < s.length ? s[i] : 0;
        }

        this.resetTalentRanks();
        if (tag.contains("Talents")) {
            this.talentPoints = tag.getInt("TalentPoints");
            CompoundTag tal = tag.getCompound("Talents");

            for (Talent t : Talent.values()) {
                if (tal.contains(t.id())) {
                    this.talents[t.ordinal()] = Mth.clamp(tal.getInt(t.id()), 0, t.maxRank);
                }
            }

            this.talents[Talent.CORE.ordinal()] = 1;
            if (tag.getInt("TalentLayout") < 2) {
                this.resetTalentRanks();
                this.talentPoints = Progression.talentPointsForLevel(this.level);
            }
        } else if (this.playerClass != null) {
            for (Stat st : Stat.values()) {
                this.stats[st.ordinal()] = this.playerClass.startStat(st);
            }

            this.talentPoints = Progression.talentPointsForLevel(this.level);
        }

        this.energy = tag.contains("Energy") ? tag.getFloat("Energy") : -1.0F;
        this.killBonus = tag.getFloat("KillBonus");
        this.fingers = tag.getInt("Fingers");
        this.paintings = tag.getInt("Paintings");
        this.technique = tag.contains("Technique") ? Technique.byId(tag.getString("Technique")) : null;
        if (this.playerClass == PlayerClass.VESSEL && this.technique == Technique.SHRINE) {
            this.technique = Technique.VESSEL;
        }

        this.reinforced = tag.getBoolean("Reinforced");
        this.infinity = tag.getBoolean("Infinity");
        int[] cd = tag.getIntArray("Cooldowns");

        for (int i = 0; i < this.cooldowns.length; i++) {
            this.cooldowns[i] = i < cd.length ? cd[i] : 0;
        }

        this.absorbed = tag.getInt("Absorbed");
        this.markUrgent();
    }

    public void copyFrom(PlayerData other) {
        this.load(other.save());
    }
}
