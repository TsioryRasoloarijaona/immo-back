package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.MontantsPieceAchat;
import com.tsix.immocrm.entities.cfg.CfgTier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Pièce d'achat à écrire par {@link EcriturePieceAchat} (FEATURE-achat-regle-commune.md §3) : ce qui dépend de l'appelant.
 * Ventilation catégorie en montants par clé : l'écrivain en tire les taux, le dernier complète Σ = 1. Ventilation projet
 * en montants sans {@code entete}, en taux avec.
 *
 * @param sansDepart message de l'erreur 500 quand le workflow de la gestion n'a pas de statut de départ
 * @param entete     en-tête saisi par la fiche (FEATURE-achat-fiche-creation.md) ; null = commissions, frais de terrain
 */
public record PieceAchatACreer(String module, String gestion, String sansDepart, String nature, String referenceType,
                               CfgTier tiers, UUID societe, UUID typeMarche, UUID frais,
                               UnaryOperator<String> description, String notes, List<Ligne> lignes,
                               List<Part> categories, List<Part> projets, EnTete entete) {

    public PieceAchatACreer(String module, String gestion, String sansDepart, String nature, String referenceType,
                            CfgTier tiers, UUID societe, UUID typeMarche, UUID frais,
                            UnaryOperator<String> description, String notes, List<Ligne> lignes,
                            List<Part> categories, List<Part> projets) {
        this(module, gestion, sansDepart, nature, referenceType, tiers, societe, typeMarche, frais, description, notes,
                lignes, categories, projets, null);
    }

    /** En-tête saisi ; textes et montants déjà normalisés ('' et 0). */
    public record EnTete(UUID module, UUID projet, UUID lot, UUID declencheur, LocalDate date, LocalDate echeance,
                         LocalDate livraison, String reference, String personne, String notes2,
                         String referenceBureauOrdre, BigDecimal regul, BigDecimal timbre) {
    }

    /** Ligne ; {@code id} d'une ligne existante (modification) ou null. Taux en fraction. */
    public record Ligne(UUID id, String code, String description, String unite, BigDecimal quantite, BigDecimal prix,
                        BigDecimal remise, BigDecimal fodec, BigDecimal tva, UUID categorie, String notes) {

        /** Ligne {@code U} × 1, TVA 0 : PU HT = PU TTC = {@code montant}. */
        public Ligne(String code, String description, BigDecimal montant, String notes, UUID categorie) {
            this(null, code, description, "U", BigDecimal.ONE, montant, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    categorie, notes);
        }

        MontantsPieceAchat.Ligne montants() {
            return new MontantsPieceAchat.Ligne(quantite, Objects.requireNonNullElse(prix, BigDecimal.ZERO), remise, fodec,
                    tva);
        }
    }

    public record Part(UUID cle, BigDecimal montant) {
    }
}
