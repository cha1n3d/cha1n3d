package com.fallenroses.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public class AddItemModifier extends LootModifier {
    public static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(
        () -> RecordCodecBuilder.create(
                inst -> codecStart(inst)
                        .and(
                            inst.group(
                                ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item),
                                Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance),
                                Codec.STRING.optionalFieldOf("path_prefix", "chests/").forGetter(m -> m.pathPrefix)
                            )
                        )
                        .apply(inst, AddItemModifier::new)
            )
    );
    private final Item item;
    private final float chance;
    private final String pathPrefix;

    public AddItemModifier(LootItemCondition[] conditions, Item item, float chance, String pathPrefix) {
        super(conditions);
        this.item = item;
        this.chance = chance;
        this.pathPrefix = pathPrefix;
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (table != null && table.getPath().startsWith(this.pathPrefix) && context.getRandom().nextFloat() < this.chance) {
            loot.add(new ItemStack(this.item));
        }

        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
