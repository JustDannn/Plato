package id.plato.monitoring;

import id.plato.classroom.ClassRoom;
import id.plato.classroom.ClassService;
import id.plato.core.Async;
import id.plato.core.Formats;
import id.plato.core.Page;
import id.plato.core.Route;
import id.plato.core.Router;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.monitoring.charts.LineChartView;
import id.plato.task.Task;
import id.plato.task.TaskFormDialog;
import id.plato.task.TaskService;
import id.plato.user.Admin;
import id.plato.user.Student;
import id.plato.user.SuperAdmin;
import id.plato.user.User;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class DashboardController implements Page {
    private static final String WEEK = "Minggu Ini";
    private static final String MONTH = "Bulan Ini";
    private static final String ALL = "Semua";

    @FXML private Label title;
    @FXML private Button primaryAction;
    @FXML private HBox body;

    private final TaskService taskService = new TaskService();
    private final ClassService classService = new ClassService();
    private final MonitoringService monitoring = new MonitoringService();
    private final User user = Session.user();

    private record Data(List<Task> tasks, List<ClassRoom> classes, MonitoringService.Summary summary) {
    }

    @Override
    public void onShow(Object param) {
        title.setText("Selamat datang, " + user.getFirstName() + "!");
        configureAction();
        body.getChildren().setAll(Ui.loading());
        Async.run(() -> new Data(user instanceof SuperAdmin ? List.of() : taskService.all(), classService.myClasses(), monitoring.summary()), this::render);
    }

    private void configureAction() {
        if (user instanceof SuperAdmin) {
            primaryAction.setText("Kelola Pengguna");
            primaryAction.setOnAction(event -> Router.navigate(Route.USERS));
        } else {
            primaryAction.setText(user instanceof Student ? "Ajukan Tugas" : "Tugas Baru");
        }
    }

    private void render(Data data) {
        if (!(user instanceof SuperAdmin)) {
            primaryAction.setOnAction(event -> new TaskFormDialog().openNew(data.classes(), saved -> Router.reload()));
        }
        Dashboard dashboard = new Dashboard(data.tasks());
        VBox next = nextCard(dashboard, data);
        VBox.setVgrow(next, Priority.ALWAYS);
        VBox left = new VBox(22, user instanceof SuperAdmin ? cohortCard(data.summary()) : overviewCard(dashboard), next);
        VBox right = new VBox(22, statsCard(dashboard, data), trendCard(data.summary()));
        left.setPrefWidth(440);
        left.setMinWidth(360);
        right.setPrefWidth(620);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        body.getChildren().setAll(left, right);
    }

    private VBox overviewCard(Dashboard dashboard) {
        ComboBox<String> period = new ComboBox<>();
        period.getItems().setAll(WEEK, MONTH, ALL);
        period.setValue(MONTH);
        VBox numbers = new VBox(14);
        Runnable update = () -> numbers.getChildren().setAll(overviewNumbers(dashboard, period.getValue()));
        period.setOnAction(event -> update.run());
        update.run();
        return Ui.card(Ui.row(10, Ui.label("Ringkasan Tugas", "h2"), Ui.spacer(), period), numbers);
    }

    private List<Node> overviewNumbers(Dashboard dashboard, String period) {
        LocalDate today = LocalDate.now();
        List<Task> scope = switch (period) {
            case WEEK -> dashboard.filterByDate(today, today.plusDays(6));
            case MONTH -> dashboard.filterByDate(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
            default -> dashboard.getTasks();
        };
        long done = dashboard.countDone(scope);
        long active = scope.size() - done;
        VBox big = new VBox(2, Ui.label(String.valueOf(active), "metric-xl"), Ui.label("Tugas Aktif", "muted"));
        HBox split = Ui.row(0,
                new VBox(2, Ui.label(String.valueOf(done), "metric-m"), Ui.label("Selesai", "muted", "small")),
                Ui.spacer(),
                rightAligned(Ui.label(String.valueOf(scope.size()), "metric-m"), Ui.label("Total Tugas", "muted", "small")));
        return List.of(big, split, progressStrip(dashboard.averageProgress(scope)));
    }

    private VBox rightAligned(Node... nodes) {
        VBox box = new VBox(2, nodes);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    /** Pita progres: bagian berwarna adalah yang sudah dicapai, garis-garis adalah sisanya. */
    private HBox progressStrip(int percent) {
        Region filled = new Region();
        filled.setStyle("-fx-background-color: linear-gradient(to right, #f9dfe0, #e6f0c8, #bfe6c6);");
        filled.setMinHeight(66);
        HBox ticks = new HBox(10);
        ticks.setAlignment(Pos.CENTER_LEFT);
        int tickCount = Math.max(1, (100 - percent) / 4);
        for (int i = 0; i < tickCount; i++) {
            Region tick = new Region();
            tick.setMinWidth(1);
            tick.setMaxWidth(1);
            tick.setMinHeight(66);
            tick.setStyle("-fx-background-color: " + (i == 0 ? "#0e0e0e" : "#d6d6d3") + ";");
            ticks.getChildren().add(tick);
        }
        HBox strip = new HBox(10, filled, ticks);
        strip.setMinHeight(66);
        strip.widthProperty().addListener((obs, old, width) -> filled.setPrefWidth(Math.max(4, width.doubleValue() * percent / 100.0)));
        return strip;
    }

    private VBox cohortCard(MonitoringService.Summary summary) {
        VBox big = new VBox(2, Ui.label(String.valueOf(summary.studentCount()), "metric-xl"), Ui.label("Mahasiswa Terdaftar", "muted"));
        HBox split = Ui.row(0,
                new VBox(2, Ui.label(String.valueOf(summary.classCount()), "metric-m"), Ui.label("Kelas", "muted", "small")),
                Ui.spacer(),
                rightAligned(Ui.label(String.valueOf(summary.cohortCount()), "metric-m"), Ui.label("Angkatan", "muted", "small")));
        return Ui.card(Ui.label("Ringkasan Angkatan", "h2"), big, split);
    }

    private VBox nextCard(Dashboard dashboard, Data data) {
        if (user instanceof SuperAdmin) {
            return classListCard(data.classes());
        }
        List<Task> upcoming = dashboard.getUpcomingTasks();
        if (upcoming.isEmpty()) {
            return Ui.card(Ui.label("Tenggat Terdekat", "h2"), Ui.empty("fth-coffee", "Tidak ada tenggat", "Semua tugas yang berjalan sudah selesai."));
        }
        Task next = upcoming.get(0);
        Button open = Ui.iconButton("fth-arrow-up-right");
        open.setOnAction(event -> Router.navigate(Route.TASK_DETAIL, next.getTaskId()));
        VBox titleBox = new VBox(6, Ui.wrapped(next.getTitle(), "h2"),
                Ui.row(6, Ui.icon("fth-clock", 14), Ui.label(Formats.date(next.getDeadline()) + " · " + next.getCourseName(), "small")));
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        gap.setMinHeight(24);
        HBox foot = Ui.row(10, Ui.label(next.getProgress() + "% selesai", "muted"), Ui.spacer(),
                Ui.chip(Formats.dueIn(next.getDeadline()), next.getDeadline().isBefore(LocalDate.now().plusDays(4)) ? "red" : "gray"));
        return Ui.card(Ui.row(10, titleBox, open), gap, Ui.progress(next.getProgress()), foot);
    }

    private VBox classListCard(List<ClassRoom> classes) {
        VBox card = Ui.card(Ui.label("Kelas", "h2"));
        classes.stream().limit(5).forEach(room -> {
            HBox row = Ui.row(10,
                    new VBox(2, Ui.label(room.getCourseName(), "h3"), Ui.label(room.getClassName(), "muted", "small")),
                    Ui.spacer(),
                    Ui.chip(room.getMemberCount() + " anggota", "gray"));
            row.getStyleClass().addAll("list-row", "list-row-clickable");
            row.setOnMouseClicked(event -> Router.navigate(Route.CLASS_DETAIL, room.getClassId()));
            card.getChildren().add(row);
        });
        return card;
    }

    private VBox statsCard(Dashboard dashboard, Data data) {
        MonitoringService.Summary summary = data.summary();
        HBox tiles = new HBox(14);
        if (user instanceof Student) {
            tiles.getChildren().setAll(
                    tile("Kelas Diikuti", String.valueOf(summary.classCount()), "Kelas aktif"),
                    tile("Tenggat 7 Hari", String.valueOf(dashboard.countDueWithin(7)), "Perlu diprioritaskan"),
                    tile("Rata-rata Progres", dashboard.averageProgress(dashboard.getTasks()) + "%", "Seluruh tugas"));
        } else if (user instanceof Admin) {
            tiles.getChildren().setAll(
                    tile("Anggota Kelas", String.valueOf(summary.memberCount()), "Anggota aktif"),
                    tile("Menunggu Validasi", String.valueOf(summary.pendingCount()), "Pengajuan baru"),
                    tile("Rata-rata Progres", dashboard.averageProgress(dashboard.getTasks()) + "%", "Seluruh anggota"));
        } else {
            tiles.getChildren().setAll(
                    tile("Admin Kelas", String.valueOf(summary.adminCount()), "Sudah ditugaskan"),
                    tile("Menunggu Validasi", String.valueOf(summary.pendingCount()), "Di seluruh kelas"),
                    tile("Kelas", String.valueOf(summary.classCount()), "Dalam wewenang"));
        }
        return Ui.card(Ui.label("Pemantauan Progres", "h2"), tiles);
    }

    private VBox tile(String caption, String value, String hint) {
        VBox tile = new VBox(Ui.label(caption, "muted"), Ui.label(value, "metric-l"), Ui.label(hint, "positive", "small"));
        tile.getStyleClass().add("tile");
        tile.setPrefWidth(100);
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    private VBox trendCard(MonitoringService.Summary summary) {
        return Ui.card(Ui.label("Tren Penyelesaian", "h2"), TrendCharts.completion(summary));
    }

    /** Dipakai juga oleh halaman Analitik. */
    static final class TrendCharts {
        private TrendCharts() {
        }

        static LineChartView completion(MonitoringService.Summary summary) {
            List<MonitoringService.TrendPoint> points = summary.completionTrend();
            List<Integer> values = points.stream().map(MonitoringService.TrendPoint::total).toList();
            List<String> labels = points.stream().map(p -> Formats.shortDate(p.weekStart())).toList();
            int gained = values.get(values.size() - 1) - values.get(values.size() - 2);
            return new LineChartView(labels, values, "+" + gained, "minggu ini");
        }
    }
}
