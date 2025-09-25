package kuke.board.comment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/*
* 기본 Comment Entity
* */
@Table(name = "comment")
@Getter
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    @Id
    private Long commentId;
    private String content;
    private Long parentCommentId;
    private Long articleId; // shard key
    private Long writerId;
    private Boolean deleted;
    private LocalDateTime createdAt;

    /*
     * 정적 팩토리 메소드 : 엔티티 객체를 생성하는 책임
     * */
    public static Comment create(Long commentId, String content, Long parentCommentId, Long articleId, Long writerId) {
        Comment comment = new Comment();
        comment.commentId = commentId;
        comment.content = content;
        /*
        * 상위 댓글 없으면
        * 자신의 댓글ID를 상위 ID로 그대로 지정
        * */
        comment.parentCommentId = parentCommentId == null ? commentId : parentCommentId;
        comment.articleId = articleId;
        comment.writerId = writerId;
        comment.deleted = false;
        comment.createdAt = LocalDateTime.now();
        return comment;
    }

    /*
     * 최상위 댓글인지 확인하는 메소드
     * 댓글 엔티티 하 책임
     * (Article 도메인과 마찬가지로, API에서 사용하는 엔티티 컨텍스트 (직접적인) 조회의 책임을 Entity 책임 하에 위치함)
     * */
    public boolean isRoot() {
        return parentCommentId.longValue() == commentId;
    }

    /*
     * 댓글을 삭제하는 메소드
     * 댓글 엔티티 하 책임
     * (Article 도메인과 마찬가지로, API에서 사용하는 엔티티 컨텍스트 (직접적인) 변경의 책임을 Entity 책임 하에 위치함)
     * */
    public void delete() {
        deleted = true;
    }
}
