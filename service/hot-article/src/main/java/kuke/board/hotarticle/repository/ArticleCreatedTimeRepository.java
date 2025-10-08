package kuke.board.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/*
* article 생성 시간 저장
* */
@Repository
@RequiredArgsConstructor
public class ArticleCreatedTimeRepository {
    private final StringRedisTemplate redisTemplate;

    // hot-article::article::{articleId}::created-time
    private static final String KEY_FORMAT = "hot-article::article::%s::created-time";

    /*
    * 게시글의 생성시간 같이 저장 :
    * - 게시글 댓글이나 좋아요, 조회 수등의 이벤트가 발생하였을때 인기글을 반드시 집계해야 하는가?
    * - 결국 내일 보여줄 인기글을 보여주기위해 "오늘" 인기글 집계가 필요하다면
    * - 오늘 00시 부터 내일 00시까지의 글에 대해서만 집계하면 됨
    * - 나머지는 집계할 필요가 없다.
    * - 이를 위해 게시글 생성시간을 파악하는데, 별도 API 호출없이 Redis에서 바로 데이터를 가져오기 위함
    * */
    public void createOrUpdate(Long articleId, LocalDateTime createdAt, Duration ttl) {
        redisTemplate.opsForValue().set(
                generateKey(articleId),
                String.valueOf(createdAt.toInstant(ZoneOffset.UTC).toEpochMilli()),
                ttl
        );
    }

    /*
    * 삭제
    * */
    public void delete(Long articleId) {
        redisTemplate.delete(generateKey(articleId));
    }

    /*
    * 조회
    * */
    public LocalDateTime read(Long articleId) {
        String result = redisTemplate.opsForValue().get(generateKey(articleId));
        if (result == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(Long.valueOf(result)), ZoneOffset.UTC
        );
    }

    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
