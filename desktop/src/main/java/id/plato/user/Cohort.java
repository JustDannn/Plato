package id.plato.user;

/** Data angkatan yang dikelola SuperAdmin. */
public class Cohort {
    private int id;
    private String name;
    private int year;
    private int studentCount;
    private int classCount;

    protected Cohort() {
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getYear() {
        return year;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public int getClassCount() {
        return classCount;
    }

    @Override
    public String toString() {
        return name;
    }
}
