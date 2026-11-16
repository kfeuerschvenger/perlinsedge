package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
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
     * Describes the sky for the debug overlay: the day, the phase and the time of day on the 24 hour clock.
     *
     * @param dayCycle the clock of the sky
     * @param fastTime whether the fast time of the debug key is on
     * @return a line such as {@code Sky: Day 1 DAY 06:00}, followed by {@code (fast)} while the fast time is on
     */
    public static String describeSky(DayCycle dayCycle, boolean fastTime) {
        final int minutes = dayCycle.clockMinuteOfDay();
        return String.format(Locale.ROOT, "Sky: Day %d %s %02d:%02d%s", dayCycle.dayNumber(), dayCycle.phase().name(),
                minutes / 60, minutes % 60, fastTime ? " (fast)" : "");
    }

    /**
     * Draws the debug overlay in the top-right corner when the debug mode is on: the smoothed frame rate and frame
     * time, the number of drawables in the frame, the game clock, the sky, the player, the tile under the mouse and
     * the entity counts.
     *
     * @param gc          graphics context
     * @param gameState   the game state to describe
     * @param canvas      the canvas, used to anchor the text to its right edge
     * @param stats       smoothed frame statistics
     * @param queuedItems number of drawables in the frame
     */
    public static void drawDebugInfo(GraphicsContext gc, GameStateManager gameState, Canvas canvas, FrameStats stats,
            int queuedItems) {
        if (!AppConfig.getInstance().debug().isEnabled()) return;

        gc.save();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.RIGHT); // Alinear a la derecha

        double xPos = canvas.getWidth() - 12; // Margen derecho
        double yPos = 30;

        gc.fillText(String.format(Locale.ROOT, "FPS: %.1f", stats.getFps()), xPos, yPos);
        yPos += 20;
        gc.fillText(String.format(Locale.ROOT, "Frame: %.2f ms", stats.getLastFrameMillis()), xPos, yPos);
        yPos += 20;
        gc.fillText(String.format(Locale.ROOT, "Drawn: %d", queuedItems), xPos, yPos);
        yPos += 20;
        gc.fillText(String.format(Locale.ROOT, "Game Time: %.1fs", gameState.getGameTime()), xPos, yPos);
        yPos += 20;
        gc.fillText(describeSky(gameState.getDayCycle(), AppConfig.getInstance().dayCycle().isFastTime()), xPos, yPos);

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
