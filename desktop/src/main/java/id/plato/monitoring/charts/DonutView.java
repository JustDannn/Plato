package id.plato.monitoring.charts;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/** Cincin persentase dengan nilai di tengah. */
public class DonutView extends StackPane {
    private static final double SIZE = 200;
    private static final double STROKE = 26;
    private static final double RADIUS = (SIZE - STROKE) / 2;

    public DonutView(int percent, String caption) {
        setMinSize(SIZE, SIZE);
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
        Arc track = arc(-360);
        track.setStroke(Color.web("#f2f2f0"));
        Arc value = arc(-360.0 * percent / 100);
        value.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#d9f2a8")), new Stop(1, Color.web("#a8e0b6"))));
        // Pane menjaga kedua busur pada titik pusat yang sama.
        Pane ring = new Pane(track, value);
        Label number = new Label(percent + "%");
        number.getStyleClass().add("metric-l");
        Label text = new Label(caption);
        text.getStyleClass().add("muted");
        VBox center = new VBox(2, number, text);
        center.setAlignment(Pos.CENTER);
        getChildren().addAll(ring, center);
    }

    private Arc arc(double length) {
        Arc arc = new Arc(SIZE / 2, SIZE / 2, RADIUS, RADIUS, 90, length);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStrokeWidth(STROKE);
        arc.setStrokeLineCap(StrokeLineCap.BUTT);
        return arc;
    }
}
