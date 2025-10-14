package kuke.board.articleread.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/*
* 다른 도메인에서 데이터 추출 시 사용하며, Service layer에서 이를 참조.
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentClient {
    private RestClient restClient;
    @Value("${endpoints.kuke-board-comment-service.url}")
    private String commentServiceUrl;

    /*
     * 참고로, PostConstruct는 모든 Bean 초기화 후 실행
     * 객체 생성 (Constructor 호출)
     * 필드 주입 / @Value 주입 / @Autowired 주입
     * 초기화 단계 (@PostConstruct 호출)
     * Bean 사용 (ApplicationContext에서 관리됨)
     * 소멸 단계 (@PreDestroy 호출)
     * 즉, @PostConstruct는
     * “스프링이 모든 의존성(@Value, @Autowired 등)을 주입 및 필드주입까지 모두 완료한 다음”
     * “이제 Bean이 완전히 준비되었으니 추가 초기화를 수행할 수 있다”는 타이밍에 실행.
     * */
    @PostConstruct
    public void initRestClient() {
        restClient = RestClient.create(commentServiceUrl);
    }

    /*
     * 댓글수 원본 데이터 읽기
     * */
    public long count(Long articleId) {
        try {
            return restClient.get()
                    .uri("/v2/comments/articles/{articleId}/count", articleId)
                    .retrieve()
                    .body(Long.class);
        } catch (Exception e) {
            log.error("[CommentClient.count] articleId={}", articleId, e);
            return 0;
        }
    }

}
