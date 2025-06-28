module com.feuerschvenger.perlinsedge {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.controlsfx.controls;
    requires java.desktop;
    requires java.logging;

    exports com.feuerschvenger.perlinsedge.app;
    exports com.feuerschvenger.perlinsedge.app.managers;
    exports com.feuerschvenger.perlinsedge.config;
    exports com.feuerschvenger.perlinsedge.domain.crafting;
    exports com.feuerschvenger.perlinsedge.domain.entities;
    exports com.feuerschvenger.perlinsedge.domain.entities.items;
    exports com.feuerschvenger.perlinsedge.domain.entities.enemies;
    exports com.feuerschvenger.perlinsedge.domain.entities.containers;
    exports com.feuerschvenger.perlinsedge.domain.entities.buildings;
    exports com.feuerschvenger.perlinsedge.domain.events;
    exports com.feuerschvenger.perlinsedge.domain.pathfinding;
    exports com.feuerschvenger.perlinsedge.domain.strategies;
    exports com.feuerschvenger.perlinsedge.domain.utils;
    exports com.feuerschvenger.perlinsedge.domain.world.generation;
    exports com.feuerschvenger.perlinsedge.domain.world.generation.util;
    exports com.feuerschvenger.perlinsedge.domain.world.model;
    exports com.feuerschvenger.perlinsedge.infra.fx.graphics;
    exports com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;
    exports com.feuerschvenger.perlinsedge.infra.fx.input;
    exports com.feuerschvenger.perlinsedge.infra.fx.utils;
    exports com.feuerschvenger.perlinsedge.infra.noise;
    exports com.feuerschvenger.perlinsedge.ui.controller;
    exports com.feuerschvenger.perlinsedge.ui.view;


    opens com.feuerschvenger.perlinsedge.app to javafx.fxml, javafx.graphics;
}