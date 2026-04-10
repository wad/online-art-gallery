package org.wadhome.oag.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wadhome.oag.domain.entity.Thumbnail;

public interface ThumbnailRepository extends JpaRepository<Thumbnail, Long> {
}
