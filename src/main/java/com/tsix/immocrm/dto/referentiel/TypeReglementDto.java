package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/**
 * {@code StlSettlementType} row — dropdown "Type" pour {@code CreerReglementRequest.typeId}, une
 * table distincte de {@code ComPaymentMethod} (voir {@code GET /api/referentiels/modes-paiement},
 * qui alimente {@code CreerDossierRequest.modePaieId} et n'a pas de lien avec un règlement).
 */
public record TypeReglementDto(
        UUID id,
        String code,
        String description,
        /**
         * {@code StlSettlementType.IsBankMandatory} — quand true, le formulaire doit demander une
         * banque ({@code CreerReglementRequest.banqueId}, voir {@code GET /api/referentiels/banques}) ;
         * quand false/null, le champ banque n'a pas de sens pour ce type de règlement (ex. ESPÈCES).
         */
        Boolean banqueObligatoire,
        /**
         * {@code StlSettlementType.IsReferenceMandatory} — quand true, le formulaire doit demander
         * une référence ({@code CreerReglementRequest.reference}, ex. n° de chèque) ; **enforcé
         * côté serveur** (400 si absente) contrairement à {@code banqueObligatoire} qui reste
         * purement indicatif. Confirmé sur données réelles : vrai pour CHÈQUE/CHEQUE
         * NOTAIRE/SYNDIC/BANQUE/CERTIFIÉ/EFFET (référence renseignée sur &gt;99% des lignes
         * réelles de ces types), faux pour VIREMENT/ESPÈCES/CMI/REGUL/VERSEMENT (référence
         * optionnelle bien que parfois renseignée).
         */
        Boolean referenceObligatoire
) {
}
