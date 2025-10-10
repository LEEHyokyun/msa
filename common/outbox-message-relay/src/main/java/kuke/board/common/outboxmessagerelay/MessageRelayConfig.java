package kuke.board.common.outboxmessagerelay;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/*
* MessageRelayConfig는 Async/Scheduling 환경을 켜주고, 컴포넌트 스캔을 통해 관련 클래스들을 빈으로 등록한다.
* 그 결과, 스캔된 클래스들 안의 @Async, @Scheduled 메서드가 실제로 비동기/주기적 실행을 하게 된다.
* Async : 비동기로 처리하기 위함
* EnableScheduling : 주기적으로 동작하도록 하기 위함
* Component : Bean 정의 클래스임을 명기하고, CGLIB 프록시 적용 (싱글톤 보장) 및 다른 설정 클래스와의 상호작용을 위함(Import 가능)
*
* Spring Boot auto-configuration (starter 패턴)
* - INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
* - 이를 통해 Spring Boot 측에서 해당 Config 파일을 로딩하고 이후 컴포넌트 스캔이 발생, 위의 환경을 구성 가능
* */
@EnableAsync
@Configuration
@ComponentScan("kuke.board.common.outboxmessagerelay")
@EnableScheduling
public class MessageRelayConfig {
    /*
    * Kafka 서버
    * */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /*
    * Bean 정의 클래스(Configuration)
    * - Kafka에게 이벤트를 전송하기 위해 필요한 Producer 설정 정보 객체를 생성
    * = return KafkaTemplate (이 Bean 객체를 다른 모듈에서 사용할 수 있도록)
    * */
    @Bean
    public KafkaTemplate<String, String> messageRelayKafkaTemplate() {
        Map<String, Object> configProps = new HashMap<>();
        /*
        * Kafka와 통신하기 위한 기본 설정
        * */
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        /*
        * Producer > Kafka 에 대한 설정으로, String을 Kafka Broker가 이해할 수 있는 byte로 바꾸겠다는 것.
        * */
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        /*
        * Kafka에 메시지 전송 시 성공처리에 대한 기준 별도 구성
        * ISR 사용하기 위한 설정
        * */
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configProps));
    }

    /*
     * Bean 정의 클래스(Configuration)
     * - application 실행 시 message relay polling을 동작하도록 하기 위한 객체
     * - 즉, 이벤트 메시지를 비동기로 전송하기 위한 비동기 스레드 풀 환경을 만든다.
     * */
    @Bean
    public Executor messageRelayPublishEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mr-pub-event-");
        return executor;
    }

    /*
    * 서비스 요청 후 10초 이후에 주기적으로 polling을 진행하기 위함
    * - application마다 샤드 할당 후 polling 할 것이므로 Single Executor로 polling 하도록 함
    * */
//    @Bean
//    public Executor messageRelayPublishPendingEventExecutor() {
//        return Executors.newSingleThreadScheduledExecutor();
//    }

    /*
    * [2025/09/26]
    * Spring 최신 버전에서는 Configuration의 빈 type을 Executor가 아닌 TaskScheduler로 반환해주어야 한다.
    * 서비스 요청 후 10초 이후에 주기적으로 polling을 진행하기 위함
    * - application마다 샤드 할당 후 polling 할 것이므로 Single Executor로 polling 하도록 함
    * */
    @Bean
    public TaskScheduler messageRelayPublishPendingEventExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("task-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
