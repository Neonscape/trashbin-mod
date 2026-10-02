package io.github.whiteviera.trashbin.menu;

import io.github.whiteviera.trashbin.TrashBinConfig;
import io.github.whiteviera.trashbin.TrashBinMod;
import io.github.whiteviera.trashbin.block.TrashBinBlockEntity;
import io.github.whiteviera.trashbin.compat.ExperienceFluidCompat;
import io.github.whiteviera.trashbin.core.RedstoneMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class TrashBinMenu extends AbstractContainerMenu {
    public static final int CLEAR = 0, EXTRACT = 1, CYCLE_REDSTONE = 2;
    private static final int DATA_SIZE = 9;
    private final TrashBinBlockEntity bin;
    private final ContainerData data;

    public static TrashBinMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        // The client needs slots and synced values only, never a live capability or block-entity NBT.
        buffer.readBlockPos();
        return new TrashBinMenu(id, inventory, null);
    }
    public TrashBinMenu(int id, Inventory playerInventory, TrashBinBlockEntity bin) {
        super(TrashBinMod.BIN_MENU.get(), id); this.bin = bin;
        ItemStackHandler storage = bin == null ? new ItemStackHandler(27) : bin.inventory();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int binSlot = col + row * 9;
            addSlot(new SlotItemHandler(storage, binSlot, 8 + col * 18, 18 + row * 18) {
                @Override public void setChanged() {
                    super.setChanged(); if (bin != null) bin.contentsChanged(binSlot);
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        data = bin == null ? new SimpleContainerData(DATA_SIZE) : new ContainerData() {
            @Override public int get(int index) {
                // Vanilla menu properties are signed 16-bit on the wire. Split large integers.
                return switch (index) {
                    case 0 -> bin.experience().points() & 0xffff;
                    case 1 -> bin.experience().points() >>> 16;
                    case 2 -> TrashBinConfig.XP_CAPACITY.get() & 0xffff;
                    case 3 -> TrashBinConfig.XP_CAPACITY.get() >>> 16;
                    case 4 -> bin.mode().ordinal();
                    case 5 -> bin.automationEnabled() ? 1 : 0;
                    case 6 -> ExperienceFluidCompat.preferred().id();
                    case 7 -> bin.occupiedSlots();
                    case 8 -> (int) (bin.experience().units() % 20);
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_SIZE; }
        };
        addDataSlots(data);
        if (bin != null) bin.startOpen(playerInventory.player);
    }
    public TrashBinBlockEntity bin() { return bin; }
    public int experiencePoints() { return (data.get(0) & 0xffff) | ((data.get(1) & 0xffff) << 16); }
    public int experienceCapacity() { return (data.get(2) & 0xffff) | ((data.get(3) & 0xffff) << 16); }
    public int fractionalUnits() { return data.get(8); }
    public RedstoneMode mode() { return RedstoneMode.byId(data.get(4)); }
    public boolean automationEnabled() { return data.get(5) != 0; }
    public int outputFluidId() { return data.get(6); }
    public int occupiedSlots() { return data.get(7); }
    @Override public boolean stillValid(Player player) { return bin == null || bin.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int action) {
        if (bin == null || player.getCommandSenderWorld().isClientSide || player.isSpectator() || !stillValid(player)
                || player.containerMenu != this) return false;
        switch (action) {
            case CLEAR -> bin.clearByPlayer(player);
            case EXTRACT -> bin.extractByPlayer(player);
            case CYCLE_REDSTONE -> bin.cycleMode();
            default -> { return false; }
        }
        broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(); ItemStack original = stack.copy();
        if (index < 27) {
            if (!moveItemStackTo(stack, 27, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack); return original;
    }
    @Override public void removed(Player player) {
        super.removed(player); if (bin != null) bin.stopOpen(player);
    }
}
