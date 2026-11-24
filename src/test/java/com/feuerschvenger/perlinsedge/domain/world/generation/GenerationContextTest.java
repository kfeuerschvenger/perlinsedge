package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationContextTest {

    private static final int WIDTH = 6;
    private static final int HEIGHT = 4;
    private static final long SEED = 1L;
    private static final long SALT = 7L;
    private static final long OTHER_SALT = 8L;
    private static final int DRAWS = 5;
    private static final int COLUMN = 3;
    private static final int ROW = 2;
    private static final int BIG_WIDTH = 300;
    private static final int BIG_ROW = 299;
    private static final int FIFTH_ROW = 5;
    private static final int BELOW_NONE = -2;
    private static final int FAR_COLUMN = 15;
    private static final long OTHER_SEED = 20_261_111L;
    private static final long OTHER_SEED_SALT = 0x5EED;
    private static final long NEGATIVE_SEED = -5L;
    private static final long NEGATIVE_SEED_SALT = 9L;
    private static final int PAIR_TILE = 4;
    private static final long PAIR_SALT = 10L;

    private final GenerationContext context = TestContexts.of(WIDTH, HEIGHT);

    @Test
    void everyArrayHasOneSlotPerTileAndStartsEmpty() {
        assertEquals(WIDTH * HEIGHT, context.size());
        assertEquals(context.size(), context.ground().length);
        assertEquals(context.size(), context.resources().length);
        assertNull(context.ground()[0]);
        assertNull(context.resources()[context.size() - 1]);
        assertEquals(GenerationContext.NONE, context.spawnIndex());
        assertEquals(GenerationContext.NONE, context.spawnHint());
    }

    @Test
    void indexIsRowMajorAndInvertible() {
        assertEquals(ROW * WIDTH + COLUMN, context.index(COLUMN, ROW));
        assertEquals(COLUMN, context.xOf(context.index(COLUMN, ROW)));
        assertEquals(ROW, context.yOf(context.index(COLUMN, ROW)));
        final var big = TestContexts.of(BIG_WIDTH, BIG_WIDTH);
        assertEquals(BIG_WIDTH * BIG_WIDTH - 1, big.index(BIG_ROW, BIG_ROW));
    }

    @Test
    void theTilesInsideTheMapIncludingItsCornersAreOnTheMap() {
        assertTrue(context.isOnMap(0, 0));
        assertTrue(context.isOnMap(WIDTH - 1, 0));
        assertTrue(context.isOnMap(0, HEIGHT - 1));
        assertTrue(context.isOnMap(WIDTH - 1, HEIGHT - 1));
        assertTrue(context.isOnMap(COLUMN, ROW));
    }

    @Test
    void theTilesBeyondAnyEdgeAreOffTheMap() {
        assertFalse(context.isOnMap(-1, ROW));
        assertFalse(context.isOnMap(COLUMN, -1));
        assertFalse(context.isOnMap(WIDTH, ROW));
        assertFalse(context.isOnMap(COLUMN, HEIGHT));
        assertFalse(context.isOnMap(-1, -1));
        assertFalse(context.isOnMap(WIDTH, HEIGHT));
    }

    @Test
    void aFieldThatNoPassComputedIsRefused() {
        assertFalse(context.hasField(NoiseField.HEIGHT));

        final var error = assertThrows(IllegalStateException.class, () -> context.field(NoiseField.HEIGHT));

        assertTrue(error.getMessage().contains("HEIGHT"));
    }

    @Test
    void definingAFieldGivesItsArrayOnceAndKeepsWhatIsWrittenToIt() {
        final var values = context.defineField(NoiseField.MOISTURE);
        values[ROW] = 0.5F;

        assertTrue(context.hasField(NoiseField.MOISTURE));
        assertSame(values, context.field(NoiseField.MOISTURE));
        assertEquals(context.size(), values.length);
        assertFalse(context.hasField(NoiseField.TREE));
        assertThrows(IllegalStateException.class, () -> context.defineField(NoiseField.MOISTURE));
    }

    @Test
    void spawnIndexAndHintAcceptTheMapAndNoneOnly() {
        context.setSpawnIndex(context.size() - 1);
        context.setSpawnHint(0);

        assertEquals(context.size() - 1, context.spawnIndex());
        assertEquals(0, context.spawnHint());
        context.setSpawnIndex(GenerationContext.NONE);
        assertEquals(GenerationContext.NONE, context.spawnIndex());
        final var pastTheEnd = context.size();
        assertThrows(IndexOutOfBoundsException.class, () -> context.setSpawnIndex(pastTheEnd));
        assertThrows(IndexOutOfBoundsException.class, () -> context.setSpawnHint(BELOW_NONE));
    }

    @Test
    void aSaltGivesTheSameStreamEveryTimeAndAnotherSaltAnotherStream() {
        final var first = context.random(SALT);
        final var second = context.random(SALT);
        final var other = context.random(OTHER_SALT);

        assertEquals(first.nextLong(), second.nextLong());
        assertNotEquals(context.random(SALT).nextLong(), other.nextLong());
    }

    @Test
    void aStreamDoesNotDependOnWhatOtherStreamsDrew() {
        final var reference = context.random(SALT).nextLong();
        final var busy = context.random(OTHER_SALT);
        for (var i = 0; i < DRAWS; i++) {
            busy.nextLong();
        }

        assertEquals(reference, context.random(SALT).nextLong());
    }

    @Test
    void theStreamDependsOnTheSeed() {
        final var other = TestContexts.of(MapType.DESERT, SEED + 1, WIDTH, HEIGHT, TestContexts.FLAT);

        assertNotEquals(context.random(SALT).nextLong(), other.random(SALT).nextLong());
    }

    @Test
    void tileHashesArePinned() {
        assertEquals(-8_232_832_824_513_865_057L, context.tileHash(0, 0, 0L));
        assertEquals(7_813_936_339_282_225_672L, context.tileHash(COLUMN, FIFTH_ROW, SALT));
        final var other = TestContexts.of(MapType.DESERT, OTHER_SEED, WIDTH, HEIGHT, TestContexts.FLAT);
        assertEquals(-6_226_927_798_593_591_928L, other.tileHash(FAR_COLUMN, ROW, OTHER_SEED_SALT));
        final var negative = TestContexts.of(MapType.DESERT, NEGATIVE_SEED, WIDTH, HEIGHT, TestContexts.FLAT);
        assertEquals(7_212_865_183_995_589_458L, negative.tileHash(1, 1, NEGATIVE_SEED_SALT));
    }

    @Test
    void tileHashesVaryWithTheTileAndTheSaltAndRepeatForTheSameInput() {
        final var base = context.tileHash(1, 2, SALT);

        assertEquals(base, context.tileHash(1, 2, SALT));
        assertNotEquals(base, context.tileHash(2, 1, SALT));
        assertNotEquals(base, context.tileHash(1, 2, OTHER_SALT));
    }

    @Test
    void aSeedAndASaltThatTradeOneForTheOtherGiveDifferentHashes() {
        final var next = TestContexts.of(MapType.DESERT, SEED + 1, WIDTH, HEIGHT, TestContexts.FLAT);

        assertNotEquals(context.tileHash(PAIR_TILE, PAIR_TILE, PAIR_SALT),
                next.tileHash(PAIR_TILE, PAIR_TILE, PAIR_SALT - 1));
        assertNotEquals(context.tileHash(PAIR_TILE, PAIR_TILE, PAIR_SALT),
                next.tileHash(PAIR_TILE, PAIR_TILE, PAIR_SALT));
    }

    @Test
    void theArraysOfTheGroundAndTheResourcesAreReachedByIndexAndReturnedAsCopies() {
        context.setGround(ROW, TileType.SAND);
        context.setResource(ROW, ResourceType.TREE);
        final var groundCopy = context.ground();
        final var resourcesCopy = context.resources();
        groundCopy[ROW] = TileType.ROCKY_GROUND;
        resourcesCopy[ROW] = null;

        assertEquals(TileType.SAND, context.groundAt(ROW));
        assertEquals(ResourceType.TREE, context.resourceAt(ROW));
        assertNull(context.groundAt(0));
        assertNull(context.resourceAt(0));
    }

    @Test
    void noTileIsReservedUntilAPassReservesIt() {
        for (var index = 0; index < context.size(); index++) {
            assertFalse(context.isReserved(index));
        }

        context.reserve(ROW);

        assertTrue(context.isReserved(ROW));
        assertFalse(context.isReserved(ROW + 1));
        assertFalse(context.isReserved(0));
    }

    @Test
    void reservingATileTwiceKeepsItReserved() {
        context.reserve(ROW);
        context.reserve(ROW);

        assertTrue(context.isReserved(ROW));
    }

    @Test
    void reservingATileOutsideTheMapIsRefused() {
        final var size = context.size();

        assertThrows(IndexOutOfBoundsException.class, () -> context.reserve(size));
        assertThrows(IndexOutOfBoundsException.class, () -> context.isReserved(-1));
    }

    @Test
    void theSiteListStartsEmptyAndIsTheSameLiveListEveryTime() {
        assertEquals(0, context.sites().size());

        context.sites().add(ROW);

        assertSame(context.sites(), context.sites());
        assertEquals(1, context.sites().size());
        assertEquals(ROW, context.sites().get(0));
    }

    @Test
    void refusesMissingArguments() {
        final var request = new MapRequest(MapType.DESERT, SEED, WIDTH, HEIGHT);

        assertThrows(NullPointerException.class, () -> new GenerationContext(null, TestContexts.FLAT));
        assertThrows(NullPointerException.class, () -> new GenerationContext(request, null));
    }
}
