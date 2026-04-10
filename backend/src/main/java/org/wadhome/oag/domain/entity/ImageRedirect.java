package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "image_redirect")
public class ImageRedirect {

    @Id
    @Column(name = "old_short_id", length = 8)
    private String oldShortId;

    @Column(name = "new_short_id", nullable = false, length = 8)
    private String newShortId;

    public String getOldShortId() { return oldShortId; }
    public void setOldShortId(String oldShortId) { this.oldShortId = oldShortId; }

    public String getNewShortId() { return newShortId; }
    public void setNewShortId(String newShortId) { this.newShortId = newShortId; }
}
