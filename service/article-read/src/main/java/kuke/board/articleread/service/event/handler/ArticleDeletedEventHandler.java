package kuke.board.articleread.service.event.handler;

import kuke.board.articleread.repository.ArticleIdListRepository;
import kuke.board.articleread.repository.ArticleQueryModelRepository;
import kuke.board.articleread.repository.BoardArticleCountRepository;
import kuke.board.common.event.Event;
import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.ArticleDeletedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleDeletedEventHandler implements EventHandler<ArticleDeletedEventPayload> {
    private final ArticleIdListRepository articleIdListRepository;
    private final ArticleQueryModelRepository articleQueryModelRepository;
    private final BoardArticleCountRepository boardArticleCountRepository;

    /*
     * Consumer 측에서 이벤트를 "실시간" 전달받아 처리하기 위한 메소드
     * 댓글 삭제 이벤트에 대한 이벤트 핸들러, 게시글 삭제 시 해당 데이터도 redis에서 삭제
     * */
    @Override
    public void handle(Event<ArticleDeletedEventPayload> event) {
        ArticleDeletedEventPayload payload = event.getPayload();
        /*
         * 게시글 삭제 시 해당 게시글을 redis에서 삭제
         * */
        articleIdListRepository.delete(payload.getBoardId(), payload.getArticleId());
        /*
        * 게시글 내역을 온전하게 삭제한 후 queyModel에서 삭제한다.
        * 이 게시글을 삭제하는 시점에 다른 사용자가 조회한다면, queryModel에서 제거 후 commit까지 해야 안보일 수 있기에,
        * 다시 말해 삭제시점에 다른 사용자가 보면 안되므로 -> articleQueryModle 먼저,
        * 삭제시점에 다른 사용자가 보아도 무방 -> 지금처럼 articleIdListRepository 먼저
        * */
        articleQueryModelRepository.delete(payload.getArticleId());
        /*
         * 게시글 삭제 시점에 게시글 목록에 대한 내역을 Redis에 저장
         * */
        boardArticleCountRepository.createOrUpdate(payload.getBoardId(), payload.getBoardArticleCount());
    }

    @Override
    public boolean supports(Event<ArticleDeletedEventPayload> event) {
        return EventType.ARTICLE_DELETED == event.getType();
    }
}
