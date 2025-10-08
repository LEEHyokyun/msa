package kuke.board.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/*
 * 인기글 집계를 위해 필요한 게시글의 조회 수
 * */
@Repository
@RequiredArgsConstructor
public class ArticleViewCountRepository {
    private final StringRedisTemplate redisTemplate;

    // hot-article::article::{articleId}::view-count
    private static final String KEY_FORMAT = "hot-article::article::{articleId}::view-count";

    /*
     * 인기글 선정에 필요한 "게시글에 대한" "view count"를 Redis에 저장하기 위함
     * */
    public void createOrUpdate(Long articleId, Long viewCount, Duration ttl) {
        redisTemplate.opsForValue().set(generateKey(articleId), String.valueOf(viewCount), ttl);
    }

    /*
     * article id를 전달받아 count 읽기
     * */
    public Long read(Long articleId) {
        String result = redisTemplate.opsForValue().get(generateKey(articleId));
        return result == null ? 0L : Long.valueOf(result);
    }

    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
