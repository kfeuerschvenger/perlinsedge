package com.feuerschvenger.perlinsedge.ui.view;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class MainMenuPanel extends VBox {
    private static final double SPACING = 20;
    private static final double PREF_WIDTH = 400;
    private static final double PREF_HEIGHT = 300;
    private static final double BUTTON_WIDTH = 240;
    private static final double BUTTON_HEIGHT = 40;

    private Runnable onNewGameAction;
    private Runnable onLoadGameAction;
    private Runnable onExitAction;

    public MainMenuPanel() {
        setSpacing(SPACING);
        setAlignment(Pos.CENTER);
        setPrefSize(PREF_WIDTH, PREF_HEIGHT);
        getStyleClass().add(UiTheme.PANEL);

        var title = new Label(UiTheme.caps("Perlin's Edge"));
        title.getStyleClass().add(UiTheme.TITLE);

        var newGameButton = createMenuButton("New Game");
        var loadGameButton = createMenuButton("Load Game");
        var exitButton = createMenuButton("Exit");

        loadGameButton.setDisable(true);

        newGameButton.setOnAction((ActionEvent e) -> {
            if (onNewGameAction != null) {
                onNewGameAction.run();
            }
        });

        loadGameButton.setOnAction((ActionEvent e) -> {
            if (onLoadGameAction != null) {
                onLoadGameAction.run();
            }
        });

        exitButton.setOnAction((ActionEvent e) -> {
            if (onExitAction != null) {
                onExitAction.run();
            }
        });

        getChildren().addAll(title, newGameButton, loadGameButton, exitButton);
    }

    private static Button createMenuButton(String text) {
        var button = new Button(UiTheme.caps(text));
        button.setPrefWidth(BUTTON_WIDTH);
        button.setPrefHeight(BUTTON_HEIGHT);
        return button;
    }

    public void setOnNewGameAction(Runnable onNewGameAction) {
        this.onNewGameAction = onNewGameAction;
    }

    public void setOnLoadGameAction(Runnable onLoadGameAction) {
        this.onLoadGameAction = onLoadGameAction;
    }

    public void setOnExitAction(Runnable onExitAction) {
        this.onExitAction = onExitAction;
    }
}
