package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Body for {@code POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements}.
 *
 * <p><b>La saisie est désormais complète et obligatoire.</b> Un règlement incomplet produisait un
 * document « Autorisation d'encaissement » troué : sans {@code typeId} le « Mode de versement »
 * sortait vide, sans {@code payeurId} le bloc « Versement effectué par » du cadre banque restait
 * blanc. Ces deux champs sont donc requis, comme {@code montant} et {@code dateEcheance}.
 *
 * <p>Deux dates distinctes, confirmées sur données réelles (voir {@code ChampDateEncaissement},
 * qui documente les mêmes colonnes pour {@code GET /api/encaissements}) — ne pas les confondre :
 * <ul>
 *   <li>{@code dateEcheance} → {@code StlSettlement.EcheanceDate} — "date d'échéance" du règlement,
 *       renseignée sur la quasi-totalité des règlements réels (&gt;99% des lignes) et quasi toujours
 *       égale à la date de saisie. <b>Requise</b> : le défaut silencieux à {@code now()} masquait
 *       une saisie incomplète.</li>
 *   <li>{@code dateReglement} → {@code StlSettlement.SettlementDate} — "date de règlement effectif"
 *       (encaissement réel du chèque, valeur bancaire...). <b>Refusée ici</b> : elle n'appartient
 *       qu'au passage à {@code POINTED}, via
 *       {@code PATCH /api/dossiers/{id}/reglements/{reglementId}/statut}. Sur 3 244 règlements
 *       réels, cette date n'est renseignée que sur les 14 lignes pointées et sur aucune des 3 218
 *       lignes en cours — l'accepter à la création casserait cet invariant.</li>
 * </ul>
 */
public record CreerReglementRequest(

        @Schema(description = "Montant du règlement. **Requis**, strictement positif, et au plus "
                + "égal au reste à payer de l'échéance (400 sinon — le message indique le reste "
                + "disponible).", example = "50000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal montant,

        @Schema(description = "Date d'échéance du règlement (`StlSettlement.EcheanceDate`). "
                + "**Requise** — il n'y a plus de valeur par défaut.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dateEcheance,

        @Schema(description = "Type de règlement (`StlSettlementType.PKey`) — voir "
                + "`GET /api/referentiels/types-reglement`. **Requis** : alimente le « Mode de "
                + "versement » de l'autorisation d'encaissement.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID typeId,

        @Schema(description = "Banque (`CfgTier.PKey`, type BAN) — voir `GET /api/referentiels/banques`. "
                + "**Requise si** le type choisi a `banqueObligatoire = true` (400 sinon) ; "
                + "facultative pour les autres types, typiquement ESPÈCES.")
        UUID banqueId,

        @Schema(description = "Référence du règlement (`StlSettlement.Code`) — n° de chèque, de "
                + "virement... **Requise si** le type choisi a `referenceObligatoire = true` "
                + "(400 sinon), c'est-à-dire pour CHÈQUE / CHEQUE NOTAIRE / SYNDIC / BANQUE / "
                + "CERTIFIÉ / EFFET ; libre pour ESPÈCES.", example = "CHQ-000123")
        String reference,

        @Schema(description = "Tiers ayant effectivement réglé (`StlSettlement.TierId`) — « réglé "
                + "par », peut différer de l'acquéreur principal (co-acquéreur, tiers payeur...). "
                + "**Requis** : alimente le bloc « Versement effectué par » de l'autorisation "
                + "d'encaissement. Confirmé renseigné sur >99% des règlements réels.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID payeurId,

        @Schema(description = "Commentaire libre. Facultatif.")
        String notes,

        @Schema(description = "Statut à la création. Seul `IN_WORK` est accepté (ou l'absence de "
                + "valeur, équivalente). **`POINTED` est refusé (400)** : pointer un règlement "
                + "exige une preuve de paiement, or aucune pièce jointe ne peut exister avant que "
                + "le règlement existe. Le parcours « saisir directement en pointé » s'obtient en "
                + "enchaînant trois appels : créer ici, déposer la preuve sur "
                + "`POST .../reglements/{reglementId}/pieces-jointes`, puis "
                + "`PATCH .../reglements/{reglementId}/statut`.",
                example = "IN_WORK", allowableValues = {"IN_WORK"})
        String statut,

        @Schema(description = "**Refusée à la création** (400 si fournie) — voir la description de "
                + "cette requête. Se renseigne au passage à `POINTED`.",
                accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime dateReglement
) {
}
