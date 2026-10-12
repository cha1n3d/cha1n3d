package com.fallenroses.technique;

import com.fallenroses.config.FRConfig;
import com.fallenroses.item.CursedObjects;
import com.fallenroses.player.ClassSelection;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.player.Talent;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.impl.BloodManipulation;
import com.fallenroses.technique.impl.BoogieWoogie;
import com.fallenroses.technique.impl.Construction;
import com.fallenroses.technique.impl.CurseManipulation;
import com.fallenroses.technique.impl.CursedSpeech;
import com.fallenroses.technique.impl.DeadlySentencing;
import com.fallenroses.technique.impl.DeathPainting;
import com.fallenroses.technique.impl.DisasterFlames;
import com.fallenroses.technique.impl.DisasterPlants;
import com.fallenroses.technique.impl.DisasterTides;
import com.fallenroses.technique.impl.Domains;
import com.fallenroses.technique.impl.GraniteBlast;
import com.fallenroses.technique.impl.Heavenly;
import com.fallenroses.technique.impl.IceFormation;
import com.fallenroses.technique.impl.IdleDeathGamble;
import com.fallenroses.technique.impl.IdleTransfiguration;
import com.fallenroses.technique.impl.Lightning;
import com.fallenroses.technique.impl.Limitless;
import com.fallenroses.technique.impl.Projection;
import com.fallenroses.technique.impl.Ratio;
import com.fallenroses.technique.impl.Rika;
import com.fallenroses.technique.impl.Shrine;
import com.fallenroses.technique.impl.SkyManipulation;
import com.fallenroses.technique.impl.Smallpox;
import com.fallenroses.technique.impl.StarRage;
import com.fallenroses.technique.impl.StrawDoll;
import com.fallenroses.technique.impl.Swarm;
import com.fallenroses.technique.impl.TenShadows;
import com.fallenroses.technique.impl.Vessel;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;

public final class AbilitySystem {
    public static void onKey(ServerPlayer player, int action) {
        if (!player.isSpectator() && player.isAlive() && !ClassSelection.inIntro(player)) {
            PlayerData data = PlayerDataCap.get(player);
            if (data != null && data.getPlayerClass() != null) {
                if (action == 5) {
                    toggleReinforcement(player, data);
                } else if (action >= 0 && action < 5) {
                    use(player, data, action);
                }
            }
        }
    }

    private static void toggleReinforcement(ServerPlayer player, PlayerData data) {
        boolean on = !data.isReinforced();
        if (on && data.getEnergy() < 5.0F && !player.getAbilities().instabuild) {
            fail(player, Component.translatable(data.usesStamina() ? "message.fallenroses.not_enough_stamina" : "message.fallenroses.not_enough", 5));
        } else {
            data.setReinforced(on);
            String key = data.usesStamina() ? "message.fallenroses.focus_" : "message.fallenroses.reinforce_";
            player.displayClientMessage(
                Component.translatable(key + (on ? "on" : "off")).withStyle(on ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true
            );
            if (on) {
                Vfx.at(player, VfxKind.REINFORCE, 1.0F, data.usesStamina() ? 1 : 0);
                player.level()
                    .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.REINFORCE.get(),
                        SoundSource.PLAYERS,
                        0.8F,
                        data.usesStamina() ? 0.8F : 1.0F
                    );
            }
        }
    }

