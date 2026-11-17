package com.feuerschvenger.perlinsedge.ui.view;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

/**
 * Entry point of the GUI theme: loads the pixel font once, attaches the single stylesheet to a scene and names
 * the style classes the panels use, so no panel needs an inline style string.
 * <p>
 * The stylesheet is an author stylesheet, so it wins over values set from code on the same property. Nodes that
 * carry a theme style class therefore get their colours, borders and font sizes from the stylesheet only.
 */
@Slf4j
public final class UiTheme {
    /** Classpath location of the stylesheet. */
    public static final String STYLESHEET_PATH = "/ui/theme.css";
    /** Classpath location of the bundled font. */
    public static final String FONT_PATH = "/assets/fonts/PixelifySans.ttf";
    /** Family name of the bundled font, as the stylesheet refers to it. */
    public static final String FONT_FAMILY = "Pixelify Sans";

    /** Dark translucent rounded panel. */
    public static final String PANEL = "pe-panel";
    /** Full screen translucent backdrop. */
    public static final String BACKDROP = "pe-backdrop";
    /** Background of an inventory grid. */
    public static final String GRID = "pe-grid";
    /** Large title text. */
    public static final String TITLE = "pe-title";
    /** Large danger-coloured title text. */
    public static final String TITLE_DANGER = "pe-title-danger";
    /** Section heading text. */
    public static final String HEADING = "pe-heading";
    /** Muted secondary text. */
    public static final String MUTED = "pe-muted";
    /** Title of a small HUD panel, bold and slightly larger than the lines under it. */
    public static final String PANEL_TITLE = "pe-panel-title";
    /** Small text. */
    public static final String SMALL = "pe-small";
    /** Dark pill behind a short label. */
    public static final String PILL = "pe-pill";
    /** Large button that holds a map choice. */
    public static final String CARD = "pe-card";
    /** Compact button, such as the hotbar page buttons. */
    public static final String PAGE_BUTTON = "pe-page-button";
    /** Square slot of the hotbar or an inventory grid. */
    public static final String SLOT = "pe-slot";
    /** Slot that holds nothing. */
    public static final String SLOT_EMPTY = "pe-slot-empty";
    /** Slot that is selected. */
    public static final String SLOT_SELECTED = "pe-slot-selected";
    /** Hotbar slot picked in the open inventory, distinct from the selected slot that is held in hand. */
    public static final String SLOT_PICKED = "pe-slot-picked";
    /** Slot that would accept the dragged item. */
    public static final String SLOT_DROP = "pe-slot-drop";
    /** Slot number shown in the corner of a hotbar slot. */
    public static final String SLOT_NUMBER = "pe-slot-number";
    /** Short text drawn on a slot icon. */
    public static final String SLOT_LABEL = "pe-slot-label";
    /** Item count at the bottom right of a slot. */
    public static final String COUNT = "pe-count";
    /** Dark list background. */
    public static final String LIST = "pe-list";
    /** Row of a list. */
    public static final String CELL = "pe-cell";
    /** Selected row of a list. */
    public static final String CELL_SELECTED = "pe-cell-selected";
    /** Crafting list cell of a recipe that cannot be crafted now. */
    public static final String CELL_UNAVAILABLE = "pe-cell-unavailable";
    /** Text that states a requirement that is met. */
    public static final String TEXT_GOOD = "pe-good";
    /** Text that states a requirement that is not met. */
    public static final String TEXT_BAD = "pe-bad";

    /** Opacity of the icon of something the player cannot afford. */
    private static final double UNAVAILABLE_ICON_OPACITY = 0.6;
    /** Removes all the colour of an icon and darkens it a little. One instance is shared, it has no state. */
    private static final ColorAdjust GREYSCALE = new ColorAdjust(0, -1, -0.2, 0);

    private static boolean fontLoaded;

    /** Not instantiable: the class only holds the theme entry points. */
    private UiTheme() {
    }

    /**
     * Attaches the theme to a scene, loading the font first. Calling it again for the same scene changes nothing.
     *
     * @param scene the scene that hosts every screen of the game
     */
    public static void install(Scene scene) {
        loadFont();
        URL stylesheet = UiTheme.class.getResource(STYLESHEET_PATH);
        if (stylesheet == null) {
            log.warn("Theme stylesheet not found: {}", STYLESHEET_PATH);
            return;
        }
        String external = stylesheet.toExternalForm();
        if (!scene.getStylesheets().contains(external)) {
            scene.getStylesheets().add(external);
        }
    }

    /** Registers the bundled font with JavaFX, once. A missing or unreadable font leaves the default font in use. */
    public static synchronized void loadFont() {
        if (fontLoaded) {
            return;
        }
        fontLoaded = true;
        try (InputStream in = UiTheme.class.getResourceAsStream(FONT_PATH)) {
            Font font = in == null ? null : Font.loadFont(in, 16);
            if (font == null) {
                log.warn("Font {} could not be loaded, the default font is used", FONT_PATH);
            } else {
                log.info("Loaded font {}", font.getFamily());
            }
        } catch (Exception e) {
            log.warn("Font {} could not be loaded, the default font is used", FONT_PATH, e);
        }
    }

    /**
     * Adds a style class to a node when the flag is set and removes it otherwise. Adding twice never duplicates it.
     *
     * @param node  node to change
     * @param style style class to add or remove
     * @param on    whether the node carries the class
     */
    public static void setClass(Node node, String style, boolean on) {
        if (on) {
            if (!node.getStyleClass().contains(style)) {
                node.getStyleClass().add(style);
            }
        } else {
            node.getStyleClass().remove(style);
        }
    }

    /**
     * Shows an icon as unavailable or restores it. The look is a real greyscale and a fade, not a fade alone: a faded
     * icon keeps its hue and still reads as available. Every list that greys out what the player cannot afford uses this.
     *
     * @param icon        the icon to change
     * @param unavailable true to grey the icon, false to show it in full colour
     */
    public static void setIconUnavailable(ImageView icon, boolean unavailable) {
        icon.setEffect(unavailable ? GREYSCALE : null);
        icon.setOpacity(unavailable ? UNAVAILABLE_ICON_OPACITY : 1.0);
    }

    /**
     * Upper-cases a label, as the theme draws every label in capitals.
     *
     * @param text text to convert, may be null
     * @return the text in upper case, or an empty string for null
     */
    public static String caps(String text) {
        return text == null ? "" : text.toUpperCase(Locale.ROOT);
    }
}
