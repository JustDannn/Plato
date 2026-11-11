package id.plato.classroom;

import id.plato.core.Async;
import id.plato.core.FormDialog;
import id.plato.core.Page;
import id.plato.core.Route;
import id.plato.core.Router;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.user.Cohort;
import id.plato.user.Student;
import id.plato.user.SuperAdmin;
import id.plato.user.User;
import id.plato.user.UserService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.Callable;

public class ClassesController implements Page {
    @FXML private VBox body;
    @FXML private Button createButton;

    private final ClassService service = new ClassService();
    private final UserService userService = new UserService();
    private final User user = Session.user();

    private record Data(List<ClassRoom> mine, List<ClassRoom> available) {
    }

    @Override
    public void onShow(Object param) {
        boolean canCreate = user instanceof SuperAdmin;
        createButton.setVisible(canCreate);
        createButton.setManaged(canCreate);
        load();
    }

    private void load() {
        body.getChildren().setAll(Ui.loading());
        Async.run(() -> new Data(service.myClasses(), user instanceof Student ? service.available() : List.of()), this::render);
    }

    private void render(Data data) {
        body.getChildren().clear();
        if (data.mine().isEmpty()) {
            body.getChildren().add(Ui.card(Ui.empty("fth-book-open", "Belum ada kelas", emptyHint())));
        } else {
            body.getChildren().add(grid(data.mine().stream().map(this::classCard).toList()));
        }
        if (user instanceof Student) {
            body.getChildren().add(Ui.label("Kelas Tersedia", "h2"));
            if (data.available().isEmpty()) {
                body.getChildren().add(Ui.label("Tidak ada kelas lain di angkatanmu.", "muted"));
            } else {
                body.getChildren().add(grid(data.available().stream().map(this::joinCard).toList()));
            }
        }
    }

    private String emptyHint() {
        return user instanceof Student ? "Gabung ke kelas yang tersedia di bawah." : "Kelas yang dikelola akan tampil di sini.";
    }

    private TilePane grid(List<Node> cards) {
        TilePane grid = new TilePane(16, 16);
        grid.setPrefTileWidth(310);
        grid.setTileAlignment(javafx.geometry.Pos.TOP_LEFT);
        grid.getChildren().setAll(cards);
        return grid;
    }

    private VBox baseCard(ClassRoom room) {
        VBox card = Ui.card(
                Ui.label(room.getCourseName(), "h3"),
                Ui.label(room.getClassName() + " · " + room.getCohortName(), "muted", "small"),
                Ui.row(8,
                        Ui.chip(room.getMemberCount() + " anggota", "gray"),
                        Ui.chip(room.getTaskCount() + " tugas", "gray")));
        card.setPrefHeight(150);
        return card;
    }

    private Node classCard(ClassRoom room) {
        VBox card = baseCard(room);
        card.getChildren().add(Ui.label(room.getAdminName() == null ? "Belum ada Admin" : "Admin: " + room.getAdminName(), "muted", "small"));
        card.getStyleClass().add("card-clickable");
        card.setOnMouseClicked(event -> Router.navigate(Route.CLASS_DETAIL, room.getClassId()));
        return card;
    }

    private Node joinCard(ClassRoom room) {
        VBox card = baseCard(room);
        Button join = Ui.secondary("Gabung");
        join.setOnAction(event -> Async.runVoid(() -> service.join(room.getClassId()), this::load));
        card.getChildren().add(join);
        return card;
    }

    @FXML
    private void create() {
        Async.run(userService::cohorts, this::openForm);
    }

    private void openForm(List<Cohort> cohorts) {
        ComboBox<Cohort> cohort = new ComboBox<>();
        cohort.getItems().setAll(cohorts);
        cohort.setMaxWidth(Double.MAX_VALUE);
        if (cohorts.size() == 1) {
            cohort.setValue(cohorts.get(0));
        }
        TextField className = new TextField();
        TextField courseName = new TextField();
        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        new FormDialog("Kelas Baru")
                .field("Angkatan", cohort)
                .field("Nama kelas", className)
                .field("Mata kuliah", courseName)
                .field("Deskripsi", description)
                .show("Simpan", () -> {
                    if (cohort.getValue() == null) {
                        throw new IllegalArgumentException("Angkatan wajib dipilih.");
                    }
                    ClassRoom.validate(className.getText(), courseName.getText());
                    ((SuperAdmin) user).manageCohort(cohort.getValue().getId());
                    int cohortId = cohort.getValue().getId();
                    String name = className.getText().trim();
                    String course = courseName.getText().trim();
                    String text = description.getText().trim();
                    Callable<ClassRoom> work = () -> service.save(null, cohortId, name, course, text);
                    return work;
                }, saved -> load());
    }
}
