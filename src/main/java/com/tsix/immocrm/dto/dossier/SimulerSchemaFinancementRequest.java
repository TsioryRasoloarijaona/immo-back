package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Body for {@code POST /api/dossiers/simulation-financement} — étape de simulation AVANT
 * création du dossier, pour prévisualiser le "Schéma de financement" (AUTO-FINANCEMENT / AVANCE /
 * FRAIS SYNDIC) sans rien persister. Prend volontairement le même sous-ensemble de champs que
 * {@link CreerDossierRequest} nécessaire au calcul (le reste — {@code prospectionId},
 * {@code modePaieId}, {@code cin}...) n'entre pas dans le calcul et n'est pas demandé ici.
 *
 * @param articleIds le(s) article(s) envisagé(s) — sert à déterminer la tranche (barème
 *                   AVANCE/SYNDIC) et la vocation (catégorie d'échéance). Le premier article de
 *                   la liste fait foi, exactement comme à la création réelle du dossier.
 * @param prix       le prix de vente envisagé. Obligatoire — sans lui, aucune ligne ne peut être
 *                   calculée (même règle qu'à la création : {@code prix} absent ⇒ pas de schéma).
 * @param remisePercent remise commerciale en % — même sémantique et même calcul qu'à la création
 *                   ({@link CreerDossierRequest#remisePercent()}), pour que la simulation affiche
 *                   exactement les montants que la création produira.
 */
public record SimulerSchemaFinancementRequest(
        List<UUID> articleIds,
        @Schema(description = "Prix de vente TTC envisagé. Avec `remisePercent` : prix AVANT "
                + "remise. Sans `remisePercent` : prix négocié, utilisé tel quel. Obligatoire.",
                example = "1000000.00")
        BigDecimal prix,
        @Schema(description = "Remise commerciale en % (0 à 100), optionnelle. Fournie ⇒ le schéma "
                + "est calculé sur le prix APRÈS remise (article principal + annexes − remise), "
                + "exactement comme le fera `POST /api/dossiers` avec les mêmes paramètres.",
                example = "10")
        BigDecimal remisePercent
) {
}
