package id.plato.monitoring;

import id.plato.classroom.ClassRoom;
import id.plato.classroom.ClassService;
import id.plato.core.Async;
import id.plato.core.Page;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.monitoring.charts.GanttView;
import id.plato.task.Task;
import id.plato.task.TaskService;
import id.plato.user.Student;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class GanttController implements Page {
    private static final double CARD_PADDING = 50;

    @FXML private ComboBox<ClassRoom> classBox;
    @FXML private VBox body;

    private final TaskService taskService = new TaskService();
    private final ClassService classService = new ClassService();
    private Dashboard dashboard;

    private record Data(List<Task> tasks, List<ClassRoom> classes) {
    }

    @Override
    public void onShow(Object param) {
        body.getChildren().setAll(Ui.loading());
        Async.run(() -> new Data(taskService.approved(), classService.myClasses()), data -> {
            dashboard = new Dashboard(data.tasks());
            classBox.getItems().setAll(data.classes());
            classBox.setOnAction(event -> render());
            body.widthProperty().addListener((obs, old, width) -> render());
            if (data.classes().isEmpty()) {
                body.getChildren().setAll(Ui.empty("fth-align-left", "Belum ada kelas", "Gabung ke kelas untuk melihat timeline tugas."));
            } else {
                classBox.setValue(data.classes().get(0));
            }
        });
    }

    private void render() {
        ClassRoom room = classBox.getValue();
        if (room == null || body.getWidth() == 0) {
            return;
        }
        List<Task> tasks = Session.user() instanceof Student student
                ? student.viewGanttChart(dashboard, room.getClassId())
                : dashboard.displayGanttChart(room.getClassId());
        if (tasks.isEmpty()) {
            body.getChildren().setAll(Ui.empty("fth-align-left", "Belum ada tugas", "Tugas yang disetujui di kelas ini akan tampil di sini."));
            return;
        }
        ScrollPane scroll = new ScrollPane(new GanttView(tasks, body.getWidth() - CARD_PADDING));
        scroll.getStyleClass().add("scroll");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToHeight(true);
        body.getChildren().setAll(scroll);
    }
}
