package io.github.whiteviera.trashbin;

import io.github.whiteviera.trashbin.block.TrashBinBlock;
import io.github.whiteviera.trashbin.block.TrashBinBlockEntity;
import io.github.whiteviera.trashbin.menu.TrashBinMenu;
import io.github.whiteviera.trashbin.platform.VersionPlatform;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TrashBinMod.ID)
public final class TrashBinMod {
    public static final String ID = "trashbin";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(VersionPlatform.blockEntities(), ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(VersionPlatform.menus(), ID);
    public static final RegistryObject<Block> BIN = BLOCKS.register("trash_bin", TrashBinBlock::new);
    public static final RegistryObject<Item> BIN_ITEM = ITEMS.register("trash_bin",
            () -> new BlockItem(BIN.get(), VersionPlatform.itemProperties()));
    public static final RegistryObject<BlockEntityType<TrashBinBlockEntity>> BIN_ENTITY = BLOCK_ENTITIES.register("trash_bin",
            () -> BlockEntityType.Builder.of(TrashBinBlockEntity::new, BIN.get()).build(null));
    public static final RegistryObject<MenuType<TrashBinMenu>> BIN_MENU = MENUS.register("trash_bin",
            () -> IForgeMenuType.create(TrashBinMenu::fromNetwork));

    public TrashBinMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus); MENUS.register(bus);
        VersionPlatform.registerCreativeTab(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TrashBinConfig.SPEC);
    }
}
