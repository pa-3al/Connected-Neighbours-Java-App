package com.app.plugins.pingpong;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class PongGamePane extends StackPane {
    private double p1Y = 185, p2Y = 185;
    private double bx = 350, by = 225, bvx = 4, bvy = 2;
    private int s1 = 0, s2 = 0;
    private boolean kZ, kS, kP, kM;
    private AnimationTimer loop;
    private Canvas canvas;

    public PongGamePane() {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #000000;");
        canvas = new Canvas(700, 450);
        getChildren().add(canvas);
        setFocusTraversable(true);

        canvas.setOnMouseClicked(e -> requestFocus());

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.Z) kZ = true;
            if (e.getCode() == KeyCode.S) kS = true;
            if (e.getCode() == KeyCode.P) kP = true;
            if (e.getCode() == KeyCode.M) kM = true;
            if (e.getCode() == KeyCode.R) reset();
        });

        setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.Z) kZ = false;
            if (e.getCode() == KeyCode.S) kS = false;
            if (e.getCode() == KeyCode.P) kP = false;
            if (e.getCode() == KeyCode.M) kM = false;
        });

        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        loop.start();
    }

    public void reset() {
        p1Y = 185; p2Y = 185;
        s1 = 0; s2 = 0;
        bx = 350; by = 225;
        bvx = Math.random() > 0.5 ? 4 : -4;
        bvy = Math.random() * 4 - 2;
    }

    public void stopGame() {
        if (loop != null) loop.stop();
    }

    private void update() {
        if (kZ) p1Y = Math.max(0, p1Y - 5);
        if (kS) p1Y = Math.min(370, p1Y + 5);
        if (kP) p2Y = Math.max(0, p2Y - 5);
        if (kM) p2Y = Math.min(370, p2Y + 5);

        bx += bvx;
        by += bvy;

        if (by <= 5 || by >= 445) bvy = -bvy;

        if (bx <= 30 && bx >= 20 && by >= p1Y && by <= p1Y + 80 && bvx < 0) {
            bvx = -bvx;
            bvy += (by - (p1Y + 40)) * 0.1;
        }
        if (bx >= 670 && bx <= 680 && by >= p2Y && by <= p2Y + 80 && bvx > 0) {
            bvx = -bvx;
            bvy += (by - (p2Y + 40)) * 0.1;
        }

        if (bx < 0) {
            s2++;
            bx = 350; by = 225;
            bvx = 4; bvy = Math.random() * 4 - 2;
        }
        if (bx > 700) {
            s1++;
            bx = 350; by = 225;
            bvx = -4; bvy = Math.random() * 4 - 2;
        }
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, 700, 450);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeLine(350, 0, 350, 450);

        gc.setFill(Color.WHITE);
        gc.fillRect(20, p1Y, 10, 80);
        gc.fillRect(670, p2Y, 10, 80);
        gc.fillRect(bx - 5, by - 5, 10, 10);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Courier New", 40));
        gc.fillText(String.valueOf(s1), 270, 50);
        gc.fillText(String.valueOf(s2), 430, 50);
    }
}
