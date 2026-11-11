package id.plato.task;

import java.time.LocalDate;

public class Milestone implements Trackable {
    private int milestoneId;
    private int taskId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate deadline;
    private int progress;

    protected Milestone() {
    }

    public Milestone(String title, String description, LocalDate startDate, LocalDate deadline) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Judul milestone wajib diisi.");
        }
        Task.requireValidSchedule(startDate, deadline);
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.startDate = startDate;
        this.deadline = deadline;
    }

    /** Memperbarui progres satu tahapan. */
    @Override
    public void updateProgress(int value) {
        Task.requireValidProgress(value);
        this.progress = value;
    }

    public String getStatus() {
        if (progress >= 100) {
            return "Selesai";
        }
        return progress > 0 ? "Berjalan" : "Belum mulai";
    }

    @Override
    public int getProgress() {
        return progress;
    }

    public int getMilestoneId() {
        return milestoneId;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }
}
