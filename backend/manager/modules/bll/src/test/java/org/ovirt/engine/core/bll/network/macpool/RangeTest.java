package org.ovirt.engine.core.bll.network.macpool;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.math.LongRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ovirt.engine.core.utils.MacAddressRangeUtils;

public class RangeTest {

    private static final int MAC_FROM_RANGE = 15;
    private static final int MAC_OUTSIDE_OF_RANGE = 25;
    private static final int RANGE_FROM = 10;
    private static final int RANGE_TO = 19;
    private static final int NUMBER_OF_MACS = 10;
    private Range rangeOf10Macs;

    @BeforeEach
    public void before() {
        rangeOf10Macs = new Range(new LongRange(RANGE_FROM, RANGE_TO));
    }

    @Test
    public void testMacIsContainedInRange() {
        assertThat(rangeOf10Macs.contains(MAC_FROM_RANGE), is(true));
    }
    @Test
    public void testMacIsNotContainedInRange() {
        assertThat(rangeOf10Macs.contains(MAC_OUTSIDE_OF_RANGE), is(false));
    }

    @Test
    public void testFailWhenUsingMacOutsideOfRange() {
        assertThrows(IllegalArgumentException.class, () -> rangeOf10Macs.use(MAC_OUTSIDE_OF_RANGE, false));
    }

    @Test
    public void testFailWhenAskingForMacOutsideOfRange() {
        assertThrows(IllegalArgumentException.class, () -> rangeOf10Macs.isAllocated(MAC_OUTSIDE_OF_RANGE));
    }

    @Test
    public void testFailWhenReturningMacOutsideOfRange() {
        assertThrows(IllegalArgumentException.class, () -> rangeOf10Macs.freeMac(MAC_OUTSIDE_OF_RANGE));
    }

    @Test
    public void testAllMacsAreAvailableAfterCreation() {
        assertThat(rangeOf10Macs.getAvailableCount(), is(NUMBER_OF_MACS));
    }

    @Test
    public void testAssigningMacWithDisallowedDuplicates() {
        assertThat(rangeOf10Macs.use(MAC_FROM_RANGE, false), is(true));
        assertThat(rangeOf10Macs.getAvailableCount(), is(NUMBER_OF_MACS - 1));
        assertThat(rangeOf10Macs.use(MAC_FROM_RANGE, false), is(false));
        assertThat(rangeOf10Macs.getAvailableCount(), is(NUMBER_OF_MACS - 1));
        assertThat(rangeOf10Macs.isAllocated(MAC_FROM_RANGE), is(true));
        assertThat(rangeOf10Macs.containsDuplicates(), is(false));
    }

    @Test
    public void testAssigningMacWithAllowedDuplicates() {
        assertThat(rangeOf10Macs.use(MAC_FROM_RANGE, true), is(true));
        assertThat(rangeOf10Macs.getAvailableCount(), is(NUMBER_OF_MACS - 1));
        assertThat(rangeOf10Macs.containsDuplicates(), is(false));
        assertThat(rangeOf10Macs.use(MAC_FROM_RANGE, true), is(true));
        assertThat(rangeOf10Macs.getAvailableCount(), is(NUMBER_OF_MACS - 1));
        assertThat(rangeOf10Macs.isAllocated(MAC_FROM_RANGE), is(true));
        assertThat(rangeOf10Macs.containsDuplicates(), is(true));
    }

    @Test
    public void testFreeMac() {
        final List<Long> allocatedMacs = rangeOf10Macs.allocateMacs(NUMBER_OF_MACS);
        assertThat(allocatedMacs.size(), is(NUMBER_OF_MACS));
        assertThat(rangeOf10Macs.getAvailableCount(), is(0));
        assertThat(rangeOf10Macs.containsDuplicates(), is(false));

        for(int i = 1; i <= NUMBER_OF_MACS; i++) {
            rangeOf10Macs.freeMac(allocatedMacs.remove(0));
            assertThat(rangeOf10Macs.getAvailableCount(), is(i));
            assertThat(rangeOf10Macs.containsDuplicates(), is(false));
        }
    }

