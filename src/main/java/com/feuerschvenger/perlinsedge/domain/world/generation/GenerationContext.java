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
        this.ground = new TileType[width * height];
        this.resources = new ResourceType[width * height];
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
     * Returns the factory the noise fields are sampled from.
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
     * Returns the terrain of every tile, null for a tile no pass has classified yet.
     *
     * @return the array of terrain types, which the caller may read and write
     */
    public TileType[] ground() {
        return ground;
    }

    /**
     * Returns the resource of every tile, null for a tile that has none.
     *
     * @return the array of resource types, which the caller may read and write
     */
    public ResourceType[] resources() {
        return resources;
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
     * Creates the random stream of a pass. The stream depends only on the seed and the salt, so a pass draws the same
     * numbers whatever runs before it, and adding or reordering another pass never moves them. Every call returns a
     * new stream that starts from the beginning.
     *
     * @param salt a constant of the pass, different for every pass that draws numbers
     * @return a new random stream
     */
    public RandomGenerator random(long salt) {
        return new SplittableRandom(Hashes.scramble(seed() ^ salt));
    }

    /**
     * Returns a value for a tile that is unrelated to the value of any neighbour, the seed or the salt, and does not
     * depend on how many numbers were drawn before. It serves per tile chances and the visual seeds of resources.
     *
     * @param x    the column
     * @param y    the row
     * @param salt a constant of the use, so two uses of the same tile are independent
     * @return a well spread 64 bit value
     */
    public long tileHash(int x, int y, long salt) {
        return Hashes.scramble(Hashes.mix(x, y) ^ (seed() + salt) * Hashes.GOLDEN_GAMMA);
    }

    /** Returns the index when it is inside the map or {@link #NONE}, and refuses it otherwise. */
    private int checkedIndexOrNone(int index) {
        if (index != NONE) {
            Objects.checkIndex(index, size());
        }
        return index;
    }
}
