package com.nippyclouding.tech_log_back.category.repository;

import com.nippyclouding.tech_log_back.category.entity.Category;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    List<Category> findByNameIn(Collection<String> names);
}
