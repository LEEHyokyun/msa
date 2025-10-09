package kuke.board.hotarticle.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class TimeCalculatorUtilsTest {
    @Test
    void test() {
        /*
        * 내일이 지나면 내일 보여줄 인기글 집계 종료..더이상 데이터 필요없음
        * */
        Duration duration = TimeCalculatorUtils.calculateDurationToMidnight();
        System.out.println("duration.getSeconds() / 60 = " + duration.getSeconds() / 60);
    }
}