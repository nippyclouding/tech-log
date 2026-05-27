package com.nippyclouding.tech_log_back.board.service;

import com.nippyclouding.tech_log_back.board.entity.Board;
import com.nippyclouding.tech_log_back.board.repository.BoardRepository;
import com.nippyclouding.tech_log_back.category.entity.BoardCategory;
import com.nippyclouding.tech_log_back.category.repository.BoardCategoryRepository;
import com.nippyclouding.tech_log_back.category.entity.Category;
import com.nippyclouding.tech_log_back.category.repository.CategoryRepository;
import com.nippyclouding.tech_log_back.image.entity.Image;
import com.nippyclouding.tech_log_back.image.repository.ImageRepository;
import com.nippyclouding.tech_log_back.image.entity.StorageType;
import com.nippyclouding.tech_log_back.image.service.LocalImageStorageService;
import com.nippyclouding.tech_log_back.image.service.StoredImage;
import com.nippyclouding.tech_log_back.newsletter.event.PostPublishedEvent;
import com.nippyclouding.tech_log_back.global.exception.BusinessException;
import com.nippyclouding.tech_log_back.global.exception.ErrorCode;
import com.nippyclouding.tech_log_back.global.dto.PageResponse;
import com.nippyclouding.tech_log_back.board.dto.PostCreateRequest;
import com.nippyclouding.tech_log_back.board.dto.PostDetailResponse;
import com.nippyclouding.tech_log_back.board.dto.PostSummaryResponse;
import com.nippyclouding.tech_log_back.board.dto.PostUpdateRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private static final String DEFAULT_COVER_IMAGE = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop";
    private static final int MAX_PUBLIC_PAGE_SIZE = 50;

    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final ImageRepository imageRepository;
    private final LocalImageStorageService localImageStorageService;
    private final ApplicationEventPublisher eventPublisher;

    // 검색
    public PageResponse<PostSummaryResponse> search(String category, String keyword, int page, int size) {
        validatePublicPaging(page, size);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        String categoryFilter = blankToNull(category);
        String keywordFilter = blankToNull(keyword);
        Page<Board> boards;
        if (categoryFilter == null && keywordFilter == null) {
            boards = boardRepository.findAll(pageRequest);
        } else if (categoryFilter == null) {
            boards = boardRepository.findByKeyword(keywordFilter, pageRequest);
        } else if (keywordFilter == null) {
            boards = boardRepository.findByCategory(categoryFilter, pageRequest);
        } else {
            boards = boardRepository.findByCategoryAndKeyword(categoryFilter, keywordFilter, pageRequest);
        }
        return PageResponse.from(boards.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public PostDetailResponse get(Long id) {
        Board board = findBoard(id);
        return toDetail(board);
    }

    @Transactional
    public PostDetailResponse create(PostCreateRequest request) {
        Board board = boardRepository.save(new Board(request.title(), request.content()));
        replaceCategories(board, request.category(), request.tags(), request.categories());
        replaceCoverImageUrl(board, request.coverImage());
        PostDetailResponse response = toDetail(board);
        publishPostCreated(response);
        return response;
    }

    @Transactional
    public PostDetailResponse create(PostCreateRequest request, List<MultipartFile> images) {
        List<StoredImage> storedImages = localImageStorageService.store(images);
        cleanupStoredImagesOnRollback(storedImages);
        Board board = boardRepository.save(new Board(request.title(), replaceImagePlaceholders(request.content(), storedImages)));
        replaceCategories(board, request.category(), request.tags(), request.categories());
        replaceUploadedImages(board, storedImages, List.of());
        PostDetailResponse response = toDetail(board);
        publishPostCreated(response);
        return response;
    }

    @Transactional
    public PostDetailResponse update(Long id, PostUpdateRequest request) {
        Board board = findBoard(id);
        List<Image> retainedImages = referencedUploadedImages(board, request.content(), request.coverImage());
        List<String> obsoleteStoredNames = unreferencedUploadedStoredNames(board, request.content(), request.coverImage());
        board.update(request.title(), request.content());
        replaceCategories(board, request.category(), request.tags(), request.categories());
        replaceCoverImageUrl(board, request.coverImage(), retainedImages);
        deleteStoredFilesAfterCommit(obsoleteStoredNames);
        return toDetail(board);
    }

    @Transactional
    public PostDetailResponse update(Long id, PostUpdateRequest request, List<MultipartFile> images) {
        Board board = findBoard(id);
        List<StoredImage> storedImages = localImageStorageService.store(images);
        cleanupStoredImagesOnRollback(storedImages);
        board.update(request.title(), replaceImagePlaceholders(request.content(), storedImages));
        replaceCategories(board, request.category(), request.tags(), request.categories());
        List<Image> retainedImages = referencedUploadedImages(board, board.getContent(), null);
        List<String> obsoleteStoredNames = unreferencedUploadedStoredNames(board, board.getContent(), null);
        if (!storedImages.isEmpty() || !obsoleteStoredNames.isEmpty()) {
            replaceUploadedImages(board, storedImages, retainedImages);
        }
        deleteStoredFilesAfterCommit(obsoleteStoredNames);
        return toDetail(board);
    }

    @Transactional
    public void delete(Long id) {
        Board board = findBoard(id);
        List<String> previousStoredNames = uploadedStoredNames(board);
        boardRepository.delete(board);
        deleteStoredFilesAfterCommit(previousStoredNames);
    }

    private Board findBoard(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
    }

    private void replaceCategories(Board board, String categoryName, List<String> tagNames, List<String> categoryNames) {
        Set<String> names = new LinkedHashSet<>();
        if (categoryName != null && !categoryName.isBlank()) {
            names.add(categoryName.trim());
        }
        if (categoryNames != null) {
            categoryNames.stream().filter(category -> category != null && !category.isBlank()).map(String::trim).forEach(names::add);
        }
        if (tagNames != null) {
            tagNames.stream().filter(tag -> tag != null && !tag.isBlank()).map(String::trim).forEach(names::add);
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("At least one category is required.");
        }

        boardCategoryRepository.deleteByBoard(board);
        board.getBoardCategories().clear();
        names.stream()
                .map(this::findCategory)
                .map(category -> new BoardCategory(board, category))
                .forEach(boardCategory -> {
                    board.getBoardCategories().add(boardCategory);
                    boardCategoryRepository.save(boardCategory);
                });
    }

    private Category findCategory(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private void replaceCoverImageUrl(Board board, String coverImage) {
        replaceCoverImageUrl(board, coverImage, List.of());
    }

    private void replaceCoverImageUrl(Board board, String coverImage, List<Image> retainedImages) {
        imageRepository.deleteByBoard(board);
        board.getImages().clear();
        boolean hasCoverImage = coverImage != null && !coverImage.isBlank();
        if (hasCoverImage) {
            Image image = new Image(
                    board,
                    StorageType.LOCAL,
                    normalizeCoverImage(coverImage),
                    "cover-image",
                    "cover-image",
                    "text/uri-list",
                    0L,
                    0,
                    true
            );
            board.getImages().add(image);
            imageRepository.save(image);
        }
        retainUploadedImages(board, retainedImages, !hasCoverImage);
    }

    private void replaceUploadedImages(Board board, List<StoredImage> storedImages, List<Image> retainedImages) {
        imageRepository.deleteByBoard(board);
        board.getImages().clear();
        storedImages.stream()
                .map(image -> new Image(
                        board,
                        StorageType.LOCAL,
                        image.publicUrl(),
                        image.originalName(),
                        image.storedName(),
                        image.contentType(),
                        image.fileSize(),
                        image.order(),
                        image.thumbnail()
                ))
                .forEach(image -> {
                    board.getImages().add(image);
                    imageRepository.save(image);
                });
        retainUploadedImages(board, retainedImages, storedImages.isEmpty());
    }

    private String replaceImagePlaceholders(String content, List<StoredImage> images) {
        String replaced = content;
        for (int i = images.size() - 1; i >= 0; i--) {
            StoredImage image = images.get(i);
            replaced = replaced.replace(
                    "[이미지: " + image.originalName() + "](pending-image:" + i + ")",
                    "![이미지 " + (i + 1) + "](" + image.publicUrl() + ")"
            );
            replaced = replaced.replace("pending-image:" + i, image.publicUrl());
        }
        if (replaced.contains("pending-image:")) {
            throw new IllegalArgumentException("본문에 배치한 이미지 파일을 다시 선택하세요.");
        }
        boolean hasInsertedImage = images.stream().anyMatch(image -> content.contains("pending-image:" + image.order()));
        if (!images.isEmpty() && !hasInsertedImage) {
            String appendedImages = images.stream()
                    .map(image -> "\n\n![" + image.originalName() + "](" + image.publicUrl() + ")")
                    .reduce("", String::concat);
            return replaced + appendedImages;
        }
        return replaced;
    }

    private PostSummaryResponse toSummary(Board board) {
        List<String> tags = tags(board);
        return PostSummaryResponse.of(
                board.getId(),
                board.getTitle(),
                excerpt(board.getContent()),
                board.getUpdatedAt(),
                tags.isEmpty() ? "General" : tags.get(0),
                tags,
                coverImage(board),
                board.getViews()
        );
    }

    private PostDetailResponse toDetail(Board board) {
        PostSummaryResponse summary = toSummary(board);
        return new PostDetailResponse(
                summary.id(),
                summary.title(),
                summary.excerpt(),
                board.getContent(),
                summary.date(),
                summary.author(),
                summary.category(),
                summary.tags(),
                summary.coverImage(),
                summary.published(),
                summary.views()
        );
    }

    private List<String> tags(Board board) {
        return board.getBoardCategories()
                .stream()
                .map(BoardCategory::getCategory)
                .map(Category::getName)
                .distinct()
                .toList();
    }

    private String coverImage(Board board) {
        return board.getImages()
                .stream()
                .filter(Image::isThumbnail)
                .findFirst()
                .map(Image::getFileKey)
                .orElse(DEFAULT_COVER_IMAGE);
    }

    private String excerpt(String content) {
        String normalized = content
                .replaceAll("(?m)^\\[align=(left|center|right)]\\s*$", "")
                .replaceAll("(?m)^\\[/align]\\s*$", "")
                .replaceAll("!\\[[^\\]]*]\\([^)]+\\)", "")
                .replaceAll("\\[([^\\]]+)]\\([^)]+\\)", "$1")
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("\\*([^*]+)\\*", "$1")
                .replaceAll("#+", "")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private String normalizeCoverImage(String coverImage) {
        return coverImage == null || coverImage.isBlank() ? DEFAULT_COVER_IMAGE : coverImage.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() || "All".equalsIgnoreCase(value) ? null : value.trim();
    }

    private void publishPostCreated(PostDetailResponse response) {
        eventPublisher.publishEvent(new PostPublishedEvent(response.id(), response.title()));
    }

    private void validatePublicPaging(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative.");
        }
        if (size < 1 || size > MAX_PUBLIC_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PUBLIC_PAGE_SIZE + ".");
        }
    }

    private List<String> uploadedStoredNames(Board board) {
        return board.getImages().stream()
                .filter(image -> image.getStorageType() == StorageType.LOCAL)
                .map(Image::getStoredName)
                .filter(storedName -> storedName != null && !"cover-image".equals(storedName))
                .toList();
    }

    private List<String> unreferencedUploadedStoredNames(Board board, String content, String coverImage) {
        String references = (content == null ? "" : content) + "\n" + (coverImage == null ? "" : coverImage);
        return uploadedStoredNames(board).stream()
                .filter(storedName -> !references.contains(storedName))
                .toList();
    }

    private List<Image> referencedUploadedImages(Board board, String content, String coverImage) {
        String references = (content == null ? "" : content) + "\n" + (coverImage == null ? "" : coverImage);
        return board.getImages().stream()
                .filter(image -> image.getStorageType() == StorageType.LOCAL)
                .filter(image -> image.getStoredName() != null && !"cover-image".equals(image.getStoredName()))
                .filter(image -> references.contains(image.getStoredName()))
                .toList();
    }

    private void retainUploadedImages(Board board, List<Image> retainedImages, boolean preserveThumbnail) {
        retainedImages.stream()
                .map(image -> new Image(
                        board,
                        image.getStorageType(),
                        image.getFileKey(),
                        image.getOriginalName(),
                        image.getStoredName(),
                        image.getContentType(),
                        image.getFileSize(),
                        image.getImageOrder(),
                        preserveThumbnail && image.isThumbnail()
                ))
                .forEach(image -> {
                    board.getImages().add(image);
                    imageRepository.save(image);
                });
    }

    private void deleteStoredFilesAfterCommit(List<String> storedNames) {
        if (storedNames.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            localImageStorageService.deleteStoredFiles(storedNames);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                localImageStorageService.deleteStoredFiles(storedNames);
            }
        });
    }

    private void cleanupStoredImagesOnRollback(List<StoredImage> storedImages) {
        List<String> storedNames = storedImages.stream().map(StoredImage::storedName).toList();
        if (storedNames.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    localImageStorageService.deleteStoredFiles(storedNames);
                }
            }
        });
    }
}
