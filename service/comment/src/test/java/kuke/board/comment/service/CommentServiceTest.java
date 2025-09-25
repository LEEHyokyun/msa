package kuke.board.comment.service;

import kuke.board.comment.entity.Comment;
import kuke.board.comment.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/*
* service 기능동작 검증을 위한 테스트
* */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    /*
    * Service Mock 객체 생성
    * "테스트의 직접 대상"이므로 InjectMocks 처리한다.
    * Mock으로 주입받아 내부적인 동작들은 모두 Mock 객체화
    * */
    @InjectMocks
    CommentService commentService;
    /*
     * 별도 Mock 객체 생성
     * "테스트의 직접 대상은 아니지만 테스트를 하는데 필요한 협력/의존 객체"이므로 Mocks 처리한다.
     * 테스트에 필요한 Mock객체의 Mocking/Stubing을 진행하기 위해 별도 Mock 객체화
     * */
    @Mock
    CommentRepository commentRepository;

    @Test
    @DisplayName("삭제할 댓글이 자식 있으면, 삭제 표시만 한다.")
    void deleteShouldMarkDeletedIfHasChildren() {
        // given
        Long articleId = 1L;
        Long commentId = 2L;
        Comment comment = createComment(articleId, commentId);

        /*
        * 아래 JPA 로직 호출이 된다는 것을 염두에 두고 Stub.
        * (*mock 객체 반환 stub)
        * */
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));
        given(commentRepository.countBy(articleId, commentId, 2L)).willReturn(2L);

        // when
        /*
        * Service 기능동작 test
        * */
        commentService.delete(commentId);

        /*
        * comment 객체는 delete 메소드를 호출한다.
        * stub : 최종적인 동작검증
        * */
        // then
        verify(comment).delete();
    }

    @Test
    @DisplayName("하위 댓글이 삭제되고, 삭제되지 않은 부모면, 하위 댓글만 삭제한다.")
    void deleteShouldDeleteChildOnlyIfNotDeletedParent() {
        // given
        Long articleId = 1L;
        Long commentId = 2L;
        Long parentCommentId = 1L;

        /*
        * 자식객체
        * */
        Comment comment = createComment(articleId, commentId, parentCommentId);
        given(comment.isRoot()).willReturn(false);

        /*
        * 부모객체(mock객체, 삭제되지 않은 상태만 가지도록 stub)
        * */
        Comment parentComment = mock(Comment.class);
        given(parentComment.getDeleted()).willReturn(false);

        /*
        * 자식객체 stub
        * */
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));
        given(commentRepository.countBy(articleId, commentId, 2L)).willReturn(1L);

        /*
        * 부모객체 stub
        * */
        given(commentRepository.findById(parentCommentId))
                .willReturn(Optional.of(parentComment));

        // when
        commentService.delete(commentId);

        // then
        verify(commentRepository).delete(comment);
        verify(commentRepository, never()).delete(parentComment);
    }

    @Test
    @DisplayName("하위 댓글이 삭제되고, 삭제된 부모면, 재귀적으로 모두 삭제한다.")
    void deleteShouldDeleteAllRecursivelyIfDeletedParent() {
        // given
        Long articleId = 1L;
        Long commentId = 2L;
        Long parentCommentId = 1L;

        Comment comment = createComment(articleId, commentId, parentCommentId);
        given(comment.isRoot()).willReturn(false);

        Comment parentComment = createComment(articleId, parentCommentId);
        given(parentComment.isRoot()).willReturn(true);
        given(parentComment.getDeleted()).willReturn(true);

        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));
        given(commentRepository.countBy(articleId, commentId, 2L)).willReturn(1L);

        given(commentRepository.findById(parentCommentId))
                .willReturn(Optional.of(parentComment));
        given(commentRepository.countBy(articleId, parentCommentId, 2L)).willReturn(1L);

        // when
        commentService.delete(commentId);

        // then
        verify(commentRepository).delete(comment);
        verify(commentRepository).delete(parentComment);
    }

    /*
    * comment mock
    * */
    private Comment createComment(Long articleId, Long commentId) {
        /*
        * mock 객체화
        * test 환경에 의존하지 않고 일관성있는 테스트 환경 및 결과 보장이 가능하다.
        * */
        Comment comment = mock(Comment.class);
        /*
        * Mock 객체 Stub 매핑정보를 정의한다.
        * - comment Mock 객체의 ArticleId나 commentId가 호출 혹은 사용 시
        * - willReturn(정의해준 articleId와 commentId)값에 따라 값을 반환해주도록 약속한다.
        * */
        given(comment.getArticleId()).willReturn(articleId);
        given(comment.getCommentId()).willReturn(commentId);
        return comment;
    }

    /*
     * comment mock(+parent id)
     * */
    private Comment createComment(Long articleId, Long commentId, Long parentCommentId) {
        Comment comment = createComment(articleId, commentId);
        given(comment.getParentCommentId()).willReturn(parentCommentId);
        return comment;
    }
}