package org.wadhome.oag.service;

import org.springframework.stereotype.Service;
import org.wadhome.oag.api.dto.ViewStatsResponse;
import org.wadhome.oag.domain.entity.PageViewDaily;
import org.wadhome.oag.domain.entity.ViewContext;
import org.wadhome.oag.domain.entity.ViewEntityType;
import org.wadhome.oag.domain.repository.GalleryRepository;
import org.wadhome.oag.domain.repository.ImageRepository;
import org.wadhome.oag.domain.repository.PageViewDailyRepository;
import org.wadhome.oag.domain.repository.TourRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ViewStatsService {

    private final PageViewDailyRepository pageViewDailyRepository;
    private final GalleryRepository galleryRepository;
    private final TourRepository tourRepository;
    private final ImageRepository imageRepository;

    public ViewStatsService(
        PageViewDailyRepository pageViewDailyRepository,
        GalleryRepository galleryRepository,
        TourRepository tourRepository,
        ImageRepository imageRepository
    ) {
        this.pageViewDailyRepository = pageViewDailyRepository;
        this.galleryRepository = galleryRepository;
        this.tourRepository = tourRepository;
        this.imageRepository = imageRepository;
    }

    public void recordView(ViewEntityType entityType, Long entityId, ViewContext context) {
        pageViewDailyRepository.upsertView(
            LocalDate.now(), entityType.name(), entityId, context.name()
        );
    }

    public List<ViewStatsResponse> getGalleryStats(LocalDate from, LocalDate to) {
        var rows = pageViewDailyRepository.findByEntityTypeAndDateRange(ViewEntityType.GALLERY, from, to);
        return buildStats(rows, ViewEntityType.GALLERY);
    }

    public List<ViewStatsResponse> getTourStats(LocalDate from, LocalDate to, UUID galleryPublicId) {
        var rows = pageViewDailyRepository.findByEntityTypeAndDateRange(ViewEntityType.TOUR, from, to);
        if (galleryPublicId != null) {
            // Filter to tours belonging to the gallery
            var gallery = galleryRepository.findByPublicId(galleryPublicId).orElse(null);
            if (gallery != null) {
                var tourIds = tourRepository.findByGalleryOrderBySortOrderAscNameAsc(gallery)
                    .stream().map(t -> t.getId()).collect(Collectors.toSet());
                rows = rows.stream().filter(r -> tourIds.contains(r.getEntityId())).toList();
            }
        }
        return buildStats(rows, ViewEntityType.TOUR);
    }

    public List<ViewStatsResponse> getImageStats(LocalDate from, LocalDate to) {
        var rows = pageViewDailyRepository.findByEntityTypeAndDateRange(ViewEntityType.IMAGE, from, to);
        return buildStats(rows, ViewEntityType.IMAGE);
    }

    private List<ViewStatsResponse> buildStats(List<PageViewDaily> rows, ViewEntityType entityType) {
        // Group by entityId
        Map<Long, List<PageViewDaily>> byEntity = rows.stream()
            .collect(Collectors.groupingBy(PageViewDaily::getEntityId));

        return byEntity.entrySet().stream().map(entry -> {
            Long entityId = entry.getKey();
            List<PageViewDaily> entityRows = entry.getValue();

            long direct = entityRows.stream()
                .filter(r -> r.getContext() == ViewContext.DIRECT)
                .mapToLong(PageViewDaily::getViewCount).sum();
            long tour = entityRows.stream()
                .filter(r -> r.getContext() == ViewContext.TOUR)
                .mapToLong(PageViewDaily::getViewCount).sum();

            List<ViewStatsResponse.DailyViewCount> daily = entityRows.stream()
                .collect(Collectors.groupingBy(PageViewDaily::getViewDate,
                    Collectors.summingInt(PageViewDaily::getViewCount)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ViewStatsResponse.DailyViewCount(e.getKey(), e.getValue()))
                .toList();

            String name = resolveEntityName(entityType, entityId);
            String code = resolveEntityCode(entityType, entityId);
            String shortId = resolveEntityShortId(entityType, entityId);

            return new ViewStatsResponse(name, code, shortId, direct + tour, direct, tour, daily);
        })
        .sorted(Comparator.comparingLong(ViewStatsResponse::totalViews).reversed())
        .toList();
    }

    private String resolveEntityName(ViewEntityType type, Long entityId) {
        return switch (type) {
            case GALLERY -> galleryRepository.findById(entityId).map(g -> g.getName()).orElse("[deleted]");
            case TOUR -> tourRepository.findById(entityId).map(t -> t.getName()).orElse("[deleted]");
            case IMAGE -> imageRepository.findById(entityId).map(i -> i.getTitle()).orElse("[deleted]");
        };
    }

    private String resolveEntityCode(ViewEntityType type, Long entityId) {
        if (type == ViewEntityType.GALLERY) {
            return galleryRepository.findById(entityId).map(g -> g.getCode()).orElse(null);
        }
        return null;
    }

    private String resolveEntityShortId(ViewEntityType type, Long entityId) {
        if (type == ViewEntityType.IMAGE) {
            return imageRepository.findById(entityId).map(i -> i.getShortId()).orElse(null);
        }
        return null;
    }
}
