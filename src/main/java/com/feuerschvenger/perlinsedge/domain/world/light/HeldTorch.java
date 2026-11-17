package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * The light of the torch the player holds. It follows the player and exists only while the player is alive and the
 * selected hotbar slot holds a torch: it is added to the registry the moment the torch is selected and taken out the
 * moment anything else is, or the player dies. Asking it to {@linkplain #sync follow the player} is all a frame has to
 * do, and it allocates nothing.
 */
public final class HeldTorch implements LightSource {

    /** How far the light reaches on the ground in tiles. */
    private final double radius;
    /** The colour the light gives to what it falls on. */
    private final Rgba colour;
    /** How strongly the light pushes back the dark at its brightest, 0 to 1. */
    private final double intensity;
    /** How the light wavers. */
    private final Flicker flicker;
    /** Where the light shines from along X in tile coordinates, the position of the player. */
    private double x;
    /** Where the light shines from along Y in tile coordinates, the position of the player. */
    private double y;

    /**
     * Creates the light of a held torch.
     *
     * @param radius    how far the light reaches on the ground in tiles, above zero
     * @param colour    the colour the light gives to what it falls on
     * @param intensity how strongly the light pushes back the dark at its brightest, 0 to 1
     * @param flicker   how the light wavers
     * @throws IllegalArgumentException if the radius is not above zero, the intensity is outside 0 to 1, or the colour
     *                                  or the flicker is missing
     */
    public HeldTorch(double radius, Rgba colour, double intensity, Flicker flicker) {
        if (!(radius > 0.0)) {
            throw new IllegalArgumentException("A light needs a radius above zero");
        }
        if (!(intensity >= 0.0 && intensity <= 1.0)) {
            throw new IllegalArgumentException("The intensity of a light must be from 0 to 1");
        }
        if (colour == null || flicker == null) {
            throw new IllegalArgumentException("A light needs a colour and a flicker");
        }
        this.radius = radius;
        this.colour = colour;
        this.intensity = intensity;
        this.flicker = flicker;
    }

    /**
     * Tells whether a player holds a torch that gives light: the player is alive and the selected hotbar slot holds a
     * torch.
     *
     * @param player the player, which may be null while there is none
     * @return true when the torch is lit
     */
    public static boolean isLit(Player player) {
        return player != null && !player.isDead() && player.getActiveItemType() == ItemType.TORCH;
    }

    /**
     * Brings the light in step with the player: while a torch is held it is registered and placed where the player
     * is, and otherwise it is taken out of the registry.
     *
     * @param registry the registry of the world
     * @param player   the player, which may be null while there is none
     * @param alpha    how far between the last two updates the frame is, 1 for the position of the last update, so the
     *                 light follows the player the way the sprite of the player does
     */
    public void sync(LightRegistry registry, Player player, double alpha) {
        if (isLit(player)) {
            x = player.getInterpolatedX(alpha);
            y = player.getInterpolatedY(alpha);
            registry.add(this);
        } else {
            registry.remove(this);
        }
    }

    /** {@inheritDoc} The position of the player at the last {@link #sync}. */
    @Override
    public double x() {
        return x;
    }

    /** {@inheritDoc} The position of the player at the last {@link #sync}. */
    @Override
    public double y() {
        return y;
    }

    /** {@inheritDoc} */
    @Override
    public double radius() {
        return radius;
    }

    /** {@inheritDoc} */
    @Override
    public Rgba colour() {
        return colour;
    }

    /** {@inheritDoc} */
    @Override
    public double intensity() {
        return intensity;
    }

    /** {@inheritDoc} */
    @Override
    public Flicker flicker() {
        return flicker;
    }
}
