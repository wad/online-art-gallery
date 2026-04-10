package org.wadhome.oag.api.dto;

import org.wadhome.oag.domain.entity.GalleryTheme;

import java.util.Arrays;
import java.util.List;

public record ThemeResponse(
    String value,
    String displayName
) {
    public static List<ThemeResponse> all() {
        return Arrays.stream(GalleryTheme.values())
            .map(t -> new ThemeResponse(t.name(), toDisplayName(t)))
            .toList();
    }

    private static String toDisplayName(GalleryTheme theme) {
        return switch (theme) {
            case LIGHT -> "Light";
            case DARK -> "Dark";
            case PASTEL -> "Pastel";
            case SPRING -> "Spring";
            case WINTER -> "Winter";
            case CYBERPUNK -> "Cyberpunk";
            case SUNSET -> "Sunset";
            case OCEAN -> "Ocean";
            case MONOCHROME -> "Monochrome";
        };
    }
}
