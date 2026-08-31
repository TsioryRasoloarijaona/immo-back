package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComActionMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ComActionMessageRepository extends JpaRepository<ComActionMessage, UUID>,
        JpaSpecificationExecutor<ComActionMessage> {

    List<ComActionMessage> findByComProspectionIdOrderByDocDateDesc(UUID prospectionId);

    Page<ComActionMessage> findByComProspectionIdOrderByDocDateDesc(UUID prospectionId, Pageable pageable);

    /**
     * {@code GET /api/hotline/recherche} — every interaction ({@code ComActionMessage}) logged
     * directly against a {@code CfgTier} (as opposed to one scoped to a single prospection or
     * dossier), most recent first.
     */
    Page<ComActionMessage> findByCfgTierIdOrderByDocDateDesc(UUID tierId, Pageable pageable);

    /**
     * {@code GET /api/calendrier/evenements} — every interaction that carries a calendar entry
     * ({@code AdmEvent}, created when the "Résultat" is a rendez-vous or a planned visit — see
     * {@code ProspectionService.RESULTATS_AVEC_DATE_PLANIFIEE}) whose {@code AdmEvent.StartHour}
     * falls within [debut, fin], earliest first.
     */
    List<ComActionMessage> findByAdmEventIsNotNullAndAdmEventStartHourBetweenOrderByAdmEventStartHourAsc(
            LocalDateTime debut, LocalDateTime fin);
}
