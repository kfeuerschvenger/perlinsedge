module com.feuerschvenger.perlinsedge {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop;
    requires java.logging;
    requires org.slf4j;
    requires static lombok;

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
    exports com.feuerschvenger.perlinsedge.ui.view.callbacks;

    opens com.feuerschvenger.perlinsedge.app to javafx.graphics;
}