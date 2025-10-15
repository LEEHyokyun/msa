package kuke.board.articleread.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
* Cachable 이외 캐싱 어노테이션 활용을 위한 어노테이션 정의(명세)
* - 이 어노테이션을 사용한 메서드들은 Aspect 정의에 의해 Aspect를 실행한다.
* - 런타임 시점에
* - 메서드에 붙일 수 있도록
* */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OptimizedCacheable {
    String type();
    long ttlSeconds();
}
