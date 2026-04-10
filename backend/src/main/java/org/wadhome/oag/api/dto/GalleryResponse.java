package org.wadhome.oag.api.dto;

import org.wadhome.oag.domain.entity.BorderStyle;
import org.wadhome.oag.domain.entity.GalleryTheme;

import java.util.List;
import java.util.UUID;

public record GalleryResponse(
    UUID publicId,
    String code,
    String name,
    String subtitle,
    String description,
    UUID headlinerImagePublicId,
    boolean isDefault,
    boolean visible,
    boolean showAllImagesTour,
    int sortOrder,
    String bioPhotoUrl,
    String bioText,
    List<BioLinkDto> bioLinks,
    GalleryTheme theme,
    BorderStyle borderStyle,
    boolean adsEnabled,
    boolean adLandingBanner,
    String adLandingBannerSlot,
    boolean adImageDetailSidebar,
    String adImageDetailSidebarSlot
) {}
