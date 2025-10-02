package kuke.board.like.repository;

import jakarta.persistence.LockModeType;
import kuke.board.like.entity.ArticleLikeCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeCountRepository extends JpaRepository<ArticleLikeCount, Long> {
    // select ... for update (*조회시점부터 비관적 락을 적용하기 위함)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ArticleLikeCount> findLockedByArticleId(Long articleId);

    /*
    * 처리를 진행하는 시점에서 읽기도 동시적으로 발생
    * 다만 해당 "쿼리"를 진행하는 시점에서 DBMS는 row에 lock을 건다.
    * 비관적 락의 한 종류, 읽기 시점부터 lock을 진행하여 동시성 문제가 발생할 여지가 없다.
    * */
    @Query(
            value = "update article_like_count set like_count = like_count + 1 where article_id = :articleId",
            nativeQuery = true
    )
    @Modifying //위 쿼리에 대한 세트
    int increase(@Param("articleId") Long articleId);

    @Query(
            value = "update article_like_count set like_count = like_count - 1 where article_id = :articleId",
            nativeQuery = true
    )
    @Modifying //위 쿼리에 대한 세트
    int decrease(@Param("articleId") Long articleId);
}
