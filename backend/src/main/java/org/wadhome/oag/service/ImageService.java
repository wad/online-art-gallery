package org.wadhome.oag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wadhome.oag.api.dto.*;
import org.wadhome.oag.domain.entity.*;
import org.wadhome.oag.domain.repository.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final GalleryRepository galleryRepository;
    private final ThumbnailService thumbnailService;
    private final TourRepository tourRepository;
    private final ImageRedirectRepository imageRedirectRepository;
    private final AsyncJobRepository asyncJobRepository;
    private final ObjectMapper objectMapper;

    public ImageService(
        ImageRepository imageRepository,
        GalleryRepository galleryRepository,
        ThumbnailService thumbnailService,
        TourRepository tourRepository,
        ImageRedirectRepository imageRedirectRepository,
        AsyncJobRepository asyncJobRepository,
        ObjectMapper objectMapper
    ) {
        this.imageRepository = imageRepository;
        this.galleryRepository = galleryRepository;
        this.thumbnailService = thumbnailService;
        this.tourRepository = tourRepository;
        this.imageRedirectRepository = imageRedirectRepository;
        this.asyncJobRepository = asyncJobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Image create(ImageCreateRequest request) {
        Image image = new Image();
        image.setShortId(generateUniqueShortId());
        image.setUrl(request.url());
        image.setTitle(request.title());
        image.setArtistName(request.artistName());
        image.setDescription(request.description());
        image.setArtCreationDate(request.artCreationDate());
        image.setArtistComments(request.artistComments());
        image.setNotes(request.notes());
        image.setAdminNotes(request.adminNotes());
        image.setNsfw(request.nsfw());

        if (request.baseImagePublicId() != null) {
            Image base = imageRepository.findByPublicId(request.baseImagePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Base image not found"));
            image.setBaseImage(base);
        }

        image = imageRepository.save(image);

        if (request.galleryPublicId() != null) {
            Gallery gallery = galleryRepository.findByPublicId(request.galleryPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));
            gallery.getImages().add(image);
            galleryRepository.save(gallery);
        }

        return image;
    }

    public String generateUniqueShortId() {
        String shortId;
        int attempts = 0;
        do {
            int n = new Random().nextInt(100_000_000);
            shortId = String.format("%08d", n);
            attempts++;
            if (attempts > 100) throw new IllegalStateException("Failed to generate unique shortId");
        } while (imageRepository.existsByShortId(shortId));
        return shortId;
    }

    @Transactional
    public Image update(UUID publicId, ImageUpdateRequest request) {
        Image image = imageRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        boolean urlChanged = !image.getUrl().equals(request.url());
        image.setUrl(request.url());
        image.setTitle(request.title());
        image.setArtistName(request.artistName());
        image.setDescription(request.description());
        image.setArtCreationDate(request.artCreationDate());
        image.setArtistComments(request.artistComments());
        image.setNotes(request.notes());
        image.setAdminNotes(request.adminNotes());
        image.setNsfw(request.nsfw());

        if (request.baseImagePublicId() != null) {
            Image base = imageRepository.findByPublicId(request.baseImagePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Base image not found"));
            validateNoCircularBaseImage(image, base);
            image.setBaseImage(base);
        } else {
            image.setBaseImage(null);
        }

        image = imageRepository.save(image);

        if (urlChanged) {
            thumbnailService.generateAndStore(image);
        }

        return image;
    }

    @Transactional
    public void delete(UUID publicId, String substituteShortId) {
        Image image = imageRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (!"00000000".equals(substituteShortId)) {
            boolean subExists = imageRepository.existsByShortId(substituteShortId);
            if (!subExists) {
                throw new IllegalArgumentException("Substitute image not found: " + substituteShortId);
            }
        }

        // Validate no loop: substituteShortId must not be the image's own shortId
        if (image.getShortId().equals(substituteShortId)) {
            throw new IllegalArgumentException("Cannot substitute image with itself");
        }

        // Update existing redirects pointing to this image to point to substitute
        imageRedirectRepository.findAll().stream()
            .filter(r -> r.getNewShortId().equals(image.getShortId()))
            .forEach(r -> {
                r.setNewShortId(substituteShortId);
                imageRedirectRepository.save(r);
            });

        // Create redirect from old shortId to substitute
        ImageRedirect redirect = new ImageRedirect();
        redirect.setOldShortId(image.getShortId());
        redirect.setNewShortId(substituteShortId);
        imageRedirectRepository.save(redirect);

        // Evict thumbnail cache
        thumbnailService.evict(image.getShortId());

        imageRepository.delete(image);
    }

    public Image getByPublicId(UUID publicId) {
        return imageRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
    }

    public Page<Image> list(String search, UUID tagId, Boolean nsfw, Boolean orphan,
                            String sort, String dir, int page, int size) {
        if (Boolean.TRUE.equals(orphan)) {
            List<Image> orphans = imageRepository.findOrphans();
            // Wrap as a page
            int start = Math.min(page * size, orphans.size());
            int end = Math.min(start + size, orphans.size());
            List<Image> slice = orphans.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(slice,
                PageRequest.of(page, size), orphans.size());
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = switch (sort != null ? sort : "") {
            case "title" -> "title";
            case "artistName" -> "artistName";
            default -> "uploadedAt";
        };
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortField));

        String normalizedSearch = (search != null && !search.isBlank()) ? search : null;
        return imageRepository.findBySearchAndNsfw(normalizedSearch, nsfw, pageRequest);
    }

    public ImageUsageResponse getUsage(UUID publicId) {
        Image image = imageRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        List<Gallery> galleries = galleryRepository.findAll().stream()
            .filter(g -> g.getImages().contains(image))
            .toList();

        List<ImageUsageResponse.GalleryUsage> usages = galleries.stream().map(g -> {
            List<Tour> tours = tourRepository.findByGalleryOrderBySortOrderAscNameAsc(g).stream()
                .filter(t -> t.getTourImages().stream()
                    .anyMatch(ti -> ti.getImage().getId().equals(image.getId())))
                .toList();
            List<ImageUsageResponse.TourUsage> tourUsages = tours.stream()
                .map(t -> new ImageUsageResponse.TourUsage(t.getPublicId(), t.getName()))
                .toList();
            return new ImageUsageResponse.GalleryUsage(g.getPublicId(), g.getName(), g.getCode(), tourUsages);
        }).toList();

        return new ImageUsageResponse(usages);
    }

    @Transactional
    public ImageBulkLoadResponse bulkLoad(ImageBulkLoadRequest request) {
        Gallery gallery = galleryRepository.findByPublicId(request.galleryPublicId())
            .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));

        List<ImageBulkLoadResponse.ImageBulkLoadResult> results = new ArrayList<>();
        int consecutiveFailures = 0;

        for (String url : request.urls()) {
            ImageCreateRequest createReq = new ImageCreateRequest(
                url, null, null, null, null, null, null, null, false, null, request.galleryPublicId()
            );
            try {
                Image image = create(createReq);
                try {
                    thumbnailService.generateAndStore(image);
                    consecutiveFailures = 0;
                } catch (ThumbnailService.ThumbnailGenerationException e) {
                    consecutiveFailures++;
                    if (consecutiveFailures >= 3 && results.size() < 3) {
                        results.add(new ImageBulkLoadResponse.ImageBulkLoadResult(
                            url, false, null, e.getMessage()));
                        return new ImageBulkLoadResponse(results, true,
                            "First 3 consecutive images failed thumbnail generation — aborting");
                    }
                    results.add(new ImageBulkLoadResponse.ImageBulkLoadResult(
                        url, true, image.getPublicId(), "Warning: thumbnail generation failed: " + e.getMessage()));
                    continue;
                }
                results.add(new ImageBulkLoadResponse.ImageBulkLoadResult(
                    url, true, image.getPublicId(), null));
            } catch (Exception e) {
                consecutiveFailures++;
                if (consecutiveFailures >= 3 && results.size() < 3) {
                    results.add(new ImageBulkLoadResponse.ImageBulkLoadResult(
                        url, false, null, e.getMessage()));
                    return new ImageBulkLoadResponse(results, true,
                        "First 3 consecutive images failed — aborting bulk load");
                }
                results.add(new ImageBulkLoadResponse.ImageBulkLoadResult(
                    url, false, null, e.getMessage()));
            }
        }
        return new ImageBulkLoadResponse(results, false, null);
    }

    @Async
    @Transactional
    public void regenerateAllThumbnails(UUID jobId) {
        AsyncJob job = asyncJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalStateException("Job not found"));

        List<Image> images = imageRepository.findAll();
        job.setTotalItems(images.size());
        asyncJobRepository.save(job);

        List<String> errors = new ArrayList<>();
        int processed = 0;
        int failed = 0;

        for (Image image : images) {
            try {
                thumbnailService.generateAndStore(image);
                processed++;
            } catch (Exception e) {
                failed++;
                errors.add("Image " + image.getShortId() + ": " + e.getMessage());
            }
            job.setProcessedItems(++processed);
            job.setFailedItems(failed);
            asyncJobRepository.save(job);
        }

        try {
            job.setErrorMessages(objectMapper.writeValueAsString(errors));
        } catch (Exception ignored) {}
        job.setStatus(AsyncJobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        asyncJobRepository.save(job);
    }

    public AsyncJob startRegenAllJob() {
        AsyncJob job = new AsyncJob();
        job.setType("REGEN_ALL_THUMBNAILS");
        job.setStatus(AsyncJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job = asyncJobRepository.save(job);
        regenerateAllThumbnails(job.getId());
        return job;
    }

    public AsyncJob getJob(UUID jobId) {
        return asyncJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private void validateNoCircularBaseImage(Image image, Image base) {
        Image current = base;
        while (current != null) {
            if (current.getId().equals(image.getId())) {
                throw new IllegalArgumentException("Circular base image reference detected");
            }
            current = current.getBaseImage();
        }
    }

    public List<Gallery> getGalleriesForImage(Image image) {
        return galleryRepository.findAll().stream()
            .filter(g -> g.getImages().stream()
                .anyMatch(i -> i.getId().equals(image.getId())))
            .toList();
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
