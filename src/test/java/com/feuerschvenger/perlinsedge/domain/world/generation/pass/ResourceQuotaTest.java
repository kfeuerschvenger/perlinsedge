package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceQuotaTest {

    private static final int MIN = 45;
    private static final int MAX = 111;
    private static final int CAP = 100;
    private static final int CAP_BEYOND_THE_SCALE = 1001;
    private static final int BIG_AREA = 90_000;
    private static final int SMALL_AREA = 22_500;
    private static final int EXPECTED_MIN_BIG = 41;
    private static final int EXPECTED_MAX_BIG = 100;
    private static final int EXPECTED_MIN_SMALL = 10;
    private static final int EXPECTED_MAX_SMALL = 25;

    private static ResourceQuota quota(int min, int max, int cap) {
        return new ResourceQuota(ResourceType.IRON_ORE, min, max, ResourceQuota.Host.ROCKY, cap);
    }

    @Test
    void theRangeScalesWithTheAreaAndRoundsToWholeTiles() {
        final var quota = quota(MIN, MAX, CAP);

        assertEquals(EXPECTED_MIN_BIG, quota.minFor(BIG_AREA));
        assertEquals(EXPECTED_MAX_BIG, quota.maxFor(BIG_AREA));
        assertEquals(EXPECTED_MIN_SMALL, quota.minFor(SMALL_AREA));
        assertEquals(EXPECTED_MAX_SMALL, quota.maxFor(SMALL_AREA));
    }

    @Test
    void anUnlimitedMaximumStaysUnlimitedWhateverTheArea() {
        final var quota = quota(MIN, ResourceQuota.UNLIMITED, CAP);

        assertEquals(ResourceQuota.UNLIMITED, quota.maxFor(BIG_AREA));
        assertEquals(ResourceQuota.UNLIMITED, quota.maxFor(Integer.MAX_VALUE));
    }

    @Test
    void aQuotaRefusesABrokenRangeCapTypeOrHost() {
        assertThrows(IllegalArgumentException.class, () -> quota(-1, MAX, CAP));
        assertThrows(IllegalArgumentException.class, () -> quota(MAX, MIN, CAP));
        assertThrows(IllegalArgumentException.class, () -> quota(MIN, MAX, 0));
        assertThrows(IllegalArgumentException.class, () -> quota(MIN, MAX, CAP_BEYOND_THE_SCALE));
        assertThrows(NullPointerException.class,
                () -> new ResourceQuota(null, MIN, MAX, ResourceQuota.Host.ROCKY, CAP));
        assertThrows(NullPointerException.class, () -> new ResourceQuota(ResourceType.TREE, MIN, MAX, null, CAP));
    }

    @Test
    void theBalanceParametersCopyTheirQuotas() {
        final var quotas = new ArrayList<>(List.of(quota(MIN, MAX, CAP)));
        final var params = new BalanceParams(quotas);
        quotas.clear();

        assertEquals(1, params.quotas().size());
        assertThrows(NullPointerException.class, () -> new BalanceParams(null));
        assertThrows(NullPointerException.class, () -> new ResourceBalancePass(null));
    }
}
