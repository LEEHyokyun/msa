package kuke.board.articleread.cache;

/*
* 원본데이터를 요청하기 위한 책임
* 원본데이터 = Generic Type(T)
* */
/*
* 단 하나의 추상메서드 보유 = FunctionalInterface
* 실제 구현 시 람다식으로 () -> ~~~~ 형태로 축약가능(return ~ -> () -> ...)
* */
@FunctionalInterface
public interface OptimizedCacheOriginDataSupplier<T> {
    /*
    * 전략패턴, 실제 구현시 람다식 대체 가능
    * generic 사용하여 다양한 타입의 클래스를 일괄적으로 다룰 수 있도록 함(이를 T로 대체)
    * */
    /*
    * class<T> interface<T> - 타입변수, 컴파일시점에 결정, 변수 그 자체.
    * class<T> class - 타입객체, 런타임시점에 해당 객체를 참조하게되는 타입객체.
    * */
    T get() throws Throwable;
}
