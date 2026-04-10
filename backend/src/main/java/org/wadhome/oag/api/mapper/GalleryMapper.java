package org.wadhome.oag.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.wadhome.oag.api.dto.BioLinkDto;
import org.wadhome.oag.api.dto.GalleryResponse;
import org.wadhome.oag.domain.entity.Gallery;
import org.wadhome.oag.domain.entity.GalleryBioLink;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GalleryMapper {

    @Mapping(target = "headlinerImagePublicId", source = "headlinerImage.publicId")
    @Mapping(target = "isDefault", source = "default")
    GalleryResponse toGalleryResponse(Gallery gallery);

    default BioLinkDto toBioLinkDto(GalleryBioLink link) {
        return new BioLinkDto(link.getLabel(), link.getUrl());
    }

    default List<BioLinkDto> toBioLinkDtos(List<GalleryBioLink> links) {
        if (links == null) return List.of();
        return links.stream().map(this::toBioLinkDto).toList();
    }
}
