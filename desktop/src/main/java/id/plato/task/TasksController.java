package id.plato.task;

import id.plato.classroom.ClassRoom;
import id.plato.classroom.ClassService;
import id.plato.core.Async;
import id.plato.core.Formats;
import id.plato.core.Page;
import id.plato.core.Route;
import id.plato.core.Router;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.user.Student;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Function;

public class TasksController implements Page {
    @FXML private Button createButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<ClassRoom> classFilter;
    @FXML private ComboBox<TaskStatus> statusFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private VBox list;

    private final TaskService service = new TaskService();
    private final ClassService classService = new ClassService();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));
    private List<ClassRoom> classes = List.of();
    private boolean resetting;

    @FXML
    private void initialize() {
        createButton.setText(Session.user() instanceof Student ? "Ajukan Tugas" : "Tugas Baru");
        statusFilter.getItems().setAll(null, TaskStatus.PENDING, TaskStatus.APPROVED, TaskStatus.REJECTED);
        useBlankLabel(classFilter, "Semua kelas", ClassRoom::toString);
        useBlankLabel(statusFilter, "Semua status", TaskStatus::toString);
        debounce.setOnFinished(event -> load());
        searchField.textProperty().addListener((obs, old, value) -> debounce.playFromStart());
        for (var control : List.of(classFilter, statusFilter)) {
            control.valueProperty().addListener((obs, old, value) -> load());
        }
        fromDate.valueProperty().addListener((obs, old, value) -> load());
        toDate.valueProperty().addListener((obs, old, value) -> load());
    }

    private <T> void useBlankLabel(ComboBox<T> box, String blank, Function<T, String> text) {
        box.setCellFactory(view -> cell(blank, text));
        box.setButtonCell(cell(blank, text));
    }

    private <T> ListCell<T> cell(String blank, Function<T, String> text) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? blank : text.apply(item));
            }
        };
    }

    @Override
    public void onShow(Object param) {
        Async.run(classService::myClasses, result -> {
            classes = result;
            classFilter.getItems().setAll((ClassRoom) null);
            classFilter.getItems().addAll(result);
            classFilter.setButtonCell(cell("Semua kelas", ClassRoom::toString));
        });
        if (param instanceof String query && !query.isBlank()) {
            searchField.setText(query);
        } else {
            load();
        }
    }

    private void load() {
        if (resetting) {
            return;
        }
        Integer classId = classFilter.getValue() == null ? null : classFilter.getValue().getClassId();
        var status = statusFilter.getValue();
        var from = fromDate.getValue();
        var to = toDate.getValue();
        String search = searchField.getText();
        Async.run(() -> service.list(classId, status, from, to, search), this::render);
    }

    private void render(List<Task> tasks) {
        list.getChildren().clear();
        if (tasks.isEmpty()) {
            list.getChildren().add(Ui.empty("fth-check-square", "Tidak ada tugas", "Ubah filter atau ajukan tugas baru."));
        }
        tasks.forEach(task -> list.getChildren().add(row(task)));
    }

    private HBox row(Task task) {
        VBox progress = new VBox(4, Ui.label(task.getProgress() + "%", "small"), Ui.progress(task.getProgress()));
        progress.setMinWidth(120);
        progress.setMaxWidth(120);
        HBox row = Ui.row(14,
                new VBox(3, Ui.label(task.getTitle(), "h3"),
                        Ui.label(task.getCourseName() + " · " + task.getTaskType() + " · " + Formats.shortDate(task.getStartDate())
                                + " – " + Formats.date(task.getDeadline()), "muted", "small")),
                Ui.spacer(),
                progress,
                TaskViews.statusChip(task),
                TaskViews.dueChip(task));
        row.getStyleClass().addAll("list-row", "list-row-clickable");
        row.setOnMouseClicked(event -> Router.navigate(Route.TASK_DETAIL, task.getTaskId()));
        return row;
    }

    @FXML
    private void create() {
        new TaskFormDialog().openNew(classes, saved -> load());
    }

    @FXML
    private void resetFilters() {
        resetting = true;
        searchField.clear();
        classFilter.setValue(null);
        statusFilter.setValue(null);
        fromDate.setValue(null);
        toDate.setValue(null);
        resetting = false;
        load();
    }
}
