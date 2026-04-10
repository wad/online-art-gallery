package org.wadhome.oag.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wadhome.oag.domain.entity.AsyncJob;

import java.util.UUID;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {
}
