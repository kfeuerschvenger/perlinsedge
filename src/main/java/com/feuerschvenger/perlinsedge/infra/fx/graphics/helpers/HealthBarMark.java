package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * The health bar of a building, queued for drawing over it.
 *
 * @param centerX  screen X of the middle of the bar
 * @param topY     screen Y of the top of the building, which the bar floats above
 * @param fraction the share of the health left, from 0 to 1
 */
public record HealthBarMark(double centerX, double topY, double fraction) { }
