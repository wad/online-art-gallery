package org.wadhome.oag.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wadhome.oag.domain.entity.TourImage;
import org.wadhome.oag.domain.entity.TourImageId;

import java.util.List;

public interface TourImageRepository extends JpaRepository<TourImage, TourImageId> {

    List<TourImage> findByIdTourIdOrderBySortOrderAsc(Long tourId);

    void deleteByIdTourIdAndIdImageId(Long tourId, Long imageId);
}
