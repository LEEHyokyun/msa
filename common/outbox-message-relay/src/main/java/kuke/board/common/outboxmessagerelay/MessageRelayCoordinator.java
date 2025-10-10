package kuke.board.common.outboxmessagerelay;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/*
* 실행중인 앱을 추적하고 관리하는 책임
* root 프로젝트 실행 시 의존모듈이므로 같이 스캔 대상이 되어 Spring Boot Application context에 의해 스캔됨
* common 모듈의 자체적인 설정에 따라(start pattern) configuration 먼저 스캔 이후 컴포넌트 스캔됨
* */
@Component
@RequiredArgsConstructor
public class MessageRelayCoordinator {
    /*
    * 앱 실행목록은 Redis로 관리
    * */
    private final StringRedisTemplate redisTemplate;

    /*
    * 컴포넌트 스캔 시 Spring IoC 컨테이너 측에서는 이 클래스에 대해 빈객체 생성 및 생성자/필드 주입
    * 이 주입 시점에 MSA 환경에 따른 Value 주입이 일어난다.
    * */
    @Value("${spring.application.name}")
    private String applicationName;

    /*
    * 현재 실행중인 appID는 UUID로 생성
    * */
    private final String APP_ID = UUID.randomUUID().toString();

    /*
    * 중앙저장소 ping = 3초마다
    * ping 3번 실패 시 application 종료로 판단
    * */
    private final int PING_INTERVAL_SECONDS = 3;
    private final int PING_FAILURE_THRESHOLD = 3;

    /*
    * 본인의 appid, 현재 실행중인 app 목록들을 전달하여
    * 이 application 인스턴스가 할당받은 샤드 목록을 반환
    * */
    public AssignedShard assignShards() {
        return AssignedShard.of(APP_ID, findAppIds(), MessageRelayConstants.SHARD_COUNT);
    }

    /*
    * Redis 중앙저장소에서 현재 실행중인 앱 인스턴스 목록을 추출하여 가져온다.
    * 따라서 본인의 앱 인스턴스도 실행 시 바로 중앙저장소에 저장되어야 함을 숙지
    * 현재 샤드키/생성시간 오름차순 정렬..마지막 부터 모든 데이터(0~-1) 조회해온다.
    * */
    private List<String> findAppIds() {
        return redisTemplate.opsForZSet().reverseRange(generateKey(), 0, -1).stream()
                /*
                * 중앙저장소 정렬은 항상 바뀜 .. sorted를 통해 appid/생성시간 순 등 조회 시 동일하게 추출하기 위함
                * */
                .sorted()
                .toList();
    }

    /*
    * RelayConfig 클래스 "로드" -> @EnableScheduled 환경 구성
    * 컴포넌트 스캔 -> 컴포넌트 클래스를 빈으로 "등록" 후 "초기화"(생성자 및 필드 주입 일어나는 시점)
    * 이후 모든 빈 등록 및 초기화 완료 시 Application Spring Context 완전 초기화
    * 초기화 이후 Spring level에서 이제 등록된 스케쥴러를 본격적으로 실행 시작한다(=스케쥴링 메서드 감시 및 동작).
    * */
    /*
    * 스케쥴 작업 실행(주기 : 3초)
    * 최초 실행 시점 : ApplicationContext 초기화 이후 3초 후에.
    * ping "실패" 상황에 따른 삭제 로직 유의
    * */
    @Scheduled(fixedDelay = PING_INTERVAL_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void ping() {
        /*
        * redis 1번 통신으로 여러개의 연산 수행
        * */
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            /*
            * 각 과정을 이해함으로써 Redis 자료구조와 메서드를 정확히 이해할 수 있기에 과정별 작동과정을 기술
            [1-1. Redis 자료구조: Sorted Set]
            *ZSet(Key: String, Score: double, Member: String) 구조
            *Key: 앱 실행 목록을 식별하는 이름 (app0, app1, app2)
            *Member: 앱 식별자 (APP_ID — 보통 앱 인스턴스 고유값)
            *Score: 최근 ping 시각 (epoch 밀리초)
            *
            *예시
            *현재 시간: 1696920000000 (epoch ms)
            *APP_ID = "APP1"
            *
            *Key: app0
            *ZSet:
            *  "APP1" -> 1696920000000
            *즉, 각 앱의 마지막 ping 시각을 score로 저장함.
`           *
            [1-2. 실행 목록 등록 (ping)]
            *conn.zAdd(key, score, member)
            *앱이 주기적으로 실행되면서 ping을 보냄 → 현재 시각을 score로 저장.
            *이미 존재하면 score가 업데이트 됨 (즉, 마지막 ping 시각 갱신).
            *
            *| 시간(ms) | 동작      | ZSet 상태      |
            *| ------ | ------- | ------------ |
            *| 0      | APP1 등록 | APP1 -> 0    |
            *| 3000   | ping    | APP1 -> 3000 |
            *| 6000   | ping    | APP1 -> 6000 |
            *
            *
            *[1-3. 오래된 앱 제거]
            *conn.zRemRangeByScore(key, -inf, threshold)
            **score가 threshold 이하인 member 제거
            *threshold = 현재 시각 - (PING_INTERVAL * PING_FAILURE_THRESHOLD)
            *3초 * 3번 실패 → 9초보다 오래된 ping → 앱 종료로 간주
            *
            *원리:
            *ZSet의 score는 최근 ping 시각을 의미
            *오래된 score를 제거하면, 실행 중이 아닌 앱을 Redis에서 자동 제거 가능
            * */
            StringRedisConnection conn = (StringRedisConnection) action;
            String key = generateKey();
            conn.zAdd(key, Instant.now().toEpochMilli(), APP_ID);
            conn.zRemRangeByScore(
                    key,
                    Double.NEGATIVE_INFINITY,
                    Instant.now().minusSeconds(PING_INTERVAL_SECONDS * PING_FAILURE_THRESHOLD).toEpochMilli()
            );
            return null;
        });
    }

    /*
    *앱 종료 신호 발생
    *예: Ctrl+C, kill 신호, 컨테이너 종료
    *Spring 컨테이너가 Bean 소멸 처리 시작
    *@PreDestroy 메서드 자동 호출 → leave()
    *Redis ZSet에서 본인 APP_ID 제거
    *앱 종료 완료
    *ping 실패로 제거되는 것과는 별개로, 정상 종료 시에도 Redis를 깔끔하게 유지하는 용도
    * */
    @PreDestroy
    public void leave() {
        redisTemplate.opsForZSet().remove(generateKey(), APP_ID);
    }

    /*
    * 앱ID에 대한 key 생성
    * */
    private String generateKey() {
        return "message-relay-coordinator::app-list::%s".formatted(applicationName);
    }
}
