package kuke.board.article.service.response;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
/*
* 페이징 쿼리를 활용하여 페이징 결과를 담는 목적으로
* 페이징 Response 객체 별도 구성
* */
public class ArticlePageResponse {
    /*
    * 페이징 조회 시 출력할 데이터
    * */
    private List<ArticleResponse> articles;
    /*
    * 페이지 활성화에 필요한 페이지카운트
    * */
    private Long articleCount;

    public static ArticlePageResponse of(List<ArticleResponse> articles, Long articleCount) {
        ArticlePageResponse response = new ArticlePageResponse();
        response.articles = articles;
        response.articleCount = articleCount;
        return response;
    }
}
