package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.RoleCategorieEcheance;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Objects.requireNonNullElse;

/**
 * Seul remplissage d'une ligne d'échéancier ({@code ComDocument}) : schéma initial, ajout, périodicité et
 * modification écrivent les colonnes qu'OMRAN écrit. Voir {@code FEATURE-interop-omran-ecritures.md} §4 et
 * {@code FEATURE-vente-echeancier.md} §1.3-1.4.
 */
@Component
public class LigneEcheancierOmran {

    /** TVA par défaut d'une ligne d'OMRAN quand la catégorie n'en porte pas ({@code ComDocument.cs:47}). */
    static final BigDecimal TVA_PAR_DEFAUT = new BigDecimal("0.2");

    /** {@code Sequence.GetNewValue("Yes.Com.SaleWithdrawal")} : la ligne {@code AdmSequence} « Désistement ». */
    public static final String SEQUENCE_QUITTANCE = "Désistement";
    private static final int LARGEUR_ANNEE = 2;
    private static final int LARGEUR_NUMERO = 5;

    private final ReferencesOmran references;
    private final UtilisateurAffiche utilisateurAffiche;
    private final CategoriesParDefaut categoriesParDefaut;
    private final GenerateurCodeSequence generateurCode;
    private final Clock clock;

    public LigneEcheancierOmran(ReferencesOmran references, UtilisateurAffiche utilisateurAffiche,
                                CategoriesParDefaut categoriesParDefaut, GenerateurCodeSequence generateurCode,
                                Clock clock) {
        this.references = references;
        this.utilisateurAffiche = utilisateurAffiche;
        this.categoriesParDefaut = categoriesParDefaut;
        this.generateurCode = generateurCode;
        this.clock = clock;
    }

    /** Nouvelle ligne : signée en création et en modification (GEN0016). */
    public void signerCreation(ComDocument document) {
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        document.setUserCreate(auteur);
        document.setUserCreateDateTime(maintenant);
        document.setUserUpdate(auteur);
        document.setUserUpdateDateTime(maintenant);
        document.setNouvelleLigne(true);
    }

    public void signerModification(ComDocument document) {
        document.setUserUpdate(utilisateurAffiche.courant().nom());
        document.setUserUpdateDateTime(LocalDateTime.now(clock));
    }

    /**
     * Colonnes dérivées de la catégorie, du dossier et du montant ; à rappeler après toute modification.
     * {@code DocDate} ramenée à minuit ; {@code AmountHT} = montant / ((1 − remise) × (1 + TVA)), non arrondi
     * (échelle 6) : la remise ancien client ne change pas le montant dû ({@code ComDocument.cs:DiscountRatio}).
     */
    public void completer(ComDocument document, ComFolder folder) {
        ComSettlementCategory categorie = document.getComSettlementCategory();
        BigDecimal tva = categorie != null && categorie.getVatRatio() != null ? categorie.getVatRatio() : TVA_PAR_DEFAUT;
        BigDecimal montant = requireNonNullElse(document.getAmount(), BigDecimal.ZERO);
        if (document.getDiscountRatio() == null) {
            document.setDiscountRatio(remiseParDefaut(categorie, folder, document.isNouvelleLigne()));
        }
        BigDecimal remise = document.getDiscountRatio();

        document.setCfgCompany(SocieteDossier.de(folder));
        document.setAdmModuleId(references.moduleCommercial());
        document.setVATRatio(tva);
        document.setAmountTTC(montant);
        document.setAmountHT(montantHT(montant, remise, tva));
        if (document.getExcludeFromPrice() == null) {
            document.setExcludeFromPrice(categorie != null && Boolean.TRUE.equals(categorie.getExcludeFromPrice()));
        }
        if (document.getDocDate() != null) {
            document.setDocDate(document.getDocDate().toLocalDate().atStartOfDay());
        }
        if (document.isNouvelleLigne()) {
            reporterAuDebutDesFinalisations(document, folder);
        }
        document.setCode(requireNonNullElse(document.getCode(), ""));
        document.setNotes(requireNonNullElse(document.getNotes(), ""));
        document.setQuittanceCode(requireNonNullElse(document.getQuittanceCode(), ""));
        numeroterQuittance(document);
    }

