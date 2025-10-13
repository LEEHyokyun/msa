package kuke.board.articleread.service.event.handler;

import kuke.board.articleread.repository.ArticleQueryModelRepository;
import kuke.board.common.event.Event;
import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.ArticleLikedEventPayload;
import kuke.board.common.event.payload.CommentCreatedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleLikedEventHandler implements EventHandler<ArticleLikedEventPayload> {
    private final ArticleQueryModelRepository articleQueryModelRepository;

    /*
     * Consumer 측에서 이벤트를 "실시간" 전달받아 처리하기 위한 메소드
     * 좋아요 생성 이벤트에 대한 이벤트 핸들러, 좋아요 데이터를 Redis에 반영
     * */
    @Override
    public void handle(Event<ArticleLikedEventPayload> event) {
        articleQueryModelRepository.read(event.getPayload().getArticleId())
                .ifPresent(articleQueryModel -> {
                    articleQueryModel.updateBy(event.getPayload());
                    articleQueryModelRepository.update(articleQueryModel);
                });
    }

    @Override
    public boolean supports(Event<ArticleLikedEventPayload> event) {
        return EventType.ARTICLE_LIKED == event.getType();
    }
}
