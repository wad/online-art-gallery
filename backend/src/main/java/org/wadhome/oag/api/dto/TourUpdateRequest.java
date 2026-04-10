package org.wadhome.oag.api.dto;

import java.util.List;
import java.util.UUID;

public record TourUpdateRequest(
    String name,
    String description,
    UUID headlinerImagePublicId,
    List<UUID> imagePublicIds,
    Integer sortOrder,
    List<UUID> tagPublicIds
) {}
