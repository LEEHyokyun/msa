package kuke.board.comment.api;

import kuke.board.comment.service.response.CommentPageResponse;
import kuke.board.comment.service.response.CommentResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

public class CommentApiTest {
    RestClient restClient = RestClient.create("http://localhost:9001");

    @Test
    void create() {
        /*
        * 부모댓글 및 하위 2개의 댓글 생성
        * */
        CommentResponse response1 = createComment(new CommentCreateRequest(1L, "my comment1", null, 1L));
        CommentResponse response2 = createComment(new CommentCreateRequest(1L, "my comment2", response1.getCommentId(), 1L));
        CommentResponse response3 = createComment(new CommentCreateRequest(1L, "my comment3", response1.getCommentId(), 1L));

        System.out.println("commentId=%s".formatted(response1.getCommentId()));
        System.out.println("\tcommentId=%s".formatted(response2.getCommentId()));
        System.out.println("\tcommentId=%s".formatted(response3.getCommentId()));

//        commentId=229790209535275008
//          commentId=229790212127354880
//          commentId=229790212320292864
    }

    CommentResponse createComment(CommentCreateRequest request) {
        return restClient.post()
                .uri("/v1/comments")
                .body(request)
                .retrieve()
                .body(CommentResponse.class);
    }

    @Test
    void read() {
        CommentResponse response = restClient.get()
                .uri("/v1/comments/{commentId}", 229790209535275008L)
                .retrieve()
                .body(CommentResponse.class);

        System.out.println("response = " + response);
    }

    @Test
    void delete() {
        //        commentId=229790209535275008 - x
        //          commentId=229790212127354880 - x
        //          commentId=229790212320292864 - x

        restClient.delete()
                .uri("/v1/comments/{commentId}", 229790212320292864L)
                .retrieve();
    }

    @Test
    void readAll() {
        CommentPageResponse response = restClient.get()
                .uri("/v1/comments?articleId=1&page=300000&pageSize=10")
                .retrieve()
                .body(CommentPageResponse.class);

        System.out.println("response.getCommentCount() = " + response.getCommentCount());
        for (CommentResponse comment : response.getComments()) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                /*1depth 댓글*/
                System.out.print("\t");
            }
            /*2depth 댓글*/
            System.out.println("comment.getCommentId() = " + comment.getCommentId());
        }

        /**
         * 1번 페이지 수행 결과
         * comment.getCommentId() = 229796041077035008
         * 	comment.getCommentId() = 229796041261584386
         * comment.getCommentId() = 229796041077035009
         * 	comment.getCommentId() = 229796041265778698
         * comment.getCommentId() = 229796041077035010
         * 	comment.getCommentId() = 229796041265778707
         * comment.getCommentId() = 229796041077035011
         * 	comment.getCommentId() = 229796041265778697
         * comment.getCommentId() = 229796041077035012
         * 	comment.getCommentId() = 229796041261584387
         */
    }

    @Test
    void readAllInfiniteScroll() {
        List<CommentResponse> responses1 = restClient.get()
                .uri("/v1/comments/infinite-scroll?articleId=1&pageSize=5")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponse>>() {
                });

        System.out.println("firstPage");
        for (CommentResponse comment : responses1) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                System.out.print("\t");
            }
            System.out.println("comment.getCommentId() = " + comment.getCommentId());
        }

        Long lastParentCommentId = responses1.getLast().getParentCommentId();
        Long lastCommentId = responses1.getLast().getCommentId();

        List<CommentResponse> responses2 = restClient.get()
                .uri("/v1/comments/infinite-scroll?articleId=1&pageSize=5&lastParentCommentId=%s&lastCommentId=%s"
                        .formatted(lastParentCommentId, lastCommentId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponse>>() {
                });

        System.out.println("secondPage");
        for (CommentResponse comment : responses2) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                System.out.print("\t");
            }
            System.out.println("comment.getCommentId() = " + comment.getCommentId());
        }
    }

    /*
    * 운영 src에 존재하는 요청 DTO를 굳이 테스트 환경에서 다시 만드는 이유
    * - 기본적으로 데이터 전달이 목적인 DTO는 테스트 대상이 아니므로 mock()할 필요가 없고 그럴 대상도 아니다.
    * - 테스트 코드에서도 실제 DTO를 그대로 생성해서 사용하는 것이 변경점 관리나 유지관리 측면에서 유리
    * - 단, 반복 생성을 줄이고 싶다면 테스트용 팩토리/Fixture 유틸을 두는 것을 권장(DTO "생성"이 빈번하게 일어날 경우)
    * - Nested Class로 생성 가능하며 정적 중첩 클래스로 선언된 예시, 다만 운영에 해당 DTO를 생성하기 전에 테스트에서 먼저 만들었을 가능성
    * (굳이 생성할 필요는 없었으나 시간 차로 인해 존재할 경우)
    * */
    @Getter
    @AllArgsConstructor
    public static class CommentCreateRequest {
        private Long articleId;
        private String content;
        private Long parentCommentId;
        private Long writerId;
    }
}
