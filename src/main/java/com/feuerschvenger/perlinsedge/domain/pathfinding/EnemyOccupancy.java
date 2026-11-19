package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;

import java.util.Arrays;
import java.util.Collection;
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
 * recorded. A tile that another enemy held when {@code sync} tried to take it is tried again on the next
 * {@code sync}, so an enemy never believes it holds a tile it lost. The queries allocate nothing: the tiles live in a
 * primitive open-addressing table, and the price list of a search is one object per enemy.</p>
 */
public final class EnemyOccupancy {

    /** The tiles one enemy holds, as the occupancy last recorded them. */
    private static final class Claim {
        private boolean recorded;
        private int currentX;
        private int currentY;
        private int targetX;
        private int targetY;
        /** Whether the enemy actually owns the tile it stands on, and the one it moves to. */
        private boolean currentHeld;
        private boolean targetHeld;
        /** The price list handed to searches of this enemy, created on the first search. */
        private HeldTilePenalty penalty;

        /** Tells whether the recorded holds are exactly the tiles an enemy stands on and moves to now. */
        private boolean isRecordedAt(int currentTileX, int currentTileY, int targetTileX, int targetTileY) {
            if (!recorded) {
                return false;
            }
            return currentX == currentTileX && currentY == currentTileY
                    && targetX == targetTileX && targetY == targetTileY;
        }
    }

    /** Prices the tiles other enemies hold for one enemy, reading the occupancy as it is when the search runs. */
    private final class HeldTilePenalty implements TilePenalty {
        private final Enemy enemy;
        private double cost;

        private HeldTilePenalty(Enemy enemy) {
            this.enemy = enemy;
        }

        @Override
        public double extraCost(int x, int y) {
            return isFreeFor(enemy, x, y) ? 0 : cost;
        }
    }

    /** A long-keyed map from tile to owner with linear probing and backward-shift deletion, so lookups never box. */
    private static final class TileOwners {
        private static final int INITIAL_CAPACITY = 64;
        /** The multiplier of the fibonacci hash that spreads tile keys over the slots, and the bits it keeps. */
        private static final long HASH_MULTIPLIER = 0x9E37_79B9_7F4A_7C15L;
        private static final int HASH_SHIFT = 32;
        /** Slots the table keeps per entry at the most load, so probing stays short. */
        private static final int SLOTS_PER_ENTRY = 2;
        /** How many times larger the table gets when it is full. */
        private static final int GROWTH_FACTOR = 2;

        private long[] keys = new long[INITIAL_CAPACITY];
        private Enemy[] owners = new Enemy[INITIAL_CAPACITY];
        private int size;

        private static int slot(long key, int mask) {
            return (int) ((key * HASH_MULTIPLIER) >>> HASH_SHIFT) & mask;
        }

        Enemy get(long key) {
            final int mask = keys.length - 1;
            for (int i = slot(key, mask); owners[i] != null; i = (i + 1) & mask) {
                if (keys[i] == key) {
                    return owners[i];
                }
            }
            return null;
        }

        void put(long key, Enemy owner) {
            if ((size + 1) * SLOTS_PER_ENTRY > keys.length) {
                grow();
            }
            final int mask = keys.length - 1;
            int i = slot(key, mask);
            while (owners[i] != null) {
                if (keys[i] == key) {
                    owners[i] = owner;
                    return;
                }
                i = (i + 1) & mask;
            }
            keys[i] = key;
            owners[i] = owner;
            size++;
        }

        /** Removes the tile only when the given enemy is its owner. */
        void remove(long key, Enemy owner) {
            final int mask = keys.length - 1;
            for (int i = slot(key, mask); owners[i] != null; i = (i + 1) & mask) {
                if (keys[i] == key) {
                    if (owners[i] == owner) {
                        deleteAt(i);
                    }
                    return;
                }
            }
        }

        private void deleteAt(int start) {
            final int mask = keys.length - 1;
            int hole = start;
            owners[hole] = null;
            size--;
            for (int j = (hole + 1) & mask; owners[j] != null; j = (j + 1) & mask) {
                // An entry may fill the hole when its ideal slot is not strictly between the hole and itself
                if (((j - slot(keys[j], mask)) & mask) >= ((j - hole) & mask)) {
                    keys[hole] = keys[j];
                    owners[hole] = owners[j];
                    owners[j] = null;
                    hole = j;
                }
            }
        }

        private void grow() {
            final long[] oldKeys = keys;
            final Enemy[] oldOwners = owners;
            keys = new long[oldKeys.length * GROWTH_FACTOR];
            owners = new Enemy[oldOwners.length * GROWTH_FACTOR];
            size = 0;
            for (var i = 0; i < oldKeys.length; i++) {
                if (oldOwners[i] != null) {
                    put(oldKeys[i], oldOwners[i]);
                }
            }
        }

