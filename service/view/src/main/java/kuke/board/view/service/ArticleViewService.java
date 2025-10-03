package kuke.board.view.service;

import kuke.board.view.repository.ArticleViewCountRepository;
import kuke.board.view.repository.ArticleViewDistributedLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/*
* RedisCountRepository를 활용한 조회수 서비스(*백업까지)
* */
@Service
@RequiredArgsConstructor
public class ArticleViewService {
    private final ArticleViewCountRepository articleViewCountRepository;
    private final ArticleViewCountBackUpProcessor articleViewCountBackUpProcessor;
    private final ArticleViewDistributedLockRepository articleViewDistributedLockRepository;

    /*
    * 개수단위백업 - Redis에 해당 데이터 개수가 가득차면, 조회시점에 이를 파악하고 백업 진행
    * 그 개수에 대해 정의
    * */
    private static final int BACK_UP_BACH_SIZE = 100;
    private static final Duration TTL = Duration.ofMinutes(10);

    /*
    * 단순 조회수 증가
    * */
    public Long increaseInit(Long articleId, Long userId) {
        return articleViewCountRepository.increase(articleId);
    }

    /*
    * 데이터 백업까지
    * */
    public Long increase(Long articleId, Long userId) {
        /*
        * 분산락 획득 실패시 증가처리를 하지 않고 현재 조회수 그대로 반환
        * */
        if (!articleViewDistributedLockRepository.lock(articleId, userId, TTL)) {
            return articleViewCountRepository.read(articleId);
        }

        /*
        * 조회수 처리
        * */
        Long count = articleViewCountRepository.increase(articleId);
        /*
        * 백업개수 도달 시 백업 진행
        * */
        if (count % BACK_UP_BACH_SIZE == 0) {
            articleViewCountBackUpProcessor.backUp(articleId, count);
        }
        return count;
    }

    /*
    * 조회수 읽어오기
    * */
    public Long count(Long articleId) {
        return articleViewCountRepository.read(articleId);
    }
}
