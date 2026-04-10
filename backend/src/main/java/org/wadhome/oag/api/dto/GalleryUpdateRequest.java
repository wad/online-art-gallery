package org.wadhome.oag.api.dto;

import jakarta.validation.constraints.Pattern;
import org.wadhome.oag.domain.entity.BorderStyle;
import org.wadhome.oag.domain.entity.GalleryTheme;

import java.util.List;
import java.util.UUID;

public record GalleryUpdateRequest(
    @Pattern(regexp = "^[A-Z][A-Z0-9]{0,4}$") String code,
    String name,
    String subtitle,
    String description,
    UUID headlinerImagePublicId,
    Boolean isDefault,
    Boolean visible,
    Boolean showAllImagesTour,
    Integer sortOrder,
    String bioPhotoUrl,
    String bioText,
    List<BioLinkDto> bioLinks,
    GalleryTheme theme,
    BorderStyle borderStyle,
    Boolean adsEnabled,
    Boolean adLandingBanner,
    String adLandingBannerSlot,
    Boolean adImageDetailSidebar,
    String adImageDetailSidebarSlot
) {}
