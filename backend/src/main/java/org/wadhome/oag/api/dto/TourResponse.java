package org.wadhome.oag.api.dto;

import java.util.List;
import java.util.UUID;

public record TourResponse(
    UUID publicId,
    String name,
    String description,
    UUID galleryPublicId,
    UUID headlinerImagePublicId,
    int sortOrder,
    List<UUID> tagPublicIds,
    List<UUID> imagePublicIds
) {}
