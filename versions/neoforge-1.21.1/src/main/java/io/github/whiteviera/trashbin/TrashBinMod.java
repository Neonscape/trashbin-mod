package io.github.whiteviera.trashbin;

import io.github.whiteviera.trashbin.block.TrashBinBlock;
import io.github.whiteviera.trashbin.block.TrashBinBlockEntity;
import io.github.whiteviera.trashbin.menu.TrashBinMenu;
import io.github.whiteviera.trashbin.platform.VersionPlatform;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TrashBinMod.ID)
public final class TrashBinMod {
    public static final String ID = "trashbin";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredHolder<Block, TrashBinBlock> BIN = BLOCKS.register("trash_bin", TrashBinBlock::new);
    public static final DeferredHolder<Item, BlockItem> BIN_ITEM = ITEMS.register("trash_bin",
            () -> new BlockItem(BIN.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrashBinBlockEntity>> BIN_ENTITY = BLOCK_ENTITIES.register("trash_bin",
            () -> BlockEntityType.Builder.of(TrashBinBlockEntity::new, BIN.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<TrashBinMenu>> BIN_MENU = MENUS.register("trash_bin",
            () -> IMenuTypeExtension.create(TrashBinMenu::fromNetwork));

    public TrashBinMod(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus); MENUS.register(bus);
        VersionPlatform.registerCreativeTab(bus);
        bus.addListener(TrashBinMod::registerCapabilities);
        container.registerConfig(ModConfig.Type.SERVER, TrashBinConfig.SPEC);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Stable wrappers check redstone/removal on every operation, including cached callers.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BIN_ENTITY.get(),
                (bin, side) -> bin.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BIN_ENTITY.get(),
                (bin, side) -> bin.automationFluids());
    }
}
