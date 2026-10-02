package io.github.whiteviera.trashbin.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceLedgerTest {
    @Test void itemRemainderSurvivesSeparateRecyclingAndReload() {
        ExperienceLedger first = new ExperienceLedger();
        first.recycleItems(32, 64, 999999);
        ExperienceLedger reloaded = new ExperienceLedger();
        reloaded.restore(first.units(), first.itemRemainder(), first.fluidRemainder());
        reloaded.recycleItems(32, 64, 999999);
        assertEquals(1, reloaded.points()); assertEquals(0, reloaded.itemRemainder());
    }
    @Test void fluidRemainderAccumulatesAcrossBuckets() {
        ExperienceLedger ledger = new ExperienceLedger();
        for (int i = 0; i < 512; i++) ledger.recycleFluid(1000, 512000, 999999);
        assertEquals(1, ledger.points()); assertEquals(0, ledger.fluidRemainder());
    }
    @Test void cacheOverflowHasExactValue() {
        ExperienceLedger ledger = new ExperienceLedger();
        assertEquals(0, ledger.addPoints(9, 10));
        assertEquals(5, ledger.recycleItems(384, 64, 10));
        assertEquals(10, ledger.points());
    }
    @Test void nativeOutputRatesHaveEqualExperienceValue() {
        ExperienceLedger ledger = new ExperienceLedger(); ledger.addPoints(8, 100);
        assertEquals(8, ledger.availableFluid(1)); assertEquals(160, ledger.availableFluid(20));
        assertEquals(60, ledger.drainFluid(60, 20, false)); assertEquals(5, ledger.points());
        assertEquals(5, ledger.drainFluid(100, 1, false)); assertEquals(0, ledger.units());
    }
    @Test void singleMillibucketDrainNeverDuplicatesOrRoundsUpXp() {
        ExperienceLedger ledger = new ExperienceLedger(); ledger.addPoints(2, 100);
        for (int i = 0; i < 21; i++) assertEquals(1, ledger.drainFluid(1, 20, false));
        assertEquals(19, ledger.units()); assertEquals(0, ledger.takePoints());
        assertEquals(0, ledger.availableFluid(1)); assertEquals(19, ledger.availableFluid(20));
    }
    @Test void fluidSimulationNeverChangesLedger() {
        ExperienceLedger ledger = new ExperienceLedger(); ledger.addPoints(42, 999999);
        for (int i = 0; i < 100; i++) assertEquals(840, ledger.drainFluid(Integer.MAX_VALUE, 20, true));
        assertEquals(42, ledger.points());
    }
    @Test void extractingPointsKeepsAllOtherProgress() {
        ExperienceLedger ledger = new ExperienceLedger();
        ledger.restore(49, 13, 28000);
        assertEquals(2, ledger.takePoints()); assertEquals(9, ledger.units());
        assertEquals(13, ledger.itemRemainder()); assertEquals(28000, ledger.fluidRemainder());
    }
    @Test void largeCapacityAndFluidViewDoNotOverflowIntegers() {
        ExperienceLedger ledger = new ExperienceLedger(); ledger.addPoints(1000000000, 1000000000);
        assertEquals(1000000000, ledger.points()); assertEquals(Integer.MAX_VALUE, ledger.availableFluid(20));
        assertEquals(Integer.MAX_VALUE, ledger.drainFluid(Integer.MAX_VALUE, 20, false));
        assertEquals(20000000000L - Integer.MAX_VALUE, ledger.units());
    }
    @Test void fluidTotalsUseLongArithmetic() {
        ExperienceLedger ledger = new ExperienceLedger(); ledger.restore(0, 0, Integer.MAX_VALUE - 1L);
        ledger.recycleFluid(Integer.MAX_VALUE, Integer.MAX_VALUE, 999999);
        assertEquals(1, ledger.points()); assertEquals(Integer.MAX_VALUE - 1L, ledger.fluidRemainder());
    }
    @Test void capacityReductionClampsCache() {
        ExperienceLedger ledger = new ExperienceLedger(); ledger.addPoints(100, 1000);
        assertEquals(90, ledger.addPoints(0, 10)); assertEquals(10, ledger.points());
    }
    @Test void arbitraryRatiosKeepExactRawRemainders() {
        ExperienceLedger ledger = new ExperienceLedger();
        for (int i = 0; i < 10000; i++) ledger.recycleItems(1, 7, 999999);
        assertEquals(1428, ledger.points()); assertEquals(4, ledger.itemRemainder());
    }
    @Test void invalidRatesFailAndCorruptPersistenceIsBounded() {
        ExperienceLedger ledger = new ExperienceLedger();
        assertThrows(IllegalArgumentException.class, () -> ledger.recycleItems(1, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> ledger.drainFluid(1, 3, false));
        ledger.restore(-1, -3, Long.MAX_VALUE);
        assertEquals(0, ledger.units()); assertEquals(0, ledger.itemRemainder()); assertEquals(Integer.MAX_VALUE, ledger.fluidRemainder());
    }
}
