package com.nippyclouding.tech_log_back.comment.entity;

import com.nippyclouding.tech_log_back.board.entity.Board;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString(exclude = "board")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "COMMENTS")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "github_id", nullable = false)
    private Long githubId;

    @Column(name = "github_name", nullable = false)
    private String githubName;

    @Column(name = "github_avatar_url", columnDefinition = "TEXT")
    private String githubAvatarUrl;

    @Column(name = "access_ip", nullable = false)
    private String accessIp;

    @Builder
    public Comment(Board board, String content, Long githubId, String githubName, String githubAvatarUrl, String accessIp) {
        if (content == null || content.isBlank() || content.length() > 500) {
            throw new IllegalArgumentException("comment content must be 1 to 500 characters");
        }
        this.board = board;
        this.content = content.trim();
        this.githubId = githubId;
        this.githubName = githubName;
        this.githubAvatarUrl = githubAvatarUrl;
        this.accessIp = accessIp;
    }

    public void delete() {
        deleted = true;
        updatedAt = LocalDateTime.now();
    }

}
