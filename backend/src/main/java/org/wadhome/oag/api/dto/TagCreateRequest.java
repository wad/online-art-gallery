package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TagCreateRequest(
    @NotBlank String name
) {}
