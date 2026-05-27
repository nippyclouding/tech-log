package com.nippyclouding.tech_log_back.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nippyclouding.tech_log_back.board.entity.Board;
import com.nippyclouding.tech_log_back.category.entity.BoardCategory;
import com.nippyclouding.tech_log_back.category.entity.Category;
import com.nippyclouding.tech_log_back.category.repository.BoardCategoryRepository;
import com.nippyclouding.tech_log_back.category.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BoardRepositorySearchTest {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BoardCategoryRepository boardCategoryRepository;

    @BeforeEach
    void setUp() {
        Category java = categoryRepository.save(new Category("Java"));
        Category spring = categoryRepository.save(new Category("Spring"));
        saveBoard("Spring Boot 배포", "EC2와 nginx 설정", spring);
        saveBoard("자바 컬렉션", "Stream 정리", java);
        saveBoard("운영 기록", "Spring 장애 분석", java);
    }

    @Test
    void findByKeyword_searchesTitleContentAndCategoryIgnoringCase() {
        assertThat(titles(boardRepository.findByKeyword("spring", PageRequest.of(0, 10)).getContent()))
                .containsExactlyInAnyOrder("Spring Boot 배포", "운영 기록");

        assertThat(titles(boardRepository.findByKeyword("java", PageRequest.of(0, 10)).getContent()))
                .containsExactlyInAnyOrder("자바 컬렉션", "운영 기록");
    }

    @Test
    void findByCategoryAndKeyword_appliesBothFilters() {
        assertThat(titles(boardRepository.findByCategoryAndKeyword("Java", "spring", PageRequest.of(0, 10)).getContent()))
                .containsExactly("운영 기록");
    }

    private void saveBoard(String title, String content, Category category) {
        Board board = boardRepository.save(new Board(title, content));
        boardCategoryRepository.save(new BoardCategory(board, category));
    }

    private List<String> titles(List<Board> boards) {
        return boards.stream().map(Board::getTitle).toList();
    }
}
