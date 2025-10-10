package kuke.board.common.outboxmessagerelay;

import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;
import kuke.board.common.event.EventType;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/*
* outbox 이벤트를 생성하는 퍼블리셔(이벤트 발행자)
* */
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private final Snowflake outboxIdSnowflake = new Snowflake();
    private final Snowflake eventIdSnowflake = new Snowflake();
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(EventType type, EventPayload payload, Long shardKey) {
        /*
        * article id = 10, shard key = 10 (기본적으로 동일샤드에 저장하기 위함, 논리샤드)
        * 10 % 4 = 2, outbox를 처리할 application에 outbox가 들어있는 샤드를 할당(물리샤드)
        * */
        Outbox outbox = Outbox.create(
                outboxIdSnowflake.nextId(),
                type,
                Event.of(
                        eventIdSnowflake.nextId(), type, payload
                ).toJson(),
                shardKey % MessageRelayConstants.SHARD_COUNT
        );
        /*
        * outbox 이벤트를 만들어 이벤트 발행
        * application 차원에서 이벤트를 발행하는 단계.
        * */
        applicationEventPublisher.publishEvent(OutboxEvent.of(outbox));
    }
}
