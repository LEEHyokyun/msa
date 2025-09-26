package kuke.board.comment.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kuke.board.comment.entity.Comment;
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

    static final int BULK_INSERT_SIZE = 2000;
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

    void insert() {
        /*
        * 짝수 > 자식 댓글 없음, 자기 자신이 부모 댓글
        * 홀수 > 이전의 코멘트를 부모 댓글로 구성
        * */
        transactionTemplate.executeWithoutResult(status -> {
            Comment prev = null;
            for(int i = 0; i < BULK_INSERT_SIZE; i++) {
                Comment comment = Comment.create(
                        snowflake.nextId(),
                        "content",
                        i % 2 == 0 ? null : prev.getCommentId(),
                        1L,
                        1L
                );
                prev = comment;
                entityManager.persist(comment);
            }
        });
    }
}
