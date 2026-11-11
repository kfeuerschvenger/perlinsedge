package com.feuerschvenger.perlinsedge.ui.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MainMenuPanel extends VBox {

    private Runnable onNewGameAction;
    private Runnable onLoadGameAction;
    private Runnable onExitAction;

    public MainMenuPanel() {
        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPrefSize(400, 300);
        getStyleClass().add(UiTheme.PANEL);

        Label title = new Label(UiTheme.caps("Perlin's Edge"));
        title.getStyleClass().add(UiTheme.TITLE);

        Button newGameButton = createMenuButton("New Game");
        Button loadGameButton = createMenuButton("Load Game");
        Button exitButton = createMenuButton("Exit");

        loadGameButton.setDisable(true);

        newGameButton.setOnAction(e -> {
            if (onNewGameAction != null) {
                onNewGameAction.run();
            }
        });

        loadGameButton.setOnAction(e -> {
            if (onLoadGameAction != null) {
                onLoadGameAction.run();
            }
        });

        exitButton.setOnAction(e -> {
            if (onExitAction != null) {
                onExitAction.run();
            }
        });

        getChildren().addAll(title, newGameButton, loadGameButton, exitButton);
    }

    private Button createMenuButton(String text) {
        Button button = new Button(UiTheme.caps(text));
        button.setPrefWidth(240);
        button.setPrefHeight(40);
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
