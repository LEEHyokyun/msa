package kuke.board.article.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/*
* 공통적으로 사용하는 util class
* private 생성자 및 final화.
* - final : 해당 공통 util에 대한 상수화 및 상속금지
* - private 생성자(NoArgsConstructor) : 인스턴스 생성 금지
* */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageLimitCalculator {

    /*
    * 현재 위치한 페이지(=page) 및 활성 페이지 개수(=movablePageCount)에 따라
    * 필요한 전체 데이터 개수를 출력하기 위한 메소드
    * */
    public static Long calculatePageLimit(Long page, Long pageSize, Long movablePageCount) {
        return (((page - 1) / movablePageCount) + 1) * pageSize * movablePageCount + 1;
    }
}
