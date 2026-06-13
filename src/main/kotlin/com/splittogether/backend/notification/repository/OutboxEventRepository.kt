package com.splittogether.backend.notification.repository

import com.splittogether.backend.notification.entity.OutboxEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {

    @Query(
        """
        SELECT e FROM OutboxEvent e
        WHERE e.processedAt IS NULL AND e.failedAt IS NULL AND e.nextAttemptAt <= :now
        ORDER BY e.id
        """
    )
    fun findProcessable(@Param("now") now: Instant, pageable: Pageable): List<OutboxEvent>
}
