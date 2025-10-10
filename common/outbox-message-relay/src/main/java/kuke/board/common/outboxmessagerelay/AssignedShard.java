package kuke.board.common.outboxmessagerelay;

import lombok.Getter;

import java.util.List;
import java.util.stream.LongStream;

/*
* application은 각각 polling하여 이벤트 메시지를 비동기로 처리할 책임을 가진다.
* 이 polling을 어느 샤드에 할지 균등하게 배분해주는 과정을 정의
* */
@Getter
public class AssignedShard {
    /*
    * 현재 할당된 샤드들
    * = 본인 앱 인덱스에 맞는 샤드들이 할당된다.
    * */
    private List<Long> shards;

    /*
    * 본인의 application 식별자
    * 현재 실행 중인 application의 식별자
    * 샤드 개수
    * - 본인에게 할당된 샤드에 대한 정보
    * */
    public static AssignedShard of(String appId, List<String> appIds, long shardCount) {
        AssignedShard assignedShard = new AssignedShard();
        assignedShard.shards = assign(appId, appIds, shardCount);
        return assignedShard;
    }

    /*
    * 할당
    * -
    * */
    private static List<Long> assign(String appId, List<String> appIds, long shardCount) {
        /*
        * 샤드 할당을 위한 앱 인덱스 추출
        * */
        int appIndex = findAppIndex(appId, appIds);
        if (appIndex == -1) {
            /*
            * index = -1, 앱 실행 중이 아니기에 샤드 할당 의미없음, 로직 종료
            * */
            return List.of();
        }

        /*
        * ShardCount / appIds.size() -> 앱 1개 당 몇개의 샤드를 맡을 것인가
        * 이 계산에 따라 각 인덱스별 샤드를 적절하게 분산
        * app0 -> index = 0 -> 0,1
        * app1 -> index = 1 -> 2,3
        * */
        long start = appIndex * shardCount / appIds.size();
        long end = (appIndex + 1) * shardCount / appIds.size() - 1;

        /*
        * app0 -> [0,1]
        * app1 -> [2,3]
        * 이런 방식으로 앱 인스턴스에 맞는 샤드 리스트를 반환
        * */
        return LongStream.rangeClosed(start, end).boxed().toList();
    }

    /*
    * 샤드를 할당하기 위해선 본인이 실행한 앱도 반드시 redis 중앙저장소에 실행목록으로 저장되어야 한다.
    * 그 실행목록에 본인의 앱 식별자가 들어있는지 확인하여 해당 인덱스 값을 추출
    * 실행목록에 없다면 앱 실행이 안되었다는 의미로, 샤드 할당 의미 없음.
    * */
    private static int findAppIndex(String appId, List<String> appIds) {
        for (int i=0; i < appIds.size(); i++) {
            if (appIds.get(i).equals(appId)) {
                return i;
            }
        }
        return -1;
    }
}
