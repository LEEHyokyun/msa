package kuke.board.like;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EntityScan(basePackages = "kuke.board")
@SpringBootApplication
@EnableJpaRepositories(basePackages = "kuke.board")
@EnableScheduling
public class LikeApplication {
    public static void main(String[] args) {
        SpringApplication.run(LikeApplication.class, args);
    }
}
