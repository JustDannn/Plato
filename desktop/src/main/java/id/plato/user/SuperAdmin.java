package id.plato.user;

import id.plato.classroom.ClassRoom;
import id.plato.core.AccessDeniedException;
import id.plato.core.Route;

import java.util.List;
import java.util.Objects;

public class SuperAdmin extends User {
    private static final String GLOBAL = "GLOBAL";

    private String adminLevel;
    private Integer cohortId;

    protected SuperAdmin() {
    }

    public boolean isGlobal() {
        return GLOBAL.equals(adminLevel);
    }

    /** Memastikan angkatan berada dalam wewenang SuperAdmin ini. */
    public void manageCohort(Integer targetCohortId) {
        if (!isGlobal() && !Objects.equals(cohortId, targetCohortId)) {
            throw new AccessDeniedException("Data ini berada di luar angkatan yang Anda kelola.");
        }
    }

    public void manageClass(ClassRoom classRoom) {
        manageCohort(classRoom.getCohortId());
    }

    public void manageAdmin(Admin admin) {
        manageUser(admin);
    }

    public void manageUser(User user) {
        if (user instanceof SuperAdmin && !isGlobal()) {
            throw new AccessDeniedException("Hanya SuperAdmin GLOBAL yang dapat mengelola SuperAdmin.");
        }
        if (user instanceof Student student) {
            manageCohort(student.getCohortId());
        }
    }

    @Override
    public List<Route> getMenu() {
        return List.of(Route.DASHBOARD, Route.COHORTS, Route.USERS, Route.CLASSES);
    }

    @Override
    public String getRoleLabel() {
        return "SuperAdmin";
    }

    @Override
    public String getDetailLine() {
        return isGlobal() ? "Tingkat global" : "Tingkat angkatan";
    }

    @Override
    public Route getSearchRoute() {
        return Route.USERS;
    }

    @Override
    public String getSearchPrompt() {
        return "Cari pengguna…";
    }

    public String getAdminLevel() {
        return adminLevel;
    }

    public Integer getCohortId() {
        return cohortId;
    }
}
