package org.wadhome.oag.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.wadhome.oag.api.dto.AsyncJobResponse;
import org.wadhome.oag.domain.entity.AsyncJob;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AsyncJobMapper {

    @Mapping(target = "jobId", source = "id")
    @Mapping(target = "errorMessages", expression = "java(parseErrorMessages(job.getErrorMessages()))")
    AsyncJobResponse toAsyncJobResponse(AsyncJob job);

    default List<String> parseErrorMessages(String json) {
        if (json == null || json.isBlank()) return List.of();
        // Simple JSON array parse; service layer stores as ["msg1","msg2",...]
        try {
            String trimmed = json.strip();
            if (trimmed.equals("[]")) return List.of();
            trimmed = trimmed.substring(1, trimmed.length() - 1); // remove [ ]
            return List.of(trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .stream()
                .map(s -> s.strip().replaceAll("^\"|\"$", ""))
                .toList();
        } catch (Exception e) {
            return List.of(json);
        }
    }
}
