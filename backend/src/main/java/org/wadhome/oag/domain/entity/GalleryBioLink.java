package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "gallery_bio_link")
public class GalleryBioLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gallery_id", nullable = false)
    private Gallery gallery;

    @Column(name = "label", nullable = false, columnDefinition = "text")
    private String label;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Long getId() { return id; }

    public Gallery getGallery() { return gallery; }
    public void setGallery(Gallery gallery) { this.gallery = gallery; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
