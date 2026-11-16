package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Which enemy holds which tile, so that every enemy stands on a tile of its own. An enemy holds the tile it stands on
 * and, while it moves, the tile it is moving to: the target is reserved when the move starts and the old tile is let
 * go once the enemy has arrived, so two enemies never hold the same tile at any moment.
 *
 * <p>Movement strategies ask {@link #isFreeFor(Enemy, int, int)} before choosing a tile and
 * {@link #tryReserve(Enemy, int, int)} to take it. {@link #sync(Enemy)} brings the holds of an enemy in line with the
 * tiles it reports, which is cheap when nothing changed, so a move that was made without a reservation is still
 * recorded. The class allocates only when an enemy changes tile.</p>
 */
public final class EnemyOccupancy {

    /** The tiles one enemy holds, as the occupancy last recorded them. */
    private static final class Claim {
        private boolean recorded;
        private int currentX;
        private int currentY;
        private int targetX;
        private int targetY;
    }

    private final Map<Long, Enemy> owners = new HashMap<>();
    private final Map<Enemy, Claim> claims = new IdentityHashMap<>();

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    /**
     * Tells whether an enemy may move to a tile.
     *
     * @param enemy the enemy asking
     * @param x     tile column
     * @param y     tile row
     * @return true when no other living enemy holds the tile
     */
    public boolean isFreeFor(Enemy enemy, int x, int y) {
        final Enemy owner = owners.get(key(x, y));
        return owner == null || owner == enemy || owner.isDead();
    }

    /**
     * Prices the tiles other enemies hold, so a path of this enemy goes around them when a detour costs less than
     * the penalty and waits behind them otherwise, as in a corridor.
     *
     * @param enemy   the enemy that searches, whose own tiles cost nothing
     * @param penalty extra cost of entering a tile another enemy holds
     * @return the price list, which reads this occupancy as it is when the search runs
     */
    public TilePenalty penaltyFor(Enemy enemy, double penalty) {
        return (x, y) -> isFreeFor(enemy, x, y) ? 0 : penalty;
    }

    /**
     * Finds the enemy that holds a tile, as the tile it stands on or as the tile it is moving to.
     *
     * @param x tile column
     * @param y tile row
     * @return the living holder, or null when the tile is free
     */
    public Enemy ownerOf(int x, int y) {
        final Enemy owner = owners.get(key(x, y));
        return owner == null || owner.isDead() ? null : owner;
    }

    /**
     * Reserves a tile as the target of an enemy, letting go of the target it reserved before. The tile the enemy
     * stands on stays held.
     *
     * @param enemy the enemy that is about to move
     * @param x     tile column
     * @param y     tile row
     * @return true when the tile is now held by the enemy, false when another enemy holds it
     */
    public boolean tryReserve(Enemy enemy, int x, int y) {
        sync(enemy);
        if (!isFreeFor(enemy, x, y)) {
            return false;
        }
        final Claim claim = claims.get(enemy);
        if (claim.targetX != claim.currentX || claim.targetY != claim.currentY) {
            unhold(claim.targetX, claim.targetY, enemy);
        }
        claim.targetX = x;
        claim.targetY = y;
        owners.put(key(x, y), enemy);
        return true;
    }

    /**
     * Brings the holds of an enemy in line with the tile it stands on and the tile it moves to. A tile that another
     * enemy already holds stays with that enemy.
     *
     * @param enemy the enemy to record
     */
    public void sync(Enemy enemy) {
        Claim claim = claims.get(enemy);
        if (claim == null) {
            claim = new Claim();
            claims.put(enemy, claim);
        }
        final int currentX = enemy.getCurrentTileX();
        final int currentY = enemy.getCurrentTileY();
        final int targetX = enemy.getTargetTileX();
        final int targetY = enemy.getTargetTileY();
        if (claim.recorded && claim.currentX == currentX && claim.currentY == currentY
                && claim.targetX == targetX && claim.targetY == targetY) {
            return;
        }
        if (claim.recorded) {
            unhold(claim.currentX, claim.currentY, enemy);
            unhold(claim.targetX, claim.targetY, enemy);
        }
        owners.putIfAbsent(key(currentX, currentY), enemy);
        owners.putIfAbsent(key(targetX, targetY), enemy);
        claim.recorded = true;
        claim.currentX = currentX;
        claim.currentY = currentY;
        claim.targetX = targetX;
        claim.targetY = targetY;
    }

    /**
     * Lets go of every tile an enemy holds, as when it dies or leaves the map.
     *
     * @param enemy the enemy to forget
     */
    public void release(Enemy enemy) {
        final Claim claim = claims.remove(enemy);
        if (claim != null && claim.recorded) {
            unhold(claim.currentX, claim.currentY, enemy);
            unhold(claim.targetX, claim.targetY, enemy);
        }
    }

    /** Forgets every enemy. */
    public void clear() {
        owners.clear();
        claims.clear();
    }

    /**
     * Forgets every enemy and records the living ones of a collection.
     *
     * @param enemies the enemies on the map, null for none
     */
    public void rebuild(Collection<? extends Enemy> enemies) {
        clear();
        if (enemies == null) {
            return;
        }
        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                sync(enemy);
            }
        }
    }

    /** @return how many tiles are held, for the tests */
    public int heldTileCount() {
        return owners.size();
    }

    private void unhold(int x, int y, Enemy enemy) {
        owners.remove(key(x, y), enemy);
    }
}
