package id.plato.monitoring.charts;

import id.plato.core.Ui;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;

/** Grid jumlah per sel; warna makin pekat ketika nilainya makin besar. */
public class HeatmapView extends GridPane {
    private static final double LABEL_SHARE = 20;
    private static final String[] SHADES = {"#f6f6f5", "#d6f0de", "#a6dfb4", "#6cc683"};

    public HeatmapView(List<String> rows, List<String> columns, int[][] values) {
        setHgap(10);
        setVgap(10);
        ColumnConstraints head = new ColumnConstraints();
        head.setPercentWidth(LABEL_SHARE);
        getColumnConstraints().add(head);
        for (int c = 0; c < columns.size(); c++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setHgrow(Priority.ALWAYS);
            column.setHalignment(HPos.CENTER);
            column.setPercentWidth((100 - LABEL_SHARE) / columns.size());
            getColumnConstraints().add(column);
            add(Ui.label(columns.get(c), "axis-label"), c + 1, rows.size());
        }
        for (int r = 0; r < rows.size(); r++) {
            Label rowLabel = Ui.label(rows.get(r), "axis-label");
            add(rowLabel, 0, r);
            for (int c = 0; c < columns.size(); c++) {
                add(cell(values[r][c]), c + 1, r);
            }
        }
    }

    private Region cell(int value) {
        Region cell = new Region();
        cell.getStyleClass().add("heat-cell");
        cell.setStyle("-fx-background-color: " + SHADES[Math.min(value, SHADES.length - 1)] + ";");
        cell.setMinHeight(48);
        cell.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(cell, new Tooltip(value + " tenggat"));
        return cell;
    }
}
