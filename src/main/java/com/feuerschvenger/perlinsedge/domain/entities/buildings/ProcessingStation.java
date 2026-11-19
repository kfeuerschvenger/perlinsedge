package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.crafting.processing.FuelMode;
import com.feuerschvenger.perlinsedge.domain.crafting.processing.ProcessingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.processing.StationProfile;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * A building that turns the items loaded into it into others over time, such as the furnace, the stone oven and the
 * campfire. One engine serves them all, and a {@link StationProfile} says what each one processes and burns.
 * <p>
 * The player loads one unit at a time. The caps belong to a role, not to an item: the inputs share one cap and the
 * fuels share another, so a furnace holds at most 20 units of iron ore and logs together. The station works on one unit at a time,
 * and the unit being processed stays loaded until its time is up: only then are the input and the fuel it costs used
 * up and the product made. The products wait in {@link #drainProducts()} for the caller to put them on the ground.
 * <p>
 * A recipe that needs fuel does not run without it. A station that has started a unit finishes it before it looks for
 * another, and waits while the fuel is out.
 */
public abstract class ProcessingStation extends Building {
    /** The name of the role of the items a recipe processes. */
    private static final String ROLE_INPUT = "Input";
    /** The name of the role of the items the station burns. */
    private static final String ROLE_FUEL = "Fuel";
    /** What is left of a slice of time below which a unit counts as finished, absorbing the error of adding fractions. */
    private static final double COMPLETION_EPSILON = 1e-9;

    /** What loading one unit of an item did. */
    public enum LoadResult {
        /** One unit went in. */
        LOADED,
        /** The station holds as much of the item as it can. */
        FULL,
        /** The station does not take the item. */
        REJECTED
    }

    private final StationProfile profile;
    /** The units held of each input and each fuel. */
    private final Map<ItemType, Integer> stock = new EnumMap<>(ItemType.class);
    private final List<Item> products = new ArrayList<>();
    /** The recipe being worked on, or null while the station has not started a unit. */
    private ProcessingRecipe active;
    /** Seconds spent on the unit being worked on. */
    private double progress;
    /** Products still paid for by a unit of fuel that was already used up, for {@link FuelMode#PER_OUTPUT}. */
    private int paidOutputs;
    /** Seconds the unit of fuel being burnt lasts for, for {@link FuelMode#BURNING}. */
    private double burnRemaining;

    /**
     * Creates a station.
     *
     * @param tileX   tile X
     * @param tileY   tile Y
     * @param type    the kind of building
     * @param isSolid whether the station blocks movement
     * @param profile what the station processes and burns
     */
    protected ProcessingStation(int tileX, int tileY, BuildingType type, boolean isSolid, StationProfile profile) {
        super(tileX, tileY, type, isSolid, true);
        this.profile = profile;
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean onInteract(Tile tile) {
        return true;
    }

    // ==================================================================
    //  Loading
    // ==================================================================

    /**
     * Tells whether the station takes an item, as input or as fuel.
     *
     * @param type the item
     * @return true when a unit of it can be loaded
     */
    public boolean accepts(ItemType type) {
        return profile.capOf(type) > 0;
    }

    /**
     * Loads one unit of an item.
     *
     * @param type the item
     * @return what happened to the unit
     */
    public LoadResult load(ItemType type) {
        final int cap = profile.capOf(type);
        if (cap == 0) {
            return LoadResult.REJECTED;
        }
        if (countInRole(type) >= cap) {
            return LoadResult.FULL;
        }
        stock.merge(type, 1, Integer::sum);
        return LoadResult.LOADED;
    }

    /**
     * Gives the units of an item that are loaded.
     *
     * @param type the item
     * @return the units held, 0 for an item that is not loaded
     */
    public int count(ItemType type) {
        return stock.getOrDefault(type, 0);
    }

    /**
     * Gives how many units the station holds at most of the role an item plays, input or fuel.
     *
     * @param type the item
     * @return the cap shared by every item of that role, 0 for an item the station does not take
     */
    public int capOf(ItemType type) {
        return profile.capOf(type);
    }

    /**
     * Gives the units loaded of every item that plays the same role as an item, which is what the cap limits.
     *
     * @param type the item
     * @return the units of the inputs when the item is an input, of the fuels when it is a fuel, 0 otherwise
     */
    public int countInRole(ItemType type) {
        if (profile.isInput(type)) {
            return totalOf(profile.inputs());
        }
        return profile.isFuel(type) ? totalOf(profile.fuels()) : 0;
    }

    /**
     * Names the role an item plays in the station.
     *
     * @param type the item
     * @return {@code Input} or {@code Fuel}
     */
    public String roleName(ItemType type) {
        return profile.isInput(type) ? ROLE_INPUT : ROLE_FUEL;
    }

    private int totalOf(List<ItemType> types) {
        return types.stream().mapToInt(this::count).sum();
    }

    /**
     * Describes what is loaded, for the hint shown when the player clicks the station with nothing to load, such as
     * {@code Input 5/20, Fuel 3/20}.
     *
     * @return the units loaded of the inputs and of the fuels with their caps, or what the station takes when it is
     * empty
     */
    public String statusHint() {
        final var inputs = totalOf(profile.inputs());
        final var fuels = totalOf(profile.fuels());
        if (inputs == 0 && fuels == 0) {
            final var accepted = new StringJoiner(", ");
            profile.accepted().forEach((ItemType type) -> accepted.add(type.getDisplayName()));
            return "Empty, takes " + accepted;
        }
        return ROLE_INPUT + " " + inputs + "/" + profile.inputCap() + ", " + ROLE_FUEL + " " + fuels + "/"
                + profile.fuelCap();
    }

    // ==================================================================
    //  State
    // ==================================================================

    /**
     * Tells whether the station shows its fire. A station that burns fuel with time is lit while it has fuel, whether
     * it cooks or not, and any other is lit while it works.
     *
     * @return true when the lit art and the light apply
     */
    public boolean isLit() {
        if (profile.fuelMode() == FuelMode.BURNING) {
            return hasBurningFuel();
        }
        return isWorking();
    }

    /**
     * Tells whether the station is processing a unit, which needs something to process and the fuel for it.
     *
     * @return true while a unit is being worked on or is ready to start
     */
    public boolean isWorking() {
        return active != null ? canRun(active) : selectRecipe() != null;
    }

    // ==================================================================
    //  Time
    // ==================================================================

    /**
     * Lets time pass. The slice may be as long as it likes: a unit that finishes inside it is made, the next one
     * starts with what is left, and fuel that burns out is replaced from the loaded fuel.
     *
     * @param seconds the time that passed, not negative
     */
    public void advance(double seconds) {
        var left = seconds;
        while (left > 0) {
            igniteIfNeeded();
            left -= work(left);
        }
    }

    /** Works for as much of a slice as nothing changes in, and gives the time it took. */
    private double work(double left) {
        if (active == null) {
            active = selectRecipe();
            progress = 0;
        }
        final var cooking = active != null && canRun(active);
        final var burning = profile.fuelMode() == FuelMode.BURNING && burnRemaining > 0;
        var slice = left;
        if (burning) {
            slice = Math.min(slice, burnRemaining);
        }
        if (cooking) {
            slice = Math.min(slice, active.seconds() - progress);
        }
        if (burning) {
            burnRemaining -= slice;
        }
        if (cooking) {
            progress += slice;
            finishIfDone();
        }
        return slice;
    }

    private void finishIfDone() {
        if (progress < active.seconds() - COMPLETION_EPSILON) {
            return;
        }
        stock.merge(active.input(), -1, Integer::sum);
        payFuelFor(active);
        products.add(new Item(active.output(), 1));
        active = null;
        progress = 0;
    }

    // ==================================================================
    //  Fuel
    // ==================================================================

    /** A unit of fuel is lit as soon as the last one is out, even when nothing is waiting to be cooked. */
    private void igniteIfNeeded() {
        if (profile.fuelMode() == FuelMode.BURNING && burnRemaining <= 0 && takeFuel()) {
            burnRemaining = profile.burnSeconds();
        }
    }

    /** Uses up the fuel of a finished unit: a unit of fuel pays for as many units as the recipe says. */
    private void payFuelFor(ProcessingRecipe recipe) {
        if (profile.fuelMode() != FuelMode.PER_OUTPUT || !recipe.needsFuel()) {
            return;
        }
        if (paidOutputs == 0 && takeFuel()) {
            paidOutputs = recipe.outputsPerFuel();
        }
        paidOutputs--;
    }

    private boolean takeFuel() {
        for (ItemType fuel : profile.fuels()) {
            if (count(fuel) > 0) {
                stock.merge(fuel, -1, Integer::sum);
                return true;
            }
        }
        return false;
    }

    private boolean hasFuelLoaded() {
        return profile.fuels().stream().anyMatch(fuel -> count(fuel) > 0);
    }

    private boolean hasBurningFuel() {
        return burnRemaining > 0 || hasFuelLoaded();
    }

    private boolean fuelAvailableFor(ProcessingRecipe recipe) {
        if (!recipe.needsFuel()) {
            return true;
        }
        if (profile.fuelMode() == FuelMode.BURNING) {
            return hasBurningFuel();
        }
        return paidOutputs > 0 || hasFuelLoaded();
    }

    private boolean canRun(ProcessingRecipe recipe) {
        return count(recipe.input()) > 0 && fuelAvailableFor(recipe);
    }

    /** Picks the first recipe, in the order of the profile, that has something to process and the fuel for it. */
    private ProcessingRecipe selectRecipe() {
        return profile.recipes().stream().filter(this::canRun).findFirst().orElse(null);
    }

    // ==================================================================
    //  Output and content
    // ==================================================================

    /**
     * Takes the products that were made since the last call.
     *
     * @return the products, one unit each, empty when nothing was made
     */
    public List<Item> drainProducts() {
        final List<Item> drained = new ArrayList<>(products);
        products.clear();
        return drained;
    }

    /**
     * Empties the station, which is what happens to a station that is demolished: everything loaded and every product
     * not yet taken falls out. The state of the work is gone with it.
     *
     * @return the loaded items and the products, one stack for each item, empty when there was nothing
     */
    public List<Item> emptyContents() {
        final List<Item> contents = new ArrayList<>();
        stock.forEach((ItemType type, Integer units) -> {
            if (units > 0) {
                contents.add(new Item(type, units));
            }
        });
        contents.addAll(drainProducts());
        stock.clear();
        active = null;
        progress = 0;
        paidOutputs = 0;
        burnRemaining = 0;
        return contents;
    }
}
