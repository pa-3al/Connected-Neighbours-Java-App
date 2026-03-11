package com.app.plugin.impl;

import com.app.infrastructure.ui.App;
import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import java.util.Set;
import javafx.scene.Node;

public class DrawingPlugin implements Plugin {

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
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/app/view/CanvasView.fxml"));
            loader.setResources(com.app.infrastructure.i18n.I18nService.getInstance().getBundle());
            Parent panel = loader.load();
            
            Set<Node> sliders = panel.lookupAll(".slider");
            for (Node node : sliders) {
                node.getStyleClass().add("tool-slider");
            }

            if (panel instanceof Pane) {
                context.addPanel("Dessin", (Pane) panel);
            }
        } catch (Exception e) {
            context.logError("Failed to load Drawing Canvas panel", e);
        }
    }

    @Override
    public void onUnload(PluginContext context) {
        if (!context.isHeadless()) {
            context.removePanel("Dessin");
        }
    }
}