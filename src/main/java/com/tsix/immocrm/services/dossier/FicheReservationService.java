package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.FicheReservationData;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Fiche de réservation" document (see
 * {@code FicheReservationPdfRenderer} / {@code FicheReservationDocxRenderer} for the actual PDF
 * and Word rendering).
 */
@Service
@Transactional(readOnly = true)
public class FicheReservationService {

    private final ComFolderRepository folderRepository;
    private final StlSettlementLineRepository settlementLineRepository;

    public FicheReservationService(ComFolderRepository folderRepository,
                                    StlSettlementLineRepository settlementLineRepository) {
        this.folderRepository = folderRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    public FicheReservationData collect(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        CfgTier client = folder.getCfgCustomer();
        StkItem article = folder.getStkItem();
        // StkItem.CfgProjectId is never populated in real data — the confirmed path is via the
        // article's tranche (StkHierarchyId -> CfgTranche.CfgProjectId). Same fix applied in
        // ActeReservationService and DossierService#genererCodeDossier.
        com.tsix.immocrm.entities.cfg.CfgProject projet = article != null && article.getStkHierarchy() != null
                ? article.getStkHierarchy().getCfgProject() : null;

        StlSettlementLine reservationLine = findReservationLine(dossierId);
        StlSettlement reservation = reservationLine != null ? reservationLine.getStlSettlement() : null;

        return new FicheReservationData(
                folder.getCode(),
                client != null ? clientNom(client) : null,
                client != null ? clientAdresse(client) : null,
                client != null ? client.getIDNumber() : null,
                article != null && article.getStkBuilding() != null ? article.getStkBuilding().getCode() : null,
                article != null ? article.getCode() : null,
                article != null ? article.getDescription() : null,
                article != null && article.getStkFloor() != null ? article.getStkFloor().getDescription() : null,
                article != null ? article.getArea() : null,
                projet != null ? projet.getDescription() : null,
                projet != null ? projetAdresse(projet) : null,
                reservation != null ? reservation.getDescription() : null,
                reservation != null ? reservation.getAmount() : null,
                reservation != null && reservation.getTier() != null ? clientNom(reservation.getTier()) : null,
                folder.getPrice(),
                LocalDate.now()
        );
    }

    /** Prefers the "AVANCE" échéance's règlement; falls back to the earliest règlement overall. */
    private StlSettlementLine findReservationLine(UUID dossierId) {
        List<StlSettlementLine> avance = settlementLineRepository.findAvanceLinesByDossierIdOrderByDateAsc(dossierId);
        if (!avance.isEmpty()) {
            return avance.get(0);
        }
        List<StlSettlementLine> all = settlementLineRepository.findByDossierIdOrderByEcheanceDateAsc(dossierId);
        return all.isEmpty() ? null : all.get(0);
    }

    private static String clientNom(CfgTier tier) {
        if (tier.getDescription() != null && !tier.getDescription().isBlank()) {
            return tier.getDescription();
        }
        String prenom = tier.getFirstName() != null ? tier.getFirstName() : "";
        String nom = tier.getLastName() != null ? tier.getLastName() : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? null : full;
    }

    private static String clientAdresse(CfgTier tier) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, tier.getAddress());
        appendPart(sb, tier.getAddress2());
        appendPart(sb, tier.getAddress3());
        appendPart(sb, tier.getCity());
        return sb.isEmpty() ? null : sb.toString();
    }

    private static String projetAdresse(com.tsix.immocrm.entities.cfg.CfgProject projet) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, projet.getAddress());
        appendPart(sb, projet.getAddress2());
        appendPart(sb, projet.getAddress3());
        appendPart(sb, projet.getCity());
        return sb.isEmpty() ? null : sb.toString();
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part != null && !part.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(part.trim());
        }
    }
}
