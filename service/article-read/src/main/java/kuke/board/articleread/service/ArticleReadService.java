package kuke.board.articleread.service;

import kuke.board.articleread.client.ArticleClient;
import kuke.board.articleread.client.CommentClient;
import kuke.board.articleread.client.LikeClient;
import kuke.board.articleread.client.ViewClient;
import kuke.board.articleread.repository.ArticleIdListRepository;
import kuke.board.articleread.repository.ArticleQueryModel;
import kuke.board.articleread.repository.ArticleQueryModelRepository;
import kuke.board.articleread.repository.BoardArticleCountRepository;
import kuke.board.articleread.service.event.handler.EventHandler;
import kuke.board.articleread.service.response.ArticleReadPageResponse;
import kuke.board.articleread.service.response.ArticleReadResponse;
import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/*
* 최종 Article Read Service layer 구성
* */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleReadService {
    private final ArticleClient articleClient;
    private final CommentClient commentClient;
    private final LikeClient likeClient;
    private final ViewClient viewClient;
    private final ArticleIdListRepository articleIdListRepository;
    /*
    * article data들이 저장되어있는 article query model
    * 조회수는 redis에 이미 저장..redis에서 바로 호출하여 가져온다는 점 유의(articleQueryModel)
    * */
    private final ArticleQueryModelRepository articleQueryModelRepository;
    private final BoardArticleCountRepository boardArticleCountRepository;
    private final List<EventHandler> eventHandlers;

    /*
    * Consumer 측에서 사용하는 메서드
    * 이벤트를 전달받아 지원할 경우 처리
    * */
    public void handleEvent(Event<EventPayload> event) {
        for (EventHandler eventHandler : eventHandlers) {
            if (eventHandler.supports(event)) {
                eventHandler.handle(event);
            }
        }
    }

    /*
    * 최종적으로 articleId에 대한 데이터를 읽어오기 위함
    * - "Redis"에서 가져온다.
    * - Redis에 없다면 원본 데이터를 Commandor 서버에 요청한다.
    * - 두 곳 모두 없다면 예외를 던진다.
    * */
    public ArticleReadResponse read(Long articleId) {
        ArticleQueryModel articleQueryModel = articleQueryModelRepository.read(articleId)
                //data null -> fetch
                .or(() -> fetch(articleId))
                .orElseThrow();

        //create response entity from query model
        return ArticleReadResponse.from(
                articleQueryModel,
                viewClient.count(articleId)
        );
    }

    /*
    * 원본 데이터를 요청
    * - 있다면 article query model 생성(article + comment + like)
    * -
    * */
    private Optional<ArticleQueryModel> fetch(Long articleId) {
        Optional<ArticleQueryModel> articleQueryModelOptional = articleClient.read(articleId)
                .map(article -> ArticleQueryModel.create(
                        article,
                        commentClient.count(articleId),
                        likeClient.count(articleId)
                ));
        /*
        * 데이터 존재할 경우 TTL = 1day의 데이터를 Redis에 생성한다.
        * 없다면 Optional.Empty()
        * */
        articleQueryModelOptional
                .ifPresent(articleQueryModel -> articleQueryModelRepository.create(articleQueryModel, Duration.ofDays(1)));
        log.info("[ArticleReadService.fetch] fetch data. articleId={}, isPresent={}", articleId, articleQueryModelOptional.isPresent());
        return articleQueryModelOptional;
    }

    public ArticleReadPageResponse readAll(Long boardId, Long page, Long pageSize) {
        return ArticleReadPageResponse.of(
                readAll(
                        readAllArticleIds(boardId, page, pageSize)
                ),
                count(boardId)
        );
    }

    private List<ArticleReadResponse> readAll(List<Long> articleIds) {
        Map<Long, ArticleQueryModel> articleQueryModelMap = articleQueryModelRepository.readAll(articleIds);
        return articleIds.stream()
                .map(articleId -> articleQueryModelMap.containsKey(articleId) ?
                        articleQueryModelMap.get(articleId) :
                        fetch(articleId).orElse(null))
                .filter(Objects::nonNull)
                .map(articleQueryModel ->
                        ArticleReadResponse.from(
                                articleQueryModel,
                                viewClient.count(articleQueryModel.getArticleId())
                        ))
                .toList();
    }

    private List<Long> readAllArticleIds(Long boardId, Long page, Long pageSize) {
        List<Long> articleIds = articleIdListRepository.readAll(boardId, (page - 1) * pageSize, pageSize);
        if (pageSize == articleIds.size()) {
            log.info("[ArticleReadService.readAllArticleIds] return redis data.");
            return articleIds;
        }
        log.info("[ArticleReadService.readAllArticleIds] return origin data.");
        return articleClient.readAll(boardId, page, pageSize).getArticles().stream()
                .map(ArticleClient.ArticleResponse::getArticleId)
                .toList();
    }

    private long count(Long boardId) {
        Long result = boardArticleCountRepository.read(boardId);
        if (result != null) {
            return result;
        }
        long count = articleClient.count(boardId);
        boardArticleCountRepository.createOrUpdate(boardId, count);
        return count;
    }

    public List<ArticleReadResponse> readAllInfiniteScroll(Long boardId, Long lastArticleId, Long pageSize) {
        return readAll(
                readAllInfiniteScrollArticleIds(boardId, lastArticleId, pageSize)
        );
    }

    private List<Long> readAllInfiniteScrollArticleIds(Long boardId, Long lastArticleId, Long pageSize) {
        List<Long> articleIds = articleIdListRepository.readAllInfiniteScroll(boardId, lastArticleId, pageSize);
        if (pageSize == articleIds.size()) {
            log.info("[ArticleReadService.readAllInfiniteScrollArticleIds] return redis data.");
            return articleIds;
        }
        log.info("[ArticleReadService.readAllInfiniteScrollArticleIds] return origin data.");
        return articleClient.readAllInfiniteScroll(boardId, lastArticleId, pageSize).stream()
                .map(ArticleClient.ArticleResponse::getArticleId)
                .toList();
    }

}
