package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
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

    /** Minutes in one hour, to split the minute of the day into the hours and minutes of the clock. */
    private static final int MINUTES_PER_HOUR = 60;
    /** Font size of the overlay text, in screen pixels. */
    private static final double FONT_SIZE = 14;
    /** Gap between the text and the right edge of the canvas. */
    private static final double RIGHT_MARGIN = 12;
    /** Y of the first line of the overlay. */
    private static final double FIRST_LINE_Y = 30;
    /** Vertical distance between two lines of the overlay. */
    private static final double LINE_HEIGHT = 20;

    private FxDebugDrawingHelper() {} // Prevent instantiation

    /**
     * Describes the sky for the debug overlay: the day, the phase and the time of day on the 24 hour clock. The day
     * number turns when the dawn begins, which is 03:00 on the clock, not at midnight.
     *
     * @param dayCycle the clock of the sky
     * @param fastTime whether the fast time of the debug key is on
     * @return a line such as {@code Sky: Day 1 DAY 06:00}, followed by {@code (fast)} while the fast time is on
     */
    public static String describeSky(DayCycle dayCycle, boolean fastTime) {
        final int minutes = dayCycle.clockMinuteOfDay();
        return String.format(Locale.ROOT, "Sky: Day %d %s %02d:%02d%s", dayCycle.dayNumber(), dayCycle.phase().name(),
                minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR, fastTime ? " (fast)" : "");
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
        if (!AppConfig.getInstance().debug().isEnabled()) {
            return;
        }

        gc.save();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, FONT_SIZE));
        gc.setTextAlign(TextAlignment.RIGHT);

        double xPos = canvas.getWidth() - RIGHT_MARGIN;
        double yPos = FIRST_LINE_Y;

        gc.fillText(String.format(Locale.ROOT, "FPS: %.1f", stats.getFps()), xPos, yPos);
        yPos += LINE_HEIGHT;
        gc.fillText(String.format(Locale.ROOT, "Frame: %.2f ms", stats.getLastFrameMillis()), xPos, yPos);
        yPos += LINE_HEIGHT;
        gc.fillText(String.format(Locale.ROOT, "Drawn: %d", queuedItems), xPos, yPos);
        yPos += LINE_HEIGHT;
        gc.fillText(String.format(Locale.ROOT, "Game Time: %.1fs", gameState.getGameTime()), xPos, yPos);
        yPos += LINE_HEIGHT;
        gc.fillText(describeSky(gameState.getDayCycle(), AppConfig.getInstance().dayCycle().isFastTime()), xPos, yPos);

        final var player = gameState.getPlayer();
        if (player != null) {
            yPos += LINE_HEIGHT;
            gc.fillText(String.format("Player: %d,%d",
                    player.getCurrentTileX(), player.getCurrentTileY()), xPos, yPos);
            yPos += LINE_HEIGHT;
            gc.fillText(String.format("Health: %d/%d", (int) player.getHealth(), (int) player.getMaxHealth()), xPos, yPos);
        }

        int mouseOverTileX = gameState.getMouseOverTileX();
        int mouseOverTileY = gameState.getMouseOverTileY();
        if (mouseOverTileX != -1) {
            yPos += LINE_HEIGHT;
            gc.fillText(String.format("Tile: %d,%d", mouseOverTileX, mouseOverTileY), xPos, yPos);

            final var tile = gameState.getCurrentMap().getTile(mouseOverTileX, mouseOverTileY);
            if (tile != null) {
                yPos += LINE_HEIGHT;
                gc.fillText("Type: " + tile.getType().name(), xPos, yPos);
                yPos += LINE_HEIGHT;
                gc.fillText("Resource: "
                        + (tile.hasResource() ? tile.getWorldResource().getType().name() : "None"), xPos, yPos);
            }
        }

        yPos += LINE_HEIGHT;
        List<Creature> creatures = gameState.getCreatures();
        gc.fillText(String.format("Creatures: %d", (creatures != null ? creatures.size() : 0)), xPos, yPos);

        yPos += LINE_HEIGHT;
        List<DroppedItem> droppedItems = gameState.getDroppedItems();
        gc.fillText(String.format("Dropped Items: %d", (droppedItems != null ? droppedItems.size() : 0)), xPos, yPos);

        yPos += LINE_HEIGHT;
        int total = (creatures != null ? creatures.size() : 0) + (droppedItems != null ? droppedItems.size() : 0);
        gc.fillText(String.format("Total Entities: %d", total), xPos, yPos);

        yPos += LINE_HEIGHT;
        gc.fillText("Mode: " + gameState.getCurrentGameMode().name(), xPos, yPos);

        gc.restore();
    }

}
