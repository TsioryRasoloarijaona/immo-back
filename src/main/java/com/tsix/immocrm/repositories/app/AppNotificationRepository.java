package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

    Page<AppNotification> findByAdmUserIdOrderByCreeLeDescIdAsc(UUID admUserId, Pageable pageable);

    long countByAdmUserIdAndLueLeIsNull(UUID admUserId);

    /** Entité par entité, jamais un UPDATE de masse : il n'émet aucun évènement Hibernate, la copie ne serait pas tenue. */
    List<AppNotification> findByAdmUserIdAndLueLeIsNull(UUID admUserId);

    Optional<AppNotification> findByIdAndAdmUserId(UUID id, UUID admUserId);
}
