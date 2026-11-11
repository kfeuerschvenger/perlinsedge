package com.feuerschvenger.perlinsedge.infra.fx.input;

/**
 * A handler that gets the first chance at every scroll event, before the camera zooms.
 */
@FunctionalInterface
public interface ScrollInterceptor {

    /**
     * Offers a scroll event to the handler.
     *
     * @param deltaY the vertical wheel delta, positive when the wheel turns up or away
     * @param ctrlDown true when Ctrl is held
     * @return true when the handler used the event, which keeps the camera from zooming for it
     */
    boolean intercept(double deltaY, boolean ctrlDown);
}
