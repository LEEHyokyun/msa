package kuke.board.common.outboxmessagerelay;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
class AssignedShardTest {

    @Test
    void ofTest() {
        // given
        Long shardCount = 64L;
        List<String> appList = List.of("appId1", "appId2", "appId3");

        // when
        /*
        * appId1 ~ appId3이 실행되고 있음을 가정
        * */
        AssignedShard assignedShard1 = AssignedShard.of(appList.get(0), appList, shardCount);
        AssignedShard assignedShard2 = AssignedShard.of(appList.get(1), appList, shardCount);
        AssignedShard assignedShard3 = AssignedShard.of(appList.get(2), appList, shardCount);
        /*
        * invalid app id..샤드 할당이 안되므로 빈 리스트
        * */
        AssignedShard assignedShard4 = AssignedShard.of("invalid", appList, shardCount);

        // then
        /*
        * 여러 개의 List<Long>을 하나의 리스트로 합치는 작업
        * 1) of : 여러 개의 값을 받아서 Stream으로 만들어주는 메서드 Stream<List<Long>>
        * 2) flatMap은 각 요소를 스트림으로 변환하고, 여러 스트림을 하나의 스트림으로 평탄화(flatten)해주는 연산 List<Long>을 Stream<Long> -> Stream<Long>
        * */
        List<Long> result = Stream.of(assignedShard1.getShards(), assignedShard2.getShards(),
                        assignedShard3.getShards(), assignedShard4.getShards())
                .flatMap(List::stream)
                .toList();

        assertThat(result).hasSize(shardCount.intValue());

        for(int i=0; i<64; i++) {
            assertThat(result.get(i)).isEqualTo(i);
        }

        assertThat(assignedShard4.getShards()).isEmpty();
    }
}