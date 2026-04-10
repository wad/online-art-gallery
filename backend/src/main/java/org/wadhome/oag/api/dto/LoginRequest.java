package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String password
) {}
