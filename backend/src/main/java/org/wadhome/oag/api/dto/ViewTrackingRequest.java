package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.wadhome.oag.domain.entity.ViewContext;
import org.wadhome.oag.domain.entity.ViewEntityType;

import java.util.UUID;

public record ViewTrackingRequest(
    @NotNull ViewEntityType type,
    @NotBlank String code,
    UUID tourPublicId,
    String imageShortId,
    ViewContext context
) {}
