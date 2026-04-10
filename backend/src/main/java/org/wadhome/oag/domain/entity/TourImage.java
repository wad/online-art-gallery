package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tour_image")
public class TourImage {

    @EmbeddedId
    private TourImageId id = new TourImageId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tourId")
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("imageId")
    @JoinColumn(name = "image_id")
    private Image image;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public TourImageId getId() { return id; }

    public Tour getTour() { return tour; }
    public void setTour(Tour tour) { this.tour = tour; }

    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
