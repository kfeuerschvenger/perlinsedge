package com.feuerschvenger.perlinsedge.domain.events;

import javafx.scene.input.KeyCode;

/**
 * Interface to listen for key actions in the game.
 */
public interface IKeyListener {

    void onKeyAction(KeyCode code);

}
