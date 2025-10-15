package kuke.board.articleread.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import kuke.board.common.dataserializer.DataSerializer;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.time.LocalDateTime;

/*
* data를 Redis에 저장하기 위한,
* 일종의 Wrapper 클래스.
* */
/*
* 참고 : Getter -> 멤버변수를 getter 메서드화 할 뿐이고, 내부 메서드는 그대로 사용하는 것
* 이때 멤버변수를 getter 메서드화할 경우 getter 프로퍼티에 맞게 변형한다.
* */
@Getter
@ToString
public class OptimizedCache {
    /*
    * Redis에 저장할 문자열 데이터
    * */
    private String data;
    /*
    * Logical TTL
    * 실제로는 Time API와 TTL을 더해서 만료시각을 저장
    * */
    private LocalDateTime expiredAt;

    public static OptimizedCache of(Object data, Duration ttl) {
        OptimizedCache optimizedCache = new OptimizedCache();
        optimizedCache.data = DataSerializer.serialize(data);
        //ttl만큼 유효..현재시각 + logical ttl = application에서 바라보는 expiredAt.
        optimizedCache.expiredAt = LocalDateTime.now().plus(ttl);
        return optimizedCache;
    }

    /*
    * logical TTL 지났는지 확인
    * Jackson 직렬화할때 is메서드로 getter 프로퍼티에 해당하여 직렬화대상,
    * 하지만 객체화하기위한 조건 메서드로 사용하므로 Redis에는 필요없기에, JsonIgnore하여 직렬화대상에서 제외한다.
    * */
    @JsonIgnore
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    /*
    * 데이터를 특정 dataType에 맞게, 객체형태의 데이터를 역직렬화.
    * */
    public <T> T parseData(Class<T> dataType) {
        return DataSerializer.deserialize(data, dataType);
    }
}
