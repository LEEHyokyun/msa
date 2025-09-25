package kuke.board.article.repository;

import kuke.board.article.entity.Article;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
/*
* SpringBootTest = 통합테스트.
* - 통합테스트로 진행할 경우 Boot 전체 환경 초기화가 필요하므로 시간이 좀 소요됨.
* */
class ArticleRepositoryTest {

    /*
    *  개별적인 테스트 환경이므로 의존성 주입은 autowired로 하여도 무방
    * */
    @Autowired
    ArticleRepository articleRepository;

    @Test
    /*
    * 특정 페이지에서 데이터를 추출하는 페이징 쿼리
    * */
    void findAllTest() {
        List<Article> articles = articleRepository.findAll(1L, 1499970L, 30L);
        log.info("articles.size = {}", articles.size());
        for (Article article : articles) {
            log.info("article = {}", article);
        }
    }

    @Test
    /*
     * 특정 페이지에서 필요한 전체 데이터 개수를 추출하는 쿼리
     * */
    void countTest() {
        Long count = articleRepository.count(1L, 10000L);
        log.info("count = {}", count);
    }

    @Test
        /*
         * 무한스크롤 최초 및 그 이후에 동작하여
         * 데이터를 추출하는 쿼리
         * */
    void findInfiniteScrollTest() {
        List<Article> articles = articleRepository.findAllInfiniteScroll(1L, 30L);
        for (Article article : articles) {
            log.info("articleId = {}", article.getArticleId());
        }

        Long lastArticleId = articles.getLast().getArticleId();
        List<Article> articles2 = articleRepository.findAllInfiniteScroll(1L, 30L, lastArticleId);
        for (Article article : articles2) {
            log.info("articleId = {}", article.getArticleId());
        }
    }
}