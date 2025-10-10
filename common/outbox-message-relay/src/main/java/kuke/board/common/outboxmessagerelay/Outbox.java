package kuke.board.common.outboxmessagerelay;

import jakarta.persistence.*;
import kuke.board.common.event.EventType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/*
* outbox 엔티티
* */
@Table(name = "outbox")
@Getter
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {
    @Id
    private Long outboxId;
    /*
    * DB에 저장되는 형식을 제한하는 것 (enum type을 String으로 저장하며, 반드시 enum에 정의해야 저장 가능)
    * 제한 효과는 DB가 아니라 애플리케이션 코드 차원에서 일어남
    * (즉, eventType은 무조건 EventType enum에 정의된 값 중 하나여야만 한다)
    * + 메타데이터 겸하기도 한다.
    * cf. Embedded, Embedable.
    * */
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    private String payload;
    private Long shardKey;
    private LocalDateTime createdAt;

    /*
    * 엔티티 생성 책임 = 도메인 책임
    * */
    public static Outbox create(Long outboxId, EventType eventType, String payload, Long shardKey) {
        Outbox outbox = new Outbox();
        outbox.outboxId = outboxId;
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.shardKey = shardKey;
        outbox.createdAt = LocalDateTime.now();
        return outbox;
    }
}
