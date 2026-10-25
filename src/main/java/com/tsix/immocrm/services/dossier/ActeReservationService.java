package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.ActeReservationData.LignePaiement;
import com.tsix.immocrm.dto.dossier.ActeReservationData.SousArticle;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/** « Acte de réservation » ; lit les échéances AVANCE / AUTO-FINANCEMENT générées à la création du dossier. */
@Service
@Transactional(readOnly = true)
public class ActeReservationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ComFolderRepository folderRepository;
    private final StkItemRepository itemRepository;
    private final ComDocumentRepository documentRepository;
    private final Clock clock;

    public ActeReservationService(ComFolderRepository folderRepository, StkItemRepository itemRepository,
                                   ComDocumentRepository documentRepository, Clock clock) {
        this.folderRepository = folderRepository;
        this.itemRepository = itemRepository;
        this.documentRepository = documentRepository;
        this.clock = clock;
    }

    public ActeReservationData collect(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        CfgTier client = folder.getCfgCustomer();
        StkItem article = folder.getStkItem();
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        // Projet via la tranche : StkItem.CfgProjectId n'est jamais peuplé.
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        BigDecimal prixVente = folder.getPrice();

        List<SousArticle> sousArticles = article == null ? List.of() :
                itemRepository.findByComFolderId(dossierId).stream()
                        .filter(it -> !it.getId().equals(article.getId()))
                        .map(it -> new SousArticle(
                                Champs.de(it, StkItem::getStkItemCategory, StkItemCategory::getDescription),
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
                Champs.de(client, CfgTier::getIDNumber),
                client != null ? adresse(client.getAddress(), client.getAddress2(), client.getAddress3(), client.getCity()) : null,
                telephone(client),
                Champs.de(projet, CfgProject::getDescription),
                Champs.de(projet, p -> adresse(p.getAddress(), p.getAddress2(), p.getAddress3(), p.getCity())),
                Champs.de(projet, CfgProject::getCfgCompany, CfgCompany::getDescription),
                article != null && article.getStkKind() != null && "TER".equalsIgnoreCase(article.getStkKind().getCode()),
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(article, StkItem::getCode),
                Champs.de(article, StkItem::getStkBuilding, StkBuilding::getCode),
                Champs.de(article, StkItem::getStkFloor, StkFloor::getDescription),
                Champs.de(article, StkItem::getArea),
                sousArticles,
                prixVente,
                avanceMontant,
                pourcentage(avanceMontant, prixVente),
                lignesSolde,
                Champs.de(tranche, CfgTranche::getCancellationPenalityPercent),
                Champs.de(tranche, CfgTranche::getArticleTransferPenality),
                Champs.de(tranche, CfgTranche::getSliceTransferPenality),
                Champs.de(tranche, CfgTranche::getCustomerTransferPenality),
                Champs.de(tranche, CfgTranche::getWarrantyDelay),
                Champs.de(tranche, CfgTranche::getSyndicAmount),
                Champs.de(tranche, CfgTranche::getSyndicPeriod),
                requireNonNullElse(Champs.de(tranche, CfgTranche::getCommerceTribunal), "Casablanca"),
                TierNomFormatter.nom(folder.getCfgCommercial()),
                LocalDate.now(clock)
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

    public static String titre(CfgTier client) {
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
        return TierNomFormatter.nom(tier);
    }

    private static String telephone(CfgTier client) {
        if (client == null) {
            return null;
        }
        return client.getMobile() != null ? client.getMobile() : client.getPhone();
    }

    public static String adresse(String a1, String a2, String a3, String ville) {
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
