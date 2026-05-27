package com.nippyclouding.tech_log_back.comment.controller;

import com.nippyclouding.tech_log_back.comment.service.CommentService;
import com.nippyclouding.tech_log_back.auth.security.GithubUser;
import com.nippyclouding.tech_log_back.global.web.ClientIpResolver;
import com.nippyclouding.tech_log_back.comment.dto.CommentCreateRequest;
import com.nippyclouding.tech_log_back.comment.dto.CommentResponse;
import com.nippyclouding.tech_log_back.comment.dto.CommentUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentResponse>> findByPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal OAuth2User user
    ) {
        Long viewerGithubId = user == null ? null : GithubUser.from(user).id();
        return ResponseEntity.ok(commentService.findByBoard(postId, viewerGithubId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal OAuth2User user,
            HttpServletRequest servletRequest
    ) {
        CommentResponse response = commentService.create(postId, request, GithubUser.from(user), ClientIpResolver.resolve(servletRequest));
        return ResponseEntity.created(URI.create("/api/posts/" + postId + "/comments/" + response.id())).body(response);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal OAuth2User user
    ) {
        return ResponseEntity.ok(commentService.update(postId, commentId, request, GithubUser.from(user)));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal OAuth2User user
    ) {
        commentService.deleteOwn(postId, commentId, GithubUser.from(user));
        return ResponseEntity.noContent().build();
    }
}
