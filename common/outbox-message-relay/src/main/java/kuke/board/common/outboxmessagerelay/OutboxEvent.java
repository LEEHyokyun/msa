package kuke.board.common.outboxmessagerelay;

import lombok.Getter;
import lombok.ToString;

/*
* outbox 객체를 통해 이벤트를 전달하기 위함
* "outbox event"로 별도 생성
* */
@Getter
@ToString
public class OutboxEvent {
    private Outbox outbox;

    public static OutboxEvent of(Outbox outbox) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.outbox = outbox;
        return outboxEvent;
    }
}
