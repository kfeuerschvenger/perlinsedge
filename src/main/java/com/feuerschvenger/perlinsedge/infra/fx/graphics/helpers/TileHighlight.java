package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.app.managers.ClickFeedback;

/**
 * A tile cursor to draw: where it is, how big the tile is, whether the ground under it is light, and which answer to a
 * click it shows.
 *
 * @param centerX     X of the centre of the tile top
 * @param centerY     Y of the centre of the tile top
 * @param tileWidth   the zoomed width of the tile
 * @param tileHeight  the zoomed height of the tile
 * @param lightGround true when the ground is light, so the crosshair is drawn dark
 * @param tone        {@link ClickFeedback.Kind#NONE} for the resting crosshair, accepted for green, rejected for red
 */
public record TileHighlight(double centerX, double centerY, double tileWidth, double tileHeight,
                            boolean lightGround, ClickFeedback.Kind tone) {
}
