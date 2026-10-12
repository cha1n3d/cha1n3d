package com.fallenroses.item;

import com.fallenroses.client.render.WeaponRenderer;
import com.fallenroses.event.CombatEvents;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.registry.ModItems;
import com.fallenroses.registry.ModParticles;
import com.fallenroses.technique.Combat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeTier;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CursedToolItem extends SwordItem implements GeoItem {
    public static final Tier CURSED_TIER = new ForgeTier(
        4, 2031, 9.0F, 4.0F, 15, BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(ModItems.CURSED_ESSENCE.get())
    );
    public static final Tier GRADE4_TIER = new ForgeTier(3, 900, 7.0F, 2.5F, 14, BlockTags.NEEDS_IRON_TOOL, () -> Ingredient.of(ModItems.CURSED_ESSENCE.get()));
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final CursedToolItem.Kind kind;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CursedToolItem(CursedToolItem.Kind kind, int attackDamage, float attackSpeed, Properties properties) {
        super(kind == CursedToolItem.Kind.SLAUGHTER_DEMON ? GRADE4_TIER : CURSED_TIER, attackDamage, attackSpeed, properties);
        this.kind = kind;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public CursedToolItem.Kind kind() {
        return this.kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.kind == CursedToolItem.Kind.MOMO_BROOM) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        } else if (this.kind != CursedToolItem.Kind.MIWA_KATANA && this.kind != CursedToolItem.Kind.MAI_REVOLVER) {
            if (this.kind != CursedToolItem.Kind.CHAIN_OF_THOUSAND_MILES && this.kind != CursedToolItem.Kind.BLACK_ROPE) {
                if (this.kind != CursedToolItem.Kind.KAMUTOKE) {
                    return super.use(level, player, hand);
                } else {
                    if (!level.isClientSide && player instanceof ServerPlayer sp) {
                        LivingEntity target = Combat.lookTarget(sp, 24.0);
                        Vec3 at = target != null ? target.position() : Combat.lookPoint(sp, 24.0);
                        CombatEvents.lightning(sp, at, target, 9.0F);
                        player.getCooldowns().addCooldown(this, 120);
                        stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
                    }

                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }
            } else {
                if (!level.isClientSide && player instanceof ServerPlayer sp && CursedObjects.toolSkill(sp, this.kind)) {
                    player.getCooldowns().addCooldown(this, switch (this.kind) {
                        case CHAIN_OF_THOUSAND_MILES -> 60;
                        case BLACK_ROPE -> 100;
                        default -> 600;
                    });
                    stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
                }

                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        } else {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                int cd = this.kind == CursedToolItem.Kind.MIWA_KATANA ? CursedObjects.iaiStance(sp) : CursedObjects.revolverShot(sp, stack);
                if (cd > 0) {
                    player.getCooldowns().addCooldown(this, cd);
                }
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        switch (this.kind) {
            case PLAYFUL_CLOUD:
                target.knockback(
                    0.6, (double)Mth.sin(attacker.getYRot() * (float) (Math.PI / 180.0)), (double)(-Mth.cos(attacker.getYRot() * (float) (Math.PI / 180.0)))
                );
                break;
            case INVERTED_SPEAR:
                for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
                    if (effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                        target.removeEffect(effect.getEffect());
                    }
                }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (this.kind == CursedToolItem.Kind.EXECUTIONER_SWORD && !level.isClientSide && executionerExpired(stack, level, entity)) {
            stack.setCount(0);
            if (entity instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.fallenroses.sword_vanished").withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            if (this.kind == CursedToolItem.Kind.MIWA_KATANA && !level.isClientSide && selected && entity instanceof ServerPlayer sp) {
                CursedObjects.iaiTick(sp);
            }

            if (level.isClientSide && selected && this.kind != CursedToolItem.Kind.PLAYFUL_CLOUD && level.random.nextInt(4) == 0) {
                Vec3 look = entity.getViewVector(1.0F);
                Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
                Vec3 p = entity.getEyePosition().add(look.scale(0.7)).add(side.normalize().scale(0.45)).add(0.0, -0.5 + level.random.nextDouble() * 0.4, 0.0);

                int color = switch (this.kind) {
                    case CHAIN_OF_THOUSAND_MILES -> 13158608;
                    case BLACK_ROPE -> 6974096;
                    default -> 13643836;
                    case INVERTED_SPEAR -> 13158655;
                    case SPLIT_SOUL -> 10124543;
                    case DRAGON_BONE -> stack.getOrCreateTag().getInt("Charge") >= 4 ? 16777215 : 9099519;
                    case KAMUTOKE -> 16773242;
                    case NANAMI_CLEAVER -> 15253600;
                    case EXECUTIONER_SWORD -> 15790304;
                    case MIWA_KATANA -> 11065599;
                    case MAI_REVOLVER -> 16769184;
                    case MOMO_BROOM -> 13168848;
                };
                level.addParticle(ModParticles.glow(color, 0.35F, 14), p.x, p.y, p.z, 0.0, 0.01, 0.0);
            }
        }
    }

    private static boolean executionerExpired(ItemStack stack, Level level, Entity holder) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Expire") || tag.getLong("Expire") <= level.getGameTime()) {
            return true;
        } else if (!(holder instanceof ServerPlayer sp)) {
            return true;
        } else {
            PlayerData d = PlayerDataCap.get(sp);
            return d == null || d.getPlayerClass() != PlayerClass.JUDGE;
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return this.kind == CursedToolItem.Kind.MOMO_BROOM ? UseAnim.NONE : super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return this.kind == CursedToolItem.Kind.MOMO_BROOM ? 72000 : super.getUseDuration(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (this.kind == CursedToolItem.Kind.MOMO_BROOM) {
            if (user instanceof Player p) {
                if (!level.isClientSide && p instanceof ServerPlayer sp && !CursedObjects.broomFuel(sp)) {
                    sp.stopUsingItem();
                    return;
                }

                Vec3 look = p.getViewVector(1.0F);
                Vec3 v = p.getDeltaMovement().scale(0.6).add(look.scale(0.45)).add(0.0, 0.06, 0.0);
                p.setDeltaMovement(v);
                p.fallDistance = 0.0F;
                if (level.isClientSide && level.random.nextInt(2) == 0) {
                    level.addParticle(ModParticles.glow(13168848, 0.4F, 12), p.getX(), p.getY() + 0.3, p.getZ(), -v.x * 0.2, -0.02, -v.z * 0.2);
                }
            }
        }
    }

    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (this.kind == CursedToolItem.Kind.EXECUTIONER_SWORD && !entity.level().isClientSide) {
            entity.discard();
            return true;
        } else {
            return super.onEntityItemUpdate(stack, entity);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (this.kind == CursedToolItem.Kind.EXECUTIONER_SWORD && stack.hasTag() && stack.getTag().contains("CondemnedName")) {
            Component who = Serializer.fromJson(stack.getTag().getString("CondemnedName"));
            if (who != null) {
                tooltip.add(Component.translatable("tooltip.fallenroses.condemned", who).withStyle(ChatFormatting.DARK_RED));
            }

            if (level != null) {
                long left = Math.max(0L, stack.getTag().getLong("Expire") - level.getGameTime()) / 20L;
                tooltip.add(Component.translatable("tooltip.fallenroses.sword_left", left).withStyle(ChatFormatting.GRAY));
            }
        }

        if (this.kind == CursedToolItem.Kind.DRAGON_BONE) {
            tooltip.add(Component.translatable("tooltip.fallenroses.dragon_charge", stack.getOrCreateTag().getInt("Charge"), 5).withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(Component.translatable("tooltip.fallenroses.cursed_tool").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = WeaponRenderer.create(CursedToolItem.this.kind.id());
                }

                return this.renderer;
            }
        });
    }

    public static enum Kind {
        PLAYFUL_CLOUD("playful_cloud"),
        INVERTED_SPEAR("inverted_spear_of_heaven"),
        SPLIT_SOUL("split_soul_katana"),
        SLAUGHTER_DEMON("slaughter_demon"),
        DRAGON_BONE("dragon_bone"),
        KAMUTOKE("kamutoke"),
        NANAMI_CLEAVER("nanami_cleaver"),
        CHAIN_OF_THOUSAND_MILES("chain_of_thousand_miles"),
        BLACK_ROPE("black_rope"),
        EXECUTIONER_SWORD("executioner_sword"),
        MIWA_KATANA("miwa_katana"),
        MAI_REVOLVER("mai_revolver"),
        MOMO_BROOM("momo_broom");

        private final String id;

        private Kind(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }
    }
}
