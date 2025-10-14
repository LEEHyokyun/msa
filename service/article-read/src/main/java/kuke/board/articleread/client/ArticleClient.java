package kuke.board.articleread.client;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
* Command 서버로 데이터를 요청하기 위한 Client 객체
* 다른 도메인에서 데이터 추출 시 사용하며, Service layer에서 이를 참조.
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleClient {
    private RestClient restClient;
    @Value("${endpoints.kuke-board-article-service.url}")
    private String articleServiceUrl;

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
        restClient = RestClient.create(articleServiceUrl);
    }

    /*
    * 게시글 원본 데이터 읽기
    * */
    /*
     * 영속성 계층에서 DB의 데이터 추출결과가 아무것도 없을때(null)
     * 이에 대한 대비를 해야하는 경우 Optional 사용
     * */
    public Optional<ArticleResponse> read(Long articleId) {
        try {
            ArticleResponse articleResponse = restClient.get()
                    .uri("/v1/articles/{articleId}", articleId)
                    .retrieve()
                    .body(ArticleResponse.class);
            return Optional.ofNullable(articleResponse);
        } catch (Exception e) {
            log.error("[ArticleClient.read] articleId={}", articleId, e);
            return Optional.empty();
        }
    }

    /*
    * 목록조회기능 추가 : 페이징
    * 원본데이터를 얻는 restClient 기본적으로 필요(Redis에 없을 경우)
    * */
    public ArticlePageResponse readAll(Long boardId, Long page, Long pageSize) {
        try {
            return restClient.get()
                    .uri("/v1/articles?boardId=%s&page=%s&pageSize=%s".formatted(boardId, page, pageSize))
                    .retrieve()
                    .body(ArticlePageResponse.class);
        } catch (Exception e) {
            log.error("[ArticleClient.readAll] boardId={}, page={}, pageSize={}", boardId, page, pageSize, e);
            return ArticlePageResponse.EMPTY;
        }
    }

    /*
     * 목록조회기능 추가 : 무한스크롤
     * 원본데이터를 얻는 restClient 기본적으로 필요(Redis에 없을 경우)
     * */
    public List<ArticleResponse> readAllInfiniteScroll(Long boardId, Long lastArticleId, Long pageSize) {
        try {
            return restClient.get()
                    .uri(
                            lastArticleId != null ?
                                    "/v1/articles/infinite-scroll?boardId=%s&lastArticleId=%s&pageSize=%s"
                                            .formatted(boardId, lastArticleId, pageSize) :
                                    "/v1/articles/infinite-scroll?boardId=%s&pageSize=%s"
                                            .formatted(boardId, pageSize)
                    )
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ArticleResponse>>() {});
        } catch (Exception e) {
            log.error("[ArticleClient.readAllInfiniteScroll] boardId={}, lastArticleId={}, pageSize={}",
                    boardId, lastArticleId, pageSize, e);
            return List.of();
        }
    }

    /*
    * 페이징 쿼리에서 사용하는 전체 게시글 수 추출
    * */
    public long count(Long boardId) {
        try {
            return restClient.get()
                    .uri("/v1/articles/boards/{boardId}/count", boardId)
                    .retrieve()
                    .body(Long.class);
        } catch (Exception e) {
            log.error("[ArticleClient.count] boardId={}", boardId, e);
            return 0;
        }
    }

    /*
    * API(도메인 전역적인) 데이터 조회용도가 아닌,
    * Client 내부에서 데이터를 만들어 단순 전달하기 위한 용도로
    * 전송용 객체를 중첩 Class로 구성하여 전달.
    * */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticlePageResponse {
        private List<ArticleResponse> articles;
        private Long articleCount;

        public static ArticlePageResponse EMPTY = new ArticlePageResponse(List.of(), 0L);
    }

    /*
     * API(도메인 전역적인) 데이터 조회용도가 아닌,
     * Client 내부에서 데이터를 만들어 단순 전달하기 위한 용도로
     * 전송용 객체를 중첩 Class로 구성하여 전달.
     * */
    @Getter
    public static class ArticleResponse {
        private Long articleId;
        private String title;
        private String content;
        private Long boardId;
        private Long writerId;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
    }
}
