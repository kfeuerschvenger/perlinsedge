package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Where the camera stands: its offset on the screen and its zoom.
 *
 * @param offsetX camera X offset
 * @param offsetY camera Y offset
 * @param zoom    camera zoom
 */
public record CameraView(double offsetX, double offsetY, double zoom) {}