    @Test
    public void testFreeMacDuplicityAllowed() {
        // Allocate one mac twice.
        assertThat(rangeOf10Macs.use(MAC_FROM_RANGE, true), is(true));
        assertThat(rangeOf10Macs.isAllocated(MAC_FROM_RANGE), is(true));
        assertThat(rangeOf10Macs.containsDuplicates(), is(false));
        assertThat(rangeOf10Macs.use(MAC_FROM_RANGE, true), is(true));
        assertThat(rangeOf10Macs.isAllocated(MAC_FROM_RANGE), is(true));
        assertThat(rangeOf10Macs.containsDuplicates(), is(true));

        // Check decreasing of duplicity usage.
        rangeOf10Macs.freeMac(MAC_FROM_RANGE);
        assertThat(rangeOf10Macs.isAllocated(MAC_FROM_RANGE), is(true));
        assertThat(rangeOf10Macs.containsDuplicates(), is(false));
        rangeOf10Macs.freeMac(MAC_FROM_RANGE);
        assertThat(rangeOf10Macs.isAllocated(MAC_FROM_RANGE), is(false));
        assertThat(rangeOf10Macs.containsDuplicates(), is(false));
    }

    @Test
    public void testAllocateMac() {
        assertThat(rangeOf10Macs.allocateMacs(5).size(), is(5));
        assertThat(rangeOf10Macs.getAvailableCount(), is(5));
        assertThat(rangeOf10Macs.allocateMacs(5).size(), is(5));
        assertThat(rangeOf10Macs.getAvailableCount(), is(0));
        assertThat(rangeOf10Macs.containsDuplicates(), is(false));
    }

    @Test
    public void testAllocateMacNoEnoughMacs() {
        assertThrows(IllegalStateException.class, () -> rangeOf10Macs.allocateMacs(NUMBER_OF_MACS + 1));
    }

    @Test
    public void testRangeStartAndRangeStopAreInclusive() {
        assertThat(new Range(new LongRange(MAC_FROM_RANGE, MAC_FROM_RANGE)).getAvailableCount(), is(1));
    }

    @Test
    public void testRangeCanContainOnlyIntSizeNumberOfElements() {
        LongRange longRange = MacAddressRangeUtils.clipRange(new LongRange(0, Long.MAX_VALUE));
        Range range = new Range(new LongRange(longRange.getMinimumLong(), longRange.getMaximumLong()));
        assertThat(range.getAvailableCount(), is(Integer.MAX_VALUE));
    }

    @Test
    public void testTooBigRange() {
        assertThrows(IllegalArgumentException.class, () -> new Range(new LongRange(0, Integer.MAX_VALUE)));
    }

    @Test
    public void testMaxSizeRange() {
        new Range(new LongRange(0, Integer.MAX_VALUE - 1));
    }


    /**
     * test that MACs arent returned in leftmost-available order. Instead, we're returning macs from left to right,
     * noting last scan position. That means that if we obtain and return mac from/to pool, we'll obtain this MAC again
     * only after all other free macs were used.
     */
    @Test
    public void testOrderOrAcquiredMACs() {
        Range range = new Range(new LongRange(0, 5));
        List<Integer> usedMacs = Arrays.asList(0, 2, 4);
        for(int i = 0; i < 5; i++) {
            boolean usedMac = usedMacs.contains(i);
            if (usedMac) {
                boolean allowDuplicates = false;
                range.use(i, allowDuplicates);
                assertThat(range.containsDuplicates(), is(false));
            }
        }

        for (Integer expectedUnallocatedMac : Arrays.asList(1, 3, 5, 1, 3, 5, 1)) {
            allocateAndFreeMacAndExpectGivenMac(range, expectedUnallocatedMac);
        }
    }

    /***
     *
     * method obtains mac from pool, assert expectation, and return it back.
     *
     * @param range range of macs
     * @param expectedMac mac, which we expect to be returned from {@code range.allocateMacs(1)}
     */
    private void allocateAndFreeMacAndExpectGivenMac(Range range, long expectedMac) {
        Long mac = range.allocateMacs(1).get(0);
        assertThat(mac, is(expectedMac));
        range.freeMac(mac);
    }
}
