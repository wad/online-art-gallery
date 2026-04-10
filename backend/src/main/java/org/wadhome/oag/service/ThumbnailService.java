package org.wadhome.oag.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wadhome.oag.config.CacheConfig;
import org.wadhome.oag.domain.entity.Image;
import org.wadhome.oag.domain.entity.Thumbnail;
import org.wadhome.oag.domain.repository.ThumbnailRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ThumbnailService {

    private static final int MAX_SIZE = 100;
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private final ThumbnailRepository thumbnailRepository;

    public ThumbnailService(ThumbnailRepository thumbnailRepository) {
        this.thumbnailRepository = thumbnailRepository;
    }

    /**
     * Generates and stores a thumbnail for the given image.
     * Throws ThumbnailGenerationException if the URL can't be fetched or isn't an image.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.THUMBNAIL_CACHE, key = "#image.shortId")
    public void generateAndStore(Image image) {
        byte[] data = fetchAndResize(image.getUrl());
        Thumbnail thumbnail = thumbnailRepository.findById(image.getId())
            .orElse(new Thumbnail());
        thumbnail.setImage(image);
        thumbnail.setData(data);
        thumbnailRepository.save(thumbnail);
    }

    @Cacheable(cacheNames = CacheConfig.THUMBNAIL_CACHE, key = "#shortId")
    public byte[] getThumbnailData(String shortId, Long imageId) {
        return thumbnailRepository.findById(imageId)
            .map(Thumbnail::getData)
            .orElse(null);
    }

    @CacheEvict(cacheNames = CacheConfig.THUMBNAIL_CACHE, key = "#shortId")
    public void evict(String shortId) {
        // Cache eviction only — actual deletion cascades from image delete
    }

    private byte[] fetchAndResize(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new ThumbnailGenerationException("Failed to fetch image URL: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ThumbnailGenerationException(
                "Image URL returned HTTP " + response.statusCode());
        }

        try (InputStream body = response.body()) {
            BufferedImage original = ImageIO.read(body);
            if (original == null) {
                throw new ThumbnailGenerationException("URL did not return a valid image");
            }

            int origW = original.getWidth();
            int origH = original.getHeight();
            int targetW, targetH;

            if (origW <= MAX_SIZE && origH <= MAX_SIZE) {
                // Scale up to fit within 100x100
                double scale = Math.min((double) MAX_SIZE / origW, (double) MAX_SIZE / origH);
                targetW = (int) (origW * scale);
                targetH = (int) (origH * scale);
            } else {
                // Scale down to fit within 100x100
                double scale = Math.min((double) MAX_SIZE / origW, (double) MAX_SIZE / origH);
                targetW = (int) (origW * scale);
                targetH = (int) (origH * scale);
            }
            targetW = Math.max(1, targetW);
            targetH = Math.max(1, targetH);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(original)
                .size(targetW, targetH)
                .outputFormat("JPEG")
                .toOutputStream(out);
            return out.toByteArray();
        } catch (ThumbnailGenerationException e) {
            throw e;
        } catch (IOException e) {
            throw new ThumbnailGenerationException("Failed to process image: " + e.getMessage(), e);
        }
    }

    public static class ThumbnailGenerationException extends RuntimeException {
        public ThumbnailGenerationException(String message) {
            super(message);
        }
        public ThumbnailGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
