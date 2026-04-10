package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ImageCreateRequest(
    @NotBlank String url,
    String title,
    String artistName,
    String description,
    String artCreationDate,
    String artistComments,
    String notes,
    String adminNotes,
    boolean nsfw,
    UUID baseImagePublicId,
    UUID galleryPublicId
) {}
