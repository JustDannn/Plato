package id.plato.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import id.plato.core.ApiClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringService {
    private final ApiClient api = ApiClient.get();

    private record Count(@JsonProperty("count") int count) {
    }

    public record TrendPoint(@JsonProperty("weekStart") LocalDate weekStart, @JsonProperty("total") int total) {
    }

    /** Field angka terisi sesuai peran pengguna. */
    public record Summary(
            @JsonProperty("classCount") int classCount,
            @JsonProperty("pendingCount") int pendingCount,
            @JsonProperty("memberCount") int memberCount,
            @JsonProperty("cohortCount") int cohortCount,
            @JsonProperty("studentCount") int studentCount,
            @JsonProperty("adminCount") int adminCount,
            @JsonProperty("completionTrend") List<TrendPoint> completionTrend) {
    }

    public List<Notification> notifications() {
        return api.get("/notifications", new TypeReference<>() { });
    }

    public int unreadCount() {
        return api.get("/notifications/unread-count", Count.class).count();
    }

    public void markRead(int id) {
        api.patch("/notifications/" + id + "/read", Map.of());
    }

    public void markAllRead() {
        api.post("/notifications/read-all", Map.of());
    }

    public void sendToClass(int classId, Notification notification) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", notification.getMessage());
        body.put("taskId", notification.getTaskId());
        api.post("/classes/" + classId + "/notifications", body);
    }

    public Summary summary() {
        return api.get("/dashboard/summary", Summary.class);
    }
}
