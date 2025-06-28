package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.events.IConfigChangeListener;
import com.feuerschvenger.perlinsedge.domain.events.IMapGenerationListener;
import com.feuerschvenger.perlinsedge.domain.world.generation.IMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGeneratorFactory;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapManager implements IConfigChangeListener, IMapGenerationListener {
    private final GameStateManager gameState;
    private final MapConfigPanel mapConfigPanel;
    private final MapGeneratorFactory mapGeneratorFactory;
    private MapGenerationContext currentMapContext;

    public MapManager(GameStateManager gameState, MapConfigPanel mapConfigPanel) {
        this.gameState = gameState;
        this.mapConfigPanel = mapConfigPanel;
        this.mapGeneratorFactory = new MapGeneratorFactory();
    }

    /**
     * Creates a new MapGenerationContext based on the provided map type and seed,
     * using other parameters from AppConfig.
     * This method is public so GameOrchestrator can use it.
     * @param mapType The type of map to generate.
     * @param seed The seed for map generation.
     * @return A new MapGenerationContext.
     */
    public MapGenerationContext createMapContext(MapType mapType, long seed) {
        return new MapGenerationContext(
                mapType,
                seed,
                AppConfig.getInstance().world().getHeightNoiseFrequency(),
                AppConfig.getInstance().world().getHeightNoiseOctaves(),
                AppConfig.getInstance().world().getTemperatureNoiseFrequency(),
                AppConfig.getInstance().world().getTemperatureNoiseOctaves(),
                AppConfig.getInstance().world().getMoistureNoiseFrequency(),
                AppConfig.getInstance().world().getMoistureNoiseOctaves(),
                AppConfig.getInstance().world().getStonePatchFrequency(),
                AppConfig.getInstance().world().getStoneThreshold(),
                AppConfig.getInstance().world().getCrystalsPatchFrequency(),
                AppConfig.getInstance().world().getCrystalsThreshold(),
                AppConfig.getInstance().world().getTreeDensity(),
                AppConfig.getInstance().world().getRiverDensity()
        );
    }

    public void generateNewMap(MapGenerationContext context) {
        System.out.println("MapManager: Generando nuevo mapa de tipo " + context.getMapType().name() + " con seed " + context.getSeed());
        this.currentMapContext = context;
        IMapGenerator activeMapGenerator = mapGeneratorFactory.createGenerator(context.getMapType());
        TileMap newMap = activeMapGenerator.generateMap(
                AppConfig.getInstance().world().getWidth(),
                AppConfig.getInstance().world().getHeight(),
                context
        );
        onMapGenerated(newMap, activeMapGenerator);
    }

    @Override
    public void onMapGenerated(TileMap newMap, IMapGenerator newGenerator) {
        gameState.setCurrentMap(newMap);
        gameState.setDroppedItems(new ArrayList<>());
        gameState.setEnemies(new ArrayList<>());

        spawnEnemies(newMap);
        mapConfigPanel.updateValuesFromGenerator(newGenerator);
    }

    public void spawnEnemies(TileMap map) {
        List<Enemy> enemies = new ArrayList<>();
        Player player = gameState.getPlayer();
        int[] playerPosition = null;
        if (player != null) {
            playerPosition = new int[]{player.getCurrentTileX(), player.getCurrentTileY()};
        }

        int numSlimes = AppConfig.getInstance().world().getBaseSlimesCount();
        System.out.println("MapManager: Trying to generate " + numSlimes + " slimes.");
        for (int i = 0; i < numSlimes; i++) {
            int[] enemyPosition = findSafePosition(playerPosition, map);
            if (enemyPosition != null) {
                enemies.add(new Slime(enemyPosition[0], enemyPosition[1]));
            } else {
                System.out.println("MapManager: Couldn't find a safe position for Slime #" + (i + 1));
            }
        }
        int numMimics = AppConfig.getInstance().world().getBaseMimicsCount();
        System.out.println("MapManager: Trying to generate " + numMimics + " mimics.");
        for (int i = 0; i < numMimics; i++) {
            int[] enemyPosition = findSafePosition(playerPosition, map);
            if (enemyPosition != null) {
                enemies.add(new Mimic(enemyPosition[0], enemyPosition[1]));
            } else {
                System.out.println("MapManager: Couldn't find a safe position for Mimic #" + (i + 1));
            }
        }
        gameState.setEnemies(enemies);
        System.out.println("MapManager: " + enemies.size() + " enemies generated and added to game state.");
    }

    private int[] findSafePosition(int[] referencePosition, TileMap map) {
        Random random = new Random();
        int attempts = 0;
        int maxAttempts = AppConfig.getInstance().world().getMaxPositioningAttempts();

        while (attempts < maxAttempts) {
            int x = random.nextInt(map.getWidth());
            int y = random.nextInt(map.getHeight());
            Tile tile = map.getTile(x, y);

            if (tile == null || !tile.getType().isWalkable() || tile.hasResource()) {
                attempts++;
                continue;
            }

            if (isPositionSurrounded(x, y, map)) {
                attempts++;
                continue;
            }

            if (referencePosition != null) {
                double distance = Math.sqrt(Math.pow(x - referencePosition[0], 2) + Math.pow(y - referencePosition[1], 2));
                if (distance < 10) {
                    attempts++;
                    continue;
                }
            }

            return new int[]{x, y};
        }
        return null;
    }

    private boolean isPositionSurrounded(int x, int y, TileMap map) {
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        int walkableNeighbors = 0;

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                Tile neighbor = map.getTile(nx, ny);
                if (neighbor != null && neighbor.getType().isWalkable()) {
                    walkableNeighbors++;
                }
            }
        }
        return walkableNeighbors < 2;
    }

    @Override
    public void onMapConfigChanged(MapType mapType, long seed,
                                   float heightFreq, int heightOctaves,
                                   float tempFreq, int tempOctaves,
                                   float moistureFreq, int moistureOctaves,
                                   float stoneFreq, float stoneThreshold,
                                   float crystalsFreq, float crystalsThreshold,
                                   float treeDensity, float riverDensity) {
        AppConfig.getInstance().world().setCurrentMapType(mapType);
        MapGenerationContext newContext = new MapGenerationContext(
                mapType, seed,
                heightFreq, heightOctaves,
                tempFreq, tempOctaves,
                moistureFreq, moistureOctaves,
                stoneFreq, stoneThreshold,
                crystalsFreq, crystalsThreshold,
                treeDensity, riverDensity
        );
        generateNewMap(newContext);
    }

    public MapGenerationContext getCurrentMapContext() {
        return currentMapContext;
    }

}