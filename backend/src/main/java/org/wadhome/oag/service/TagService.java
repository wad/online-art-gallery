package org.wadhome.oag.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wadhome.oag.api.dto.TagCreateRequest;
import org.wadhome.oag.domain.entity.Image;
import org.wadhome.oag.domain.entity.Tag;
import org.wadhome.oag.domain.entity.Tour;
import org.wadhome.oag.domain.entity.TourImage;
import org.wadhome.oag.domain.repository.*;

import java.util.List;
import java.util.UUID;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ImageRepository imageRepository;
    private final GalleryRepository galleryRepository;
    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;

    public TagService(
        TagRepository tagRepository,
        ImageRepository imageRepository,
        GalleryRepository galleryRepository,
        TourRepository tourRepository,
        TourImageRepository tourImageRepository
    ) {
        this.tagRepository = tagRepository;
        this.imageRepository = imageRepository;
        this.galleryRepository = galleryRepository;
        this.tourRepository = tourRepository;
        this.tourImageRepository = tourImageRepository;
    }

    public List<Tag> listAll() {
        return tagRepository.findAllOrderByName();
    }

    public long getImageCount(Tag tag) {
        return imageRepository.findAll().stream()
            .filter(img -> img.getTags().contains(tag))
            .count();
    }

    @Transactional
    public Tag create(TagCreateRequest request) {
        if (tagRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Tag already exists: " + request.name());
        }
        Tag tag = new Tag();
        tag.setName(request.name());
        return tagRepository.save(tag);
    }

    @Transactional
    public void delete(UUID tagPublicId) {
        Tag tag = tagRepository.findByPublicId(tagPublicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Tag not found"));

        // Find tours associated with this tag before removing it
        List<Tour> affectedTours = tourRepository.findAll().stream()
            .filter(t -> t.getTags().contains(tag))
            .toList();

        // Remove tag from all images
        imageRepository.findAll().stream()
            .filter(img -> img.getTags().contains(tag))
            .forEach(img -> {
                img.getTags().remove(tag);
                imageRepository.save(img);
            });

        // For each affected tour, remove images that were only included via this tag
        for (Tour tour : affectedTours) {
            removeImagesOnlyLinkedByTag(tour, tag);
        }

        tagRepository.delete(tag);
    }

    @Transactional
    public void addTagToImage(UUID imagePublicId, UUID tagPublicId) {
        Image image = imageRepository.findByPublicId(imagePublicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Image not found"));
        Tag tag = tagRepository.findByPublicId(tagPublicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Tag not found"));

        image.getTags().add(tag);
        imageRepository.save(image);

        // Auto-sync: add image to tours that are associated with this tag
        // within galleries that contain this image
        List<Tour> toursWithTag = tourRepository.findAll().stream()
            .filter(t -> t.getTags().contains(tag))
            .filter(t -> t.getGallery().getImages().stream()
                .anyMatch(i -> i.getId().equals(image.getId())))
            .toList();

        for (Tour tour : toursWithTag) {
            boolean alreadyInTour = tour.getTourImages().stream()
                .anyMatch(ti -> ti.getImage().getId().equals(image.getId()));
            if (!alreadyInTour) {
                int nextOrder = tour.getTourImages().size();
                TourImage ti = new TourImage();
                ti.setTour(tour);
                ti.setImage(image);
                ti.setSortOrder(nextOrder);
                tourImageRepository.save(ti);
            }
        }
    }

    @Transactional
    public void removeTagFromImage(UUID imagePublicId, UUID tagPublicId) {
        Image image = imageRepository.findByPublicId(imagePublicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Image not found"));
        Tag tag = tagRepository.findByPublicId(tagPublicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Tag not found"));

        image.getTags().remove(tag);
        imageRepository.save(image);

        // Auto-sync: remove image from tours associated with this tag,
        // but only if the image has no other tags that the tour is also associated with
        List<Tour> toursWithTag = tourRepository.findAll().stream()
            .filter(t -> t.getTags().contains(tag))
            .toList();

        for (Tour tour : toursWithTag) {
            boolean hasOtherMatchingTag = image.getTags().stream()
                .anyMatch(otherTag -> tour.getTags().contains(otherTag));
            if (!hasOtherMatchingTag) {
                tourImageRepository.deleteByIdTourIdAndIdImageId(tour.getId(), image.getId());
            }
        }
    }

    private void removeImagesOnlyLinkedByTag(Tour tour, Tag deletedTag) {
        List<TourImage> toRemove = tour.getTourImages().stream()
            .filter(ti -> {
                Image img = ti.getImage();
                // Check if this image has any remaining tags that the tour is associated with
                return img.getTags().stream()
                    .filter(t -> !t.equals(deletedTag))
                    .noneMatch(t -> tour.getTags().contains(t));
            })
            .toList();

        for (TourImage ti : toRemove) {
            tourImageRepository.deleteByIdTourIdAndIdImageId(tour.getId(), ti.getImage().getId());
        }
    }
}
