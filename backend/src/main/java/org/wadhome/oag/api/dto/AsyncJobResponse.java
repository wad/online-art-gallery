package org.wadhome.oag.api.dto;

import org.wadhome.oag.domain.entity.AsyncJobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AsyncJobResponse(
    UUID jobId,
    String type,
    AsyncJobStatus status,
    Integer totalItems,
    int processedItems,
    int failedItems,
    List<String> errorMessages,
    Instant startedAt,
    Instant completedAt
) {}
