package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BioLinkDto(
    @NotBlank String label,
    @NotBlank String url
) {}
