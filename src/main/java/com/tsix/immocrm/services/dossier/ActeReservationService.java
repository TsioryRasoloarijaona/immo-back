package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.ActeReservationData.LignePaiement;
import com.tsix.immocrm.dto.dossier.ActeReservationData.SousArticle;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Acte de réservation" contract (see
 * {@code ActeReservationPdfRenderer} / {@code ActeReservationDocxRenderer} for the actual PDF
 * and Word rendering). Reuses the "AVANCE"/"AUTO-FINANCEMENT" échéancier lines auto-generated on
 * Dossier creation (see {@code DossierService#genererSchemaFinancement}).
 */
@Service
@Transactional(readOnly = true)
public class ActeReservationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ComFolderRepository folderRepository;
    private final StkItemRepository itemRepository;
    private final ComDocumentRepository documentRepository;

    public ActeReservationService(ComFolderRepository folderRepository, StkItemRepository itemRepository,
                                   ComDocumentRepository documentRepository) {
        this.folderRepository = folderRepository;
        this.itemRepository = itemRepository;
        this.documentRepository = documentRepository;
    }

    public ActeReservationData collect(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        CfgTier client = folder.getCfgCustomer();
        StkItem article = folder.getStkItem();
        CfgTranche tranche = article != null ? article.getStkHierarchy() : null;
        // StkItem.CfgProjectId is never populated in real data — confirmed path is via the
        // tranche (StkHierarchyId -> CfgTranche.CfgProjectId). Same fix in FicheReservationService.
        com.tsix.immocrm.entities.cfg.CfgProject projet = tranche != null ? tranche.getCfgProject() : null;
        BigDecimal prixVente = folder.getPrice();

        List<SousArticle> sousArticles = article == null ? List.of() :
                itemRepository.findByComFolderId(dossierId).stream()
                        .filter(it -> !it.getId().equals(article.getId()))
                        .map(it -> new SousArticle(
                                it.getStkItemCategory() != null ? it.getStkItemCategory().getDescription() : null,
                                it.getArea()))
                        .toList();

        List<ComDocument> echeances = documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId);

        BigDecimal avanceMontant = echeances.stream()
                .filter(ActeReservationService::isAvance)
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LignePaiement> lignesSolde = echeances.stream()
                .filter(d -> !isAvance(d) && !Boolean.TRUE.equals(d.getExcludeFromPrice()))
                .map(d -> new LignePaiement(
                        d.getAmount(),
                        pourcentage(d.getAmount(), prixVente),
                        d.getDocDate() != null ? d.getDocDate().toLocalDate() : null))
                .toList();

        return new ActeReservationData(
                folder.getCode(),
                titre(client),
                client != null ? nomClient(client) : null,
                client != null ? client.getIDNumber() : null,
                client != null ? adresse(client.getAddress(), client.getAddress2(), client.getAddress3(), client.getCity()) : null,
                client != null ? (client.getMobile() != null ? client.getMobile() : client.getPhone()) : null,
                projet != null ? projet.getDescription() : null,
                projet != null ? adresse(projet.getAddress(), projet.getAddress2(), projet.getAddress3(), projet.getCity()) : null,
                article != null && article.getStkItemCategory() != null ? article.getStkItemCategory().getDescription() : null,
                article != null ? article.getCode() : null,
                article != null && article.getStkBuilding() != null ? article.getStkBuilding().getCode() : null,
                article != null && article.getStkFloor() != null ? article.getStkFloor().getDescription() : null,
                article != null ? article.getArea() : null,
                sousArticles,
                prixVente,
                avanceMontant,
                pourcentage(avanceMontant, prixVente),
                lignesSolde,
                tranche != null ? tranche.getCancellationPenalityPercent() : null,
                tranche != null ? tranche.getArticleTransferPenality() : null,
                tranche != null ? tranche.getSliceTransferPenality() : null,
                tranche != null ? tranche.getCustomerTransferPenality() : null,
                tranche != null ? tranche.getWarrantyDelay() : null,
                tranche != null ? tranche.getSyndicAmount() : null,
                tranche != null ? tranche.getSyndicPeriod() : null,
                tranche != null && tranche.getCommerceTribunal() != null ? tranche.getCommerceTribunal() : "Casablanca",
                nomTier(folder.getCfgCommercial()),
                LocalDate.now()
        );
    }

    private static boolean isAvance(ComDocument d) {
        return d.getComSettlementCategory() != null && Boolean.TRUE.equals(d.getComSettlementCategory().getIsSaleAdvance());
    }

    private static BigDecimal pourcentage(BigDecimal montant, BigDecimal total) {
        if (montant == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return montant.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static String titre(CfgTier client) {
        if (client == null || client.getAdmSexType() == null || client.getAdmSexType().getCode() == null) {
            return "";
        }
        return switch (client.getAdmSexType().getCode()) {
            case "M" -> "Monsieur";
            case "F" -> "Madame";
            default -> "";
        };
    }

    private static String nomClient(CfgTier tier) {
        return nomTier(tier);
    }

    private static String nomTier(CfgTier tier) {
        if (tier == null) return null;
        if (tier.getDescription() != null && !tier.getDescription().isBlank()) {
            return tier.getDescription();
        }
        String prenom = tier.getFirstName() != null ? tier.getFirstName() : "";
        String nom = tier.getLastName() != null ? tier.getLastName() : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? null : full;
    }

    private static String adresse(String a1, String a2, String a3, String ville) {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{a1, a2, a3, ville}) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(part.trim());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
