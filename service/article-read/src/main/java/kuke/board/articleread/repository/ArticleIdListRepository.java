package kuke.board.articleread.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
* 게시글 목록 최적화
* 모든 글을 Redis에 저장하지 않고 최신글 1000개에 대해서만 저장한다.
* */
@Repository
@RequiredArgsConstructor //for final
public class ArticleIdListRepository {
    private final StringRedisTemplate redisTemplate;

    // article-read::board::{boardId}::article-list
    private static final String KEY_FORMAT = "article-read::board::%s::article-list";

    public void add(Long boardId, Long articleId, Long limit) {
        /*
        * 게시글 목록을 조회하기 위함
        * - key = article-read::board::%s::article-list
        * - value = article id가 이상적이지만 long > double 캐스팅 시 유실발생하여 0으로 지정
        * - score = padded string, 동일 score일 경우 value 오름차순으로 정리, value에 padded string 오름차순 정렬을 위해 문자열 그대로 삽입한다.
        * */
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection conn = (StringRedisConnection) action;
            String key = generateKey(boardId);
            conn.zAdd(key, 0, toPaddedString(articleId));

            /*
            * redis에 데이터 넣는 시점에 상위 1000개만 유지하도록 나머지 데이터 삭제한다.
            * */
            conn.zRemRange(key, 0, - limit - 1);
            return null;
        });
    }

    /*
    * 특정 key에 저장된 특정 게시글 삭제
    * */
    public void delete(Long boardId, Long articleId) {
        redisTemplate.opsForZSet().remove(generateKey(boardId), toPaddedString(articleId));
    }

    /*
    * redis에서 최신 데이터(페이징) 조회
    * */
    public List<Long> readAll(Long boardId, Long offset, Long limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(generateKey(boardId), offset, offset + limit - 1)
                .stream().map(Long::valueOf).toList();
    }

    /*
    * redis에서 최신 데이터(무한스크롤) 조회
    * */
    public List<Long> readAllInfiniteScroll(Long boardId, Long lastArticleId, Long limit) {
        return redisTemplate.opsForZSet().reverseRangeByLex(
                generateKey(boardId),
                //6 5 4 3 2 1
                lastArticleId == null ?
                        //6 5 4 3
                        Range.unbounded() :
                        //last article id = 3 = left bounded
                        //2 1
                        Range.leftUnbounded(Range.Bound.exclusive(toPaddedString(lastArticleId))),
                Limit.limit().count(limit.intValue())
        ).stream().map(Long::valueOf).toList();
    }

    /*
    * long type -> 문자열 전환
    * article id가 너무 길어져서 타입캐스팅으로 인한 데이터 유실 방지
    * */
    private String toPaddedString(Long articleId) {
        return "%019d".formatted(articleId);
        // 1234 -> 0000000000000001234
    }

    private String generateKey(Long boardId) {
        return KEY_FORMAT.formatted(boardId);
    }
}
