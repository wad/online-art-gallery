package org.wadhome.oag.api.mapper;

import org.mapstruct.Mapper;
import org.wadhome.oag.api.dto.TagResponse;
import org.wadhome.oag.domain.entity.Tag;

@Mapper(componentModel = "spring")
public interface TagMapper {

    default TagResponse toTagResponse(Tag tag, long imageCount) {
        return new TagResponse(tag.getPublicId(), tag.getName(), imageCount);
    }
}
