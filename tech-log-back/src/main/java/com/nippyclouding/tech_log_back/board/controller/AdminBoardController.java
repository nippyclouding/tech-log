package com.nippyclouding.tech_log_back.board.controller;

import com.nippyclouding.tech_log_back.board.service.BoardService;
import com.nippyclouding.tech_log_back.board.dto.PostCreateRequest;
import com.nippyclouding.tech_log_back.board.dto.PostDetailResponse;
import com.nippyclouding.tech_log_back.board.dto.PostUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts")
public class AdminBoardController {

    private final BoardService boardService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostDetailResponse> create(@Valid @RequestBody PostCreateRequest request) {
        PostDetailResponse response = boardService.create(request);
        return ResponseEntity.created(URI.create("/api/posts/" + response.id())).body(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDetailResponse> createWithImages(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "categories", required = false) List<String> categories,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "tags", required = false) String tags,
            @RequestPart(name = "images", required = false) List<MultipartFile> images
    ) {
        List<String> selectedCategories = normalizeCategories(category, categories);
        PostCreateRequest request = new PostCreateRequest(title, null, content, selectedCategories.get(0), null, splitTags(tags), selectedCategories);
        PostDetailResponse response = boardService.create(request, images);
        return ResponseEntity.created(URI.create("/api/posts/" + response.id())).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostDetailResponse> update(@PathVariable Long id, @Valid @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(boardService.update(id, request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDetailResponse> updateWithImages(
            @PathVariable Long id,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "categories", required = false) List<String> categories,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "tags", required = false) String tags,
            @RequestPart(name = "images", required = false) List<MultipartFile> images
    ) {
        List<String> selectedCategories = normalizeCategories(category, categories);
        PostUpdateRequest request = new PostUpdateRequest(title, null, content, selectedCategories.get(0), null, splitTags(tags), selectedCategories);
        return ResponseEntity.ok(boardService.update(id, request, images));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    private List<String> normalizeCategories(String category, List<String> categories) {
        List<String> selected = categories == null ? List.of() : categories.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (!selected.isEmpty()) {
            return selected;
        }
        if (category != null && !category.isBlank()) {
            return List.of(category.trim());
        }
        throw new IllegalArgumentException("At least one category is required.");
    }
}
