package com.nippyclouding.tech_log_back.image.entity;

import com.nippyclouding.tech_log_back.board.entity.Board;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "IMAGES")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false)
    private StorageType storageType;

    @Column(name = "file_key", nullable = false, columnDefinition = "TEXT")
    private String fileKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "stored_name", nullable = false)
    private String storedName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "image_order", nullable = false)
    private int imageOrder;

    @Column(name = "is_thumbnail", nullable = false)
    private boolean thumbnail;

    @Builder
    public Image(
            Board board,
            StorageType storageType,
            String fileKey,
            String originalName,
            String storedName,
            String contentType,
            long fileSize,
            int imageOrder,
            boolean thumbnail
    ) {
        this.board = board;
        this.storageType = storageType;
        this.fileKey = fileKey;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.imageOrder = imageOrder;
        this.thumbnail = thumbnail;
    }
}
