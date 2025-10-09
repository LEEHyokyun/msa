package kuke.board.hotarticle.service;

import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;
import kuke.board.hotarticle.repository.ArticleCreatedTimeRepository;
import kuke.board.hotarticle.repository.HotArticleListRepository;
import kuke.board.hotarticle.service.eventhandler.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
* 인기글 점수를 업데이트한다(Repository layer는 계산한 점수만 전달)
* 다른 layer에서 생성자 주입받기위해 Component 지정
* */
@Component
@RequiredArgsConstructor
public class HotArticleScoreUpdater {
    /*
    * 점수계산 및 해당 점수를 Redis에 업데이트
    * */
    private final HotArticleListRepository hotArticleListRepository;
    private final HotArticleScoreCalculator hotArticleScoreCalculator;
    /*
    * 내일 보여줄 인기글 집계 대상인지 확인
    * (=오늘 생성된 게시글인지 Redis에 있는 데이터로 확인)
    * */
    private final ArticleCreatedTimeRepository articleCreatedTimeRepository;

    /*
    * 인기글은 최대 10개
    * */
    private static final long HOT_ARTICLE_COUNT = 10;
    /*
    * 인기글 TTL = 7, 실제 저장은 일단은 이것보다는 좀 더 넉넉하게 저장
    * */
    private static final Duration HOT_ARTICLE_TTL = Duration.ofDays(10);

    /*
    * 점수 집계
    * */
    public void update(Event<EventPayload> event, EventHandler<EventPayload> eventHandler) {
        /*
        * eventHandler의 eventPayload에서 article 정보 추출
        * */
        Long articleId = eventHandler.findArticleId(event);

        /*
        * 기본적으로 해당 아티클이 인기글 집계 대상인가 확인
        * (오늘 날짜에 생성되었는지 확인)
        * */
        LocalDateTime createdTime = articleCreatedTimeRepository.read(articleId);

        if (!isArticleCreatedToday(createdTime)) {
            return;
        }

        /*
        * Handler = 지금 받아온 이벤트 내역을 저장
        * */
        eventHandler.handle(event);

        /*
        * article id / score
        * - 해당 시점에 그 게시글의 모든 정보를 실시간으로 다 계산 (=Redis)
        * */
        long score = hotArticleScoreCalculator.calculate(articleId);
        /*
        * 점수 신규
        * "생성 혹은 update"
        * (*누적이 아님! 실시간 바로 반영하는 방식)
        * */
        hotArticleListRepository.add(
                articleId,
                createdTime,
                score,
                HOT_ARTICLE_COUNT,
                HOT_ARTICLE_TTL
        );
    }

    private boolean isArticleCreatedToday(LocalDateTime createdTime) {
        return createdTime != null && createdTime.toLocalDate().equals(LocalDate.now());
    }
}
