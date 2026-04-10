package org.wadhome.oag.api.dto;

import java.util.List;
import java.util.UUID;

public record BulkMetadataUpdateResponse(
    List<ImageUpdateResult> results
) {
    public record ImageUpdateResult(
        UUID imagePublicId,
        boolean success,
        String error
    ) {}
}
