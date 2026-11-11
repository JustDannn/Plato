package id.plato.monitoring;

import id.plato.core.Async;
import id.plato.core.Formats;
import id.plato.core.Page;
import id.plato.core.Ui;
import id.plato.monitoring.charts.BarChartView;
import id.plato.monitoring.charts.DonutView;
import id.plato.monitoring.charts.HeatmapView;
import id.plato.task.Task;
import id.plato.task.TaskService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalyticsController implements Page {
    private static final String MONTH = "Bulan Ini";
    private static final String ALL = "Semua";

    @FXML private VBox body;

    private final TaskService taskService = new TaskService();
    private final MonitoringService monitoring = new MonitoringService();

    private record Data(List<Task> tasks, MonitoringService.Summary summary) {
    }

    @Override
    public void onShow(Object param) {
        body.getChildren().setAll(Ui.loading());
        Async.run(() -> new Data(taskService.approved(), monitoring.summary()), this::render);
    }

    private void render(Data data) {
        Dashboard dashboard = new Dashboard(data.tasks());
        body.getChildren().setAll(
                pair(deadlinesCard(dashboard), Ui.card(Ui.label("Tren Penyelesaian", "h2"), DashboardController.TrendCharts.completion(data.summary()))),
                pair(courseCard(dashboard), loadCard(dashboard)));
    }

    private HBox pair(VBox left, VBox right) {
        left.setPrefWidth(100);
        right.setPrefWidth(100);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return new HBox(22, left, right);
    }

    private VBox deadlinesCard(Dashboard dashboard) {
        YearMonth now = YearMonth.now();
        Map<YearMonth, Integer> perMonth = dashboard.deadlinesPerMonth(now.plusMonths(2), 7);
        List<String> labels = perMonth.keySet().stream().map(m -> Formats.monthShort(m.atDay(1))).toList();
        List<YearMonth> months = new ArrayList<>(perMonth.keySet());
        return Ui.card(Ui.label("Tenggat per Bulan", "h2"),
                new BarChartView(labels, new ArrayList<>(perMonth.values()), months.indexOf(now)));
    }

    private VBox courseCard(Dashboard dashboard) {
        ComboBox<String> period = new ComboBox<>();
        period.getItems().setAll(MONTH, ALL);
        period.setValue(ALL);
        HBox content = new HBox(28);
        Runnable update = () -> renderCourse(content, dashboard, period.getValue());
        period.setOnAction(event -> update.run());
        update.run();
        return Ui.card(Ui.row(10, Ui.label("Progres Mata Kuliah", "h2"), Ui.spacer(), period), content);
    }

    private void renderCourse(HBox content, Dashboard dashboard, String period) {
        LocalDate today = LocalDate.now();
        List<Task> scope = MONTH.equals(period)
                ? dashboard.filterByDate(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()))
                : dashboard.getTasks();
        VBox rows = new VBox(18);
        rows.setAlignment(javafx.geometry.Pos.CENTER);
        HBox.setHgrow(rows, Priority.ALWAYS);
        dashboard.progressByCourse(scope).forEach((course, progress) ->
                rows.getChildren().add(courseRow(course, progress)));
        if (scope.isEmpty()) {
            rows.getChildren().add(Ui.label("Tidak ada tugas pada periode ini.", "muted"));
        }
        content.getChildren().setAll(new DonutView(dashboard.averageProgress(scope), "Rata-rata"), rows);
    }

    private HBox courseRow(String course, int progress) {
        Label value = Ui.label(progress + "%");
        value.setMinWidth(Region.USE_PREF_SIZE);
        return Ui.row(10, Ui.wrapped(course, "muted"), Ui.spacer(), value);
    }

    private VBox loadCard(Dashboard dashboard) {
        List<String> weeks = List.of("Minggu ini", "Minggu +1", "Minggu +2");
        List<String> days = List.of("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min");
        return Ui.card(Ui.label("Beban Tenggat per Hari", "h2"), new HeatmapView(weeks, days, dashboard.weeklyLoad(LocalDate.now(), 3)));
    }
}
