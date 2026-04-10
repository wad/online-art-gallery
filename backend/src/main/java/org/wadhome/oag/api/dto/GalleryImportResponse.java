package org.wadhome.oag.api.dto;

import java.util.List;

public record GalleryImportResponse(
    GalleryResponse gallery,
    List<String> warnings
) {}
