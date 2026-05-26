package com.nippyclouding.tech_log_back.image.service;

import com.nippyclouding.tech_log_back.global.config.ImageStorageProperties;
import com.nippyclouding.tech_log_back.global.exception.BusinessException;
import com.nippyclouding.tech_log_back.global.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorageService.class);

    private final ImageStorageProperties properties;

    public List<StoredImage> store(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<MultipartFile> images = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        validateTotalSize(images);

        Path uploadDir = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            log.error("Could not create image upload directory: {}", uploadDir, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        List<StoredImage> storedImages = new ArrayList<>();
        try {
            for (int i = 0; i < images.size(); i++) {
                storedImages.add(storeOne(images.get(i), uploadDir, i));
            }
        } catch (RuntimeException e) {
            deleteStoredFiles(storedImages.stream().map(StoredImage::storedName).toList());
            throw e;
        }
        return storedImages;
    }

    public void deleteStoredFiles(Collection<String> storedNames) {
        if (storedNames == null || storedNames.isEmpty()) {
            return;
        }
        Path uploadDir = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
        storedNames.stream()
                .filter(storedName -> storedName != null && !storedName.isBlank())
                .forEach(storedName -> deleteOne(uploadDir, storedName));
    }

    private StoredImage storeOne(MultipartFile file, Path uploadDir, int order) {
        validateImage(file);
        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String storedName = UUID.randomUUID() + extension(originalName);
        Path target = uploadDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            deleteOne(uploadDir, storedName);
            log.error("Could not store uploaded image in directory: {}", uploadDir, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return new StoredImage(
                properties.publicPath() + "/" + storedName,
                originalName,
                storedName,
                file.getContentType(),
                file.getSize(),
                order,
                order == 0
        );
    }

    private void validateTotalSize(List<MultipartFile> files) {
        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalSize > properties.maxTotalSize().toBytes()) {
            throw new BusinessException(ErrorCode.IMAGE_TOTAL_SIZE_EXCEEDED);
        }
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, "Only image files can be uploaded.");
        }
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index).toLowerCase(Locale.ROOT);
    }

    private void deleteOne(Path uploadDir, String storedName) {
        Path target = uploadDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadDir)) {
            log.warn("Skipping image deletion outside upload directory: {}", storedName);
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete stored image file: {}", storedName, e);
        }
    }
}
