package kuke.board.common.outboxmessagerelay;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/*
* 샤딩이 되어있는 상황, 샤딩 개수는 4개로 가정
* */
/*
* access level : "생성자는 만들되, private으로 제한해서 외부에서 new로 호출 못하게 하라."
* - 상수/유틸용 클래스로 생성자 선언없이 바로 상수를 가져다 쓸 수 있도록 함
* */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageRelayConstants {
    public static final int SHARD_COUNT = 4; // 임의의 값
}
