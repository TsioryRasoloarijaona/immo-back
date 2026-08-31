package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Body for {@code POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements}.
 * <p>
 * Carries two distinct dates, confirmed against real data (see {@code EcheancierService.addReglement}
 * and {@code ChampDateEncaissement}, which documents the same two columns for
 * {@code GET /api/encaissements}) — do not conflate them:
 * <ul>
 *   <li>{@code dateEcheance} → {@code StlSettlement.EcheanceDate} — "date d'échéance" du règlement,
 *       renseignée sur la quasi-totalité des règlements réels (&gt;99% des lignes, tous types
 *       confondus) et quasi toujours égale à la date de saisie — defaults to now() if omitted.</li>
 *   <li>{@code dateReglement} → {@code StlSettlement.SettlementDate} — "date de règlement effectif"
 *       (encaissement réel du chèque, valeur bancaire...), renseignée a posteriori et donc
 *       optionnelle : confirmée quasi jamais renseignée en données réelles (14 lignes sur ~3 260
 *       au moment de cette vérification, sans écart régulier avec {@code dateEcheance} —
 *       de -2 à +129 jours selon les cas) — no default derivable from real data, left null unless
 *       explicitly provided.</li>
 * </ul>
 */
public record CreerReglementRequest(
        @Schema(description = "Montant du règlement.", example = "50000.00")
        BigDecimal montant,
        @Schema(description = "Date d'échéance du règlement (StlSettlement.EcheanceDate) — "
                + "défaut : maintenant, si absent.")
        LocalDateTime dateEcheance,
        @Schema(description = "Date de règlement effectif (StlSettlement.SettlementDate) — "
                + "encaissement réel/valeur bancaire, renseignée a posteriori. Optionnelle, "
                + "aucune valeur par défaut (rarement connue à la saisie).")
        LocalDateTime dateReglement,
        @Schema(description = "Type de règlement (StlSettlementType.PKey) — voir "
                + "GET /api/referentiels/types-reglement.")
        UUID typeId,
        @Schema(description = "Banque (CfgTier.PKey, type BAN) — voir GET /api/referentiels/banques. "
                + "Pertinent seulement si le type le requiert (TypeReglementDto.banqueObligatoire).")
        UUID banqueId,
        @Schema(description = "Référence du règlement (StlSettlement.Code) — n° de chèque, de "
                + "virement... Requise si TypeReglementDto.referenceObligatoire est vrai pour le "
                + "type choisi (400 sinon).", example = "CHQ-000123")
        String reference,
        @Schema(description = "Tiers ayant effectivement réglé (StlSettlement.TierId) — \"réglé "
                + "par\", peut différer de l'acquéreur principal (co-acquéreur, tiers payeur...). "
                + "Confirmé quasi toujours renseigné en données réelles (>99%). Optionnel : "
                + "reste null si absent, pas de valeur par défaut (pas de tiers \"acquéreur "
                + "principal\" implicite côté serveur).")
        UUID payeurId,
        String notes
) {
}
