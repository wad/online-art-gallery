package org.wadhome.oag.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wadhome.oag.api.dto.*;
import org.wadhome.oag.domain.entity.*;
import org.wadhome.oag.domain.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final ImageRepository imageRepository;
    private final TourRepository tourRepository;
    private final GalleryBioLinkRepository bioLinkRepository;

    public GalleryService(
        GalleryRepository galleryRepository,
        ImageRepository imageRepository,
        TourRepository tourRepository,
        GalleryBioLinkRepository bioLinkRepository
    ) {
        this.galleryRepository = galleryRepository;
        this.imageRepository = imageRepository;
        this.tourRepository = tourRepository;
        this.bioLinkRepository = bioLinkRepository;
    }

    public List<Gallery> listAll() {
        return galleryRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    public Gallery getByPublicId(UUID publicId) {
        return galleryRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Gallery not found"));
    }

    public Gallery getByCode(String code) {
        return galleryRepository.findByCode(code)
            .orElseThrow(() -> new ImageService.ResourceNotFoundException("Gallery not found"));
    }

    @Transactional
    public Gallery create(GalleryCreateRequest request) {
        validateCode(request.code(), null);
        Gallery gallery = new Gallery();
        applyCreateFields(gallery, request);
        return galleryRepository.save(gallery);
    }

    @Transactional
    public Gallery update(UUID publicId, GalleryUpdateRequest request) {
        Gallery gallery = getByPublicId(publicId);
        if (request.code() != null) {
            validateCode(request.code(), publicId);
            gallery.setCode(request.code());
        }
        if (request.name() != null) gallery.setName(request.name());
        if (request.subtitle() != null) gallery.setSubtitle(request.subtitle());
        if (request.description() != null) gallery.setDescription(request.description());
        if (request.visible() != null) gallery.setVisible(request.visible());
        if (request.showAllImagesTour() != null) gallery.setShowAllImagesTour(request.showAllImagesTour());
        if (request.sortOrder() != null) gallery.setSortOrder(request.sortOrder());
        if (request.bioPhotoUrl() != null) gallery.setBioPhotoUrl(request.bioPhotoUrl());
        if (request.bioText() != null) gallery.setBioText(request.bioText());
        if (request.theme() != null) gallery.setTheme(request.theme());
        if (request.borderStyle() != null) gallery.setBorderStyle(request.borderStyle());
        if (request.adsEnabled() != null) gallery.setAdsEnabled(request.adsEnabled());
        if (request.adLandingBanner() != null) gallery.setAdLandingBanner(request.adLandingBanner());
        if (request.adLandingBannerSlot() != null) gallery.setAdLandingBannerSlot(request.adLandingBannerSlot());
        if (request.adImageDetailSidebar() != null) gallery.setAdImageDetailSidebar(request.adImageDetailSidebar());
        if (request.adImageDetailSidebarSlot() != null) gallery.setAdImageDetailSidebarSlot(request.adImageDetailSidebarSlot());
        if (request.isDefault() != null && request.isDefault()) {
            setDefault(gallery);
        }
        if (request.headlinerImagePublicId() != null) {
            Image headliner = imageRepository.findByPublicId(request.headlinerImagePublicId())
                .orElseThrow(() -> new ImageService.ResourceNotFoundException("Headliner image not found"));
            gallery.setHeadlinerImage(headliner);
        }
        if (request.bioLinks() != null) {
            updateBioLinks(gallery, request.bioLinks());
        }
        return galleryRepository.save(gallery);
    }

    @Transactional
    public void setDefault(Gallery gallery) {
        galleryRepository.findByIsDefaultTrue().ifPresent(existing -> {
            if (!existing.getId().equals(gallery.getId())) {
                existing.setDefault(false);
                galleryRepository.save(existing);
            }
        });
        gallery.setDefault(true);
    }

    @Transactional
    public void setDefaultByPublicId(UUID publicId) {
        Gallery gallery = getByPublicId(publicId);
        setDefault(gallery);
        galleryRepository.save(gallery);
    }

    @Transactional
    public void addImages(UUID galleryPublicId, List<UUID> imagePublicIds) {
        Gallery gallery = getByPublicId(galleryPublicId);
        for (UUID imgId : imagePublicIds) {
            imageRepository.findByPublicId(imgId).ifPresent(img -> gallery.getImages().add(img));
        }
        galleryRepository.save(gallery);
    }

    @Transactional
    public void removeImage(UUID galleryPublicId, UUID imagePublicId) {
        Gallery gallery = getByPublicId(galleryPublicId);
        imageRepository.findByPublicId(imagePublicId).ifPresent(img -> {
            gallery.getImages().remove(img);
            // Also remove from gallery's tours
            tourRepository.findByGalleryOrderBySortOrderAscNameAsc(gallery).forEach(tour -> {
                tour.getTourImages().removeIf(ti -> ti.getImage().getId().equals(img.getId()));
            });
        });
        galleryRepository.save(gallery);
    }

    @Transactional
    public void delete(UUID publicId) {
        Gallery gallery = getByPublicId(publicId);
        galleryRepository.delete(gallery);
    }

    @Transactional
    public void reorder(List<Map<String, Object>> reorderItems) {
        for (Map<String, Object> item : reorderItems) {
            UUID publicId = UUID.fromString((String) item.get("publicId"));
            int sortOrder = (Integer) item.get("sortOrder");
            galleryRepository.findByPublicId(publicId).ifPresent(g -> {
                g.setSortOrder(sortOrder);
                galleryRepository.save(g);
            });
        }
    }

    public GalleryExportDto export(UUID publicId) {
        Gallery gallery = getByPublicId(publicId);

        List<String> imageShortIds = gallery.getImages().stream()
            .map(Image::getShortId)
            .toList();

        String headlinerShortId = gallery.getHeadlinerImage() != null
            ? gallery.getHeadlinerImage().getShortId() : null;

        List<BioLinkDto> bioLinks = gallery.getBioLinks().stream()
            .map(l -> new BioLinkDto(l.getLabel(), l.getUrl()))
            .toList();

        GalleryExportDto.GalleryData galleryData = new GalleryExportDto.GalleryData(
            gallery.getName(), gallery.getSubtitle(), gallery.getDescription(),
            gallery.getTheme(), gallery.getBorderStyle(), gallery.isVisible(),
            gallery.isShowAllImagesTour(), gallery.isAdsEnabled(), gallery.isAdLandingBanner(),
            gallery.getAdLandingBannerSlot(), gallery.isAdImageDetailSidebar(),
            gallery.getAdImageDetailSidebarSlot(), gallery.getBioPhotoUrl(), gallery.getBioText(),
            bioLinks, headlinerShortId, imageShortIds
        );

        List<Tour> tours = tourRepository.findByGalleryOrderBySortOrderAscNameAsc(gallery);
        List<GalleryExportDto.TourData> tourData = tours.stream().map(t -> {
            List<String> tourImageShortIds = t.getTourImages().stream()
                .map(ti -> ti.getImage().getShortId())
                .toList();
            String tourHeadlinerShortId = t.getHeadlinerImage() != null
                ? t.getHeadlinerImage().getShortId() : null;
            List<String> tagNames = t.getTags().stream().map(Tag::getName).toList();
            return new GalleryExportDto.TourData(
                t.getName(), t.getDescription(), t.getSortOrder(),
                tourHeadlinerShortId, tourImageShortIds, tagNames
            );
        }).toList();

        return new GalleryExportDto(1, galleryData, tourData);
    }

    @Transactional
    public GalleryImportResponse importGallery(GalleryImportRequest request) {
        List<String> warnings = new ArrayList<>();
        GalleryExportDto export = request.export();
        GalleryExportDto.GalleryData data = export.gallery();

        // Step 1: Create the gallery
        Gallery gallery = new Gallery();
        gallery.setCode(request.code());
        gallery.setName(data.name());
        gallery.setSubtitle(data.subtitle());
        gallery.setDescription(data.description());
        if (data.theme() != null) gallery.setTheme(data.theme());
        if (data.borderStyle() != null) gallery.setBorderStyle(data.borderStyle());
        gallery.setVisible(data.visible());
        gallery.setShowAllImagesTour(data.showAllImagesTour());
        gallery.setAdsEnabled(data.adsEnabled());
        gallery.setAdLandingBanner(data.adLandingBanner());
        gallery.setAdLandingBannerSlot(data.adLandingBannerSlot());
        gallery.setAdImageDetailSidebar(data.adImageDetailSidebar());
        gallery.setAdImageDetailSidebarSlot(data.adImageDetailSidebarSlot());
        gallery.setBioPhotoUrl(data.bioPhotoUrl());
        gallery.setBioText(data.bioText());

        if (data.bioLinks() != null) {
            updateBioLinks(gallery, data.bioLinks());
        }
        gallery = galleryRepository.save(gallery);

        // Step 2: Associate images
        if (data.imageShortIds() != null) {
            for (String shortId : data.imageShortIds()) {
                var imageOpt = imageRepository.findByShortId(shortId);
                if (imageOpt.isPresent()) {
                    gallery.getImages().add(imageOpt.get());
                } else {
                    warnings.add("Image " + shortId + " not found — skipped");
                }
            }
            gallery = galleryRepository.save(gallery);
        }

        // Set headliner
        if (data.headlinerImageShortId() != null) {
            imageRepository.findByShortId(data.headlinerImageShortId())
                .ifPresentOrElse(
                    gallery::setHeadlinerImage,
                    () -> warnings.add("Headliner image " + data.headlinerImageShortId() + " not found")
                );
            gallery = galleryRepository.save(gallery);
        }

        // Steps 3-5: Build tours
        if (export.tours() != null) {
            for (GalleryExportDto.TourData tourData : export.tours()) {
                Tour tour = new Tour();
                tour.setName(tourData.name());
                tour.setDescription(tourData.description());
                tour.setSortOrder(tourData.sortOrder());
                tour.setGallery(gallery);
                tour = tourRepository.save(tour);

                // Step 3: Resolve tour images from gallery's images
                if (tourData.imageShortIds() != null) {
                    int sortOrder = 0;
                    for (String shortId : tourData.imageShortIds()) {
                        boolean inGallery = gallery.getImages().stream()
                            .anyMatch(img -> img.getShortId().equals(shortId));
                        if (inGallery) {
                            var imageOpt = imageRepository.findByShortId(shortId);
                            if (imageOpt.isPresent()) {
                                TourImage ti = new TourImage();
                                ti.setTour(tour);
                                ti.setImage(imageOpt.get());
                                ti.setSortOrder(sortOrder++);
                                tour.getTourImages().add(ti);
                            }
                        } else {
                            warnings.add("Tour '" + tourData.name() + "': image " + shortId + " not in gallery — skipped");
                        }
                    }
                }

                // Step 5: Set headliner
                if (tourData.headlinerImageShortId() != null) {
                    imageRepository.findByShortId(tourData.headlinerImageShortId())
                        .ifPresent(tour::setHeadlinerImage);
                }

                tourRepository.save(tour);
            }
        }

        return new GalleryImportResponse(null, warnings); // gallery response mapped by controller
    }

    private void validateCode(String code, UUID excludePublicId) {
        if (!code.matches("^[A-Z][A-Z0-9]{0,4}$")) {
            throw new IllegalArgumentException("Gallery code must match ^[A-Z][A-Z0-9]{0,4}$");
        }
        galleryRepository.findByCode(code).ifPresent(existing -> {
            if (excludePublicId == null || !existing.getPublicId().equals(excludePublicId)) {
                throw new IllegalArgumentException("Gallery code already in use: " + code);
            }
        });
    }

    private void applyCreateFields(Gallery gallery, GalleryCreateRequest request) {
        gallery.setCode(request.code());
        gallery.setName(request.name());
        gallery.setSubtitle(request.subtitle());
        gallery.setDescription(request.description());
        gallery.setVisible(request.visible());
        gallery.setShowAllImagesTour(request.showAllImagesTour());
        gallery.setSortOrder(request.sortOrder());
        gallery.setBioPhotoUrl(request.bioPhotoUrl());
        gallery.setBioText(request.bioText());
        if (request.theme() != null) gallery.setTheme(request.theme());
        gallery.setBorderStyle(request.borderStyle());
        gallery.setAdsEnabled(request.adsEnabled());
        gallery.setAdLandingBanner(request.adLandingBanner());
        gallery.setAdLandingBannerSlot(request.adLandingBannerSlot());
        gallery.setAdImageDetailSidebar(request.adImageDetailSidebar());
        gallery.setAdImageDetailSidebarSlot(request.adImageDetailSidebarSlot());
        if (request.isDefault()) {
            setDefault(gallery);
        }
        if (request.headlinerImagePublicId() != null) {
            imageRepository.findByPublicId(request.headlinerImagePublicId())
                .ifPresent(gallery::setHeadlinerImage);
        }
        if (request.bioLinks() != null) {
            updateBioLinks(gallery, request.bioLinks());
        }
    }

    private void updateBioLinks(Gallery gallery, List<BioLinkDto> linkDtos) {
        gallery.getBioLinks().clear();
        for (int i = 0; i < linkDtos.size(); i++) {
            BioLinkDto dto = linkDtos.get(i);
            GalleryBioLink link = new GalleryBioLink();
            link.setGallery(gallery);
            link.setLabel(dto.label());
            link.setUrl(dto.url());
            link.setSortOrder(i);
            gallery.getBioLinks().add(link);
        }
    }
}
