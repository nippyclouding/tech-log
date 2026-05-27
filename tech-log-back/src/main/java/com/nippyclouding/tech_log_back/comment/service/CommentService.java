package com.nippyclouding.tech_log_back.comment.service;

import com.nippyclouding.tech_log_back.board.entity.Board;
import com.nippyclouding.tech_log_back.board.repository.BoardRepository;
import com.nippyclouding.tech_log_back.comment.entity.Comment;
import com.nippyclouding.tech_log_back.comment.repository.CommentRepository;
import com.nippyclouding.tech_log_back.global.exception.BusinessException;
import com.nippyclouding.tech_log_back.global.exception.ErrorCode;
import com.nippyclouding.tech_log_back.auth.security.GithubUser;
import com.nippyclouding.tech_log_back.comment.dto.CommentCreateRequest;
import com.nippyclouding.tech_log_back.comment.dto.AdminCommentResponse;
import com.nippyclouding.tech_log_back.comment.dto.CommentResponse;
import com.nippyclouding.tech_log_back.comment.dto.CommentUpdateRequest;
import com.nippyclouding.tech_log_back.global.dto.PageResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;

    public List<CommentResponse> findByBoard(Long boardId, Long viewerGithubId) {
        return commentRepository.findByBoardIdAndDeletedFalseOrderByUpdatedAtAsc(boardId)
                .stream()
                .map(comment -> CommentResponse.from(comment, viewerGithubId))
                .toList();
    }

    public List<CommentResponse> findAllForAdmin() {
        return commentRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    public PageResponse<AdminCommentResponse> findAllForAdmin(int page, int size) {
        return PageResponse.from(commentRepository.findAllByOrderByUpdatedAtAsc(PageRequest.of(page, size)).map(AdminCommentResponse::from));
    }

    @Transactional
    public CommentResponse create(Long boardId, CommentCreateRequest request, GithubUser githubUser, String accessIp) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
        Comment comment = new Comment(
                board,
                request.content(),
                githubUser.id(),
                githubUser.login().isBlank() ? githubUser.name() : githubUser.login(),
                githubUser.avatarUrl(),
                accessIp
        );
        return CommentResponse.from(commentRepository.save(comment), githubUser.id());
    }

    @Transactional
    public CommentResponse update(Long boardId, Long id, CommentUpdateRequest request, GithubUser githubUser) {
        Comment comment = findOwnedComment(boardId, id, githubUser);
        comment.updateContent(request.content());
        return CommentResponse.from(comment, githubUser.id());
    }

    @Transactional
    public void deleteOwn(Long boardId, Long id, GithubUser githubUser) {
        findOwnedComment(boardId, id, githubUser).delete();
    }

    @Transactional
    public void delete(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        comment.delete();
    }

    private Comment findOwnedComment(Long boardId, Long id, GithubUser githubUser) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.isDeleted() || !comment.getBoard().getId().equals(boardId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getGithubId().equals(githubUser.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return comment;
    }
}
