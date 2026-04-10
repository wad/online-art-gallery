package org.wadhome.oag.api.dto;

import java.util.UUID;

public record TagResponse(
    UUID publicId,
    String name,
    long imageCount
) {}
