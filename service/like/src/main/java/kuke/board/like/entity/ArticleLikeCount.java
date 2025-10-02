package kuke.board.like.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/*
* 좋아요와 좋아요수는 도메인(Database) 동일
* 단 성능적 이점을 확보하고자 기능적 분리
* */
@Table(name = "article_like_count")
@Getter
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleLikeCount {
    @Id
    private Long articleId; // shard key
    private Long likeCount;

    /*
    * 낙관락 이용
    * */
    @Version
    private Long version;

    /*
    * 엔티티가 없으면 성립할 수 없는 기능이나 데이터의 유효성/정합성(불변조건) 유지를 위해
    * DDD 관점에서(도메인 모델링) 엔티티 책임 하에 둔다.
    * */

    /*
    * 최초 초기화 시
    * */
    public static ArticleLikeCount init(Long articleId, Long likeCount) {
        ArticleLikeCount articleLikeCount = new ArticleLikeCount();
        articleLikeCount.articleId = articleId;
        articleLikeCount.likeCount = likeCount;
        articleLikeCount.version = 0L;
        return articleLikeCount;
    }

    /*
    * 좋아요 증가
    * */
    public void increase() {
        this.likeCount++;
    }

    /*
    * 좋아요 감소
    * */
    public void decrease() {
        this.likeCount--;
    }
}
