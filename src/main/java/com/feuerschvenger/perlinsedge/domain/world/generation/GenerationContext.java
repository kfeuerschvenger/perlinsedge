package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.utils.Hashes;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * The state of one generation: flat arrays that the passes fill in turn and that are turned into tiles at the end. A
 * tile at column {@code x} and row {@code y} lives at index {@code y * width + x} in every array, so a loop that runs
 * the rows outside and the columns inside walks the memory in order and creates no object per tile.
 * <p>
 * A context belongs to one generation and one thread.
 * <p>
 * Ownership of the arrays: the noise fields are handed out as the live arrays, because the passes read and write them
 * once per tile. The ground and the resources are reached one tile at a time through {@link #groundAt(int)},
 * {@link #setGround(int, TileType)}, {@link #resourceAt(int)} and {@link #setResource(int, ResourceType)}, and
 * {@link #ground()} and {@link #resources()} return copies, so no caller holds the internal arrays.
 */
public final class GenerationContext {

    /** The value of a spawn index or a spawn hint that has not been set. */
    public static final int NONE = -1;

    private final MapRequest request;
    private final NoiseSourceFactory noiseFactory;
    private final int width;
    private final int height;
    private final float[][] fields = new float[NoiseField.values().length][];
    private final TileType[] ground;
    private final ResourceType[] resources;
    private int spawnIndex = NONE;
    private int spawnHint = NONE;
    private final long seedKey;
    private int passOccurrence;

    /**
     * Creates the context of a request, with no field computed, no ground and no resource.
     *
     * @param request      what to generate
     * @param noiseFactory builds the noise sources of the noise fields, which keeps the noise library outside the
     *                     domain
     */
    public GenerationContext(MapRequest request, NoiseSourceFactory noiseFactory) {
        this.request = Objects.requireNonNull(request, "request");
        this.noiseFactory = Objects.requireNonNull(noiseFactory, "noiseFactory");
        this.width = request.width();
        this.height = request.height();
        this.seedKey = Hashes.scramble(request.seed() + Hashes.GOLDEN_GAMMA);
        this.ground = new TileType[request.tileCount()];
        this.resources = new ResourceType[request.tileCount()];
    }

    /**
     * Returns the request this context was created for.
     *
     * @return the request
     */
    public MapRequest request() {
        return request;
    }

    /**
     * Returns the kind of map being generated.
     *
     * @return the map type
     */
    public MapType type() {
        return request.type();
    }

    /**
     * Returns the seed of the map.
     *
     * @return the seed
     */
    public long seed() {
        return request.seed();
    }

    /**
     * Returns the number of columns.
     *
     * @return the width in tiles
     */
    public int width() {
        return width;
    }

    /**
     * Returns the number of rows.
     *
     * @return the height in tiles
     */
    public int height() {
        return height;
    }

    /**
     * Returns the number of tiles, which is the length of every array.
     *
     * @return width times height
     */
    public int size() {
        return ground.length;
    }

    /**
     * Returns the factory the noise fields are sampled from. Only {@code NoiseFieldsPass} may call this: every other
     * pass reads the arrays that pass filled, so the noise library stays in one place.
     *
     * @return the noise source factory
     */
    public NoiseSourceFactory noiseFactory() {
        return noiseFactory;
    }

    /**
     * Returns the array index of a tile. The coordinates are not checked, because this is called in the hot loops.
     *
     * @param x the column
     * @param y the row
     * @return {@code y * width + x}
     */
    public int index(int x, int y) {
        return y * width + x;
    }

    /**
     * Returns the column of an array index.
     *
     * @param index an index into the arrays
     * @return the column
     */
    public int xOf(int index) {
        return index % width;
    }

    /**
     * Returns the row of an array index.
     *
     * @param index an index into the arrays
     * @return the row
     */
    public int yOf(int index) {
        return index / width;
    }

    /**
     * Tells whether a pass has computed a noise field.
     *
     * @param field the field
     * @return true when {@link #field(NoiseField)} would return its array
     */
    public boolean hasField(NoiseField field) {
        return fields[field.ordinal()] != null;
    }

    /**
     * Returns the values of a noise field that a pass computed.
     *
     * @param field the field
     * @return the array of the field, indexed like every other array, that the caller may read and write
     * @throws IllegalStateException when no pass has computed the field
     */
    public float[] field(NoiseField field) {
        final var values = fields[field.ordinal()];
        if (values == null) {
            throw new IllegalStateException("no pass computed the field " + field);
        }
        return values;
    }

    /**
     * Creates the array of a noise field, filled with zeros, for the pass that computes it.
     *
     * @param field the field
     * @return the new array, which {@link #field(NoiseField)} returns from now on
     * @throws IllegalStateException when the field already exists
     */
    public float[] defineField(NoiseField field) {
        if (hasField(field)) {
            throw new IllegalStateException("the field " + field + " was already computed");
        }
        final var values = new float[size()];
        fields[field.ordinal()] = values;
        return values;
    }

    /**
     * Returns the terrain of a tile.
     *
     * @param index an index into the arrays
     * @return the terrain, null for a tile no pass has classified yet
     */
    public TileType groundAt(int index) {
        return ground[index];
    }

    /**
     * Sets the terrain of a tile.
     *
     * @param index an index into the arrays
     * @param type  the terrain, or null to leave the tile unclassified
     */
    public void setGround(int index, TileType type) {
        ground[index] = type;
    }

    /**
     * Returns a copy of the terrain of every tile, so it is for reading outside the hot loops, such as in a test.
     *
     * @return a new array with null for a tile no pass has classified yet
     */
    public TileType[] ground() {
        return ground.clone();
    }

    /**
     * Returns the resource of a tile.
     *
     * @param index an index into the arrays
     * @return the resource, null for a tile that has none
     */
    public ResourceType resourceAt(int index) {
        return resources[index];
    }

    /**
     * Sets the resource of a tile.
     *
     * @param index an index into the arrays
     * @param type  the resource, or null to remove it
     */
    public void setResource(int index, ResourceType type) {
        resources[index] = type;
    }

    /**
     * Returns a copy of the resource of every tile, so it is for reading outside the hot loops, such as in a test.
     *
     * @return a new array with null for a tile that has no resource
     */
    public ResourceType[] resources() {
        return resources.clone();
    }

    /**
     * Returns the index of the tile where the player starts.
     *
     * @return the index, or {@link #NONE} when no pass chose one
     */
    public int spawnIndex() {
        return spawnIndex;
    }

    /**
     * Sets the tile where the player starts.
     *
     * @param index an index into the arrays, or {@link #NONE}
     * @throws IndexOutOfBoundsException when the index is neither {@link #NONE} nor inside the map
     */
    public void setSpawnIndex(int index) {
        spawnIndex = checkedIndexOrNone(index);
    }

    /**
     * Returns the tile a pass asked the spawn to be near, such as the middle of a clearing.
     *
     * @return the index, or {@link #NONE} when no pass asked for one
     */
    public int spawnHint() {
        return spawnHint;
    }

    /**
     * Sets the tile the spawn should be near.
     *
     * @param index an index into the arrays, or {@link #NONE}
     * @throws IndexOutOfBoundsException when the index is neither {@link #NONE} nor inside the map
     */
    public void setSpawnHint(int index) {
        spawnHint = checkedIndexOrNone(index);
    }

    /**
     * Records which occurrence of its name the pass about to run is, so {@link #random(long)} gives two instances of
     * the same pass different streams. The pipeline calls it before every pass, and with 0 once the run ends.
     *
     * @param occurrence how many passes with the same name ran before this one, 0 for the first
     */
    void beginPass(int occurrence) {
        passOccurrence = occurrence;
    }

    /**
     * Creates the random stream of a pass. The stream depends on the seed, the salt and which occurrence of its name
     * the running pass is, so a pass draws the same numbers whatever other passes run before it, and adding or
     * reordering a pass of another name never moves them. The first occurrence of a pass gets the plain stream of its
     * salt, and so does any call made outside a pipeline run, including after a run that finished or failed; each later
     * occurrence of the same pass within a run gets a stream of its own. Every call returns a new stream that starts
     * from the beginning.
     *
     * @param salt a constant of the pass, different for every pass that draws numbers
     * @return a new random stream
     */
    public RandomGenerator random(long salt) {
        return new SplittableRandom(Hashes.scramble((seed() ^ salt) + passOccurrence * Hashes.GOLDEN_GAMMA));
    }

    /**
     * Returns a value for a tile that is unrelated to the value of any neighbour, and does not depend on how many
     * numbers were drawn before. The seed and the salt are scrambled separately before they are combined, so no two
     * pairs of seed and salt that differ in both give the same value for a tile by cancelling each other out. It
     * serves per tile chances and the visual seeds of resources.
     *
     * @param x    the column
     * @param y    the row
     * @param salt a constant of the use, so two uses of the same tile are independent
     * @return a well spread 64 bit value
     */
    public long tileHash(int x, int y, long salt) {
        final var saltKey = Hashes.scramble(salt - Hashes.GOLDEN_GAMMA);
        return Hashes.scramble(Hashes.scramble(Hashes.mix(x, y) ^ seedKey) ^ saltKey);
    }

    /** Returns the index when it is inside the map or {@link #NONE}, and refuses it otherwise. */
    private int checkedIndexOrNone(int index) {
        if (index != NONE) {
            Objects.checkIndex(index, size());
        }
        return index;
    }
}
