package id.plato.classroom;

import java.time.LocalDateTime;

public class Membership {
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private int membershipId;
    private int userId;
    private int classId;
    private String name;
    private String email;
    private String studentId;
    private LocalDateTime joinedAt;
    private String status;

    protected Membership() {
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    /** Status berikutnya bila keanggotaan dialihkan. */
    public String toggledStatus() {
        return isActive() ? INACTIVE : ACTIVE;
    }

    public int getMembershipId() {
        return membershipId;
    }

    public int getUserId() {
        return userId;
    }

    public int getClassId() {
        return classId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getStudentId() {
        return studentId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public String getStatus() {
        return status;
    }
}