    private static void use(ServerPlayer player, PlayerData data, int slot) {
        Technique technique = data.getTechnique();
        if (technique == null) {
            fail(player, Component.translatable("message.fallenroses.no_technique"));
        } else if (!data.isSlotUnlocked(slot)) {
            int need = technique.ability(slot).fingers();
            fail(
                player,
                data.getFingers() < need
                    ? Component.translatable("message.fallenroses.slot_fingers", data.getFingers(), need)
                    : Component.translatable("message.fallenroses.slot_locked")
            );
        } else if (data.confiscatedTicks > 0) {
            fail(player, Component.translatable("message.fallenroses.confiscated", data.confiscatedTicks / 20));
        } else {
            AbilityDef ability = technique.ability(slot);
            boolean free = player.getAbilities().instabuild;
            if (ability.toggle() && data.isInfinity()) {
                data.setInfinity(false);
                data.setCooldown(slot, Math.round((float)ability.cooldown() * data.cooldownMultiplier(slot)));
                player.displayClientMessage(
                    ability.displayName().append(Component.translatable("message.fallenroses.toggle_off")).withStyle(ChatFormatting.GRAY), true
                );
            } else if (data.getCooldown(slot) > 0) {
                fail(
                    player,
                    Component.translatable("message.fallenroses.cooldown", ability.displayName(), String.format("%.1f", (float)data.getCooldown(slot) / 20.0F))
                );
            } else {
                float cost = ability.cost() * data.costMultiplier(slot);
                if (CursedObjects.BlindfoldItem.wearing(player)) {
                    cost *= 0.75F;
                }

                if (!free && data.getEnergy() < cost) {
                    fail(
                        player,
                        Component.translatable(
                            data.usesStamina() ? "message.fallenroses.not_enough_stamina" : "message.fallenroses.not_enough", Math.round(cost)
                        )
                    );
                } else if (slot == 4 && technique.hasDomain() && Domains.isInsideOwnDomain(player)) {
                    fail(player, Component.translatable("message.fallenroses.domain_active"));
                } else {
                    float power = data.power(slot);

                    boolean done = switch (technique) {
                        case LIMITLESS -> Limitless.cast(player, data, ability.id(), power);
                        case SHRINE -> Shrine.cast(player, data, ability.id(), power);
                        case CURSED_SPEECH -> CursedSpeech.cast(player, data, ability.id(), power);
                        case BOOGIE_WOOGIE -> BoogieWoogie.cast(player, data, ability.id(), power);
                        case HEAVENLY_BODY -> Heavenly.cast(player, data, ability.id(), power);
                        case BLOOD_MANIPULATION -> BloodManipulation.cast(player, data, ability.id(), power);
                        case RATIO -> Ratio.cast(player, data, ability.id(), power);
                        case STRAW_DOLL -> StrawDoll.cast(player, data, ability.id(), power);
                        case DISASTER_FLAMES -> DisasterFlames.cast(player, data, ability.id(), power);
                        case IDLE_TRANSFIGURATION -> IdleTransfiguration.cast(player, data, ability.id(), power);
                        case DISASTER_PLANTS -> DisasterPlants.cast(player, data, ability.id(), power);
                        case CURSE_MANIPULATION -> CurseManipulation.cast(player, data, ability.id(), power);
                        case TEN_SHADOWS -> TenShadows.cast(player, data, ability.id(), power);
                        case PROJECTION -> Projection.cast(player, data, ability.id(), power);
                        case ICE_FORMATION -> IceFormation.cast(player, data, ability.id(), power);
                        case DISASTER_TIDES -> DisasterTides.cast(player, data, ability.id(), power);
                        case SMALLPOX -> Smallpox.cast(player, data, ability.id(), power);
                        case IDLE_DEATH_GAMBLE -> IdleDeathGamble.cast(player, data, ability.id(), power);
                        case LIGHTNING -> Lightning.cast(player, data, ability.id(), power);
                        case STAR_RAGE -> StarRage.cast(player, data, ability.id(), power);
                        case DEATH_PAINTING -> DeathPainting.cast(player, data, ability.id(), power);
                        case VESSEL -> Vessel.cast(player, data, ability.id(), power);
                        case DEADLY_SENTENCING -> DeadlySentencing.cast(player, data, ability.id(), power);
                        case GRANITE_BLAST -> GraniteBlast.cast(player, data, ability.id(), power);
                        case SWARM -> Swarm.cast(player, data, ability.id(), power);
                        case CONSTRUCTION -> Construction.cast(player, data, ability.id(), power);
                        case SKY_MANIPULATION -> SkyManipulation.cast(player, data, ability.id(), power);
                        case RIKA -> Rika.cast(player, data, ability.id(), power);
                    };
                    if (done && technique == Technique.PROJECTION) {
                        data.projectionStacks = Math.min(5, data.projectionStacks + 1);
                        data.projectionDecay = 80;
                    }

                    if (done) {
                        Vfx.play(
                            player.serverLevel(),
                            VfxKind.MANGA_CAST,
                            player.getEyePosition(),
                            player.getViewVector(1.0F),
                            (float)slot,
                            technique.color(),
                            player.getId(),
                            technique.ordinal()
                        );
                        if (!free) {
                            data.consume(cost);
                        }

                        int chain = data.talentRank(Talent.TECH_CHAIN);
                        if (chain > 0) {
                            for (int i = 0; i < 5; i++) {
                                if (i != slot && data.getCooldown(i) > 0) {
                                    data.setCooldown(i, Math.max(0, data.getCooldown(i) - 20 * chain));
                                }
                            }
                        }

                        data.sinceCast = 0;
                        data.setCooldown(slot, Math.round((float)ability.cooldown() * data.cooldownMultiplier(slot)));
                        player.displayClientMessage(ability.displayName().withStyle(s -> s.withColor(technique.color()).withBold(slot == 4)), true);
                    }
                }
            }
        }
    }

