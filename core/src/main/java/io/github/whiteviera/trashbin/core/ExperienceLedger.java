package io.github.whiteviera.trashbin.core;

/** Integer arithmetic only. One unit is 1/20 XP, also one mB of Sophisticated XP. */
public final class ExperienceLedger {
    public static final int UNITS_PER_POINT = 20;
    private long units;
    private long itemRemainder;
    private long fluidRemainder;

    public long units() { return units; }
    public int points() { return (int) (units / UNITS_PER_POINT); }
    public long itemRemainder() { return itemRemainder; }
    public long fluidRemainder() { return fluidRemainder; }

    public void restore(long units, long items, long fluid) {
        this.units = Math.max(0, Math.min(units, (long) Integer.MAX_VALUE * UNITS_PER_POINT));
        itemRemainder = Math.max(0, Math.min(items, Integer.MAX_VALUE));
        fluidRemainder = Math.max(0, Math.min(fluid, Integer.MAX_VALUE));
    }

    /** Returns whole overflow points; callers choose manual orb emission or automatic discard. */
    public long recycleItems(long count, int itemsPerPoint, int capacity) {
        requirePositive(itemsPerPoint);
        if (count < 0 || count > Integer.MAX_VALUE) throw new IllegalArgumentException("item count");
        long total = itemRemainder + count;
        itemRemainder = total % itemsPerPoint;
        return addPoints(total / itemsPerPoint, capacity);
    }

    public long recycleFluid(int amount, int millibucketsPerPoint, int capacity) {
        requirePositive(millibucketsPerPoint);
        if (amount < 0) throw new IllegalArgumentException("fluid amount");
        long total = fluidRemainder + amount;
        fluidRemainder = total % millibucketsPerPoint;
        return addPoints(total / millibucketsPerPoint, capacity);
    }

    public long addPoints(long points, int capacity) {
        requirePositive(capacity);
        if (points < 0 || points > 2L * Integer.MAX_VALUE) throw new IllegalArgumentException("points");
        long total = units + points * UNITS_PER_POINT;
        units = Math.min(total, (long) capacity * UNITS_PER_POINT);
        return (total - units) / UNITS_PER_POINT;
    }

    /** Whole XP can become orbs; fractional XP and recycling remainders stay in the bin. */
    public int takePoints() {
        int points = points();
        units -= (long) points * UNITS_PER_POINT;
        return points;
    }

    public int availableFluid(int millibucketsPerPoint) {
        requirePositive(millibucketsPerPoint);
        if (millibucketsPerPoint != 1 && millibucketsPerPoint != 20)
            throw new IllegalArgumentException("unsupported experience unit");
        return (int) Math.min(Integer.MAX_VALUE, units / (UNITS_PER_POINT / millibucketsPerPoint));
    }

    public int drainFluid(int requested, int millibucketsPerPoint, boolean simulate) {
        int taken = Math.min(Math.max(0, requested), availableFluid(millibucketsPerPoint));
        if (!simulate) units -= (long) taken * (UNITS_PER_POINT / millibucketsPerPoint);
        return taken;
    }

    private static void requirePositive(int value) {
        if (value <= 0) throw new IllegalArgumentException("value must be positive");
    }
}
