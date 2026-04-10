package org.wadhome.oag.api.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wadhome.oag.api.dto.ViewStatsResponse;
import org.wadhome.oag.service.ViewStatsService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final ViewStatsService viewStatsService;

    public AdminStatsController(ViewStatsService viewStatsService) {
        this.viewStatsService = viewStatsService;
    }

    @GetMapping("/galleries")
    public ResponseEntity<List<ViewStatsResponse>> galleryStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(viewStatsService.getGalleryStats(from, to));
    }

    @GetMapping("/tours")
    public ResponseEntity<List<ViewStatsResponse>> tourStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) UUID galleryPublicId
    ) {
        return ResponseEntity.ok(viewStatsService.getTourStats(from, to, galleryPublicId));
    }

    @GetMapping("/images")
    public ResponseEntity<List<ViewStatsResponse>> imageStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(viewStatsService.getImageStats(from, to));
    }
}
