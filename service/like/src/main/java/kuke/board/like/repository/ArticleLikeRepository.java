package kuke.board.like.repository;

import kuke.board.like.entity.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {
    /*
    * article Id, user Id를 통한 좋아요 조회
    * 게시글 당 1회이므로 List Collection이 아닌 Object.
    * */
    Optional<ArticleLike> findByArticleIdAndUserId(Long articleId, Long userId);
}
