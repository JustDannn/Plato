package id.plato.monitoring;

import id.plato.task.Milestone;
import id.plato.task.Task;
import id.plato.task.TaskStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Mengolah data Task dan Milestone untuk tampilan, tanpa memilikinya (dependency). */
public class Dashboard {
    private final List<Task> tasks;

    public Dashboard(List<Task> allTasks) {
        this.tasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.APPROVED).toList();
    }

    public List<Task> getTasks() {
        return tasks;
    }

    /** Tenggat tugas dan milestone pada bulan tersebut, dikelompokkan per tanggal. */
    public Map<LocalDate, List<CalendarEntry>> displayCalendar(YearMonth month) {
        List<CalendarEntry> entries = new ArrayList<>();
        for (Task task : tasks) {
            entries.add(new CalendarEntry(task.getDeadline(), task.getTitle(), false, task));
            for (Milestone milestone : task.getMilestones()) {
                entries.add(new CalendarEntry(milestone.getDeadline(), milestone.getTitle(), true, task));
            }
        }
        return entries.stream()
                .filter(entry -> YearMonth.from(entry.date()).equals(month))
                .collect(Collectors.groupingBy(CalendarEntry::date, TreeMap::new, Collectors.toList()));
    }

    /** Tugas satu kelas, urut dari tanggal mulai. */
    public List<Task> displayGanttChart(int classId) {
        return tasks.stream()
                .filter(t -> t.getClassId() == classId)
                .sorted(Comparator.comparing(Task::getStartDate).thenComparing(Task::getDeadline))
                .toList();
    }

    /** Tugas yang rentang pengerjaannya beririsan dengan periode. Batas null berarti terbuka. */
    public List<Task> filterByDate(LocalDate from, LocalDate to) {
        return tasks.stream()
                .filter(t -> from == null || !t.getDeadline().isBefore(from))
                .filter(t -> to == null || !t.getStartDate().isAfter(to))
                .toList();
    }

    public List<Task> getUpcomingTasks() {
        LocalDate today = LocalDate.now();
        return tasks.stream()
                .filter(t -> !t.isDone() && !t.getDeadline().isBefore(today))
                .sorted(Comparator.comparing(Task::getDeadline))
                .toList();
    }

    public long countDueWithin(int days) {
        LocalDate limit = LocalDate.now().plusDays(days);
        return getUpcomingTasks().stream().filter(t -> !t.getDeadline().isAfter(limit)).count();
    }

    public long countDone(List<Task> scope) {
        return scope.stream().filter(Task::isDone).count();
    }

    public int averageProgress(List<Task> scope) {
        return (int) Math.round(scope.stream().mapToInt(Task::getProgress).average().orElse(0));
    }

    /** Rata-rata progres per mata kuliah. */
    public Map<String, Integer> progressByCourse(List<Task> scope) {
        Map<String, List<Task>> grouped = scope.stream()
                .collect(Collectors.groupingBy(Task::getCourseName, TreeMap::new, Collectors.toList()));
        Map<String, Integer> result = new LinkedHashMap<>();
        grouped.forEach((course, items) -> result.put(course, averageProgress(items)));
        return result;
    }

    /** Jumlah tenggat per bulan, berakhir pada bulan terakhir yang diminta. */
    public Map<YearMonth, Integer> deadlinesPerMonth(YearMonth last, int months) {
        Map<YearMonth, Integer> result = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = last.minusMonths(i);
            result.put(month, (int) tasks.stream().filter(t -> YearMonth.from(t.getDeadline()).equals(month)).count());
        }
        return result;
    }

    /** Tenggat per hari: baris = minggu mulai minggu berjalan, kolom = Senin sampai Minggu. */
    public int[][] weeklyLoad(LocalDate reference, int weeks) {
        int[][] load = new int[weeks][7];
        LocalDate monday = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (Task task : tasks) {
            long offset = ChronoUnit.DAYS.between(monday, task.getDeadline());
            if (offset >= 0 && offset < weeks * 7L) {
                load[(int) (offset / 7)][(int) (offset % 7)]++;
            }
        }
        return load;
    }
}
