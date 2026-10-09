package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.HistoriqueEvenementDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Objects.requireNonNullElse;

/**
 * Chronologie d'un dossier fusionnant statuts, échéances, règlements et pièces jointes (pas
 * {@code AdmWebHistorics}, trop technique) ; triée et paginée en mémoire, le volume par dossier est faible.
 */
@Service
@Transactional(readOnly = true)
public class DossierHistoriqueService {

    private final ComFolderRepository folderRepository;
    private final StrFolderStatusHistoryRepository statusHistoryRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final AdmAttachedFileSummaryRepository attachedFileSummaryRepository;
    private final AuteurStatutDossier auteurStatut;

    public DossierHistoriqueService(ComFolderRepository folderRepository,
                                     StrFolderStatusHistoryRepository statusHistoryRepository,
                                     ComDocumentRepository documentRepository,
                                     StlSettlementLineRepository settlementLineRepository,
                                     AdmAttachedFileSummaryRepository attachedFileSummaryRepository,
                                     AuteurStatutDossier auteurStatut) {
        this.auteurStatut = auteurStatut;
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
        List<StrFolderStatusHistory> statuts = statusHistoryRepository.findByComFolderIdOrderByDocDateAsc(dossierId);
        Function<StrFolderStatusHistory, String> auteur = auteurStatut.resoudre(statuts);
        statuts.forEach(h -> evenements.add(toStatutEvent(h, auteur.apply(h))));
        documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId).forEach(d -> evenements.add(toEcheanceEvent(d)));
        settlementLineRepository.findByDossierIdOrderByEcheanceDateAsc(dossierId).forEach(l -> evenements.add(toReglementEvent(l)));
        attachedFileSummaryRepository.findByDossierId(dossierId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .forEach(a -> evenements.add(toPieceJointeEvent(a)));

        // Plus récent d'abord, non datés en dernier. Pas `nullsLast(naturalOrder()).reversed()` :
        // reversed() inverse aussi la place des null, qui passeraient en tête.
        evenements.sort(Comparator.comparing(HistoriqueEvenementDto::date,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int debut = Math.min((int) pageable.getOffset(), evenements.size());
        int fin = Math.min(debut + pageable.getPageSize(), evenements.size());
        return new PageImpl<>(evenements.subList(debut, fin), pageable, evenements.size());
    }

    private static HistoriqueEvenementDto toStatutEvent(StrFolderStatusHistory h, String auteur) {
        String statut = h.getStrFolderStatus() != null ? h.getStrFolderStatus().getCode() : "?";
        String description = "Statut changé vers " + statut;
        if (h.getDescription() != null && !h.getDescription().isBlank()) {
            description += " — " + h.getDescription();
        }
        return new HistoriqueEvenementDto(h.getDocDate(), HistoriqueEvenementDto.STATUT, auteur, description);
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
        // Date de saisie, à défaut d'encaissement (les règlements legacy n'ont pas de
        // UserCreateDateTime) ; les deux peuvent manquer : événement non daté.
        LocalDateTime date = Champs.de(s, StlSettlement::getUserCreateDateTime);
        if (date == null) {
            date = Champs.de(s, StlSettlement::getSettlementDate);
        }
        return new HistoriqueEvenementDto(date, HistoriqueEvenementDto.REGLEMENT,
                Champs.de(s, StlSettlement::getUserCreate), description);
    }

    private static HistoriqueEvenementDto toPieceJointeEvent(AdmAttachedFileSummary a) {
        String description = "Pièce jointe ajoutée"
                + (a.getDescription() != null && !a.getDescription().isBlank() ? " : " + a.getDescription() : "");
        return new HistoriqueEvenementDto(a.getUserCreateDateTime(), HistoriqueEvenementDto.PIECE_JOINTE,
                a.getUserCreate(), description);
    }

    private static String montant(BigDecimal value) {
        return requireNonNullElse(value, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP) + " DH";
    }
}
