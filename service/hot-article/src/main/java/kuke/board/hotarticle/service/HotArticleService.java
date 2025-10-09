package kuke.board.hotarticle.service;

import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;
import kuke.board.common.event.EventType;
import kuke.board.hotarticle.client.ArticleClient;
import kuke.board.hotarticle.repository.HotArticleListRepository;
import kuke.board.hotarticle.service.eventhandler.EventHandler;
import kuke.board.hotarticle.service.response.HotArticleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor //for final member variables.
public class HotArticleService {
    /*
    * articleClient : 게시글 정보까지 모두 조회 후 추출할 수 있도록
    * */
    private final ArticleClient articleClient;
    /*
    * 이벤트핸들러(List)
    * Event 객체를 매개변수로 전달받아 해당하는 이벤트핸들러를 매핑해주기 위함
    * 참고로 Spring Framework에 의해 해당 리스트의 생성자 주입 시점에 해당 구현체들이 주입되어 할당됨
    * */
    private final List<EventHandler> eventHandlers;
    /*
    * 인기글 처리하기 전에 인기글 집계를 위해 인기글 점수 계산
    * */
    private final HotArticleScoreUpdater hotArticleScoreUpdater;
    /*
    * 인기글 조회
    * */
    private final HotArticleListRepository hotArticleListRepository;

    /*
    * Event를 매개변수로 전달받아 이벤트에 대응하는 eventHandler 찾기
    * 참고로 eventHandler 리스트는 이미 구현체들이 주입된 상태이므로 별도 선언과정 필요없음
    * */
    public void handleEvent(Event<EventPayload> event) {
        EventHandler<EventPayload> eventHandler = findEventHandler(event);
        /*
        * 처리가능한 이벤트핸들러가 없으면 그대로 종료
        * */
        if (eventHandler == null) {
            return;
        }

        /*
        * 게시글 생성 및 삭제 이벤트일 경우엔 점수 집계 대상이 아님
        * 목록만 신규 생성해주거나 삭제
        * */
        if (isArticleCreatedOrDeleted(event)) {
            eventHandler.handle(event);
        } else {
            /*
            * 그 이외 경우는 이미 점수가 생성된 상태..update만 진행
            * */
            hotArticleScoreUpdater.update(event, eventHandler);
        }
    }

    /*
     * Event를 매개변수로 전달받아 이벤트에 대응하는 eventHandler 찾기
     *
     * */
    private EventHandler<EventPayload> findEventHandler(Event<EventPayload> event) {
        return eventHandlers.stream()
                .filter(eventHandler -> eventHandler.supports(event))
                .findAny()
                /*
                * Wrapper -> Object
                * */
                .orElse(null);
    }

    private boolean isArticleCreatedOrDeleted(Event<EventPayload> event) {
        return EventType.ARTICLE_CREATED == event.getType() || EventType.ARTICLE_DELETED == event.getType();
    }

    /*
    * 조회(dateStr = YYYYMMDD)
    * - Redis에 있는 인기글 id를 조회하여
    * - RestClient로 원본 데이터 내용을 추출하여 리스트화
    * */
    public List<HotArticleResponse> readAll(String dateStr) {
        return hotArticleListRepository.readAll(dateStr).stream()
                .map(articleClient::read)
                .filter(Objects::nonNull) //data none null -> filtering
                .map(HotArticleResponse::from)
                .toList();
    }
}
