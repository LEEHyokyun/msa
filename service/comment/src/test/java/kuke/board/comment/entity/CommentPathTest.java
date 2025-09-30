package kuke.board.comment.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentPathTest {
    @Test
    void createChildCommentTest() {
        /*
        * param이 좀 헷갈리게 되어있는데.. 내가 생성하고 싶은 댓글의 depth를 기준으로.
        * param = "현황"임.
        * param 1 - 부모댓글의 depth(*부모댓글의 depth가 root이면 빈 문자열로 표기)
        * param 2 - 자식댓글의 depth
        * */
        // 00000 <- 생성(최초 생성)
        createChildCommentTest(CommentPath.create(""), null, "00000");

        // 00000
        //      00000 <- 생성
        createChildCommentTest(CommentPath.create("00000"), null, "0000000000");

        // 00000
        // 00001 <- 생성
        createChildCommentTest(CommentPath.create(""), "00000", "00001");

        // 0000z
        //      abcdz
        //          zzzzz
        //              zzzzz
        //      abce0 <- 생성
        createChildCommentTest(CommentPath.create("0000z"), "0000zabcdzzzzzzzzzzz", "0000zabce0");
    }

    void createChildCommentTest(CommentPath commentPath, String descendantsTopPath, String expectedChildPath) {
        CommentPath childCommentPath = commentPath.createChildCommentPath(descendantsTopPath);
        assertThat(childCommentPath.getPath()).isEqualTo(expectedChildPath);
    }

    /*
    * max depth
    * = isDepthOverflowed
    * */
    @Test
    void createChildCommentPathIfMaxDepthTest() {
        assertThatThrownBy(() ->
                CommentPath.create("zzzzz".repeat(5)).createChildCommentPath(null)
        ).isInstanceOf(IllegalStateException.class);
    }

    /*
    * max chunk
    * -> isChunkOverflowed
    * */
    @Test
    void createChildCommentPathIfChunkOverflowTest() {
        // given
        CommentPath commentPath = CommentPath.create("");

        // when, then
        assertThatThrownBy(() -> commentPath.createChildCommentPath("zzzzz"))
                .isInstanceOf(IllegalStateException.class);
    }
}