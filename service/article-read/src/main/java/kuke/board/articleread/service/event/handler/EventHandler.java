package kuke.board.articleread.service.event.handler;

import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;

/*
* event handler의 일원화 및 유지관리를 위해 인터페이스화(factory pattern)
*
* */
public interface EventHandler<T extends EventPayload> {
    /*
    * 이벤트 다루기
    * 이벤트 지원여부
    * */
    void handle(Event<T> event);
    boolean supports(Event<T> event);
}
