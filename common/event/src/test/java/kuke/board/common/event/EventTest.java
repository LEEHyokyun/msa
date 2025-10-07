package kuke.board.common.event;

import kuke.board.common.event.payload.ArticleCreatedEventPayload;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    /*
    * Kafka 통신을 위해
    * Event 객체를 생성하는 Json 직렬화 과정
    * Event 객체의 Json을 다시 Event로 역직렬화하여
    * Kafka 통신을 하기 위한 준비가 되었는지 확인
    * */
    @Test
    void serde() {
        // given
        ArticleCreatedEventPayload payload = ArticleCreatedEventPayload.builder()
                .articleId(1L)
                .title("title")
                .content("content")
                .boardId(1L)
                .writerId(1L)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .boardArticleCount(23L)
                .build();

        Event<EventPayload> event = Event.of(
                1234L,
                EventType.ARTICLE_CREATED,
                payload
        );

        /*
        * kafka로 보내기 위해 Event 객체를 Json으로 직렬화
        * (*Kafka가 데이터로그에 쌓는 메시지의 형태는 Json 등의 문자열 타입)
        * */
        String json = event.toJson();
        System.out.println("json = " + json);

        // when
        /*
        * 위에서 보낸 Json 문자열을 Event 객체로 역직렬화
        * */
        Event<EventPayload> result = Event.fromJson(json);

        // then
        assertThat(result.getEventId()).isEqualTo(event.getEventId());
        assertThat(result.getType()).isEqualTo(event.getType());
        assertThat(result.getPayload()).isInstanceOf(payload.getClass());

        ArticleCreatedEventPayload resultPayload = (ArticleCreatedEventPayload) result.getPayload();
        assertThat(resultPayload.getArticleId()).isEqualTo(payload.getArticleId());
        assertThat(resultPayload.getTitle()).isEqualTo(payload.getTitle());
        assertThat(resultPayload.getCreatedAt()).isEqualTo(payload.getCreatedAt());
    }
}