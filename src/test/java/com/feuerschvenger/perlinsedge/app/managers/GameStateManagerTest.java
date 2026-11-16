package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import com.feuerschvenger.perlinsedge.ui.controller.KeyController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateManagerTest {

    @Test
    void gameplayInputIsBlockedWhenThereIsNoPlayer() {
        GameStateManager state = new GameStateManager();

        assertTrue(state.isGameplayInputBlocked(), "a rebuild that failed halfway leaves no player");
        assertFalse(state.isPlayerDead(), "the missing player is not a dead one");
    }

    @Test
    void gameplayInputIsBlockedWhileThePlayerIsDeadAndOpenOtherwise() {
        GameStateManager state = new GameStateManager();
        Player player = new Player(1, 1);
        state.setPlayer(player);
        assertFalse(state.isGameplayInputBlocked());

        player.takeDamage(1000);
        assertTrue(state.isGameplayInputBlocked());

        player.revive(1, 1);
        assertFalse(state.isGameplayInputBlocked());
    }

    @Test
    void theKeysAreIgnoredWithoutAPlayerAndWorkOnceThereIsOne() {
        GameStateManager state = new GameStateManager();
        List<Integer> numbers = new ArrayList<>();
        List<String> drops = new ArrayList<>();
        KeyController keys = new KeyController(() -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, numbers::add, delta -> { });
        keys.setDropActions(() -> drops.add("one"), () -> drops.add("stack"));
        keys.setInputBlocked(state::isGameplayInputBlocked);

        keys.onKeyAction(GameKey.DIGIT_3);
        keys.onKeyAction(GameKey.DROP_ONE);
        assertTrue(numbers.isEmpty() && drops.isEmpty(), "nothing acts without a player");

        state.setPlayer(new Player(0, 0));
        keys.onKeyAction(GameKey.DIGIT_3);
        keys.onKeyAction(GameKey.DROP_ONE);
        assertEquals(List.of(3), numbers);
        assertEquals(List.of("one"), drops);
    }
}
