package org.wadhome.oag.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wadhome.oag.domain.entity.Image;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByPublicId(UUID publicId);

    Optional<Image> findByShortId(String shortId);

    boolean existsByShortId(String shortId);

    @Query("""
        SELECT i FROM Image i
        WHERE (:search IS NULL OR
               LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(i.artistName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:nsfw IS NULL OR i.nsfw = :nsfw)
        """)
    Page<Image> findBySearchAndNsfw(
        @Param("search") String search,
        @Param("nsfw") Boolean nsfw,
        Pageable pageable
    );

    @Query("""
        SELECT i FROM Image i
        WHERE NOT EXISTS (
            SELECT 1 FROM Gallery g JOIN g.images gi WHERE gi = i
        )
        """)
    List<Image> findOrphans();
}
