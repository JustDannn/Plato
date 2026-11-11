package id.plato.task;

import id.plato.core.Formats;
import id.plato.core.Ui;
import javafx.scene.control.Label;

/** Representasi visual status tugas yang dipakai beberapa halaman. */
public final class TaskViews {
    private TaskViews() {
    }

    public static Label statusChip(Task task) {
        return Ui.chip(task.getStatus().toString(), switch (task.getStatus()) {
            case APPROVED -> "green";
            case PENDING -> "amber";
            case REJECTED -> "red";
            case DRAFT -> "gray";
        });
    }

    public static Label dueChip(Task task) {
        if (task.isDone()) {
            return Ui.chip("Selesai", "green");
        }
        String tone = task.isOverdue() ? "red" : "gray";
        return Ui.chip(Formats.dueIn(task.getDeadline()), tone);
    }
}
