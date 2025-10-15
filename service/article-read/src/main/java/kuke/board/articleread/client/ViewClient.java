package kuke.board.articleread.client;

import jakarta.annotation.PostConstruct;
import kuke.board.articleread.cache.OptimizedCacheable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/*
* 다른 도메인에서 데이터 추출 시 사용하며, Service layer에서 이를 참조.
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewClient {
    private RestClient restClient;
    @Value("${endpoints.kuke-board-view-service.url}")
    private String viewServiceUrl;

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
        restClient = RestClient.create(viewServiceUrl);
    }

    /*
     * 좋아요수 원본 데이터 읽기
     * = MySQL ?
     * = Redis ?
     * = 일단은 MySQL에서 추출
     * */
    /*
    * cacheable -> articleId로 받은 매개변수를 그대로 key로 사용(=> key = #articleId), 마찬가지로 value = 반환값
    * caching AOP로 인해 먼저 Redis에서 해당 key값의 데이터를 조회해오고, 없으면 원본데이터(로직) 추출 진행
    * 이떄 redis에서 해당 count 추출, 없다면 원본데이터 요청하여 추출
    * */
//    @Cacheable(key = "#articleId", value = "articleViewCount")
    /*
    * Caching Aspect의 적용 대상(포인트컷)
    * */
    @OptimizedCacheable(type = "articleViewCount", ttlSeconds = 1)
    public long count(Long articleId) {
        log.info("[ViewClient.count] articleId={}", articleId);
        try {
            return restClient.get()
                    .uri("/v1/article-views/articles/{articleId}/count", articleId)
                    .retrieve()
                    .body(Long.class);
        } catch (Exception e) {
            log.error("[ViewClient.count] articleId={}", articleId, e);
            return 0;
        }
    }

}
