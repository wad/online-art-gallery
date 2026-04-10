package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record TourCreateRequest(
    @NotBlank String name,
    String description,
    @NotNull UUID galleryPublicId,
    UUID headlinerImagePublicId,
    List<UUID> imagePublicIds,
    int sortOrder,
    List<UUID> tagPublicIds
) {}
