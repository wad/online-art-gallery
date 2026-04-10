package org.wadhome.oag.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wadhome.oag.domain.entity.Gallery;
import org.wadhome.oag.domain.entity.Tour;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourRepository extends JpaRepository<Tour, Long> {

    Optional<Tour> findByPublicId(UUID publicId);

    List<Tour> findByGalleryOrderBySortOrderAscNameAsc(Gallery gallery);

    List<Tour> findByGalleryIdOrderBySortOrderAscNameAsc(Long galleryId);
}
