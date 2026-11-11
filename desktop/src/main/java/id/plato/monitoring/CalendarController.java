package id.plato.monitoring;

import id.plato.core.Async;
import id.plato.core.Formats;
import id.plato.core.Page;
import id.plato.core.Route;
import id.plato.core.Router;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.task.TaskService;
import id.plato.user.Student;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class CalendarController implements Page {
    private static final List<String> DAYS = List.of("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min");
    private static final int MAX_VISIBLE = 2;

    @FXML private GridPane grid;
    @FXML private VBox side;
    @FXML private Label monthLabel;

    private final TaskService service = new TaskService();
    private Dashboard dashboard;
    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();

    @FXML
    private void initialize() {
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
    }

    @Override
    public void onShow(Object param) {
        side.getChildren().setAll(Ui.loading());
        Async.run(service::approved, tasks -> {
            dashboard = new Dashboard(tasks);
            render();
        });
    }

    @FXML
    private void previous() {
        month = month.minusMonths(1);
        render();
    }

    @FXML
    private void next() {
        month = month.plusMonths(1);
        render();
    }

    @FXML
    private void today() {
        month = YearMonth.now();
        selected = LocalDate.now();
        render();
    }

    private Map<LocalDate, List<CalendarEntry>> entries() {
        return Session.user() instanceof Student student
                ? student.viewCalendar(dashboard, month)
                : dashboard.displayCalendar(month);
    }

    private void render() {
        if (dashboard == null) {
            return;
        }
        monthLabel.setText(Formats.month(month.atDay(1)));
        Map<LocalDate, List<CalendarEntry>> entries = entries();
        grid.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            Label head = Ui.label(DAYS.get(i), "cal-head");
            head.setMaxWidth(Double.MAX_VALUE);
            grid.add(head, i, 0);
        }
        LocalDate cursor = month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue() - 1L);
        for (int row = 1; row <= 6; row++) {
            for (int column = 0; column < 7; column++) {
                grid.add(cell(cursor, entries.getOrDefault(cursor, List.of())), column, row);
                cursor = cursor.plusDays(1);
            }
        }
        renderSide(entries.getOrDefault(selected, List.of()));
    }

    private VBox cell(LocalDate date, List<CalendarEntry> items) {
        Label day = Ui.label(String.valueOf(date.getDayOfMonth()), "cal-day");
        if (date.equals(LocalDate.now())) {
            day.getStyleClass().add("cal-today");
        }
        VBox cell = new VBox(new HBox(day));
        cell.getStyleClass().add("cal-cell");
        cell.setMinHeight(88);
        cell.setPrefHeight(88);
        cell.setMinWidth(0);
        if (!YearMonth.from(date).equals(month)) {
            cell.getStyleClass().add("cal-cell-out");
        }
        if (date.equals(selected)) {
            cell.getStyleClass().add("cal-cell-selected");
        }
        items.stream().limit(MAX_VISIBLE).forEach(item -> cell.getChildren().add(entryLabel(item)));
        if (items.size() > MAX_VISIBLE) {
            cell.getChildren().add(Ui.label("+" + (items.size() - MAX_VISIBLE) + " lainnya", "muted", "small"));
        }
        cell.setOnMouseClicked(event -> {
            selected = date;
            if (!YearMonth.from(date).equals(month)) {
                month = YearMonth.from(date);
            }
            render();
        });
        return cell;
    }

    private Label entryLabel(CalendarEntry entry) {
        Label label = Ui.label(entry.title(), "cal-entry");
        if (entry.milestone()) {
            label.getStyleClass().add("cal-entry-milestone");
        }
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinWidth(0);
        return label;
    }

    private void renderSide(List<CalendarEntry> items) {
        side.getChildren().setAll(Ui.label(Formats.date(selected), "h2"));
        if (items.isEmpty()) {
            side.getChildren().add(Ui.empty("fth-sun", "Tidak ada tenggat", "Pilih tanggal lain untuk melihat tugas."));
        }
        for (CalendarEntry entry : items) {
            VBox row = new VBox(4,
                    Ui.wrapped(entry.title(), "h3"),
                    Ui.wrapped(entry.task().getCourseName(), "muted", "small"),
                    Ui.row(6, Ui.chip(entry.milestone() ? "Milestone" : "Tugas", entry.milestone() ? "gray" : "green"),
                            Ui.label(entry.task().getProgress() + "%", "small")));
            row.getStyleClass().addAll("list-row", "list-row-clickable");
            row.setOnMouseClicked(event -> Router.navigate(Route.TASK_DETAIL, entry.task().getTaskId()));
            side.getChildren().add(row);
        }
    }
}
