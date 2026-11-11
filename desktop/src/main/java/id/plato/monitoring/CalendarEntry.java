package id.plato.monitoring;

import id.plato.task.Task;

import java.time.LocalDate;

/** Satu tenggat tugas atau milestone pada kalender. */
public record CalendarEntry(LocalDate date, String title, boolean milestone, Task task) {
}