        void clear() {
            Arrays.fill(owners, null);
            size = 0;
        }

        int size() {
            return size;
        }
    }

    private final TileOwners owners = new TileOwners();
    private final Map<Enemy, Claim> claims = new IdentityHashMap<>();

    /** A tile key holds the column in its high 32 bits and the row in its low 32 bits. */
    private static final int KEY_X_SHIFT = 32;
    private static final long KEY_Y_MASK = 0xFFFF_FFFFL;

    private static long key(int x, int y) {
        return ((long) x << KEY_X_SHIFT) | (y & KEY_Y_MASK);
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
     * the penalty and waits behind them otherwise, as in a corridor. The price list is one object per enemy that is
     * reused by every search, so asking for it allocates nothing after the first time and the list must not be kept
     * past the search it was asked for.
     *
     * @param enemy   the enemy that searches, whose own tiles cost nothing
     * @param penalty extra cost of entering a tile another enemy holds
     * @return the price list, which reads this occupancy as it is when the search runs
     */
    public TilePenalty penaltyFor(Enemy enemy, double penalty) {
        final var claim = claimOf(enemy);
        if (claim.penalty == null) {
            claim.penalty = new HeldTilePenalty(enemy);
        }
        claim.penalty.cost = penalty;
        return claim.penalty;
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
     * <p>The caller must set the target tile of the enemy to this tile in the same update, before the next
     * {@link #sync(Enemy)}: the occupancy follows the tiles the enemy reports, and a reservation the enemy does not
     * report is let go again by that sync. A call that returns false changes nothing.</p>
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
        final var claim = claims.get(enemy);
        if (claim.targetX != claim.currentX || claim.targetY != claim.currentY) {
            unhold(claim.targetX, claim.targetY, enemy);
        }
        claim.targetX = x;
        claim.targetY = y;
        claim.targetHeld = true;
        owners.put(key(x, y), enemy);
        return true;
    }

    /**
     * Brings the holds of an enemy in line with the tile it stands on and the tile it moves to. A tile that another
     * enemy already holds stays with that enemy, and the enemy that did not win it tries again on the next call.
     *
     * @param enemy the enemy to record
     */
    public void sync(Enemy enemy) {
        final var claim = claimOf(enemy);
        final int currentX = enemy.getCurrentTileX();
        final int currentY = enemy.getCurrentTileY();
        final int targetX = enemy.getTargetTileX();
        final int targetY = enemy.getTargetTileY();
        if (claim.isRecordedAt(currentX, currentY, targetX, targetY)) {
            if (!claim.currentHeld) {
                claim.currentHeld = acquire(currentX, currentY, enemy);
            }
            if (!claim.targetHeld) {
                claim.targetHeld = acquire(targetX, targetY, enemy);
            }
            return;
        }
        if (claim.recorded) {
            unhold(claim.currentX, claim.currentY, enemy);
            unhold(claim.targetX, claim.targetY, enemy);
        }
        claim.recorded = true;
        claim.currentX = currentX;
        claim.currentY = currentY;
        claim.targetX = targetX;
        claim.targetY = targetY;
        claim.currentHeld = acquire(currentX, currentY, enemy);
        claim.targetHeld = acquire(targetX, targetY, enemy);
    }

    /**
     * Lets go of every tile an enemy holds, as when it dies or leaves the map.
     *
     * @param enemy the enemy to forget
     */
    public void release(Enemy enemy) {
        final var claim = claims.remove(enemy);
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

    /**
     * Counts the tiles held. This is a seam for the tests, which check that a release leaves nothing behind where
     * {@link #ownerOf(int, int)} cannot tell a released tile from one held by a dead enemy; the game never reads it.
     *
     * @return how many tiles are held
     */
    public int heldTileCount() {
        return owners.size();
    }

    private Claim claimOf(Enemy enemy) {
        var claim = claims.get(enemy);
        if (claim == null) {
            claim = new Claim();
            claims.put(enemy, claim);
        }
        return claim;
    }

    /** Takes a tile no living enemy holds and reports whether the enemy owns it afterwards. */
    private boolean acquire(int x, int y, Enemy enemy) {
        final long key = key(x, y);
        final Enemy owner = owners.get(key);
        if (owner == null || owner.isDead()) {
            owners.put(key, enemy);
            return true;
        }
        return owner == enemy;
    }

    private void unhold(int x, int y, Enemy enemy) {
        owners.remove(key(x, y), enemy);
    }
}
