package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "image")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "short_id", nullable = false, unique = true, updatable = false, length = 8)
    private String shortId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "title")
    private String title;

    @Column(name = "artist_name")
    private String artistName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "art_creation_date")
    private String artCreationDate;

    @Column(name = "artist_comments", columnDefinition = "text")
    private String artistComments;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "admin_notes", columnDefinition = "text")
    private String adminNotes;

    @Column(name = "nsfw", nullable = false)
    private boolean nsfw = false;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_image_id")
    private Image baseImage;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "image_tag",
        joinColumns = @JoinColumn(name = "image_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    private void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public Long getId() { return id; }

    public UUID getPublicId() { return publicId; }

    public String getShortId() { return shortId; }
    public void setShortId(String shortId) { this.shortId = shortId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getArtCreationDate() { return artCreationDate; }
    public void setArtCreationDate(String artCreationDate) { this.artCreationDate = artCreationDate; }

    public String getArtistComments() { return artistComments; }
    public void setArtistComments(String artistComments) { this.artistComments = artistComments; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public boolean isNsfw() { return nsfw; }
    public void setNsfw(boolean nsfw) { this.nsfw = nsfw; }

    public Instant getUploadedAt() { return uploadedAt; }

    public Image getBaseImage() { return baseImage; }
    public void setBaseImage(Image baseImage) { this.baseImage = baseImage; }

    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }
}
