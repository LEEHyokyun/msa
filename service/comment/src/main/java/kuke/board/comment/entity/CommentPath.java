package kuke.board.comment.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
/*
* 독립적인 Entity 객체가 아닌 다른 Entity에 종속되어 사용하는 객체(Value Object)임을 명시하는 어노테이션
* Embedded의 어노테이션에서 추적하여 VO에 해당하는 모든 필드정보를 테이블과 매핑한다.
* */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentPath {
    private String path;

    /*
    * path가 가질 수 있는 캐릭터 셋 정의
    * */
    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /*
    * 1 depth 당 경로정보를 나타내는 최대 문자열의 개수(청크사이즈)
    * = 5
    * */
    private static final int DEPTH_CHUNK_SIZE = 5;

    /*
    * 최대 depth
    * = 5
    * */
    private static final int MAX_DEPTH = 5;

    // MIN_CHUNK = "00000", MAX_CHUNK = "zzzzz" (각각의 끝 문자열을 5번 반복한 것이 최소/최대)
    private static final String MIN_CHUNK = String.valueOf(CHARSET.charAt(0)).repeat(DEPTH_CHUNK_SIZE);
    private static final String MAX_CHUNK = String.valueOf(CHARSET.charAt(CHARSET.length() - 1)).repeat(DEPTH_CHUNK_SIZE);

    /*
    * path 정보를 전달받아 생성
    * */
    public static CommentPath create(String path) {
        if (isDepthOverflowed(path)) {
            throw new IllegalStateException("depth overflowed");
        }
        CommentPath commentPath = new CommentPath();
        commentPath.path = path;
        return commentPath;
    }

    /*
    * 문자열 길이에 따른 depth 크기 반환
    * over flow > true, in range > false
    * */
    private static boolean isDepthOverflowed(String path) {
        return calDepth(path) > MAX_DEPTH;
    }

    private static int calDepth(String path) {
        return path.length() / DEPTH_CHUNK_SIZE;
    }

    public int getDepth() {
        return calDepth(path);
    }

    /*
    * 부모댓글인지 확인
    * */
    public boolean isRoot() {
        return calDepth(path) == 1;
    }

    /*
    * 현재 path의 최상위 부모댓글의 path를 추출(DEPTH 사이즈만큼 최초 5자리만 절삭)
    * */
    public String getParentPath() {
        return path.substring(0, path.length() - DEPTH_CHUNK_SIZE);
    }

    /*
    * 현재 댓글의 "하위" 댓글 path 생성하기
    * */
    public CommentPath createChildCommentPath(String descendantsTopPath) {
        if (descendantsTopPath == null) {
            //현재 댓글 path + 00000(min chunk)
            return CommentPath.create(path + MIN_CHUNK);
        }
        //현재 댓글 + 자식최근댓글에서 절삭 추출한, 현재 depth에서의 children top path
        String childrenTopPath = findChildrenTopPath(descendantsTopPath);
        return CommentPath.create(increase(childrenTopPath));
    }

    /*
    * 자신이 속한 계층의 댓글 채번을 위해 최근 자식 댓글들을 추출하여 절삭하는 과정 필요
    * - substring -> 절삭
    * - depth + 1 -> 최근 자식 댓글의 계층을 구하기 위해 현재 depth + 1
    * - chunk size (5) 만큼 곱해서 그만큼 모두 절삭함.
    *
    * 예를 들어 지금 1depth에서 children top path를 추출한다고 할때,
    * - 00000 01010 02020이 descendants top path라 할 경우
    * - 00000 01010을 추출해야 하므로 descendants top path . substring 0 ~ 11
    * */
    private String findChildrenTopPath(String descendantsTopPath) {
        //절삭 (**get depth = 부모댓글임, 즉 부모댓글의 "하위댓글"을 작성하는 것)
        return descendantsTopPath.substring(0, (getDepth() + 1) * DEPTH_CHUNK_SIZE);
    }

    private String increase(String path) {
        /*
        * 끝의 5자리를 넘겨받아 그대로 + 1
        * 00000 00000 -> 00000 00001 로, 현재 depth의 다음 path를 채번하기 위함
        * */
        String lastChunk = path.substring(path.length() - DEPTH_CHUNK_SIZE);

        //현재 depth에서 last chunk가 zzzzz일 경우 다음 댓글 작성 불가함
        if (isChunkOverflowed(lastChunk)) {
            throw new IllegalStateException("chunk overflowed");
        }

        int charsetLength = CHARSET.length();

        /*
        * 각 문자열을 CHARSET과 비교하여 10진수로 변환하는 과정
        * */
        int value = 0;
        for (char ch : lastChunk.toCharArray()) {
            value = value * charsetLength + CHARSET.indexOf(ch);
        }

        /*
        * 10진수 변환 값을 +1 한 후에
        * */
        value = value + 1;

        /*
        * 이를 다시 문자열로 변환하여 다음 path를 추출하는 과정
        * */
        String result = "";
        for (int i=0; i < DEPTH_CHUNK_SIZE; i++) {
            result = CHARSET.charAt(value % charsetLength) + result;
            value /= charsetLength;
        }

        /*
        * 현재 depth에서 마지막 5자리 뺀 앞자리 + 새로 채번한 뒤의 5자리
        * */
        return path.substring(0, path.length() - DEPTH_CHUNK_SIZE) + result;
    }

    private boolean isChunkOverflowed(String lastChunk) {
        return MAX_CHUNK.equals(lastChunk);
    }

}
