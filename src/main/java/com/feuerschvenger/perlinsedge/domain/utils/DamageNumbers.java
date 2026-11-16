package com.feuerschvenger.perlinsedge.domain.utils;

import java.util.Random;

/**
 * The floating numbers that show a hit or a heal: each rises a few art pixels over under a second and fades out.
 * The list is a fixed pool, so it never grows: when every slot is in use a new number takes the place of the oldest.
 * All times are on the game clock, which stands still while the game is paused, so a pause freezes the numbers where
 * they are. The class knows nothing about how they are drawn, the renderer reads the slots.
 */
public final class DamageNumbers {

    /** What a number reports, which decides its colour. */
    public enum Kind {
        /** Damage the player dealt to a resource. */
        RESOURCE,
        /** Damage the player dealt to an enemy. */
        ENEMY,
        /** Damage the player took. */
        PLAYER,
        /** Health the player restored. */
        HEAL
    }

    /** Seconds a number lives. */
    public static final double LIFETIME_SECONDS = 0.8;
    /** How far a number rises over its life, in art pixels. */
    public static final double RISE_ART_PIXELS = 12.0;
    /** The widest a number may be shifted to either side when it appears, in art pixels. */
    public static final double MAX_OFFSET_ART_PIXELS = 6.0;
    /** The number of slots, which is the most numbers on screen at once. */
    public static final int CAPACITY = 48;

    /** One slot of the pool. A slot is reused, never replaced. */
    public static final class Entry {
        private Kind kind;
        private int value;
        private String text = "";
        private double tileX;
        private double tileY;
        private double startTime = Double.NEGATIVE_INFINITY;
        private double offsetX;

        /** @return what the number reports */
        public Kind getKind() { return kind; }

        /** @return the amount shown, always positive */
        public int getValue() { return value; }

        /** @return the text to draw: the amount, with a plus sign in front of a heal. Built once, when the number starts. */
        public String getText() { return text; }

        /** @return the column of the point the number rises from, in tiles */
        public double getTileX() { return tileX; }

        /** @return the row of the point the number rises from, in tiles */
        public double getTileY() { return tileY; }

        /** @return the shift to the side the number was given when it appeared, in art pixels */
        public double getOffsetX() { return offsetX; }

        /**
         * Tells whether the number is on screen at a game time.
         *
         * @param gameTime the game time in seconds
         * @return true from the moment it appears until its lifetime is over
         */
        public boolean isActive(double gameTime) {
            final double elapsed = gameTime - startTime;
            return elapsed >= 0 && elapsed < LIFETIME_SECONDS;
        }

        /**
         * Gives how far through its life the number is.
         *
         * @param gameTime the game time in seconds
         * @return 0 when it appears, rising to 1 when it is gone
         */
        public double progress(double gameTime) {
            return Math.max(0.0, Math.min(1.0, (gameTime - startTime) / LIFETIME_SECONDS));
        }

        /**
         * Gives how high the number has risen.
         *
         * @param gameTime the game time in seconds
         * @return art pixels above the point it started from
         */
        public double rise(double gameTime) {
            return RISE_ART_PIXELS * progress(gameTime);
        }

        /**
         * Gives the opacity of the number: fully opaque for the first part of its life, then fading out.
         *
         * @param gameTime the game time in seconds
         * @return 1 down to 0
         */
        public double opacity(double gameTime) {
            final double p = progress(gameTime);
            return p < FADE_START ? 1.0 : 1.0 - (p - FADE_START) / (1.0 - FADE_START);
        }
    }

    /** The share of the life after which the number starts to fade. */
    private static final double FADE_START = 0.4;

    private final Entry[] slots = new Entry[CAPACITY];
    private final Random random;

    /** Creates an empty pool. */
    public DamageNumbers() {
        this(new Random());
    }

    /**
     * Creates an empty pool with its own source of the random shift to the side.
     *
     * @param random the source of the shifts
     */
    public DamageNumbers(Random random) {
        this.random = random;
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new Entry();
        }
    }

    /**
     * Starts a number. The amount is rounded to whole points and a number that rounds to nothing is not shown.
     *
     * @param kind     what the number reports
     * @param amount   the damage or the health restored, as a positive amount
     * @param tileX    column of the point the number rises from, in tiles
     * @param tileY    row of the point the number rises from, in tiles
     * @param gameTime the game time in seconds
     */
    public void spawn(Kind kind, double amount, double tileX, double tileY, double gameTime) {
        final int value = (int) Math.round(amount);
        if (value <= 0) {
            return;
        }
        final Entry entry = slotFor(gameTime);
        entry.kind = kind;
        entry.value = value;
        entry.text = kind == Kind.HEAL ? "+" + value : Integer.toString(value);
        entry.tileX = tileX;
        entry.tileY = tileY;
        entry.startTime = gameTime;
        // A little to either side, so numbers that appear one after the other on the same spot do not overlap
        entry.offsetX = (random.nextDouble() * 2.0 - 1.0) * MAX_OFFSET_ART_PIXELS;
    }

    /** The first slot that is not in use, or the oldest when all are. */
    private Entry slotFor(double gameTime) {
        Entry oldest = slots[0];
        for (Entry slot : slots) {
            if (!slot.isActive(gameTime)) {
                return slot;
            }
            if (slot.startTime < oldest.startTime) {
                oldest = slot;
            }
        }
        return oldest;
    }

    /**
     * Gives the number of slots, which is {@link #CAPACITY}. A slot is on screen when {@link Entry#isActive} says so.
     *
     * @return the slot count
     */
    public int size() {
        return slots.length;
    }

    /**
     * Gives one slot.
     *
     * @param index from 0 to {@link #size()} - 1
     * @return the slot
     */
    public Entry get(int index) {
        return slots[index];
    }

    /**
     * Counts the numbers on screen.
     *
     * @param gameTime the game time in seconds
     * @return how many slots are in use at that time
     */
    public int countActive(double gameTime) {
        int count = 0;
        for (Entry slot : slots) {
            if (slot.isActive(gameTime)) {
                count++;
            }
        }
        return count;
    }

    /** Removes every number, for when the world is rebuilt. */
    public void clear() {
        for (Entry slot : slots) {
            slot.startTime = Double.NEGATIVE_INFINITY;
        }
    }
}
