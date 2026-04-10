package org.wadhome.oag.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wadhome.oag.domain.entity.Gallery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {

    Optional<Gallery> findByPublicId(UUID publicId);

    Optional<Gallery> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Gallery> findByIsDefaultTrue();

    List<Gallery> findAllByOrderBySortOrderAscNameAsc();

    List<Gallery> findByVisibleTrueOrderBySortOrderAscNameAsc();
}
