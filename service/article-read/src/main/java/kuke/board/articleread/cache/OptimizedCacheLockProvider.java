package kuke.board.articleread.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/*
* 캐시갱신 요청에 대해 분산락을 획득시도,
* 획득에 성공한 요청에 한해 캐시적재를 가능하도록 한다
* = 캐시 중복적재를 방지
* */
@Component
@RequiredArgsConstructor
public class OptimizedCacheLockProvider {
    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "optimized-cache-lock::"; //key prefix
    private static final Duration LOCK_TTL = Duration.ofSeconds(3); //TTL

    /*
    * 단순히 key가 있는지만 확인, 없을 경우 획득(=TTL 3sec, Redis String)
    * 있을 경우 선점 불가(=false)
    * */
    public boolean lock(String key) {
        return redisTemplate.opsForValue().setIfAbsent(
                generateLockKey(key),
                "",
                LOCK_TTL
        );
    }

    /*
    * 분산락 제거(delete)
    * */
    public void unlock(String key) {
        redisTemplate.delete(generateLockKey(key));
    }

    private String generateLockKey(String key) {
        return KEY_PREFIX + key;
    }
}
