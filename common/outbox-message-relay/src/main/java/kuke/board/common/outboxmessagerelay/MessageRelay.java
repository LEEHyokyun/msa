package kuke.board.common.outboxmessagerelay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
* 최종적으로, 미전송/실패 메시지를 추출하여 Kafka Broker로 전송하는 역할 담당
* */
@Slf4j
@Component
@RequiredArgsConstructor //for final
public class MessageRelay {
    private final OutboxRepository outboxRepository;
    private final MessageRelayCoordinator messageRelayCoordinator;
    private final KafkaTemplate<String, String> messageRelayKafkaTemplate;

    /*
    * 트랜잭션 이벤트에 대한 처리(커밋 전)
    * - Application Context(=Event)와 트랜잭션을 서로 연결하여
    * - 비즈니스 로직에서 발생한 이벤트와 그 비즈니스 로직의 트랜잭션 컨텍스트를 서로 연결
    * - 트랜잭션 이벤트 phase(HOOK)와 이에 대한 감지(리스너)를 기반으로 동작이 이루어진다.
    * = Outbox에 이벤트 메시지를 기록.
    * */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createOutbox(OutboxEvent outboxEvent) {
        log.info("[MessageRelay.createOutbox] outboxEvent={}", outboxEvent);
        outboxRepository.save(outboxEvent.getOutbox());
    }

    /*
    * 커밋 후에(트랜잭션 컨텍스트 종료 후 별도 트랜잭션 컨텍스트에서 실행)
    * 비동기로 Kafka Broker에게 이벤트 메시지를 전달한다.
    * KafkaTemplate을 사용하여 전송
    * - outbox 메시지 전송
    * - 동일 key = 순차보장, 전송 성공 시 outbox에서 해당 메시지 제거
    * */
    @Async("messageRelayPublishEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(OutboxEvent outboxEvent) {
        publishEvent(outboxEvent.getOutbox());
    }

    private void publishEvent(Outbox outbox) {
        try {
            /*
            * 비동기처리가 아닌 트랜잭션 처리 이후의 동기적인 처리로
            * 최초 서비스 요청 후 이벤트 발행, 해당 이벤트 발행 성공할 경우 즉각적인 outbox 메시지 삭제가 발생한다.
            * */
            /*
            * 동일 topic으로 partition에 메시지가 분산되어 붙는데
            * key가 동일하다면 동일한 partition에 붙어 이때 순차처리를 보장할 수 있다.
            * payload = data
            * */
            messageRelayKafkaTemplate.send(
                    outbox.getEventType().getTopic(), //topic
                    String.valueOf(outbox.getShardKey()), //key
                    outbox.getPayload() //data
                    /*
                    * Completable Future 반환
                    * 결과대기
                    * */
            ).get(1, TimeUnit.SECONDS);
            /*
            * 메시지 전달 성공 시 그대로 해당 이벤트 메시지를 outbox에서 제거
            * */
            outboxRepository.delete(outbox);
        } catch (Exception e) {
            log.error("[MessageRelay.publishEvent] outbox={}", outbox, e);
        }
    }


    /*
     * 주기적으로 outbox에 polling하여 미전송 혹은 전송실패한 이벤트 메시지를 비동기로 Kafka Broker에 전송하는 스케쥴러
     * fixedDelay = 10, 스케쥴링 메서드 종료 후 다음 실행까지 10초 대기시간(주기)
     * initialDelay = 5, 최초 Application 초기화 후 스케쥴러 동작 시 대기시간
     * */
    @Scheduled(
            fixedDelay = 10,
            initialDelay = 5,
            timeUnit = TimeUnit.SECONDS,
            scheduler = "messageRelayPublishPendingEventExecutor"
    )
    public void publishPendingEvent() {
        /*
        * 본인 앱에 맞는 샤드 목록 추출
        *
        * */
        AssignedShard assignedShard = messageRelayCoordinator.assignShards();
        log.info("[MessageRelay.publishPendingEvent] assignedShard size={}", assignedShard.getShards().size());
        for (Long shard : assignedShard.getShards()) {
            /*
            * 할당받은 샤드 기준으로 polling.
            * 10초 동안 남아있는, 즉 10초 동안 전송을 실패한 메시지들에 대해
            * 최대 100개 만큼 조회
            * */
            /*
            * 데이터 처리의 공백 문제?
            * <= 조건 때문에 “공백 발생” 우려는 없음(=JPA에서 지원해주는 기능).
            * Scheduler가 실패하더라도, 다음 실행에서 10초 이전까지의 모든 메시지를 다시 조회하고 처리하므로 결국 재시도됨.
            * 즉, 데이터 누락 위험이 없음.
            * 다만, Pageable.ofSize(100) 때문에 한 번 조회 시 최대 100개만 처리 가능 → 메시지가 100개 이상 누적될 경우 다음 스케줄까지 남은 메시지가 유지
            * 이건 의도적인 제한으로, 한 번에 대량 처리로 인한 부하를 막기 위함
            * */
            List<Outbox> outboxes = outboxRepository.findAllByShardKeyAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                    shard,
                    LocalDateTime.now().minusSeconds(10),
                    Pageable.ofSize(100)
            );
            /*
            * 조회해온 리스트에 대해 그대로 이벤트를 Kafka로 전송
            * */
            for (Outbox outbox : outboxes) {
                publishEvent(outbox);
            }
        }
    }
}
