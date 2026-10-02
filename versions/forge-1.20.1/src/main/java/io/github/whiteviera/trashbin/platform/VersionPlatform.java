package io.github.whiteviera.trashbin.platform;

import io.github.whiteviera.trashbin.TrashBinMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

public final class VersionPlatform {
    public static IForgeRegistry<BlockEntityType<?>> blockEntities() { return ForgeRegistries.BLOCK_ENTITY_TYPES; }
    public static IForgeRegistry<MenuType<?>> menus() { return ForgeRegistries.MENU_TYPES; }
    public static void openMenu(ServerPlayer player, MenuProvider menu, BlockPos pos) { NetworkHooks.openScreen(player, menu, pos); }
    public static void registerCreativeTab(IEventBus bus) {
        DeferredRegister<CreativeModeTab> tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrashBinMod.ID);
        tabs.register("trashbin", () -> CreativeModeTab.builder().title(translatable("itemGroup.trashbin"))
                .icon(() -> new ItemStack(TrashBinMod.BIN_ITEM.get()))
                .displayItems((parameters, output) -> output.accept(TrashBinMod.BIN_ITEM.get())).build());
        tabs.register(bus);
    }
    public static Item.Properties itemProperties() { return new Item.Properties(); }
    public static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F, 6.0F).sound(SoundType.LANTERN).noOcclusion();
    }
    public static Component literal(String text) { return Component.literal(text); }
    public static Component translatable(String key, Object... args) { return Component.translatable(key, args); }
    private VersionPlatform() {}
}
