package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.infra.fx.utils.BuildingSprites;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingIconsTest {

    private static final int SCRATCH_SIZE = 160;

    private static Image image(BuildingType type, int rotation) {
        return FxToolkitSupport.onFx(() -> BuildingIcons.image(type, rotation));
    }

    @Test
    void everyBuildingTypeHasAnIcon() {
        for (BuildingType type : BuildingType.values()) {
            final Image icon = image(type, 0);

            assertNotNull(icon, type + " has no icon");
            assertTrue(icon.getWidth() > 0 && icon.getHeight() > 0, type + " icon is empty");
        }
    }

    @Test
    void anIconWithNoArtIsTheProceduralShapeCroppedToItsContent() {
        for (BuildingType type : BuildingType.values()) {
            if (BuildingSprites.getInstance().restingImage(type, 0) != null) {
                continue;
            }
            final Image icon = image(type, 0);

            assertTrue(icon.getWidth() < SCRATCH_SIZE && icon.getHeight() < SCRATCH_SIZE,
                    type + " was not cropped from the scratch canvas");
            assertSame(icon, image(type, 0), "a procedural icon is drawn once");
        }
    }

    @Test
    void anIconWithArtIsTheArtItself() {
        for (BuildingType type : BuildingType.values()) {
            final Image art = BuildingSprites.getInstance().restingImage(type, 0);
            if (art != null) {
                assertSame(art, image(type, 0), type + " icon is not its sprite");
            }
        }
    }

    @Test
    void aViewIsFittedToTheRequestedSizeKeepingTheRatio() {
        final ImageView view = FxToolkitSupport.onFx(() -> BuildingIcons.view(BuildingType.CHEST, 40));

        assertEquals(40, view.getFitWidth(), 1e-9);
        assertEquals(40, view.getFitHeight(), 1e-9);
        assertTrue(view.isPreserveRatio());
        assertNotNull(view.getImage());
    }

    @Test
    void aViewWithoutARotationShowsTheFirstRotation() {
        final ImageView plain = FxToolkitSupport.onFx(() -> BuildingIcons.view(BuildingType.BED, 32));
        final ImageView rotated = FxToolkitSupport.onFx(() -> BuildingIcons.view(BuildingType.BED, 0, 32));

        assertSame(plain.getImage(), rotated.getImage());
    }

    @Test
    void showingAnotherTypeReplacesTheImageAndSmoothsOnlyTheProceduralOnes() {
        final ImageView view = FxToolkitSupport.onFx(ImageView::new);

        for (BuildingType type : BuildingType.values()) {
            FxToolkitSupport.onFx(() -> {
                BuildingIcons.show(view, type, 0);
                return null;
            });

            assertSame(image(type, 0), view.getImage());
            final boolean sprite = BuildingSprites.getInstance().restingImage(type, 0) != null;
            assertEquals(!sprite, view.isSmooth(), type + " smoothing");
        }
    }
}
