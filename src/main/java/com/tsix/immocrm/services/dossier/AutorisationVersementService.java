package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.AutorisationVersementData;
import com.tsix.immocrm.dto.dossier.AutorisationVersementData.ClientAutorisation;
import com.tsix.immocrm.dto.dossier.AutorisationVersementData.Payeur;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * « Autorisation d'Encaissement » d'un <b>règlement</b>, pas d'une échéance : voir
 * {@link AutorisationVersementData}. Responsable commercial = {@code ComFolder.CfgCommercialId},
 * pas l'utilisateur de bureau connecté ({@code AdmWebSessions}) du template legacy.
 */
@Service
@Transactional(readOnly = true)
public class AutorisationVersementService {

    private final ComFolderRepository folderRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final ComFolderCustomerRepository folderCustomerRepository;

    public AutorisationVersementService(ComFolderRepository folderRepository, StlSettlementRepository settlementRepository,
                                         StlSettlementLineRepository settlementLineRepository,
                                         ComFolderCustomerRepository folderCustomerRepository) {
        this.folderRepository = folderRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.folderCustomerRepository = folderCustomerRepository;
    }

    public AutorisationVersementData collect(UUID dossierId, UUID reglementId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
        StlSettlement settlement = settlementRepository.findById(reglementId)
                .orElseThrow(() -> new ResourceNotFoundException("Règlement introuvable: " + reglementId));
        if (settlementLineRepository.findBySettlementIdAndDossierId(reglementId, dossierId).isEmpty()) {
            throw new ResourceNotFoundException("Ce règlement n'est pas rattaché à ce dossier: " + reglementId);
        }

        StkItem article = folder.getStkItem();
        // Projet via la tranche : StkItem.CfgProjectId n'est jamais peuplé.
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgCompany entreprise = Champs.de(projet, CfgProject::getCfgCompany);

        List<ClientAutorisation> clients = new ArrayList<>();
        if (folder.getCfgCustomer() != null) {
            clients.add(toClient(folder.getCfgCustomer()));
        }
        folderCustomerRepository.findByComFolderId(dossierId).stream()
                .map(ComFolderCustomer::getCfgTier)
                .filter(Objects::nonNull)
                .map(AutorisationVersementService::toClient)
                .forEach(clients::add);

        StlAccount compte = settlement.getStlAccount();
        String rib = compte != null ? rib(compte) : null;
        String banqueNom = Champs.de(compte, StlAccount::getCfgBank, TierNomFormatter::nom);

        CfgTier commercial = folder.getCfgCommercial();
        CfgTier payeurTier = settlement.getTier();

        return new AutorisationVersementData(
                folder.getCode(),
                settlement.getCode(),
                Champs.de(tranche, CfgTranche::getDescription),
                Champs.de(article, StkItem::getCode),
                folder.getPrice(),
                settlement.getStlSettlementType() != null ? settlement.getStlSettlementType().getDescription() : null,
                clients,
                settlement.getAmount(),
                // StlSettlement n'a pas d'équivalent de ComDocument.PaymentAuthorizationDate : DocDate,
                // la date de saisie, en tient lieu.
                settlement.getDocDate(),
                rib,
                banqueNom,
                Champs.de(tranche, CfgTranche::getWithdrawalDelay),
                Champs.de(commercial, CfgTier::getAdmTierTitle, AdmTierTitle::getCode),
                TierNomFormatter.nom(commercial),
                Champs.de(entreprise, CfgCompany::getDescription),
                payeurTier != null ? toPayeur(payeurTier) : null
        );
    }

    private static ClientAutorisation toClient(CfgTier tier) {
        return new ClientAutorisation(
                tier.getAdmTierTitle() != null ? tier.getAdmTierTitle().getCode() : null,
                TierNomFormatter.nom(tier),
                pieceIdentite(tier)
        );
    }

    private static Payeur toPayeur(CfgTier tier) {
        return new Payeur(tier.getLastName(), tier.getFirstName(), pieceIdentite(tier));
    }

    // « {type}: {numéro} » — RecuReglementService.pieceIdentite, dupliquée, écrit « {type} N° {numéro} ».
    private static String pieceIdentite(CfgTier tier) {
        String type = tier.getAdmIdentityDocumentType() != null ? tier.getAdmIdentityDocumentType().getCode() : null;
        if (type == null && tier.getIDNumber() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (type != null) {
            sb.append(type);
        }
        if (tier.getIDNumber() != null) {
            if (!sb.isEmpty()) sb.append(": ");
            sb.append(tier.getIDNumber());
        }
        return sb.toString();
    }

    // StlAccountView.RIB est une colonne calculée de vue : reconstruit à partir des 4 colonnes réelles.
    private static String rib(StlAccount compte) {
        return vide(compte.getBankCode()) + " " + vide(compte.getAgencyCode()) + " "
                + vide(compte.getAccountNumber()) + " " + vide(compte.getAccountKey());
    }

    private static String vide(String value) {
        return value != null ? value : "";
    }
}
