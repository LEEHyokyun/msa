package kuke.board.common.event;

import kuke.board.common.dataserializer.DataSerializer;
import lombok.Getter;

/*
* Event 통신을 위한 클래스
* - 이벤트에 대해 eventId를 기반으로 구별한다.
* - 이벤트 객체를 생성한다(*Kafka로 전달할때 Json 직렬화 및 데이터 받아올때 역직렬화 필요).
* - 이벤트가 어떤 타입인지 나타낸다.
* - 이벤트가 어떤 데이터를 가지고 있는지 나타낸다.
* */
@Getter
public class Event<T extends EventPayload> {
    private Long eventId;
    private EventType type;
    private T payload;

    /*
    * 이벤트 객체 생성
    * */
    public static Event<EventPayload> of(Long eventId, EventType type, EventPayload payload) {
        Event<EventPayload> event = new Event<>();
        event.eventId = eventId;
        event.type = type;
        event.payload = payload;
        return event;
    }

    /*
    * Kafka 통신을 위한 직렬화
    * Event Class -> Json
    * */
    public String toJson() {
        return DataSerializer.serialize(this);
    }

    /*
    * Kafka 통신을 위한 역직렬화(역직렬화 후 Json 정보에서 EventRaw 정보를 받아오고 이를 Event 객체화)
    * Json -> Event
    * */
    public static Event<EventPayload> fromJson(String json) {
        EventRaw eventRaw = DataSerializer.deserialize(json, EventRaw.class);
        if (eventRaw == null) {
            return null;
        }
        Event<EventPayload> event = new Event<>();
        event.eventId = eventRaw.getEventId();
        event.type = EventType.from(eventRaw.getType());
        event.payload = DataSerializer.deserialize(eventRaw.getPayload(), event.type.getPayloadClass());
        return event;
    }

    /*
    * Type에 따라 Payload의 클래스를 다르게 구성하기 위함
    * Payload 클래스를 일단 구별하기 위해 type / Object(payload)를 매개변수로 구성
    * */
    @Getter
    private static class EventRaw {
        private Long eventId;
        private String type;
        private Object payload;
    }
}
