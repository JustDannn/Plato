package id.plato.task;

import com.fasterxml.jackson.core.type.TypeReference;
import id.plato.core.ApiClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskService {
    private final ApiClient api = ApiClient.get();

    public List<Task> list(Integer classId, TaskStatus status, LocalDate from, LocalDate to, String search) {
        String query = ApiClient.query("classId", classId)
                + ApiClient.query("status", status == null ? null : status.name())
                + ApiClient.query("from", from)
                + ApiClient.query("to", to)
                + ApiClient.query("q", search == null || search.isBlank() ? null : search);
        return api.get("/tasks?" + query, new TypeReference<>() { });
    }

    public List<Task> all() {
        return list(null, null, null, null, null);
    }

    public List<Task> approved() {
        return list(null, TaskStatus.APPROVED, null, null, null);
    }

    public List<Task> pending() {
        return list(null, TaskStatus.PENDING, null, null, null);
    }

    public Task find(int id) {
        return api.get("/tasks/" + id, Task.class);
    }

    public Task create(Task task) {
        Map<String, Object> body = taskBody(task);
        body.put("classId", task.getClassId());
        body.put("milestones", task.getMilestones().stream().map(this::milestoneBody).toList());
        return api.post("/tasks", body, Task.class);
    }

    public Task update(int id, Task task) {
        return api.put("/tasks/" + id, taskBody(task), Task.class);
    }

    public void delete(int id) {
        api.delete("/tasks/" + id);
    }

    public Task approve(int id) {
        return api.post("/tasks/" + id + "/approve", Map.of(), Task.class);
    }

    public Task reject(int id, String reason) {
        return api.post("/tasks/" + id + "/reject", Map.of("reason", reason), Task.class);
    }

    public Task updateProgress(int taskId, int value) {
        return api.put("/tasks/" + taskId + "/progress", Map.of("progress", value), Task.class);
    }

    public Task updateMilestoneProgress(int milestoneId, int value) {
        return api.put("/milestones/" + milestoneId + "/progress", Map.of("progress", value), Task.class);
    }

    public Task addMilestone(int taskId, Milestone milestone) {
        return api.post("/tasks/" + taskId + "/milestones", milestoneBody(milestone), Task.class);
    }

    public void deleteMilestone(int milestoneId) {
        api.delete("/milestones/" + milestoneId);
    }

    private Map<String, Object> taskBody(Task task) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", task.getTitle());
        body.put("description", task.getDescription());
        body.put("taskType", task.getTaskType().name());
        body.put("startDate", task.getStartDate().toString());
        body.put("deadline", task.getDeadline().toString());
        return body;
    }

    private Map<String, Object> milestoneBody(Milestone milestone) {
        return Map.of(
                "title", milestone.getTitle(),
                "description", milestone.getDescription(),
                "startDate", milestone.getStartDate().toString(),
                "deadline", milestone.getDeadline().toString());
    }
}
