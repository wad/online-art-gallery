package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "thumbnail")
public class Thumbnail {

    @Id
    @Column(name = "image_id")
    private Long imageId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "image_id")
    private Image image;

    @Lob
    @Column(name = "data", nullable = false)
    private byte[] data;

    public Long getImageId() { return imageId; }

    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
