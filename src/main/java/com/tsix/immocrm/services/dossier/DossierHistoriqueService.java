package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.HistoriqueEvenementDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * {@code GET /api/dossiers/{id}/historique}: a single, chronological, traceable timeline merging
 * every kind of event that happens on a Dossier — status changes (with note), échéances,
 * règlements, pièces jointes. Each source is a distinct table (there's no single unified audit
 * table for this in the schema, aside from the generic-but-technical {@code AdmWebHistorics}
 * field-diff log, which this deliberately avoids in favor of business-readable events); merged
 * and sorted in memory since a dossier's total event count stays small, then paginated like every
 * other list endpoint.
 */
@Service
@Transactional(readOnly = true)
public class DossierHistoriqueService {

    private final ComFolderRepository folderRepository;
    private final StrFolderStatusHistoryRepository statusHistoryRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final AdmAttachedFileSummaryRepository attachedFileSummaryRepository;

    public DossierHistoriqueService(ComFolderRepository folderRepository,
                                     StrFolderStatusHistoryRepository statusHistoryRepository,
                                     ComDocumentRepository documentRepository,
                                     StlSettlementLineRepository settlementLineRepository,
                                     AdmAttachedFileSummaryRepository attachedFileSummaryRepository) {
        this.folderRepository = folderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.attachedFileSummaryRepository = attachedFileSummaryRepository;
    }

    public Page<HistoriqueEvenementDto> findHistorique(UUID dossierId, Pageable pageable) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }

        List<HistoriqueEvenementDto> evenements = new ArrayList<>();
        statusHistoryRepository.findByComFolderIdOrderByDocDateAsc(dossierId).forEach(h -> evenements.add(toStatutEvent(h)));
        documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId).forEach(d -> evenements.add(toEcheanceEvent(d)));
        settlementLineRepository.findByDossierIdOrderByEcheanceDateAsc(dossierId).forEach(l -> evenements.add(toReglementEvent(l)));
        attachedFileSummaryRepository.findByDossierId(dossierId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .forEach(a -> evenements.add(toPieceJointeEvent(a)));

        evenements.sort(Comparator.comparing(HistoriqueEvenementDto::date,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        int debut = Math.min((int) pageable.getOffset(), evenements.size());
        int fin = Math.min(debut + pageable.getPageSize(), evenements.size());
        return new PageImpl<>(evenements.subList(debut, fin), pageable, evenements.size());
    }

    private static HistoriqueEvenementDto toStatutEvent(StrFolderStatusHistory h) {
        String statut = h.getStrFolderStatus() != null ? h.getStrFolderStatus().getCode() : "?";
        String description = "Statut changé vers " + statut;
        if (h.getDescription() != null && !h.getDescription().isBlank()) {
            description += " — " + h.getDescription();
        }
        return new HistoriqueEvenementDto(h.getDocDate(), HistoriqueEvenementDto.STATUT, null, description);
    }

    private static HistoriqueEvenementDto toEcheanceEvent(ComDocument d) {
        String categorie = d.getComSettlementCategory() != null ? d.getComSettlementCategory().getCode() : "Échéance";
        String description = "Échéance " + categorie + " créée : " + montant(d.getAmount());
        if (d.getDescription() != null && !d.getDescription().isBlank()) {
            description += " (" + d.getDescription() + ")";
        }
        return new HistoriqueEvenementDto(
                d.getUserCreateDateTime() != null ? d.getUserCreateDateTime() : d.getDocDate(),
                HistoriqueEvenementDto.ECHEANCE, d.getUserCreate(), description);
    }

    private static HistoriqueEvenementDto toReglementEvent(StlSettlementLine l) {
        StlSettlement s = l.getStlSettlement();
        String description = "Règlement reçu : " + montant(l.getAmount());
        if (s != null && s.getDescription() != null && !s.getDescription().isBlank()) {
            description += " — " + s.getDescription();
        }
        return new HistoriqueEvenementDto(
                s != null && s.getUserCreateDateTime() != null ? s.getUserCreateDateTime()
                        : s != null ? s.getSettlementDate() : null,
                HistoriqueEvenementDto.REGLEMENT, s != null ? s.getUserCreate() : null, description);
    }

    private static HistoriqueEvenementDto toPieceJointeEvent(AdmAttachedFileSummary a) {
        String description = "Pièce jointe ajoutée"
                + (a.getDescription() != null && !a.getDescription().isBlank() ? " : " + a.getDescription() : "");
        return new HistoriqueEvenementDto(a.getUserCreateDateTime(), HistoriqueEvenementDto.PIECE_JOINTE,
                a.getUserCreate(), description);
    }

    private static String montant(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP) + " DH";
    }
}
