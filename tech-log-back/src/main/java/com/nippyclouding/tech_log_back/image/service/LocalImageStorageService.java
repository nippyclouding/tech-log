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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalImageStorageService {

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
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, "Could not create image upload directory.");
        }

        List<StoredImage> storedImages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            storedImages.add(storeOne(images.get(i), uploadDir, i));
        }
        return storedImages;
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
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, "Could not save image file.");
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
}
