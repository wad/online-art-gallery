package org.wadhome.oag.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.wadhome.oag.domain.entity.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByPublicId(UUID publicId);

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT t FROM Tag t ORDER BY t.name ASC")
    List<Tag> findAllOrderByName();
}
