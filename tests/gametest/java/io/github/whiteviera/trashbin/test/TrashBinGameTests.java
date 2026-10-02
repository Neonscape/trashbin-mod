package io.github.whiteviera.trashbin.test;

import io.github.whiteviera.trashbin.TrashBinConfig;
import io.github.whiteviera.trashbin.TrashBinMod;
import io.github.whiteviera.trashbin.block.TrashBinBlock;
import io.github.whiteviera.trashbin.block.TrashBinBlockEntity;
import io.github.whiteviera.trashbin.compat.ExperienceFluidCompat;
import io.github.whiteviera.trashbin.menu.TrashBinMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

@GameTestHolder(TrashBinMod.ID)
@PrefixGameTestTemplate(false)
public final class TrashBinGameTests {
    private static final Capability<IItemHandler> ITEM = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<IFluidHandler> FLUID = CapabilityManager.get(new CapabilityToken<>() {});
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static TrashBinBlockEntity bin(GameTestHelper h) {
        h.setBlock(POS, TrashBinMod.BIN.get());
        return (TrashBinBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS));
    }
    private static IItemHandler items(TrashBinBlockEntity b) { return b.getCapability(ITEM).orElseThrow(IllegalStateException::new); }
    private static IFluidHandler fluids(TrashBinBlockEntity b) { return b.getCapability(FLUID).orElseThrow(IllegalStateException::new); }
    private static Player player(GameTestHelper h) {
        Player p = h.makeMockPlayer(); BlockPos pos = h.absolutePos(POS); p.setPos(pos.getX(), pos.getY(), pos.getZ()); return p;
    }
    private static AABB bounds(GameTestHelper h) { return new AABB(h.absolutePos(POS)).inflate(2); }
    private static long xp(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ExperienceOrb.class, bounds(h)).stream().mapToLong(ExperienceOrb::getValue).sum();
    }
    private static void check(GameTestHelper h, boolean condition, String message) { if (!condition) h.fail(message); }
    private static void checkFalse(GameTestHelper h, boolean condition, String message) { check(h, !condition, message); }

    @GameTest(template = "empty", batch = "trashbin")
    public static void manualFullInventoryNeverEvicts(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h);
        for (int i = 0; i < 27; i++) b.inventory().setStackInSlot(i, new ItemStack(Items.STONE, 64));
        check(h, b.inventory().insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "manual full bin must reject input");
        check(h, b.experience().points() == 0 && b.occupiedSlots() == 27, "manual insertion must not destroy items"); h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void simulatedOverflowIsSafeAndOldestGroupIsRecycled(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h);
        b.inventory().setStackInSlot(12, new ItemStack(Items.STONE, 64));
        for (int i = 0; i < 27; i++) if (i != 12) b.inventory().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        IItemHandler handler = items(b);
        for (int i = 0; i < 10; i++)
            check(h, ItemHandlerHelper.insertItem(handler, new ItemStack(Items.DIRT, 5), true).isEmpty(), "simulate must predict acceptance");
        check(h, b.inventory().getStackInSlot(12).is(Items.STONE) && b.experience().points() == 0, "simulation changed the bin");
        check(h, ItemHandlerHelper.insertItem(handler, new ItemStack(Items.DIRT, 5), false).isEmpty(), "full automation rejected input");
        check(h, b.inventory().getStackInSlot(12).is(Items.DIRT), "FIFO did not evict first inserted group");
        check(h, b.experience().points() == 64 / TrashBinConfig.ITEMS_PER_XP.get(), "wrong recycled XP");
        check(h, b.inventory().getStackInSlot(0).getCount() == 64 && b.occupiedSlots() == 27, "newer groups were lost"); h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void partialStackMergesWithoutEvicting(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h);
        for (int i = 0; i < 27; i++) b.inventory().setStackInSlot(i, new ItemStack(Items.STONE, 64));
        b.inventory().setStackInSlot(26, new ItemStack(Items.DIRT, 16));
        ItemStack rest = ItemHandlerHelper.insertItem(items(b), new ItemStack(Items.DIRT, 32), false);
        check(h, rest.isEmpty() && b.inventory().getStackInSlot(26).getCount() == 48, "compatible partial stack not merged");
        check(h, b.inventory().getStackInSlot(0).getCount() == 64 && b.experience().points() == 0, "merge evicted an unrelated group"); h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin", timeoutTicks = 80)
    public static void vanillaHopperInputsIntoFullBin(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h);
        b.inventory().setStackInSlot(7, new ItemStack(Items.STONE, 64));
        for (int i = 0; i < 27; i++) if (i != 7) b.inventory().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        h.setBlock(POS.above(), Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS.above()));
        hopper.setItem(0, new ItemStack(Items.DIRT));
        h.runAfterDelay(16, () -> {
            check(h, hopper.getItem(0).isEmpty() && b.inventory().getStackInSlot(7).is(Items.DIRT), "vanilla hopper did not insert into full bin");
            check(h, b.experience().points() == 64 / TrashBinConfig.ITEMS_PER_XP.get(), "hopper insertion recycled wrong XP"); h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void automaticOverflowDoesNotSpawnOrbs(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h); int capacity = TrashBinConfig.XP_CAPACITY.get();
        b.experience().restore((long) capacity * 20, 0, 0);
        for (int i = 0; i < 27; i++) b.inventory().setStackInSlot(i, new ItemStack(Items.STONE, 64));
        ItemHandlerHelper.insertItem(items(b), new ItemStack(Items.DIRT), false);
        fluids(b).fill(new FluidStack(Fluids.WATER, TrashBinConfig.FLUID_MB_PER_XP.get()), IFluidHandler.FluidAction.EXECUTE);
        check(h, b.experience().points() == capacity && xp(h) == 0, "automatic overflow must be discarded without orbs"); h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void fluidInputSimulationAndRemainders(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h); IFluidHandler f = fluids(b); int rate = TrashBinConfig.FLUID_MB_PER_XP.get();
        check(h, f.fill(new FluidStack(Fluids.WATER, rate), IFluidHandler.FluidAction.SIMULATE) == rate, "fluid simulation rejected disposal");
        check(h, b.experience().units() == 0 && b.experience().fluidRemainder() == 0, "fluid simulation changed cache");
        int half = rate / 2;
        f.fill(new FluidStack(Fluids.WATER, half), IFluidHandler.FluidAction.EXECUTE);
        f.fill(new FluidStack(Fluids.LAVA, rate - half), IFluidHandler.FluidAction.EXECUTE);
        check(h, b.experience().points() == 1 && b.experience().fluidRemainder() == 0, "fluid progress failed to accumulate");
        if (ExperienceFluidCompat.preferred().id() == 0)
            check(h, f.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "output exists without compatible mod");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void optionalFluidOutputPriorityPrecisionAndGating(GameTestHelper h) {
        String fixture = System.getProperty("trashbin.test.compat", "none");
        if (fixture.equals("none")) { h.succeed(); return; }
        TrashBinBlockEntity b = bin(h); IFluidHandler f = fluids(b);
        ExperienceFluidCompat.Output output = ExperienceFluidCompat.preferred();
        check(h, output.id() == (fixture.equals("sophisticated") ? 2 : 1), "wrong optional fluid priority");
        b.experience().restore(160, 0, 0);
        check(h, f.getTanks() == 2 && f.getFluidInTank(1).getAmount() == 8 * output.millibucketsPerPoint(), "wrong virtual output amount");
        check(h, f.drain(1, IFluidHandler.FluidAction.SIMULATE).getAmount() == 1 && b.experience().units() == 160,
                "output simulation modified cache");
        check(h, f.drain(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "wrong fluid request drained XP");
        check(h, f.drain(new FluidStack(output.fluid(), 1), IFluidHandler.FluidAction.EXECUTE).getAmount() == 1,
                "matching fluid request failed");
        check(h, b.experience().units() == 160 - 20 / output.millibucketsPerPoint(), "single mB drain rounded XP");
        b.cycleMode();
        check(h, f.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "disabled redstone allowed fluid output");
        h.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
        f.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        check(h, b.experience().units() == 0, "output drain lost fractional XP or left unexpected cache");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void redstoneGatesHeldCapabilitiesButManualAccessWorks(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h); IItemHandler handler = items(b); IFluidHandler f = fluids(b);
        b.cycleMode(); // powered-only; currently unpowered
        checkFalse(h, b.automationEnabled(), "powered mode enabled without signal");
        check(h, ItemHandlerHelper.insertItem(handler, new ItemStack(Items.STONE), false).getCount() == 1, "stale item capability bypassed gate");
        check(h, f.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "stale fluid capability bypassed gate");
        b.inventory().setStackInSlot(0, new ItemStack(Items.DIRT, 2));
        check(h, handler.extractItem(0, 1, false).isEmpty(), "disabled automatic extraction succeeded");
        check(h, b.inventory().extractItem(0, 1, false).getCount() == 1, "manual access was disabled");
        h.setBlock(POS.east(), Blocks.REDSTONE_BLOCK); b.updateActiveState();
        check(h, b.automationEnabled() && b.getBlockState().getValue(TrashBinBlock.ACTIVE), "powered mode or visual state did not activate");
        check(h, handler.extractItem(0, 1, false).getCount() == 1, "held capability did not recover");
        b.cycleMode(); checkFalse(h, b.automationEnabled(), "unpowered mode allowed powered automation");
        h.setBlock(POS.east(), Blocks.AIR); b.updateActiveState();
        check(h, b.automationEnabled(), "unpowered mode did not recover"); h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void persistenceKeepsInventoryAgeExperienceAndMode(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h);
        b.inventory().setStackInSlot(23, new ItemStack(Items.STONE, 64));
        for (int i = 0; i < 27; i++) if (i != 23) b.inventory().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        b.experience().restore(201, 31, 12345); b.cycleMode(); b.cycleMode();
        CompoundTag saved = b.saveWithoutMetadata();
        b.load(saved);
        check(h, b.experience().units() == 201 && b.experience().itemRemainder() == 31 && b.experience().fluidRemainder() == 12345,
                "reload changed XP or fractional progress");
        check(h, b.mode().ordinal() == 2, "reload lost redstone mode");
        ItemHandlerHelper.insertItem(items(b), new ItemStack(Items.DIRT), false);
        check(h, b.inventory().getStackInSlot(23).is(Items.DIRT), "reload lost FIFO age"); h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void menuShowsLargeXpAndRejectsInvalidActions(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h); b.experience().restore(999999L * 20, 0, 0);
        Player p = player(h); TrashBinMenu menu = new TrashBinMenu(1, p.getInventory(), b); p.containerMenu = menu;
        check(h, menu.experiencePoints() == 999999, "large menu XP was truncated to 16 bits");
        checkFalse(h, menu.clickMenuButton(p, 99), "unknown menu action accepted");
        p.setPos(p.getX() + 20, p.getY(), p.getZ());
        checkFalse(h, menu.clickMenuButton(p, TrashBinMenu.CLEAR), "remote menu action accepted");
        menu.removed(p); h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void manualOverflowAndExtractionConserveExperience(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h); int capacity = TrashBinConfig.XP_CAPACITY.get();
        b.experience().restore((long) capacity * 20, 0, 0);
        b.inventory().setStackInSlot(0, new ItemStack(Items.STONE, 64));
        Player p = player(h); b.clearByPlayer(p);
        long recycled = 64 / TrashBinConfig.ITEMS_PER_XP.get();
        check(h, b.occupiedSlots() == 0 && xp(h) == recycled, "manual overflow not emitted at player");
        b.extractByPlayer(p);
        check(h, b.experience().points() == 0 && xp(h) == (long) capacity + recycled, "extraction lost or duplicated XP");
        b.extractByPlayer(p); check(h, xp(h) == (long) capacity + recycled, "repeated extraction duplicated XP"); h.succeed();
    }

    @GameTest(template = "empty", batch = "trashbin")
    public static void breakingDropsContentsAndXpExactlyOnce(GameTestHelper h) {
        TrashBinBlockEntity b = bin(h); b.inventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
        b.experience().restore(840, 32, 1000);
        h.getLevel().destroyBlock(h.absolutePos(POS), true);
        long diamonds = h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds(h)).stream()
                .filter(e -> e.getItem().is(Items.DIAMOND)).mapToLong(e -> e.getItem().getCount()).sum();
        long bins = h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds(h)).stream()
                .filter(e -> e.getItem().is(TrashBinMod.BIN_ITEM.get())).mapToLong(e -> e.getItem().getCount()).sum();
        check(h, diamonds == 7 && bins == 1 && xp(h) == 42, "break lost item, bin drop, or XP");
        b.dropContents(); check(h, xp(h) == 42, "double removal duplicated XP"); h.succeed();
    }
}
