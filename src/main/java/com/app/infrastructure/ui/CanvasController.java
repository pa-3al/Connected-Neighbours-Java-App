package com.app.infrastructure.ui;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.app.infrastructure.i18n.I18nService;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;

public class CanvasController {

    @FXML private StackPane viewport;
    @FXML private StackPane zoomGroup;
    @FXML private Canvas canvas;
    @FXML private Rectangle paperParams;
    
    @FXML private ColorPicker colorPicker;
    @FXML private Slider sizeSlider;

    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 20.0;
    
    private double lastMouseX, lastMouseY;
    private boolean isSpaceDown = false;
    private boolean isEraser = false;

    private final Affine transform = new Affine();

    @FXML
    public void initialize() {

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);

        zoomGroup.getTransforms().add(transform);

        colorPicker.setValue(Color.BLACK);

        setupGestures();
        clearCanvas(); 
    }

    @FXML
    private void onImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nService.getInstance().get("action.file.import.title"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        File file = fileChooser.showOpenDialog(viewport.getScene().getWindow());
        if (file != null) {
            try {
                Image image = new Image(file.toURI().toString());
                resizeCanvas(image.getWidth(), image.getHeight());
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.drawImage(image, 0, 0);
            } catch (Exception e) {
                showError(I18nService.getInstance().get("action.file.import.error"), 
                          I18nService.getInstance().get("action.file.load.error.msg", e.getMessage()));
            }
        }
    }

    @FXML
    private void onExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nService.getInstance().get("action.file.export.title"));
        fileChooser.setInitialFileName("dessin.png");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image", "*.png")
        );

        File file = fileChooser.showSaveDialog(viewport.getScene().getWindow());
        if (file != null) {
            try {
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT); 
                params.setTransform(Transform.scale(1, 1)); 

                WritableImage snapshot = canvas.snapshot(params, null);
                java.awt.image.BufferedImage image = SwingFXUtils.fromFXImage(snapshot, null);
                
                java.awt.image.BufferedImage cropped = autoCrop(image);

                ImageIO.write(cropped, "png", file);

            } catch (IOException e) {
                showError(I18nService.getInstance().get("action.file.export.error"), 
                          I18nService.getInstance().get("action.file.save.error.msg", e.getMessage()));
            }
        }
    }

    private java.awt.image.BufferedImage autoCrop(java.awt.image.BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getRGB(x, y);

                if ((pixel >> 24) != 0x00) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX == -1) {

             return source;
        }

        return source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    @FXML
    private void onClear() {
        clearCanvas();
    }
    
    private void clearCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void resizeCanvas(double width, double height) {
        canvas.setWidth(width);
        canvas.setHeight(height);
        paperParams.setWidth(width);
        paperParams.setHeight(height);
        onResetZoom();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void onDrawMode() {
        isEraser = false;
    }

    @FXML
    private void onEraseMode() {
        isEraser = true;
    }

    private void setupGestures() {
        viewport.setOnScroll(this::handleZoom);
        viewport.setOnMousePressed(this::handleMousePressed);
        viewport.setOnMouseDragged(this::handleMouseDragged);
        viewport.setOnMouseReleased(this::handleMouseReleased);
        viewport.setOnKeyPressed(this::handleKeyPressed);
        viewport.setOnKeyReleased(this::handleKeyReleased);
    }

    private void handleZoom(ScrollEvent e) {
        if (e.isControlDown()) {
            double delta = e.getDeltaY();
            double scaleFactor = (delta > 0) ? 1.1 : 0.9;
             double newScale = transform.getMxx() * scaleFactor;
             if (newScale > MAX_SCALE || newScale < MIN_SCALE) return;
             
             Point2D pivot = zoomGroup.parentToLocal(e.getX(), e.getY());
             
             transform.appendScale(scaleFactor, scaleFactor, pivot.getX(), pivot.getY());
             e.consume();
        }
    }
    
    private void handleMousePressed(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        Point2D p = zoomGroup.parentToLocal(e.getX(), e.getY());
        
        if (e.getButton() == MouseButton.PRIMARY && !isSpaceDown) {
            handleDrawStart(p);
        }
    }

    private void handleMouseDragged(MouseEvent e) {
         if (isSpaceDown || e.getButton() == MouseButton.MIDDLE) {

             double dx = e.getX() - lastMouseX;
             double dy = e.getY() - lastMouseY;
             transform.appendTranslation(dx, dy);
             lastMouseX = e.getX();
             lastMouseY = e.getY();
         } else if (e.getButton() == MouseButton.PRIMARY) {

             Point2D p = zoomGroup.parentToLocal(e.getX(), e.getY());
             handleDrawMove(p);
         }
    }

    private void handleMouseReleased(MouseEvent e) {

        if (e.getButton() == MouseButton.PRIMARY && !isSpaceDown) {
             GraphicsContext gc = canvas.getGraphicsContext2D();
             gc.closePath();
        }
    }

    private void handleKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.SPACE) {
            isSpaceDown = true;
            viewport.setCursor(javafx.scene.Cursor.MOVE);
        }
    }
    
    private void handleKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.SPACE) {
            isSpaceDown = false;
            viewport.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private void onResetZoom() {
        transform.setToIdentity();
    }
    
    private void handleDrawStart(Point2D worldPoint) {

        Point2D canvasLocal = canvas.parentToLocal(worldPoint.getX(), worldPoint.getY());
        double x = canvasLocal.getX();
        double y = canvasLocal.getY();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        if (isEraser) {
             gc.setStroke(Color.TRANSPARENT);
             gc.setLineWidth(sizeSlider.getValue());
        } else {
            gc.setGlobalBlendMode(BlendMode.SRC_OVER);
            gc.setStroke(colorPicker.getValue());
            gc.setLineWidth(sizeSlider.getValue());
        }
        
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        gc.beginPath();
        gc.moveTo(x, y);
        gc.stroke();
    }

    private void handleDrawMove(Point2D worldPoint) {
        Point2D canvasLocal = canvas.parentToLocal(worldPoint.getX(), worldPoint.getY());
        double x = canvasLocal.getX();
        double y = canvasLocal.getY();
        
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        if (isEraser) {
            double size = sizeSlider.getValue();

            gc.closePath();

            double radius = size / 2;
            for (double angle = 0; angle < 360; angle += 5) {
                for (double r = 0; r <= radius; r += 1) {
                    double px = x + r * Math.cos(Math.toRadians(angle));
                    double py = y + r * Math.sin(Math.toRadians(angle));
                    gc.clearRect(px - 0.5, py - 0.5, 1, 1);
                }
            }
        } else {
            gc.lineTo(x, y);
            gc.stroke();
        }
    }

}