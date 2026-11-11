package id.plato.user;

import id.plato.core.Route;
import id.plato.monitoring.CalendarEntry;
import id.plato.monitoring.Dashboard;
import id.plato.task.Task;
import id.plato.task.Trackable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class Student extends User {
    private String studentId;
    private Integer cohortId;
    private String studyProgram;
    private int semester;

    protected Student() {
    }

    /** Menandai tugas sebagai pengajuan milik mahasiswa ini. */
    public void submitTask(Task task) {
        task.assignCreator(userId);
        task.submitForApproval();
    }

    public List<Task> viewTask(Dashboard dashboard, LocalDate from, LocalDate to) {
        return dashboard.filterByDate(from, to);
    }

    public Map<LocalDate, List<CalendarEntry>> viewCalendar(Dashboard dashboard, YearMonth month) {
        return dashboard.displayCalendar(month);
    }

    public List<Task> viewGanttChart(Dashboard dashboard, int classId) {
        return dashboard.displayGanttChart(classId);
    }

    /** Pintu masuk pembaruan progres; perilakunya bergantung pada objek Task atau Milestone. */
    public void updateProgress(Trackable item, int value) {
        item.updateProgress(value);
    }

    @Override
    public List<Route> getMenu() {
        return List.of(Route.DASHBOARD, Route.CLASSES, Route.TASKS, Route.CALENDAR, Route.GANTT, Route.ANALYTICS);
    }

    @Override
    public String getRoleLabel() {
        return "Mahasiswa";
    }

    @Override
    public String getDetailLine() {
        return studentId + " · " + studyProgram + " · Semester " + semester;
    }

    public String getStudentId() {
        return studentId;
    }

    public Integer getCohortId() {
        return cohortId;
    }

    public String getStudyProgram() {
        return studyProgram;
    }

    public int getSemester() {
        return semester;
    }
}
