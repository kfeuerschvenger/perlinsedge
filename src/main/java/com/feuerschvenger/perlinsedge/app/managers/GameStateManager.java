package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central repository for game state information.
 * Tracks entities, timers, player status, and world state.
 */
@Getter
@Setter
public class GameStateManager {
    // Entity State
    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<DroppedItem> droppedItems = new ArrayList<>();

    // World State
    private TileMap currentMap;
    /** The game time in seconds of the last hit on each resource, which the hit cooldown is measured from. */
    private final Map<Resource, Double> resourceHitTimes = new HashMap<>();
    /** The seed the current map was generated from, which also scatters the cosmetic ground decals. */
    private long worldSeed;

    // Timing & Cooldowns
    @Setter(lombok.AccessLevel.NONE)
    private double gameTime = 0;
    private double playerRespawnTimer = -1;

    // Input & UI State
    private int mouseOverTileX = -1;
    private int mouseOverTileY = -1;
    /** The flash the cursor shows on the tile of the last click order. */
    private final ClickFeedback clickFeedback = new ClickFeedback();
    private boolean isGamePaused = false;
    private boolean isInventoryOpen = false;
    @Getter(lombok.AccessLevel.NONE)
    private boolean shouldCenterCamera = true;

    // Game Configuration
    private GameMode currentGameMode = GameMode.EXPLORATION;

    /**
     * Resets all game state to initial values.
     * Clears entities, resets timers, and restores default settings.
     */
    public void resetGameState() {
        // Entity reset
        player = null;
        enemies.clear();
        droppedItems.clear();

        // World reset
        currentMap = null;
        resourceHitTimes.clear();

        // Timer reset
        gameTime = 0;
        playerRespawnTimer = -1;

        // UI/Input reset
        mouseOverTileX = -1;
        mouseOverTileY = -1;
        clickFeedback.clear();
        isGamePaused = false;
        isInventoryOpen = false;
        shouldCenterCamera = false;
        currentGameMode = GameMode.EXPLORATION;
    }

    /** @return true while the player exists and is dead, which is when gameplay input is ignored */
    public boolean isPlayerDead() {
        return player != null && player.isDead();
    }

    /**
     * Advances the game clock by the specified time delta.
     * @param deltaTime Time elapsed since last update (in seconds)
     */
    public void incrementGameTime(double deltaTime) {
        gameTime += deltaTime;
    }

    /**
     * Replaces the enemies with a copy of the given list.
     *
     * @param enemies the new enemies, null clears the list
     */
    public void setEnemies(List<Enemy> enemies) {
        this.enemies.clear();
        if (enemies != null) {
            this.enemies.addAll(enemies);
        }
    }

    /**
     * Replaces the dropped items with a copy of the given list.
     *
     * @param droppedItems the new dropped items, null clears the list
     */
    public void setDroppedItems(List<DroppedItem> droppedItems) {
        this.droppedItems.clear();
        if (droppedItems != null) {
            this.droppedItems.addAll(droppedItems);
        }
    }

    /** @return the mouse-over tile as {x, y}, or {-1, -1} when there is none */
    public int[] getMouseOverTile() {
        return new int[]{mouseOverTileX, mouseOverTileY};
    }

    /**
     * Sets the tile under the mouse.
     *
     * @param x tile X, -1 for none
     * @param y tile Y, -1 for none
     */
    public void setMouseOverTile(int x, int y) {
        this.mouseOverTileX = x;
        this.mouseOverTileY = y;
    }

    /** @return true when the camera should be centred on the player at the next update */
    public boolean shouldCenterCamera() {
        return shouldCenterCamera;
    }

    /**
     * Requests or cancels centring the camera on the player.
     *
     * @param shouldCenterCamera true to centre at the next update
     */
    public void setShouldCenterCamera(boolean shouldCenterCamera) {
        this.shouldCenterCamera = shouldCenterCamera;
    }

}
