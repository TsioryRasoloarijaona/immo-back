package com.tsix.immocrm.dto.encaissement;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneEncaissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shape for {@code GET /api/encaissements} — reproduit l'écran legacy "Encaissements Vente"
 * (module Vente → onglet "Encaissements Vente"). Backed by {@code StlSettlement}, restreint aux
 * règlements liés à au moins une échéance de Dossier de vente (via {@code StlSettlementLine.ComDocumentId}
 * — un {@code StlSettlement} peut en théorie aussi servir des achats/locations, hors périmètre ici).
 *
 * <p><strong>Toutes les clés ne sont pas toujours présentes.</strong> Le paramètre
 * {@code ?colonnes=} sélectionne celles qui sont sérialisées ; les autres sont absentes de la
 * réponse — voir {@link ColonneEncaissement}, qui fait aussi office de liste blanche et alimente
 * {@code GET /api/referentiels/colonnes/encaissements}.
 *
 * <p>Sans ce paramètre, la réponse est <em>exactement</em> celle d'avant son introduction : les
 * vingt-sept champs historiques, dans le même ordre, et rien d'autre.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc — dérivé directement de
 * {@code StlSettlement} et de l'écran "Encaissements Vente" observé dans OMRAN Estate.
 */
@JsonFilter(ColonneEncaissement.FILTRE)
public record EncaissementDto(
        UUID id,
        /** {@code StlSettlement.Code} — "Référence" dans l'écran legacy (ex. n° de chèque). */
        String reference,
        /** {@code StlSettlement.Status} brut ("IN_WORK"/"POINTED"/"CANCELED" confirmés sur données réelles ; "UNPAID"/"LOST" existent comme filtres dans l'écran legacy mais aucune ligne réelle ne les porte à ce jour — voir le guide front). */
        String etat,
        UUID dossierId,
        String dossierCode,
        /** "Date p[ièce]" — date de saisie du règlement. */
        LocalDateTime dateDocument,
        /** "Date éch[éance]" — date de l'échéance réglée. */
        LocalDateTime dateEcheance,
        /** Date de règlement effectif (encaissement du chèque, etc.). */
        LocalDateTime dateReglement,
        UUID tierId,
        /** "Tier" dans l'écran legacy — le tiers qui a réglé (peut différer de l'acquéreur principal). */
        String tierNom,
        /** "Montant brut" — {@code StlSettlement.Amount}. */
        BigDecimal montantBrut,
        /** "Montant brut rest[ant]" — calculé : {@code montantBrut - somme des lignes déjà affectées à une échéance}. */
        BigDecimal montantBrutRestant,
        /**
         * "Montant net" — calculé : {@code montantBrut - AmountRS (retenue source) - AmountStamp
         * (droit de timbre)}. Formule non confirmable sur données réelles (AmountRS/AmountStamp
         * valent toujours 0 dans yesRems à ce jour) — voir le guide front.
         */
        BigDecimal montantNet,
        /** "Comptabilisé" — {@code StlSettlement.IsAccounted}. */
        Boolean comptabilise,
        UUID typeId,
        String typeCode,
        String typeLibelle,
        /** "Entité" — {@code StlSettlement.CfgCompanyId}. */
        UUID entiteId,
        String entiteCode,
        String entiteLibelle,
        /** "Compte" — {@code StlSettlement.StlAccountId}. */
        UUID compteId,
        String compteCode,
        /** "Dépôt" — {@code StlSettlement.StlDepositId} (FOURNISSEUR/CLIENT/BANQUE). */
        UUID depotId,
        String depotCode,
        UUID banqueId,
        String banqueNom,
        /**
         * {@code StlSettlement.Notes} — mesuré à 3 lignes sur 643. Non groupable :
         * {@code nvarchar(max)}, que le {@code COUNT(DISTINCT)} du comptage de groupes refuse.
         */
        String notes,

        // ------------------------------------------------------------------
        // Ajouts du lot « sélecteur de colonnes et groupement ». Hors sélection par défaut : un
        // client qui ne passe pas ?colonnes= ne les voit pas apparaître.
        // Voir instructions/FEATURE-colonnes-groupement-encaissements.md.
        // ------------------------------------------------------------------

        /**
         * "Libellé" — {@code StlSettlement.Description}. Mesuré à 643/643 avec 530 valeurs
         * distinctes : le champ le plus informatif que la liste ne servait pas.
         */
        String description,
        /** "Montant base" — {@code StlSettlement.AmountBase}, 643/643 tous non-zéro. */
        BigDecimal montantBase,
        /** Tiers <em>payeur</em> ({@code TierPayerId}) — ne diverge de {@code tierId} que sur 7 lignes sur 643. */
        UUID tierPayeurId,
        /** Nom du tiers payeur, même formatage que {@code tierNom}. */
        String tierPayeurNom,
        /** "Remis au centre" — {@code IsDcoumentDeliveredToCenter} (la faute de frappe est celle du schéma légataire). */
        Boolean remisAuCentre,
        /** "N° de reçu" — {@code ReceiptCode}, mesuré à 19 lignes sur 643. */
        String recuCode,
        /** Date de remise au centre — mesurée à 4 lignes sur 643. */
        LocalDateTime dateRemiseAuCentre,
        /** "Bordereau" — {@code SlipCode}, mesuré à 3 lignes sur 643. */
        String bordereauCode,
        /** Date du bordereau — mesurée à 4 lignes sur 643. */
        LocalDateTime dateBordereau,
        /** Piste d'audit — {@code UserCreate}, 643/643, 17 valeurs distinctes. */
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
