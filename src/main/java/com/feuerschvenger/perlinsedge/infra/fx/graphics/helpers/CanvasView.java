package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * The canvas as a frame sees it: the integer art scale it is drawn at, the screen position of the camera origin that
 * the art grid is aligned to, and the size of the canvas.
 *
 * @param artScale the integer number of screen pixels per art pixel
 * @param originX  screen X of the camera origin, which the art grid is aligned to
 * @param originY  screen Y of the camera origin
 * @param width    canvas width in pixels
 * @param height   canvas height in pixels
 */
public record CanvasView(double artScale, double originX, double originY, double width, double height) {}
