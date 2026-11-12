package com.feuerschvenger.perlinsedge.ui.view;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every button of the theme looks plainly different when it is disabled. */
class DisabledStateTest {

    private static Button styledButton(boolean disabled, String... extraClasses) {
        return FxToolkitSupport.onFx(() -> {
            Button button = new Button("Craft");
            button.getStyleClass().addAll(extraClasses);
            button.setDisable(disabled);
            StackPane root = new StackPane(button);
            Scene scene = new Scene(root, 200, 100);
            scene.getStylesheets().add(UiTheme.class.getResource(UiTheme.STYLESHEET_PATH).toExternalForm());
            root.applyCss();
            return button;
        });
    }

    @Test
    void aDisabledButtonIsDimmerAndGreyerThanAnEnabledOne() {
        Button enabled = styledButton(false);
        Button disabled = styledButton(true);

        assertEquals(1.0, FxToolkitSupport.onFx(enabled::getOpacity), 1e-9);
        assertTrue(FxToolkitSupport.onFx(disabled::getOpacity) <= 0.6, "a disabled button is faded");

        Color enabledText = (Color) FxToolkitSupport.onFx(enabled::getTextFill);
        Color disabledText = (Color) FxToolkitSupport.onFx(disabled::getTextFill);
        assertNotEquals(enabledText, disabledText);
        assertTrue(disabledText.getBrightness() < enabledText.getBrightness() - 0.2,
                "the text is clearly darker: " + disabledText + " against " + enabledText);
        assertTrue(disabledText.getSaturation() < 0.3, "and grey: " + disabledText);
    }

    @Test
    void everyButtonVariantHasTheDisabledLook() {
        for (String variant : new String[] {UiTheme.CARD, UiTheme.PAGE_BUTTON}) {
            Button disabled = styledButton(true, variant);

            assertTrue(FxToolkitSupport.onFx(disabled::getOpacity) <= 0.6, variant);
        }
    }
}
