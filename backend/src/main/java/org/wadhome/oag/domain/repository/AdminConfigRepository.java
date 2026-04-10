package org.wadhome.oag.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wadhome.oag.domain.entity.AdminConfig;

import java.util.Optional;

public interface AdminConfigRepository extends JpaRepository<AdminConfig, Long> {

    Optional<AdminConfig> findFirstBy();
}
