package com.nippyclouding.tech_log_back.comment.repository;

import com.nippyclouding.tech_log_back.comment.entity.Comment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "board")
    List<Comment> findByBoardIdAndDeletedFalseOrderByUpdatedAtAsc(Long boardId);

    @EntityGraph(attributePaths = "board")
    List<Comment> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = "board")
    Page<Comment> findAllByOrderByUpdatedAtAsc(Pageable pageable);
}
