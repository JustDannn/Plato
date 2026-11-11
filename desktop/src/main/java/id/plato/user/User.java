package id.plato.user;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import id.plato.core.Route;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "role")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Student.class, name = "STUDENT"),
        @JsonSubTypes.Type(value = Admin.class, name = "ADMIN"),
        @JsonSubTypes.Type(value = SuperAdmin.class, name = "SUPERADMIN")
})
public abstract class User {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static UserGateway gateway;

    protected int userId;
    protected String name;
    protected String email;
    /** Hanya terisi selama proses autentikasi; tidak pernah dikirim balik dari server. */
    protected String password;
    protected LocalDateTime createdAt;
    protected boolean isActive = true;

    public static void useGateway(UserGateway userGateway) {
        gateway = userGateway;
    }

    /** Memeriksa kredensial akun ini tanpa membuat sesi baru. */
    public boolean login(String email, String password) {
        if (email == null || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Email dan password wajib diisi.");
        }
        this.password = password;
        try {
            return gateway.verifyCredentials(email, this.password);
        } finally {
            this.password = null;
        }
    }

    public void logout() {
        gateway.endSession();
    }

    public void updateProfile(String name, String email) {
        validateProfile(name, email);
        gateway.saveProfile(name.trim(), email.trim());
        this.name = name.trim();
        this.email = email.trim();
    }

    public static void validateProfile(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nama wajib diisi.");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Format email tidak valid.");
        }
    }

    /** Menu utama sesuai hak akses peran. */
    public abstract List<Route> getMenu();

    public abstract String getRoleLabel();

    public abstract String getDetailLine();

    public Route getSearchRoute() {
        return Route.TASKS;
    }

    public String getSearchPrompt() {
        return "Cari tugas atau mata kuliah…";
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return name.trim().split("\\s+")[0];
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return isActive;
    }
}
