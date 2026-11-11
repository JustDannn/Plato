package id.plato.monitoring;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class Notification {
    private int notificationId;
    private int userId;
    private Integer taskId;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;

    protected Notification() {
    }

    public static Notification reminder(int taskId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Pesan wajib diisi.");
        }
        Notification notification = new Notification();
        notification.taskId = taskId;
        notification.message = message.trim();
        notification.type = "REMINDER";
        return notification;
    }

    /** Daftar notifikasi terbaru, seluruhnya atau yang belum dibaca saja. */
    public static List<Notification> getNotification(List<Notification> all, boolean unreadOnly) {
        return all.stream()
                .filter(n -> !unreadOnly || !n.read)
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .toList();
    }

    public void markAsRead() {
        read = true;
    }

    public String getIcon() {
        return switch (type) {
            case "SUBMISSION_APPROVED" -> "fth-check-circle";
            case "SUBMISSION_REJECTED" -> "fth-x-circle";
            case "NEW_SUBMISSION" -> "fth-inbox";
            case "REMINDER" -> "fth-clock";
            default -> "fth-file-text";
        };
    }

    public int getNotificationId() {
        return notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