    /** {@code ComDocument.cs:Amount} : HT = montant / ((1 − remise) × (1 + TVA)), 0 pour un montant nul ou une remise totale. */
    public static BigDecimal montantHT(BigDecimal montant, BigDecimal remise, BigDecimal tva) {
        if (montant.signum() == 0 || remise.compareTo(BigDecimal.ONE) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal diviseur = BigDecimal.ONE.subtract(remise).multiply(BigDecimal.ONE.add(tva));
        return montant.divide(diviseur, 6, RoundingMode.HALF_UP);
    }

    /** Début des finalisations de la tranche du dossier, {@code null} s'il n'est pas renseigné. */
    public static LocalDateTime debutDesFinalisations(ComFolder folder) {
        CfgTranche tranche = trancheDe(folder);
        return Champs.de(tranche, CfgTranche::getStartDateFinalisation);
    }

    static CfgTranche trancheDe(ComFolder folder) {
        return folder != null ? Champs.de(folder.getStkItem(), StkItem::getStkHierarchy) : null;
    }

    /** {@code SetDefaluts} : une catégorie « basée sur la date des finalisations » n'est jamais datée avant elle. */
    private static void reporterAuDebutDesFinalisations(ComDocument document, ComFolder folder) {
        ComSettlementCategory categorie = document.getComSettlementCategory();
        LocalDateTime debut = debutDesFinalisations(folder);
        if (categorie == null || !Boolean.TRUE.equals(categorie.getIsConfermentBasedDate()) || debut == null) {
            return;
        }
        LocalDateTime minuit = debut.toLocalDate().atStartOfDay();
        if (document.getDocDate() == null || document.getDocDate().isBefore(minuit)) {
            document.setDocDate(minuit);
        }
    }

    /**
     * Remise ancien client des frais de la tranche ({@code SetDefaluts}) ; celle de la pré-réservation n'est
     * posée qu'à la génération ({@code GenerateInitialDocuments}), donc sur une ligne neuve.
     */
    private BigDecimal remiseParDefaut(ComSettlementCategory categorie, ComFolder folder, boolean nouvelle) {
        CfgTranche tranche = trancheDe(folder);
        if (folder == null || !Boolean.TRUE.equals(folder.getIsOldCustomer()) || tranche == null) {
            return BigDecimal.ZERO;
        }
        Optional<RoleCategorieEcheance> role = categoriesParDefaut.role(categorie);
        if (role.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal remise = switch (role.get()) {
            case FRAIS_DOSSIER -> tranche.getDefaultExpenseAmountDiscount();
            case FRAIS_SYNDIC -> tranche.getSyndicAmountDiscount();
            case AUTRES_FRAIS -> tranche.getOtherExpenseAmountDiscount();
            case PRE_RESERVATION -> nouvelle ? tranche.getBeforeReservationAmountDiscount() : null;
            default -> null;
        };
        return requireNonNullElse(remise, BigDecimal.ZERO);
    }

    /** {@code ComDocument.cs:BeforeInsert} : une ligne de frais reçoit son numéro de quittance à l'insertion. */
    private void numeroterQuittance(ComDocument document) {
        ComSettlementCategory categorie = document.getComSettlementCategory();
        if (!document.isNouvelleLigne() || !document.getQuittanceCode().isEmpty() || categorie == null
                || !Boolean.TRUE.equals(categorie.getIsSaleExpense())) {
            return;
        }
        document.setQuittanceCode(generateurCode.suivant(SEQUENCE_QUITTANCE, LARGEUR_ANNEE, LARGEUR_NUMERO));
    }
}
