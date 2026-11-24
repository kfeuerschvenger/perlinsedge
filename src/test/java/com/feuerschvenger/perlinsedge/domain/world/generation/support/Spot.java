package com.feuerschvenger.perlinsedge.domain.world.generation.support;

/**
 * A tile position the harness reports or marks. The current generator produces no spawn point, so the harness derives
 * one; this is replaced by the spawn point of the map once the pipeline produces it.
 *
 * @param x column
 * @param y row
 */
public record Spot(int x, int y) {
}
