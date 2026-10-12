package com.fallenroses.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "fallenroses");
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.fallenroses"))
                .icon(() -> new ItemStack(ModItems.SUKUNA_FINGER.get()))
                .displayItems((params, out) -> ModItems.ITEMS.getEntries().forEach(item -> out.accept(item.get())))
                .build()
    );

    private ModCreativeTabs() {
    }
}
