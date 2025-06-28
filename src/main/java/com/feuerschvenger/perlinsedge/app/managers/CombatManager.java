package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.LootManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

import java.util.ArrayList;
import java.util.List;

public class CombatManager {
    private final GameStateManager gameState;

    public CombatManager(GameStateManager gameState) {
        this.gameState = gameState;
    }

    public void handlePlayerAttack() {
        Player player = gameState.getPlayer();
        if (player == null || player.isDead() || gameState.isGamePaused()) return;

        int playerTileX = player.getCurrentTileX();
        int playerTileY = player.getCurrentTileY();
        int dx = gameState.getMouseOverTileX() - playerTileX;
        int dy = gameState.getMouseOverTileY() - playerTileY;

        int direction = calculateAttackDirection(dx, dy);
        player.startAttack(direction);
        applyMeleeDamage();
    }

    private int calculateAttackDirection(int dx, int dy) {
        double angle = Math.atan2(dy, dx);
        if (angle < 0) angle += 2 * Math.PI;

        double sectorSize = Math.PI / 4;
        double adjustedAngle = angle + sectorSize / 2;
        if (adjustedAngle > 2 * Math.PI) adjustedAngle -= 2 * Math.PI;

        return (int) (adjustedAngle / sectorSize) % 8;
    }

    private void applyMeleeDamage() {
        Player player = gameState.getPlayer();
        if (player == null) return;

        double[] attackPos = player.getAttackPosition();
        double attackX = attackPos[0];
        double attackY = attackPos[1];

        for (Enemy enemy : new ArrayList<>(gameState.getEnemies())) {
            double enemyX = enemy.getCurrentTileX();
            double enemyY = enemy.getCurrentTileY();
            double distance = Math.sqrt(Math.pow(attackX - enemyX, 2) + Math.pow(attackY - enemyY, 2));

            if (distance <= player.getAttackRange()) {
                enemy.takeDamage(player.getAttackDamage());
                if (enemy.isDead()) {
                    handleEnemyDeath(enemy);
                }
            }
        }
    }

    private void handleEnemyDeath(Enemy enemy) {
        List<Item> drops = LootManager.getInstance().generateLootForEnemy(enemy.getClass());
        for (Item item : drops) {
            DroppedItem dropped = new DroppedItem(
                    enemy.getCurrentTileX(),
                    enemy.getCurrentTileY(),
                    item,
                    gameState.getGameTime(),
                    DroppedItem.DropSource.ENEMY
            );
            gameState.getDroppedItems().add(dropped);
        }
        gameState.getEnemies().remove(enemy);
    }
}