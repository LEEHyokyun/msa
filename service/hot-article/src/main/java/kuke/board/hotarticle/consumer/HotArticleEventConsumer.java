package kuke.board.hotarticle.consumer;

import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;
import kuke.board.common.event.EventType;
import kuke.board.hotarticle.service.HotArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor //for final
public class HotArticleEventConsumer {
    private final HotArticleService hotArticleService;

    /*
    * Kafka에서 발행하는 메시지를 구독하여 Consume
    * message를 역직렬화하여 최종적으로 Service에서 이를 처리하도록 한다.
    * */
    @KafkaListener(topics = {
            EventType.Topic.KUKE_BOARD_ARTICLE,
            EventType.Topic.KUKE_BOARD_COMMENT,
            EventType.Topic.KUKE_BOARD_LIKE,
            EventType.Topic.KUKE_BOARD_VIEW
    })
    public void listen(String message, Acknowledgment ack) {
        log.info("[HotArticleEventConsumer.listen] received message={}", message);
        Event<EventPayload> event = Event.fromJson(message);
        if (event != null) {
            /*
            * 이벤트 역직렬화 후 처리한다.
            * */
            hotArticleService.handleEvent(event);
        }
        /*
        * 메시지 처리 완료, Kafka 측에 성공 응답
        * 이 이후에 commit이 이루어져야 하므로 auto-commit = false로 설정하는 것이다.
        * offset commit
        * */
        ack.acknowledge();
    }
}
