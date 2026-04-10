package org.wadhome.oag.api.mapper;

import org.mapstruct.*;
import org.wadhome.oag.api.dto.*;
import org.wadhome.oag.domain.entity.Gallery;
import org.wadhome.oag.domain.entity.Image;
import org.wadhome.oag.domain.entity.Tag;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    @Mapping(target = "thumbnailUrl", expression = "java(\"/api/v1/public/thumbnails/\" + image.getShortId())")
    @Mapping(target = "baseImagePublicId", source = "baseImage.publicId")
    @Mapping(target = "tagNames", expression = "java(toTagNames(image.getTags()))")
    @Mapping(target = "galleryPublicIds", expression = "java(toGalleryPublicIds(galleries))")
    ImageResponse toImageResponse(Image image, @Context List<Gallery> galleries);

    @Mapping(target = "thumbnailUrl", expression = "java(\"/api/v1/public/thumbnails/\" + image.getShortId())")
    @Mapping(target = "baseImagePublicId", source = "baseImage.publicId")
    @Mapping(target = "tagNames", expression = "java(toTagNames(image.getTags()))")
    PublicImageResponse toPublicImageResponse(Image image);

    default List<String> toTagNames(Set<Tag> tags) {
        if (tags == null) return List.of();
        return tags.stream().map(Tag::getName).sorted().toList();
    }

    default List<UUID> toGalleryPublicIds(List<Gallery> galleries) {
        if (galleries == null) return List.of();
        return galleries.stream().map(Gallery::getPublicId).toList();
    }
}