    private static void fail(ServerPlayer player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
        player.playNotifySound(ModSounds.UI_ERROR.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
    }

    public static void tick(ServerPlayer player, PlayerData data) {
        boolean creative = player.getAbilities().instabuild;
        if (data.isReinforced()) {
            float drain = FRConfig.REINFORCE_DRAIN.get().floatValue() / 20.0F * (data.hasTalent(Talent.REINFORCE_MASTERY) ? 0.6F : 1.0F);
            if (!creative) {
                if (data.getEnergy() >= drain) {
                    data.setEnergy(data.getEnergy() - drain);
                } else {
                    data.setReinforced(false);
                    player.displayClientMessage(
                        Component.translatable(data.usesStamina() ? "message.fallenroses.stamina_empty" : "message.fallenroses.reinforce_empty")
                            .withStyle(ChatFormatting.GRAY),
                        true
                    );
                }
            }

            ReverseTechnique.tick(player, data);
        } else if (!data.isInfinity()) {
            data.regen(data.regenPerTick());
        }

        if (data.isInfinity()) {
            Limitless.tickInfinity(player, data, creative);
        }

        Heavenly.tick(player, data);
        IdleDeathGamble.tick(player, data);
        Lightning.tick(player, data);
        GraniteBlast.tick(player, data);
        SkyManipulation.tick(player, data);
        if (data.getPlayerClass() == PlayerClass.VESSEL && data.getPaintings() > 0 && player.tickCount % 10 == 0) {
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(ModEffects.ROT.get());
        }

        if (data.getPlayerClass() == PlayerClass.CURSED_SPIRIT && player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            boolean body = data.hasTalent(Talent.SPIRIT_BODY);
            int period = body ? 20 : 40;
            float price = body ? 1.5F : 2.5F;
            if (player.tickCount % period == 0 && (creative || data.getEnergy() >= price)) {
                if (!creative) {
                    data.setEnergy(data.getEnergy() - price);
                }

                player.heal(1.0F);
                if (player.tickCount % (period * 3) == 0) {
                    Vfx.at(player, VfxKind.RCT_HEAL);
                }
            }
        }

        if (data.hasTalent(Talent.RECOVERY) && player.tickCount % 40 == 0 && player.getHealth() < player.getMaxHealth() && player.isAlive()) {
            player.heal(1.0F);
        }
    }

    private AbilitySystem() {
    }
}
