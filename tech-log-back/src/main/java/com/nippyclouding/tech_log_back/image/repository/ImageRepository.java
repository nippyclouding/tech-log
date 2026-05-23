package com.nippyclouding.tech_log_back.image.repository;

import com.nippyclouding.tech_log_back.board.entity.Board;
import com.nippyclouding.tech_log_back.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {

    void deleteByBoard(Board board);
}
