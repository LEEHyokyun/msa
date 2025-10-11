package kuke.board.view.service;

import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.ArticleViewedEventPayload;
import kuke.board.common.outboxmessagerelay.OutboxEventPublisher;
import kuke.board.view.entity.ArticleViewCount;
import kuke.board.view.repository.ArticleViewCountBackUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
* Redis에서의 조회수를 MySQL에 백업
* 백업시점에 outbox 동작 실행
* */
@Component
@RequiredArgsConstructor
public class ArticleViewCountBackUpProcessor {
    /*
     * outbox pattern 로직 추가
     * */
    private final OutboxEventPublisher outboxEventPublisher;
    private final ArticleViewCountBackUpRepository articleViewCountBackUpRepository;

    /*
    * 백업과정
    * */
    @Transactional
    public void backUp(Long articleId, Long viewCount) {
        //update
        int result = articleViewCountBackUpRepository.updateViewCount(articleId, viewCount);

        /*
        * result = 0 -> 삽입된 데이터가 없으므로 초기 데이터 생성과정 필요
        * */
        if (result == 0) {
            articleViewCountBackUpRepository.findById(articleId)
                    .ifPresentOrElse(
                            ignored -> { }, //No Logic
                        () -> articleViewCountBackUpRepository.save(ArticleViewCount.init(articleId, viewCount)) //Logic for No Data
                    );
        }

        /*
         * outbox pattern 로직 추가
         * */
        outboxEventPublisher.publish(
                EventType.ARTICLE_VIEWED,
                ArticleViewedEventPayload.builder()
                        .articleId(articleId)
                        .articleViewCount(viewCount)
                        .build(),
                articleId
        );
    }
}
