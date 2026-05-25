package com.nippyclouding.tech_log_back.image.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nippyclouding.tech_log_back.global.config.ImageStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class LocalImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deleteStoredFiles_deletesOnlyFilesInsideUploadDirectory() throws Exception {
        Path uploadedFile = Files.writeString(tempDir.resolve("stored.png"), "image");
        Path outsideFile = Files.writeString(tempDir.resolveSibling(tempDir.getFileName() + "-outside.png"), "outside");
        LocalImageStorageService service = new LocalImageStorageService(
                new ImageStorageProperties(tempDir.toString(), "/image", DataSize.ofMegabytes(20))
        );

        service.deleteStoredFiles(List.of("stored.png", "../outside.png"));

        assertThat(uploadedFile).doesNotExist();
        assertThat(outsideFile).exists();
        Files.deleteIfExists(outsideFile);
    }

    @Test
    void store_removesPreviouslyStoredFilesWhenLaterImageIsInvalid() {
        LocalImageStorageService service = new LocalImageStorageService(
                new ImageStorageProperties(tempDir.toString(), "/image", DataSize.ofMegabytes(20))
        );
        MockMultipartFile valid = new MockMultipartFile("images", "valid.png", "image/png", "valid".getBytes());
        MockMultipartFile invalid = new MockMultipartFile("images", "invalid.txt", "text/plain", "invalid".getBytes());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.store(List.of(valid, invalid)));

        assertThat(tempDir).isEmptyDirectory();
    }
}
