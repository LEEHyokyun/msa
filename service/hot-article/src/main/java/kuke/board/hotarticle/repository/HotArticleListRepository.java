package kuke.board.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HotArticleListRepository {
    /*
    * 인기글을 최종적으로 저정하는 장소는 Reids(=Persistent Layer)
    * Redis에 인기글을 저장하고 폐기하는 처리 로직을 담당
    * Key : hot-article::list:해당일자
    * Value : Sorted Set 자료구조를 활용하여 인기글 점수에 따라 정렬하여 데이터 저장
    * */
    private final StringRedisTemplate redisTemplate;

    // hot-article::list::{yyyyMMdd}
    private static final String KEY_FORMAT = "hot-article::list::%s";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /*
    * Redis에 데이터를 저장
    * - 선정된 인기글 Id
    * - 일자(time)
    * - 집계점수
    * - 인기글 개수(10개)
    * - 인기글 저장 ttl(최근 7일)
    * */
    public void add(Long articleId, LocalDateTime time, Long score, Long limit, Duration ttl) {
        /*
        * executePipelined - Redis에 한번만 연결한 상태로 여러번의 연산 수행 가능
        * */
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            /*
            * action -> connection casting 후 Redis와 Connect / Do Something.
            * */
            StringRedisConnection conn = (StringRedisConnection) action;
            String key = generateKey(time);
            /*
            * Redis에 실제로 적재되는 데이터는 "articleId"
            * */
            conn.zAdd(key, score, String.valueOf(articleId));
            /*
            * 상위 10건만 남기고 나머지는 제거
            * */
            conn.zRemRange(key, 0, - limit - 1);
            /*
            * 해당 데이터 적재의 만료시간(TTL) 정의
            * */
            conn.expire(key, ttl.toSeconds());
            return null;
        });
    }

    /*
    * 원본 게시글 삭제 시 Redis에서도 삭제
    * */
    public void remove(Long articleId, LocalDateTime time) {
        redisTemplate.opsForZSet().remove(generateKey(time), String.valueOf(articleId));
    }

    /*
    * 해당 일자를 이용하여 key 생성
    * */
    private String generateKey(LocalDateTime time) {
        return generateKey(TIME_FORMATTER.format(time));
    }

    private String generateKey(String dateStr) {
        return KEY_FORMAT.formatted(dateStr);
    }

    /*
    *
    * */
    public List<Long> readAll(String dateStr) {
        /*
        * sorted set 자료구조에서 데이터를 조회
        * */
        return redisTemplate.opsForZSet()
                /*
                * 정렬된 데이터를 역순 조회
                * */
                .reverseRangeWithScores(generateKey(dateStr), 0, -1).stream()
                .peek(tuple ->
                        log.info("[HotArticleListRepository.readAll] articleId={}, score={}", tuple.getValue(), tuple.getScore()))
                /*
                * sorted set에서 추출해온 tuple의 value(=article id)를 추출 후 list화
                * */
                .map(ZSetOperations.TypedTuple::getValue)
                .map(Long::valueOf)
                .toList();
    }
}
