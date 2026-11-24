package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
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
        assertThrows(IndexOutOfBoundsException.class, () -> context.setSpawnIndex(context.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> context.setSpawnHint(-2));
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
        assertEquals(258_863_698_125_685_209L, context.tileHash(0, 0, 0L));
        assertEquals(-2_387_521_308_164_195_811L, context.tileHash(3, 5, SALT));
        final var other = TestContexts.of(MapType.DESERT, 20_261_111L, WIDTH, HEIGHT, TestContexts.FLAT);
        assertEquals(1_246_064_204_876_323_019L, other.tileHash(15, 2, 0x5EED));
        final var negative = TestContexts.of(MapType.DESERT, -5L, WIDTH, HEIGHT, TestContexts.FLAT);
        assertEquals(7_990_827_086_467_904_430L, negative.tileHash(1, 1, 9L));
    }

    @Test
    void tileHashesVaryWithTheTileAndTheSaltAndRepeatForTheSameInput() {
        final var base = context.tileHash(1, 2, SALT);

        assertEquals(base, context.tileHash(1, 2, SALT));
        assertNotEquals(base, context.tileHash(2, 1, SALT));
        assertNotEquals(base, context.tileHash(1, 2, OTHER_SALT));
    }

    @Test
    void refusesMissingArguments() {
        final var request = new MapRequest(MapType.DESERT, SEED, WIDTH, HEIGHT);

        assertThrows(NullPointerException.class, () -> new GenerationContext(null, TestContexts.FLAT));
        assertThrows(NullPointerException.class, () -> new GenerationContext(request, null));
    }
}
