package kuke.board.articleread.cache;

import kuke.board.common.dataserializer.DataSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
public class OptimizedCacheManager {
    private final StringRedisTemplate redisTemplate;
    private final OptimizedCacheLockProvider optimizedCacheLockProvider;

    private static final String DELIMITER = "::";

    /*
    * 캐싱처리 정의
    * */
    /*
    * 참고
    * Class<?> : returnType이라는 타입객체 사용시 컴파일 시점에 그대로 사용 가능
    * CLass<T> : returnType 타입객체의 형태를 컴파일 시점에 반드시 특정하거나 캐스팅해주어야 사용 가능
    * */
    public Object process(String type, long ttlSeconds, Object[] args, Class<?> returnType,
                          OptimizedCacheOriginDataSupplier<?> originDataSupplier) throws Throwable {
        /*
        * cache가 어떤 type인가? 그 type의 indicator = type
        * 컴파일시점에 강제할 필요가 없으며, 범용적 처리 로직을 생성하고자 Class<?> 처리.
        * */
        String key = generateKey(type, args);

        /*
        * key에 대한 캐싱객체가 존재하지 않는다면 refresh(갱신)
        * 지금의 요청이 첫 요청이므로 바로 캐싱에 적재해주면 된다.
        * */
        String cachedData = redisTemplate.opsForValue().get(key);
        if (cachedData == null) {
            /*
            * key가 없다는 것은 캐싱적재가 필요하다는 의미,
            * 원본데이터를 요청해서 다시 캐시에 적재하는 것
            * */
            return refresh(originDataSupplier, key, ttlSeconds);
        }

        /*
        * 캐싱객체가 존재한다면 역직렬화하여 받아온다.
        * 이 요청이 첫번째 요청이 아님.
        * */
        OptimizedCache optimizedCache = DataSerializer.deserialize(cachedData, OptimizedCache.class);
        if (optimizedCache == null) {
            /*
            * 역직렬화 안되었을 경우 대비 : 재역직렬화
            * */
            return refresh(originDataSupplier, key, ttlSeconds);
        }

        /*
        * expiredAt = "언제" 캐시데이터가 만료되는가 그 시각에 대한 "특정"
        * 현재시각 기준 + 5초 넘기지 않았다면 아직 physical TTL 이전으로
        * 캐싱객체 존재..parse해서 받아온다.
        * 다만 logcial을 넘겼다면 (이 아래) 데이터 갱신 필요하다.
        * */
        if (!optimizedCache.isExpired()) {
            return optimizedCache.parseData(returnType);
        }

        /*
        * 이 이후의 로직은 만료 이후의 시점..락 획득을 하고 캐싱 적재, 실패 시 그냥 기존 캐싱객체 반환
        * 캐싱객체가 있지만 락획득실패 시 받아온 캐싱객체 그대로 반환
        * */
        if (!optimizedCacheLockProvider.lock(key)) {
            return optimizedCache.parseData(returnType);
        }
        
        /*
        * 락획득성공 -> 캐싱갱신 및 적재
        * 최종적으로 락 해제
        * */
        try {
            return refresh(originDataSupplier, key, ttlSeconds);
        } finally {
            optimizedCacheLockProvider.unlock(key);
        }
    }

    /*
    * 캐싱 갱신이 필요할 경우
    * 원본 데이터 객체를 가져와서 캐싱적재.
    * */
    private Object refresh(OptimizedCacheOriginDataSupplier<?> originDataSupplier, String key, long ttlSeconds) throws Throwable {
        Object result = originDataSupplier.get();

        /*
        * 전달 매개변수는 logical TTL
        * 실제 redis에 반영하는 physical TTL은 위에서 정의한 logical TTL + 5sec.
        * */
        OptimizedCacheTTL optimizedCacheTTL = OptimizedCacheTTL.of(ttlSeconds);
        OptimizedCache optimizedCache = OptimizedCache.of(result, optimizedCacheTTL.getLogicalTTL());

        redisTemplate.opsForValue()
                .set(
                        key,
                        DataSerializer.serialize(optimizedCache),
                        optimizedCacheTTL.getPhysicalTTL()
                );

        return result;
    }

    /*
    * redis에 저장하기 위한 key 생성
    * */
    private String generateKey(String prefix, Object[] args) {
        /*
        * prefix = 'prefix'
        * args = ['key0', 'key1'];
        * prefix::key0::key1
        * */
        return prefix + DELIMITER +
                Arrays.stream(args)
                        .map(String::valueOf)
                        /*
                        * Stream 각각의 요소를 ::로 concat하면서 이어붙여 collect(String)
                        * */
                        .collect(joining(DELIMITER));
    }

}
