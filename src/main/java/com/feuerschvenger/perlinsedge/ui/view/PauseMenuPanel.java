package com.feuerschvenger.perlinsedge.ui.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Panel for displaying the pause menu in the game.
 * Contains buttons for resuming, saving, loading, and exiting the game.
 */
public class PauseMenuPanel extends VBox {
    private static final double BUTTON_WIDTH = 240;

    private final Button resumeButton;
    private final Button saveButton;
    private final Button loadButton;
    private final Button backToMainMenuButton;
    private final Button exitButton;

    private Runnable onBackToMainMenuAction;

    /**
     * Constructor for the PauseMenuPanel.
     * Initializes the layout, buttons, and styles.
     */
    public PauseMenuPanel() {
        // Set panel layout properties
        setAlignment(Pos.CENTER);
        setSpacing(16);
        getStyleClass().add(UiTheme.PANEL);

        // Create title text
        Label title = new Label(UiTheme.caps("Game Paused"));
        title.getStyleClass().add(UiTheme.HEADING);

        // Create buttons
        resumeButton = createButton("Resume Game");
        saveButton = createButton("Save Game");
        loadButton = createButton("Load Game");
        backToMainMenuButton = createButton("Back to Main Menu");
        exitButton = createButton("Exit to Desktop");

        saveButton.setDisable(true);
        loadButton.setDisable(true);

        // Add elements to the layout
        getChildren().addAll(title, resumeButton, saveButton, loadButton, backToMainMenuButton, exitButton);

        // Initially hidden and unmanaged until needed
        setVisible(false);
        setManaged(false);

        backToMainMenuButton.setOnAction(e -> {
            if (onBackToMainMenuAction != null) {
                onBackToMainMenuAction.run();
            }
        });
    }

    private static Button createButton(String text) {
        Button button = new Button(UiTheme.caps(text));
        button.setPrefWidth(BUTTON_WIDTH);
        return button;
    }

    // Event handler methods
    public void setOnResumeAction(Runnable action) {
        resumeButton.setOnAction(e -> action.run());
    }

    public void setOnSaveAction(Runnable action) {
        saveButton.setOnAction(e -> action.run());
    }

    public void setOnLoadAction(Runnable action) {
        loadButton.setOnAction(e -> action.run());
    }

    public void setOnBackToMainMenuAction(Runnable action) {
        this.onBackToMainMenuAction = action;
    }

    public void setOnExitAction(Runnable action) {
        exitButton.setOnAction(e -> action.run());
    }

}
