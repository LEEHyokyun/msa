package kuke.board.article.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kuke.board.article.entity.Article;
import kuke.board.common.snowflake.Snowflake;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class DataInitializer {
    @PersistenceContext
    EntityManager entityManager;
    @Autowired
    TransactionTemplate transactionTemplate;
    Snowflake snowflake = new Snowflake();
    CountDownLatch latch = new CountDownLatch(EXECUTE_COUNT);

    /*
    * 1번에 넣을 DATA
    * = 2000건
    * */
    static final int BULK_INSERT_SIZE = 2000;
    /*
    * DATA를 넣는 행위의 총 수행 횟수
    * = 6000번
    * */
    static final int EXECUTE_COUNT = 6000;


    @Test
    void initialize() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for(int i = 0; i < EXECUTE_COUNT; i++) {
            executorService.submit(() -> {
                insert();
                latch.countDown();
                System.out.println("latch.getCount() = " + latch.getCount());
            });
        }
        latch.await();
        executorService.shutdown();
    }

    /*
    * Test 데이터를 반영하기 위한 Java 로직/짚고넘어가야할 부분!
    * 1. 트랜잭션 종료와 JPA의 관계
       JPA(EntityManager) 자체는 DB에 직접 "바로" 쓰는 게 아니라, 영속성 컨텍스트라는 1차 캐시를 두고 엔티티 상태를 관리합니다.
       entityManager.persist(entity) → 엔티티를 "관리 대상(Managed)"으로 등록만 함.
       실제 **DB 반영(INSERT/UPDATE/DELETE SQL 실행)**은 보통 두 가지 타이밍에서 일어납니다:
       Flush 시점 (직접 entityManager.flush() 호출하거나, JPQL 실행 직전에 자동 flush 등)
       트랜잭션 커밋 시점 (스프링이 commit 호출하면서 JPA가 flush → DB 반영)

      2. transactionTemplate.executeWithoutResult(...)의 역할
       이게 **트랜잭션 경계(boundary)**를 잡아주는 장치.
       executeWithoutResult가 시작되면 Spring이 트랜잭션을 열고, 끝날 때 commit or rollback을 호출합니다.
       commit 시점에 JPA는 “영속성 컨텍스트의 변경 내역(스냅샷 비교 결과)”을 flush → DB 반영합니다.
    * */
    void insert() {
        /*
        * 트랜잭션의 경계를 나눈다.
        * = 1 Transaction Thread
        * */
        transactionTemplate.executeWithoutResult(status -> {
            for(int i = 0; i < BULK_INSERT_SIZE; i++) {
                Article article = Article.create(
                        snowflake.nextId(),
                        "title" + i,
                        "content" + i,
                        1L,
                        1L
                );
                entityManager.persist(article);
            }
        });
    }
}
