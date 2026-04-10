package org.wadhome.oag.api.dto;

import java.time.LocalDate;
import java.util.List;

public record ViewStatsResponse(
    String entityName,
    String entityCode,
    String entityShortId,
    long totalViews,
    long directViews,
    long tourViews,
    List<DailyViewCount> dailyCounts
) {
    public record DailyViewCount(
        LocalDate date,
        int count
    ) {}
}
