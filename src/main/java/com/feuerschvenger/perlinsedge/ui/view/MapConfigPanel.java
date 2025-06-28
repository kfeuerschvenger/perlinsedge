package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.events.IConfigChangeListener;
import com.feuerschvenger.perlinsedge.domain.world.generation.IMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import java.text.DecimalFormat;
import java.util.Random;

public class MapConfigPanel extends VBox {

    private ComboBox<MapType> mapTypeComboBox;
    private TextField seedInputField;
    private long currentSeed;

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
        super(8);
        this.currentSeed = initialSeed;
        initUI(initialMapType);
        setupListeners();
        updateSeedDisplay(initialSeed);
    }

    public void setMapConfigListener(IConfigChangeListener listener) {
        this.listener = listener;
    }

    private void initUI(MapType initialMapType) {
        setPadding(new Insets(10, 10, 10, 10));
        Color bkg = Color.rgb(0, 0, 0, 0.6);
        setBackground(new Background(new BackgroundFill(bkg, new javafx.scene.layout.CornerRadii(5), Insets.EMPTY)));
        setLayoutX(10);
        setLayoutY(10);

        mapTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(MapType.values()));
        mapTypeComboBox.setConverter(mapTypeConverter);

        mapTypeComboBox.setButtonCell(new ListCell<MapType>() {
            @Override
            protected void updateItem(MapType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(mapTypeConverter.toString(item));
                    setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-background-color: #333;");
                }
            }
        });

        mapTypeComboBox.setCellFactory(lv -> new ListCell<MapType>() {
            @Override
            protected void updateItem(MapType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(mapTypeConverter.toString(item));
                    setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-background-color: #333;");
                    setOnMouseEntered(e -> setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-background-color: #555;"));
                    setOnMouseExited(e -> setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-background-color: #333;"));
                }
            }
        });

        mapTypeComboBox.setStyle("-fx-background-color: #333;");

        mapTypeComboBox.setValue(initialMapType);

        seedInputField = new TextField(String.valueOf(currentSeed));
        seedInputField.setStyle("-fx-font-size: 14px; -fx-background-color: #333; -fx-text-fill: white;");

        HBox topRow = new HBox(8);
        topRow.getChildren().addAll(mapTypeComboBox, seedInputField);

        HBox.setHgrow(mapTypeComboBox, Priority.ALWAYS);
        HBox.setHgrow(seedInputField, Priority.ALWAYS);

        this.getChildren().add(topRow);

        heightFreqSlider = createSlider(0.001, 0.1, 0.01, 0.001, 0.01, 10, "heightFreq");
        heightFreqValueLabel = createValueLabel(heightFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Height Freq:", heightFreqSlider, heightFreqValueLabel));

        heightOctavesSlider = createSlider(1, 10, 5, 1, 1, 0, "heightOctaves");
        heightOctavesSlider.setSnapToTicks(true);
        heightOctavesValueLabel = createValueLabel(heightOctavesSlider.getValue());
        this.getChildren().add(createSliderRow("Height Octaves:", heightOctavesSlider, heightOctavesValueLabel));

        tempFreqSlider = createSlider(0.001, 0.1, 0.02, 0.001, 0.01, 10, "tempFreq");
        tempFreqValueLabel = createValueLabel(tempFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Temp Freq:", tempFreqSlider, tempFreqValueLabel));

        tempOctavesSlider = createSlider(1, 10, 3, 1, 1, 0, "tempOctaves");
        tempOctavesSlider.setSnapToTicks(true);
        tempOctavesValueLabel = createValueLabel(tempOctavesSlider.getValue());
        this.getChildren().add(createSliderRow("Temp Octaves:", tempOctavesSlider, tempOctavesValueLabel));

        moistureFreqSlider = createSlider(0.001, 0.1, 0.03, 0.001, 0.01, 10, "moistureFreq");
        moistureFreqValueLabel = createValueLabel(moistureFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Moisture Freq:", moistureFreqSlider, moistureFreqValueLabel));

        moistureOctavesSlider = createSlider(1, 10, 3, 1, 1, 0, "moistureOctaves");
        moistureOctavesSlider.setSnapToTicks(true);
        moistureOctavesValueLabel = createValueLabel(moistureOctavesSlider.getValue());
        this.getChildren().add(createSliderRow("Moisture Octaves:", moistureOctavesSlider, moistureOctavesValueLabel));

        stoneFreqSlider = createSlider(0.0, 0.1, 0.1, 0.01, 0.05, 5, "stoneFreq");
        stoneFreqValueLabel = createValueLabel(stoneFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Stone Freq:", stoneFreqSlider, stoneFreqValueLabel));

        stoneThresholdSlider = createSlider(0.8, 3.0, 0.99, 0.01, 0.1, 10, "stoneThreshold");
        stoneThresholdValueLabel = createValueLabel(stoneThresholdSlider.getValue());
        this.getChildren().add(createSliderRow("Stone Threshold:", stoneThresholdSlider, stoneThresholdValueLabel));

        crystalsFreqSlider = createSlider(0.0, 0.1, 0.095, 0.01, 0.05, 5, "crystalsFreq");
        crystalsFreqValueLabel = createValueLabel(crystalsFreqSlider.getValue());
        this.getChildren().add(createSliderRow("Crystals Freq:", crystalsFreqSlider, crystalsFreqValueLabel));

        crystalsThresholdSlider = createSlider(0.8, 3.0, 0.916, 0.01, 0.1, 10, "crystalsThreshold");
        crystalsThresholdValueLabel = createValueLabel(crystalsThresholdSlider.getValue());
        this.getChildren().add(createSliderRow("Crystals Threshold:", crystalsThresholdSlider, crystalsThresholdValueLabel));

        float baseTreeDensity = AppConfig.getInstance().world().getTreeDensity();
        treeDensitySlider = createSlider(0.0f, 1.0f, baseTreeDensity, 0.01f, 0.1f, 5, "treeDensity");
        treeDensityValueLabel = createValueLabel(treeDensitySlider.getValue());
        this.getChildren().add(createSliderRow("Tree Density:", treeDensitySlider, treeDensityValueLabel));

        float baseRiverDensity = AppConfig.getInstance().world().getRiverDensity();
        riverDensitySlider = createSlider(0.0f, 0.5f, baseRiverDensity, 0.001f, 0.01f, 10, "riverDensity");
        riverDensityValueLabel = createValueLabel(riverDensitySlider.getValue());
        this.getChildren().add(createSliderRow("River Density:", riverDensitySlider, riverDensityValueLabel));
    }

    private void setupListeners() {
        mapTypeComboBox.setOnAction(event -> notifyConfigChanged());

        seedInputField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    long newSeed = Long.parseLong(seedInputField.getText());
                    updateSeedAndNotify(newSeed, false);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid seed input: '" + seedInputField.getText() + "'. Restoring previous seed.");
                    seedInputField.setText(String.valueOf(currentSeed));
                }
            }
        });
        seedInputField.setOnAction(event -> {
            try {
                long newSeed = Long.parseLong(seedInputField.getText());
                updateSeedAndNotify(newSeed, false);
            } catch (NumberFormatException e) {
                System.err.println("Invalid seed input: '" + seedInputField.getText() + "'. Restoring previous seed.");
                seedInputField.setText(String.valueOf(currentSeed));
            }
        });

        heightFreqSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(heightFreqValueLabel, newVal.doubleValue()));
        heightFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        heightOctavesSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(heightOctavesValueLabel, newVal.doubleValue()));
        heightOctavesSlider.setOnMouseReleased(event -> notifyConfigChanged());

        tempFreqSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(tempFreqValueLabel, newVal.doubleValue()));
        tempFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        tempOctavesSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(tempOctavesValueLabel, newVal.doubleValue()));
        tempOctavesSlider.setOnMouseReleased(event -> notifyConfigChanged());

        moistureFreqSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(moistureFreqValueLabel, newVal.doubleValue()));
        moistureFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        moistureOctavesSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(moistureOctavesValueLabel, newVal.doubleValue()));
        moistureOctavesSlider.setOnMouseReleased(event -> notifyConfigChanged());

        stoneFreqSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(stoneFreqValueLabel, newVal.doubleValue()));
        stoneFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        stoneThresholdSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(stoneThresholdValueLabel, newVal.doubleValue()));
        stoneThresholdSlider.setOnMouseReleased(event -> notifyConfigChanged());

        crystalsFreqSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(crystalsFreqValueLabel, newVal.doubleValue()));
        crystalsFreqSlider.setOnMouseReleased(event -> notifyConfigChanged());
        crystalsThresholdSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(crystalsThresholdValueLabel, newVal.doubleValue()));
        crystalsThresholdSlider.setOnMouseReleased(event -> notifyConfigChanged());

        treeDensitySlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(treeDensityValueLabel, newVal.doubleValue()));
        treeDensitySlider.setOnMouseReleased(event -> notifyConfigChanged());
        riverDensitySlider.valueProperty().addListener((obs, oldVal, newVal) -> updateValueLabel(riverDensityValueLabel, newVal.doubleValue()));
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

    private void updateSeedAndNotify(long newSeed, boolean forceNotify) {
        if (newSeed != currentSeed || forceNotify) {
            currentSeed = newSeed;
            seedInputField.setText(String.valueOf(currentSeed));
            notifyConfigChanged();
        }
    }

    public void generateNewRandomMap() {
        Random random = new Random();
        long newSeed = random.nextLong();
        updateSeedAndNotify(newSeed, true);
    }

    private void notifyConfigChanged() {
        if (listener != null) {
            listener.onMapConfigChanged(
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
            );
        }
    }

    public void updateValuesFromGenerator(IMapGenerator generator) {
        if (generator == null) return;
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
    public void updateSeedDisplay(long newSeed) {
        this.currentSeed = newSeed;
        seedInputField.setText(String.valueOf(newSeed));
    }

    private Slider createSlider(double min, double max, double initial, double blockInc, double majorTick, int minorCount, String id) {
        Slider slider = new Slider(min, max, initial);
        slider.setPrefWidth(200);
        slider.setBlockIncrement(blockInc);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(majorTick);
        slider.setMinorTickCount(minorCount);
        slider.setOrientation(Orientation.HORIZONTAL);
        slider.setId(id);
        return slider;
    }

    private Label createValueLabel(double initialValue) {
        Label label = new Label(decimalFormat.format(initialValue));
        label.setFont(new Font("Arial", 14));
        label.setTextFill(Color.WHITE);
        return label;
    }

    private HBox createSliderRow(String labelText, Slider slider, Label valueLabel) {
        Label label = new Label(labelText);
        label.setFont(new Font("Arial", 14));
        label.setTextFill(Color.WHITE);
        return new HBox(10, label, slider, valueLabel);
    }

    private StringConverter<MapType> createMapTypeConverter() {
        return new StringConverter<MapType>() {
            @Override
            public String toString(MapType mapType) {
                if (mapType == null) return "";
                return mapType.name().charAt(0) + mapType.name().substring(1).toLowerCase().replace('_', ' ');
            }

            @Override
            public MapType fromString(String string) {
                try {
                    return MapType.valueOf(string.toUpperCase().replace(' ', '_'));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        };
    }

}