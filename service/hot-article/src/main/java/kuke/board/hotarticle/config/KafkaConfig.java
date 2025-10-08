package kuke.board.hotarticle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaConfig {
    /*
    * 메시지를 소비하는 Consumer를 구성하고 동작하는 전략을 구성해줄 수 있는 설정 클래스.
    * ConsumerFactory: application.yml 기반으로 KafkaConsumer 인스턴스를 만들어주는 설정
    * ConcurrentKafkaListenerContainerFactory: Consumer를 실행하는 ListenerContainer를 생성하는 공장 → 동작 전략(AckMode, concurrency 등)을 커스터마이징할 수 있음.
    * KafkaConfig 클래스: 이 Factory를 Bean으로 등록해, @KafkaListener가 어떤 실행 전략으로 메시지를 소비할지 지정.
    * */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); //manual commit
        return factory;
    }
}
