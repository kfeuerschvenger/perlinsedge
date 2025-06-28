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

public class GameStateManager {
    private Player player;
    private List<Enemy> enemies = new ArrayList<>();
    private List<DroppedItem> droppedItems = new ArrayList<>();
    private TileMap currentMap;
    private Map<Resource, Long> resourceHitTimes = new HashMap<>();
    private double gameTime = 0;
    private double playerRespawnTimer = -1;
    private double lastDropTime = -10;
    private double attackCooldownTimer = 0;
    private int mouseOverTileX = -1;
    private int mouseOverTileY = -1;
    private boolean isGamePaused = false;
    private boolean shouldCenterCamera = true;
    private GameMode currentGameMode = GameMode.EXPLORATION;
    private double screenWidth = 0;
    private double screenHeight = 0;

    /**
     * Resets the entire game state to a default, uninitialized state, ready for a new game.
     */
    public void resetGameState() {
        this.player = null;
        this.enemies.clear();
        this.droppedItems.clear();
        this.currentMap = null;
        this.resourceHitTimes.clear();
        this.gameTime = 0;
        this.playerRespawnTimer = 0;
        this.lastDropTime = 0;
        this.attackCooldownTimer = 0;
        this.mouseOverTileX = -1;
        this.mouseOverTileY = -1;
        this.isGamePaused = false;
        this.shouldCenterCamera = false;
        this.currentGameMode = GameMode.EXPLORATION;
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
        this.enemies = enemies;
    }

    public List<DroppedItem> getDroppedItems() {
        return droppedItems;
    }

    public void setDroppedItems(List<DroppedItem> droppedItems) {
        this.droppedItems = droppedItems;
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

    public void setResourceHitTimes(Map<Resource, Long> resourceHitTimes) {
        this.resourceHitTimes = resourceHitTimes;
    }

    public double getGameTime() {
        return gameTime;
    }

    public void setGameTime(double gameTime) {
        this.gameTime = gameTime;
    }

    public void incrementGameTime(double deltaTime) {
        gameTime += deltaTime;
    }

    public double getPlayerRespawnTimer() {
        return playerRespawnTimer;
    }

    public void setPlayerRespawnTimer(double playerRespawnTimer) { this.playerRespawnTimer = playerRespawnTimer; }

    public double getLastDropTime() {
        return lastDropTime;
    }

    public void setLastDropTime(double time) {
        this.lastDropTime = time;
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

    public boolean isGamePaused() {
        return isGamePaused;
    }

    public void setGamePaused(boolean gamePaused) {
        isGamePaused = gamePaused;
    }

    public boolean shouldCenterCamera() {
        return shouldCenterCamera;
    }

    public void setShouldCenterCamera(boolean shouldCenterCamera) {
        this.shouldCenterCamera = shouldCenterCamera;
    }

    public GameMode getCurrentGameMode() { return currentGameMode; }

    public void setCurrentGameMode(GameMode currentGameMode) { this.currentGameMode = currentGameMode; }

    public void setScreenSize(double screenWidth, double screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public double getScreenWidth() {
        return screenWidth;
    }

    public double getScreenHeight() {
        return screenHeight;
    }

}