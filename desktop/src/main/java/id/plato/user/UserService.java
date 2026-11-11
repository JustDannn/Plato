package id.plato.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import id.plato.core.ApiClient;

import java.util.List;
import java.util.Map;

public class UserService implements UserGateway {
    private final ApiClient api = ApiClient.get();

    public record LoginResult(@JsonProperty("token") String token, @JsonProperty("user") User user) {
    }

    private record TokenResult(@JsonProperty("token") String token) {
    }

    private record VerifyResult(@JsonProperty("valid") boolean valid) {
    }

    public LoginResult signIn(String email, String password) {
        return api.post("/auth/login", Map.of("email", email, "password", password), LoginResult.class);
    }

    @Override
    public boolean verifyCredentials(String email, String password) {
        return api.post("/auth/verify", Map.of("email", email, "password", password), VerifyResult.class).valid();
    }

    @Override
    public void endSession() {
        api.post("/auth/logout", Map.of());
    }

    @Override
    public void saveProfile(String name, String email) {
        api.put("/users/me", Map.of("name", name, "email", email), User.class);
    }

    public void changePassword(String current, String next) {
        String token = api.put("/users/me/password",
                Map.of("currentPassword", current, "newPassword", next), TokenResult.class).token();
        api.setToken(token);
    }

    public List<User> list(String search) {
        return api.get("/users?" + ApiClient.query("q", search), new TypeReference<>() { });
    }

    public User create(Map<String, Object> body) {
        return api.post("/users", body, User.class);
    }

    public User update(int id, Map<String, Object> body) {
        return api.put("/users/" + id, body, User.class);
    }

    public void delete(int id) {
        api.delete("/users/" + id);
    }

    public List<Cohort> cohorts() {
        return api.get("/cohorts", new TypeReference<>() { });
    }

    public Cohort saveCohort(Integer id, String name, int year) {
        Map<String, Object> body = Map.of("name", name, "year", year);
        return id == null ? api.post("/cohorts", body, Cohort.class) : api.put("/cohorts/" + id, body, Cohort.class);
    }

    public void deleteCohort(int id) {
        api.delete("/cohorts/" + id);
    }
}
