package id.plato.task;

import id.plato.core.Async;
import id.plato.core.Dialogs;
import id.plato.core.FormDialog;
import id.plato.core.Formats;
import id.plato.core.Page;
import id.plato.core.Router;
import id.plato.core.Session;
import id.plato.core.Ui;
import id.plato.monitoring.MonitoringService;
import id.plato.monitoring.Notification;
import id.plato.user.Admin;
import id.plato.user.Student;
import id.plato.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.concurrent.Callable;
import java.util.function.IntFunction;

public class TaskDetailController implements Page {
    @FXML private Label title;
    @FXML private HBox actions;
    @FXML private VBox body;

    private final TaskService service = new TaskService();
    private final MonitoringService monitoring = new MonitoringService();
    private final User user = Session.user();
    private int taskId;

    @Override
    public void onShow(Object param) {
        taskId = (Integer) param;
        body.getChildren().setAll(Ui.loading());
        Async.run(() -> service.find(taskId), this::render);
    }

    private void reload() {
        Async.run(() -> service.find(taskId), this::render);
    }

    private void render(Task task) {
        title.setText(task.getTitle());
        renderActions(task);
        body.getChildren().clear();

        if (task.getStatus() == TaskStatus.REJECTED) {
            VBox notice = new VBox(Ui.wrapped("Ditolak: " + task.getRejectionReason()));
            notice.getStyleClass().add("notice-red");
            body.getChildren().add(notice);
        }
        VBox info = Ui.card(
                Ui.row(8, TaskViews.statusChip(task), Ui.chip(task.getTaskType().toString(), "gray"), TaskViews.dueChip(task)),
                Ui.label(task.getCourseName() + " · " + task.getClassName(), "h3"),
                Ui.wrapped(task.getDescription().isBlank() ? "Belum ada deskripsi." : task.getDescription(), "muted"),
                Ui.label(Formats.date(task.getStartDate()) + " – " + Formats.date(task.getDeadline()) + " · diajukan oleh " + task.getCreatedByName(), "small"));
        body.getChildren().addAll(info, progressCard(task));
        if (task.getTaskType() == TaskType.PROJECT) {
            body.getChildren().add(milestonesCard(task));
        }
    }

    private boolean canUpdateProgress(Task task) {
        return user instanceof Student && task.getStatus() == TaskStatus.APPROVED;
    }

    private VBox progressCard(Task task) {
        String heading = user instanceof Student ? "Progres Saya" : "Rata-rata Progres Kelas";
        VBox card = Ui.card(Ui.row(10, Ui.label(heading, "h2"), Ui.spacer(), Ui.label(task.getProgress() + "%", "metric-m")));
        if (canUpdateProgress(task) && !task.hasMilestones()) {
            card.getChildren().add(progressEditor(task, value -> service.updateProgress(taskId, value)));
        } else {
            card.getChildren().add(Ui.progress(task.getProgress()));
            if (task.hasMilestones()) {
                card.getChildren().add(Ui.label("Dihitung otomatis dari progres milestone.", "muted", "small"));
            }
        }
        return card;
    }

    private HBox progressEditor(Trackable item, IntFunction<Task> save) {
        Slider slider = new Slider(0, 100, item.getProgress());
        slider.setBlockIncrement(5);
        Label value = Ui.label(item.getProgress() + "%");
        value.setMinWidth(44);
        Button apply = Ui.secondary("Simpan");
        apply.setDisable(true);
        slider.valueProperty().addListener((obs, old, next) -> {
            value.setText(next.intValue() + "%");
            apply.setDisable(next.intValue() == item.getProgress());
        });
        apply.setOnAction(event -> {
            int target = (int) slider.getValue();
            ((Student) user).updateProgress(item, target);
            apply.setDisable(true);
            Async.run(() -> save.apply(target), this::render);
        });
        HBox.setHgrow(slider, Priority.ALWAYS);
        return Ui.row(12, slider, value, apply);
    }

