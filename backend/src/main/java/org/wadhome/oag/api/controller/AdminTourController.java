package org.wadhome.oag.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.wadhome.oag.api.dto.TourCreateRequest;
import org.wadhome.oag.api.dto.TourResponse;
import org.wadhome.oag.api.dto.TourUpdateRequest;
import org.wadhome.oag.api.mapper.TourMapper;
import org.wadhome.oag.domain.entity.Tour;
import org.wadhome.oag.service.ImageService;
import org.wadhome.oag.service.TourService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/galleries/{galleryPublicId}/tours")
public class AdminTourController {

    private final TourService tourService;
    private final TourMapper tourMapper;

    public AdminTourController(TourService tourService, TourMapper tourMapper) {
        this.tourService = tourService;
        this.tourMapper = tourMapper;
    }

    @PostMapping
    public ResponseEntity<TourResponse> create(
        @PathVariable UUID galleryPublicId,
        @Valid @RequestBody TourCreateRequest request
    ) {
        Tour tour = tourService.create(galleryPublicId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{tourPublicId}")
            .buildAndExpand(tour.getPublicId())
            .toUri();
        return ResponseEntity.created(location).body(tourMapper.toTourResponse(tour));
    }

    @GetMapping
    public ResponseEntity<List<TourResponse>> list(@PathVariable UUID galleryPublicId) {
        List<TourResponse> responses = tourService.listByGallery(galleryPublicId).stream()
            .map(tourMapper::toTourResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{tourPublicId}")
    public ResponseEntity<TourResponse> get(
        @PathVariable UUID galleryPublicId,
        @PathVariable UUID tourPublicId
    ) {
        Tour tour = tourService.getByPublicId(tourPublicId);
        return ResponseEntity.ok(tourMapper.toTourResponse(tour));
    }

    @PutMapping("/{tourPublicId}")
    public ResponseEntity<TourResponse> update(
        @PathVariable UUID galleryPublicId,
        @PathVariable UUID tourPublicId,
        @Valid @RequestBody TourUpdateRequest request
    ) {
        Tour tour = tourService.update(tourPublicId, request);
        return ResponseEntity.ok(tourMapper.toTourResponse(tour));
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(
        @PathVariable UUID galleryPublicId,
        @RequestBody List<Map<String, Object>> reorderItems
    ) {
        tourService.reorder(galleryPublicId, reorderItems);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tourPublicId}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID galleryPublicId,
        @PathVariable UUID tourPublicId
    ) {
        tourService.delete(tourPublicId);
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
