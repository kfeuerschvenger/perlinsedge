package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * Where stone outcrops.
 *
 * @param rockyPerMille        the chance in thousandths that a tile of rocky ground holds stone, from 0 to 1000
 * @param loosePerTenThousand  the chance in ten thousandths that any other walkable land that can hold a resource holds
 *                             a loose stone, from 0 to 10000, so even a map with no rock has a few stones
 */
public record StoneParams(int rockyPerMille, int loosePerTenThousand) {

    /** Checks both chances are in range. */
    public StoneParams {
        if (rockyPerMille < 0 || rockyPerMille > RockResourceParams.PER_MILLE_SCALE) {
            throw new IllegalArgumentException("the rocky stone chance must be from 0 to "
                    + RockResourceParams.PER_MILLE_SCALE + " per mille: " + rockyPerMille);
        }
        if (loosePerTenThousand < 0 || loosePerTenThousand > Chance.TEN_THOUSAND_SCALE) {
            throw new IllegalArgumentException("the loose stone chance must be from 0 to "
                    + Chance.TEN_THOUSAND_SCALE + " per ten thousand: " + loosePerTenThousand);
        }
    }
}
