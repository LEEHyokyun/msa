package kuke.board.articleread.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Map;

/*
* CacheConfig가 주 도메인 MSA의 패키지 안에 있는 @Configuration 클래스라면,
* Spring Boot 애플리케이션 시작 시 컴포넌트 스캔 대상에 포함한다.
* 스프링 부트는 @SpringBootApplication(혹은 @ComponentScan) 기준으로 기본 패키지와 그 하위 패키지를 모두 스캔
* */
/*
* 조회서비스에 트래픽이 병목할 경우를 대비하여,
* Redis 캐시를 사용하여 조회기능을 최적화하기 위해 캐싱기능 추가
* */
@Configuration
@EnableCaching
public class CacheConfig {
    /*
    * Configuration -> Bean 등록, 이후 스프링 컨테이너에서 사용가능하도록 / 객체활용가능하도록 한다.
    * */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                /*
                * 빠른 시간 내 조회수를 캐싱하여 최대한 빠르게 데이터를 추출해오기 위함
                * 너무 길면 실시간 반영 힘듦.
                * TTL = 1초.
                *
                * Configuration -> Application Context에 등록하여 redisCacheManger 빈을 초기화 및 등록
                *               -> 환경설정 정보를 읽고, 컨테이너가 해당 환경대로 후에 cache AOP 동작을 진행하고 Redis와 상호작용할 수 있도록 함.
                *               -> 말 그대로 환경설정을 위함
                * EnableCaching -> Cache Interceptor / Advisor 생성하여 메서드 실행 시 AOP동작 및 CacheEvict 등의 메타정보를 읽도록 한다.
                *               -> Configuration 설정정보를 기반으로 Cachable 등의 Bean을 감지하고 읽어서 AOP 동작을 완료하도록 한다.
                *               -> key : articleViewCount::#articleId(매개변수) / value : articleViewCount(실제 해당 메서드의 반환값)
                * */
                .withInitialCacheConfigurations(
                        Map.of(
                                "articleViewCount", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(1))
                        )
                )
                .build();
    }
}
