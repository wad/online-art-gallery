package org.wadhome.oag.api.dto;

import java.util.List;
import java.util.UUID;

public record ImageUsageResponse(
    List<GalleryUsage> galleries
) {
    public record GalleryUsage(
        UUID galleryPublicId,
        String galleryName,
        String galleryCode,
        List<TourUsage> tours
    ) {}

    public record TourUsage(
        UUID tourPublicId,
        String tourName
    ) {}
}
