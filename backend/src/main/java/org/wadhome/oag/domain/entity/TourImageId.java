package org.wadhome.oag.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TourImageId implements Serializable {

    @Column(name = "tour_id")
    private Long tourId;

    @Column(name = "image_id")
    private Long imageId;

    public TourImageId() {}

    public TourImageId(Long tourId, Long imageId) {
        this.tourId = tourId;
        this.imageId = imageId;
    }

    public Long getTourId() { return tourId; }
    public Long getImageId() { return imageId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TourImageId that)) return false;
        return Objects.equals(tourId, that.tourId) && Objects.equals(imageId, that.imageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tourId, imageId);
    }
}
