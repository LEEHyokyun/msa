package kuke.board.articleread.cache;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/*
* AOP의 동작시점과 동작로직을 구체화
* */
/*
* Cahcing 처리를 Customized AOP(Aspect)로 처리하기 위함 = Aspect.
* Cache 어노테이션을 정의하고
* 어느 시점에 처리할 것인지(Jointpoint의 pre? post? 결정.)
* */
@Aspect
@Component
@RequiredArgsConstructor //for final
public class OptimizedCacheAspect {
    private final OptimizedCacheManager optimizedCacheManager;

    @Around("@annotation(OptimizedCacheable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        OptimizedCacheable cacheable = findAnnotation(joinPoint);
        /*
        * AOP로 추출할 수 있는 정보는 type 정보와 ttl Seconds에 대한 정보.
        * 이외 jointpoint로 가져올 수 있는 정보(메서드 시그니처)는 매개변수..
        * 서로 추출할 수 있는 정보가 다르므로 어노테이션 명세 등과 비교하면서 로직 구성해줄 것.
        * */
        /*
        * 동작 = process 동작.
        * */

        /*
        * 원본데이터 추출 = DataSupplier의 get() = 람다 그 자체!
        * 즉 get()의 구현부인 return부가 람다식으로 표현됨
        * 여기서는 viewClient의 조회수가 그 경우.
        * */


        return optimizedCacheManager.process(
                cacheable.type(),
                cacheable.ttlSeconds(),
                joinPoint.getArgs(),
                findReturnType(joinPoint),
                () -> joinPoint.proceed()

        );
    }

    /*
    * joinPoint는 AOP가 감지한 메서드 호출 정보를 담고 있어요.
    * joinPoint.getSignature() → 메서드 시그니처 정보 가져오기.
    * (MethodSignature) signature → MethodSignature로 캐스팅해야 메서드 반환타입, 파라미터, 어노테이션 등의 상세 정보에 접근 가능.
    * getAnnotation(OptimizedCacheable.class) → 해당 메서드에 붙은 어노테이션 인스턴스를 가져오기.
    * 즉, AOP가 동작할 때, 실제 메서드에서 어노테이션 정보(메서드 실행 정보)를 가져오기 위해 필요합니다.
    * */
    private OptimizedCacheable findAnnotation(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        return methodSignature.getMethod().getAnnotation(OptimizedCacheable.class);
    }

    /*
    * 어떤 return Type이 올지 모른다..컴파일 시점에서 강제하지 않기 위해 Class<?>
    * */
    private Class<?> findReturnType(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        return methodSignature.getReturnType();
    }
}
