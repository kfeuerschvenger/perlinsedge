package com.feuerschvenger.perlinsedge.ui.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class MainMenuPanel extends VBox {

    private Runnable onNewGameAction;
    private Runnable onLoadGameAction;
    private Runnable onExitAction;

    public MainMenuPanel() {
        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPrefSize(400, 300);

        setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);" +
                "-fx-border-color: #333333;" +
                "-fx-border-width: 2;" +
                "-fx-padding: 20;");

        Text title = new Text("Perlin's Edge");
        title.setFont(Font.font("Arial", 48));
        title.setFill(Color.WHITE);

        Button newGameButton = createMenuButton("New Game");
        Button loadGameButton = createMenuButton("Load Game");
        Button exitButton = createMenuButton("Exit");

        loadGameButton.setDisable(true);
        loadGameButton.setTextFill(Color.GRAY);

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
        Button button = new Button(text);
        button.setPrefWidth(200);
        button.setPrefHeight(40);
        button.setFont(Font.font("Arial", 18));
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: #555555; -fx-background-radius: 5;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #777777; -fx-background-radius: 5;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #555555; -fx-background-radius: 5;"));
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