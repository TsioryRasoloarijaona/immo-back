package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNullElse;

/**
 * Seul remplissage d'une ligne d'échéancier ({@code ComDocument}) : schéma initial, ajout et
 * modification écrivent les colonnes qu'OMRAN écrit. Voir {@code FEATURE-interop-omran-ecritures.md} §4.
 */
@Component
public class LigneEcheancierOmran {

    /** TVA par défaut d'une ligne d'OMRAN quand la catégorie n'en porte pas ({@code ComDocument.cs:47}). */
    static final BigDecimal TVA_PAR_DEFAUT = new BigDecimal("0.2");

    private final ReferencesOmran references;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public LigneEcheancierOmran(ReferencesOmran references, UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.references = references;
        this.utilisateurAffiche = utilisateurAffiche;
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
    }

    public void signerModification(ComDocument document) {
        document.setUserUpdate(utilisateurAffiche.courant().nom());
        document.setUserUpdateDateTime(LocalDateTime.now(clock));
    }

    /**
     * Colonnes dérivées de la catégorie, du dossier et du montant ; à rappeler après toute modification.
     * {@code DocDate} ramenée à minuit, {@code AmountHT} non arrondi (échelle 6).
     */
    public void completer(ComDocument document, ComFolder folder) {
        ComSettlementCategory categorie = document.getComSettlementCategory();
        BigDecimal tva = categorie != null && categorie.getVatRatio() != null ? categorie.getVatRatio() : TVA_PAR_DEFAUT;
        BigDecimal montant = requireNonNullElse(document.getAmount(), BigDecimal.ZERO);

        document.setCfgCompany(SocieteDossier.de(folder));
        document.setAdmModuleId(references.moduleCommercial());
        document.setVATRatio(tva);
        document.setAmountTTC(montant);
        document.setAmountHT(montant.divide(BigDecimal.ONE.add(tva), 6, RoundingMode.HALF_UP));
        document.setDiscountRatio(BigDecimal.ZERO);
        if (document.getExcludeFromPrice() == null) {
            document.setExcludeFromPrice(categorie != null && Boolean.TRUE.equals(categorie.getExcludeFromPrice()));
        }
        if (document.getDocDate() != null) {
            document.setDocDate(document.getDocDate().toLocalDate().atStartOfDay());
        }
        document.setCode(requireNonNullElse(document.getCode(), ""));
        document.setNotes(requireNonNullElse(document.getNotes(), ""));
        document.setQuittanceCode(requireNonNullElse(document.getQuittanceCode(), ""));
    }
}
