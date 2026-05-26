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
    Page<Board> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"boardCategories.category", "images"})
    @Query("select distinct b from Board b join b.boardCategories bc join bc.category c where c.name = :category")
    Page<Board> findByCategory(@Param("category") String category, Pageable pageable);

    @EntityGraph(attributePaths = {"boardCategories.category", "images"})
    @Query("select distinct b from Board b left join b.boardCategories bc left join bc.category c where lower(b.title) like concat('%', lower(:keyword), '%') or lower(b.content) like concat('%', lower(:keyword), '%') or lower(c.name) like concat('%', lower(:keyword), '%')")
    Page<Board> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"boardCategories.category", "images"})
    @Query("select distinct b from Board b join b.boardCategories bc join bc.category c where c.name = :category and (lower(b.title) like concat('%', lower(:keyword), '%') or lower(b.content) like concat('%', lower(:keyword), '%') or lower(c.name) like concat('%', lower(:keyword), '%'))")
    Page<Board> findByCategoryAndKeyword(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"boardCategories.category", "images"})
    Optional<Board> findById(Long id);
}
