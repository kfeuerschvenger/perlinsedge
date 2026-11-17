package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldTorchTest {

    private LightRegistry registry;
    private HeldTorch torch;
    private Player player;

    @BeforeEach
    void setUp() {
        registry = new LightRegistry();
        torch = AppConfig.getInstance().lights().newHeldTorch();
        player = new Player(5, 7);
        // The torch lands in the first hotbar slot, and slot one stays empty
        player.addItem(new Item(ItemType.TORCH, 3));
        player.selectHotbarSlot(0);
    }

    @Test
    void theTorchHasTheValuesOfTheConfiguration() {
        assertEquals(4.5, torch.radius(), 1e-12);
        assertEquals(1.0, torch.intensity(), 1e-12);
        assertEquals(1.0, torch.colour().r(), 1e-12);
        assertEquals(0.72, torch.colour().g(), 1e-12);
        assertEquals(0.38, torch.colour().b(), 1e-12);
        assertFalse(torch.flicker().isSteady());
        assertTrue(torch.flicker().intensityAmplitude() >= 0.08 && torch.flicker().intensityAmplitude() <= 0.12);
        assertTrue(torch.flicker().radiusAmplitude() >= 0.04 && torch.flicker().radiusAmplitude() <= 0.06);
    }

    @Test
    void aSelectedTorchGivesLightWhereThePlayerStands() {
        torch.sync(registry, player, 1.0);

        assertEquals(1, registry.size());
        assertSame(torch, registry.get(0));
        assertEquals(5.0, torch.x(), 1e-12);
        assertEquals(7.0, torch.y(), 1e-12);
    }

    @Test
    void theLightAppearsAndDisappearsTheMomentTheSelectionChanges() {
        torch.sync(registry, player, 1.0);
        assertTrue(registry.contains(torch));

        player.selectHotbarSlot(1);
        torch.sync(registry, player, 1.0);
        assertFalse(registry.contains(torch), "an empty hand gives no light");

        player.selectHotbarSlot(0);
        torch.sync(registry, player, 1.0);
        assertTrue(registry.contains(torch), "the torch is lit again as it is selected");
        assertEquals(1, registry.size(), "and registered once");
    }

    @Test
    void anotherItemInHandGivesNoLight() {
        player.addItem(new Item(ItemType.PICKAXE, 1));
        player.selectHotbarSlot(1);

        torch.sync(registry, player, 1.0);

        assertEquals(ItemType.PICKAXE, player.getActiveItemType());
        assertTrue(registry.isEmpty());
    }

    @Test
    void aTorchThatIsUsedUpGivesNoLight() {
        final Player lonely = new Player(1, 1);
        lonely.addItem(new Item(ItemType.TORCH, 1));
        lonely.selectHotbarSlot(0);
        torch.sync(registry, lonely, 1.0);
        assertTrue(registry.contains(torch));

        lonely.consumeActiveItem();
        torch.sync(registry, lonely, 1.0);

        assertFalse(registry.contains(torch));
    }

    @Test
    void aDeadPlayerGivesNoLightEvenWithTheTorchSelected() {
        torch.sync(registry, player, 1.0);

        player.takeDamage(1_000_000);
        assertTrue(player.isDead());
        assertEquals(ItemType.TORCH, player.getActiveItemType(), "the torch is still the selected item");
        torch.sync(registry, player, 1.0);

        assertFalse(registry.contains(torch));
    }

    @Test
    void aRevivedPlayerLightsTheTorchAgain() {
        player.takeDamage(1_000_000);
        torch.sync(registry, player, 1.0);
        assertTrue(registry.isEmpty());

        player.revive(2, 3);
        player.selectHotbarSlot(0);
        player.addItem(new Item(ItemType.TORCH, 1));
        torch.sync(registry, player, 1.0);

        assertEquals(1, registry.size());
        assertEquals(2.0, torch.x(), 1e-12);
    }

    @Test
    void noPlayerMeansNoLight() {
        torch.sync(registry, player, 1.0);

        torch.sync(registry, null, 1.0);

        assertTrue(registry.isEmpty());
        assertFalse(HeldTorch.isLit(null));
    }

    @Test
    void theLightFollowsThePlayerAndItsInterpolation() {
        torch.sync(registry, player, 1.0);
        assertEquals(5.0, torch.x(), 1e-12);

        // A step is made: the player is now at 6, and was at 5 at the update before
        player.setPosition(6, 7);
        player.setPosX(6.0);
        torch.sync(registry, player, 1.0);
        assertEquals(player.getInterpolatedX(1.0), torch.x(), 1e-12);
        assertEquals(player.getInterpolatedY(1.0), torch.y(), 1e-12);

        torch.sync(registry, player, 0.5);
        assertEquals(player.getInterpolatedX(0.5), torch.x(), 1e-12);
        assertEquals(player.getInterpolatedY(0.5), torch.y(), 1e-12);
    }

    @Test
    void aTorchRefusesValuesThatCannotLight() {
        final Rgba colour = new Rgba(1, 1, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> new HeldTorch(0.0, colour, 1.0, Flicker.STEADY));
        assertThrows(IllegalArgumentException.class, () -> new HeldTorch(3.0, colour, -0.1, Flicker.STEADY));
        assertThrows(IllegalArgumentException.class, () -> new HeldTorch(3.0, null, 1.0, Flicker.STEADY));
        assertThrows(IllegalArgumentException.class, () -> new HeldTorch(3.0, colour, 1.0, null));
    }
}
