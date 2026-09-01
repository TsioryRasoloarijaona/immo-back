package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Body for {@code POST /api/dossiers}: creates a Dossier from an existing Prospection.
 *
 * NOTE (confirmed): {@code articleIds} is a list, never a single {@code articleId} — a
 * Dossier can link several {@code StkItem}s (e.g. a main lot + its annex).
 *
 * {@code prix} a deux lectures selon que {@code remisePercent} est fourni ou non — voir la
 * documentation des deux champs. Les deux modes restent supportés volontairement : le mode
 * historique (prix négocié seul, remise déduite) reste le comportement par défaut pour ne rien
 * casser côté front, le mode remise explicite est celui de l'écran de création où le commercial
 * saisit lui-même la remise.
 *
 * {@code modePaieId} ({@code ComPaymentMethod}) and {@code cin} (buyer's national ID number)
 * are required at creation, per business decision — even though the underlying DB columns
 * ({@code ComFolder.ComPaymentMethodId}, {@code CfgTier.IDNumber}) are nullable. {@code cin}
 * is written onto the linked prospect's {@code CfgTier.IDNumber} (visible afterwards via
 * {@code GET /api/prospects/{id}} → {@code pieceIdentite.numero}) — it does not create a
 * separate CIN field on the Dossier itself, since identity documents belong to the person,
 * not the sale.
 */
public record CreerDossierRequest(
        UUID prospectionId,
        List<UUID> articleIds,
        @Schema(description = "Prix de vente TTC. **Avec `remisePercent`** : prix AVANT remise "
                + "(remplace le prix commercial de l'article principal comme base de calcul, "
                + "même rôle que le `prix` d'une Offre) — le prix réellement dû est recalculé "
                + "en appliquant la remise. **Sans `remisePercent`** (mode historique) : prix "
                + "négocié APRÈS remise, et la remise est alors déduite de l'écart avec le prix "
                + "commercial de l'article. Optionnel : sans lui, aucun montant n'est calculé et "
                + "aucun schéma de financement n'est généré.",
                example = "1000000.00")
        BigDecimal prix,
        @Schema(description = "Remise commerciale en pourcentage (0 à 100), saisie par le "
                + "commercial. Optionnel. Fournie ⇒ elle est appliquée telle quelle sur le prix "
                + "avant remise (article principal + annexes) et se répercute sur `prixApresRemise`, "
                + "`totalAPayer` **et** sur le schéma de financement généré (AVANCE / "
                + "AUTO-FINANCEMENT calculés sur le prix après remise). Absente ⇒ comportement "
                + "historique inchangé, la remise est déduite du prix négocié.",
                example = "10")
        BigDecimal remisePercent,
        String notes,
        UUID modePaieId,
        String cin
) {
}
