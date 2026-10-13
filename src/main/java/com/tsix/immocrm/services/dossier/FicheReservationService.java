package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.dto.dossier.FicheReservationData;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FicheReservationService {

    private final ComFolderRepository folderRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public FicheReservationService(ComFolderRepository folderRepository,
                                    StlSettlementLineRepository settlementLineRepository,
                                    UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.folderRepository = folderRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /**
     * Écrit : la première impression pose {@code ContractPrintDate}, comme {@code BeforeRunReport} d'OMRAN
     * ({@code Yes.Com.Front.Sales/Sale.cs:1006-1017}) ; une réimpression ne la change pas.
     */
    @Transactional
    public FicheReservationData collect(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
        if (folder.getContractPrintDate() == null) {
            LocalDateTime maintenant = LocalDateTime.now(clock);
            folder.setContractPrintDate(maintenant);
            folder.setUserUpdate(utilisateurAffiche.courant().nom());
            folder.setUserUpdateDateTime(maintenant);
            folderRepository.save(folder);
        }

        CfgTier client = folder.getCfgCustomer();
        StkItem article = folder.getStkItem();
        // Projet via la tranche : StkItem.CfgProjectId n'est jamais peuplé.
        CfgProject projet = Champs.de(article, StkItem::getStkHierarchy, CfgTranche::getCfgProject);

        StlSettlementLine reservationLine = findReservationLine(dossierId);
        StlSettlement reservation = Champs.de(reservationLine, StlSettlementLine::getStlSettlement);

        return new FicheReservationData(
                folder.getCode(),
                Champs.de(client, FicheReservationService::clientNom),
                Champs.de(client, FicheReservationService::clientAdresse),
                Champs.de(client, CfgTier::getIDNumber),
                Champs.de(article, StkItem::getStkBuilding, StkBuilding::getCode),
                Champs.de(article, StkItem::getCode),
                Champs.de(article, StkItem::getDescription),
                Champs.de(article, StkItem::getStkFloor, StkFloor::getDescription),
                Champs.de(article, StkItem::getArea),
                Champs.de(projet, CfgProject::getDescription),
                Champs.de(projet, FicheReservationService::projetAdresse),
                Champs.de(projet, CfgProject::getCfgCompany, CfgCompany::getDescription),
                Champs.de(reservation, StlSettlement::getDescription),
                Champs.de(reservation, StlSettlement::getAmount),
                Champs.de(reservation, StlSettlement::getTier, FicheReservationService::clientNom),
                folder.getPrice(),
                LocalDate.now(clock)
        );
    }

    /** Le règlement non annulé de l'échéance AVANCE, à défaut le plus ancien du dossier. */
    private StlSettlementLine findReservationLine(UUID dossierId) {
        return premiereQuiCompte(settlementLineRepository.findAvanceLinesByDossierIdOrderByDateAsc(dossierId))
                .or(() -> premiereQuiCompte(settlementLineRepository.findByDossierIdOrderByEcheanceDateAsc(dossierId)))
                .orElse(null);
    }

    private static Optional<StlSettlementLine> premiereQuiCompte(List<StlSettlementLine> lignes) {
        return lignes.stream().filter(StatutReglement::compteDansLesTotaux).findFirst();
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

    private static String projetAdresse(CfgProject projet) {
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
