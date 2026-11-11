package id.plato.monitoring.charts;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/** Dasar grafik: menggambar ulang bentuk JavaFX setiap ukuran berubah. */
abstract class ChartPane extends Pane {
    private double drawnWidth = -1;
    private double drawnHeight = -1;

    ChartPane(double height) {
        setPrefHeight(height);
        setMinHeight(height);
    }

    protected abstract void draw(double width, double height);

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width > 0 && height > 0 && (width != drawnWidth || height != drawnHeight)) {
            drawnWidth = width;
            drawnHeight = height;
            getChildren().clear();
            draw(width, height);
        }
        super.layoutChildren();
    }

    protected Label text(String value, double x, double y, double width, javafx.geometry.Pos alignment) {
        Label label = new Label(value);
        label.getStyleClass().add("axis-label");
        label.setAlignment(alignment);
        label.setPrefWidth(width);
        label.relocate(x, y);
        label.setManaged(false);
        label.resize(width, 18);
        getChildren().add(label);
        return label;
    }

    /** Batas atas sumbu Y yang bulat, minimal 4 agar label sumbu bulat. */
    protected static int niceMax(int max) {
        int rounded = Math.max(4, max);
        return rounded % 4 == 0 ? rounded : rounded + (4 - rounded % 4);
    }
}
