package com.nippyclouding.tech_log_back.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nippyclouding.tech_log_back.board.dto.PostCreateRequest;
import com.nippyclouding.tech_log_back.board.dto.PostDetailResponse;
import com.nippyclouding.tech_log_back.board.entity.Board;
import com.nippyclouding.tech_log_back.board.repository.BoardRepository;
import com.nippyclouding.tech_log_back.category.entity.BoardCategory;
import com.nippyclouding.tech_log_back.category.entity.Category;
import com.nippyclouding.tech_log_back.category.repository.BoardCategoryRepository;
import com.nippyclouding.tech_log_back.category.repository.CategoryRepository;
import com.nippyclouding.tech_log_back.global.exception.BusinessException;
import com.nippyclouding.tech_log_back.global.exception.ErrorCode;
import com.nippyclouding.tech_log_back.image.entity.Image;
import com.nippyclouding.tech_log_back.image.repository.ImageRepository;
import com.nippyclouding.tech_log_back.image.service.LocalImageStorageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BoardCategoryRepository boardCategoryRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private LocalImageStorageService localImageStorageService;

    @InjectMocks
    private BoardService boardService;

    @Test
    @DisplayName("게시글을 조회하면 조회수가 1 증가하고 상세 응답을 반환한다")
    void get_increasesViewsAndReturnsDetail() {
        // given
        Board board = board("테스트 제목", "테스트 본문입니다.");
        setId(board, 1L);
        Category category = category("Spring");
        board.getBoardCategories().add(new BoardCategory(board, category));

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when
        PostDetailResponse response = boardService.get(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("테스트 제목");
        assertThat(response.category()).isEqualTo("Spring");
        assertThat(response.views()).isEqualTo(1L);
        assertThat(board.getViews()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 BOARD_NOT_FOUND 예외가 발생한다")
    void get_throwsBoardNotFoundWhenMissing() {
        // given
        given(boardRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.get(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 생성 시 카테고리, 태그 중복을 제거하고 대표 이미지를 저장한다")
    void create_deduplicatesCategoriesAndStoresCoverImage() {
        // given
        PostCreateRequest request = new PostCreateRequest(
                "새 글",
                null,
                "새 글 본문입니다.",
                "Spring",
                "/image/cover.png",
                List.of("Java", "Spring"),
                List.of("Backend", "Java")
        );

        given(boardRepository.save(any(Board.class))).willAnswer(invocation -> {
            Board savedBoard = invocation.getArgument(0);
            setId(savedBoard, 10L);
            return savedBoard;
        });
        given(categoryRepository.findByName("Spring")).willReturn(Optional.of(category("Spring")));
        given(categoryRepository.findByName("Backend")).willReturn(Optional.of(category("Backend")));
        given(categoryRepository.findByName("Java")).willReturn(Optional.empty());
        given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        PostDetailResponse response = boardService.create(request);

        // then
        assertThat(response.title()).isEqualTo("새 글");
        assertThat(response.tags()).containsExactly("Spring", "Backend", "Java");
        assertThat(response.coverImage()).isEqualTo("/image/cover.png");
        verify(boardCategoryRepository, times(3)).save(any(BoardCategory.class));

        ArgumentCaptor<Image> imageCaptor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().isThumbnail()).isTrue();
        assertThat(imageCaptor.getValue().getFileKey()).isEqualTo("/image/cover.png");
    }

    @Test
    @DisplayName("게시글 생성 시 카테고리가 하나도 없으면 예외가 발생한다")
    void create_throwsWhenCategoriesAreEmpty() {
        // given
        PostCreateRequest request = new PostCreateRequest("새 글", null, "본문", null, null, List.of(), List.of());
        given(boardRepository.save(any(Board.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when & then
        assertThatThrownBy(() -> boardService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one category is required.");
    }

    private Board board(String title, String content) {
        return new Board(title, content);
    }

    private Category category(String name) {
        return new Category(name);
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
