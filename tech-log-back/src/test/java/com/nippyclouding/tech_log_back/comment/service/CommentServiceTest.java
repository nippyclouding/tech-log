package com.nippyclouding.tech_log_back.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nippyclouding.tech_log_back.auth.security.GithubUser;
import com.nippyclouding.tech_log_back.board.entity.Board;
import com.nippyclouding.tech_log_back.board.repository.BoardRepository;
import com.nippyclouding.tech_log_back.comment.dto.CommentCreateRequest;
import com.nippyclouding.tech_log_back.comment.dto.CommentResponse;
import com.nippyclouding.tech_log_back.comment.dto.CommentUpdateRequest;
import com.nippyclouding.tech_log_back.comment.entity.Comment;
import com.nippyclouding.tech_log_back.comment.repository.CommentRepository;
import com.nippyclouding.tech_log_back.global.exception.BusinessException;
import com.nippyclouding.tech_log_back.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("GitHub 사용자 정보로 댓글을 생성한다")
    void create_savesCommentWithGithubUser() {
        // given
        Board board = new Board("게시글", "게시글 본문입니다.");
        ReflectionTestUtils.setField(board, "id", 1L);
        GithubUser githubUser = new GithubUser(100L, "octocat", "Mona", null, "https://avatar.example/octocat.png");
        CommentCreateRequest request = new CommentCreateRequest(" 안녕하세요 ");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 5L);
            return comment;
        });

        // when
        CommentResponse response = commentService.create(1L, request, githubUser, "127.0.0.1");

        // then
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.authorName()).isEqualTo("octocat");
        assertThat(response.content()).isEqualTo("안녕하세요");
        assertThat(response.authorGithubUrl()).isEqualTo("https://github.com/octocat");
        assertThat(response.ownedByCurrentUser()).isTrue();
    }

    @Test
    @DisplayName("댓글 생성 시 게시글이 없으면 BOARD_NOT_FOUND 예외가 발생한다")
    void create_throwsWhenBoardMissing() {
        // given
        GithubUser githubUser = new GithubUser(100L, "octocat", "Mona", null, "");
        given(boardRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.create(1L, new CommentCreateRequest("댓글"), githubUser, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글을 삭제하면 삭제 표시가 되고 응답에서는 삭제 문구가 노출된다")
    void delete_marksCommentDeleted() {
        // given
        Board board = new Board("게시글", "게시글 본문입니다.");
        ReflectionTestUtils.setField(board, "id", 1L);
        Comment comment = new Comment(board, "삭제될 댓글", 100L, "octocat", "", "127.0.0.1");
        ReflectionTestUtils.setField(comment, "id", 5L);
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));

        // when
        commentService.delete(5L);

        // then
        assertThat(comment.isDeleted()).isTrue();
        assertThat(CommentResponse.from(comment).content()).isEqualTo("삭제된 댓글입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 삭제하면 COMMENT_NOT_FOUND 예외가 발생한다")
    void delete_throwsWhenCommentMissing() {
        // given
        given(commentRepository.findById(5L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.delete(5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자는 자신의 댓글 내용을 수정할 수 있다")
    void update_updatesOwnedComment() {
        Board board = new Board("게시글", "게시글 본문입니다.");
        ReflectionTestUtils.setField(board, "id", 1L);
        Comment comment = new Comment(board, "이전 댓글", 100L, "octocat", "", "127.0.0.1");
        ReflectionTestUtils.setField(comment, "id", 5L);
        GithubUser githubUser = new GithubUser(100L, "octocat", "Mona", null, "");
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));

        CommentResponse response = commentService.update(1L, 5L, new CommentUpdateRequest(" 수정된 댓글 "), githubUser);

        assertThat(response.content()).isEqualTo("수정된 댓글");
        assertThat(response.ownedByCurrentUser()).isTrue();
    }

    @Test
    @DisplayName("작성자는 자신의 댓글을 삭제할 수 있다")
    void deleteOwn_marksOwnedCommentDeleted() {
        Board board = new Board("게시글", "게시글 본문입니다.");
        ReflectionTestUtils.setField(board, "id", 1L);
        Comment comment = new Comment(board, "삭제될 댓글", 100L, "octocat", "", "127.0.0.1");
        GithubUser githubUser = new GithubUser(100L, "octocat", "Mona", null, "");
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));

        commentService.deleteOwn(1L, 5L, githubUser);

        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("다른 작성자의 댓글은 수정할 수 없다")
    void update_throwsWhenNotOwner() {
        Board board = new Board("게시글", "게시글 본문입니다.");
        ReflectionTestUtils.setField(board, "id", 1L);
        Comment comment = new Comment(board, "다른 사람 댓글", 200L, "other", "", "127.0.0.1");
        GithubUser githubUser = new GithubUser(100L, "octocat", "Mona", null, "");
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(1L, 5L, new CommentUpdateRequest("수정"), githubUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("다른 게시글에 속한 댓글은 본인 댓글이라도 삭제할 수 없다")
    void deleteOwn_throwsWhenPostDoesNotMatch() {
        Board board = new Board("게시글", "게시글 본문입니다.");
        ReflectionTestUtils.setField(board, "id", 2L);
        Comment comment = new Comment(board, "댓글", 100L, "octocat", "", "127.0.0.1");
        GithubUser githubUser = new GithubUser(100L, "octocat", "Mona", null, "");
        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteOwn(1L, 5L, githubUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }
}
