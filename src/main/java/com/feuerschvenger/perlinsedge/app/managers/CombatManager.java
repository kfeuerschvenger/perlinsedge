package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.LootManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.pathfinding.InteractionPlanner;
import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manages combat interactions between player and enemies.
 * Handles attack direction calculation, damage application, enemy updates, and death processing.
 */
@Slf4j
@RequiredArgsConstructor
public class CombatManager {
    private static final double FULL_CIRCLE_RADIANS = 2 * Math.PI;
    private static final double DIRECTION_SECTOR_SIZE = Math.PI / 4; // 45-degree sectors
    private static final double HALF_SECTOR_ADJUSTMENT = DIRECTION_SECTOR_SIZE / 2;

    private final GameStateManager gameState;

    /**
     * Updates all enemies in the game world.
     *
     * @param deltaTime Time since last update in seconds
     */
    public void updateEnemies(double deltaTime) {
        Player player = gameState.getPlayer();
        TileMap map = gameState.getCurrentMap();

        if (player == null || map == null) return;

        // Create a safe copy to avoid concurrent modification
        List<Enemy> enemies = new ArrayList<>(gameState.getEnemies());

        for (Enemy enemy : enemies) {
            // Skip dead enemies (should be removed but just in case)
            if (enemy.isDead()) continue;

            // Update enemy state
            enemy.update(deltaTime, map, player);

            // Handle enemy attacks on player
            handleEnemyAttack(enemy, player, deltaTime);
        }
    }

    /**
     * Handles enemy attack logic against the player.
     */
    private void handleEnemyAttack(Enemy enemy, Player player, double deltaTime) {
        if (!enemy.canAttack(player)) return;

        // Update enemy attack cooldown
        enemy.updateAttackCooldown(deltaTime);

        if (enemy.isReadyToAttack()) {
            player.takeDamage(enemy.getAttackDamage());
            enemy.resetAttackCooldown();
            if (log.isDebugEnabled()) {
                log.debug("{} attacked player. Player health: {}",
                        enemy.getClass().getSimpleName(), String.format(Locale.ROOT, "%.1f", player.getHealth()));
            }
        }
    }

    /** What came of an order to hit one enemy. */
    public enum Strike {
        /** The swing landed on the enemy. */
        HIT,
        /** The player swung too recently to swing again. */
        COOLDOWN,
        /** The attack cannot happen: no player, a dead player or enemy, a paused game, or the enemy out of reach. */
        REFUSED
    }

    /**
     * Swings at one enemy with the item in hand. The player turns toward the enemy and the swing lands on that enemy
     * alone, for the damage of the active item, so a click on an enemy never depends on where the mouse rests after
     * the click. The player's own cooldown gates the swing.
     *
     * @param target the enemy to hit
     * @return {@link Strike#HIT} when the swing landed, {@link Strike#COOLDOWN} when it must wait, and
     *         {@link Strike#REFUSED} when the enemy is not within melee reach of the tile the player stands on
     */
    public Strike strikeEnemy(Enemy target) {
        if (!isValidAttackState() || target == null || target.isDead()) return Strike.REFUSED;

        final Player player = gameState.getPlayer();
        if (!InteractionPlanner.inReach(player.getCurrentTileX(), player.getCurrentTileY(),
                target.getCurrentTileX(), target.getCurrentTileY())) {
            return Strike.REFUSED;
        }
        final int direction = calculateAttackDirection(target.getCurrentTileX() - player.getCurrentTileX(),
                target.getCurrentTileY() - player.getCurrentTileY());
        if (!player.startAttack(direction)) return Strike.COOLDOWN;

        damage(target, player);
        return Strike.HIT;
    }

    /**
     * Calculates attack direction based on vector components.
     * Divides the circle into 8 directional sectors (45° each).
     * @return Direction index (0-7) corresponding to compass directions
     */
    private int calculateAttackDirection(int dx, int dy) {
        double angle = Math.atan2(dy, dx);
        double normalizedAngle = (angle < 0) ? angle + FULL_CIRCLE_RADIANS : angle;
        double adjustedAngle = (normalizedAngle + HALF_SECTOR_ADJUSTMENT) % FULL_CIRCLE_RADIANS;

        return (int) (adjustedAngle / DIRECTION_SECTOR_SIZE);
    }

    /** Deals the damage of the item in hand to an enemy, shows the number and processes its death. */
    private void damage(Enemy enemy, Player player) {
        enemy.takeDamage(player.getAttackDamage());
        gameState.getDamageNumbers().spawn(DamageNumbers.Kind.ENEMY, player.getAttackDamage(),
                enemy.getPosX(), enemy.getPosY(), gameState.getGameTime());
        if (enemy.isDead()) {
            handleEnemyDeath(enemy);
        }
    }

    /**
     * Handles enemy death sequence:
     * 1. Generates loot drops
     * 2. Creates dropped items in world
     * 3. Removes enemy from active list
     */
    private void handleEnemyDeath(Enemy enemy) {
        generateLootDrops(enemy);
        gameState.getEnemies().remove(enemy);
        log.debug("{} defeated", enemy.getClass().getSimpleName());
    }

    /**
     * Generates and registers loot drops at enemy's position.
     */
    private void generateLootDrops(Enemy enemy) {
        List<Item> drops = LootManager.getInstance().generateLootForEnemy(enemy.getClass());

        for (Item item : drops) {
            gameState.getDroppedItems().add(
                    new DroppedItem(
                            enemy.getCurrentTileX(),
                            enemy.getCurrentTileY(),
                            item,
                            gameState.getGameTime(),
                            DroppedItem.DropSource.ENEMY
                    )
            );
        }
    }

    /**
     * Validates conditions for player attack.
     * @return true if attack should proceed, false otherwise
     */
    private boolean isValidAttackState() {
        Player player = gameState.getPlayer();
        return player != null &&
                !player.isDead() &&
                !gameState.isGamePaused();
    }

}