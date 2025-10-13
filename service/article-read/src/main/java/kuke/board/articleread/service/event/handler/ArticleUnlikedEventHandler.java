package kuke.board.articleread.service.event.handler;

import kuke.board.articleread.repository.ArticleQueryModelRepository;
import kuke.board.common.event.Event;
import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.ArticleLikedEventPayload;
import kuke.board.common.event.payload.ArticleUnlikedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleUnlikedEventHandler implements EventHandler<ArticleUnlikedEventPayload> {
    private final ArticleQueryModelRepository articleQueryModelRepository;

    /*
     * Consumer 측에서 이벤트를 "실시간" 전달받아 처리하기 위한 메소드
     * 좋아요 취소 이벤트에 대한 이벤트 핸들러, 좋아요 취소 후 Redis에 데이터 수정
     * */
    @Override
    public void handle(Event<ArticleUnlikedEventPayload> event) {
        articleQueryModelRepository.read(event.getPayload().getArticleId())
                .ifPresent(articleQueryModel -> {
                    articleQueryModel.updateBy(event.getPayload());
                    articleQueryModelRepository.update(articleQueryModel);
                });
    }

    @Override
    public boolean supports(Event<ArticleUnlikedEventPayload> event) {
        return EventType.ARTICLE_UNLIKED == event.getType();
    }
}
