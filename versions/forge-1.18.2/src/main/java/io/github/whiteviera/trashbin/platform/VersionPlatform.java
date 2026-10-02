package io.github.whiteviera.trashbin.platform;

import io.github.whiteviera.trashbin.TrashBinMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

public final class VersionPlatform {
    private static final CreativeModeTab TAB = new CreativeModeTab("trashbin") {
        @Override public ItemStack makeIcon() { return new ItemStack(TrashBinMod.BIN_ITEM.get()); }
    };
    public static void registerCreativeTab(IEventBus bus) {}
    public static IForgeRegistry<BlockEntityType<?>> blockEntities() { return ForgeRegistries.BLOCK_ENTITIES; }
    public static IForgeRegistry<MenuType<?>> menus() { return ForgeRegistries.CONTAINERS; }
    public static void openMenu(ServerPlayer player, MenuProvider menu, BlockPos pos) { NetworkHooks.openGui(player, menu, pos); }
    public static Item.Properties itemProperties() { return new Item.Properties().tab(TAB); }
    public static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of(Material.METAL).strength(2.5F, 6.0F).sound(SoundType.LANTERN).noOcclusion();
    }
    public static Component literal(String text) { return new TextComponent(text); }
    public static Component translatable(String key, Object... args) { return new TranslatableComponent(key, args); }
    private VersionPlatform() {}
}
