package kuke.board.comment.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Table(name = "comment_v2")
@Getter
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentV2 {
    @Id
    private Long commentId;
    private String content;
    private Long articleId; // shard key
    private Long writerId;

    /*
    * parent Id 불필요
    * 그대신 comment Path 정보만 필요
    * 내부적으로 기본 자료형이 아닌 값을 표현하는 객체 (참조형) Value Object가 필요할 경우 사용하는 어노테이션
    * -> Embedded 어노테이션 사용
    * */
    @Embedded
    private CommentPath commentPath;
    private Boolean deleted;
    private LocalDateTime createdAt;

    public static CommentV2 create(Long commentId, String content, Long articleId, Long writerId, CommentPath commentPath) {
        CommentV2 comment = new CommentV2();
        comment.commentId = commentId;
        comment.content = content;
        comment.articleId = articleId;
        comment.writerId = writerId;
        comment.commentPath = commentPath;
        comment.deleted = false;
        comment.createdAt = LocalDateTime.now();
        return comment;
    }

    public boolean isRoot() {
        return commentPath.isRoot();
    }

    public void delete() {
        deleted = true;
    }
}
