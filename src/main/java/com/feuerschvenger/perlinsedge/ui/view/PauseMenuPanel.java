package com.feuerschvenger.perlinsedge.ui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Panel for displaying the pause menu in the game.
 * Contains buttons for resuming, saving, loading, and exiting the game.
 */
public class PauseMenuPanel extends VBox {
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
        setPadding(new Insets(20));
        setSpacing(20);
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");

        // Create title text
        Text title = new Text("Game Paused");
        title.setFont(Font.font(24));
        title.setFill(Color.WHITE);

        // Create buttons
        resumeButton = new Button("Resume Game");
        saveButton = new Button("Save Game");
        loadButton = new Button("Load Game");
        backToMainMenuButton = new Button("Back to Main Menu");
        exitButton = new Button("Exit to Desktop");

        // Set a common preferred width for all buttons
        double buttonWidth = 200;
        resumeButton.setPrefWidth(buttonWidth);
        saveButton.setPrefWidth(buttonWidth);
        loadButton.setPrefWidth(buttonWidth);
        backToMainMenuButton.setPrefWidth(buttonWidth);
        exitButton.setPrefWidth(buttonWidth);

        saveButton.setDisable(true);
        saveButton.setTextFill(Color.GRAY);
        loadButton.setDisable(true);
        loadButton.setTextFill(Color.GRAY);

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