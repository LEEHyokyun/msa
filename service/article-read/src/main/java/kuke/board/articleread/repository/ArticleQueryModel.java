package kuke.board.articleread.repository;

import kuke.board.articleread.client.ArticleClient;
import kuke.board.common.event.payload.*;
import lombok.Getter;

import java.time.LocalDateTime;

/*
* 도메인 경계가 불명확하거나(지금의 게시글 조회 서비스가 별도 도메인이 아닌 Query side "layer"/View Model라면) 단순 Repository 하위 전달용 객체라면, 지금처럼 ArticleQueryModel을 Repository와 직관적으로 가까이.
* 하지만 지금과 같이 MSA 환경에서 도메인 경계가 명확하고, 상태변경을 책임진다고 하면, 다른 서비스에서도 해당 객체모델을 사용한다고 하면 별도 model 프로젝트를 두어 관리하는게 맞다.
* 지금의 경우 상태변경로직까지 존재하므로, 도메인의 일부로 보아야 합리적이며 이에 따라 다른 model 계층으로 별도 빼놓는 것이 일반적.
* */
@Getter
public class ArticleQueryModel {
    private Long articleId;
    private String title;
    private String content;
    private Long boardId;
    private Long writerId;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Long articleCommentCount;
    private Long articleLikeCount;
    /*
    * 조회수 -> Event로 전달받는 것이 아닌 Redis에서 직접 전달받는다.
    * DTO != Response .. 책임분리 명확히.
    * */

    /*
    * 게시글 생성 이벤트 발생 시
    * (게시글 생성 시 데이터 적재)
    * */
    public static ArticleQueryModel create(ArticleCreatedEventPayload payload) {
        ArticleQueryModel articleQueryModel = new ArticleQueryModel();
        articleQueryModel.articleId = payload.getArticleId();
        articleQueryModel.title = payload.getTitle();
        articleQueryModel.content = payload.getContent();
        articleQueryModel.boardId = payload.getBoardId();
        articleQueryModel.writerId = payload.getWriterId();
        articleQueryModel.createdAt = payload.getCreatedAt();
        articleQueryModel.modifiedAt = payload.getModifiedAt();
        articleQueryModel.articleCommentCount = 0L;
        articleQueryModel.articleLikeCount = 0L;
        return articleQueryModel;
    }

    /*
     * 게시글 생성이 없거나 Redis에 적재된 데이터가 없는 상태
     * (원본 데이터, 즉 커맨드 서버에 원본 데이터를 요청하여 없는 상태에서 원본 데이터를 적재)
     * */
    public static ArticleQueryModel create(ArticleClient.ArticleResponse article, Long commentCount, Long likeCount) {
        ArticleQueryModel articleQueryModel = new ArticleQueryModel();
        articleQueryModel.articleId = article.getArticleId();
        articleQueryModel.title = article.getTitle();
        articleQueryModel.content = article.getContent();
        articleQueryModel.boardId = article.getBoardId();
        articleQueryModel.writerId = article.getWriterId();
        articleQueryModel.createdAt = article.getCreatedAt();
        articleQueryModel.modifiedAt = article.getModifiedAt();
        articleQueryModel.articleCommentCount = commentCount;
        articleQueryModel.articleLikeCount = likeCount;
        return articleQueryModel;
    }

    /*
    * 각 커맨드가 상태변경을 하였을떄 이에 대해 실시간으로 상태변경을 해준다.
    * - 댓글 생성 및 삭제
    * - 게시글 좋아요 및 좋아요 취소
    * - 게시글 수정
    * 상태변경에 대해 ArticleQueryModel이 그 상태변경 컨텍스트를 같이 저장.
    * */
    public void updateBy(CommentCreatedEventPayload payload) {
        this.articleCommentCount = payload.getArticleCommentCount();
    }

    public void updateBy(CommentDeletedEventPayload payload) {
        this.articleCommentCount = payload.getArticleCommentCount();
    }

    public void updateBy(ArticleLikedEventPayload payload) {
        this.articleLikeCount = payload.getArticleLikeCount();
    }

    public void updateBy(ArticleUnlikedEventPayload payload) {
        this.articleLikeCount = payload.getArticleLikeCount();
    }

    public void updateBy(ArticleUpdatedEventPayload payload) {
        this.title = payload.getTitle();
        this.content = payload.getContent();
        this.boardId = payload.getBoardId();
        this.writerId = payload.getWriterId();
        this.createdAt = payload.getCreatedAt();
        this.modifiedAt = payload.getModifiedAt();
    }
}
