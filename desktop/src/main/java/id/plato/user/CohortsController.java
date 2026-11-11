package id.plato.user;

import id.plato.core.Async;
import id.plato.core.Dialogs;
import id.plato.core.FormDialog;
import id.plato.core.Session;
import id.plato.core.Ui;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.Callable;

public class CohortsController {
    @FXML private VBox list;
    @FXML private Button createButton;

    private final UserService service = new UserService();
    private final SuperAdmin superAdmin = (SuperAdmin) Session.user();

    @FXML
    private void initialize() {
        createButton.setVisible(superAdmin.isGlobal());
        createButton.setManaged(superAdmin.isGlobal());
        load();
    }

    private void load() {
        list.getChildren().setAll(Ui.loading());
        Async.run(service::cohorts, this::render);
    }

    private void render(List<Cohort> cohorts) {
        list.getChildren().clear();
        if (cohorts.isEmpty()) {
            list.getChildren().add(Ui.empty("fth-layers", "Belum ada angkatan", "Tambahkan angkatan untuk mulai mengelola kelas."));
        }
        cohorts.forEach(cohort -> list.getChildren().add(row(cohort)));
    }

    private HBox row(Cohort cohort) {
        Button edit = Ui.secondary("Ubah");
        edit.setOnAction(event -> openForm(cohort));
        HBox row = Ui.row(14,
                new VBox(2, Ui.label(cohort.getName(), "h3"), Ui.label("Tahun " + cohort.getYear(), "muted", "small")),
                Ui.spacer(),
                Ui.chip(cohort.getStudentCount() + " mahasiswa", "gray"),
                Ui.chip(cohort.getClassCount() + " kelas", "gray"),
                edit);
        if (superAdmin.isGlobal()) {
            Button delete = Ui.danger("Hapus");
            delete.setOnAction(event -> delete(cohort));
            row.getChildren().add(delete);
        }
        row.getStyleClass().add("list-row");
        return row;
    }

    private void delete(Cohort cohort) {
        if (Dialogs.confirm("Hapus Angkatan", "Hapus angkatan " + cohort.getName() + "? Angkatan yang masih memiliki mahasiswa atau kelas tidak dapat dihapus.", "Hapus")) {
            Async.runVoid(() -> service.deleteCohort(cohort.getId()), this::load);
        }
    }

    @FXML
    private void create() {
        openForm(null);
    }

    private void openForm(Cohort cohort) {
        TextField name = new TextField(cohort == null ? "" : cohort.getName());
        TextField year = new TextField(cohort == null ? "" : String.valueOf(cohort.getYear()));
        new FormDialog(cohort == null ? "Angkatan Baru" : "Ubah Angkatan")
                .field("Nama angkatan", name)
                .field("Tahun", year)
                .show("Simpan", () -> {
                    int parsedYear = parseYear(year.getText());
                    String trimmed = name.getText().trim();
                    Integer id = cohort == null ? null : cohort.getId();
                    Callable<Cohort> work = () -> service.saveCohort(id, trimmed, parsedYear);
                    return work;
                }, saved -> load());
    }

    private int parseYear(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tahun harus berupa angka.");
        }
    }
}
