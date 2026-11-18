package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.ui.view.BuildingPalettePanel;
import com.feuerschvenger.perlinsedge.ui.view.DeathOverlay;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.HintLabel;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.ItemHotbarPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;

/**
 * The panels the orchestrator feeds and listens to.
 *
 * @param mapConfigPanel       map tuning panel
 * @param healthBar            player health view
 * @param inventoryPanel       inventory view
 * @param buildingPalettePanel palette of the building pieces, shown while the hammer is in hand
 * @param itemHotbarPanel      hotbar of the items the player carries
 * @param hintLabel            short on-screen hints for actions that did nothing
 * @param deathOverlay         death screen shown while the player is dead
 */
public record GamePanels(MapConfigPanel mapConfigPanel, HealthBar healthBar, InventoryPanel inventoryPanel,
                         BuildingPalettePanel buildingPalettePanel, ItemHotbarPanel itemHotbarPanel,
                         HintLabel hintLabel, DeathOverlay deathOverlay) {
}
