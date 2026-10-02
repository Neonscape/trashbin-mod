package io.github.whiteviera.trashbin.block;

import io.github.whiteviera.trashbin.TrashBinConfig;
import io.github.whiteviera.trashbin.TrashBinMod;
import io.github.whiteviera.trashbin.compat.ExperienceFluidCompat;
import io.github.whiteviera.trashbin.core.ArrivalOrder;
import io.github.whiteviera.trashbin.core.ExperienceLedger;
import io.github.whiteviera.trashbin.core.RedstoneMode;
import io.github.whiteviera.trashbin.menu.TrashBinMenu;
import io.github.whiteviera.trashbin.platform.VersionPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public final class TrashBinBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 27;
    private static final Capability<IItemHandler> ITEM_CAP = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<IFluidHandler> FLUID_CAP = CapabilityManager.get(new CapabilityToken<>() {});
    private final ArrivalOrder arrivals = new ArrivalOrder(SLOT_COUNT);
    private final ExperienceLedger experience = new ExperienceLedger();
    private final ItemStack[] previous = new ItemStack[SLOT_COUNT];
    private RedstoneMode mode = RedstoneMode.IGNORE;
    private boolean loading;
    private boolean dropped;
    private int openCount;

    /** Player menus access this directly. Redstone and automatic eviction do not apply here. */
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) {
            contentsChanged(slot);
        }
    };
    private final IItemHandler automation = new AutomationItems();
    private final IFluidHandler fluidHandler = new RecyclingFluidHandler();
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> automation);
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidHandler);

    public TrashBinBlockEntity(BlockPos pos, BlockState state) { super(TrashBinMod.BIN_ENTITY.get(), pos, state); }
    public ItemStackHandler inventory() { return inventory; }
    /** SlotItemHandler permits in-place stack changes, so menu slots must notify the owner too. */
    public void contentsChanged(int slot) {
        if (loading) return;
        ItemStack current = inventory.getStackInSlot(slot);
        ItemStack old = previous[slot];
        if (current.isEmpty()) arrivals.emptied(slot);
        else {
            if (old != null && !old.isEmpty() && !ItemStack.isSameItemSameTags(old, current)) arrivals.emptied(slot);
            arrivals.inserted(slot, level == null ? 0 : level.getGameTime());
        }
        previous[slot] = current.copy();
        changedInventory();
    }
    public ExperienceLedger experience() { return experience; }
    public RedstoneMode mode() { return mode; }
    public boolean automationEnabled() {
        return level != null && !level.isClientSide && !isRemoved() && mode.allows(level.hasNeighborSignal(worldPosition));
    }
    public void cycleMode() { mode = mode.next(); setChanged(); updateActiveState(); }
    public void updateActiveState() {
        if (level == null || level.isClientSide || isRemoved()) return;
        BlockState state = getBlockState();
        if (state.getBlock() instanceof TrashBinBlock && state.getValue(TrashBinBlock.ACTIVE) != automationEnabled()) {
            level.setBlock(worldPosition, state.setValue(TrashBinBlock.ACTIVE, automationEnabled()), 3);
        }
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, TrashBinBlockEntity bin) {
        if (level.getGameTime() % 20 != 0) return;
        bin.updateActiveState();
        long before = bin.experience.units();
        bin.experience.addPoints(0, TrashBinConfig.XP_CAPACITY.get()); // safe after a configuration reload
        if (before != bin.experience.units()) bin.setChanged();
        // Close stale lids after disconnects/reloads without continuously scanning the world.
        int viewers = 0;
        for (Player player : level.players())
            if (!player.isSpectator() && player.containerMenu instanceof TrashBinMenu menu && menu.bin() == bin) viewers++;
        if (viewers != bin.openCount || state.getValue(TrashBinBlock.OPEN) != (viewers > 0)) {
            bin.openCount = viewers; bin.updateLid();
        }
    }
    public void startOpen(Player player) {
        if (!player.isSpectator()) { openCount++; if (openCount == 1) updateLid(); }
    }
    public void stopOpen(Player player) {
        if (!player.isSpectator()) { openCount = Math.max(0, openCount - 1); if (openCount == 0) updateLid(); }
    }
    private void updateLid() {
        if (level == null || level.isClientSide || isRemoved()) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof TrashBinBlock)) return;
        boolean open = openCount > 0;
        if (state.getValue(TrashBinBlock.OPEN) == open) return;
        level.setBlock(worldPosition, state.setValue(TrashBinBlock.OPEN, open), 3);
        level.playSound(null, worldPosition, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE,
                SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
    }
    public void clearByPlayer(Player player) {
        if (!(level instanceof ServerLevel)) return;
        long count = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            count += inventory.getStackInSlot(slot).getCount();
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        long overflow = experience.recycleItems(count, TrashBinConfig.ITEMS_PER_XP.get(), TrashBinConfig.XP_CAPACITY.get());
        spawnExperience(player.position(), overflow); setChanged();
    }
    public void extractByPlayer(Player player) {
        if (level instanceof ServerLevel) { spawnExperience(player.position(), experience.takePoints()); setChanged(); }
    }
    private void recycleAutomatically(ItemStack stack) {
        // Deliberately discard overflow, as requested. Input remains a dependable disposal sink.
        experience.recycleItems(stack.getCount(), TrashBinConfig.ITEMS_PER_XP.get(), TrashBinConfig.XP_CAPACITY.get());
        setChanged();
    }
    public void dropContents() {
        if (!(level instanceof ServerLevel) || dropped) return;
        dropped = true;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, inventory.getStackInSlot(slot));
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        spawnExperience(Vec3.atCenterOf(worldPosition), experience.takePoints());
        experience.restore(0, 0, 0);
    }
    private void spawnExperience(Vec3 pos, long points) {
        if (!(level instanceof ServerLevel server) || points <= 0) return;
        // Bounded entity count even for a billion-point cache; each orb retains its exact XP value.
        long portion = Math.max(1, (points + 31) / 32);
        while (points > 0) {
            int value = (int) Math.min(Integer.MAX_VALUE, Math.min(points, portion));
            server.addFreshEntity(new ExperienceOrb(server, pos.x, pos.y + 0.1, pos.z, value));
            points -= value;
        }
    }
    private void changedInventory() {
        setChanged();
        if (level != null && !level.isClientSide) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }
    public int occupiedSlots() {
        int count = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) if (!inventory.getStackInSlot(slot).isEmpty()) count++;
        return count;
    }
    public int comparatorSignal() {
        double fullness = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) fullness += (double) stack.getCount() / Math.min(stack.getMaxStackSize(), inventory.getSlotLimit(slot));
        }
        return (int) Math.floor(fullness / SLOT_COUNT * 14) + (fullness > 0 ? 1 : 0);
    }
    public boolean stillValid(Player player) {
        return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }
    @Override public Component getDisplayName() { return VersionPlatform.translatable("block.trashbin.trash_bin"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new TrashBinMenu(id, playerInventory, this);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DataVersion", 1);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putLongArray("ArrivalOrder", arrivals.order());
        tag.putLongArray("ArrivalGameTime", arrivals.gameTime());
        tag.putLong("ExperienceUnits", experience.units());
        tag.putLong("ItemRemainder", experience.itemRemainder());
        tag.putLong("FluidRemainder", experience.fluidRemainder());
        tag.putInt("RedstoneMode", mode.ordinal());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); loading = true;
        try {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
            // Do not permit malformed NBT to change the real storage size.
            if (inventory.getSlots() != SLOT_COUNT) inventory.setSize(SLOT_COUNT);
            arrivals.restore(tag.getLongArray("ArrivalOrder"), tag.getLongArray("ArrivalGameTime"));
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) arrivals.emptied(slot); else arrivals.inserted(slot, 0);
                previous[slot] = stack.copy();
            }
            experience.restore(tag.getLong("ExperienceUnits"), tag.getLong("ItemRemainder"), tag.getLong("FluidRemainder"));
            mode = RedstoneMode.byId(tag.getInt("RedstoneMode"));
        } finally { loading = false; }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!isRemoved()) {
            if (cap == ITEM_CAP) return itemCapability.cast();
            if (cap == FLUID_CAP) return fluidCapability.cast();
        }
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); fluidCapability.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps(); itemCapability = LazyOptional.of(() -> automation); fluidCapability = LazyOptional.of(() -> fluidHandler);
    }

    private final class AutomationItems implements IItemHandler {
        // The final slot is a virtual, always-empty intake, not additional storage.
        // Vanilla hoppers preflight the inventory and will never call insert on a full one.
        @Override public int getSlots() { return SLOT_COUNT + 1; }
        @Override public ItemStack getStackInSlot(int slot) { return slot == SLOT_COUNT ? ItemStack.EMPTY : inventory.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return slot == SLOT_COUNT ? 64 : inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return automationEnabled() && (slot == SLOT_COUNT || inventory.isItemValid(slot, stack));
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !automationEnabled()) return stack;
            if (slot != SLOT_COUNT) return inventory.insertItem(slot, stack, simulate);
            int accepted = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), getSlotLimit(slot)));
            if (accepted <= 0) return stack;
            ItemStack portion = stack.copy(); portion.setCount(accepted);
            ItemStack internalRemainder = ItemHandlerHelper.insertItemStacked(inventory, portion, simulate);
            if (!simulate) {
                if (!internalRemainder.isEmpty()) {
                    int oldest = arrivals.oldest();
                    ItemStack evicted = inventory.getStackInSlot(oldest).copy();
                    inventory.setStackInSlot(oldest, ItemStack.EMPTY);
                    recycleAutomatically(evicted);
                    inventory.insertItem(oldest, internalRemainder, false);
                }
            }
            if (accepted == stack.getCount()) return ItemStack.EMPTY;
            ItemStack remainder = stack.copy(); remainder.shrink(accepted); return remainder;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot != SLOT_COUNT && automationEnabled() ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    }

    private final class RecyclingFluidHandler implements IFluidHandler {
        // Tank 0 is a disposal inlet; tank 1 is a virtual XP output when a compatible fluid exists.
        @Override public int getTanks() { return ExperienceFluidCompat.preferred().id() == 0 ? 1 : 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (tank == 0) return FluidStack.EMPTY;
            checkOutputTank(tank);
            ExperienceFluidCompat.Output output = ExperienceFluidCompat.preferred();
            return new FluidStack(output.fluid(), experience.availableFluid(output.millibucketsPerPoint()));
        }
        @Override public int getTankCapacity(int tank) {
            if (tank == 0) return Integer.MAX_VALUE;
            checkOutputTank(tank);
            return (int) Math.min(Integer.MAX_VALUE, (long) TrashBinConfig.XP_CAPACITY.get() * ExperienceFluidCompat.preferred().millibucketsPerPoint());
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && !stack.isEmpty();
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !automationEnabled()) return 0;
            if (action.execute()) {
                experience.recycleFluid(resource.getAmount(), TrashBinConfig.FLUID_MB_PER_XP.get(), TrashBinConfig.XP_CAPACITY.get());
                setChanged();
            }
            return resource.getAmount();
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.getFluid().isSame(ExperienceFluidCompat.preferred().fluid())) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            ExperienceFluidCompat.Output output = ExperienceFluidCompat.preferred();
            if (!automationEnabled() || output.fluid() == Fluids.EMPTY || maxDrain <= 0) return FluidStack.EMPTY;
            int amount = experience.drainFluid(maxDrain, output.millibucketsPerPoint(), action.simulate());
            if (action.execute() && amount > 0) setChanged();
            return amount == 0 ? FluidStack.EMPTY : new FluidStack(output.fluid(), amount);
        }
        private void checkOutputTank(int tank) {
            if (tank != 1 || getTanks() != 2) throw new IndexOutOfBoundsException("fluid tank " + tank);
        }
    }
}
