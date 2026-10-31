package io.github.whiteviera.trashbin.platform;

import io.github.whiteviera.trashbin.TrashBinMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VersionPlatform {
    public static void openMenu(ServerPlayer player, MenuProvider menu, BlockPos pos) { player.openMenu(menu, pos); }
    public static void registerCreativeTab(IEventBus bus) {
        DeferredRegister<CreativeModeTab> tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrashBinMod.ID);
        tabs.register("trashbin", () -> CreativeModeTab.builder().title(translatable("itemGroup.trashbin"))
                .icon(() -> new ItemStack(TrashBinMod.BIN_ITEM.get()))
                .displayItems((parameters, output) -> output.accept(TrashBinMod.BIN_ITEM.get())).build());
        tabs.register(bus);
    }
    public static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F, 6.0F).sound(SoundType.LANTERN).noOcclusion();
    }
    public static Component translatable(String key, Object... args) { return Component.translatable(key, args); }
    private VersionPlatform() {}
}
