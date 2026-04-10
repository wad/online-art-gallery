package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GalleryImportRequest(
    @NotNull GalleryExportDto export,
    @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9]{0,4}$") String code
) {}
