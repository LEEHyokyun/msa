package kuke.board.articleread.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/*
* 페이징 번호 쿼리 시 게시글 반환수도 같이 추출해주어야 한다.
* 이 역시 원본 데이터를 요청해야 하지만, 성능적 유리함을 위해 이 역시도 같이 redis에 저장한다.
* */
@Repository
@RequiredArgsConstructor
public class BoardArticleCountRepository {
    private final StringRedisTemplate redisTemplate;

    // article-read::board-article-count::board::{boardId}
    private static final String KEY_FORMAT = "article-read::board-article-count::board::%s";

    /*
    * 페이징 쿼리시 필요한 매개변수를 받아와서 redis에 저장
    * 1차적으로는 어차피 반드시 MySQL을 다녀와야 하는데, 그 이후에 캐싱처리가 되는 것.
    * */
    public void createOrUpdate(Long boardId, Long articleCount) {
        redisTemplate.opsForValue().set(generateKey(boardId), String.valueOf(articleCount));
    }

    /*
    * 게시글 반환 수를 추출한다.
    * */
    public Long read(Long boardId) {
        String result = redisTemplate.opsForValue().get(generateKey(boardId));
        return result == null ? 0L : Long.valueOf(result);
    }

    private String generateKey(Long boardId) {
        return KEY_FORMAT.formatted(boardId);
    }
}
