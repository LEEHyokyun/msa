package kuke.board.hotarticle.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeCalculatorUtils {
    public static Duration calculateDurationToMidnight() {
        /*
        * 지금
        * */
        LocalDateTime now = LocalDateTime.now();
        /*
        * 다음날 00시
        * */
        LocalDateTime midnight = now.plusDays(1).with(LocalTime.MIDNIGHT);
        /*
        * 다음날 보여줄 인기글을 위해
        * 집계에 필요한 데이터가 지속해야할 시간
        * */
        return Duration.between(now, midnight);
    }
}
