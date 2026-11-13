package com.feuerschvenger.perlinsedge.domain.entities.items;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

/** Works out how many hits break a fresh resource, which the tests use to check the balance of the tool table. */
public final class ToolHits {

    /** Not instantiable: the class only holds the calculation. */
    private ToolHits() {
    }

    /**
     * Gives the number of hits it takes to break a fresh resource.
     *
     * @param tool   the item held in hand, null for a bare hand
     * @param target the resource
     * @return the hits, rounded up
     */
    public static int hitsToBreak(ItemType tool, ResourceType target) {
        final int health = AppConfig.getInstance().gameMechanics().getResourceBaseHealth(target);
        return (int) Math.ceil(health / (double) ToolStats.resourceDamage(tool, target));
    }
}