    private VBox milestonesCard(Task task) {
        HBox head = Ui.row(10, Ui.label("Milestone", "h2"), Ui.spacer());
        boolean managing = isManagingAdmin(task);
        if (managing) {
            Button add = Ui.secondary("Tambah Milestone");
            add.setOnAction(event -> openMilestoneForm());
            head.getChildren().add(add);
        }
        VBox card = Ui.card(head);
        if (!task.hasMilestones()) {
            card.getChildren().add(Ui.empty("fth-flag", "Belum ada milestone", "Tahapan proyek akan tampil di sini."));
        }
        for (Milestone milestone : task.getMilestones()) {
            HBox top = Ui.row(10,
                    new VBox(2, Ui.label(milestone.getTitle(), "h3"),
                            Ui.label(Formats.shortDate(milestone.getStartDate()) + " – " + Formats.date(milestone.getDeadline()), "muted", "small")),
                    Ui.spacer(),
                    Ui.chip(milestone.getStatus(), milestone.getProgress() >= 100 ? "green" : "gray"));
            if (managing) {
                Button delete = Ui.danger("Hapus");
                delete.setOnAction(event -> {
                    if (Dialogs.confirm("Hapus Milestone", "Hapus milestone " + milestone.getTitle() + "?", "Hapus")) {
                        Async.runVoid(() -> service.deleteMilestone(milestone.getMilestoneId()), this::reload);
                    }
                });
                top.getChildren().add(delete);
            }
            VBox row = new VBox(10, top, canUpdateProgress(task)
                    ? progressEditor(milestone, value -> service.updateMilestoneProgress(milestone.getMilestoneId(), value))
                    : Ui.progress(milestone.getProgress()));
            row.getStyleClass().add("list-row");
            card.getChildren().add(row);
        }
        return card;
    }

    private boolean isManagingAdmin(Task task) {
        return user instanceof Admin admin && admin.getAssignedClassId() != null && admin.getAssignedClassId() == task.getClassId();
    }

    private void renderActions(Task task) {
        actions.getChildren().clear();
        Button back = Ui.secondary("Kembali");
        back.setOnAction(event -> Router.back());
        actions.getChildren().add(back);

        boolean managing = isManagingAdmin(task);
        boolean owner = user instanceof Student && task.getCreatedBy() == user.getUserId() && task.getStatus() != TaskStatus.APPROVED;
        if (managing && task.getStatus() == TaskStatus.PENDING) {
            Admin admin = (Admin) user;
            Button approve = Ui.primary("Setujui", "fth-check");
            approve.setOnAction(event -> {
                admin.approveTask(task);
                Async.run(() -> service.approve(taskId), this::render);
            });
            Button reject = Ui.danger("Tolak");
            reject.setOnAction(event -> RejectDialog.open(admin, task, service, this::render));
            actions.getChildren().addAll(reject, approve);
        }
        if (managing && task.getStatus() == TaskStatus.APPROVED) {
            Button remind = Ui.secondary("Kirim Pengingat");
            remind.setOnAction(event -> openReminder(task));
            actions.getChildren().add(remind);
        }
        if (managing || owner) {
            Button edit = Ui.secondary(owner && task.getStatus() == TaskStatus.REJECTED ? "Perbaiki & Ajukan Ulang" : "Ubah");
            edit.setOnAction(event -> new TaskFormDialog().openEdit(task, this::render));
            Button delete = Ui.danger("Hapus");
            delete.setOnAction(event -> {
                if (Dialogs.confirm("Hapus Tugas", "Hapus tugas " + task.getTitle() + "?", "Hapus")) {
                    Async.runVoid(() -> service.delete(taskId), Router::back);
                }
            });
            actions.getChildren().addAll(edit, delete);
        }
    }

    private void openReminder(Task task) {
        TextArea message = new TextArea("Pengingat: " + task.getTitle() + " berakhir " + Formats.date(task.getDeadline()) + ".");
        message.setPrefRowCount(3);
        message.setWrapText(true);
        new FormDialog("Kirim Pengingat")
                .field("Pesan untuk anggota kelas", message)
                .show("Kirim", () -> {
                    Notification notification = ((Admin) user).sendNotification(task, message.getText());
                    Callable<Boolean> work = () -> {
                        monitoring.sendToClass(task.getClassId(), notification);
                        return true;
                    };
                    return work;
                }, ok -> { });
    }

    private void openMilestoneForm() {
        TextField name = new TextField();
        DatePicker start = new DatePicker();
        DatePicker deadline = new DatePicker();
        start.setMaxWidth(Double.MAX_VALUE);
        deadline.setMaxWidth(Double.MAX_VALUE);
        new FormDialog("Tambah Milestone")
                .field("Judul", name)
                .field("Tanggal mulai", start)
                .field("Tenggat", deadline)
                .show("Simpan", () -> {
                    Milestone milestone = new Milestone(name.getText(), "", start.getValue(), deadline.getValue());
                    Callable<Task> work = () -> service.addMilestone(taskId, milestone);
                    return work;
                }, this::render);
    }
}
