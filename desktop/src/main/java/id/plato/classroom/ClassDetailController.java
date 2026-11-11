package id.plato.classroom;

import id.plato.core.Async;
import id.plato.core.Dialogs;
import id.plato.core.FormDialog;
import id.plato.core.Formats;
import id.plato.core.Page;
import id.plato.core.Route;
import id.plato.core.Router;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.task.Task;
import id.plato.task.TaskService;
import id.plato.user.Admin;
import id.plato.user.Student;
import id.plato.user.SuperAdmin;
import id.plato.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.concurrent.Callable;

public class ClassDetailController implements Page {
    @FXML private Label title;
    @FXML private HBox actions;
    @FXML private VBox body;

    private final ClassService service = new ClassService();
    private final TaskService taskService = new TaskService();
    private final User user = Session.user();
    private int classId;

    @Override
    public void onShow(Object param) {
        classId = (Integer) param;
        load();
    }

    private void load() {
        body.getChildren().setAll(Ui.loading());
        Async.run(() -> {
            ClassRoom room = service.find(classId);
            room.setMembers(service.members(classId));
            room.setTasks(taskService.list(classId, null, null, null, null));
            return room;
        }, this::render);
    }

    private boolean canManage() {
        return user instanceof SuperAdmin || user instanceof Admin;
    }

    private void render(ClassRoom room) {
        title.setText(room.getCourseName());
        renderActions(room);
        VBox info = Ui.card(
                Ui.label(room.getClassName() + " · " + room.getCohortName(), "h3"),
                Ui.wrapped(room.getDescription().isBlank() ? "Belum ada deskripsi." : room.getDescription(), "muted"),
                Ui.label(room.getAdminName() == null ? "Belum ada Admin" : "Admin: " + room.getAdminName(), "small"));

        VBox members = membersCard(room);
        VBox tasks = tasksCard(room);
        HBox.setHgrow(members, Priority.ALWAYS);
        HBox.setHgrow(tasks, Priority.ALWAYS);
        members.setPrefWidth(100);
        tasks.setPrefWidth(100);
        HBox columns = new HBox(20, members, tasks);
        body.getChildren().setAll(info, columns);
    }

    private void renderActions(ClassRoom room) {
        actions.getChildren().clear();
        Button back = Ui.secondary("Kembali");
        back.setOnAction(event -> Router.back());
        actions.getChildren().add(back);
        if (canManage()) {
            Button edit = Ui.secondary("Ubah Kelas");
            edit.setOnAction(event -> openEdit(room));
            actions.getChildren().add(edit);
        }
        if (user instanceof SuperAdmin superAdmin) {
            Button delete = Ui.danger("Hapus Kelas");
            delete.setOnAction(event -> {
                superAdmin.manageClass(room);
                if (Dialogs.confirm("Hapus Kelas", "Hapus " + room.getCourseName() + " beserta seluruh tugasnya?", "Hapus")) {
                    Async.runVoid(() -> service.delete(classId), () -> Router.navigate(Route.CLASSES));
                }
            });
            actions.getChildren().add(delete);
        }
    }

    private VBox membersCard(ClassRoom room) {
        HBox head = Ui.row(10, Ui.label("Anggota", "h2"), Ui.spacer());
        if (canManage()) {
            Button add = Ui.secondary("Tambah Anggota");
            add.setOnAction(event -> openAddMember());
            head.getChildren().add(add);
        }
        VBox card = Ui.card(head);
        if (room.getMembers().isEmpty()) {
            card.getChildren().add(Ui.empty("fth-users", "Belum ada anggota", "Mahasiswa dapat bergabung dari menu Kelas."));
        }
        for (Membership member : room.getMembers()) {
            HBox row = Ui.row(12,
                    Ui.avatar(member.getName(), 34),
                    new VBox(2, Ui.label(member.getName(), "h3"),
                            Ui.label(member.getStudentId() + " · bergabung " + Formats.date(member.getJoinedAt().toLocalDate()), "muted", "small")),
                    Ui.spacer());
            if (canManage()) {
                row.getChildren().add(member.isActive() ? Ui.chip("Aktif", "green") : Ui.chip("Nonaktif", "red"));
                Button toggle = Ui.secondary(member.isActive() ? "Nonaktifkan" : "Aktifkan");
                toggle.setOnAction(event -> Async.runVoid(
                        () -> service.setMemberStatus(classId, member.getMembershipId(), member.toggledStatus()), this::load));
                row.getChildren().add(toggle);
            }
            row.getStyleClass().add("list-row");
            card.getChildren().add(row);
        }
        return card;
    }

    private VBox tasksCard(ClassRoom room) {
        VBox card = Ui.card(Ui.label("Tugas", "h2"));
        if (room.getTaskList().isEmpty()) {
            String hint = user instanceof Student ? "Ajukan tugas dari menu Tugas." : "Tugas kelas akan tampil di sini.";
            card.getChildren().add(Ui.empty("fth-check-square", "Belum ada tugas", hint));
        }
        for (Task task : room.getTaskList()) {
            HBox row = Ui.row(12,
                    new VBox(2, Ui.label(task.getTitle(), "h3"), Ui.label("Tenggat " + Formats.date(task.getDeadline()), "muted", "small")),
                    Ui.spacer(),
                    Ui.chip(task.getStatus().toString(), "gray"));
            row.getStyleClass().addAll("list-row", "list-row-clickable");
            row.setOnMouseClicked(event -> Router.navigate(Route.TASK_DETAIL, task.getTaskId()));
            card.getChildren().add(row);
        }
        return card;
    }

    private void openAddMember() {
        TextField email = new TextField();
        email.setPromptText("email mahasiswa");
        new FormDialog("Tambah Anggota")
                .field("Email mahasiswa", email)
                .show("Tambahkan", () -> {
                    String value = email.getText().trim();
                    Callable<Boolean> work = () -> {
                        service.addMember(classId, value);
                        return true;
                    };
                    return work;
                }, ok -> load());
    }

    private void openEdit(ClassRoom room) {
        TextField className = new TextField(room.getClassName());
        TextField courseName = new TextField(room.getCourseName());
        TextArea description = new TextArea(room.getDescription());
        description.setPrefRowCount(3);
        new FormDialog("Ubah Kelas")
                .field("Nama kelas", className)
                .field("Mata kuliah", courseName)
                .field("Deskripsi", description)
                .show("Simpan", () -> {
                    ClassRoom.validate(className.getText(), courseName.getText());
                    String name = className.getText().trim();
                    String course = courseName.getText().trim();
                    String text = description.getText().trim();
                    Callable<ClassRoom> work = () -> service.save(classId, null, name, course, text);
                    return work;
                }, saved -> load());
    }
}
