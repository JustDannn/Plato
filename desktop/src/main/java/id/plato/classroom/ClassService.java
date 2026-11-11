package id.plato.classroom;

import com.fasterxml.jackson.core.type.TypeReference;
import id.plato.core.ApiClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassService {
    private final ApiClient api = ApiClient.get();

    public List<ClassRoom> myClasses() {
        return api.get("/classes", new TypeReference<>() { });
    }

    public List<ClassRoom> available() {
        return api.get("/classes/available", new TypeReference<>() { });
    }

    public ClassRoom find(int id) {
        return api.get("/classes/" + id, ClassRoom.class);
    }

    public ClassRoom save(Integer id, Integer cohortId, String className, String courseName, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("className", className);
        body.put("courseName", courseName);
        body.put("description", description);
        if (cohortId != null) {
            body.put("cohortId", cohortId);
        }
        return id == null ? api.post("/classes", body, ClassRoom.class) : api.put("/classes/" + id, body, ClassRoom.class);
    }

    public void delete(int id) {
        api.delete("/classes/" + id);
    }

    public void join(int id) {
        api.post("/classes/" + id + "/join", Map.of());
    }

    public List<Membership> members(int id) {
        return api.get("/classes/" + id + "/members", new TypeReference<>() { });
    }

    public void addMember(int id, String email) {
        api.post("/classes/" + id + "/members", Map.of("email", email));
    }

    public void setMemberStatus(int classId, int membershipId, String status) {
        api.patch("/classes/" + classId + "/members/" + membershipId, Map.of("status", status));
    }
}
