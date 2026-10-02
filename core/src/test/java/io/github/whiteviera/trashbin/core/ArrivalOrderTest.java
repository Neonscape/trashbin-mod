package io.github.whiteviera.trashbin.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArrivalOrderTest {
    @Test void oldestIsArrivalOrderRatherThanSlotOrWorldTime() {
        ArrivalOrder order = new ArrivalOrder(27);
        order.inserted(12, 100); order.inserted(0, 1); order.inserted(24, 100);
        assertEquals(12, order.oldest()); order.emptied(12); assertEquals(0, order.oldest());
    }
    @Test void mergingKeepsGroupAgeButReplacingEmptySlotGetsNewAge() {
        ArrivalOrder order = new ArrivalOrder(3);
        order.inserted(2, 10); order.inserted(1, 20); order.inserted(2, 30);
        assertEquals(2, order.oldest()); assertEquals(10, order.gameTime()[2]);
        order.emptied(2); order.inserted(2, 40); assertEquals(1, order.oldest());
    }
    @Test void persistenceAndSequenceOverflowPreserveOrdering() {
        ArrivalOrder order = new ArrivalOrder(3);
        order.restore(new long[]{Long.MAX_VALUE, Long.MAX_VALUE - 1, 0}, new long[]{4, 3, 0});
        order.inserted(2, 1); assertEquals(1, order.oldest());
        ArrivalOrder restored = new ArrivalOrder(3); restored.restore(order.order(), order.gameTime());
        assertEquals(1, restored.oldest()); restored.emptied(1); assertEquals(0, restored.oldest());
    }
    @Test void allRedstoneModesMatchTheTruthTable() {
        assertTrue(RedstoneMode.IGNORE.allows(false)); assertTrue(RedstoneMode.IGNORE.allows(true));
        assertFalse(RedstoneMode.WHEN_POWERED.allows(false)); assertTrue(RedstoneMode.WHEN_POWERED.allows(true));
        assertTrue(RedstoneMode.WHEN_UNPOWERED.allows(false)); assertFalse(RedstoneMode.WHEN_UNPOWERED.allows(true));
        assertEquals(RedstoneMode.IGNORE, RedstoneMode.WHEN_UNPOWERED.next()); assertEquals(RedstoneMode.IGNORE, RedstoneMode.byId(9));
    }
}
