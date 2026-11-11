package id.plato.classroom;

import id.plato.task.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassRoom {
    private int classId;
    private int cohortId;
    private String cohortName;
    private String className;
    private String courseName;
    private String description;
    private Integer adminId;
    private String adminName;
    private int memberCount;
    private int taskCount;

    /** Aggregation: tugas tetap memiliki identitas sendiri. */
    private final List<Task> taskList = new ArrayList<>();
    private final List<Membership> members = new ArrayList<>();

    protected ClassRoom() {
    }

    public void setTasks(List<Task> tasks) {
        taskList.clear();
        tasks.forEach(this::addTask);
    }

    public void addTask(Task task) {
        if (task.getClassId() != classId) {
            throw new IllegalArgumentException("Tugas bukan milik kelas ini.");
        }
        taskList.add(task);
    }

    public void setMembers(List<Membership> memberships) {
        members.clear();
        members.addAll(memberships);
    }

    public List<Task> getTaskList() {
        return Collections.unmodifiableList(taskList);
    }

    public List<Membership> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public boolean hasActiveMember(int studentUserId) {
        return members.stream().anyMatch(m -> m.getUserId() == studentUserId && m.isActive());
    }

    public static void validate(String className, String courseName) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("Nama kelas wajib diisi.");
        }
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Nama mata kuliah wajib diisi.");
        }
    }

    public int getClassId() {
        return classId;
    }

    public int getCohortId() {
        return cohortId;
    }

    public String getCohortName() {
        return cohortName;
    }

    public String getClassName() {
        return className;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getDescription() {
        return description;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public String getAdminName() {
        return adminName;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public int getTaskCount() {
        return taskCount;
    }

    @Override
    public String toString() {
        return courseName + " (" + className + ")";
    }
}
