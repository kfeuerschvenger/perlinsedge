package com.feuerschvenger.perlinsedge.ui.view;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Player health as a horizontal row of heart icons, each shown full, half or empty.
 * The 16x16 icons are drawn at 2x with smoothing off, so every art pixel stays a whole square block.
 */
@Slf4j
public class HealthBar extends HBox {
    /** Hearts in the row. */
    public static final int HEART_COUNT = 5;

    private static final double HEART_SIZE = 32; // the 16x16 icon at 2x
    private static final double SPACING = 4;

    private final ImageView[] icons = new ImageView[HEART_COUNT];
    private final Map<HeartState, Image> heartImages = new EnumMap<>(HeartState.class);
    /** The values the row shows now, so an unchanged health costs nothing on the fixed update. */
    private double shownHealth = Double.NaN;
    private double shownMaxHealth = Double.NaN;

    public HealthBar() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(SPACING);

        for (HeartState state : HeartState.values()) {
            heartImages.put(state, loadHeart(state));
        }
        for (int i = 0; i < HEART_COUNT; i++) {
            ImageView icon = new ImageView();
            icon.setFitWidth(HEART_SIZE);
            icon.setFitHeight(HEART_SIZE);
            icon.setSmooth(false);
            icons[i] = icon;
            getChildren().add(icon);
        }
        updateHealth(1, 1);
    }

    /**
     * Updates the hearts to show the current health.
     *
     * @param currentHealth The current health value
     * @param maxHealth The maximum health value
     */
    public void updateHealth(double currentHealth, double maxHealth) {
        if (currentHealth == shownHealth && maxHealth == shownMaxHealth) {
            return;
        }
        shownHealth = currentHealth;
        shownMaxHealth = maxHealth;
        double fraction = maxHealth > 0 ? currentHealth / maxHealth : 0;
        HeartState[] states = HeartState.row(fraction, HEART_COUNT);
        for (int i = 0; i < HEART_COUNT; i++) {
            Image image = heartImages.get(states[i]);
            if (icons[i].getImage() != image) {
                icons[i].setImage(image);
            }
        }
    }

    private Image loadHeart(HeartState state) {
        try (InputStream is = getClass().getResourceAsStream(state.getPath())) {
            if (is != null) {
                return new Image(is);
            }
            log.info("Heart icon not found: {}, a placeholder is drawn", state.getPath());
        } catch (Exception e) {
            log.warn("Heart icon could not be loaded: {}", state.getPath(), e);
        }
        return createFallbackHeartImage(state);
    }

    private Image createFallbackHeartImage(HeartState state) {
        Color color = state == HeartState.EMPTY ? Color.DARKGRAY : Color.PINK;
        Rectangle placeholder = new Rectangle(HEART_SIZE, HEART_SIZE, color);
        placeholder.setArcWidth(HEART_SIZE / 2);
        placeholder.setArcHeight(HEART_SIZE / 2);
        return placeholder.snapshot(null, null);
    }

}
