package kuke.board.hotarticle.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.random.RandomGenerator;

/*
* article, comment, like, view 서비스에 대해 인기글 집계 서비스를 요청한다.
* 최종적으로 4개의 서비스와 상호작용하여 인기글을 집계한다.
* 이 집계 과정을 테스트하기 위함.
* */
public class DataInitializer {
    /*
    * 4개의 restClient
    * */
    RestClient articleServiceClient = RestClient.create("http://localhost:9000");
    RestClient commentServiceClient = RestClient.create("http://localhost:9001");
    RestClient likeServiceClient = RestClient.create("http://localhost:9002");
    RestClient viewServiceClient = RestClient.create("http://localhost:9003");

    @Test
    void initialize() {
        /*
        * 데이터 30개 생성
        * - 댓글 수, 좋아요 수, 조회 수
        * */
        for(int i=0; i<30; i++) {
            Long articleId = createArticle();
            long commentCount = RandomGenerator.getDefault().nextLong(10);
            long likeCount = RandomGenerator.getDefault().nextLong(10);
            long viewCount = RandomGenerator.getDefault().nextLong(200);

            createComment(articleId, commentCount);
            like(articleId, likeCount);
            view(articleId, viewCount);
        }
    }

    Long createArticle() {
        return articleServiceClient.post()
                .uri("/v1/articles")
                .body(new ArticleCreateRequest("title", "content", 1L, 1L))
                .retrieve()
                .body(ArticleResponse.class)
                .getArticleId();
    }

    /*
    * 철저하게 분리/독립적인 테스트 환경을 위해 중첩 클래스를 사용한다.
    * = static
    * */
    @Getter
    @AllArgsConstructor
    static class ArticleCreateRequest {
        private String title;
        private String content;
        private Long writerId;
        private Long boardId;
    }

    @Getter
    static class ArticleResponse {
        private Long articleId;
    }

    void createComment(Long articleId, long commentCount) {
        while(commentCount-- > 0) {
            commentServiceClient.post()
                    .uri("/v2/comments")
                    .body(new CommentCreateRequest(articleId, "content", 1L))
                    .retrieve();
        }
    }

    @Getter
    @AllArgsConstructor
    static class CommentCreateRequest {
        private Long articleId;
        private String content;
        private Long writerId;
    }

    void like(Long articleId, long likeCount) {
        while(likeCount-- > 0) {
            likeServiceClient.post()
                    .uri("/v1/article-likes/articles/{articleId}/users/{userId}/pessimistic-lock-1", articleId, likeCount)
                    .retrieve();
        }
    }

    void view(Long articleId, long viewCount) {
        while(viewCount-- > 0) {
            viewServiceClient.post()
                    .uri("/v1/article-views/articles/{articleId}/users/{userId}", articleId, viewCount)
                    .retrieve();
        }
    }
}
