package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.LootManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.pathfinding.InteractionPlanner;
import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

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
     * Updates all creatures in the game world. The attack of a creature belongs to its attack strategy, which runs
     * inside {@link Creature#update(double, CreatureContext)}, so nothing here strikes the player.
     *
     * @param deltaTime Time since last update in seconds
     */
    public void updateCreatures(double deltaTime) {
        var player = gameState.getPlayer();
        TileMap map = gameState.getCurrentMap();

        if (player == null || map == null) {
            return;
        }

        // Create a safe copy to avoid concurrent modification
        List<Creature> creatures = new ArrayList<>(gameState.getCreatures());
        final var occupancy = gameState.getCreatureOccupancy();
        final var context = new CreatureContext(map, player, occupancy);

        for (Creature creature : creatures) {
            // Skip dead creatures (should be removed but just in case), which hold no tile
            if (creature.isDead()) {
                occupancy.release(creature);
                continue;
            }

            creature.update(deltaTime, context);
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
    public Strike strikeEnemy(Creature target) {
        if (!isValidAttackState() || target == null || target.isDead()) {
            return Strike.REFUSED;
        }

        final var player = gameState.getPlayer();
        if (!InteractionPlanner.inReach(player.getCurrentTileX(), player.getCurrentTileY(),
                target.getCurrentTileX(), target.getCurrentTileY())) {
            return Strike.REFUSED;
        }
        final int direction = calculateAttackDirection(target.getCurrentTileX() - player.getCurrentTileX(),
                target.getCurrentTileY() - player.getCurrentTileY());
        if (!player.startAttack(direction)) {
            return Strike.COOLDOWN;
        }

        damage(target, player);
        return Strike.HIT;
    }

    /**
     * Calculates attack direction based on vector components.
     * Divides the circle into 8 directional sectors (45° each).
     * @return Direction index (0-7) corresponding to compass directions
     */
    private static int calculateAttackDirection(int dx, int dy) {
        double angle = Math.atan2(dy, dx);
        double normalizedAngle = (angle < 0) ? angle + FULL_CIRCLE_RADIANS : angle;
        double adjustedAngle = (normalizedAngle + HALF_SECTOR_ADJUSTMENT) % FULL_CIRCLE_RADIANS;

        return (int) (adjustedAngle / DIRECTION_SECTOR_SIZE);
    }

    /** Deals the damage of the item in hand to an enemy, shows the number and processes its death. */
    private void damage(Creature creature, Player player) {
        // The number shows what the hit took off, so a blow that kills a creature shows only the health it had left
        final double healthBefore = creature.getHealth();
        creature.takeDamage(player.getAttackDamage(), player);
        gameState.getDamageNumbers().spawn(DamageNumbers.Kind.ENEMY, healthBefore - creature.getHealth(),
                creature.getPosX(), creature.getPosY(), gameState.getGameTime());
        if (creature.isDead()) {
            handleCreatureDeath(creature);
        }
    }

    /**
     * Handles creature death sequence:
     * 1. Generates loot drops
     * 2. Creates dropped items in world
     * 3. Removes creature from active list
     */
    private void handleCreatureDeath(Creature creature) {
        generateLootDrops(creature);
        gameState.getCreatures().remove(creature);
        gameState.getCreatureOccupancy().release(creature);
        log.debug("{} defeated", creature.getClass().getSimpleName());
    }

    /**
     * Generates and registers loot drops at the creature's position.
     */
    private void generateLootDrops(Creature creature) {
        List<Item> drops = LootManager.getInstance().generateLootForEnemy(creature.getClass());

        for (Item item : drops) {
            gameState.getDroppedItems().add(
                    new DroppedItem(
                            creature.getCurrentTileX(),
                            creature.getCurrentTileY(),
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
        var player = gameState.getPlayer();
        return player != null &&
                !player.isDead() &&
                !gameState.isGamePaused();
    }

}
