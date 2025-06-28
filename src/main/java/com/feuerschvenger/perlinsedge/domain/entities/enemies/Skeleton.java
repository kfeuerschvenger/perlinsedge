package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

public class Skeleton extends Enemy {

    public Skeleton(int startX, int startY, double maxHealth, double attackDamage, double attackCooldown,
                    EnemyMovementStrategy movementStrategy, EnemyAttackStrategy attackStrategy) {
        super(startX, startY, maxHealth, attackDamage, attackCooldown, movementStrategy, attackStrategy);
    }

    @Override
    public void update(double deltaTime, TileMap map, Player player) {
        // TODO: Implement Skeleton specific behavior
        // Skeletons can shoot arrows at the player from a distance
    }

    @Override
    public double getRenderOffsetY() {
        return 0;
    }

}
