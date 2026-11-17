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
 * @param generation liens d'une pièce générée depuis d'autres (FEATURE-achat-generation.md G6) ; null = création libre
 */
public record PieceAchatACreer(String module, String gestion, String sansDepart, String nature, String referenceType,
                               CfgTier tiers, UUID societe, UUID typeMarche, UUID frais,
                               UnaryOperator<String> description, String notes, List<Ligne> lignes,
                               List<Part> categories, List<Part> projets, EnTete entete, Generation generation) {

    public PieceAchatACreer(String module, String gestion, String sansDepart, String nature, String referenceType,
                            CfgTier tiers, UUID societe, UUID typeMarche, UUID frais,
                            UnaryOperator<String> description, String notes, List<Ligne> lignes,
                            List<Part> categories, List<Part> projets, EnTete entete) {
        this(module, gestion, sansDepart, nature, referenceType, tiers, societe, typeMarche, frais, description, notes,
                lignes, categories, projets, entete, null);
    }

    public PieceAchatACreer(String module, String gestion, String sansDepart, String nature, String referenceType,
                            CfgTier tiers, UUID societe, UUID typeMarche, UUID frais,
                            UnaryOperator<String> description, String notes, List<Ligne> lignes,
                            List<Part> categories, List<Part> projets) {
        this(module, gestion, sansDepart, nature, referenceType, tiers, societe, typeMarche, frais, description, notes,
                lignes, categories, projets, null);
    }

    /** La même pièce, générée : gestion d'arrivée et liens de {@code g}. */
    PieceAchatACreer generee(Generation g, String sansDepartGeneree) {
        return new PieceAchatACreer(module, g.gestion(), sansDepartGeneree, nature, referenceType, tiers, societe,
                typeMarche, frais, description, notes, lignes, categories, projets, entete, g);
    }

    /**
     * Pièce générée : gestion d'arrivée, devise de la 1re source, liens écrits ({@code BuyQuoteId}, {@code BuyOrderId}),
     * BL facturés ({@code BuyInvoiceId}) et BC d'une ligne {@code BuyOrderInvoice} chacun.
     */
    public record Generation(String gestion, UUID devise, UUID devis, UUID commande, List<UUID> livraisons,
                             List<UUID> commandes) {
    }

    /** En-tête saisi ; textes et montants déjà normalisés ('' et 0). */
    public record EnTete(UUID module, UUID projet, UUID lot, UUID declencheur, LocalDate date, LocalDate echeance,
                         LocalDate livraison, String reference, String personne, String notes2,
                         String referenceBureauOrdre, BigDecimal regul, BigDecimal timbre) {
    }

    /** Ligne ; {@code id} d'une ligne existante (modification) ou null ; {@code bordereau} écrit seulement s'il est non nul. */
    public record Ligne(UUID id, String code, String description, String unite, BigDecimal quantite, BigDecimal prix,
                        BigDecimal remise, BigDecimal fodec, BigDecimal tva, UUID categorie, String notes,
                        UUID bordereau) {

        public Ligne(UUID id, String code, String description, String unite, BigDecimal quantite, BigDecimal prix,
                     BigDecimal remise, BigDecimal fodec, BigDecimal tva, UUID categorie, String notes) {
            this(id, code, description, unite, quantite, prix, remise, fodec, tva, categorie, notes, null);
        }

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
