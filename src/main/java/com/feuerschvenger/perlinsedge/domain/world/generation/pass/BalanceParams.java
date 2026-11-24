package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.List;

/**
 * The counts the balancing pass brings the resources of a map to.
 *
 * @param quotas the quota of each resource, applied in this order, copied
 */
public record BalanceParams(List<ResourceQuota> quotas) {

    /** Copies the quotas. */
    public BalanceParams {
        quotas = List.copyOf(quotas);
    }
}
