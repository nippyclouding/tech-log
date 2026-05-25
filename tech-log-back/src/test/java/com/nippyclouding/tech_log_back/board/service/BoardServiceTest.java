package com.nippyclouding.tech_log_back.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nippyclouding.tech_log_back.board.dto.PostCreateRequest;
import com.nippyclouding.tech_log_back.board.dto.PostDetailResponse;
import com.nippyclouding.tech_log_back.board.dto.PostUpdateRequest;
import com.nippyclouding.tech_log_back.board.entity.Board;
import com.nippyclouding.tech_log_back.board.repository.BoardRepository;
import com.nippyclouding.tech_log_back.category.entity.BoardCategory;
import com.nippyclouding.tech_log_back.category.entity.Category;
import com.nippyclouding.tech_log_back.category.repository.BoardCategoryRepository;
import com.nippyclouding.tech_log_back.category.repository.CategoryRepository;
import com.nippyclouding.tech_log_back.global.exception.BusinessException;
import com.nippyclouding.tech_log_back.global.exception.ErrorCode;
import com.nippyclouding.tech_log_back.image.entity.Image;
import com.nippyclouding.tech_log_back.image.entity.StorageType;
import com.nippyclouding.tech_log_back.image.repository.ImageRepository;
import com.nippyclouding.tech_log_back.image.service.LocalImageStorageService;
import com.nippyclouding.tech_log_back.image.service.StoredImage;
import com.nippyclouding.tech_log_back.newsletter.event.PostPublishedEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BoardService boardService;

    @Test
    @DisplayName("공개 게시글 조회는 조회수를 변경하지 않고 상세 응답을 반환한다")
    void get_doesNotMutateViewsAndReturnsDetail() {
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
        assertThat(response.views()).isZero();
        assertThat(board.getViews()).isZero();
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
    @DisplayName("공개 게시글 검색은 50개를 넘는 페이지 크기를 거부한다")
    void search_rejectsOversizedPublicPage() {
        assertThatThrownBy(() -> boardService.search(null, null, 0, 51))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be between 1 and 50.");

        verify(boardRepository, never()).search(any(), any(), any());
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
        given(categoryRepository.findByName("Java")).willReturn(Optional.of(category("Java")));

        // when
        PostDetailResponse response = boardService.create(request);

        // then
        assertThat(response.title()).isEqualTo("새 글");
        assertThat(response.tags()).containsExactly("Spring", "Backend", "Java");
        assertThat(response.coverImage()).isEqualTo("/image/cover.png");
        verify(boardCategoryRepository, times(3)).save(any(BoardCategory.class));
        verify(eventPublisher).publishEvent(any(PostPublishedEvent.class));

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

    @Test
    @DisplayName("게시글 저장은 카테고리 메뉴에서 생성되지 않은 카테고리를 자동 생성하지 않는다")
    void create_rejectsUnregisteredCategory() {
        // given
        PostCreateRequest request = new PostCreateRequest("새 글", null, "본문", "NewCategory", null, List.of(), List.of());
        given(boardRepository.save(any(Board.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(categoryRepository.findByName("NewCategory")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("여러 업로드 이미지 placeholder를 해당 이미지 URL로 치환한다")
    void create_replacesMultipleUploadedImagePlaceholders() {
        // given
        List<StoredImage> images = uploadedImages(2);
        given(localImageStorageService.store(any())).willReturn(images);
        given(boardRepository.save(any(Board.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(categoryRepository.findByName("Spring")).willReturn(Optional.of(category("Spring")));

        PostCreateRequest request = imagePostRequest(images);

        // when
        PostDetailResponse response = boardService.create(request, List.of());

        // then
        assertThat(response.content())
                .contains("![image-0.png](/image/0.png)")
                .contains("![image-1.png](/image/1.png)");
    }

    @Test
    @DisplayName("두 자리 이미지 순번도 앞 순번 URL과 충돌하지 않는다")
    void create_replacesDoubleDigitUploadedImagePlaceholderWithoutPrefixCollision() {
        // given
        List<StoredImage> images = uploadedImages(11);
        given(localImageStorageService.store(any())).willReturn(images);
        given(boardRepository.save(any(Board.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(categoryRepository.findByName("Spring")).willReturn(Optional.of(category("Spring")));

        PostCreateRequest request = imagePostRequest(images);

        // when
        PostDetailResponse response = boardService.create(request, List.of());

        // then
        assertThat(response.content())
                .contains("![image-10.png](/image/10.png)")
                .doesNotContain("/image/1.png0");
    }

    @Test
    @DisplayName("이미지 수정 시 본문에서 더 이상 참조하지 않는 이전 파일만 삭제한다")
    void updateWithImages_deletesOnlyUnreferencedPreviousFile() {
        // given
        Board board = board("기존 글", "기존 본문");
        setId(board, 1L);
        board.getImages().add(uploadedImage(board, "keep.png", true));
        board.getImages().add(uploadedImage(board, "remove.png", false));
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(categoryRepository.findByName("Spring")).willReturn(Optional.of(category("Spring")));
        given(localImageStorageService.store(any())).willReturn(uploadedImages(1));
        PostUpdateRequest request = new PostUpdateRequest(
                "수정 글",
                null,
                "기존 이미지 ![](/image/keep.png)\n\n[이미지: image-0.png](pending-image:0)",
                "Spring",
                null,
                List.of(),
                List.of("Spring")
        );

        TransactionSynchronizationManager.initSynchronization();
        try {
            // when
            boardService.update(1L, request, List.of());

            // then
            verify(localImageStorageService, never()).deleteStoredFiles(List.of("remove.png"));
            assertThat(board.getImages()).extracting(Image::getStoredName).contains("keep.png").doesNotContain("remove.png");
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(localImageStorageService).deleteStoredFiles(List.of("remove.png"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("이미지 업로드 후 게시글 저장이 실패하면 새 파일을 롤백 정리한다")
    void createWithImages_cleansUploadedFilesAfterRollback() {
        // given
        List<StoredImage> images = uploadedImages(1);
        given(localImageStorageService.store(any())).willReturn(images);
        given(boardRepository.save(any(Board.class))).willAnswer(invocation -> invocation.getArgument(0));
        PostCreateRequest invalidRequest = new PostCreateRequest("이미지 글", null, "본문", null, null, List.of(), List.of());

        TransactionSynchronizationManager.initSynchronization();
        try {
            // when & then
            assertThatThrownBy(() -> boardService.create(invalidRequest, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(localImageStorageService, never()).deleteStoredFiles(List.of("0.png"));

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(localImageStorageService).deleteStoredFiles(List.of("0.png"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private PostCreateRequest imagePostRequest(List<StoredImage> images) {
        String content = images.stream()
                .map(image -> "[이미지: " + image.originalName() + "](pending-image:" + image.order() + ")")
                .reduce("본문", (result, placeholder) -> result + "\n\n" + placeholder);
        return new PostCreateRequest("이미지 글", null, content, "Spring", null, List.of(), List.of("Spring"));
    }

    private List<StoredImage> uploadedImages(int count) {
        return IntStream.range(0, count)
                .mapToObj(order -> new StoredImage(
                        "/image/" + order + ".png",
                        "image-" + order + ".png",
                        order + ".png",
                        "image/png",
                        1L,
                        order,
                        order == 0
                ))
                .toList();
    }

    private Image uploadedImage(Board board, String storedName, boolean thumbnail) {
        return new Image(
                board,
                StorageType.LOCAL,
                "/image/" + storedName,
                storedName,
                storedName,
                "image/png",
                1L,
                thumbnail ? 0 : 1,
                thumbnail
        );
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
