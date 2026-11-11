package id.plato.monitoring.charts;

import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import java.util.List;

public class BarChartView extends ChartPane {
    private static final double LEFT = 34;
    private static final double BOTTOM = 28;
    private static final double TOP = 8;

    private final List<String> labels;
    private final List<Integer> values;
    private final int highlight;

    public BarChartView(List<String> labels, List<Integer> values, int highlight) {
        super(260);
        this.labels = labels;
        this.values = values;
        this.highlight = highlight;
    }

    @Override
    protected void draw(double width, double height) {
        int max = niceMax(values.stream().mapToInt(Integer::intValue).max().orElse(0));
        double plotHeight = height - BOTTOM - TOP;
        for (int i = 0; i <= 4; i++) {
            text(String.valueOf(max * i / 4), 0, TOP + plotHeight - plotHeight * i / 4 - 9, LEFT - 8, Pos.CENTER_LEFT);
        }
        double slot = (width - LEFT) / values.size();
        double barWidth = Math.min(56, slot * 0.66);
        for (int i = 0; i < values.size(); i++) {
            double x = LEFT + slot * i + (slot - barWidth) / 2;
            double barHeight = Math.max(3, plotHeight * values.get(i) / max);
            double y = TOP + plotHeight - barHeight;
            boolean active = i == highlight;
            Rectangle bar = new Rectangle(x, y, barWidth, barHeight);
            bar.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, active ? Color.web("#cdeed5") : Color.web("#ececea")),
                    new Stop(1, active ? Color.web("#cdeed500") : Color.web("#ececea00"))));
            Rectangle cap = new Rectangle(x, y, barWidth, 2);
            cap.setFill(active ? Color.web("#43b05c") : Color.web("#d8d8d5"));
            getChildren().addAll(bar, cap);
            text(labels.get(i), LEFT + slot * i, height - 20, slot, Pos.CENTER);
        }
    }
}
