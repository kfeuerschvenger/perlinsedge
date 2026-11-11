package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FrameStats;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.Locale;

public final class FxDebugDrawingHelper {

    private FxDebugDrawingHelper() {} // Prevent instantiation

    /**
     * Draws the compact FPS overlay in the top-left corner. It is toggled with F6 and works without debug mode.
     *
     * @param gc          graphics context
     * @param stats       smoothed frame statistics
     * @param queuedItems number of drawables in the frame
     */
    public static void drawFpsOverlay(GraphicsContext gc, FrameStats stats, int queuedItems) {
        gc.save();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(8, 8, 150, 58);
        gc.setFill(Color.LIME);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format(Locale.ROOT, "FPS: %.1f", stats.getFps()), 16, 28);
        gc.fillText(String.format(Locale.ROOT, "Frame: %.2f ms", stats.getLastFrameMillis()), 16, 44);
        gc.fillText(String.format(Locale.ROOT, "Drawn: %d", queuedItems), 16, 60);
        gc.restore();
    }

    public static void drawDebugInfo(GraphicsContext gc, GameStateManager gameState, Canvas canvas, double deltaTime) {
        if (!AppConfig.getInstance().debug().isEnabled()) return;

        gc.save();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.RIGHT); // Alinear a la derecha

        double fps = (deltaTime > 0) ? 1.0 / deltaTime : 0;
        double xPos = canvas.getWidth() - 12; // Margen derecho
        double yPos = 30;

        gc.fillText(String.format(Locale.ROOT, "FPS: %.1f", fps), xPos, yPos);
        yPos += 20;
        gc.fillText(String.format(Locale.ROOT, "Frame: %.3fms", deltaTime * 1000), xPos, yPos);
        yPos += 20;
        gc.fillText(String.format(Locale.ROOT, "Game Time: %.1fs", gameState.getGameTime()), xPos, yPos);

        Player player = gameState.getPlayer();
        if (player != null) {
            yPos += 20;
            gc.fillText(String.format("Player: %d,%d",
                    player.getCurrentTileX(), player.getCurrentTileY()), xPos, yPos);
            yPos += 20;
            gc.fillText(String.format("Health: %d/%d", (int) player.getHealth(), (int) player.getMaxHealth()), xPos, yPos);
        }

        int mouseOverTileX = gameState.getMouseOverTileX();
        int mouseOverTileY = gameState.getMouseOverTileY();
        if (mouseOverTileX != -1) {
            yPos += 20;
            gc.fillText(String.format("Tile: %d,%d", mouseOverTileX, mouseOverTileY), xPos, yPos);

            Tile tile = gameState.getCurrentMap().getTile(mouseOverTileX, mouseOverTileY);
            if (tile != null) {
                yPos += 20;
                gc.fillText(String.format("Type: %s", tile.getType().name()), xPos, yPos);
                yPos += 20;
                gc.fillText(String.format("Resource: %s",
                        tile.hasResource() ? tile.getWorldResource().getType().name() : "None"), xPos, yPos);
            }
        }

        yPos += 20;
        List<Enemy> enemies = gameState.getEnemies();
        gc.fillText(String.format("Enemies: %d", (enemies != null ? enemies.size() : 0)), xPos, yPos);

        yPos += 20;
        List<DroppedItem> droppedItems = gameState.getDroppedItems();
        gc.fillText(String.format("Dropped Items: %d", (droppedItems != null ? droppedItems.size() : 0)), xPos, yPos);

        yPos += 20;
        int total = (enemies != null ? enemies.size() : 0) + (droppedItems != null ? droppedItems.size() : 0);
        gc.fillText(String.format("Total Entities: %d", total), xPos, yPos);

        yPos += 20;
        gc.fillText(String.format("Mode: %s", gameState.getCurrentGameMode().name()), xPos, yPos);

        gc.restore();
    }

}
