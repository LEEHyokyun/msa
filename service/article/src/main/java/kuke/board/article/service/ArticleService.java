package kuke.board.article.service;

import kuke.board.article.entity.Article;
import kuke.board.article.entity.BoardArticleCount;
import kuke.board.article.repository.ArticleRepository;
import kuke.board.article.repository.BoardArticleCountRepository;
import kuke.board.article.service.request.ArticleCreateRequest;
import kuke.board.article.service.request.ArticleUpdateRequest;
import kuke.board.article.service.response.ArticlePageResponse;
import kuke.board.article.service.response.ArticleResponse;
import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.ArticleCreatedEventPayload;
import kuke.board.common.event.payload.ArticleDeletedEventPayload;
import kuke.board.common.event.payload.ArticleUpdatedEventPayload;
import kuke.board.common.outboxmessagerelay.OutboxEventPublisher;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {
    /*
    * RequiredArgsConstructor
    * - 단순 필드 주입이 아닌 생성자 주입을 통해 안정적으로 의존성을 주입한다.
    * - 생성자 주입을 통한 의존성 주입으로 의존성 주입 순환 오류로 인한 의존성 역전 현상을 방지한다.
    * */
    private final Snowflake snowflake = new Snowflake();
    private final ArticleRepository articleRepository;
    /*
    * outbox pattern 로직 추가
    * */
    private final OutboxEventPublisher outboxEventPublisher;
    private final BoardArticleCountRepository boardArticleCountRepository;

    @Transactional
    public ArticleResponse create(ArticleCreateRequest request) {
        /*
        * DATA NULL -> INSERT
        * DATA NOT NULL -> UPDATE
        * */
        Article article = articleRepository.save(
                Article.create(snowflake.nextId(), request.getTitle(), request.getContent(), request.getBoardId(), request.getWriterId())
        );

        /*
        * 게시글 생성 시점에 게시글 수 처리, 최초 없을 경우 init(*save 명기)
        * */
        int result = boardArticleCountRepository.increase(request.getBoardId());
        if (result == 0) {
            boardArticleCountRepository.save(
                    BoardArticleCount.init(request.getBoardId(), 1L)
            );
        }

        /*
         * outbox pattern 로직 추가
         * */
        outboxEventPublisher.publish(
                EventType.ARTICLE_CREATED,
                ArticleCreatedEventPayload.builder()
                        .articleId(article.getArticleId())
                        .title(article.getTitle())
                        .content(article.getContent())
                        .boardId(article.getBoardId())
                        .writerId(article.getWriterId())
                        .createdAt(article.getCreatedAt())
                        .modifiedAt(article.getModifiedAt())
                        .boardArticleCount(count(article.getBoardId()))
                        .build(),
                article.getBoardId()
        );

        return ArticleResponse.from(article);
    }

    @Transactional
    public ArticleResponse update(Long articleId, ArticleUpdateRequest request) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        article.update(request.getTitle(), request.getContent());
        /*
        * 트랜잭션이 끝나는 시점에서 영속성 객체인 article(Repository를 통해 객체를 호출하였으므로)의 변화 감지(Dirty Checking)
        * SnapShot의 변화 감지 후 DB에 최종적으로 반영
        * */

        /*
         * outbox pattern 로직 추가
         * */
        outboxEventPublisher.publish(
                EventType.ARTICLE_UPDATED,
                ArticleUpdatedEventPayload.builder()
                        .articleId(article.getArticleId())
                        .title(article.getTitle())
                        .content(article.getContent())
                        .boardId(article.getBoardId())
                        .writerId(article.getWriterId())
                        .createdAt(article.getCreatedAt())
                        .modifiedAt(article.getModifiedAt())
                        .build(),
                article.getBoardId()
        );
        return ArticleResponse.from(article);
    }

    public ArticleResponse read(Long articleId) {
        return ArticleResponse.from(articleRepository.findById(articleId).orElseThrow());
    }

    @Transactional
    public void delete(Long articleId) {
        /*
         * 게시글 생성 시점에 게시글 수 처리
         * */
        Article article = articleRepository.findById(articleId).orElseThrow();
        articleRepository.delete(article);
        boardArticleCountRepository.decrease(article.getBoardId());

        /*
         * outbox pattern 로직 추가
         * */
        outboxEventPublisher.publish(
                EventType.ARTICLE_DELETED,
                ArticleDeletedEventPayload.builder()
                        .articleId(article.getArticleId())
                        .title(article.getTitle())
                        .content(article.getContent())
                        .boardId(article.getBoardId())
                        .writerId(article.getWriterId())
                        .createdAt(article.getCreatedAt())
                        .modifiedAt(article.getModifiedAt())
                        .boardArticleCount(count(article.getBoardId()))
                        .build(),
                article.getBoardId()
        );
    }

    public ArticlePageResponse readAll(Long boardId, Long page, Long pageSize) {
        return ArticlePageResponse.of(
                articleRepository.findAll(boardId, (page - 1) * pageSize, pageSize).stream()
                        .map(ArticleResponse::from)
                        .toList(),
                articleRepository.count(
                        boardId,
                        PageLimitCalculator.calculatePageLimit(page, pageSize, 10L)
                )
        );
    }

    /*
    * 무한스크롤 동작은 전체 데이터 개수 등 페이징 쿼리에서 필요한 다른 데이터가 필요없어진다.
    * 이에 따라 PagingDTO가 아닌 일반 ArticleDTO를 사용
    * */
    public List<ArticleResponse> readAllInfiniteScroll(Long boardId, Long pageSize, Long lastArticleId) {
        List<Article> articles = lastArticleId == null ?
                articleRepository.findAllInfiniteScroll(boardId, pageSize) :
                articleRepository.findAllInfiniteScroll(boardId, pageSize, lastArticleId);
        return articles.stream().map(ArticleResponse::from).toList();
    }

    /*
    * 게시글 수 반환
    * */
    public Long count(Long boardId) {
        return boardArticleCountRepository.findById(boardId)
                .map(BoardArticleCount::getArticleCount)
                .orElse(0L);
    }
}
