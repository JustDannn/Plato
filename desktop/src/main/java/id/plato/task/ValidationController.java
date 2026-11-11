package id.plato.task;

import id.plato.core.Async;
import id.plato.core.Formats;
import id.plato.core.Page;
import id.plato.core.Route;
import id.plato.core.Router;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.user.Admin;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ValidationController implements Page {
    @FXML private VBox list;

    private final TaskService service = new TaskService();
    private final Admin admin = (Admin) Session.user();

    @Override
    public void onShow(Object param) {
        load();
    }

    private void load() {
        list.getChildren().setAll(Ui.card(Ui.loading()));
        Async.run(service::pending, this::render);
    }

    private void render(List<Task> tasks) {
        list.getChildren().clear();
        if (tasks.isEmpty()) {
            list.getChildren().add(Ui.card(Ui.empty("fth-shield", "Tidak ada pengajuan", "Pengajuan baru dari mahasiswa akan tampil di sini.")));
        }
        tasks.forEach(task -> list.getChildren().add(card(task)));
    }

    private VBox card(Task task) {
        Button detail = Ui.secondary("Detail");
        detail.setOnAction(event -> Router.navigate(Route.TASK_DETAIL, task.getTaskId()));
        Button reject = Ui.danger("Tolak");
        reject.setOnAction(event -> RejectDialog.open(admin, task, service, saved -> load()));
        Button approve = Ui.primary("Setujui", "fth-check");
        approve.setOnAction(event -> {
            admin.approveTask(task);
            Async.run(() -> service.approve(task.getTaskId()), saved -> load());
        });
        HBox head = Ui.row(10, Ui.label(task.getTitle(), "h2"), Ui.chip(task.getTaskType().toString(), "gray"), Ui.spacer(), detail, reject, approve);
        return Ui.card(head,
                Ui.wrapped(task.getDescription().isBlank() ? "Tanpa deskripsi." : task.getDescription(), "muted"),
                Ui.label("Diajukan oleh " + task.getCreatedByName() + " · " + Formats.date(task.getStartDate())
                        + " – " + Formats.date(task.getDeadline()), "small"));
    }
}
