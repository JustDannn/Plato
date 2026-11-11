package id.plato.user;

import id.plato.core.AccessDeniedException;
import id.plato.core.Route;
import id.plato.monitoring.Notification;
import id.plato.task.Task;

import java.time.LocalDate;
import java.util.List;

public class Admin extends User {
    private String adminCode;
    private Integer assignedClassId;
    private LocalDate appointmentDate;

    protected Admin() {
    }

    public void approveTask(Task task) {
        manageClassTask(task);
        task.approve(userId);
    }

    public void rejectTask(Task task, String reason) {
        manageClassTask(task);
        task.reject(userId, reason);
    }

    /** Memastikan tugas berada di kelas yang dimoderasi Admin ini. */
    public void manageClassTask(Task task) {
        if (assignedClassId == null || assignedClassId != task.getClassId()) {
            throw new AccessDeniedException("Tugas ini berada di luar kelas yang Anda kelola.");
        }
    }

    /** Menyusun pengingat tugas untuk dikirim ke anggota kelas. */
    public Notification sendNotification(Task task, String message) {
        manageClassTask(task);
        return Notification.reminder(task.getTaskId(), message);
    }

    @Override
    public List<Route> getMenu() {
        return List.of(Route.DASHBOARD, Route.CLASSES, Route.TASKS, Route.VALIDATION, Route.CALENDAR, Route.GANTT, Route.ANALYTICS);
    }

    @Override
    public String getRoleLabel() {
        return "Admin Kelas";
    }

    @Override
    public String getDetailLine() {
        return "Kode " + adminCode;
    }

    public String getAdminCode() {
        return adminCode;
    }

    public Integer getAssignedClassId() {
        return assignedClassId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }
}
