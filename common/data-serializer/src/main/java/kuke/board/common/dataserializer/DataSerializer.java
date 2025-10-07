package kuke.board.common.dataserializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataSerializer {
    /*
    * Kafka와 통신(이벤트를 주고 받을때) 시 필요한 util Module.
    * (*Kafka로 전달할때 Json 직렬화 및 데이터 받아올때 역직렬화 필요).
    * */
    private static final ObjectMapper objectMapper = initialize();

    /*
    * Object Mapper
    * - 시간 관련 직렬화 : Java Time Module
    * - 역직렬화 시 정하지 않은 세팅 있을 경우 오류가 나지 않도록 설정
    * */
    private static ObjectMapper initialize() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /*
    * 데이터 역직렬화
    * String type -> class
    * */
    public static <T> T deserialize(String data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (JsonProcessingException e) {
            /*
            * error 발생 시 null 반환
            * */
            log.error("[DataSerializer.deserialize] data={}, clazz={}", data, clazz, e);
            return null;
        }
    }

    /*
    * 데이터 역직렬화
    * Object -> Type
    * */
    public static <T> T deserialize(Object data, Class<T> clazz) {
        return objectMapper.convertValue(data, clazz);
    }

    /*
    * 직렬화
    * Object -> Json
    * */
    public static String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            /*
             * error 발생 시 null 반환
             * */
            log.error("[DataSerializer.serialize] object={}", object, e);
            return null;
        }
    }
}
