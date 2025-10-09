package kuke.board.hotarticle.controller;

import kuke.board.hotarticle.service.HotArticleService;
import kuke.board.hotarticle.service.response.HotArticleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
* 인기글 조회
* */
@RestController
@RequiredArgsConstructor //for final
public class HotArticleController {
    private final HotArticleService hotArticleService;

    /*
    * 특정날짜를 매개변수로 해당일의 인기글 조회
    * */
    @GetMapping("/v1/hot-articles/articles/date/{dateStr}")
    public List<HotArticleResponse> readAll(
            @PathVariable("dateStr") String dateStr
    ) {
        return hotArticleService.readAll(dateStr);
    }
}
