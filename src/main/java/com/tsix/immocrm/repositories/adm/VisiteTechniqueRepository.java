package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Visites techniques : {@code AdmEvent} rattachés à un dossier ({@code ComFolderId}) et à sa tranche. */
public interface VisiteTechniqueRepository extends JpaRepository<AdmEvent, UUID> {

    List<AdmEvent> findByCfgTrancheIdAndComFolderIdIsNotNullAndStartHourGreaterThanEqualAndStartHourLessThanOrderByStartHourAsc(
            UUID trancheId, LocalDateTime debut, LocalDateTime fin);

    long countByCfgTrancheIdAndComFolderIdIsNotNullAndStartHourGreaterThanEqualAndStartHourLessThan(
            UUID trancheId, LocalDateTime debut, LocalDateTime fin);
}
