package org.wadhome.oag.api.dto;

import java.util.List;
import java.util.UUID;

public record ImageBulkLoadResponse(
    List<ImageBulkLoadResult> results,
    boolean aborted,
    String abortReason
) {
    public record ImageBulkLoadResult(
        String url,
        boolean success,
        UUID imagePublicId,
        String error
    ) {}
}
