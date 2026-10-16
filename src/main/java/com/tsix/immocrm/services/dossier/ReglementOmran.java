package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.requireNonNullElse;

/**
 * Colonnes qu'OMRAN écrit sur un règlement de vente et que nos écrans ne saisissent pas
 * ({@code FEATURE-interop-omran-ecritures.md} §5). Montant, dates, tiers et type sont posés avant l'appel.
 */
@Component
public class ReglementOmran {

    static final String CATEGORIE = "None";
    static final String NON_DEFINI = "Non définie";
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int LONGUEUR_DESCRIPTION = 255;

    private final ReferencesOmran references;

    public ReglementOmran(ReferencesOmran references) {
        this.references = references;
    }

    public void completer(StlSettlement reglement, ComFolder folder) {
        boolean credit = !Boolean.FALSE.equals(reglement.getIsCredit());
        reglement.setAdmModuleId(references.moduleCommercial());
        reglement.setStrEntityId(references.entiteReglement(credit));
        reglement.setStlCurrencyId(references.deviseParDefaut());
        reglement.setStlCurrencyAffectationId(reglement.getStlCurrencyId());
        reglement.setConvertValue(BigDecimal.ONE);
        reglement.setConvertUnit(1);
        reglement.setAmountBase(reglement.getAmount());
        reglement.setAmountBaseConverted(reglement.getAmount());
        reglement.setAmountRS(BigDecimal.ZERO);
        reglement.setRsRatio(BigDecimal.ZERO);
        reglement.setAmountStamp(BigDecimal.ZERO);
        reglement.setRSAmountSettled(false);
        reglement.setIsAccounted(false);
        reglement.setIsDcoumentDeliveredToCenter(false);
        reglement.setCategory(CATEGORIE);
        reglement.setItemNumber(NON_DEFINI);
        reglement.setItemDescription(NON_DEFINI);
        reglement.setCfgCompany(SocieteDossier.de(folder));
        if (reglement.getEcheanceDate() == null) {
            reglement.setEcheanceDate(reglement.getDocDate());
        }
        reglement.setSlipCode(requireNonNullElse(reglement.getSlipCode(), ""));
        reglement.setRSSheetCode(requireNonNullElse(reglement.getRSSheetCode(), ""));
        reglement.setReceiptCode(requireNonNullElse(reglement.getReceiptCode(), ""));
        reglement.setPropositionSheetCode(requireNonNullElse(reglement.getPropositionSheetCode(), ""));
        reglement.setDescription(description(reglement));
    }

    /** {@code <type> [<code banque>] [N° <référence>] du jj/mm/aaaa} ({@code Yes.Stl.Commun/Settlement.cs:795-816}). */
    static String description(StlSettlement reglement) {
        StringBuilder texte = new StringBuilder();
        StlSettlementType type = reglement.getStlSettlementType();
        if (type != null && type.getDescription() != null) {
            texte.append(type.getDescription());
        }
        CfgTier banque = reglement.getCfgBank();
        if (banque != null && banque.getCode() != null) {
            texte.append(' ').append(banque.getCode());
        }
        if (StringUtils.hasText(reglement.getCode())) {
            texte.append(" N° ").append(reglement.getCode());
        }
        if (reglement.getDocDate() != null) {
            texte.append(" du ").append(reglement.getDocDate().format(JJ_MM_AAAA));
        }
        String resultat = texte.toString();
        return resultat.length() > LONGUEUR_DESCRIPTION ? resultat.substring(0, LONGUEUR_DESCRIPTION) : resultat;
    }
}
