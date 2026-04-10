package org.wadhome.oag.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.wadhome.oag.api.dto.*;
import org.wadhome.oag.api.mapper.GalleryMapper;
import org.wadhome.oag.domain.entity.Gallery;
import org.wadhome.oag.service.GalleryService;
import org.wadhome.oag.service.ImageService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/galleries")
public class AdminGalleryController {

    private final GalleryService galleryService;
    private final GalleryMapper galleryMapper;

    public AdminGalleryController(GalleryService galleryService, GalleryMapper galleryMapper) {
        this.galleryService = galleryService;
        this.galleryMapper = galleryMapper;
    }

    @PostMapping
    public ResponseEntity<GalleryResponse> create(@Valid @RequestBody GalleryCreateRequest request) {
        Gallery gallery = galleryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{publicId}")
            .buildAndExpand(gallery.getPublicId())
            .toUri();
        return ResponseEntity.created(location).body(galleryMapper.toGalleryResponse(gallery));
    }

    @GetMapping
    public ResponseEntity<List<GalleryResponse>> list() {
        List<GalleryResponse> responses = galleryService.listAll().stream()
            .map(galleryMapper::toGalleryResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<GalleryResponse> get(@PathVariable UUID publicId) {
        return ResponseEntity.ok(galleryMapper.toGalleryResponse(galleryService.getByPublicId(publicId)));
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<GalleryResponse> update(
        @PathVariable UUID publicId,
        @Valid @RequestBody GalleryUpdateRequest request
    ) {
        return ResponseEntity.ok(galleryMapper.toGalleryResponse(galleryService.update(publicId, request)));
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody List<Map<String, Object>> reorderItems) {
        galleryService.reorder(reorderItems);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{publicId}/default")
    public ResponseEntity<Void> setDefault(@PathVariable UUID publicId) {
        galleryService.setDefaultByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{publicId}/images")
    public ResponseEntity<Void> addImages(
        @PathVariable UUID publicId,
        @RequestBody Map<String, List<UUID>> body
    ) {
        galleryService.addImages(publicId, body.get("imagePublicIds"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{publicId}/images/{imagePublicId}")
    public ResponseEntity<Void> removeImage(
        @PathVariable UUID publicId,
        @PathVariable UUID imagePublicId
    ) {
        galleryService.removeImage(publicId, imagePublicId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        galleryService.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{publicId}/export")
    public ResponseEntity<GalleryExportDto> export(@PathVariable UUID publicId) {
        return ResponseEntity.ok(galleryService.export(publicId));
    }

    @PostMapping("/import")
    public ResponseEntity<GalleryImportResponse> importGallery(@Valid @RequestBody GalleryImportRequest request) {
        GalleryImportResponse result = galleryService.importGallery(request);
        Gallery gallery = galleryService.getByPublicId(galleryService.getByCode(request.code()).getPublicId());
        GalleryImportResponse withGallery = new GalleryImportResponse(
            galleryMapper.toGalleryResponse(gallery), result.warnings());
        return ResponseEntity.status(HttpStatus.CREATED).body(withGallery);
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
