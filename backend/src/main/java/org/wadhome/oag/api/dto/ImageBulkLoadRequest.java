package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ImageBulkLoadRequest(
    @NotEmpty List<String> urls,
    @NotNull UUID galleryPublicId
) {}
