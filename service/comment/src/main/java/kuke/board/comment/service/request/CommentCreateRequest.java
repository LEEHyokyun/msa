package kuke.board.comment.service.request;

import lombok.Getter;

@Getter
/*
* 댓글 API에 대한 요청DTO
* */
public class CommentCreateRequest {
    private Long articleId;
    private String content;
    private Long parentCommentId;
    private Long writerId;
}
