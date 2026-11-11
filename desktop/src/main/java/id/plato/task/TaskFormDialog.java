package id.plato.task;

import id.plato.classroom.ClassRoom;
import id.plato.core.FormDialog;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.user.Admin;
import id.plato.user.Student;
import id.plato.user.User;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Formulir pengajuan atau perubahan tugas, termasuk milestone untuk proyek. */
public final class TaskFormDialog {
    private final TaskService service = new TaskService();
    private final ComboBox<ClassRoom> classBox = new ComboBox<>();
    private final TextField titleField = new TextField();
    private final TextArea descriptionField = new TextArea();
    private final ComboBox<TaskType> typeBox = new ComboBox<>();
    private final DatePicker startPicker = new DatePicker();
    private final DatePicker deadlinePicker = new DatePicker();
    private final VBox milestoneBox = new VBox(10);
    private final List<MilestoneRow> rows = new ArrayList<>();

    private record MilestoneRow(HBox node, TextField title, DatePicker start, DatePicker deadline) {
    }

    public void openNew(List<ClassRoom> classes, Consumer<Task> onSaved) {
        classBox.getItems().setAll(classes);
        if (classes.size() == 1) {
            classBox.setValue(classes.get(0));
        }
        classBox.setMaxWidth(Double.MAX_VALUE);
        classBox.setPromptText("Pilih kelas");
        configureCommon();
        typeBox.setValue(TaskType.INDIVIDUAL);
        boolean isStudent = Session.user() instanceof Student;

        Button addMilestone = Ui.secondary("Tambah Milestone");
        addMilestone.setOnAction(event -> addRow());
        VBox projectSection = new VBox(10, Ui.label("Milestone", "form-label"), milestoneBox, addMilestone);
        projectSection.setVisible(false);
        projectSection.setManaged(false);
        typeBox.setOnAction(event -> {
            boolean project = typeBox.getValue() == TaskType.PROJECT;
            projectSection.setVisible(project);
            projectSection.setManaged(project);
            projectSection.getScene().getWindow().sizeToScene();
        });

        form(isStudent ? "Ajukan Tugas" : "Tugas Baru", true)
                .node(projectSection)
                .show(isStudent ? "Ajukan" : "Publikasikan", this::prepareCreate, onSaved);
    }

    public void openEdit(Task task, Consumer<Task> onSaved) {
        configureCommon();
        titleField.setText(task.getTitle());
        descriptionField.setText(task.getDescription());
        typeBox.setValue(task.getTaskType());
        startPicker.setValue(task.getStartDate());
        deadlinePicker.setValue(task.getDeadline());
        form("Ubah Tugas", false).show("Simpan", () -> {
            Task updated = new Task(task.getClassId(), titleField.getText(), descriptionField.getText(),
                    typeBox.getValue(), startPicker.getValue(), deadlinePicker.getValue());
            Callable<Task> work = () -> service.update(task.getTaskId(), updated);
            return work;
        }, onSaved);
    }

    private void configureCommon() {
        typeBox.getItems().setAll(TaskType.values());
        typeBox.setMaxWidth(Double.MAX_VALUE);
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);
        startPicker.setMaxWidth(Double.MAX_VALUE);
        deadlinePicker.setMaxWidth(Double.MAX_VALUE);
    }

    private FormDialog form(String heading, boolean withClass) {
        FormDialog dialog = new FormDialog(heading);
        if (withClass) {
            dialog.field("Kelas", classBox);
        }
        HBox dates = new HBox(12, labeled("Tanggal mulai", startPicker), labeled("Tenggat", deadlinePicker));
        return dialog.field("Judul", titleField)
                .field("Deskripsi", descriptionField)
                .field("Jenis tugas", typeBox)
                .node(dates);
    }

    private VBox labeled(String text, javafx.scene.Node control) {
        VBox box = new VBox(6, Ui.label(text, "form-label"), control);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private void addRow() {
        TextField title = new TextField();
        title.setPromptText("Judul tahapan");
        DatePicker start = new DatePicker(startPicker.getValue());
        DatePicker deadline = new DatePicker(deadlinePicker.getValue());
        start.setPrefWidth(135);
        deadline.setPrefWidth(135);
        Button remove = Ui.iconButton("fth-x");
        HBox node = new HBox(8, title, start, deadline, remove);
        HBox.setHgrow(title, Priority.ALWAYS);
        MilestoneRow row = new MilestoneRow(node, title, start, deadline);
        remove.setOnAction(event -> {
            rows.remove(row);
            milestoneBox.getChildren().remove(node);
            node.getScene().getWindow().sizeToScene();
        });
        rows.add(row);
        milestoneBox.getChildren().add(node);
        node.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                scene.getWindow().sizeToScene();
            }
        });
    }

    private Callable<Task> prepareCreate() {
        if (classBox.getValue() == null) {
            throw new IllegalArgumentException("Kelas wajib dipilih.");
        }
        Task task = new Task(classBox.getValue().getClassId(), titleField.getText(), descriptionField.getText(),
                typeBox.getValue(), startPicker.getValue(), deadlinePicker.getValue());
        if (typeBox.getValue() == TaskType.PROJECT) {
            for (MilestoneRow row : rows) {
                task.addMilestone(new Milestone(row.title().getText(), "", row.start().getValue(), row.deadline().getValue()));
            }
        }
        User user = Session.user();
        if (user instanceof Student student) {
            student.submitTask(task);
        } else if (user instanceof Admin admin) {
            admin.manageClassTask(task);
        }
        return () -> service.create(task);
    }
}
