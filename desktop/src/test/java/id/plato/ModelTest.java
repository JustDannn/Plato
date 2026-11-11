package id.plato;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.plato.core.AccessDeniedException;
import id.plato.core.ApiClient;
import id.plato.core.Route;
import id.plato.monitoring.Dashboard;
import id.plato.task.Milestone;
import id.plato.task.Task;
import id.plato.task.TaskStatus;
import id.plato.task.TaskType;
import id.plato.user.Admin;
import id.plato.user.Student;
import id.plato.user.SuperAdmin;
import id.plato.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {
    private static final ObjectMapper JSON = ApiClient.createMapper();
    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 20);

    private Task individual() {
        return new Task(1, "Laporan", "", TaskType.INDIVIDUAL, START, END);
    }

    private Task project() {
        return new Task(1, "Proyek", "", TaskType.PROJECT, START, END);
    }

    private Admin adminOf(int classId) throws Exception {
        return (Admin) JSON.readValue("{\"role\":\"ADMIN\",\"userId\":9,\"name\":\"A\",\"assignedClassId\":" + classId + "}", User.class);
    }

    private Task approvedFromJson(String deadline, int progress) throws Exception {
        return JSON.readValue("{\"taskId\":1,\"classId\":1,\"courseName\":\"PBO\",\"title\":\"T\",\"taskType\":\"INDIVIDUAL\",\"status\":\"APPROVED\","
                + "\"startDate\":\"2026-12-01\",\"deadline\":\"" + deadline + "\",\"progress\":" + progress + "}", Task.class);
    }

    @Test
    void deadlineBeforeStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Task(1, "X", "", TaskType.GROUP, END, START));
    }

    @Test
    void progressOutsideRangeIsRejected() {
        Task task = individual();
        assertThrows(IllegalArgumentException.class, () -> task.updateProgress(-1));
        assertThrows(IllegalArgumentException.class, () -> task.updateProgress(101));
        task.updateProgress(100);
        assertEquals(100, task.getProgress());
    }

    @Test
    void progressOfProjectIsAverageOfMilestones() {
        Task task = project();
        Milestone first = new Milestone("A", "", START, START.plusDays(3));
        Milestone second = new Milestone("B", "", START.plusDays(4), END);
        task.addMilestone(first);
        task.addMilestone(second);
        first.updateProgress(100);
        task.updateProgress(0);
        assertEquals(50, task.getProgress());
    }

    @Test
    void milestoneOutsideTaskRangeIsRejected() {
        Task task = project();
        assertThrows(IllegalArgumentException.class,
                () -> task.addMilestone(new Milestone("Jauh", "", START, END.plusDays(1))));
    }

    @Test
    void milestonesOnlyAllowedForProjects() {
        assertThrows(IllegalStateException.class,
                () -> individual().addMilestone(new Milestone("A", "", START, END)));
    }

    @Test
    void milestoneListIsReadOnly() {
        List<Milestone> view = project().getMilestones();
        assertThrows(UnsupportedOperationException.class, () -> view.add(new Milestone("A", "", START, END)));
    }

    @Test
    void submissionMovesThroughValidationStates() {
        Task task = individual();
        task.submitForApproval();
        assertEquals(TaskStatus.PENDING, task.getStatus());
        task.reject(9, "Tanggal keliru");
        assertEquals(TaskStatus.REJECTED, task.getStatus());
        assertEquals("Tanggal keliru", task.getRejectionReason());
        task.submitForApproval();
        task.approve(9);
        assertEquals(TaskStatus.APPROVED, task.getStatus());
        assertThrows(IllegalStateException.class, () -> task.approve(9));
    }

    @Test
    void rejectionRequiresReason() {
        Task task = individual();
        task.submitForApproval();
        assertThrows(IllegalArgumentException.class, () -> task.reject(9, "  "));
    }

    @Test
    void adminCannotValidateAnotherClass() throws Exception {
        Task task = individual();
        task.submitForApproval();
        assertThrows(AccessDeniedException.class, () -> adminOf(2).approveTask(task));
        adminOf(1).approveTask(task);
        assertEquals(TaskStatus.APPROVED, task.getStatus());
    }

    @Test
    void usersDeserializeToSubclassByRole() throws Exception {
        assertInstanceOf(Student.class, JSON.readValue("{\"role\":\"STUDENT\",\"name\":\"S\"}", User.class));
        assertInstanceOf(Admin.class, JSON.readValue("{\"role\":\"ADMIN\",\"name\":\"A\"}", User.class));
        assertInstanceOf(SuperAdmin.class, JSON.readValue("{\"role\":\"SUPERADMIN\",\"name\":\"X\"}", User.class));
    }

    @Test
    void menuDependsOnRole() throws Exception {
        User student = JSON.readValue("{\"role\":\"STUDENT\",\"name\":\"S\"}", User.class);
        User admin = JSON.readValue("{\"role\":\"ADMIN\",\"name\":\"A\"}", User.class);
        assertFalse(student.getMenu().contains(Route.VALIDATION));
        assertTrue(admin.getMenu().contains(Route.VALIDATION));
    }

    @Test
    void cohortSuperAdminIsScoped() throws Exception {
        SuperAdmin scoped = (SuperAdmin) JSON.readValue("{\"role\":\"SUPERADMIN\",\"adminLevel\":\"COHORT\",\"cohortId\":1}", User.class);
        SuperAdmin global = (SuperAdmin) JSON.readValue("{\"role\":\"SUPERADMIN\",\"adminLevel\":\"GLOBAL\"}", User.class);
        scoped.manageCohort(1);
        assertThrows(AccessDeniedException.class, () -> scoped.manageCohort(2));
        global.manageCohort(2);
    }

    @Test
    void dashboardIgnoresUnapprovedTasks() throws Exception {
        Task pending = individual();
        pending.submitForApproval();
        Dashboard dashboard = new Dashboard(List.of(pending, approvedFromJson("2026-12-10", 0)));
        assertEquals(1, dashboard.getTasks().size());
    }

    @Test
    void calendarGroupsByDeadlineInMonth() throws Exception {
        Dashboard dashboard = new Dashboard(List.of(approvedFromJson("2026-12-10", 0), approvedFromJson("2027-01-05", 0)));
        var calendar = dashboard.displayCalendar(YearMonth.of(2026, 12));
        assertEquals(1, calendar.size());
        assertEquals(1, calendar.get(LocalDate.of(2026, 12, 10)).size());
    }

    @Test
    void filterKeepsTasksOverlappingPeriod() throws Exception {
        Dashboard dashboard = new Dashboard(List.of(approvedFromJson("2026-12-10", 0)));
        assertEquals(1, dashboard.filterByDate(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 6)).size());
        assertEquals(0, dashboard.filterByDate(LocalDate.of(2026, 12, 11), null).size());
    }

    @Test
    void averageAndCourseProgress() throws Exception {
        List<Task> tasks = List.of(approvedFromJson("2026-12-10", 40), approvedFromJson("2026-12-11", 60));
        Dashboard dashboard = new Dashboard(tasks);
        assertEquals(50, dashboard.averageProgress(tasks));
        assertEquals(50, dashboard.progressByCourse(tasks).get("PBO"));
    }
}
