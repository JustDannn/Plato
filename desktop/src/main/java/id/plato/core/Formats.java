package id.plato.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class Formats {
    public static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", ID);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", ID);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", ID);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", ID);
    private static final DateTimeFormatter MONTH_SHORT = DateTimeFormatter.ofPattern("MMM", ID);

    private Formats() {
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : DATE.format(date);
    }

    public static String shortDate(LocalDate date) {
        return SHORT.format(date);
    }

    public static String dateTime(LocalDateTime time) {
        return time == null ? "-" : DATE_TIME.format(time);
    }

    public static String month(LocalDate date) {
        return MONTH.format(date);
    }

    public static String monthShort(LocalDate date) {
        return MONTH_SHORT.format(date);
    }

    public static String dueIn(LocalDate deadline) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        if (days == 0) {
            return "Hari ini";
        }
        if (days == 1) {
            return "Besok";
        }
        return days > 0 ? days + " hari lagi" : "Lewat " + -days + " hari";
    }
}
