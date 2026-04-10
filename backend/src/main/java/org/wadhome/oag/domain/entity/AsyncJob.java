package org.wadhome.oag.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "async_job")
public class AsyncJob {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AsyncJobStatus status;

    @Column(name = "total_items")
    private Integer totalItems;

    @Column(name = "processed_items")
    private int processedItems = 0;

    @Column(name = "failed_items")
    private int failedItems = 0;

    @Column(name = "error_messages", columnDefinition = "text")
    private String errorMessages;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public AsyncJobStatus getStatus() { return status; }
    public void setStatus(AsyncJobStatus status) { this.status = status; }

    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }

    public int getProcessedItems() { return processedItems; }
    public void setProcessedItems(int processedItems) { this.processedItems = processedItems; }

    public int getFailedItems() { return failedItems; }
    public void setFailedItems(int failedItems) { this.failedItems = failedItems; }

    public String getErrorMessages() { return errorMessages; }
    public void setErrorMessages(String errorMessages) { this.errorMessages = errorMessages; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
