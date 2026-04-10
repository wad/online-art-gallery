package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tour")
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gallery_id", nullable = false)
    private Gallery gallery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "headliner_image_id")
    private Image headlinerImage;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tour_tag",
        joinColumns = @JoinColumn(name = "tour_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TourImage> tourImages = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public Long getId() { return id; }

    public UUID getPublicId() { return publicId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Gallery getGallery() { return gallery; }
    public void setGallery(Gallery gallery) { this.gallery = gallery; }

    public Image getHeadlinerImage() { return headlinerImage; }
    public void setHeadlinerImage(Image headlinerImage) { this.headlinerImage = headlinerImage; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }

    public List<TourImage> getTourImages() { return tourImages; }
    public void setTourImages(List<TourImage> tourImages) { this.tourImages = tourImages; }
}
