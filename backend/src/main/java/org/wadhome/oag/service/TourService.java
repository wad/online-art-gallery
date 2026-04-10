package org.wadhome.oag.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wadhome.oag.api.dto.TourCreateRequest;
import org.wadhome.oag.api.dto.TourUpdateRequest;
import org.wadhome.oag.domain.entity.*;
import org.wadhome.oag.domain.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TourService {

    private final TourRepository tourRepository;
    private final GalleryRepository galleryRepository;
    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final TourImageRepository tourImageRepository;

    public TourService(
        TourRepository tourRepository,
        GalleryRepository galleryRepository,
        ImageRepository imageRepository,
        TagRepository tagRepository,
        TourImageRepository tourImageRepository
    ) {
        this.tourRepository = tourRepository;
        this.galleryRepository = galleryRepository;
        this.imageRepository = imageRepository;
        this.tagRepository = tagRepository;
        this.tourImageRepository = tourImageRepository;
    }

    public List<Tour> listByGallery(UUID galleryPublicId) {
        Gallery gallery = galleryRepository.findByPublicId(galleryPublicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Gallery not found"));
        return tourRepository.findByGalleryOrderBySortOrderAscNameAsc(gallery);
    }

    public Tour getByPublicId(UUID publicId) {
        return tourRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Tour not found"));
    }

    @Transactional
    public Tour create(UUID galleryPublicId, TourCreateRequest request) {
        Gallery gallery = galleryRepository.findByPublicId(galleryPublicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Gallery not found"));

        Tour tour = new Tour();
        tour.setName(request.name());
        tour.setDescription(request.description());
        tour.setGallery(gallery);
        tour.setSortOrder(request.sortOrder());

        if (request.headlinerImagePublicId() != null) {
            imageRepository.findByPublicId(request.headlinerImagePublicId())
                .ifPresent(tour::setHeadlinerImage);
        }

        // Resolve tag associations
        if (request.tagPublicIds() != null) {
            for (UUID tagId : request.tagPublicIds()) {
                var foundTag = tagRepository.findByPublicId(tagId);
                foundTag.ifPresent(tag -> tour.getTags().add(tag));
            }
        }

        Tour savedTour = tourRepository.save(tour);

        // Build image list — start with explicit images
        List<UUID> imageIds = new ArrayList<>();
        if (request.imagePublicIds() != null) {
            imageIds.addAll(request.imagePublicIds());
        }

        // Auto-populate from tags: add images matching tour tags, scoped to gallery
        if (!savedTour.getTags().isEmpty()) {
            gallery.getImages().stream()
                .filter(img -> img.getTags().stream().anyMatch(t -> savedTour.getTags().contains(t)))
                .map(Image::getPublicId)
                .filter(pid -> !imageIds.contains(pid))
                .forEach(imageIds::add);
        }

        setTourImages(savedTour, imageIds);
        return tourRepository.save(savedTour);
    }

    @Transactional
    public Tour update(UUID publicId, TourUpdateRequest request) {
        Tour tour = getByPublicId(publicId);

        if (request.name() != null) tour.setName(request.name());
        if (request.description() != null) tour.setDescription(request.description());
        if (request.sortOrder() != null) tour.setSortOrder(request.sortOrder());
        if (request.headlinerImagePublicId() != null) {
            imageRepository.findByPublicId(request.headlinerImagePublicId())
                .ifPresent(tour::setHeadlinerImage);
        }
        if (request.tagPublicIds() != null) {
            tour.getTags().clear();
            for (UUID tagId : request.tagPublicIds()) {
                tagRepository.findByPublicId(tagId).ifPresent(tag -> tour.getTags().add(tag));
            }
        }
        if (request.imagePublicIds() != null) {
            setTourImages(tour, request.imagePublicIds());
        }

        return tourRepository.save(tour);
    }

    @Transactional
    public void delete(UUID publicId) {
        Tour tour = getByPublicId(publicId);
        tourRepository.delete(tour);
    }

    @Transactional
    public void reorder(UUID galleryPublicId, List<Map<String, Object>> reorderItems) {
        for (Map<String, Object> item : reorderItems) {
            UUID tourPublicId = UUID.fromString((String) item.get("publicId"));
            int sortOrder = (Integer) item.get("sortOrder");
            tourRepository.findByPublicId(tourPublicId).ifPresent(t -> {
                t.setSortOrder(sortOrder);
                tourRepository.save(t);
            });
        }
    }

    private void setTourImages(Tour tour, List<UUID> imagePublicIds) {
        // Clear existing
        tourImageRepository.deleteAll(tour.getTourImages());
        tour.getTourImages().clear();

        // Add in order
        for (int i = 0; i < imagePublicIds.size(); i++) {
            UUID imagePublicId = imagePublicIds.get(i);
            imageRepository.findByPublicId(imagePublicId).ifPresent(img -> {
                TourImage ti = new TourImage();
                ti.setTour(tour);
                ti.setImage(img);
                ti.setSortOrder(imagePublicIds.indexOf(imagePublicId));
                tourImageRepository.save(ti);
                tour.getTourImages().add(ti);
            });
        }
    }
}
