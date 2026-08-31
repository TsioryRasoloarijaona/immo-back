package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.AutorisationVersementData;
import com.tsix.immocrm.dto.dossier.AutorisationVersementData.ClientAutorisation;
import com.tsix.immocrm.dto.dossier.AutorisationVersementData.Payeur;
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
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Autorisation d'Encaissement" document (see
 * {@code AutorisationVersementPdfRenderer} for the PDF rendering) — modeled on the legacy
 * Stimulsoft template {@code instructions/templates/autorisation_virement/ComSaleDocumentAutorisationVersement.yes}.
 * Scoped to one règlement ({@code StlSettlement}) of a dossier, not one échéance — same per-item
 * scope as {@link RecuReglementService}. See {@link AutorisationVersementData}'s javadoc for why
 * this was corrected from an initial échéance-scoped version: the fields that actually make the
 * document useful (compte, mode de versement, tiers payeur) live almost exclusively on
 * {@code StlSettlement} in real data, not on the underlying {@code ComDocument}.
 * <p>
 * "Responsable Commercial" comes from {@code ComFolder.CfgCommercialId}, not the legacy
 * {@code AdmWebSessions} (logged-in desktop user) — same REST-API substitution already applied to
 * every other document in this package (see {@code ActeReservationService}/
 * {@code FicheDossierVenteService}).
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
        // Même chemin confirmé (StkItem.CfgProjectId n'est jamais peuplé) que RecuReglementService/
        // FicheTropPercuService/FicheBancaireService : StkItem -> StkHierarchy (tranche) -> CfgProject.
        CfgTranche tranche = article != null ? article.getStkHierarchy() : null;
        CfgProject projet = tranche != null ? tranche.getCfgProject() : null;
        CfgCompany entreprise = projet != null ? projet.getCfgCompany() : null;

        List<ClientAutorisation> clients = new ArrayList<>();
        if (folder.getCfgCustomer() != null) {
            clients.add(toClient(folder.getCfgCustomer()));
        }
        for (ComFolderCustomer coAcquereur : folderCustomerRepository.findByComFolderId(dossierId)) {
            if (coAcquereur.getCfgTier() != null) {
                clients.add(toClient(coAcquereur.getCfgTier()));
            }
        }

        StlAccount compte = settlement.getStlAccount();
        String rib = compte != null ? rib(compte) : null;
        String banqueNom = compte != null && compte.getCfgBank() != null ? TierNomFormatter.nom(compte.getCfgBank()) : null;

        CfgTier commercial = folder.getCfgCommercial();
        CfgTier payeurTier = settlement.getTier();

        return new AutorisationVersementData(
                folder.getCode(),
                settlement.getCode(),
                tranche != null ? tranche.getDescription() : null,
                article != null ? article.getCode() : null,
                folder.getPrice(),
                settlement.getStlSettlementType() != null ? settlement.getStlSettlementType().getDescription() : null,
                clients,
                settlement.getAmount(),
                // Pas d'équivalent direct de ComDocument.PaymentAuthorizationDate sur StlSettlement
                // (confirmé, aucune colonne "PaymentAuthorizationDate"/analogue) — DocDate (date de
                // saisie du règlement) est le meilleur repli, même sémantique "date d'émission" que
                // ChampDateEncaissement.DATE_PIECE.
                settlement.getDocDate(),
                rib,
                banqueNom,
                tranche != null ? tranche.getWithdrawalDelay() : null,
                commercial != null && commercial.getAdmTierTitle() != null ? commercial.getAdmTierTitle().getCode() : null,
                TierNomFormatter.nom(commercial),
                entreprise != null ? entreprise.getDescription() : null,
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

    // Même format que RecuReglementService.pieceIdentite ("{type} N° {numéro}") — dupliqué plutôt
    // que partagé, même précédent que le reste du package (chaque document a sa propre variante
    // minime : ici sans le "N° " préfixé quand le type est absent).
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

    /**
     * RIB = BankCode + AgencyCode + AccountNumber + AccountKey, espace-séparés. {@code
     * StlAccountView.RIB} (legacy) est une colonne *calculée* de la vue — pas de colonne de base
     * du même nom (confirmé via {@code INFORMATION_SCHEMA.COLUMNS}) — donc reconstruite ici à
     * partir des 4 colonnes réelles plutôt que mappée telle quelle.
     */
    private static String rib(StlAccount compte) {
        return vide(compte.getBankCode()) + " " + vide(compte.getAgencyCode()) + " "
                + vide(compte.getAccountNumber()) + " " + vide(compte.getAccountKey());
    }

    private static String vide(String value) {
        return value != null ? value : "";
    }
}
