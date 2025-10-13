package kuke.board.articleread.service.response;

import kuke.board.articleread.repository.ArticleQueryModel;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/*
* API level에서 필요한, 단순 전달용이 아닌 컨텍스트의 내용을 응답값으로 반영하기 위한 전역적 스냅샷
* = Response DTO.
* */
@Getter
/*
* 참고)
* @Override
public String toString() {
    return "ArticleReadResponse(articleId=" + this.articleId +
           ", title=" + this.title +
           ", content=" + this.content +
           ", boardId=" + this.boardId +
           ", ...)";
}
* */
@ToString
public class ArticleReadResponse {
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
    * DTO != ArticleQeuryModel
    * 조회수 필요!
    * */
    private Long articleViewCount;

    /*
    * view count 별도 매개변수로
    * */
    public static ArticleReadResponse from(ArticleQueryModel articleQueryModel, Long viewCount) {
        ArticleReadResponse response = new ArticleReadResponse();
        response.articleId = articleQueryModel.getArticleId();
        response.title = articleQueryModel.getTitle();
        response.content = articleQueryModel.getContent();
        response.boardId = articleQueryModel.getBoardId();
        response.writerId = articleQueryModel.getWriterId();
        response.createdAt = articleQueryModel.getCreatedAt();
        response.modifiedAt = articleQueryModel.getModifiedAt();
        response.articleCommentCount = articleQueryModel.getArticleCommentCount();
        response.articleLikeCount = articleQueryModel.getArticleLikeCount();
        response.articleViewCount = viewCount;
        return response;
    }
}
