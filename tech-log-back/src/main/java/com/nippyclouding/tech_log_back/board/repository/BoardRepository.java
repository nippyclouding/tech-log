package com.nippyclouding.tech_log_back.board.repository;

import com.nippyclouding.tech_log_back.board.entity.Board;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @EntityGraph(attributePaths = {"boardCategories.category", "images"})
    @Query("select distinct b from Board b left join b.boardCategories bc left join bc.category c where (:category is null or c.name = :category) and (:keyword is null or lower(b.title) like lower(concat('%', :keyword, '%')) or lower(b.content) like lower(concat('%', :keyword, '%')) or lower(c.name) like lower(concat('%', :keyword, '%')))")
    Page<Board> search(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"boardCategories.category", "images"})
    Optional<Board> findById(Long id);
}
