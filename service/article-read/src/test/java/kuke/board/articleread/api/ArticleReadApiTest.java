package kuke.board.articleread.api;

import kuke.board.articleread.service.response.ArticleReadPageResponse;
import kuke.board.articleread.service.response.ArticleReadResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

public class ArticleReadApiTest {
    //목록조회
    RestClient articleReadRestClient = RestClient.create("http://localhost:9005");
    //단건조회
    RestClient articleRestClient = RestClient.create("http://localhost:9000");

    /*
    * redis에서 articleQueryModel을 저장하여 MySQL을 경유하지 않고 바로 읽는다.
    * - fetch 로그가 없어야 하며
    * - redis에서 읽기 기능이 정상적으로 작동하는지 확인
    * */
    @Test
    void readTestByArticleQueryModel() {
        ArticleReadResponse response = articleReadRestClient.get()
                .uri("/v1/articles/{articleId}", 236371812009361408L)
                .retrieve()
                .body(ArticleReadResponse.class);

        System.out.println("response = " + response);
    }

    /*
     * redis에서 articleQueryModel을 저장하여 MySQL을 경유한다.
     * - fetch 로그가 남겨져야 한다.
     * */
    @Test
    void readTestByMySQLRawData() {
        ArticleReadResponse response = articleReadRestClient.get()
                .uri("/v1/articles/{articleId}", 229089422410231808L)
                .retrieve()
                .body(ArticleReadResponse.class);

        System.out.println("response = " + response);
    }
    /*
     * 1번 카테고리의 1페이지 5개 게시글 목록 조회.
     * */
    @Test
    void readAllTest() {
        /*
        * 데이터 생성 후 Redis에도 데이터 생성,
        * 이 생성한 데이터들에 대해 article query model로 조회한다.
        * */
        /*목록조회(article query model)*/
        ArticleReadPageResponse response1 = articleReadRestClient.get()
                //.uri("/v1/articles?boardId=%s&page=%s&pageSize=%s".formatted(1L, 1L, 5)) //redis 조회 대상
                .uri("/v1/articles?boardId=%s&page=%s&pageSize=%s".formatted(1L, 3000L, 5)) //origin 조회 대상
                .retrieve()
                .body(ArticleReadPageResponse.class);

        System.out.println("response1.getArticleCount() = " + response1.getArticleCount());
        for (ArticleReadResponse article : response1.getArticles()) {
            System.out.println("article.getArticleId() = " + article.getArticleId());
        }

        /*단건조회(원본)*/
        ArticleReadPageResponse response2 = articleRestClient.get()
                //.uri("/v1/articles?boardId=%s&page=%s&pageSize=%s".formatted(1L, 1L, 5)) //redis 조회 대상
                .uri("/v1/articles?boardId=%s&page=%s&pageSize=%s".formatted(1L, 3000L, 5)) //origin 조회 대상
                .retrieve()
                .body(ArticleReadPageResponse.class);

        System.out.println("response2.getArticleCount() = " + response2.getArticleCount());
        for (ArticleReadResponse article : response2.getArticles()) {
            System.out.println("article.getArticleId() = " + article.getArticleId());
        }
    }

    @Test
    void readAllInfiniteScrollTest() {
        /*
        * 무한스크롤 테스트
        * */
        List<ArticleReadResponse> response1 = articleReadRestClient.get()
                //.uri("/v1/articles/infinite-scroll?boardId=%s&pageSize=%s".formatted(1L, 5L))
                .uri("/v1/articles/infinite-scroll?boardId=%s&pageSize=%s&lastArticleId=%s".formatted(1L, 5L, 236668503844204544L))
                .retrieve()
                .body(new ParameterizedTypeReference<List<ArticleReadResponse>>() {
                });

        for (ArticleReadResponse response : response1) {
            System.out.println("response = " + response.getArticleId());
        }

        List<ArticleReadResponse> response2 = articleRestClient.get()
                //.uri("/v1/articles/infinite-scroll?boardId=%s&pageSize=%s".formatted(1L, 5L))
                .uri("/v1/articles/infinite-scroll?boardId=%s&pageSize=%s&lastArticleId=%s".formatted(1L, 5L, 236668503844204544L))
                .retrieve()
                .body(new ParameterizedTypeReference<List<ArticleReadResponse>>() {
                });

        for (ArticleReadResponse response : response2) {
            System.out.println("response = " + response.getArticleId());
        }
    }
}
