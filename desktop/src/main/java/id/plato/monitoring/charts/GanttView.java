package id.plato.monitoring.charts;

import id.plato.core.Formats;
import id.plato.core.Route;
import id.plato.core.Router;
import id.plato.core.Ui;
import id.plato.task.Milestone;
import id.plato.task.Task;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/** Timeline tugas satu kelas beserta milestone-nya. */
public class GanttView extends Pane {
    private static final double LABEL_WIDTH = 240;
    private static final double MIN_DAY_WIDTH = 8;
    private static final double HEADER = 34;
    private static final double TASK_ROW = 44;
    private static final double MILESTONE_ROW = 30;
    private static final int MARGIN_DAYS = 3;
    private static final double SCROLLBAR_ROOM = 16;

    private final LocalDate first;
    private final long days;
    private final double dayWidth;

    /** @param availableWidth lebar area; rentang pendek direntangkan agar seluruhnya terlihat */
    public GanttView(List<Task> tasks, double availableWidth) {
        LocalDate today = LocalDate.now();
        LocalDate start = tasks.stream().map(Task::getStartDate).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate end = tasks.stream().map(Task::getDeadline).max(Comparator.naturalOrder()).orElseThrow();
        first = (start.isBefore(today) ? start : today).minusDays(MARGIN_DAYS);
        LocalDate last = (end.isAfter(today) ? end : today).plusDays(MARGIN_DAYS);
        days = ChronoUnit.DAYS.between(first, last) + 1;
        dayWidth = Math.max(MIN_DAY_WIDTH, (availableWidth - LABEL_WIDTH) / days);
        double width = LABEL_WIDTH + days * dayWidth;

        double height = HEADER + tasks.stream().mapToDouble(t -> TASK_ROW + t.getMilestones().size() * MILESTONE_ROW).sum();
        drawMonths(height);
        double y = HEADER;
        for (Task task : tasks) {
            y = drawTask(task, y);
        }
        drawToday(today, y);
        setPrefSize(width, y + SCROLLBAR_ROOM);
        setMinSize(width, y + SCROLLBAR_ROOM);
    }

    private double xOf(LocalDate date) {
        return LABEL_WIDTH + ChronoUnit.DAYS.between(first, date) * dayWidth;
    }

    private void drawMonths(double height) {
        addMonthLabel(first, xOf(first));
        for (LocalDate month = first.plusMonths(1).withDayOfMonth(1); ChronoUnit.DAYS.between(first, month) < days; month = month.plusMonths(1)) {
            getChildren().add(region("gantt-grid", xOf(month), 0, 1, height));
            addMonthLabel(month, xOf(month));
        }
    }

    private void addMonthLabel(LocalDate month, double x) {
        Label label = Ui.label(Formats.month(month), "muted", "small");
        label.relocate(x + 6, 6);
        getChildren().add(label);
    }

    private double drawTask(Task task, double y) {
        Label name = Ui.label(task.getTitle(), "gantt-row-label");
        name.setMaxWidth(LABEL_WIDTH - 16);
        name.resize(LABEL_WIDTH - 16, 20);
        name.setManaged(false);
        name.relocate(0, y + 12);
        getChildren().add(name);
        drawBar(y + 8, task.getStartDate(), task.getDeadline(), task.getProgress(), 26, "gantt-bar", "gantt-bar-fill",
                task.getTitle() + " · " + Formats.date(task.getStartDate()) + " – " + Formats.date(task.getDeadline()) + " · " + task.getProgress() + "%", task);
        double next = y + TASK_ROW;
        for (Milestone milestone : task.getMilestones()) {
            Label label = Ui.label(milestone.getTitle(), "muted", "small");
            label.setMaxWidth(LABEL_WIDTH - 32);
            label.resize(LABEL_WIDTH - 32, 18);
            label.setManaged(false);
            label.relocate(16, next + 6);
            getChildren().add(label);
            drawBar(next + 6, milestone.getStartDate(), milestone.getDeadline(), milestone.getProgress(), 16, "gantt-milestone", "gantt-milestone-fill",
                    milestone.getTitle() + " · " + Formats.date(milestone.getDeadline()) + " · " + milestone.getProgress() + "%", task);
            next += MILESTONE_ROW;
        }
        return next;
    }

    private void drawBar(double y, LocalDate start, LocalDate end, int progress, double height, String track, String fill, String tip, Task task) {
        double x = xOf(start);
        double width = (ChronoUnit.DAYS.between(start, end) + 1) * dayWidth;
        Region bar = region(track, x, y, width, height);
        Region done = region(fill, x, y, width * progress / 100.0, height);
        for (Region part : List.of(bar, done)) {
            Tooltip.install(part, new Tooltip(tip));
            part.setCursor(javafx.scene.Cursor.HAND);
            part.setOnMouseClicked(event -> Router.navigate(Route.TASK_DETAIL, task.getTaskId()));
        }
        getChildren().addAll(bar, done);
    }

    private void drawToday(LocalDate today, double height) {
        getChildren().add(region("gantt-today", xOf(today), HEADER - 8, 1.5, height - HEADER + 8));
    }

    private Region region(String style, double x, double y, double width, double height) {
        Region region = new Region();
        region.getStyleClass().add(style);
        region.setManaged(false);
        region.resizeRelocate(x, y, width, height);
        return region;
    }
}
