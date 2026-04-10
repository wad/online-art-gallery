package org.wadhome.oag.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.wadhome.oag.api.dto.TourResponse;
import org.wadhome.oag.domain.entity.Image;
import org.wadhome.oag.domain.entity.Tag;
import org.wadhome.oag.domain.entity.Tour;
import org.wadhome.oag.domain.entity.TourImage;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TourMapper {

    @Mapping(target = "galleryPublicId", source = "gallery.publicId")
    @Mapping(target = "headlinerImagePublicId", source = "headlinerImage.publicId")
    @Mapping(target = "tagPublicIds", expression = "java(toTagPublicIds(tour))")
    @Mapping(target = "imagePublicIds", expression = "java(toImagePublicIds(tour))")
    TourResponse toTourResponse(Tour tour);

    default List<UUID> toTagPublicIds(Tour tour) {
        if (tour.getTags() == null) return List.of();
        return tour.getTags().stream().map(Tag::getPublicId).toList();
    }

    default List<UUID> toImagePublicIds(Tour tour) {
        if (tour.getTourImages() == null) return List.of();
        return tour.getTourImages().stream()
            .map(TourImage::getImage)
            .map(Image::getPublicId)
            .toList();
    }
}
