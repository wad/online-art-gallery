package org.wadhome.oag.api.dto;

import java.util.List;
import java.util.UUID;

public record PublicImageResponse(
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
    boolean nsfw,
    UUID baseImagePublicId,
    List<String> tagNames
) {}
