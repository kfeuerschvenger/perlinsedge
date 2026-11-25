package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;

import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Which creature holds which tile, so that every creature stands on a tile of its own. A creature holds the tile it
 * stands on and, while it moves, the tile it is moving to: the target is reserved when the move starts and the old tile
 * is let go once the creature has arrived, so two creatures never hold the same tile at any moment.
 *
 * <p>Movement strategies ask {@link #isFreeFor(Creature, int, int)} before choosing a tile and
 * {@link #tryReserve(Creature, int, int)} to take it. {@link #sync(Creature)} brings the holds of a creature in line
 * with the tiles it reports, which is cheap when nothing changed, so a move that was made without a reservation is
 * still recorded. A tile that another creature held when {@code sync} tried to take it is tried again on the next
 * {@code sync}, so a creature never believes it holds a tile it lost. The queries allocate nothing: the tiles live in
 * a primitive open-addressing table, and the price list of a search is one object per creature.</p>
 */
public final class CreatureOccupancy {

    /** The tiles one creature holds, as the occupancy last recorded them. */
    private static final class Claim {
        private boolean recorded;
        private int currentX;
        private int currentY;
        private int targetX;
        private int targetY;
        /** Whether the creature actually owns the tile it stands on, and the one it moves to. */
        private boolean currentHeld;
        private boolean targetHeld;
        /** The price list handed to searches of this creature, created on the first search. */
        private HeldTilePenalty penalty;

        /** Tells whether the recorded holds are exactly the tiles a creature stands on and moves to now. */
        private boolean isRecordedAt(int currentTileX, int currentTileY, int targetTileX, int targetTileY) {
            if (!recorded) {
                return false;
            }
            return currentX == currentTileX && currentY == currentTileY
                    && targetX == targetTileX && targetY == targetTileY;
        }
    }

    /** Prices the tiles other creatures hold for one creature, reading the occupancy as it is when the search runs. */
    private final class HeldTilePenalty implements TilePenalty {
        private final Creature creature;
        private double cost;

        private HeldTilePenalty(Creature creature) {
            this.creature = creature;
        }

        @Override
        public double extraCost(int x, int y) {
            return isFreeFor(creature, x, y) ? 0 : cost;
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
        private Creature[] owners = new Creature[INITIAL_CAPACITY];
        private int size;

        private static int slot(long key, int mask) {
            return (int) ((key * HASH_MULTIPLIER) >>> HASH_SHIFT) & mask;
        }

        Creature get(long key) {
            final int mask = keys.length - 1;
            for (int i = slot(key, mask); owners[i] != null; i = (i + 1) & mask) {
                if (keys[i] == key) {
                    return owners[i];
                }
            }
            return null;
        }

        void put(long key, Creature owner) {
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

        /** Removes the tile only when the given creature is its owner. */
        void remove(long key, Creature owner) {
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
            final Creature[] oldOwners = owners;
            keys = new long[oldKeys.length * GROWTH_FACTOR];
            owners = new Creature[oldOwners.length * GROWTH_FACTOR];
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
    private final Map<Creature, Claim> claims = new IdentityHashMap<>();

    /** A tile key holds the column in its high 32 bits and the row in its low 32 bits. */
    private static final int KEY_X_SHIFT = 32;
    private static final long KEY_Y_MASK = 0xFFFF_FFFFL;

    private static long key(int x, int y) {
        return ((long) x << KEY_X_SHIFT) | (y & KEY_Y_MASK);
    }

    /**
     * Tells whether a creature may move to a tile.
     *
     * @param creature the creature asking
     * @param x        tile column
     * @param y        tile row
     * @return true when no other living creature holds the tile
     */
    public boolean isFreeFor(Creature creature, int x, int y) {
        final Creature owner = owners.get(key(x, y));
        return owner == null || owner == creature || owner.isDead();
    }

    /**
     * Prices the tiles other creatures hold, so a path of this creature goes around them when a detour costs less than
     * the penalty and waits behind them otherwise, as in a corridor. The price list is one object per creature that is
     * reused by every search, so asking for it allocates nothing after the first time and the list must not be kept
     * past the search it was asked for.
     *
     * @param creature the creature that searches, whose own tiles cost nothing
     * @param penalty  extra cost of entering a tile another creature holds
     * @return the price list, which reads this occupancy as it is when the search runs
     */
    public TilePenalty penaltyFor(Creature creature, double penalty) {
        final var claim = claimOf(creature);
        if (claim.penalty == null) {
            claim.penalty = new HeldTilePenalty(creature);
        }
        claim.penalty.cost = penalty;
        return claim.penalty;
    }

    /**
     * Finds the creature that holds a tile, as the tile it stands on or as the tile it is moving to.
     *
     * @param x tile column
     * @param y tile row
     * @return the living holder, or null when the tile is free
     */
    public Creature ownerOf(int x, int y) {
        final Creature owner = owners.get(key(x, y));
        return owner == null || owner.isDead() ? null : owner;
    }

    /**
     * Reserves a tile as the target of a creature, letting go of the target it reserved before. The tile the creature
     * stands on stays held.
     *
     * <p>The caller must set the target tile of the creature to this tile in the same update, before the next
     * {@link #sync(Creature)}: the occupancy follows the tiles the creature reports, and a reservation the creature
     * does not report is let go again by that sync. A call that returns false changes nothing.</p>
     *
     * @param creature the creature that is about to move
     * @param x        tile column
     * @param y        tile row
     * @return true when the tile is now held by the creature, false when another creature holds it
     */
    public boolean tryReserve(Creature creature, int x, int y) {
        sync(creature);
        if (!isFreeFor(creature, x, y)) {
            return false;
        }
        final var claim = claims.get(creature);
        if (claim.targetX != claim.currentX || claim.targetY != claim.currentY) {
            unhold(claim.targetX, claim.targetY, creature);
        }
        claim.targetX = x;
        claim.targetY = y;
        claim.targetHeld = true;
        owners.put(key(x, y), creature);
        return true;
    }

    /**
     * Brings the holds of a creature in line with the tile it stands on and the tile it moves to. A tile that another
     * creature already holds stays with that creature, and the creature that did not win it tries again on the next
     * call.
     *
     * @param creature the creature to record
     */
    public void sync(Creature creature) {
        final var claim = claimOf(creature);
        final int currentX = creature.getCurrentTileX();
        final int currentY = creature.getCurrentTileY();
        final int targetX = creature.getTargetTileX();
        final int targetY = creature.getTargetTileY();
        if (claim.isRecordedAt(currentX, currentY, targetX, targetY)) {
            if (!claim.currentHeld) {
                claim.currentHeld = acquire(currentX, currentY, creature);
            }
            if (!claim.targetHeld) {
                claim.targetHeld = acquire(targetX, targetY, creature);
            }
            return;
        }
        if (claim.recorded) {
            unhold(claim.currentX, claim.currentY, creature);
            unhold(claim.targetX, claim.targetY, creature);
        }
        claim.recorded = true;
        claim.currentX = currentX;
        claim.currentY = currentY;
        claim.targetX = targetX;
        claim.targetY = targetY;
        claim.currentHeld = acquire(currentX, currentY, creature);
        claim.targetHeld = acquire(targetX, targetY, creature);
    }

    /**
     * Lets go of every tile a creature holds, as when it dies or leaves the map.
     *
     * @param creature the creature to forget
     */
    public void release(Creature creature) {
        final var claim = claims.remove(creature);
        if (claim != null && claim.recorded) {
            unhold(claim.currentX, claim.currentY, creature);
            unhold(claim.targetX, claim.targetY, creature);
        }
    }

    /** Forgets every creature. */
    public void clear() {
        owners.clear();
        claims.clear();
    }

    /**
     * Forgets every creature and records the living ones of a collection.
     *
     * @param creatures the creatures on the map, null for none
     */
    public void rebuild(Collection<? extends Creature> creatures) {
        clear();
        if (creatures == null) {
            return;
        }
        for (Creature creature : creatures) {
            if (!creature.isDead()) {
                sync(creature);
            }
        }
    }

    /**
     * Counts the tiles held. This is a seam for the tests, which check that a release leaves nothing behind where
     * {@link #ownerOf(int, int)} cannot tell a released tile from one held by a dead creature; the game never reads it.
     *
     * @return how many tiles are held
     */
    public int heldTileCount() {
        return owners.size();
    }

    private Claim claimOf(Creature creature) {
        var claim = claims.get(creature);
        if (claim == null) {
            claim = new Claim();
            claims.put(creature, claim);
        }
        return claim;
    }

    /** Takes a tile no living creature holds and reports whether the creature owns it afterwards. */
    private boolean acquire(int x, int y, Creature creature) {
        final long key = key(x, y);
        final Creature owner = owners.get(key);
        if (owner == null || owner.isDead()) {
            owners.put(key, creature);
            return true;
        }
        return owner == creature;
    }

    private void unhold(int x, int y, Creature creature) {
        owners.remove(key(x, y), creature);
    }
}
