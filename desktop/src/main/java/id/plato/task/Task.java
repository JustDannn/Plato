package id.plato.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Task implements Trackable {
    private int taskId;
    private int classId;
    private String className;
    private String courseName;
    private int createdBy;
    private String createdByName;
    private String title;
    private String description;
    private TaskType taskType;
    private LocalDate startDate;
    private LocalDate deadline;
    private TaskStatus status = TaskStatus.DRAFT;
    private String rejectionReason;
    private Integer validatedBy;
    private int progress;
    private LocalDateTime createdAt;

    /** Composition: milestone hanya ada sebagai bagian dari Task. */
    private final List<Milestone> milestones = new ArrayList<>();

    protected Task() {
    }

    public Task(int classId, String title, String description, TaskType taskType, LocalDate startDate, LocalDate deadline) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Judul tugas wajib diisi.");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("Jenis tugas wajib dipilih.");
        }
        requireValidSchedule(startDate, deadline);
        this.classId = classId;
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.taskType = taskType;
        this.startDate = startDate;
        this.deadline = deadline;
    }

    static void requireValidSchedule(LocalDate startDate, LocalDate deadline) {
        if (startDate == null || deadline == null) {
            throw new IllegalArgumentException("Tanggal mulai dan tenggat wajib diisi.");
        }
        if (deadline.isBefore(startDate)) {
            throw new IllegalArgumentException("Tenggat tidak boleh lebih awal dari tanggal mulai.");
        }
    }

    static void requireValidProgress(int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Progres harus 0 sampai 100.");
        }
    }

    public void assignCreator(int userId) {
        this.createdBy = userId;
    }

    public void addMilestone(Milestone milestone) {
        if (taskType != TaskType.PROJECT) {
            throw new IllegalStateException("Milestone hanya untuk tugas proyek.");
        }
        if (milestone.getStartDate().isBefore(startDate) || milestone.getDeadline().isAfter(deadline)) {
            throw new IllegalArgumentException("Milestone \"" + milestone.getTitle() + "\" harus berada dalam rentang tanggal tugas.");
        }
        milestones.add(milestone);
    }

    public void submitForApproval() {
        if (status != TaskStatus.DRAFT && status != TaskStatus.REJECTED) {
            throw new IllegalStateException("Tugas ini sudah diajukan.");
        }
        status = TaskStatus.PENDING;
        rejectionReason = null;
    }

    public void approve(int adminId) {
        requirePending();
        status = TaskStatus.APPROVED;
        validatedBy = adminId;
    }

    public void reject(int adminId, String reason) {
        requirePending();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Alasan penolakan wajib diisi.");
        }
        status = TaskStatus.REJECTED;
        validatedBy = adminId;
        rejectionReason = reason.trim();
    }

    private void requirePending() {
        if (status != TaskStatus.PENDING) {
            throw new IllegalStateException("Hanya pengajuan berstatus menunggu yang dapat divalidasi.");
        }
    }

    /** Dengan milestone, progres dihitung otomatis; tanpa milestone, nilai diisi manual. */
    @Override
    public void updateProgress(int value) {
        requireValidProgress(value);
        if (hasMilestones()) {
            progress = (int) Math.round(milestones.stream().mapToInt(Milestone::getProgress).average().orElse(0));
        } else {
            progress = value;
        }
    }

    public boolean hasMilestones() {
        return !milestones.isEmpty();
    }

    public boolean isDone() {
        return progress >= 100;
    }

    public boolean isOverdue() {
        return !isDone() && deadline.isBefore(LocalDate.now());
    }

    public List<Milestone> getMilestones() {
        return Collections.unmodifiableList(milestones);
    }

    @Override
    public int getProgress() {
        return progress;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
