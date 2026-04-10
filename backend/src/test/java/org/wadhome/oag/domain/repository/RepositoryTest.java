package org.wadhome.oag.domain.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.wadhome.oag.domain.entity.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private AdminConfigRepository adminConfigRepository;

    @Autowired
    private AsyncJobRepository asyncJobRepository;

    @Test
    void imageCanBeSavedAndFoundByShortId() {
        Image image = new Image();
        image.setShortId("00001234");
        image.setUrl("https://example.com/image.jpg");
        imageRepository.save(image);

        Optional<Image> found = imageRepository.findByShortId("00001234");
        assertThat(found).isPresent();
        assertThat(found.get().getPublicId()).isNotNull();
        assertThat(found.get().getUrl()).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    void imageCanBeSavedAndFoundByPublicId() {
        Image image = new Image();
        image.setShortId("00001235");
        image.setUrl("https://example.com/image2.jpg");
        Image saved = imageRepository.save(image);

        Optional<Image> found = imageRepository.findByPublicId(saved.getPublicId());
        assertThat(found).isPresent();
    }

    @Test
    void orphanDetectionFindsImagesWithNoGalleryAssociation() {
        Image orphan = new Image();
        orphan.setShortId("00001236");
        orphan.setUrl("https://example.com/orphan.jpg");
        imageRepository.save(orphan);

        Image associated = new Image();
        associated.setShortId("00001237");
        associated.setUrl("https://example.com/associated.jpg");
        imageRepository.save(associated);

        Gallery gallery = new Gallery();
        gallery.setCode("TEST1");
        gallery.setName("Test Gallery");
        gallery.getImages().add(associated);
        galleryRepository.save(gallery);

        List<Image> orphans = imageRepository.findOrphans();
        assertThat(orphans).extracting(Image::getShortId).contains("00001236");
        assertThat(orphans).extracting(Image::getShortId).doesNotContain("00001237");
    }

    @Test
    void tagCanBeSavedAndFoundByName() {
        Tag tag = new Tag();
        tag.setName("landscape");
        tagRepository.save(tag);

        Optional<Tag> found = tagRepository.findByName("landscape");
        assertThat(found).isPresent();
        assertThat(found.get().getPublicId()).isNotNull();
    }

    @Test
    void galleryDefaultFlagWorks() {
        Gallery g = new Gallery();
        g.setCode("DEFLT");
        g.setName("Default Gallery");
        g.setDefault(true);
        galleryRepository.save(g);

        Optional<Gallery> found = galleryRepository.findByIsDefaultTrue();
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("DEFLT");
    }

    @Test
    void galleryCanBeFoundByCode() {
        Gallery g = new Gallery();
        g.setCode("ABC");
        g.setName("ABC Gallery");
        galleryRepository.save(g);

        Optional<Gallery> found = galleryRepository.findByCode("ABC");
        assertThat(found).isPresent();
    }

    @Test
    void tourBelongsToGallery() {
        Gallery gallery = new Gallery();
        gallery.setCode("TOUR1");
        gallery.setName("Tour Gallery");
        galleryRepository.save(gallery);

        Tour tour = new Tour();
        tour.setName("My Tour");
        tour.setGallery(gallery);
        tourRepository.save(tour);

        List<Tour> tours = tourRepository.findByGalleryOrderBySortOrderAscNameAsc(gallery);
        assertThat(tours).hasSize(1);
        assertThat(tours.get(0).getName()).isEqualTo("My Tour");
    }

    @Test
    void adminConfigCanBeStoredAndRetrieved() {
        AdminConfig config = new AdminConfig();
        config.setPasswordHash("$2a$10$somehashedpassword");
        adminConfigRepository.save(config);

        Optional<AdminConfig> found = adminConfigRepository.findFirstBy();
        assertThat(found).isPresent();
        assertThat(found.get().getPasswordHash()).startsWith("$2a$10$");
    }

    @Test
    void asyncJobCanBeTracked() {
        AsyncJob job = new AsyncJob();
        job.setType("REGEN_ALL_THUMBNAILS");
        job.setStatus(AsyncJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job.setTotalItems(10);
        asyncJobRepository.save(job);

        assertThat(job.getId()).isNotNull();
        Optional<AsyncJob> found = asyncJobRepository.findById(job.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(AsyncJobStatus.RUNNING);
    }

    @Test
    void imageSearchByTitle() {
        Image img = new Image();
        img.setShortId("00009999");
        img.setUrl("https://example.com/search.jpg");
        img.setTitle("Starry Night");
        imageRepository.save(img);

        var page = imageRepository.findBySearchAndNsfw("starry", null,
            org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Image::getTitle).contains("Starry Night");
    }

    @Test
    void galleryImageScopingWorks() {
        Image img1 = new Image();
        img1.setShortId("00002001");
        img1.setUrl("https://example.com/1.jpg");
        imageRepository.save(img1);

        Image img2 = new Image();
        img2.setShortId("00002002");
        img2.setUrl("https://example.com/2.jpg");
        imageRepository.save(img2);

        Gallery g = new Gallery();
        g.setCode("SCOPE");
        g.setName("Scoped Gallery");
        g.getImages().add(img1);
        galleryRepository.save(g);

        Gallery saved = galleryRepository.findByCode("SCOPE").orElseThrow();
        assertThat(saved.getImages()).hasSize(1);
        assertThat(saved.getImages().iterator().next().getShortId()).isEqualTo("00002001");
    }
}
