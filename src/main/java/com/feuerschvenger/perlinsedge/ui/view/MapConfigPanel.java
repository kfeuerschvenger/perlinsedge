package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.events.IConfigChangeListener;
import com.feuerschvenger.perlinsedge.domain.world.generation.IMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Random;

@Slf4j
public class MapConfigPanel extends VBox {
    private static final double PANEL_SPACING = 8;
    /** Distance of the panel from the top left corner of the screen. */
    private static final double PANEL_OFFSET = 10;
    private static final double TOP_ROW_SPACING = 8;
    private static final double SLIDER_WIDTH = 200;
    private static final double SLIDER_ROW_SPACING = 10;

    /**
     * The range and the tick layout of a slider, and the value it starts at.
     *
     * @param min            lowest value
     * @param max            highest value
     * @param initial        starting value
     * @param blockIncrement step of a click on the track
     * @param majorTick      distance between the labelled ticks
     * @param minorTickCount minor ticks between two labelled ticks
     */
    private record SliderSpec(double min, double max, double initial, double blockIncrement, double majorTick,
                              int minorTickCount) {
        SliderSpec withInitial(double value) {
            return new SliderSpec(min, max, value, blockIncrement, majorTick, minorTickCount);
        }
    }

    private static final SliderSpec HEIGHT_FREQ_SPEC = new SliderSpec(0.001, 0.1, 0.01, 0.001, 0.01, 10);
    private static final SliderSpec HEIGHT_OCTAVES_SPEC = new SliderSpec(1, 10, 5, 1, 1, 0);
    private static final SliderSpec TEMP_FREQ_SPEC = new SliderSpec(0.001, 0.1, 0.02, 0.001, 0.01, 10);
    private static final SliderSpec TEMP_OCTAVES_SPEC = new SliderSpec(1, 10, 3, 1, 1, 0);
    private static final SliderSpec MOISTURE_FREQ_SPEC = new SliderSpec(0.001, 0.1, 0.03, 0.001, 0.01, 10);
    private static final SliderSpec MOISTURE_OCTAVES_SPEC = new SliderSpec(1, 10, 3, 1, 1, 0);
    private static final SliderSpec STONE_FREQ_SPEC = new SliderSpec(0.0, 0.1, 0.1, 0.01, 0.05, 5);
    private static final SliderSpec STONE_THRESHOLD_SPEC = new SliderSpec(0.8, 3.0, 0.99, 0.01, 0.1, 10);
    private static final SliderSpec CRYSTALS_FREQ_SPEC = new SliderSpec(0.0, 0.1, 0.095, 0.01, 0.05, 5);
    private static final SliderSpec CRYSTALS_THRESHOLD_SPEC = new SliderSpec(0.8, 3.0, 0.916, 0.01, 0.1, 10);
    /** The density sliders start at the value of the world configuration, so their initial value is replaced. */
    private static final SliderSpec TREE_DENSITY_SPEC = new SliderSpec(0.0F, 1.0F, 0.0, 0.01F, 0.1F, 5);
    private static final SliderSpec RIVER_DENSITY_SPEC = new SliderSpec(0.0F, 0.5F, 0.0, 0.001F, 0.01F, 10);

    private ComboBox<MapType> mapTypeComboBox;
    private TextField seedInputField;
    private long currentSeed;
    /** Draws the seed of every randomly generated map. */
    private final Random seedSource = new Random();

    private Slider heightFreqSlider;
    private Label heightFreqValueLabel;
    private Slider heightOctavesSlider;
    private Label heightOctavesValueLabel;

    private Slider tempFreqSlider;
    private Label tempFreqValueLabel;
    private Slider tempOctavesSlider;
    private Label tempOctavesValueLabel;

    private Slider moistureFreqSlider;
    private Label moistureFreqValueLabel;
    private Slider moistureOctavesSlider;
    private Label moistureOctavesValueLabel;

    private Slider stoneFreqSlider;
    private Label stoneFreqValueLabel;
    private Slider stoneThresholdSlider;
    private Label stoneThresholdValueLabel;

    private Slider crystalsFreqSlider;
    private Label crystalsFreqValueLabel;
    private Slider crystalsThresholdSlider;
    private Label crystalsThresholdValueLabel;

