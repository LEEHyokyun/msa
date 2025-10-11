package kuke.board.comment.service;

import kuke.board.comment.entity.ArticleCommentCount;
import kuke.board.comment.entity.CommentPath;
import kuke.board.comment.entity.CommentV2;
import kuke.board.comment.repository.ArticleCommentCountRepository;
import kuke.board.comment.repository.CommentRepositoryV2;
import kuke.board.comment.service.request.CommentCreateRequestV2;
import kuke.board.comment.service.response.CommentPageResponse;
import kuke.board.comment.service.response.CommentResponse;
import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.CommentCreatedEventPayload;
import kuke.board.common.event.payload.CommentDeletedEventPayload;
import kuke.board.common.outboxmessagerelay.OutboxEventPublisher;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
public class CommentServiceV2 {
    private final Snowflake snowflake = new Snowflake();
    private final CommentRepositoryV2 commentRepository;
    /*
     * outbox pattern 로직 추가
     * */
    private final OutboxEventPublisher outboxEventPublisher;
    private final ArticleCommentCountRepository articleCommentCountRepository;

    /*
    * 경로열거에 의한 댓글 생성
    * */
    @Transactional
    public CommentResponse create(CommentCreateRequestV2 request) {
        /*
        * 기본적으로 상위 댓글을 찾고
        * 상위댓글이 없다면 빈 문자열, 상위댓글이 있다면 그대로 추출하여
        * 댓글정보를 생성한다.
        * */
        CommentV2 parent = findParent(request);
        CommentPath parentCommentPath = parent == null ? CommentPath.create("") : parent.getCommentPath();
        CommentV2 comment = commentRepository.save(
                CommentV2.create(
                        snowflake.nextId(),
                        request.getContent(),
                        request.getArticleId(),
                        request.getWriterId(),
                        parentCommentPath.createChildCommentPath(
                                commentRepository.findDescendantsTopPath(request.getArticleId(), parentCommentPath.getPath())
                                        .orElse(null)
                        )
                )
        );

        /*
        * 댓글 생성시점에 댓글 수 증가
        * 최초 생성 시 데이터 없으면 1로 초기화(save 명기)
        * */
        int result = articleCommentCountRepository.increase(request.getArticleId());
        if (result == 0) {
            articleCommentCountRepository.save(
                    ArticleCommentCount.init(request.getArticleId(), 1L)
            );
        }

        /*
         * outbox pattern 로직 추가
         * */
        outboxEventPublisher.publish(
                EventType.COMMENT_CREATED,
                CommentCreatedEventPayload.builder()
                        .commentId(comment.getCommentId())
                        .content(comment.getContent())
                        .articleId(comment.getArticleId())
                        .writerId(comment.getWriterId())
                        .deleted(comment.getDeleted())
                        .createdAt(comment.getCreatedAt())
                        .articleCommentCount(count(comment.getArticleId()))
                        .build(),
                comment.getArticleId()
        );

        return CommentResponse.from(comment);
    }

    private CommentV2 findParent(CommentCreateRequestV2 request) {
        String parentPath = request.getParentPath();
        if (parentPath == null) {
            return null;
        }

        /*
        * 부모댓글의 정보 그대로 추출(*삭제되지 않은 댓글)
        * */
        return commentRepository.findByPath(parentPath)
                .filter(not(CommentV2::getDeleted))
                .orElseThrow();
    }

    /*
    * 댓글내용 불러오기
    * */
    public CommentResponse read(Long commentId) {
        return CommentResponse.from(
                commentRepository.findById(commentId).orElseThrow()
        );
    }

    /*
    * 댓글 삭제하기
    * - 댓글을 찾아서 삭제가 되지 않은 것을 확인
    * - 자식이 있을 경우 삭제표시, 없을 경우 hard delete
    * */
    @Transactional
    public void delete(Long commentId) {
        commentRepository.findById(commentId)
                .filter(not(CommentV2::getDeleted))
                .ifPresent(comment -> {
                    if(hasChildren(comment)) {
                        /*
                        * 삭제표시(soft delete) 시 댓글 수 감소처리 하지 않는다.
                        * */
                        comment.delete();
                    } else {
                        /*
                        * 물리적 삭제 시에만 감소 처리한다.
                        * */
                        delete(comment);
                    }

                    /*
                     * outbox pattern 로직 추가
                     * */
                    outboxEventPublisher.publish(
                            EventType.COMMENT_DELETED,
                            CommentDeletedEventPayload.builder()
                                    .commentId(comment.getCommentId())
                                    .content(comment.getContent())
                                    .articleId(comment.getArticleId())
                                    .writerId(comment.getWriterId())
                                    .deleted(comment.getDeleted())
                                    .createdAt(comment.getCreatedAt())
                                    .articleCommentCount(count(comment.getArticleId()))
                                    .build(),
                            comment.getArticleId()
                    );
                });
    }

    /*
    * 자식댓글유무 확인 = descendants top path 댓글 조회\
    * Optional isPresent()
    * */
    private boolean hasChildren(CommentV2 comment) {
        return commentRepository.findDescendantsTopPath(
                comment.getArticleId(),
                comment.getCommentPath().getPath()
        ).isPresent();
    }

    /*
    * 삭제, root 댓글이 아니라면 상위 댓글을 찾아서 재귀적 삭제
    * */
    private void delete(CommentV2 comment) {
        commentRepository.delete(comment);
        /*
         * 댓글 물리적 삭제시점에 댓글 수 감소
         * */
        articleCommentCountRepository.decrease(comment.getArticleId());
        if (!comment.isRoot()) {
            commentRepository.findByPath(comment.getCommentPath().getParentPath())
                    .filter(CommentV2::getDeleted)
                    .filter(not(this::hasChildren))
                    .ifPresent(this::delete);
        }
    }

    /*
    * 페이징 쿼리
    * */
    public CommentPageResponse readAll(Long articleId, Long page, Long pageSize) {
        return CommentPageResponse.of(
                commentRepository.findAll(articleId, (page - 1) * pageSize, pageSize).stream()
                        .map(CommentResponse::from)
                        .toList(),
                commentRepository.count(articleId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L))
                /*
                * 필요시 count 바로 호출 가능
                * */
                //count(articleId);
        );
    }

    /*
    * 무한 스크롤
    * */
    public List<CommentResponse> readAllInfiniteScroll(Long articleId, String lastPath, Long pageSize) {
        List<CommentV2> comments = lastPath == null ?
                commentRepository.findAllInfiniteScroll(articleId, pageSize) :
                commentRepository.findAllInfiniteScroll(articleId, lastPath, pageSize);

        return comments.stream()
                .map(CommentResponse::from)
                .toList();
    }

    /*
    * 전체 댓글 수 없으면 0을 반환
    * */
    public Long count(Long articleId) {
        return articleCommentCountRepository.findById(articleId)
                .map(ArticleCommentCount::getCommentCount)
                .orElse(0L);
    }
}
