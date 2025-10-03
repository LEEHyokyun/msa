package kuke.board.view.api;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewApiTest {
    RestClient restClient = RestClient.create("http://localhost:9003");

    @Test
    void viewTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(10000);

        /*
        * 조회수 증가와 백업이 모두 하나의 API에서 이루어짐에 유의
        * */
        for(int i=0; i<10000; i++) {
            executorService.submit(() -> {
                restClient.post()
                        .uri("/v1/article-views/articles/{articleId}/users/{userId}", 5L, 1L)
                        .retrieve();
                latch.countDown();
            });
        }

        latch.await();

        Long count = restClient.get()
                .uri("/v1/article-views/articles/{articleId}/count", 5L)
                .retrieve()
                .body(Long.class);

        /*
        * lock 해제 - count = 1001
        * lock 정상 - count = 1
        * */
        System.out.println("count = " + count);
    }
}
