package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "gallery")
public class Gallery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "code", nullable = false, unique = true, length = 5)
    private String code;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "subtitle", columnDefinition = "text")
    private String subtitle;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "headliner_image_id")
    private Image headlinerImage;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @Column(name = "visible", nullable = false)
    private boolean visible = true;

    @Column(name = "show_all_images_tour", nullable = false)
    private boolean showAllImagesTour = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "bio_photo_url", columnDefinition = "text")
    private String bioPhotoUrl;

    @Column(name = "bio_text", columnDefinition = "text")
    private String bioText;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 50)
    private GalleryTheme theme = GalleryTheme.LIGHT;

    @Enumerated(EnumType.STRING)
    @Column(name = "border_style", length = 50)
    private BorderStyle borderStyle;

    // Ad settings
    @Column(name = "ads_enabled", nullable = false)
    private boolean adsEnabled = false;

    @Column(name = "ad_landing_banner", nullable = false)
    private boolean adLandingBanner = false;

    @Column(name = "ad_landing_banner_slot", columnDefinition = "text")
    private String adLandingBannerSlot;

    @Column(name = "ad_image_detail_sidebar", nullable = false)
    private boolean adImageDetailSidebar = false;

    @Column(name = "ad_image_detail_sidebar_slot", columnDefinition = "text")
    private String adImageDetailSidebarSlot;

    @OneToMany(mappedBy = "gallery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<GalleryBioLink> bioLinks = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "gallery_image",
        joinColumns = @JoinColumn(name = "gallery_id"),
        inverseJoinColumns = @JoinColumn(name = "image_id")
    )
    private Set<Image> images = new HashSet<>();

    @PrePersist
    private void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public Long getId() { return id; }

    public UUID getPublicId() { return publicId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Image getHeadlinerImage() { return headlinerImage; }
    public void setHeadlinerImage(Image headlinerImage) { this.headlinerImage = headlinerImage; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isShowAllImagesTour() { return showAllImagesTour; }
    public void setShowAllImagesTour(boolean showAllImagesTour) { this.showAllImagesTour = showAllImagesTour; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getBioPhotoUrl() { return bioPhotoUrl; }
    public void setBioPhotoUrl(String bioPhotoUrl) { this.bioPhotoUrl = bioPhotoUrl; }

    public String getBioText() { return bioText; }
    public void setBioText(String bioText) { this.bioText = bioText; }

    public GalleryTheme getTheme() { return theme; }
    public void setTheme(GalleryTheme theme) { this.theme = theme; }

    public BorderStyle getBorderStyle() { return borderStyle; }
    public void setBorderStyle(BorderStyle borderStyle) { this.borderStyle = borderStyle; }

    public boolean isAdsEnabled() { return adsEnabled; }
    public void setAdsEnabled(boolean adsEnabled) { this.adsEnabled = adsEnabled; }

    public boolean isAdLandingBanner() { return adLandingBanner; }
    public void setAdLandingBanner(boolean adLandingBanner) { this.adLandingBanner = adLandingBanner; }

    public String getAdLandingBannerSlot() { return adLandingBannerSlot; }
    public void setAdLandingBannerSlot(String adLandingBannerSlot) { this.adLandingBannerSlot = adLandingBannerSlot; }

    public boolean isAdImageDetailSidebar() { return adImageDetailSidebar; }
    public void setAdImageDetailSidebar(boolean adImageDetailSidebar) { this.adImageDetailSidebar = adImageDetailSidebar; }

    public String getAdImageDetailSidebarSlot() { return adImageDetailSidebarSlot; }
    public void setAdImageDetailSidebarSlot(String adImageDetailSidebarSlot) { this.adImageDetailSidebarSlot = adImageDetailSidebarSlot; }

    public List<GalleryBioLink> getBioLinks() { return bioLinks; }
    public void setBioLinks(List<GalleryBioLink> bioLinks) { this.bioLinks = bioLinks; }

    public Set<Image> getImages() { return images; }
    public void setImages(Set<Image> images) { this.images = images; }
}
