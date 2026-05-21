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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private static final String DEFAULT_COVER_IMAGE = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop";

    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final ImageRepository imageRepository;
    private final LocalImageStorageService localImageStorageService;

    public PageResponse<PostSummaryResponse> search(String category, String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return PageResponse.from(boardRepository.search(blankToNull(category), blankToNull(keyword), pageRequest).map(this::toSummary));
    }

    @Transactional
    public PostDetailResponse get(Long id) {
        Board board = findBoard(id);
        board.increaseViews();
        return toDetail(board);
    }

    @Transactional
    public PostDetailResponse create(PostCreateRequest request) {
        Board board = boardRepository.save(new Board(request.title(), request.content()));
        replaceCategories(board, request.category(), request.tags(), request.categories());
        replaceCoverImageUrl(board, request.coverImage());
        return toDetail(board);
    }

    @Transactional
    public PostDetailResponse create(PostCreateRequest request, List<MultipartFile> images) {
        List<StoredImage> storedImages = localImageStorageService.store(images);
        Board board = boardRepository.save(new Board(request.title(), replaceImagePlaceholders(request.content(), storedImages)));
        replaceCategories(board, request.category(), request.tags(), request.categories());
        replaceUploadedImages(board, storedImages);
        return toDetail(board);
    }

    @Transactional
    public PostDetailResponse update(Long id, PostUpdateRequest request) {
        Board board = findBoard(id);
        board.update(request.title(), request.content());
        replaceCategories(board, request.category(), request.tags(), request.categories());
        replaceCoverImageUrl(board, request.coverImage());
        return toDetail(board);
    }

    @Transactional
    public PostDetailResponse update(Long id, PostUpdateRequest request, List<MultipartFile> images) {
        Board board = findBoard(id);
        List<StoredImage> storedImages = localImageStorageService.store(images);
        board.update(request.title(), replaceImagePlaceholders(request.content(), storedImages));
        replaceCategories(board, request.category(), request.tags(), request.categories());
        if (!storedImages.isEmpty()) {
            replaceUploadedImages(board, storedImages);
        }
        return toDetail(board);
    }

    @Transactional
    public void delete(Long id) {
        Board board = findBoard(id);
        boardRepository.delete(board);
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
                .map(this::findOrCreateCategory)
                .map(category -> new BoardCategory(board, category))
                .forEach(boardCategory -> {
                    board.getBoardCategories().add(boardCategory);
                    boardCategoryRepository.save(boardCategory);
                });
    }

    private Category findOrCreateCategory(String name) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(new Category(name)));
    }

    private void replaceCoverImageUrl(Board board, String coverImage) {
        imageRepository.deleteByBoard(board);
        board.getImages().clear();
        if (coverImage != null && !coverImage.isBlank()) {
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
    }

    private void replaceUploadedImages(Board board, List<StoredImage> storedImages) {
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
    }

    private String replaceImagePlaceholders(String content, List<StoredImage> images) {
        String replaced = content;
        for (int i = 0; i < images.size(); i++) {
            StoredImage image = images.get(i);
            replaced = replaced.replace(
                    "[이미지: " + image.originalName() + "](pending-image:" + i + ")",
                    "![" + image.originalName() + "](" + image.publicUrl() + ")"
            );
            replaced = replaced.replace("pending-image:" + i, image.publicUrl());
        }
        if (!images.isEmpty() && replaced.contains("pending-image:")) {
            throw new IllegalArgumentException("Invalid image placeholder in content.");
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
        String normalized = content.replaceAll("#+", "").replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private String normalizeCoverImage(String coverImage) {
        return coverImage == null || coverImage.isBlank() ? DEFAULT_COVER_IMAGE : coverImage.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() || "All".equalsIgnoreCase(value) ? null : value.trim();
    }
}
