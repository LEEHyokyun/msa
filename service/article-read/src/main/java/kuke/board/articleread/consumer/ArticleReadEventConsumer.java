package kuke.board.articleread.consumer;

import kuke.board.articleread.service.ArticleReadService;
import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;
import kuke.board.common.event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/*
* Produce가 발행한 이벤트를 처리하는 책임
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleReadEventConsumer {
    private final ArticleReadService articleReadService;

    /*
    * Consuming 대상 이벤트 : ARTICLE / COMMENT / LIKE .. 쓰기대상
    * Consuming 로직은 별도 MSA를 구성하여 처리하는 것이 좋을 듯하다(본 도메인은 조회대상이므로)
    * 또한 처리자체가 효율이 떨어지는 부분 발생..appliation 20개이고 topic 파티션이 이보다 적다면
    * 5개 파티션을 처리할 컨슈머 5개는 이를 처리하지만, 나머지 15개가 그대로 유휴상태로 되어 효율 매우 떨어짐
    * 카프카 설정 부분도 고려해야할 듯하다.
    * */
    @KafkaListener(topics = {
            EventType.Topic.KUKE_BOARD_ARTICLE,
            EventType.Topic.KUKE_BOARD_COMMENT,
            EventType.Topic.KUKE_BOARD_LIKE
    })
    public void listen(String message, Acknowledgment ack) {
        log.info("[ArticleReadEventConsumer.listen] message={}", message);
        Event<EventPayload> event = Event.fromJson(message);
        if (event != null) {
            articleReadService.handleEvent(event);
        }
        ack.acknowledge();
    }
}
