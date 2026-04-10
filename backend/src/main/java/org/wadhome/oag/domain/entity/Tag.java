package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

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
}
