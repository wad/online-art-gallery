package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "page_view_daily")
public class PageViewDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private ViewEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "context", nullable = false, length = 20)
    private ViewContext context;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    public Long getId() { return id; }

    public LocalDate getViewDate() { return viewDate; }
    public void setViewDate(LocalDate viewDate) { this.viewDate = viewDate; }

    public ViewEntityType getEntityType() { return entityType; }
    public void setEntityType(ViewEntityType entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public ViewContext getContext() { return context; }
    public void setContext(ViewContext context) { this.context = context; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
}
