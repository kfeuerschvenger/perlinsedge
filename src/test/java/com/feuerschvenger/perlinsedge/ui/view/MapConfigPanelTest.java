package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.PerlinMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapConfigPanelTest {

    private static final long SEED = 4242L;

    private final List<MapGenerationContext> reported = new ArrayList<>();
    private MapConfigPanel panel;

    @BeforeEach
    void setUp() {
        panel = FxToolkitSupport.onFx(() -> {
            final MapConfigPanel created = new MapConfigPanel(MapType.DESERT, SEED);
            created.setMapConfigListener(reported::add);
            return created;
        });
    }

    private <T> T onFx(Callable<T> task) {
        return FxToolkitSupport.onFx(task);
    }

    private void run(Runnable action) {
        onFx(() -> {
            action.run();
            return null;
        });
    }

    private Slider slider(String id) {
        return (Slider) panel.lookup("#" + id);
    }

    private ComboBox<MapType> combo() {
        return comboOf(panel.lookup(".combo-box"));
    }

    private static <T> ComboBox<T> comboOf(Node node) {
        return (ComboBox<T>) node;
    }

    private TextField seedField() {
        return (TextField) panel.lookup(".text-field");
    }

    private void release(Slider slider) {
        run(() -> Event.fireEvent(slider, new MouseEvent(MouseEvent.MOUSE_RELEASED, 1, 1, 1, 1, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, true, false, false, null)));
    }

    @Test
    void thePanelStartsOnTheGivenMapAndSeed() {
        assertEquals(MapType.DESERT, combo().getValue());
        assertEquals(String.valueOf(SEED), seedField().getText());
        assertTrue(panel.getStyleClass().contains(UiTheme.PANEL));
    }

    @Test
    void releasingASliderReportsEveryParameterOfTheMap() {
        final Slider height = slider("heightFreq");
        run(() -> height.setValue(0.05));
        run(() -> slider("heightOctaves").setValue(7));
        run(() -> slider("treeDensity").setValue(0.5));
        run(() -> slider("riverDensity").setValue(0.25));

        release(height);

        final MapGenerationContext context = reported.getLast();
        assertEquals(MapType.DESERT, context.getMapType());
        assertEquals(SEED, context.getSeed());
        assertEquals(0.05f, context.getHeightNoiseFrequency(), 1e-6);
        assertEquals(7, context.getHeightNoiseOctaves());
        assertEquals(0.5f, context.getTreeDensity(), 1e-6);
        assertEquals(0.25f, context.getRiverDensity(), 1e-6);
    }

    @Test
    void everySliderReportsWhenItIsReleased() {
        final String[] ids = {"heightFreq", "heightOctaves", "tempFreq", "tempOctaves", "moistureFreq",
                "moistureOctaves", "stoneFreq", "stoneThreshold", "crystalsFreq", "crystalsThreshold", "treeDensity",
                "riverDensity"};
        for (String id : ids) {
            final int before = reported.size();

            release(slider(id));

            assertEquals(before + 1, reported.size(), id + " did not report");
        }
    }

    @Test
    void movingASliderShowsItsValueInTheLabelBesideIt() {
        final Slider octaves = slider("moistureOctaves");

        run(() -> octaves.setValue(6));

        final Label value = onFx(() -> (Label) ((javafx.scene.layout.HBox) octaves.getParent()).getChildren().get(2));
        assertEquals("6", value.getText());
    }

    @Test
    void choosingAnotherMapTypeReportsTheNewType() {
        run(() -> {
            combo().setValue(MapType.TUNDRA);
            combo().fireEvent(new ActionEvent());
        });

        assertEquals(MapType.TUNDRA, reported.getLast().getMapType());
    }

    @Test
    void committingANewSeedReportsItAndTheSameSeedIsNotReported() {
        run(() -> {
            seedField().setText("77");
            seedField().fireEvent(new ActionEvent());
        });
        assertEquals(1, reported.size());
        assertEquals(77L, reported.getFirst().getSeed());

        run(() -> seedField().fireEvent(new ActionEvent()));

        assertEquals(1, reported.size(), "the seed did not change");
    }

    @Test
    void anInvalidSeedIsRestoredToThePreviousOne() {
        run(() -> {
            seedField().setText("not a number");
            seedField().fireEvent(new ActionEvent());
        });

        assertEquals(String.valueOf(SEED), seedField().getText());
        assertTrue(reported.isEmpty());
    }

    @Test
    void generatingARandomMapPicksAndReportsANewSeed() {
        run(panel::generateNewRandomMap);

        assertEquals(1, reported.size());
        assertEquals(String.valueOf(reported.getFirst().getSeed()), seedField().getText());
    }

    @Test
    void nothingIsReportedWithoutAListener() {
        final MapConfigPanel quiet = onFx(() -> new MapConfigPanel(MapType.SWAMP, 1L));

        run(quiet::generateNewRandomMap);

        assertTrue(reported.isEmpty());
    }

    @Test
    void theControlsFollowTheValuesOfAGenerator() {
        final PerlinMapGenerator generator = new PerlinMapGenerator(FastNoiseLiteAdapter.FACTORY);
        generator.generateMap(8, 8, new MapGenerationContext(MapType.ISLANDS, 99L, 0.04f, 4, 0.05f, 2, 0.06f, 5,
                0.07f, 1.2f, 0.08f, 1.4f, 0.6f, 0.1f));

        run(() -> panel.updateValuesFromGenerator(generator));

        assertEquals(MapType.ISLANDS, combo().getValue());
        assertEquals("99", seedField().getText());
        assertEquals(0.04, slider("heightFreq").getValue(), 1e-6);
        assertEquals(4, slider("heightOctaves").getValue(), 1e-6);
        assertEquals(0.05, slider("tempFreq").getValue(), 1e-6);
        assertEquals(5, slider("moistureOctaves").getValue(), 1e-6);
        assertEquals(1.2, slider("stoneThreshold").getValue(), 1e-6);
        assertEquals(0.6, slider("treeDensity").getValue(), 1e-6);
    }

    @Test
    void aMissingGeneratorLeavesTheControlsAlone() {
        run(() -> panel.updateValuesFromGenerator(null));

        assertEquals(MapType.DESERT, combo().getValue());
        assertEquals(String.valueOf(SEED), seedField().getText());
    }

    @Test
    void theMapTypeIsShownByItsDisplayNameAndParsedBack() {
        final StringConverter<MapType> converter = combo().getConverter();

        assertEquals(MapType.TUNDRA.getDisplayName(), converter.toString(MapType.TUNDRA));
        assertEquals("", converter.toString(null));
        assertEquals(MapType.DENSE_FOREST, converter.fromString("dense forest"));
        assertNull(converter.fromString("nowhere"));
    }

    @Test
    void theCellsOfTheListAndTheButtonShowTheDisplayName() {
        final ComboBox<MapType> combo = combo();
        final ListCell<MapType> row = onFx(() -> combo.getCellFactory().call(null));
        final ListCell<MapType> button = onFx(combo::getButtonCell);

        for (ListCell<MapType> cell : List.of(row, button)) {
            run(() -> {
                cell.updateListView(new ListView<>(FXCollections.observableArrayList(MapType.SWAMP)));
                cell.updateIndex(0);
            });
            assertEquals(MapType.SWAMP.getDisplayName(), cell.getText());
            run(() -> cell.updateIndex(-1));
            assertNull(cell.getText());
        }
    }
}
