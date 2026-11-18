package com.feuerschvenger.perlinsedge.infra.fx.graphics;

/**
 * Takes a look at the cells of a layer while the layer lends them out, so the layer never hands its array to a caller
 * that could keep it. The sink must not change the cells and must not keep the array past the call.
 */
@FunctionalInterface
public interface PixelSink {

    /**
     * Receives the cells of the layer.
     *
     * @param pixels the cells as ARGB, row by row, which the sink must not change or keep
     */
    void accept(int[] pixels);
}
