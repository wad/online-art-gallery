package org.wadhome.oag.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImageResponse(
    UUID publicId,
    String shortId,
    String url,
    String thumbnailUrl,
    String title,
    String artistName,
    String description,
    String artCreationDate,
    String artistComments,
    String notes,
    String adminNotes,
    boolean nsfw,
    Instant uploadedAt,
    UUID baseImagePublicId,
    List<String> tagNames,
    List<UUID> galleryPublicIds
) {}
