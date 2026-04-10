package org.wadhome.oag.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wadhome.oag.domain.repository.ImageRepository;
import org.wadhome.oag.service.ThumbnailService;

@RestController
@RequestMapping("/api/v1/public/thumbnails")
public class PublicThumbnailController {

    private final ThumbnailService thumbnailService;
    private final ImageRepository imageRepository;

    public PublicThumbnailController(ThumbnailService thumbnailService, ImageRepository imageRepository) {
        this.thumbnailService = thumbnailService;
        this.imageRepository = imageRepository;
    }

    @GetMapping("/{shortId}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String shortId) {
        var imageOpt = imageRepository.findByShortId(shortId);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = thumbnailService.getThumbnailData(shortId, imageOpt.get().getId());
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(data);
    }
}
