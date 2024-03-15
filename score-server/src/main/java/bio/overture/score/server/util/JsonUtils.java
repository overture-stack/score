package bio.overture.score.server.util;

import static com.google.common.base.Strings.emptyToNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.SneakyThrows;
import lombok.val;

/** Utility functions related to deal with JSON */
public class JsonUtils {

  protected static final ObjectMapper mapper = mapper();

  public static ObjectMapper mapper() {
    val mapper = new ObjectMapper();

    /* Register Modules */
    val specialModule = new SimpleModule();
    specialModule.addDeserializer(String.class, SpecialStringJsonDeserializer.instance);

    val isoDateFormatter = DateTimeFormatter.ISO_DATE_TIME;
    val dateTimeDeserializer = new LocalDateTimeDeserializer(isoDateFormatter);
    val dateTimeSerializer = new LocalDateTimeSerializer(isoDateFormatter);

    val javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(LocalDateTime.class, dateTimeDeserializer);
    javaTimeModule.addSerializer(LocalDateTime.class, dateTimeSerializer);

    mapper.registerModule(specialModule);
    mapper.registerModule(javaTimeModule);

    mapper.disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
    mapper.disable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

    // Doesn't work! Fields with the value '""' (empty string) are not being deserialized as null.
    // mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    return mapper;
  }

  @SneakyThrows
  public static JsonNode read(URL url) {
    return mapper.readTree(url);
  }

  public static <T> T convertValue(Object fromValue, Class<T> toValue) {
    return mapper().convertValue(fromValue, toValue);
  }

  /**
   * Since the ACCEPT_EMPTY_STRING_AS_NULL_OBJECT DeserializationFeature is not working properly,
   * created custom string deserialization handling of empty string.
   */
  public static class SpecialStringJsonDeserializer extends StdDeserializer<String> {
    public static final SpecialStringJsonDeserializer instance =
        new SpecialStringJsonDeserializer();

    public SpecialStringJsonDeserializer() {
      super(String.class);
    }

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      val result = StringDeserializer.instance.deserialize(jsonParser, deserializationContext);
      return emptyToNull(result);
    }
  }
}
