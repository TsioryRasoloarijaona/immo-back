package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.entities.cfg.CfgTier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Pièce d'achat à écrire par {@link EcriturePieceAchat} (FEATURE-achat-regle-commune.md §3) : ce qui dépend de l'appelant.
 * Ventilations en montants par clé (projet, catégorie) : l'écrivain en tire les taux, le dernier complète Σ = 1.
 *
 * @param sansDepart message de l'erreur 500 quand le workflow de la gestion n'a pas de statut de départ
 */
public record PieceAchatACreer(String module, String gestion, String sansDepart, String nature, String referenceType,
                               CfgTier tiers, UUID societe, UUID typeMarche, UUID frais,
                               UnaryOperator<String> description, String notes, List<Ligne> lignes,
                               List<Part> categories, List<Part> projets) {

    /** Ligne {@code U} × 1, TVA 0 : PU HT = PU TTC = {@code montant}. */
    public record Ligne(String code, String description, BigDecimal montant, String notes, UUID categorie) {
    }

    public record Part(UUID cle, BigDecimal montant) {
    }
}
