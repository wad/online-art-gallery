package org.wadhome.oag.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wadhome.oag.api.dto.ThemeResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
public class PublicMetaController {

    @GetMapping("/themes")
    public ResponseEntity<List<ThemeResponse>> getThemes() {
        return ResponseEntity.ok(ThemeResponse.all());
    }
}
