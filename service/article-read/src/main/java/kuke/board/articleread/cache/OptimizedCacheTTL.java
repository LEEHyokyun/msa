package kuke.board.articleread.cache;

import lombok.Getter;

import java.time.Duration;

/*
* 정책적으로 정한 logcial TTL보다
* 실제 데이터 존재시간인 physical TTL을 더 길게 설정하도록 해주는 클래스
* physical TTL = logical TTL + 5sec.
* */
@Getter
public class OptimizedCacheTTL {
    private Duration logicalTTL;
    private Duration physicalTTL;

    public static final long PHYSICAL_TTL_DELAY_SECONDS = 5;

    public static OptimizedCacheTTL of(long ttlSeconds) {
        OptimizedCacheTTL optimizedCacheTTL = new OptimizedCacheTTL();
        optimizedCacheTTL.logicalTTL = Duration.ofSeconds(ttlSeconds);
        optimizedCacheTTL.physicalTTL = optimizedCacheTTL.logicalTTL.plusSeconds(PHYSICAL_TTL_DELAY_SECONDS);
        return optimizedCacheTTL;
    }
}
