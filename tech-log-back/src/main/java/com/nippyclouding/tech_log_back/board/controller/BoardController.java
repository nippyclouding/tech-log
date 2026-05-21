package com.nippyclouding.tech_log_back.board.controller;

import com.nippyclouding.tech_log_back.board.service.BoardService;
import com.nippyclouding.tech_log_back.global.dto.PageResponse;
import com.nippyclouding.tech_log_back.board.dto.PostDetailResponse;
import com.nippyclouding.tech_log_back.board.dto.PostSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<PageResponse<PostSummaryResponse>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(boardService.search(category, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.get(id));
    }
}
