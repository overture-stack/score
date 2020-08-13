package bio.overture.score.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Map;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

//    public static ObjectMapper mapper() {
//        val specialModule = new SimpleModule();
//        val mapper = new ObjectMapper().registerModule(specialModule);
//
//        mapper.disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
//        mapper.disable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS);
//        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//        mapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
//
//        // Doesn't work! Fields with the value '""' (empty string) are not being deserialized as null.
//        // mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
//
//        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
//        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
//        return mapper;
//    }

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
