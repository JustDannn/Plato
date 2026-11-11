package id.plato.monitoring.charts;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.List;

public class LineChartView extends ChartPane {
    private static final double LEFT = 34;
    private static final double RIGHT = 14;
    private static final double BOTTOM = 28;
    private static final double TOP = 12;

    private final List<String> labels;
    private final List<Integer> values;
    private final String calloutValue;
    private final String calloutText;

    public LineChartView(List<String> labels, List<Integer> values, String calloutValue, String calloutText) {
        super(260);
        this.labels = labels;
        this.values = values;
        this.calloutValue = calloutValue;
        this.calloutText = calloutText;
    }

    @Override
    protected void draw(double width, double height) {
        int max = niceMax(values.stream().mapToInt(Integer::intValue).max().orElse(0));
        double plotHeight = height - BOTTOM - TOP;
        double plotWidth = width - LEFT - RIGHT;

        for (int i = 0; i <= 4; i++) {
            double y = TOP + plotHeight - plotHeight * i / 4;
            text(String.valueOf(max * i / 4), 0, y - 9, LEFT - 8, Pos.CENTER_LEFT);
            Line grid = new Line(LEFT, y, width - RIGHT, y);
            grid.setStroke(Color.web("#ededeb"));
            grid.getStrokeDashArray().setAll(3.0, 4.0);
            getChildren().add(grid);
        }

        int count = values.size();
        double step = plotWidth / Math.max(1, count - 1);
        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = LEFT + step * i;
            ys[i] = TOP + plotHeight - plotHeight * values.get(i) / max;
            text(labels.get(i), xs[i] - step / 2, height - 20, step, Pos.CENTER);
        }

        Path area = curve(xs, ys);
        area.getElements().addAll(new LineTo(xs[count - 1], TOP + plotHeight), new LineTo(xs[0], TOP + plotHeight), new ClosePath());
        area.setStroke(Color.TRANSPARENT);
        area.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#43b05c33")), new Stop(1, Color.web("#43b05c00"))));

        Path line = curve(xs, ys);
        line.setStroke(Color.web("#43b05c"));
        line.setStrokeWidth(2.2);
        line.setFill(Color.TRANSPARENT);

        Circle end = new Circle(xs[count - 1], ys[count - 1], 5.5, Color.web("#43b05c"));
        getChildren().addAll(area, line, end);
        addCallout(xs[count - 1], ys[count - 1]);
    }

    private Path curve(double[] xs, double[] ys) {
        Path path = new Path(new MoveTo(xs[0], ys[0]));
        for (int i = 1; i < xs.length; i++) {
            double mid = (xs[i - 1] + xs[i]) / 2;
            path.getElements().add(new CubicCurveTo(mid, ys[i - 1], mid, ys[i], xs[i], ys[i]));
        }
        return path;
    }

    private void addCallout(double x, double y) {
        Label value = new Label(calloutValue);
        value.setStyle("-fx-font-family: 'Inter Medium'; -fx-text-fill: #0e0e0e;");
        Label caption = new Label(calloutText);
        caption.getStyleClass().add("muted");
        HBox callout = new HBox(6, value, caption);
        callout.getStyleClass().add("callout");
        callout.setManaged(false);
        getChildren().add(callout);
        callout.applyCss();
        callout.autosize();
        double calloutWidth = callout.prefWidth(-1);
        double calloutHeight = callout.prefHeight(-1);
        callout.resize(calloutWidth, calloutHeight);
        double above = y - calloutHeight - 12;
        callout.relocate(Math.max(LEFT, x - calloutWidth + 6), above >= 0 ? above : y + 12);
    }
}
