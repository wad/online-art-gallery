package org.wadhome.oag.api.dto;

import org.wadhome.oag.domain.entity.BorderStyle;
import org.wadhome.oag.domain.entity.GalleryTheme;

import java.util.List;

public record GalleryExportDto(
    int formatVersion,
    GalleryData gallery,
    List<TourData> tours
) {
    public record GalleryData(
        String name,
        String subtitle,
        String description,
        GalleryTheme theme,
        BorderStyle borderStyle,
        boolean visible,
        boolean showAllImagesTour,
        boolean adsEnabled,
        boolean adLandingBanner,
        String adLandingBannerSlot,
        boolean adImageDetailSidebar,
        String adImageDetailSidebarSlot,
        String bioPhotoUrl,
        String bioText,
        List<BioLinkDto> bioLinks,
        String headlinerImageShortId,
        List<String> imageShortIds
    ) {}

    public record TourData(
        String name,
        String description,
        int sortOrder,
        String headlinerImageShortId,
        List<String> imageShortIds,
        List<String> tagNames
    ) {}
}
