package bio.overture.score.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Map;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> toValue) {
        return fromJson(mapper.readTree(json), toValue);
    }

    public static <T> T fromJson(JsonNode json, Class<T> toValue) {
        return mapper.convertValue(json, toValue);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json)
            throws IllegalArgumentException, IOException {
        return fromJson(json, Map.class);
    }

    @SneakyThrows
    public static String toJson(Object o) {
        return mapper.writeValueAsString(o);
    }
}
