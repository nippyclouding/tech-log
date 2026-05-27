package com.nippyclouding.tech_log_back.admin.repository;

import com.nippyclouding.tech_log_back.admin.entity.Admin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsername(String username);
}
