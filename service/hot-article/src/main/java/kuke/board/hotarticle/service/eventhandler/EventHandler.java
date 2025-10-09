package kuke.board.hotarticle.service.eventhandler;

import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;

/*
* Event가 발생하여 Listener에서 이를 읽고 처리하기 위한 핸들러
* 각 클래스의 행동을 정의해주는 Strategy pattern(전략패턴) 사용.
* */
/*
* 인터페이스 구현 시점에서 본인이 다룰 이벤트 객체 형태를 강제한다.
* 구현체가 반드시 "자기가 다룰 이벤트 타입"을 명시하도록 강제할 수 있기 때문.
* -> T : OK, ? : NOT OK.
* */
public interface EventHandler<T extends EventPayload> {
    /*
    * eventpayload를 받아서 이벤트를 처리한다.
    * */
    void handle(Event<T> event);
    /*
     * eventpayload를 받아서 핸들러 구현체가 해당 처리를 지원하는지 확인
     * */
    boolean supports(Event<T> event);
    /*
     * Event가 어떤 게시글에 대한 이벤트인지 알 수 있도록 확인
     * */
    Long findArticleId(Event<T> event);
}
