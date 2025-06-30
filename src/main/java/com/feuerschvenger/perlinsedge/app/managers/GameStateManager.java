package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central repository for game state information.
 * Tracks entities, timers, player status, and world state.
 */
public class GameStateManager {
    // Entity State
    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<DroppedItem> droppedItems = new ArrayList<>();

    // World State
    private TileMap currentMap;
    private final Map<Resource, Long> resourceHitTimes = new HashMap<>();

    // Timing & Cooldowns
    private double gameTime = 0;
    private double playerRespawnTimer = -1;
    private double lastDropTime = -10;
    private double attackCooldownTimer = 0;

    // Input & UI State
    private int mouseOverTileX = -1;
    private int mouseOverTileY = -1;
    private boolean isGamePaused = false;
    private boolean isInventoryOpen = false;
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
        lastDropTime = -10;
        attackCooldownTimer = 0;

        // UI/Input reset
        mouseOverTileX = -1;
        mouseOverTileY = -1;
        isGamePaused = false;
        isInventoryOpen = false;
        shouldCenterCamera = false;
        currentGameMode = GameMode.EXPLORATION;
    }

    /**
     * Advances the game clock by the specified time delta.
     * @param deltaTime Time elapsed since last update (in seconds)
     */
    public void incrementGameTime(double deltaTime) {
        gameTime += deltaTime;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public void setEnemies(List<Enemy> enemies) {
        this.enemies.clear();
        if (enemies != null) {
            this.enemies.addAll(enemies);
        }
    }

    public List<DroppedItem> getDroppedItems() {
        return droppedItems;
    }

    public void setDroppedItems(List<DroppedItem> droppedItems) {
        this.droppedItems.clear();
        if (droppedItems != null) {
            this.droppedItems.addAll(droppedItems);
        }
    }

    public TileMap getCurrentMap() {
        return currentMap;
    }

    public void setCurrentMap(TileMap currentMap) {
        this.currentMap = currentMap;
    }

    public Map<Resource, Long> getResourceHitTimes() {
        return resourceHitTimes;
    }

    public double getGameTime() {
        return gameTime;
    }

    public double getPlayerRespawnTimer() {
        return playerRespawnTimer;
    }

    public void setPlayerRespawnTimer(double playerRespawnTimer) {
        this.playerRespawnTimer = playerRespawnTimer;
    }

    public double getLastDropTime() {
        return lastDropTime;
    }

    public void setLastDropTime(double lastDropTime) {
        this.lastDropTime = lastDropTime;
    }

    public double getAttackCooldownTimer() {
        return attackCooldownTimer;
    }

    public void setAttackCooldownTimer(double attackCooldownTimer) {
        this.attackCooldownTimer = attackCooldownTimer;
    }

    public int getMouseOverTileX() {
        return mouseOverTileX;
    }

    public void setMouseOverTileX(int mouseOverTileX) {
        this.mouseOverTileX = mouseOverTileX;
    }

    public int getMouseOverTileY() {
        return mouseOverTileY;
    }

    public void setMouseOverTileY(int mouseOverTileY) {
        this.mouseOverTileY = mouseOverTileY;
    }

    public int[] getMouseOverTile() {
        return new int[]{mouseOverTileX, mouseOverTileY};
    }

    public void setMouseOverTile(int x, int y) {
        this.mouseOverTileX = x;
        this.mouseOverTileY = y;
    }

    public boolean isGamePaused() {
        return isGamePaused;
    }

    public void setGamePaused(boolean gamePaused) {
        isGamePaused = gamePaused;
    }

    public boolean isInventoryOpen() {
        return isInventoryOpen;
    }

    public void setInventoryOpen(boolean inventoryOpen) {
        isInventoryOpen = inventoryOpen;
    }

    public boolean shouldCenterCamera() {
        return shouldCenterCamera;
    }

    public void setShouldCenterCamera(boolean shouldCenterCamera) {
        this.shouldCenterCamera = shouldCenterCamera;
    }

    public GameMode getCurrentGameMode() {
        return currentGameMode;
    }

    public void setCurrentGameMode(GameMode currentGameMode) {
        this.currentGameMode = currentGameMode;
    }

}