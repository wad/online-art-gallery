package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_config")
public class AdminConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    public Long getId() { return id; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
