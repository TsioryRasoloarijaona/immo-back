package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.referentiel.ReferencesOmran;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNullElse;

/**
 * Seule méthode qui complète une échéance {@code ComDocument} comme OMRAN l'écrit : schéma initial,
 * ajout et modification passent tous par elle. Voir {@code FEATURE-interop-omran-ecritures.md} §4.
 */
@Component
public class LigneEcheancierOmran {

    /** Taux appliqué par OMRAN quand la catégorie n'en porte pas ({@code ComDocument.cs:47}). */
    static final BigDecimal TVA_PAR_DEFAUT = new BigDecimal("0.2");

    private final ReferencesOmran references;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public LigneEcheancierOmran(ReferencesOmran references, UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.references = references;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** Société du projet de l'article principal ; jamais {@code StkItem.CfgProjectId}, colonne morte. */
    public static CfgCompany societe(ComFolder folder) {
        CfgTranche tranche = Champs.de(folder, ComFolder::getStkItem, StkItem::getStkHierarchy);
        return Champs.de(tranche, CfgTranche::getCfgProject, CfgProject::getCfgCompany);
    }

    /** {@code DocDate} sans l'heure, comme {@code DateTime.Today} d'OMRAN. */
    public static LocalDateTime minuit(LocalDateTime date) {
        return date == null ? null : date.toLocalDate().atStartOfDay();
    }

    /** Ligne neuve : signée en création et en mise à jour (GEN0016), puis complétée. */
    public void completerCreation(ComDocument document) {
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        document.setUserCreate(auteur);
        document.setUserCreateDateTime(maintenant);
        document.setUserUpdate(auteur);
        document.setUserUpdateDateTime(maintenant);
        completer(document);
    }

    /** Ligne modifiée : seule la mise à jour est signée. */
    public void completerModification(ComDocument document) {
        document.setUserUpdate(utilisateurAffiche.courant().nom());
        document.setUserUpdateDateTime(LocalDateTime.now(clock));
        completer(document);
    }

    private void completer(ComDocument document) {
        ComSettlementCategory categorie = document.getComSettlementCategory();
        BigDecimal tva = requireNonNullElse(Champs.de(categorie, ComSettlementCategory::getVatRatio), TVA_PAR_DEFAUT);
        BigDecimal montant = requireNonNullElse(document.getAmount(), BigDecimal.ZERO);

        document.setCfgCompany(societe(document.getComFolder()));
        document.setAdmModuleId(references.moduleVente());
        document.setDocDate(minuit(document.getDocDate()));
        document.setVATRatio(tva);
        document.setAmountTTC(montant);
        // Non arrondi au centime : OMRAN garde les 6 décimales (100 000 / 1,2 = 83 333,333333).
        document.setAmountHT(montant.divide(BigDecimal.ONE.add(tva), 6, RoundingMode.HALF_UP));
        document.setDiscountRatio(requireNonNullElse(document.getDiscountRatio(), BigDecimal.ZERO));
        if (document.getExcludeFromPrice() == null) {
            document.setExcludeFromPrice(Boolean.TRUE.equals(
                    Champs.de(categorie, ComSettlementCategory::getExcludeFromPrice)));
        }
        document.setCode(requireNonNullElse(document.getCode(), ""));
        document.setNotes(requireNonNullElse(document.getNotes(), ""));
        document.setQuittanceCode(requireNonNullElse(document.getQuittanceCode(), ""));
    }
}
