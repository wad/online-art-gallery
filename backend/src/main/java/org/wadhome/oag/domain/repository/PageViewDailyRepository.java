package org.wadhome.oag.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wadhome.oag.domain.entity.PageViewDaily;
import org.wadhome.oag.domain.entity.ViewContext;
import org.wadhome.oag.domain.entity.ViewEntityType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PageViewDailyRepository extends JpaRepository<PageViewDaily, Long> {

    Optional<PageViewDaily> findByViewDateAndEntityTypeAndEntityIdAndContext(
        LocalDate viewDate, ViewEntityType entityType, Long entityId, ViewContext context
    );

    @Modifying
    @Query(value = """
        INSERT INTO page_view_daily (view_date, entity_type, entity_id, context, view_count)
        VALUES (:viewDate, :entityType, :entityId, :context, 1)
        ON CONFLICT (view_date, entity_type, entity_id, context)
        DO UPDATE SET view_count = page_view_daily.view_count + 1
        """, nativeQuery = true)
    void upsertView(
        @Param("viewDate") LocalDate viewDate,
        @Param("entityType") String entityType,
        @Param("entityId") Long entityId,
        @Param("context") String context
    );

    @Query("""
        SELECT p FROM PageViewDaily p
        WHERE p.entityType = :entityType
          AND p.viewDate BETWEEN :from AND :to
        ORDER BY p.entityId, p.viewDate
        """)
    List<PageViewDaily> findByEntityTypeAndDateRange(
        @Param("entityType") ViewEntityType entityType,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