    private Slider treeDensitySlider;
    private Label treeDensityValueLabel;

    private Slider riverDensitySlider;
    private Label riverDensityValueLabel;

    private final DecimalFormat decimalFormat = new DecimalFormat("#.###");
    private IConfigChangeListener listener;

    private final StringConverter<MapType> mapTypeConverter = createMapTypeConverter();

    public MapConfigPanel(MapType initialMapType, long initialSeed) {
        super(PANEL_SPACING);
        this.currentSeed = initialSeed;
        initUI(initialMapType);
        setupListeners();
        updateSeedDisplay(initialSeed);
    }

    public void setMapConfigListener(IConfigChangeListener listener) {
        this.listener = listener;
    }

    private void initUI(MapType initialMapType) {
        getStyleClass().add(UiTheme.PANEL);
        setLayoutX(PANEL_OFFSET);
        setLayoutY(PANEL_OFFSET);

        mapTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(MapType.values()));
        mapTypeComboBox.setConverter(mapTypeConverter);

        mapTypeComboBox.setButtonCell(new ListCell<MapType>() {
            @Override
            protected void updateItem(MapType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : mapTypeConverter.toString(item));
            }
        });

        mapTypeComboBox.setCellFactory(lv -> new ListCell<MapType>() {
            @Override
            protected void updateItem(MapType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : mapTypeConverter.toString(item));
            }
        });

        mapTypeComboBox.setValue(initialMapType);

        seedInputField = new TextField(String.valueOf(currentSeed));

        var topRow = new HBox(TOP_ROW_SPACING);
        topRow.getChildren().addAll(mapTypeComboBox, seedInputField);

        HBox.setHgrow(mapTypeComboBox, Priority.ALWAYS);
        HBox.setHgrow(seedInputField, Priority.ALWAYS);

        this.getChildren().add(topRow);

        heightFreqSlider = createSlider(HEIGHT_FREQ_SPEC, "heightFreq");
        heightFreqValueLabel = createValueLabel(heightFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Height Freq:", heightFreqSlider, heightFreqValueLabel));

        heightOctavesSlider = createSlider(HEIGHT_OCTAVES_SPEC, "heightOctaves");
        heightOctavesSlider.setSnapToTicks(true);
        heightOctavesValueLabel = createValueLabel(heightOctavesSlider.getValue());
        this.getChildren().add(createSliderRow("Height Octaves:", heightOctavesSlider, heightOctavesValueLabel));

        tempFreqSlider = createSlider(TEMP_FREQ_SPEC, "tempFreq");
        tempFreqValueLabel = createValueLabel(tempFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Temp Freq:", tempFreqSlider, tempFreqValueLabel));

        tempOctavesSlider = createSlider(TEMP_OCTAVES_SPEC, "tempOctaves");
        tempOctavesSlider.setSnapToTicks(true);
        tempOctavesValueLabel = createValueLabel(tempOctavesSlider.getValue());
        this.getChildren().add(createSliderRow("Temp Octaves:", tempOctavesSlider, tempOctavesValueLabel));

        moistureFreqSlider = createSlider(MOISTURE_FREQ_SPEC, "moistureFreq");
        moistureFreqValueLabel = createValueLabel(moistureFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Moisture Freq:", moistureFreqSlider, moistureFreqValueLabel));

        moistureOctavesSlider = createSlider(MOISTURE_OCTAVES_SPEC, "moistureOctaves");
        moistureOctavesSlider.setSnapToTicks(true);
        moistureOctavesValueLabel = createValueLabel(moistureOctavesSlider.getValue());
        this.getChildren().add(createSliderRow("Moisture Octaves:", moistureOctavesSlider, moistureOctavesValueLabel));

        stoneFreqSlider = createSlider(STONE_FREQ_SPEC, "stoneFreq");
        stoneFreqValueLabel = createValueLabel(stoneFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Stone Freq:", stoneFreqSlider, stoneFreqValueLabel));

        stoneThresholdSlider = createSlider(STONE_THRESHOLD_SPEC, "stoneThreshold");
        stoneThresholdValueLabel = createValueLabel(stoneThresholdSlider.getValue());
        this.getChildren().add(createSliderRow("Stone Threshold:", stoneThresholdSlider, stoneThresholdValueLabel));

        crystalsFreqSlider = createSlider(CRYSTALS_FREQ_SPEC, "crystalsFreq");
        crystalsFreqValueLabel = createValueLabel(crystalsFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Crystals Freq:", crystalsFreqSlider, crystalsFreqValueLabel));

        crystalsThresholdSlider = createSlider(CRYSTALS_THRESHOLD_SPEC, "crystalsThreshold");
        crystalsThresholdValueLabel = createValueLabel(crystalsThresholdSlider.getValue());
        this.getChildren().add(createSliderRow("Crystals Threshold:", crystalsThresholdSlider, crystalsThresholdValueLabel));

        float baseTreeDensity = AppConfig.getInstance().world().getTreeDensity();
        treeDensitySlider = createSlider(TREE_DENSITY_SPEC.withInitial(baseTreeDensity), "treeDensity");
        treeDensityValueLabel = createValueLabel(treeDensitySlider.getValue());
        this.getChildren().add(createSliderRow("Tree Density:", treeDensitySlider, treeDensityValueLabel));

        float baseRiverDensity = AppConfig.getInstance().world().getRiverDensity();
        riverDensitySlider = createSlider(RIVER_DENSITY_SPEC.withInitial(baseRiverDensity), "riverDensity");
        riverDensityValueLabel = createValueLabel(riverDensitySlider.getValue());
        this.getChildren().add(createSliderRow("River Density:", riverDensitySlider, riverDensityValueLabel));
    }

    private void setupListeners() {
        mapTypeComboBox.setOnAction(event -> notifyConfigChanged());

        seedInputField.focusedProperty().subscribe((Boolean oldValue, Boolean focused) -> {
            if (Boolean.FALSE.equals(focused)) {
                commitSeedInput();
            }
        });
        seedInputField.setOnAction(event -> commitSeedInput());

        heightFreqSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(heightFreqValueLabel, newVal.doubleValue()));
        heightFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        heightOctavesSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(heightOctavesValueLabel, newVal.doubleValue()));
        heightOctavesSlider.setOnMouseReleased(event -> notifyConfigChanged());

        tempFreqSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(tempFreqValueLabel, newVal.doubleValue()));
        tempFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        tempOctavesSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(tempOctavesValueLabel, newVal.doubleValue()));
        tempOctavesSlider.setOnMouseReleased(event -> notifyConfigChanged());

        moistureFreqSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(moistureFreqValueLabel, newVal.doubleValue()));
        moistureFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        moistureOctavesSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(moistureOctavesValueLabel, newVal.doubleValue()));
        moistureOctavesSlider.setOnMouseReleased(event -> notifyConfigChanged());

        stoneFreqSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(stoneFreqValueLabel, newVal.doubleValue()));
        stoneFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        stoneThresholdSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(stoneThresholdValueLabel, newVal.doubleValue()));
        stoneThresholdSlider.setOnMouseReleased(event -> notifyConfigChanged());

        crystalsFreqSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(crystalsFreqValueLabel, newVal.doubleValue()));
        crystalsFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        crystalsThresholdSlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(crystalsThresholdValueLabel, newVal.doubleValue()));
        crystalsThresholdSlider.setOnMouseReleased(event -> notifyConfigChanged());

        treeDensitySlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(treeDensityValueLabel, newVal.doubleValue()));
        treeDensitySlider.setOnMouseReleased(event -> notifyConfigChanged());
        riverDensitySlider.valueProperty().addListener(
                (ObservableValue<? extends Number> obs, Number oldVal, Number newVal) ->
                        updateValueLabel(riverDensityValueLabel, newVal.doubleValue()));
        riverDensitySlider.setOnMouseReleased(event -> notifyConfigChanged());
    }

    /**
     * Updates the text of a given value label with a formatted double value.
     * @param label The Label to update.
     * @param value The new value to display.
     */
    private void updateValueLabel(Label label, double value) {
        if (label != null) {
            label.setText(decimalFormat.format(value));
        }
    }

    /** Applies the seed typed in the field, or puts the previous seed back when the text is not a number. */
    private void commitSeedInput() {
        try {
            var newSeed = Long.parseLong(seedInputField.getText());
            updateSeedAndNotify(newSeed, false);
        } catch (NumberFormatException _) {
            log.warn("Invalid seed input: '{}'. Restoring previous seed.", seedInputField.getText());
            seedInputField.setText(String.valueOf(currentSeed));
        }
    }

    private void updateSeedAndNotify(long newSeed, boolean forceNotify) {
        if (newSeed != currentSeed || forceNotify) {
            currentSeed = newSeed;
            seedInputField.setText(String.valueOf(currentSeed));
            notifyConfigChanged();
        }
    }

    public void generateNewRandomMap() {
        var newSeed = seedSource.nextLong();
        updateSeedAndNotify(newSeed, true);
    }

    private void notifyConfigChanged() {
        if (listener != null) {
            listener.onMapConfigChanged(new MapGenerationContext(
                    mapTypeComboBox.getValue(),
                    currentSeed,
                    (float) heightFreqSlider.getValue(),
                    (int) heightOctavesSlider.getValue(),
                    (float) tempFreqSlider.getValue(),
                    (int) tempOctavesSlider.getValue(),
                    (float) moistureFreqSlider.getValue(),
                    (int) moistureOctavesSlider.getValue(),
                    (float) stoneFreqSlider.getValue(),
                    (float) stoneThresholdSlider.getValue(),
                    (float) crystalsFreqSlider.getValue(),
                    (float) crystalsThresholdSlider.getValue(),
                    (float) treeDensitySlider.getValue(),
                    (float) riverDensitySlider.getValue()
            ));
        }
    }

    public void updateValuesFromGenerator(IMapGenerator generator) {
        if (generator == null) {
            return;
        }
        mapTypeComboBox.setValue(generator.getMapType());
        updateSeedDisplay(generator.getSeed());

        heightFreqSlider.setValue(generator.getHeightNoiseFrequency());
        heightOctavesSlider.setValue(generator.getHeightNoiseOctaves());
        tempFreqSlider.setValue(generator.getTemperatureNoiseFrequency());
        tempOctavesSlider.setValue(generator.getTemperatureNoiseOctaves());
        moistureFreqSlider.setValue(generator.getMoistureNoiseFrequency());
        moistureOctavesSlider.setValue(generator.getMoistureNoiseOctaves());
        stoneFreqSlider.setValue(generator.getStonePatchFrequency());
        stoneThresholdSlider.setValue(generator.getStoneThreshold());
        crystalsFreqSlider.setValue(generator.getCrystalsPatchFrequency());
        crystalsThresholdSlider.setValue(generator.getCrystalsThreshold());
        treeDensitySlider.setValue(generator.getTreeDensity());
        riverDensitySlider.setValue(generator.getRiverDensity());
    }

    /**
     * Updates the seed display in the UI without triggering a regeneration.
     * This is useful when loading a new map or initializing the panel.
     * @param newSeed The new seed to display.
     */
    private void updateSeedDisplay(long newSeed) {
        this.currentSeed = newSeed;
        seedInputField.setText(String.valueOf(newSeed));
    }

    private static Slider createSlider(SliderSpec spec, String id) {
        var slider = new Slider(spec.min(), spec.max(), spec.initial());
        slider.setPrefWidth(SLIDER_WIDTH);
        slider.setBlockIncrement(spec.blockIncrement());
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(spec.majorTick());
        slider.setMinorTickCount(spec.minorTickCount());
        slider.setOrientation(Orientation.HORIZONTAL);
        slider.setId(id);
        return slider;
    }

    private Label createValueLabel(double initialValue) {
        return new Label(decimalFormat.format(initialValue));
    }

    private static HBox createSliderRow(String labelText, Slider slider, Label valueLabel) {
        var label = new Label(UiTheme.caps(labelText));
        label.getStyleClass().add(UiTheme.MUTED);
        return new HBox(SLIDER_ROW_SPACING, label, slider, valueLabel);
    }

    private static StringConverter<MapType> createMapTypeConverter() {
        return new StringConverter<MapType>() {
            @Override
            public String toString(MapType mapType) {
                if (mapType == null) {
                    return "";
                }
                return mapType.getDisplayName();
            }

            @Override
            public MapType fromString(String string) {
                try {
                    return MapType.valueOf(string.toUpperCase(Locale.ROOT).replace(' ', '_'));
                } catch (IllegalArgumentException _) {
                    return null;
                }
            }
        };
    }

}
