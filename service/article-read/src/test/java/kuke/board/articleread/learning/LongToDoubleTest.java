package kuke.board.articleread.learning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/*
*
* */
public class LongToDoubleTest {
    @Test
    void longToDoubleTest() {
        /*
        * 데이터 표현 방식 차이에 따른 데이터 유실 발생(long > double 캐스팅 할 경우)*
        * longValue = 111111111111111111
        * doubleValue = 111111111111111104
        * longValue2 = 111111111111111104
        * */
        // long은 64비트로 정수
        // double은 64비트로 부동소수점
        long longValue = 111_111_111_111_111_111L;
        System.out.println("longValue = " + longValue);
        double doubleValue = longValue;
        System.out.println("doubleValue = " + new BigDecimal(doubleValue).toString());
        long longValue2 = (long) doubleValue;
        System.out.println("longValue2 = " + longValue2);
    }
}
