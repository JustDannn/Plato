package id.plato.task;

import id.plato.core.FormDialog;
import id.plato.user.Admin;
import javafx.scene.control.TextArea;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Meminta alasan penolakan sebelum pengajuan ditolak. */
final class RejectDialog {
    private RejectDialog() {
    }

    static void open(Admin admin, Task task, TaskService service, Consumer<Task> onDone) {
        TextArea reason = new TextArea();
        reason.setPrefRowCount(3);
        reason.setWrapText(true);
        new FormDialog("Tolak Pengajuan")
                .field("Alasan penolakan untuk " + task.getTitle(), reason)
                .show("Tolak", () -> {
                    String text = reason.getText();
                    admin.rejectTask(task, text);
                    Callable<Task> work = () -> service.reject(task.getTaskId(), text.trim());
                    return work;
                }, onDone);
    }
}
