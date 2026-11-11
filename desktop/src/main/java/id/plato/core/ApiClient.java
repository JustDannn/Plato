package id.plato.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Klien REST tunggal yang dipakai seluruh service. */
public final class ApiClient {
    private static final ApiClient INSTANCE = new ApiClient();
    private static final ObjectMapper MAPPER = createMapper();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String baseUrl = resolveBaseUrl();
    private volatile String token;

    private ApiClient() {
    }

    public static ApiClient get() {
        return INSTANCE;
    }

    /** Model memakai field privat, sehingga Jackson membaca field tanpa perlu setter publik. */
    public static ObjectMapper createMapper() {
        SimpleModule utcTimes = new SimpleModule();
        utcTimes.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
                return LocalDateTime.ofInstant(Instant.parse(parser.getText()), ZoneId.systemDefault());
            }
        });
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(utcTimes)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    private static String resolveBaseUrl() {
        String configured = System.getProperty("plato.api.url", System.getenv("PLATO_API_URL"));
        return configured != null ? configured : "http://localhost:8080/api/v1";
    }

    public void setToken(String token) {
        this.token = token;
    }

    public static String query(String key, Object value) {
        return value == null ? "" : "&" + key + "=" + URLEncoder.encode(value.toString(), StandardCharsets.UTF_8);
    }

    public <T> T get(String path, Class<T> type) {
        return read(send("GET", path, null), MAPPER.constructType(type));
    }

    public <T> T get(String path, TypeReference<T> type) {
        return read(send("GET", path, null), MAPPER.constructType(type));
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return read(send("POST", path, body), MAPPER.constructType(type));
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return read(send("PUT", path, body), MAPPER.constructType(type));
    }

    public void post(String path, Object body) {
        send("POST", path, body);
    }

    public void patch(String path, Object body) {
        send("PATCH", path, body);
    }

    public void delete(String path) {
        send("DELETE", path, null);
    }

    private <T> T read(String body, JavaType type) {
        try {
            return MAPPER.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new ApiException(500, "Respons server tidak dapat dibaca.");
        }
    }

    private String send(String method, String path, Object body) {
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body));
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .method(method, publisher);
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ApiException(response.statusCode(), errorMessage(response.body()));
            }
            return response.body();
        } catch (IOException e) {
            throw new ApiException(0, "Tidak dapat terhubung ke server. Pastikan Docker berjalan (docker compose up -d).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Permintaan dibatalkan.");
        }
    }

    private String errorMessage(String body) {
        try {
            JsonNode error = MAPPER.readTree(body).get("error");
            if (error != null) {
                return error.asText();
            }
        } catch (JsonProcessingException ignored) {
            // Respons non-JSON memakai pesan umum di bawah.
        }
        return "Terjadi kesalahan pada server.";
    }
}
