package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import javafx.scene.paint.Color;

/**
 * The ghost of the piece being placed: where it stands, which piece it is and the tint that says whether it can be
 * placed, green for yes, red for a tile that refuses it and amber for missing resources.
 *
 * @param tileX the hovered tile X
 * @param tileY the hovered tile Y
 * @param type  the selected piece
 * @param tint  the tint as the domain gives it
 * @param color the same tint as a drawing colour
 */
public record BuildingPreview(int tileX, int tileY, BuildingType type, Rgba tint, Color color) { }
