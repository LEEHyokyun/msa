package kuke.board.hotarticle.service;

import kuke.board.common.event.Event;
import kuke.board.common.event.EventType;
import kuke.board.hotarticle.service.eventhandler.EventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/*
* 단위테스트
* */
@ExtendWith(MockitoExtension.class)
class HotArticleServiceTest {
    @InjectMocks
    HotArticleService hotArticleService;
    /*
    * 테스트 대상의 멤버변수 리스트
    * 격리된 테스트 환경에서 Mock 객체를 list로 주입하기 위해 Mock화
    * */
    @Mock
    List<EventHandler> eventHandlers;
    @Mock
    HotArticleScoreUpdater hotArticleScoreUpdater;

    /*
    * key point : 조건에 따라 특정 행동을 호출하였는가?
    * */

    /*
    * 이벤트에 맞는 핸들러가 없을 경우(Mock 검증)
    * */
    @Test
    void handleEventIfEventHandlerNotFoundTest() {
        // given
        Event event = mock(Event.class);
        EventHandler eventHandler = mock(EventHandler.class);
        /*
        * 비어있는 stream 반환 = list도 같이 mock화
        * */
        given(eventHandler.supports(event)).willReturn(false);
        given(eventHandlers.stream()).willReturn(Stream.of(eventHandler)); //will return mock

        // when
        hotArticleService.handleEvent(event);

        // then
        verify(eventHandler, never()).handle(event); //Mock에 따라 핸들 이벤트는 처리 불가
        verify(hotArticleScoreUpdater, never()).update(event, eventHandler); //update 이벤트 처리 불가
    }

    /*
     * 이벤트에 맞는 핸들러가 있을 경우(Mock 검증)
     * */
    @Test
    void handleEventIfArticleCreatedEventTest() {
        // given
        Event event = mock(Event.class);
        given(event.getType()).willReturn(EventType.ARTICLE_CREATED);

        EventHandler eventHandler = mock(EventHandler.class);
        given(eventHandler.supports(event)).willReturn(true);
        given(eventHandlers.stream()).willReturn(Stream.of(eventHandler));

        // when
        hotArticleService.handleEvent(event);

        // then
        verify(eventHandler).handle(event);
        verify(hotArticleScoreUpdater, never()).update(event, eventHandler);
    }

    @Test
    void handleEventIfArticleDeletedEventTest() {
        // given
        Event event = mock(Event.class);
        given(event.getType()).willReturn(EventType.ARTICLE_DELETED);

        EventHandler eventHandler = mock(EventHandler.class);
        given(eventHandler.supports(event)).willReturn(true);
        given(eventHandlers.stream()).willReturn(Stream.of(eventHandler));

        // when
        hotArticleService.handleEvent(event);

        // then
        verify(eventHandler).handle(event);
        verify(hotArticleScoreUpdater, never()).update(event, eventHandler);
    }

    @Test
    void handleEventIfScoreUpdatableEventTest() {
        // given
        Event event = mock(Event.class);
        given(event.getType()).willReturn(mock(EventType.class));

        /*
         * 비어있는 stream 반환 = list도 같이 mock화
         * */
        EventHandler eventHandler = mock(EventHandler.class);
        given(eventHandler.supports(event)).willReturn(true);
        given(eventHandlers.stream()).willReturn(Stream.of(eventHandler));

        // when
        hotArticleService.handleEvent(event);

        // then
        verify(eventHandler, never()).handle(event); //true -> handle 이벤트는 호출안됨 update 이벤트는 호출
        verify(hotArticleScoreUpdater).update(event, eventHandler);
    }
}