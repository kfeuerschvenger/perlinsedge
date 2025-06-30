package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.LootManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages combat interactions between player and enemies.
 * Handles attack direction calculation, damage application, enemy updates, and death processing.
 */
public class CombatManager {
    private static final double FULL_CIRCLE_RADIANS = 2 * Math.PI;
    private static final double DIRECTION_SECTOR_SIZE = Math.PI / 4; // 45-degree sectors
    private static final double HALF_SECTOR_ADJUSTMENT = DIRECTION_SECTOR_SIZE / 2;

    private final GameStateManager gameState;

    public CombatManager(GameStateManager gameState) {
        this.gameState = gameState;
    }

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
            System.out.printf("%s attacked player. Player health: %.1f%n",
                    enemy.getClass().getSimpleName(), player.getHealth());
        }
    }

    /**
     * Initiates and processes a player attack based on mouse position.
     * Validates game state and player conditions before attacking.
     */
    public void handlePlayerAttack() {
        if (!isValidAttackState()) return;

        Player player = gameState.getPlayer();
        int[] attackDirection = calculateAttackVector(player);
        int direction = calculateAttackDirection(attackDirection[0], attackDirection[1]);

        player.startAttack(direction);
        applyMeleeDamage(player);
    }

    /**
     * Calculates the vector from player to mouse position in tile coordinates.
     * @return [deltaX, deltaY] array
     */
    private int[] calculateAttackVector(Player player) {
        return new int[] {
                gameState.getMouseOverTileX() - player.getCurrentTileX(),
                gameState.getMouseOverTileY() - player.getCurrentTileY()
        };
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

    /**
     * Applies melee damage to enemies within attack range.
     * Processes enemy death for defeated enemies.
     */
    private void applyMeleeDamage(Player player) {
        double[] attackPos = player.getAttackPosition();
        List<Enemy> enemies = new ArrayList<>(gameState.getEnemies()); // Safe copy for modification

        for (Enemy enemy : enemies) {
            if (isWithinAttackRange(attackPos, enemy, player.getAttackRange())) {
                enemy.takeDamage(player.getAttackDamage());
                if (enemy.isDead()) {
                    handleEnemyDeath(enemy);
                }
            }
        }
    }

    /**
     * Checks if enemy is within circular attack range.
     * @return true if distance <= attack radius
     */
    private boolean isWithinAttackRange(double[] attackPos, Enemy enemy, double range) {
        double dx = attackPos[0] - enemy.getCurrentTileX();
        double dy = attackPos[1] - enemy.getCurrentTileY();
        return (dx * dx + dy * dy) <= (range * range); // Squared distance comparison
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
        System.out.printf("%s defeated!%n", enemy.getClass().getSimpleName());
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