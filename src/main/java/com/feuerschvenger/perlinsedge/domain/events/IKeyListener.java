package com.feuerschvenger.perlinsedge.domain.events;

/**
 * Interface to listen for key actions in the game.
 */
public interface IKeyListener {

    /**
     * Called when a key the game cares about is pressed.
     *
     * @param key the pressed key
     */
    void onKeyAction(GameKey key);

}
