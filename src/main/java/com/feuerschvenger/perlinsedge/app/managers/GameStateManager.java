package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingStation;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.ProcessingStation;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.utils.ClickFeedback;
import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.light.HeldTorch;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
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
    /** Which enemy holds which tile, so every enemy stands on a tile of its own. */
    private final EnemyOccupancy enemyOccupancy = new EnemyOccupancy();
    private final List<DroppedItem> droppedItems = new ArrayList<>();

    // World State
    private TileMap currentMap;
    /** The game time in seconds of the last hit on each resource, which the hit cooldown is measured from. */
    private final Map<Resource, Double> resourceHitTimes = new HashMap<>();
    /** The processing stations standing on the map, which are the ones that need time to pass. */
    private final List<ProcessingStation> stations = new ArrayList<>();
    /** The seed the current map was generated from, which also scatters the cosmetic ground decals. */
    private long worldSeed;

    // Timing & Cooldowns
    @Setter(lombok.AccessLevel.NONE)
    private double gameTime;
    private double playerRespawnTimer = -1;
    /** The clock of the sky, advanced next to the game clock so a paused game stands still. */
    private final DayCycle dayCycle = new DayCycle(AppConfig.getInstance().dayCycle().settings());

    /**
     * The lights of the world. Whatever gives light registers a source here, and the renderer lights the world from
     * them. The registry belongs to the world, so a new world and a rebuilt map start with it empty.
     */
    private final LightRegistry lights = new LightRegistry();
    /** The light of the torch held in hand, which is in the registry only while a torch is the selected item. */
    private final HeldTorch heldTorch = AppConfig.getInstance().lights().newHeldTorch();

    // Input & UI State
    private int mouseOverTileX = -1;
    private int mouseOverTileY = -1;
    /** The flash the cursor shows on the tile of the last click order. */
    private final ClickFeedback clickFeedback = new ClickFeedback();
    /** The numbers that float up from a hit or a heal, on the game clock. */
    private final DamageNumbers damageNumbers = new DamageNumbers();
    private boolean isGamePaused;
    private boolean isInventoryOpen;
    /** The place of crafting the open inventory belongs to: the inventory itself, or the station that was clicked. */
    private CraftingStation craftingStation = CraftingStation.INVENTORY;
	@Getter(lombok.AccessLevel.NONE)
    private boolean shouldCenterCamera = true; // true to center at the next update

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
        enemyOccupancy.clear();
        droppedItems.clear();

        // World reset
        currentMap = null;
        stations.clear();
        resourceHitTimes.clear();

        // The lights belong to the world that is gone
        lights.clear();

        // Timer reset
        gameTime = 0;
        dayCycle.reset();
        // The fast time is a debug switch held in the shared configuration, so a new world does not inherit it
        AppConfig.getInstance().dayCycle().setFastTime(false);
        playerRespawnTimer = -1;

        // UI/Input reset
        mouseOverTileX = -1;
        mouseOverTileY = -1;
        clickFeedback.clear();
        damageNumbers.clear();
        isGamePaused = false;
        isInventoryOpen = false;
        craftingStation = CraftingStation.INVENTORY;
        shouldCenterCamera = false;
        currentGameMode = GameMode.EXPLORATION;
    }

    /** @return true while the player exists and is dead, which is when gameplay input is ignored */
    public boolean isPlayerDead() {
        return player != null && player.isDead();
    }

    /**
     * Tells whether gameplay keys and the gameplay use of the wheel must be ignored: there is no player to act for,
     * as after a rebuild that failed halfway, or the player is dead.
     *
     * @return true when no live player can act
     */
    public boolean isGameplayInputBlocked() {
        return player == null || player.isDead();
    }

    /**
     * Advances the game clock by the specified time delta.
     * @param deltaTime Time elapsed since last update (in seconds)
     */
    public void incrementGameTime(double deltaTime) {
        gameTime += deltaTime;
    }

    /**
     * Brings the light of the held torch in step with the player: it is registered where the player is while a torch is
     * the selected item and the player is alive, and taken out otherwise. The game calls it with every update, so the
     * registry is right for whatever reads it, and the renderer calls it with the interpolation of its frame, so the
     * light is where the sprite of the player is drawn and appears and disappears the moment the selection changes.
     *
     * @param alpha how far between the last two updates the frame is, 1 for the position of the last update
     */
    public void syncLights(double alpha) {
        heldTorch.sync(lights, player, alpha);
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
        enemyOccupancy.rebuild(this.enemies);
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

}
