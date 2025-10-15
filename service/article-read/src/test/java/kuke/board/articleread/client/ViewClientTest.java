package kuke.board.articleread.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class ViewClientTest {
    @Autowired
    ViewClient viewClient;

    /*
    * 캐싱 동작에 대한 테스트
    * */
    @Test
    void readCacheableTest() throws InterruptedException {
        viewClient.count(1L); // 로그 출력(캐시 데이터가 없으므로)
        viewClient.count(1L); // 로그 미출력(캐시 데이터가 있으므로)
        viewClient.count(1L); // 로그 미출력(캐시 데이터가 있으므로)

        TimeUnit.SECONDS.sleep(3);
        viewClient.count(1L); // 로그 출력(캐시 데이터가 없으므로(만료))
    }

    /*
    * 5개의 스레드 풀의 동시요청
    * */
    @Test
    void readCacheableMultiThreadTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        viewClient.count(1L); // init cache

        for(int i=0;i <5; i++) {
            CountDownLatch latch = new CountDownLatch(5);
            /*
            * 각 스레드풀 별 5번의 요청으로 동시성 상황을 가정한다.
            * 문제상황발생 - 동시에 많은 조회요청이 발생할 경우 최초 캐싱데이터 존재 시에는 괜찮지만, 캐싱 만료 후 데이터가 없을때는 직접 원본데이터를 요청해야 하는 상황
            *            - 이로인해 성능/비용적 소모가 발생한다.
            *
            * */
            for(int j=0;j<5;j++) {
                executorService.submit(() -> {
                    viewClient.count(1L);
                    latch.countDown();
                });
            }
            latch.await();
            TimeUnit.SECONDS.sleep(2);
            System.out.println("=== cache expired ===");
        }
    }
}