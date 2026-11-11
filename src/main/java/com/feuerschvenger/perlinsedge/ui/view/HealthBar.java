package com.feuerschvenger.perlinsedge.ui.view;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Custom health bar UI component with visual heart icon.
 * Features dynamic coloring based on health percentage and rounded bar design.
 */
@Slf4j
public class HealthBar extends VBox {
    // Constants for sizing and styling
    private static final double BAR_WIDTH = 15;
    private static final double BAR_HEIGHT = 100;
    private static final double HEART_SIZE = 32; // the 16x16 icon at 2x, so its pixels stay whole
    private static final double CORNER_RADIUS = 5;
    private static final double STROKE_WIDTH = 2;
    private static final double SPACING = 2;

    // Health color thresholds
    private static final double HIGH_HEALTH = 0.6;
    private static final double MEDIUM_HEALTH = 0.3;

    // Health color values (RGB)
    private static final Color HIGH_HEALTH_COLOR = Color.rgb(252, 92, 101);
    private static final Color MEDIUM_HEALTH_COLOR = Color.rgb(242, 75, 98);
    private static final Color LOW_HEALTH_COLOR = Color.rgb(235, 59, 90);
    private static final Color BACKGROUND_COLOR = Color.GRAY;
    private static final Color STROKE_COLOR = Color.BLACK;

    private final Rectangle healthRect;
    private final Rectangle backgroundRect;
    private final ImageView heartIcon;
    private final Map<HeartState, Image> heartImages = new EnumMap<>(HeartState.class);
    private HeartState heartState;

    public HealthBar() {
        // Configure container
        setAlignment(Pos.CENTER);
        setSpacing(SPACING);
        setPrefSize(BAR_WIDTH, BAR_HEIGHT + SPACING + HEART_SIZE);

        // Create health bar components
        backgroundRect = createBackgroundRect();
        healthRect = createHealthRect();
        heartIcon = createHeartIcon();

        // Assemble health bar
        StackPane healthBarContainer = new StackPane();
        healthBarContainer.setPrefSize(BAR_WIDTH, BAR_HEIGHT);
        healthBarContainer.getChildren().add(backgroundRect);
        healthBarContainer.getChildren().add(healthRect);

        StackPane.setAlignment(healthRect, Pos.BOTTOM_CENTER);

        getChildren().addAll(healthBarContainer, heartIcon);
    }

    /**
     * Updates the health bar visualization based on current health.
     *
     * @param currentHealth The current health value
     * @param maxHealth The maximum health value
     */
    public void updateHealth(double currentHealth, double maxHealth) {
        double healthPercentage = calculateHealthPercentage(currentHealth, maxHealth);
        updateBarDimensions(healthPercentage);
        updateBarColor(healthPercentage);
        updateHeart(HeartState.of(healthPercentage));
    }

    // ==================================================================
    //  PRIVATE HELPER METHODS
    // ==================================================================

    private Rectangle createBackgroundRect() {
        Rectangle rect = new Rectangle(BAR_WIDTH, BAR_HEIGHT, BACKGROUND_COLOR);
        styleRectangle(rect);
        return rect;
    }

    private Rectangle createHealthRect() {
        Rectangle rect = new Rectangle(BAR_WIDTH, BAR_HEIGHT, HIGH_HEALTH_COLOR);
        styleRectangle(rect);
        return rect;
    }

    private void styleRectangle(Rectangle rect) {
        rect.setArcWidth(CORNER_RADIUS);
        rect.setArcHeight(CORNER_RADIUS);
        rect.setStroke(STROKE_COLOR);
        rect.setStrokeWidth(STROKE_WIDTH);
    }

    private ImageView createHeartIcon() {
        ImageView icon = new ImageView();
        icon.setFitWidth(HEART_SIZE);
        icon.setFitHeight(HEART_SIZE);
        icon.setSmooth(false);

        for (HeartState state : HeartState.values()) {
            Image image = loadHeart(state);
            if (image != null) {
                heartImages.put(state, image);
            }
        }
        heartState = HeartState.FULL;
        icon.setImage(imageFor(heartState, icon));
        return icon;
    }

    private void updateHeart(HeartState state) {
        if (state != heartState) {
            heartState = state;
            heartIcon.setImage(imageFor(state, heartIcon));
        }
    }

    private Image imageFor(HeartState state, ImageView icon) {
        Image image = heartImages.get(state);
        if (image != null) {
            return image;
        }
        // Without art the placeholder is built once and kept for every state
        return heartImages.computeIfAbsent(state, ignored -> createFallbackHeartImage());
    }

    private Image loadHeart(HeartState state) {
        try (InputStream is = getClass().getResourceAsStream(state.getPath())) {
            if (is == null) {
                log.debug("Heart icon not found: {}", state.getPath());
                return null;
            }
            return new Image(is);
        } catch (Exception e) {
            log.warn("Heart icon could not be loaded: {}", state.getPath(), e);
            return null;
        }
    }

    private Image createFallbackHeartImage() {
        Rectangle heartPlaceholder = new Rectangle(HEART_SIZE, HEART_SIZE, Color.PINK);
        heartPlaceholder.setArcWidth(HEART_SIZE / 2);
        heartPlaceholder.setArcHeight(HEART_SIZE / 2);
        return heartPlaceholder.snapshot(null, null);
    }

    private double calculateHealthPercentage(double current, double max) {
        return Math.max(0, Math.min(1, current / max));
    }

    private void updateBarDimensions(double percentage) {
        healthRect.setHeight(BAR_HEIGHT * percentage);
    }

    private void updateBarColor(double percentage) {
        if (percentage > HIGH_HEALTH) {
            healthRect.setFill(HIGH_HEALTH_COLOR);
        } else if (percentage > MEDIUM_HEALTH) {
            healthRect.setFill(MEDIUM_HEALTH_COLOR);
        } else {
            healthRect.setFill(LOW_HEALTH_COLOR);
        }
    }

}