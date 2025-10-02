package kuke.board.view.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/*
* Redis를 통한 조회수 처리 위한 Repository "Class"
* 실제 JPA Repository를 사용하는 것이 아닌 RedisTemplate 활용
* */
@Repository
@RequiredArgsConstructor
public class ArticleViewCountRepository {
    /*
    * Redis 의존성 주입
    * = Redis와의 통신
    * */
    private final StringRedisTemplate redisTemplate;

    /*
    * key , value
    * view::article::{article_id}::view_count , article_id에 대한 조회수
    * */
    // view::article::{article_id}::view_count
    private static final String KEY_FORMAT = "view::article::%s::view_count";

    /*
    * key를 활용하여 조회수 읽기
    * 최초 return = String
    * */
    public Long read(Long articleId) {
        String result = redisTemplate.opsForValue().get(generateKey(articleId));
        return result == null ? 0L : Long.valueOf(result);
    }

    /*
    * 조회수 증가하기
    * Value 추출(opsForValue) 그 후 증가(increase)
    * */
    public Long increase(Long articleId) {
        return redisTemplate.opsForValue().increment(generateKey(articleId));
    }

    /*
    * key 생성(String에서의 formatted / template literal 활용!)
    * */
    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
