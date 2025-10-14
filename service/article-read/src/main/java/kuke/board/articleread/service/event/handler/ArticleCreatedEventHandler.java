package kuke.board.articleread.service.event.handler;

import kuke.board.articleread.repository.ArticleIdListRepository;
import kuke.board.articleread.repository.ArticleQueryModel;
import kuke.board.articleread.repository.ArticleQueryModelRepository;
import kuke.board.articleread.repository.BoardArticleCountRepository;
import kuke.board.common.event.Event;
import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.ArticleCreatedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ArticleCreatedEventHandler implements EventHandler<ArticleCreatedEventPayload> {
    private final ArticleIdListRepository articleIdListRepository;
    private final ArticleQueryModelRepository articleQueryModelRepository;
    private final BoardArticleCountRepository boardArticleCountRepository;

    /*
    * Consumer 측에서 이벤트를 "실시간" 전달받아 처리하기 위한 메소드
    * 게시글 생성 이벤트에 대한 이벤트 핸들러, 게시글 생성후 1일 만료기간의 Redis 데이터를 생성.
    * */
    @Override
    public void handle(Event<ArticleCreatedEventPayload> event) {
        ArticleCreatedEventPayload payload = event.getPayload();
        articleQueryModelRepository.create(
                ArticleQueryModel.create(payload),
                Duration.ofDays(1)
        );
        /*
        * 게시글 생성 시점에 게시글 목록에 대한 내역을 Redis에 저장
        * 1000개까지만 저장한다(즉, 저장 후 역정렬 순으로 1000개만 저장, 나머지는 삭제).
        * */
        articleIdListRepository.add(payload.getBoardId(), payload.getArticleId(), 1000L);
        /*
        * 게시글 목록 개수도 같이 반영
        * */
        boardArticleCountRepository.createOrUpdate(payload.getBoardId(), payload.getBoardArticleCount());
    }

    @Override
    public boolean supports(Event<ArticleCreatedEventPayload> event) {
        return EventType.ARTICLE_CREATED == event.getType();
    }
}
