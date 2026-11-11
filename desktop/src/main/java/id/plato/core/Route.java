package id.plato.core;

/** Daftar halaman aplikasi. Lokasi FXML relatif terhadap resources/id/plato/views. */
public enum Route {
    DASHBOARD("Dashboard", "fth-grid", "monitoring/dashboard"),
    CLASSES("Kelas", "fth-book-open", "classroom/classes"),
    CLASS_DETAIL("Detail Kelas", "fth-book-open", "classroom/class-detail"),
    TASKS("Tugas", "fth-check-square", "task/tasks"),
    TASK_DETAIL("Detail Tugas", "fth-check-square", "task/task-detail"),
    VALIDATION("Validasi", "fth-shield", "task/validation"),
    CALENDAR("Kalender", "fth-calendar", "monitoring/calendar"),
    GANTT("Gantt Chart", "fth-align-left", "monitoring/gantt"),
    ANALYTICS("Analitik", "fth-bar-chart-2", "monitoring/analytics"),
    NOTIFICATIONS("Notifikasi", "fth-bell", "monitoring/notifications"),
    COHORTS("Angkatan", "fth-layers", "user/cohorts"),
    USERS("Pengguna", "fth-users", "user/users"),
    SETTINGS("Pengaturan", "fth-settings", "user/settings"),
    HELP("Bantuan", "fth-help-circle", "help");

    private final String title;
    private final String icon;
    private final String view;

    Route(String title, String icon, String view) {
        this.title = title;
        this.icon = icon;
        this.view = view;
    }

    public String title() {
        return title;
    }

    public String icon() {
        return icon;
    }

    public String resource() {
        return "/id/plato/views/" + view + ".fxml";
    }
}
