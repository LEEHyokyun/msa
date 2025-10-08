package kuke.board.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/*
* 인기글 집계를 위해 필요한 게시글의 댓글 수
* */
@Repository
@RequiredArgsConstructor
public class ArticleCommentCountRepository {
    private final StringRedisTemplate redisTemplate;

    // hot-article::article::{articleId}::comment-count
    private static final String KEY_FORMAT = "hot-article::article::%s::comment-count";

    /*
    * 인기글 선정에 필요한 "게시글에 대한" "comment count"를 Redis에 저장하기 위함
    * */
    public void createOrUpdate(Long articleId, Long commentCount, Duration ttl) {
        /*
        * 데이터가 없으면 신규 생성, 없으면 수정
        * 인기글 산정까지만 필요, 나머지는 제거
        * */
        redisTemplate.opsForValue().set(generateKey(articleId), String.valueOf(commentCount), ttl);
    }

    /*
    * article id를 전달받아 count 읽기
    * */
    public Long read(Long articleId) {
        /*
        * 데이터가 없으면 0, 있으면 result 반환
        * */
        String result = redisTemplate.opsForValue().get(generateKey(articleId));
        return result == null ? 0L : Long.valueOf(result);
    }

    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
