package kuke.board.like;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * 보통은 이러한 main 모듈에서 컴포넌트 스캔을 해주고 스케쥴링 환경 구성(EnableScheduling)해주는 것이 일반적임.
 * - Repository : DB Exception 추상화하여 예외처리를 위함, EnableJpaRepository를 통해 프록시 빈 생성 필요함.
 * - EnableScheduling : 역시 마찬가지로 보통은 main 모듈에서 스케쥴링 환경을 구성해주고 스케쥴러 등록만 별도 해주는 것이 편하다.
 * - Entity는 스캔 대상이 아니다, 마찬가지로 main 모듈에서 entity scan을 해주어야 편하다.
 * */
@EntityScan(basePackages = "kuke.board")
@SpringBootApplication
@EnableJpaRepositories(basePackages = "kuke.board")
@EnableScheduling
public class LikeApplication {
    public static void main(String[] args) {
        SpringApplication.run(LikeApplication.class, args);
    }
}
