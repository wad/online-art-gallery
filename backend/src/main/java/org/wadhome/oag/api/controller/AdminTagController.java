package org.wadhome.oag.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.wadhome.oag.api.dto.TagCreateRequest;
import org.wadhome.oag.api.dto.TagResponse;
import org.wadhome.oag.api.mapper.TagMapper;
import org.wadhome.oag.domain.entity.Tag;
import org.wadhome.oag.service.ImageService;
import org.wadhome.oag.service.TagService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tags")
public class AdminTagController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    public AdminTagController(TagService tagService, TagMapper tagMapper) {
        this.tagService = tagService;
        this.tagMapper = tagMapper;
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> list() {
        List<Tag> tags = tagService.listAll();
        List<TagResponse> responses = tags.stream()
            .map(t -> tagMapper.toTagResponse(t, tagService.getImageCount(t)))
            .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagCreateRequest request) {
        Tag tag = tagService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{publicId}")
            .buildAndExpand(tag.getPublicId())
            .toUri();
        return ResponseEntity.created(location).body(tagMapper.toTagResponse(tag, 0));
    }

    @DeleteMapping("/{tagPublicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID tagPublicId) {
        tagService.delete(tagPublicId);
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
