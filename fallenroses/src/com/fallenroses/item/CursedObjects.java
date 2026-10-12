package com.fallenroses.item;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.entity.BlueOrbEntity;
import com.fallenroses.entity.HollowPurpleEntity;
import com.fallenroses.entity.TechniqueProjectile;
import com.fallenroses.entity.curse.CurseEntity;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.fallenroses.entity.curse.CursedSpirit;
import com.fallenroses.player.FRAdvancements;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModItems;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.technique.Technique;
import com.fallenroses.technique.impl.DeathPainting;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CursedObjects {
    public static final ArmorMaterial BLINDFOLD_MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return 400;
        }

        @Override
        public int getDefenseForType(Type type) {
            return 1;
        }

        @Override
        public int getEnchantmentValue() {
            return 20;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.CURSED_ESSENCE.get());
        }

        @Override
        public String getName() {
            return "fallenroses:blindfold";
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };
    private static final String IAI = "fr_iai_until";
    private static final String IAI_HIT = "fr_iai_hit";

    static boolean toolSkill(ServerPlayer p, CursedToolItem.Kind kind) {
        PlayerData d = PlayerDataCap.get(p);
        float power = d == null ? 1.0F : d.power();
        ServerLevel level = p.serverLevel();
        switch (kind) {
            case CHAIN_OF_THOUSAND_MILES:
                LivingEntity t = Combat.lookTarget(p, 26.0);
                if (t == null) {
                    return false;
                }

                Vec3 from = p.getEyePosition().add(p.getViewVector(1.0F).scale(0.6));
                Vfx.play(level, VfxKind.CHAIN_PULL, from, t.getBoundingBox().getCenter().subtract(from), 1.0F, 13158608, t.getId(), 0);
                Combat.sound(p, p.position(), ModSounds.WEAPON_CHAIN, 1.2F, 0.9F);
                Vec3 pull = p.position().subtract(t.position());
                double dist = pull.length();
                t.setDeltaMovement(pull.normalize().scale(Math.min(2.4, 0.35 + dist * 0.12)).add(0.0, 0.35, 0.0));
                t.hurtMarked = true;
                Combat.hurt(t, p, p, 4.0F * power);
                return true;
            case EXECUTIONER_SWORD:
                LivingEntity tgt = Combat.lookTarget(p, 10.0);
                if (tgt == null) {
                    return false;
                }

                Vfx.play(level, VfxKind.VERDICT, tgt.position(), Vec3.ZERO, 1.2F, 14737632, tgt.getId(), 0);
                Combat.sound(p, tgt.position(), ModSounds.COFFIN_BELL, 1.6F, 0.7F);
                p.displayClientMessage(Component.translatable("message.fallenroses.verdict_trial").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                tgt.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 40, 0), p);
                Scheduler.later(
                    40,
                    () -> {
                        if (tgt.isAlive() && p.isAlive()) {
                            boolean death = p.getRandom().nextFloat() < 0.3F;
                            if (death) {
                                Vfx.play(level, VfxKind.VERDICT, tgt.position(), Vec3.ZERO, 1.6F, 16777215, tgt.getId(), 1);
                                Combat.sound(p, tgt.position(), ModSounds.CLEAVE, 2.0F, 0.6F);
                                Combat.shake(level, tgt.position(), 1.2F, 12, 20.0);
                                Combat.hurt(tgt, p, p, Math.max(30.0F * power, tgt.getHealth() < tgt.getMaxHealth() * 0.3F ? tgt.getHealth() + 1.0F : 0.0F));
                                p.displayClientMessage(
                                    Component.translatable("message.fallenroses.verdict_death").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true
                                );
                            } else {
                                tgt.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 2), p);
                                tgt.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1), p);
                                Vfx.play(level, VfxKind.SPEECH_SEAL, tgt.getBoundingBox().getCenter(), Vec3.ZERO, tgt.getBbHeight(), 14737632, tgt.getId(), 60);
                                p.displayClientMessage(Component.translatable("message.fallenroses.verdict_confiscation").withStyle(ChatFormatting.GRAY), true);
                            }
                        }
                    }
                );
                return true;
            default:
                return false;
        }
    }

    static int iaiStance(ServerPlayer p) {
        p.getPersistentData().putLong("fr_iai_until", p.level().getGameTime() + 60L);
        p.getPersistentData().putIntArray("fr_iai_hit", new int[0]);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false));
        Vfx.play(p.serverLevel(), VfxKind.STATIC_FIELD, p.position(), Vec3.ZERO, 3.5F, 11065599, p.getId(), 60);
        Combat.sound(p, p.position(), ModSounds.CLEAVE, 0.8F, 1.8F);
        p.displayClientMessage(Component.translatable("message.fallenroses.iai").withStyle(ChatFormatting.AQUA), true);
        return 160;
    }

    static void iaiTick(ServerPlayer p) {
        CompoundTag tag = p.getPersistentData();
        if (tag.getLong("fr_iai_until") > p.level().getGameTime()) {
            Set<Integer> hit = new HashSet<>();

            for (int id : tag.getIntArray("fr_iai_hit")) {
                hit.add(id);
            }

            PlayerData d = PlayerDataCap.get(p);
            float power = d == null ? 1.0F : d.power();
            boolean any = false;

            for (LivingEntity e : Combat.around(p, p.position().add(0.0, 1.0, 0.0), 3.5)) {
                if (hit.add(e.getId())) {
                    any = true;
                    Combat.hurt(e, p, p, 10.0F * power);
                    Vfx.play(p.serverLevel(), VfxKind.SLASH_HIT, e.getBoundingBox().getCenter(), e.position().subtract(p.position()), 1.6F, 16777215, -1, 10);
                    Combat.sound(p, e.position(), ModSounds.CLEAVE, 1.4F, 1.3F);
                }
            }

            if (any) {
                tag.putIntArray("fr_iai_hit", hit.stream().mapToInt(Integer::intValue).toArray());
            }
        }
    }

    /** Mai's revolver: six rounds in the cylinder; the sixth is a bullet Mai builds with Construction. */
    static int revolverUse(ServerPlayer p, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Rounds")) {
            tag.putInt("Rounds", 6);
        }

        int rounds = tag.getInt("Rounds");
        if (rounds <= 0 || p.isShiftKeyDown() && rounds < 6) {
            if (rounds <= 0) {
                Combat.sound(p, p.position(), ModSounds.WEAPON_REVOLVER_EMPTY, 0.8F, 1.0F);
            }

            Combat.sound(p, p.position(), ModSounds.WEAPON_REVOLVER_RELOAD, 0.9F, 1.0F);
            tag.putInt("Rounds", 6);
            p.displayClientMessage(Component.translatable("message.fallenroses.revolver_reload").withStyle(ChatFormatting.GRAY), true);
            return 28;
        } else {
            PlayerData d = PlayerDataCap.get(p);
            boolean construct = rounds == 1 && (p.getAbilities().instabuild || d != null && d.consume(20.0F));
            float power = d == null ? 1.0F : d.power();
            revolverFire(p, construct, power);
            stack.hurtAndBreak(1, p, x -> x.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            tag.putInt("Rounds", rounds - 1);
            p.displayClientMessage(
                construct
                    ? Component.translatable("message.fallenroses.revolver_last").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("message.fallenroses.revolver_ammo", cylinder(rounds - 1)).withStyle(ChatFormatting.GRAY),
                true
            );
            return construct ? 20 : 7;
        }
    }

    /** "●●●◆○○": rounds left, the constructed sixth as a diamond. */
    private static String cylinder(int rounds) {
        StringBuilder b = new StringBuilder();

        for (int i = 6; i >= 1; i--) {
            b.append(i > rounds ? "○" : (i == 1 ? "◆" : "●"));
        }

        return b.toString();
    }

    /** Hitscan: a bullet stops at the first body (the constructed one pierces three) or a wall; headshots deal 1.5x. */
    private static void revolverFire(ServerPlayer p, boolean construct, float power) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(48.0));
        BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }

        List<LivingEntity> hits = new ArrayList<>();
        List<Vec3> points = new ArrayList<>();
        for (Entity e : level.getEntities(p, p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0), ex -> Combat.valid(ex, p))) {
            Optional<Vec3> at = e.getBoundingBox().inflate(0.25).clip(eye, end);
            if (at.isPresent()) {
                int i = 0;
                double dist = at.get().distanceToSqr(eye);
                while (i < points.size() && points.get(i).distanceToSqr(eye) < dist) {
                    i++;
                }

                hits.add(i, (LivingEntity)e);
                points.add(i, at.get());
            }
        }

        int pierce = construct ? 3 : 1;
        Vec3 stop = end;

        for (int i = 0; i < Math.min(pierce, hits.size()); i++) {
            LivingEntity t = hits.get(i);
            Vec3 at = points.get(i);
            boolean head = t.getBbHeight() > 1.0F && at.y > t.getEyeY() - 0.3;
            float dmg = (construct ? 13.0F : 5.5F) * power * (head ? 1.5F : 1.0F);
            Combat.hurt(t, p, p, dmg);
            if (construct) {
                Combat.knock(t, look, 0.9, 0.25);
            }

            Vfx.play(level, VfxKind.HIT_SPARK, at, look.reverse(), head ? 1.3F : 0.8F, construct ? 13421823 : 16769184, -1, 0);
            if (head) {
                Combat.sound(p, at, ModSounds.RATIO_CRIT, 0.7F, 1.7F);
                p.displayClientMessage(Component.translatable("message.fallenroses.revolver_headshot").withStyle(ChatFormatting.RED), true);
            }

            stop = i == pierce - 1 ? at : end;
        }

        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 barrel = eye.add(look.scale(0.9)).add(right.scale(0.32)).add(0.0, -0.22, 0.0);
        Vfx.play(level, VfxKind.MUZZLE_FLASH, barrel, stop.subtract(barrel), 1.0F, 16769184, p.getId(), construct ? 1 : 0);
        Combat.sound(p, p.position(), construct ? ModSounds.WEAPON_REVOLVER_CONSTRUCT : ModSounds.WEAPON_REVOLVER, construct ? 3.0F : 2.4F, 0.95F + p.getRandom().nextFloat() * 0.1F);
        if (construct) {
            Combat.shake(level, p.position(), 0.5F, 6, 4.0);
        }
    }

    // ---------------------------------------------------------------- Black Rope

    /**
     * Black Rope: a cursed tool that unravels cursed techniques on contact. Strips buffs, breaks reinforcement
     * and Infinity; a lash also drains cursed energy, jams the target's techniques for a moment and wounds summons.
     */
    public static void unravel(ServerPlayer p, LivingEntity t, float power, boolean lash) {
        for (MobEffectInstance eff : new ArrayList<>(t.getActiveEffects())) {
            if (eff.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                t.removeEffect(eff.getEffect());
            }
        }

        if (t instanceof ServerPlayer tp) {
            PlayerData td = PlayerDataCap.get(tp);
            if (td != null) {
                td.setInfinity(false);
                td.setReinforced(false);
                if (lash) {
                    td.setEnergy(Math.max(0.0F, td.getEnergy() - 15.0F));

                    for (int i = 0; i < 5; i++) {
                        td.setCooldown(i, Math.max(td.getCooldown(i), 30));
                    }
                }
            }
        }

        if (lash && t instanceof CurseEntity ce && ce.isSummon()) {
            Combat.hurt(t, p, p, t.getMaxHealth() * 0.35F);
        }

        Vfx.play(p.serverLevel(), VfxKind.UNRAVEL, t.getBoundingBox().getCenter(), Vec3.ZERO, Math.max(1.0F, t.getBbHeight() * 0.7F), 11579647, t.getId(), 0);
        Combat.sound(p, t.position(), ModSounds.WEAPON_ROPE_UNRAVEL, lash ? 1.2F : 0.7F, 1.0F + p.getRandom().nextFloat() * 0.2F);
    }

    /** Tap: a long lash in front; it also cuts technique projectiles out of the air. */
    static void ropeLash(ServerPlayer p) {
        PlayerData d = PlayerDataCap.get(p);
        float power = d == null ? 1.0F : d.power();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 hand = p.getEyePosition().add(look.scale(0.6)).add(0.0, -0.3, 0.0);
        Vfx.play(p.serverLevel(), VfxKind.ROPE_WHIP, hand, look.scale(7.5), 1.0F, 1052696, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.WEAPON_ROPE_WHIP, 1.3F, 0.9F + p.getRandom().nextFloat() * 0.2F);

        for (LivingEntity e : Combat.cone(p, 7.5, 0.72)) {
            Combat.hurt(e, p, p, 6.0F * power);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), p);
            unravel(p, e, power, true);
        }

        cutProjectiles(p, hand, look, 7.5, 1.4);
    }

    /** Hold: the rope whirls around its holder, shredding techniques that come close and lashing whoever steps in. */
    static void ropeSpin(ServerPlayer p, ItemStack stack, int ticks) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position().add(0.0, 1.0, 0.0);
        if (ticks % 10 == 8) {
            Vfx.play(level, VfxKind.ROPE_SPIN, c, Vec3.ZERO, 1.0F, 1052696, p.getId(), 11);
        }

        if (ticks % 24 == 8) {
            Combat.sound(p, p.position(), ModSounds.WEAPON_ROPE_SPIN, 1.0F, 1.0F);
        }

        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 0, false, false));
        cutProjectiles(p, c, Vec3.ZERO, 3.4, 3.4);
        if (ticks % 10 == 0) {
            PlayerData d = PlayerDataCap.get(p);
            float power = d == null ? 1.0F : d.power();

            for (LivingEntity e : Combat.around(p, c, 3.0)) {
                Combat.hurt(e, p, p, 3.0F * power);
                Combat.knock(e, e.position().subtract(p.position()), 0.7, 0.2);
                unravel(p, e, power, false);
            }

            stack.hurtAndBreak(1, p, x -> x.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
    }

    /** Destroys technique projectiles (not Hollow Purple) within reach of a line from `from` along `dir`, or of a point. */
    private static void cutProjectiles(ServerPlayer p, Vec3 from, Vec3 dir, double length, double radius) {
        ServerLevel level = p.serverLevel();
        Vec3 to = from.add(dir.scale(length));
        AABB box = new AABB(from, to).inflate(radius);

        for (Entity e : level.getEntities(p, box, ex -> ex instanceof TechniqueProjectile || ex instanceof BlueOrbEntity || ex instanceof Projectile)) {
            if (e instanceof HollowPurpleEntity || e instanceof Projectile pr && pr.getOwner() == p) {
                continue;
            }

            Vec3 at = e.position();
            Vec3 seg = to.subtract(from);
            double len2 = seg.lengthSqr();
            double k = len2 < 1.0E-6 ? 0.0 : Mth.clamp(at.subtract(from).dot(seg) / len2, 0.0, 1.0);
            if (at.distanceTo(from.add(seg.scale(k))) <= radius) {
                Vfx.play(level, VfxKind.UNRAVEL, at, Vec3.ZERO, 0.8F, 11579647, -1, 0);
                Combat.sound(p, at, ModSounds.WEAPON_ROPE_UNRAVEL, 0.8F, 1.4F);
                e.discard();
            }
        }
    }

    // ---------------------------------------------------------------- Momo's broom

    public static final int BROOM_MAX = 240;
    private static final String BROOM = "fr_broom";

    /** Flight time left (ticks), kept on the player: a full broom flies 12 seconds, then needs rest. */
    public static int broomCharge(Player p) {
        CompoundTag t = p.getPersistentData();
        return t.contains(BROOM) ? t.getInt(BROOM) : BROOM_MAX;
    }

    static void setBroomCharge(Player p, int v) {
        p.getPersistentData().putInt(BROOM, Mth.clamp(v, 0, BROOM_MAX));
    }

    /** Take-off needs a quarter of the charge and the cursed energy to lift off. */
    static boolean broomCanStart(ServerPlayer p) {
        if (p.getAbilities().instabuild) {
            return true;
        } else if (broomCharge(p) < BROOM_MAX / 4) {
            p.displayClientMessage(Component.translatable("message.fallenroses.broom_tired").withStyle(ChatFormatting.GRAY), true);
            return false;
        } else {
            PlayerData d = PlayerDataCap.get(p);
            if (d != null && d.getPlayerClass() != null && d.getEnergy() >= 5.0F) {
                return true;
            } else {
                p.displayClientMessage(Component.translatable("message.fallenroses.broom_no_energy").withStyle(ChatFormatting.GRAY), true);
                return false;
            }
        }
    }

    /** Server, every flying tick: cursed energy, flight charge, the wind. False = the broom gives out. */
    static boolean broomServerTick(ServerPlayer p, ItemStack stack, int ticks) {
        if (p.getAbilities().instabuild) {
            return true;
        } else {
            PlayerData d = PlayerDataCap.get(p);
            int charge = broomCharge(p) - 1;
            setBroomCharge(p, charge);
            if (ticks % 40 == 0) {
                stack.hurtAndBreak(1, p, x -> x.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            }

            if (ticks % 10 == 0) {
                p.displayClientMessage(Component.translatable("message.fallenroses.broom_charge", broomBar(charge)).withStyle(ChatFormatting.AQUA), true);
            }

            if (ticks % 36 == 1) {
                Combat.sound(p, p.position(), ModSounds.WEAPON_BROOM, 0.6F, 0.9F + (float)p.getDeltaMovement().length() * 0.4F);
            }

            if (charge > 0 && d != null && d.getPlayerClass() != null && d.consume(0.25F)) {
                return true;
            } else {
                landBroom(p, charge > 0 ? "message.fallenroses.broom_no_energy" : "message.fallenroses.broom_tired");
                return false;
            }
        }
    }

    /** Ends a flight: a short glide down instead of a fall, and the broom rests a moment. */
    public static void landBroom(ServerPlayer p, String message) {
        p.stopUsingItem();
        p.getCooldowns().addCooldown(ModItems.MOMO_BROOM.get(), 30);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, false, false));
        if (message != null) {
            p.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static String broomBar(int charge) {
        int n = Math.round(10.0F * (float)charge / (float)BROOM_MAX);
        return "▮".repeat(n) + "▯".repeat(10 - n);
    }

    /**
     * Both sides, every flying tick: steer toward where the player looks with some inertia, at most ~11 blocks per
     * second, climbing at most ~6; above 22 blocks over the ground the broom refuses to climb and sinks.
     */
    static void broomPhysics(Player p) {
        Vec3 look = p.getViewVector(1.0F);
        Vec3 want = look.scale(1.05);
        double wy = Mth.clamp(want.y, -1.0, 0.36);
        if (heightAboveGround(p, 24) > 22.0) {
            wy = Math.min(wy, -0.15);
        }

        Vec3 v = p.getDeltaMovement();
        double k = 0.12;
        p.setDeltaMovement(v.x + (want.x - v.x) * k, v.y + (wy - v.y) * k + 0.08, v.z + (want.z - v.z) * k);
        p.fallDistance = 0.0F;
    }

    private static double heightAboveGround(Player p, int max) {
        Level level = p.level();
        Vec3 from = p.position();
        BlockHitResult hit = level.clip(new ClipContext(from, from.add(0.0, -(double)max - 1.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        return hit.getType() == HitResult.Type.MISS ? (double)max + 1.0 : from.y - hit.getLocation().y;
    }

    /** Off the broom: the charge comes back, faster on the ground. */
    static void broomRest(Player p) {
        int c = broomCharge(p);
        if (c < BROOM_MAX) {
            setBroomCharge(p, c + (p.onGround() ? 3 : 1));
        }
    }

    private CursedObjects() {
    }

    public static class BlindfoldItem extends ArmorItem {
        public BlindfoldItem(Properties properties) {
            super(CursedObjects.BLINDFOLD_MATERIAL, Type.HELMET, properties);
        }

        public static boolean wearing(Player p) {
            return p.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof CursedObjects.BlindfoldItem;
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (!level.isClientSide && entity instanceof ServerPlayer p && p.getItemBySlot(EquipmentSlot.HEAD) == stack) {
                if (p.tickCount % 40 == 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));

                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24.0), ex -> ex instanceof CursedSpirit)) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
                    }

                    PlayerData d = PlayerDataCap.get(p);
                    if (d != null && !d.usesStamina()) {
                        d.regen(d.getMax() * 0.01F);
                    }
                }

                return;
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.fallenroses.six_eyes_blindfold.desc").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.fallenroses.cursed_tool").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    public static class DeathPaintingItem extends Item {
        public DeathPaintingItem(Properties properties) {
            super(properties);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.EAT;
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return 40;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!(entity instanceof ServerPlayer p)) {
                return stack;
            } else {
                PlayerData d = PlayerDataCap.get(p);
                if (d != null && d.getPlayerClass() == PlayerClass.VESSEL) {
                    boolean first = d.getPaintings() == 0;
                    if (d.getPaintings() >= 9) {
                        p.displayClientMessage(Component.translatable("message.fallenroses.paintings_full").withStyle(ChatFormatting.GRAY), true);
                        return stack;
                    }

                    d.setPaintings(d.getPaintings() + 1);
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                    p.displayClientMessage(
                        Component.translatable(first ? "message.fallenroses.vessel_painting_first" : "message.fallenroses.vessel_painting", d.getPaintings())
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                        false
                    );
                    Vfx.at(p, VfxKind.BLOOD_BURST, 1.5F, 0);
                    p.level().playSound(null, p.blockPosition(), ModSounds.BLOOD_BURST.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
                } else if (d == null || d.getPlayerClass() != PlayerClass.CURSED_SPIRIT) {
                    p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                    DeathPainting.rot(p, p, 2, 160);
                    p.displayClientMessage(Component.translatable("message.fallenroses.death_painting_reject").withStyle(ChatFormatting.DARK_RED), true);
                } else if (d.getTechnique() == Technique.DEATH_PAINTING) {
                    p.displayClientMessage(Component.translatable("message.fallenroses.already_known").withStyle(ChatFormatting.GRAY), true);
                    d.regen(d.getMax() * 0.5F);
                } else {
                    d.setTechnique(Technique.DEATH_PAINTING);
                    p.displayClientMessage(Component.translatable("message.fallenroses.learned", Technique.DEATH_PAINTING.displayName()), false);
                    Vfx.at(p, VfxKind.LEVEL_UP, 0.8F, Technique.DEATH_PAINTING.color());
                    p.level().playSound(null, p.blockPosition(), ModSounds.WOMB_WAIL.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
                }

                if (!p.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                return stack;
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.fallenroses.death_painting.desc").withStyle(ChatFormatting.GRAY));
        }
    }

    public static class InventoryCurseItem extends Item {
        public InventoryCurseItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                CursedObjects.Storage storage = new CursedObjects.Storage(sp, stack, hand);
                sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.threeRows(id, inv, storage), stack.getHoverName()));
                level.playSound(null, sp.blockPosition(), ModSounds.MOUTH_AMBIENT.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.fallenroses.inventory_curse.desc").withStyle(ChatFormatting.GRAY));
            if (stack.hasTag() && stack.getTag().contains("Items")) {
                int n = stack.getTag().getList("Items", 10).size();
                tooltip.add(Component.translatable("tooltip.fallenroses.inventory_count", n).withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    public static class PrisonRealmItem extends Item {
        public PrisonRealmItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                ServerLevel sl = sp.serverLevel();
                CompoundTag tag = stack.getOrCreateTag();
                if (tag.contains("Sealed")) {
                    Vec3 at = Combat.lookPoint(sp, 4.0);
                    Entity e = EntityType.loadEntityRecursive(tag.getCompound("Sealed"), sl, x -> {
                        x.moveTo(at.x, at.y, at.z, x.getYRot(), x.getXRot());
                        return x;
                    });
                    if (e == null) {
                        tag.remove("Sealed");
                        tag.remove("SealedName");
                        return InteractionResultHolder.fail(stack);
                    } else {
                        e.setUUID(UUID.randomUUID());
                        sl.addFreshEntity(e);
                        tag.remove("Sealed");
                        tag.remove("SealedName");
                        Vfx.play(sl, VfxKind.PRISON_REALM, at, Vec3.ZERO, Math.max(1.0F, e.getBbHeight()), 15261904, sp.getId(), 1);
                        sl.playSound(null, at.x, at.y, at.z, ModSounds.COFFIN_BELL.get(), SoundSource.PLAYERS, 1.5F, 1.2F);
                        player.displayClientMessage(
                            Component.translatable("message.fallenroses.prison_release", e.getDisplayName()).withStyle(ChatFormatting.GRAY), true
                        );
                        player.getCooldowns().addCooldown(this, 60);
                        return InteractionResultHolder.consume(stack);
                    }
                } else {
                    LivingEntity t = Combat.lookTarget(sp, 10.0);
                    if (t != null && !(t instanceof Player)) {
                        if (!(t.getMaxHealth() > 300.0F) && t.canChangeDimensions()) {
                            CompoundTag saved = new CompoundTag();
                            if (!t.saveAsPassenger(saved)) {
                                return InteractionResultHolder.fail(stack);
                            } else {
                                tag.put("Sealed", saved);
                                tag.putString("SealedName", Serializer.toJson(t.getDisplayName()));
                                Vfx.play(
                                    sl,
                                    VfxKind.PRISON_REALM,
                                    t.getBoundingBox().getCenter(),
                                    Vec3.ZERO,
                                    Math.max(1.0F, t.getBbHeight()),
                                    6957616,
                                    sp.getId(),
                                    0
                                );
                                sl.playSound(null, t.getX(), t.getY(), t.getZ(), ModSounds.COFFIN_BELL.get(), SoundSource.PLAYERS, 1.8F, 0.6F);
                                sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 0.5F);

                                for (Entity passenger : t.getPassengers()) {
                                    passenger.stopRiding();
                                }

                                t.discard();
                                player.displayClientMessage(
                                    Component.translatable("message.fallenroses.prison_sealed", t.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE), true
                                );
                                player.getCooldowns().addCooldown(this, 200);
                                FRAdvancements.award(sp, "story/prison_realm");
                                return InteractionResultHolder.consume(stack);
                            }
                        } else {
                            player.displayClientMessage(Component.translatable("message.fallenroses.prison_too_strong").withStyle(ChatFormatting.RED), true);
                            return InteractionResultHolder.fail(stack);
                        }
                    } else {
                        player.displayClientMessage(Component.translatable("message.fallenroses.no_target").withStyle(ChatFormatting.GRAY), true);
                        return InteractionResultHolder.fail(stack);
                    }
                }
            } else {
                return InteractionResultHolder.success(stack);
            }
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return stack.hasTag() && stack.getTag().contains("Sealed");
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.fallenroses.prison_realm.desc").withStyle(ChatFormatting.GRAY));
            if (stack.hasTag() && stack.getTag().contains("SealedName")) {
                Component name = Serializer.fromJson(stack.getTag().getString("SealedName"));
                if (name != null) {
                    tooltip.add(Component.translatable("tooltip.fallenroses.sealed", name).withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
        }
    }

    static final class Storage extends SimpleContainer {
        private final ServerPlayer owner;
        private final ItemStack stack;
        private final InteractionHand hand;
        private boolean loading;

        Storage(ServerPlayer owner, ItemStack stack, InteractionHand hand) {
            super(27);
            this.owner = owner;
            this.stack = stack;
            this.hand = hand;
            this.loading = true;
            if (stack.hasTag()) {
                NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(stack.getTag(), items);

                for (int i = 0; i < 27; i++) {
                    this.setItem(i, items.get(i));
                }
            }

            this.loading = false;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack item) {
            return !(item.getItem() instanceof CursedObjects.InventoryCurseItem);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!this.loading) {
                NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

                for (int i = 0; i < 27; i++) {
                    ItemStack s = this.getItem(i);
                    if (s.getItem() instanceof CursedObjects.InventoryCurseItem) {
                        this.removeItemNoUpdate(i);
                        if (!this.owner.getInventory().add(s)) {
                            this.owner.drop(s, false);
                        }
                    } else {
                        items.set(i, s);
                    }
                }

                ContainerHelper.saveAllItems(this.stack.getOrCreateTag(), items);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return player == this.owner && player.getItemInHand(this.hand) == this.stack;
        }
    }
}
