package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.entities.items.ToolStats;
import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every hit or heal that lands starts one floating number, of the kind and the amount it reports. */
class DamageNumbersWiringTest {

    private GameStateManager state;
    private Player player;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        player = new Player(5, 5);
        state.setPlayer(player);
    }

    private DamageNumbers.Entry only() {
        DamageNumbers numbers = state.getDamageNumbers();
        DamageNumbers.Entry found = null;
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i).isActive(state.getGameTime())) {
                assertTrue(found == null, "more than one number is shown");
                found = numbers.get(i);
            }
        }
        assertTrue(found != null, "no number is shown");
        return found;
    }

    @Test
    void aHitOnAnEnemyShowsAYellowKindNumberWithTheDamageOfTheItemInHand() {
        player.setItemAt(0, new Item(ItemType.SWORD, 1));
        Slime slime = new Slime(5, 5);
        state.getEnemies().add(slime);

        new CombatManager(state).strikeEnemy(slime);

        DamageNumbers.Entry entry = only();
        assertEquals(DamageNumbers.Kind.ENEMY, entry.getKind());
        assertEquals((int) Math.round(ToolStats.combatDamage(ItemType.SWORD)), entry.getValue());
    }

    @Test
    void aHitOnAResourceShowsAResourceNumberWithTheDamageOfTheTool() {
        TileMap map = new TileMap(3, 3);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.HILL));
            }
        }
        Tile tile = map.getTile(1, 1);
        tile.setWorldResource(new Resource(ResourceType.TREE, 1));
        player.setItemAt(0, new Item(ItemType.AXE, 1));
        state.setCurrentMap(map);
        player.setPosition(0, 0);

        new ResourceManager(state, hint -> { }).handleTileInteraction(player, tile);

        DamageNumbers.Entry entry = only();
        assertEquals(DamageNumbers.Kind.RESOURCE, entry.getKind());
        assertEquals(ToolStats.resourceDamage(ItemType.AXE, ResourceType.TREE), entry.getValue());
    }

    @Test
    void aBlowThatKillsAnEnemyShowsTheHealthItHadLeftNotTheFullDamageOfTheItem() {
        player.setItemAt(0, new Item(ItemType.SWORD, 1));
        Slime slime = new Slime(5, 5);
        slime.takeDamage(slime.getHealth() - 5);
        state.getEnemies().add(slime);

        new CombatManager(state).strikeEnemy(slime);

        assertTrue(slime.isDead());
        assertEquals(5, only().getValue());
    }

    @Test
    void theBlowThatBreaksAResourceShowsTheHealthItHadLeft() {
        TileMap map = new TileMap(3, 3);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.HILL));
            }
        }
        Tile tile = map.getTile(1, 1);
        Resource tree = new Resource(ResourceType.TREE, 1);
        tree.takeDamage(tree.getHealth() - 3);
        tile.setWorldResource(tree);
        player.setItemAt(0, new Item(ItemType.AXE, 1));
        state.setCurrentMap(map);
        player.setPosition(0, 0);

        new ResourceManager(state, hint -> { }).handleTileInteraction(player, tile);

        assertTrue(tree.isDepleted());
        assertEquals(3, only().getValue());
    }

    @Test
    void aHealShowsAGreenKindNumberWithTheHealthRestored() {
        player.takeDamage(60);
        player.drainDamageTaken();
        player.setItemAt(0, new Item(ItemType.POTION_HEALTH, 1));

        new ItemUseManager(state, () -> { }, hint -> { }).useActiveItem();

        DamageNumbers.Entry entry = only();
        assertEquals(DamageNumbers.Kind.HEAL, entry.getKind());
        assertEquals("+50", entry.getText());
    }

    @Test
    void healthLostByThePlayerShowsOneRedKindNumberOnTheNextUpdate() {
        PlayerManager manager = FxToolkitSupport.onFx(() -> new PlayerManager(state, new HealthBar(),
                new InventoryPanel(state)));
        player.takeDamage(10);
        player.takeDamage(7);

        FxToolkitSupport.onFx(() -> {
            manager.updatePlayer(0.016);
            return null;
        });

        DamageNumbers.Entry entry = only();
        assertEquals(DamageNumbers.Kind.PLAYER, entry.getKind());
        assertEquals(17, entry.getValue());
    }

    @Test
    void theNumbersAreClearedWhenTheGameStateIsReset() {
        state.getDamageNumbers().spawn(DamageNumbers.Kind.ENEMY, 5, 0, 0, state.getGameTime());

        state.resetGameState();

        assertEquals(0, state.getDamageNumbers().countActive(0));
    }
}
