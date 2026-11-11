package id.plato.user;

import id.plato.classroom.ClassRoom;
import id.plato.classroom.ClassService;
import id.plato.core.Async;
import id.plato.core.Dialogs;
import id.plato.core.FormDialog;
import id.plato.core.Page;
import id.plato.core.Session;
import id.plato.core.Ui;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class UsersController implements Page {
    private static final String ALL = "Semua peran";
    private static final String STUDENT = "Mahasiswa";
    private static final String ADMIN = "Admin Kelas";
    private static final String SUPER = "SuperAdmin";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private VBox list;

    private final UserService service = new UserService();
    private final ClassService classService = new ClassService();
    private final SuperAdmin actor = (SuperAdmin) Session.user();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    @FXML
    private void initialize() {
        roleFilter.getItems().setAll(ALL, STUDENT, ADMIN, SUPER);
        roleFilter.setValue(ALL);
        roleFilter.setOnAction(event -> load());
        debounce.setOnFinished(event -> load());
        searchField.textProperty().addListener((obs, old, value) -> debounce.playFromStart());
    }

    @Override
    public void onShow(Object param) {
        if (param instanceof String query && !query.isBlank()) {
            searchField.setText(query);
        } else {
            load();
        }
    }

    private void load() {
        String search = searchField.getText();
        Async.run(() -> service.list(search), this::render);
    }

    private void render(List<User> users) {
        list.getChildren().clear();
        List<User> visible = users.stream()
                .filter(u -> ALL.equals(roleFilter.getValue()) || u.getRoleLabel().equals(roleFilter.getValue()))
                .toList();
        if (visible.isEmpty()) {
            list.getChildren().add(Ui.empty("fth-users", "Pengguna tidak ditemukan", "Ubah kata kunci atau filter peran."));
        }
        visible.forEach(user -> list.getChildren().add(row(user)));
    }

    private HBox row(User user) {
        Button edit = Ui.secondary("Ubah");
        edit.setOnAction(event -> openEdit(user));
        Button delete = Ui.danger("Hapus");
        delete.setOnAction(event -> delete(user));
        delete.setDisable(user.getUserId() == actor.getUserId());
        HBox row = Ui.row(14,
                Ui.avatar(user.getName(), 38),
                new VBox(2, Ui.label(user.getName(), "h3"), Ui.label(user.getEmail() + " · " + user.getDetailLine(), "muted", "small")),
                Ui.spacer(),
                Ui.chip(user.getRoleLabel(), "gray"),
                user.isActive() ? Ui.chip("Aktif", "green") : Ui.chip("Nonaktif", "red"),
                edit, delete);
        row.getStyleClass().add("list-row");
        return row;
    }

    private void delete(User user) {
        if (Dialogs.confirm("Hapus Pengguna", "Hapus akun " + user.getName() + " beserta seluruh datanya?", "Hapus")) {
            Async.runVoid(() -> service.delete(user.getUserId()), this::load);
        }
    }

    @FXML
    private void create() {
        Async.run(() -> new FormData(service.cohorts(), classService.myClasses()), this::openCreate);
    }

    private record FormData(List<Cohort> cohorts, List<ClassRoom> classes) {
    }

    private void openCreate(FormData data) {
        ComboBox<String> role = new ComboBox<>();
        role.getItems().addAll(STUDENT, ADMIN);
        if (actor.isGlobal()) {
            role.getItems().add(SUPER);
        }
        role.setValue(STUDENT);
        role.setMaxWidth(Double.MAX_VALUE);
        TextField name = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        RoleFields fields = new RoleFields(data);
        VBox roleBox = new VBox(14);
        roleBox.getChildren().setAll(fields.forRole(STUDENT));
        role.setOnAction(event -> {
            roleBox.getChildren().setAll(fields.forRole(role.getValue()));
            roleBox.getScene().getWindow().sizeToScene();
        });

        new FormDialog("Pengguna Baru")
                .field("Peran", role)
                .field("Nama", name)
                .field("Email", email)
                .field("Password (minimal 8 karakter)", password)
                .node(roleBox)
                .show("Simpan", () -> {
                    User.validateProfile(name.getText(), email.getText());
                    Map<String, Object> body = fields.body(role.getValue());
                    body.put("name", name.getText().trim());
                    body.put("email", email.getText().trim());
                    body.put("password", password.getText());
                    Callable<User> work = () -> service.create(body);
                    return work;
                }, saved -> load());
    }

    private void openEdit(User user) {
        Async.run(classService::myClasses, classes -> showEdit(user, classes));
    }

    private void showEdit(User user, List<ClassRoom> classes) {
        TextField name = new TextField(user.getName());
        TextField email = new TextField(user.getEmail());
        PasswordField password = new PasswordField();
        ComboBox<String> status = new ComboBox<>();
        status.getItems().setAll("Aktif", "Nonaktif");
        status.setValue(user.isActive() ? "Aktif" : "Nonaktif");
        status.setMaxWidth(Double.MAX_VALUE);
        status.setDisable(user.getUserId() == actor.getUserId());

        FormDialog dialog = new FormDialog("Ubah Pengguna")
                .field("Nama", name)
                .field("Email", email)
                .field("Password baru (kosongkan bila tidak diubah)", password)
                .field("Status", status);

        ComboBox<ClassRoom> assigned = new ComboBox<>();
        if (user instanceof Admin admin) {
            assigned.getItems().setAll(classes);
            assigned.setMaxWidth(Double.MAX_VALUE);
            classes.stream()
                    .filter(c -> admin.getAssignedClassId() != null && c.getClassId() == admin.getAssignedClassId())
                    .findFirst().ifPresent(assigned::setValue);
            dialog.field("Kelas yang dimoderasi", assigned);
        }

        dialog.show("Simpan", () -> {
            actor.manageUser(user);
            User.validateProfile(name.getText(), email.getText());
            Map<String, Object> body = new HashMap<>();
            body.put("name", name.getText().trim());
            body.put("email", email.getText().trim());
            body.put("isActive", "Aktif".equals(status.getValue()));
            if (!password.getText().isEmpty()) {
                body.put("password", password.getText());
            }
            if (user instanceof Admin) {
                body.put("assignedClassId", assigned.getValue() == null ? null : assigned.getValue().getClassId());
            }
            Callable<User> work = () -> service.update(user.getUserId(), body);
            return work;
        }, saved -> load());
    }

    /** Field tambahan formulir yang berbeda untuk tiap peran. */
    private final class RoleFields {
        private final TextField studentId = new TextField();
        private final TextField program = new TextField("Sains Data");
        private final TextField semester = new TextField("1");
        private final ComboBox<Cohort> cohort = new ComboBox<>();
        private final TextField adminCode = new TextField();
        private final ComboBox<ClassRoom> classRoom = new ComboBox<>();
        private final ComboBox<String> level = new ComboBox<>();

        RoleFields(FormData data) {
            cohort.getItems().setAll(data.cohorts());
            cohort.setMaxWidth(Double.MAX_VALUE);
            classRoom.getItems().setAll(data.classes());
            classRoom.setMaxWidth(Double.MAX_VALUE);
            level.getItems().setAll("COHORT", "GLOBAL");
            level.setValue("COHORT");
            level.setMaxWidth(Double.MAX_VALUE);
        }

        List<Node> forRole(String role) {
            List<Node> nodes = new ArrayList<>();
            switch (role) {
                case STUDENT -> {
                    nodes.add(labeled("NIM", studentId));
                    nodes.add(labeled("Angkatan", cohort));
                    nodes.add(labeled("Program studi", program));
                    nodes.add(labeled("Semester", semester));
                }
                case ADMIN -> {
                    nodes.add(labeled("Kode admin", adminCode));
                    nodes.add(labeled("Kelas yang dimoderasi", classRoom));
                }
                default -> {
                    nodes.add(labeled("Tingkat", level));
                    nodes.add(labeled("Angkatan (wajib untuk COHORT)", cohort));
                }
            }
            return nodes;
        }

        private VBox labeled(String text, Node control) {
            return new VBox(6, Ui.label(text, "form-label"), control);
        }

        Map<String, Object> body(String role) {
            Map<String, Object> body = new HashMap<>();
            switch (role) {
                case STUDENT -> {
                    if (cohort.getValue() == null) {
                        throw new IllegalArgumentException("Angkatan wajib dipilih.");
                    }
                    actor.manageCohort(cohort.getValue().getId());
                    body.put("role", "STUDENT");
                    body.put("studentId", studentId.getText().trim());
                    body.put("cohortId", cohort.getValue().getId());
                    body.put("studyProgram", program.getText().trim());
                    body.put("semester", parseSemester());
                }
                case ADMIN -> {
                    body.put("role", "ADMIN");
                    body.put("adminCode", adminCode.getText().trim());
                    body.put("assignedClassId", classRoom.getValue() == null ? null : classRoom.getValue().getClassId());
                }
                default -> {
                    body.put("role", "SUPERADMIN");
                    body.put("adminLevel", level.getValue());
                    body.put("cohortId", cohort.getValue() == null ? null : cohort.getValue().getId());
                }
            }
            return body;
        }

        private int parseSemester() {
            try {
                return Integer.parseInt(semester.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Semester harus berupa angka.");
            }
        }
    }
}
