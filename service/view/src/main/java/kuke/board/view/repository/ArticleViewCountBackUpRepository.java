package kuke.board.view.repository;

import kuke.board.view.entity.ArticleViewCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/*
 * MySQL를 통한 조회수 백업 처리를 위한 Repository Interface.
 * 실제 JPA Repository 사용.
 * */
@Repository
public interface ArticleViewCountBackUpRepository extends JpaRepository<ArticleViewCount, Long> {
    /*
    * 비관적 락을 이용하여 조회 및 처리가 하나의 단계에서 바로 이루어질 수 있도록 작성
    * MySQL RDB로 백업하는 처리 Repository.
    * 조회수가 현재 데이터보다 큰 값에 대해서만 update.
    * */
    @Query(
            value = "update article_view_count set view_count = :viewCount " +
                    "where article_id = :articleId and view_count < :viewCount",
            nativeQuery = true
    )
    @Modifying
    int updateViewCount(
            @Param("articleId") Long articleId,
            @Param("viewCount") Long viewCount
    );
}
