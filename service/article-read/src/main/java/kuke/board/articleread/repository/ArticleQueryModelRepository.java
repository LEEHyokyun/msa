package kuke.board.articleread.repository;

import kuke.board.common.dataserializer.DataSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/*
* articleQueryModel에서 정의한 상태변경을
* 최종적으로 redis에 반영
* */
@Repository
@RequiredArgsConstructor
public class ArticleQueryModelRepository {
    private final StringRedisTemplate redisTemplate;

    // article-read::article::{articleId}
    private static final String KEY_FORMAT = "article-read::article::%s";

    /*
    * article query model을 받아 최초 생성
    * - 신규 데이터 저장
    * */
    public void create(ArticleQueryModel articleQueryModel, Duration ttl) {
        redisTemplate.opsForValue()
                //serializer : Json
                .set(generateKey(articleQueryModel), DataSerializer.serialize(articleQueryModel), ttl);
    }
    
    /*
    * 데이터 수정
    * */
    public void update(ArticleQueryModel articleQueryModel) {
        //update - only query model(데이터가 있을 경우만 수정)
        redisTemplate.opsForValue().setIfPresent(generateKey(articleQueryModel), DataSerializer.serialize(articleQueryModel));
    }

    /*
    * 데이터 삭제
    * */
    public void delete(Long articleId) {
        redisTemplate.delete(generateKey(articleId));
    }

    /*
    * 영속성 계층에서 DB의 데이터 추출결과가 아무것도 없을때(null)
    * 이에 대한 대비를 해야하는 경우 Optional 사용
    * 데이터 읽기
    * */
    public Optional<ArticleQueryModel> read(Long articleId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(generateKey(articleId))
        ).map(json -> DataSerializer.deserialize(json, ArticleQueryModel.class));
    }

    private String generateKey(ArticleQueryModel articleQueryModel) {
        return generateKey(articleQueryModel.getArticleId());
    }

    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }

    /*
    * articleIds -> keyList 만들어서 "내역들 조회"(=multiGet) -> 최종 ArticleQueryModel로 역직렬화 후
    * */
    public Map<Long, ArticleQueryModel> readAll(List<Long> articleIds) {
        List<String> keyList = articleIds.stream().map(this::generateKey).toList();
        return redisTemplate.opsForValue().multiGet(keyList).stream()
                /*
                * article id를 통해 Redis 조회가능한 keyList화 후, 해당 key값에 해당하는 데이터 조회
                * */
                .filter(Objects::nonNull)
                /*
                * 이중 Null이 아닌 데이터를 필터링하여 ArticleQuery Class로 역직렬화
                 * */
                .map(json -> DataSerializer.deserialize(json, ArticleQueryModel.class))
                /*
                * 이후 key - 객체자체(identity()) Map화하여 수집한다.
                 * */
                .collect(toMap(ArticleQueryModel::getArticleId, identity()));
    }
}
