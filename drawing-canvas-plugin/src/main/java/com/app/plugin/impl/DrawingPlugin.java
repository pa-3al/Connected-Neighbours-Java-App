package com.app.plugin.impl;

import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class DrawingPlugin implements Plugin {

    private static final String PANEL_TITLE = "Dessin";

    @Override
    public String getId() {
        return "drawing-canvas";
    }

    @Override
    public String getName() {
        return "Drawing Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Ajoute une zone de dessin interactive dans la sidebar.";
    }

    @Override
    public String getAuthor() {
        return "System";
    }

    @Override
    public void onLoad(PluginContext context) {
        if (context.isHeadless()) {
            return;
        }

        try {
            Pane panel = createDrawingPanel();
            context.addPanel(PANEL_TITLE, panel);
        } catch (Exception e) {
            context.logError("Failed to load Drawing Canvas panel", e);
        }
    }

    @Override
    public void onUnload(PluginContext context) {
        if (!context.isHeadless()) {
            context.removePanel(PANEL_TITLE);
        }
    }

    private Pane createDrawingPanel() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("drawing-canvas-root");

        Canvas canvas = new Canvas(300, 300);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        initCanvas(gc, canvas.getWidth(), canvas.getHeight());

        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.setStyle(
            "-fx-border-color: #888; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-color: white; -fx-background-radius: 4;"
        );
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        canvasHolder.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue() - 2;
            if (w > 0) {
                canvas.setWidth(w);
            }
        });
        canvasHolder.heightProperty().addListener((obs, oldH, newH) -> {
            double h = newH.doubleValue() - 2;
            if (h > 0) {
                canvas.setHeight(h);
            }
        });

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setTooltip(new Tooltip("Couleur du trait"));
        colorPicker.setPrefWidth(45);
        colorPicker.setMaxWidth(45);

        Slider sizeSlider = new Slider(1, 30, 3);
        sizeSlider.setShowTickLabels(false);
        sizeSlider.setShowTickMarks(false);
        sizeSlider.setPrefWidth(90);
        sizeSlider.setMaxWidth(120);
        sizeSlider.setTooltip(new Tooltip("Épaisseur du trait"));
        sizeSlider.getStyleClass().add("slider");
        sizeSlider.getStyleClass().add("tool-slider");

        Label sizeLabel = new Label("3 px");
        sizeLabel.setMinWidth(40);
        sizeSlider.valueProperty().addListener((obs, oldV, newV) ->
            sizeLabel.setText(String.format("%.0f px", newV.doubleValue()))
        );

        Button clearBtn = new Button("Effacer");
        clearBtn.setTooltip(new Tooltip("Effacer le dessin"));
        clearBtn.setOnAction(e -> initCanvas(gc, canvas.getWidth(), canvas.getHeight()));

        HBox toolbar = new HBox(8, colorPicker, sizeSlider, sizeLabel, clearBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 0, 4, 0));

        canvas.setOnMousePressed(e -> {
            gc.setStroke(colorPicker.getValue());
            gc.setLineWidth(sizeSlider.getValue());
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });

        canvas.setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });

        canvas.setOnMouseReleased(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            gc.closePath();
        });

        root.getChildren().addAll(toolbar, canvasHolder);
        return root;
    }

    private void initCanvas(GraphicsContext gc, double width, double height) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
    }
}
