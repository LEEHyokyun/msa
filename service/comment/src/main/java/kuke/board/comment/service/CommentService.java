package kuke.board.comment.service;

import kuke.board.comment.entity.Comment;
import kuke.board.comment.repository.CommentRepository;
import kuke.board.comment.service.request.CommentCreateRequest;
import kuke.board.comment.service.response.CommentPageResponse;
import kuke.board.comment.service.response.CommentResponse;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
/*
* 댓글 기능 구축을 위한 댓글 Service 생성
* RequiredArgsConstructor로 인해 생성자 주입을 진행한다(*AutoWired은 권장사항이 아니며, 독립적인 테스트 환경 사용 권장)
* */
public class CommentService {
    private final Snowflake snowflake = new Snowflake();
    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponse create(CommentCreateRequest request) {
        Comment parent = findParent(request);
        Comment comment = commentRepository.save(
                Comment.create(
                        snowflake.nextId(),
                        request.getContent(),
                        parent == null ? null : parent.getCommentId(),
                        request.getArticleId(),
                        request.getWriterId()
                )
        );
        return CommentResponse.from(comment);
    }

    private Comment findParent(CommentCreateRequest request) {
        Long parentCommentId = request.getParentCommentId();
        /*
        * 상위댓글이 존재하지 않을 경우 null 반환
        * */
        if ( parentCommentId == null) {
            return null;
        }
        return commentRepository.findById(parentCommentId)
                /*
                * 상위 댓글이 존재할 경우
                * - 삭제하지 않은 항목(not)
                * - 해당 댓글 root 여부가 true일 경우만
                * 필터링하여 객체를 반환한다.
                * */
                .filter(not(Comment::getDeleted))
                .filter(Comment::isRoot)
                .orElseThrow();
    }

    public CommentResponse read(Long commentId) {
        return CommentResponse.from(
                commentRepository.findById(commentId).orElseThrow()
        );
    }

    @Transactional
    public void delete(Long commentId) {
        commentRepository.findById(commentId)
                /*
                * 해당 댓글이 기삭제 건인지 먼저 확인
                * - 이후 해당 댓글이 존재하면(ifPresent)
                * - 해당 댓글 객체의 하위 댓글 여부를 이어서 확인
                * - 하위 댓글 존재시 삭제표시만 진행(delete = true)
                * - 하위 댓글 존재하지 않으면 바로 삭제(JPA를 통한 hard delete)
                * */
                .filter(not(Comment::getDeleted))
                /*
                * filter 이후 조건에 맞는 값이 없으면 → Optional.empty()
                * → ifPresent는 실행되지 않음
                * → 그냥 끝 (null 리턴 아님, void 종료).
                * */
                .ifPresent(comment -> {
                    if (hasChildren(comment)) {
                        comment.delete();
                    } else {
                        delete(comment);
                    }
                });
    }

    /*
    * 댓글의 자식 존재 여부를 조회함(*2계층)
    * 해당 댓글의 자식 여부를 조회하므로 조회대상의 comment_id = 조회조건에서의 parent_id
    * */
    private boolean hasChildren(Comment comment) {
        return commentRepository.countBy(comment.getArticleId(), comment.getCommentId(), 2L) == 2;
    }

    /*
    * 실제 삭제 : JPA 책임
    * (*컨텍스트 변경이 이루어지지 않는 동작이므로 JPA를 통해 진행)
    * */
    private void delete(Comment comment) {
        /*
        * 상위 댓글이라면 바로 삭제 가능
        * */
        commentRepository.delete(comment);
        /*
        * 상위 댓글이 아니라면 상위댓글까지 자식여부를 조회하여
        * 재귀적으로 삭제해야 함(상위 댓글이 기삭제된 건에 대해 자식존재여부 확인 후 최종 삭제함)
        * 재귀적 호출이므로 Service에서 설정한(=this) hasChildren / delete 메소드를 사용한다.
        * */
        if (!comment.isRoot()) {
            commentRepository.findById(comment.getParentCommentId())
                    .filter(Comment::getDeleted)
                    .filter(not(this::hasChildren))
                    .ifPresent(this::delete); //재귀호출
        }
    }

    /*
    * 페이징
    * */
    public CommentPageResponse readAll(Long articleId, Long page, Long pageSize) {
        return CommentPageResponse.of(
                commentRepository.findAll(articleId, (page - 1) * pageSize, pageSize).stream()
                        .map(CommentResponse::from)
                        .toList(),
                commentRepository.count(articleId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L))
        );
    }

    /*
    * 무한스크롤
    * */
    public List<CommentResponse> readAll(Long articleId, Long lastParentCommentId, Long lastCommentId, Long limit) {
        List<Comment> comments = lastParentCommentId == null || lastCommentId == null ?
                commentRepository.findAllInfiniteScroll(articleId, limit) :
                commentRepository.findAllInfiniteScroll(articleId, lastParentCommentId, lastCommentId, limit);
        return comments.stream()
                .map(CommentResponse::from)
                .toList();
    }

}
