package io.github.whiteviera.trashbin.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

/** Arrival order survives reloads and is independent of world-clock changes. */
public final class ArrivalOrder {
    private final long[] order;
    private final long[] gameTime;
    private long sequence;
    public ArrivalOrder(int size) { order = new long[size]; gameTime = new long[size]; }
    public void inserted(int slot, long tick) {
        if (order[slot] != 0) return; // merging preserves the original group's age
        if (sequence == Long.MAX_VALUE) renumber();
        order[slot] = ++sequence;
        gameTime[slot] = tick;
    }
    public void emptied(int slot) { order[slot] = 0; gameTime[slot] = 0; }
    public int oldest() {
        int result = -1;
        for (int i = 0; i < order.length; i++)
            if (order[i] > 0 && (result == -1 || order[i] < order[result])) result = i;
        return result;
    }
    public long[] order() { return order.clone(); }
    public long[] gameTime() { return gameTime.clone(); }
    public void restore(long[] ordinals, long[] times) {
        Arrays.fill(order, 0); Arrays.fill(gameTime, 0); sequence = 0;
        for (int i = 0; i < Math.min(order.length, ordinals.length); i++) {
            order[i] = Math.max(0, ordinals[i]);
            if (i < times.length) gameTime[i] = Math.max(0, times[i]);
            sequence = Math.max(sequence, order[i]);
        }
    }
    private void renumber() {
        int[] slots = IntStream.range(0, order.length).filter(i -> order[i] > 0).boxed()
                .sorted(Comparator.comparingLong(i -> order[i])).mapToInt(i -> i).toArray();
        sequence = 0;
        for (int slot : slots) order[slot] = ++sequence;
    }
}
