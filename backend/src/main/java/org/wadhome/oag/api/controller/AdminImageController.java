package org.wadhome.oag.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.wadhome.oag.api.dto.*;
import org.wadhome.oag.api.mapper.AsyncJobMapper;
import org.wadhome.oag.api.mapper.ImageMapper;
import org.wadhome.oag.domain.entity.AsyncJob;
import org.wadhome.oag.domain.entity.Gallery;
import org.wadhome.oag.domain.entity.Image;
import org.wadhome.oag.service.ImageService;
import org.wadhome.oag.service.TagService;
import org.wadhome.oag.service.ThumbnailService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/images")
public class AdminImageController {

    private final ImageService imageService;
    private final TagService tagService;
    private final ThumbnailService thumbnailService;
    private final ImageMapper imageMapper;
    private final AsyncJobMapper asyncJobMapper;

    public AdminImageController(
        ImageService imageService,
        TagService tagService,
        ThumbnailService thumbnailService,
        ImageMapper imageMapper,
        AsyncJobMapper asyncJobMapper
    ) {
        this.imageService = imageService;
        this.tagService = tagService;
        this.thumbnailService = thumbnailService;
        this.imageMapper = imageMapper;
        this.asyncJobMapper = asyncJobMapper;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ImageCreateRequest request) {
        Image image = imageService.create(request);
        String thumbnailWarning = null;
        try {
            thumbnailService.generateAndStore(image);
        } catch (ThumbnailService.ThumbnailGenerationException e) {
            thumbnailWarning = e.getMessage();
        }

        List<Gallery> galleries = imageService.getGalleriesForImage(image);
        ImageResponse response = imageMapper.toImageResponse(image, galleries);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{publicId}")
            .buildAndExpand(image.getPublicId())
            .toUri();

        if (thumbnailWarning != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                .location(location)
                .body(Map.of("image", response, "thumbnailWarning", thumbnailWarning));
        }
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<ImageBulkLoadResponse> bulkLoad(@Valid @RequestBody ImageBulkLoadRequest request) {
        return ResponseEntity.ok(imageService.bulkLoad(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ImageResponse>> list(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) UUID tag,
        @RequestParam(required = false) Boolean nsfw,
        @RequestParam(required = false) Boolean orphan,
        @RequestParam(defaultValue = "uploadedAt") String sort,
        @RequestParam(defaultValue = "desc") String dir,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<Image> images = imageService.list(q, tag, nsfw, orphan, sort, dir, page, size);
        Page<ImageResponse> responses = images.map(img -> {
            List<Gallery> galleries = imageService.getGalleriesForImage(img);
            return imageMapper.toImageResponse(img, galleries);
        });
        return ResponseEntity.ok(PageResponse.of(responses));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<ImageResponse> get(@PathVariable UUID publicId) {
        Image image = imageService.getByPublicId(publicId);
        List<Gallery> galleries = imageService.getGalleriesForImage(image);
        return ResponseEntity.ok(imageMapper.toImageResponse(image, galleries));
    }

    @GetMapping("/{publicId}/usage")
    public ResponseEntity<ImageUsageResponse> getUsage(@PathVariable UUID publicId) {
        return ResponseEntity.ok(imageService.getUsage(publicId));
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<?> update(
        @PathVariable UUID publicId,
        @Valid @RequestBody ImageUpdateRequest request
    ) {
        String thumbnailWarning = null;
        Image image;
        try {
            image = imageService.update(publicId, request);
        } catch (ThumbnailService.ThumbnailGenerationException e) {
            // URL was updated but thumbnail regen failed — still return updated image
            Image existing = imageService.getByPublicId(publicId);
            List<Gallery> galleries = imageService.getGalleriesForImage(existing);
            return ResponseEntity.ok(Map.of(
                "image", imageMapper.toImageResponse(existing, galleries),
                "thumbnailWarning", e.getMessage()
            ));
        }
        List<Gallery> galleries = imageService.getGalleriesForImage(image);
        ImageResponse response = imageMapper.toImageResponse(image, galleries);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bulk-metadata")
    public ResponseEntity<BulkMetadataUpdateResponse> bulkMetadataUpdate(
        @Valid @RequestBody BulkMetadataUpdateRequest request
    ) {
        List<BulkMetadataUpdateResponse.ImageUpdateResult> results = request.updates().stream()
            .map(update -> {
                try {
                    Image image = imageService.getByPublicId(update.imagePublicId());
                    ImageUpdateRequest updateReq = new ImageUpdateRequest(
                        image.getUrl(),
                        update.title() != null ? update.title() : image.getTitle(),
                        update.artistName() != null ? update.artistName() : image.getArtistName(),
                        update.description() != null ? update.description() : image.getDescription(),
                        update.artCreationDate() != null ? update.artCreationDate() : image.getArtCreationDate(),
                        update.artistComments() != null ? update.artistComments() : image.getArtistComments(),
                        update.notes() != null ? update.notes() : image.getNotes(),
                        update.adminNotes() != null ? update.adminNotes() : image.getAdminNotes(),
                        update.nsfw() != null ? update.nsfw() : image.isNsfw(),
                        image.getBaseImage() != null ? image.getBaseImage().getPublicId() : null
                    );
                    imageService.update(update.imagePublicId(), updateReq);
                    return new BulkMetadataUpdateResponse.ImageUpdateResult(update.imagePublicId(), true, null);
                } catch (Exception e) {
                    return new BulkMetadataUpdateResponse.ImageUpdateResult(update.imagePublicId(), false, e.getMessage());
                }
            })
            .toList();
        return ResponseEntity.ok(new BulkMetadataUpdateResponse(results));
    }

    @PutMapping("/{publicId}/base-image")
    public ResponseEntity<Void> setBaseImage(
        @PathVariable UUID publicId,
        @RequestBody Map<String, UUID> body
    ) {
        UUID baseImagePublicId = body.get("baseImagePublicId");
        Image image = imageService.getByPublicId(publicId);
        ImageUpdateRequest req = new ImageUpdateRequest(
            image.getUrl(), image.getTitle(), image.getArtistName(),
            image.getDescription(), image.getArtCreationDate(), image.getArtistComments(),
            image.getNotes(), image.getAdminNotes(), image.isNsfw(), baseImagePublicId
        );
        imageService.update(publicId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{publicId}/base-image")
    public ResponseEntity<Void> clearBaseImage(@PathVariable UUID publicId) {
        Image image = imageService.getByPublicId(publicId);
        ImageUpdateRequest req = new ImageUpdateRequest(
            image.getUrl(), image.getTitle(), image.getArtistName(),
            image.getDescription(), image.getArtCreationDate(), image.getArtistComments(),
            image.getNotes(), image.getAdminNotes(), image.isNsfw(), null
        );
        imageService.update(publicId, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{publicId}/regenerate-thumbnail")
    public ResponseEntity<?> regenerateThumbnail(@PathVariable UUID publicId) {
        Image image = imageService.getByPublicId(publicId);
        try {
            thumbnailService.generateAndStore(image);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (ThumbnailService.ThumbnailGenerationException e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/regenerate-all-thumbnails")
    public ResponseEntity<Map<String, UUID>> regenerateAllThumbnails() {
        AsyncJob job = imageService.startRegenAllJob();
        return ResponseEntity.accepted().body(Map.of("jobId", job.getId()));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<AsyncJobResponse> getJob(@PathVariable UUID jobId) {
        AsyncJob job = imageService.getJob(jobId);
        return ResponseEntity.ok(asyncJobMapper.toAsyncJobResponse(job));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID publicId,
        @RequestParam String substituteShortId
    ) {
        imageService.delete(publicId, substituteShortId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{publicId}/tags")
    public ResponseEntity<Void> addTag(
        @PathVariable UUID publicId,
        @RequestBody Map<String, UUID> body
    ) {
        tagService.addTagToImage(publicId, body.get("tagPublicId"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{publicId}/tags/{tagPublicId}")
    public ResponseEntity<Void> removeTag(
        @PathVariable UUID publicId,
        @PathVariable UUID tagPublicId
    ) {
        tagService.removeTagFromImage(publicId, tagPublicId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ImageService.ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ImageService.ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        return ResponseEntity.badRequest().body(problem);
    }
}
