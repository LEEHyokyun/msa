package kuke.board.common.event;

import kuke.board.common.event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {
    /*
    * Event Type에 대한 정의
    * - 게시글 생성
    * - 게시글 수정
    * - 게시글 삭제
    * - 댓글 생성
    * - 댓글 삭제
    * - 게시글 좋아요 생성
    * - 게시글 좋아요 취소
    * - 조회(100개마다 이벤트 통신)
    * 각 Event Type Class를 type/topic에 대입
    * */
    ARTICLE_CREATED(ArticleCreatedEventPayload.class, Topic.KUKE_BOARD_ARTICLE),
    ARTICLE_UPDATED(ArticleUpdatedEventPayload.class, Topic.KUKE_BOARD_ARTICLE),
    ARTICLE_DELETED(ArticleDeletedEventPayload.class, Topic.KUKE_BOARD_ARTICLE),
    COMMENT_CREATED(CommentCreatedEventPayload.class, Topic.KUKE_BOARD_COMMENT),
    COMMENT_DELETED(CommentDeletedEventPayload.class, Topic.KUKE_BOARD_COMMENT),
    ARTICLE_LIKED(ArticleLikedEventPayload.class, Topic.KUKE_BOARD_LIKE),
    ARTICLE_UNLIKED(ArticleUnlikedEventPayload.class, Topic.KUKE_BOARD_LIKE),
    ARTICLE_VIEWED(ArticleViewedEventPayload.class, Topic.KUKE_BOARD_VIEW)
    ;

    /*
    * Enum이 관리하는 데이터 혹은 항목
    * - Event들이 어떤 payload를 가지는지
    * - Event들이 어떤 topic으로 전달될 수 있는지
    * */
    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    /*
    * EventType
    * */
    public static EventType from(String type) {
        try {
            return valueOf(type);
        } catch (Exception e) {
            log.error("[EventType.from] type={}", type, e);
            return null;
        }
    }

    /*
    * Topic
    * */
    public static class Topic {
        public static final String KUKE_BOARD_ARTICLE = "kuke-board-article";
        public static final String KUKE_BOARD_COMMENT = "kuke-board-comment";
        public static final String KUKE_BOARD_LIKE = "kuke-board-like";
        public static final String KUKE_BOARD_VIEW = "kuke-board-view";
    }
}
