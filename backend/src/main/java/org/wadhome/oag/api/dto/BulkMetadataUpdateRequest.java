package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BulkMetadataUpdateRequest(
    @NotEmpty List<ImageMetadataUpdate> updates
) {
    public record ImageMetadataUpdate(
        @NotNull UUID imagePublicId,
        String title,
        String artistName,
        String description,
        String artCreationDate,
        String artistComments,
        String notes,
        String adminNotes,
        Boolean nsfw
    ) {}
}
