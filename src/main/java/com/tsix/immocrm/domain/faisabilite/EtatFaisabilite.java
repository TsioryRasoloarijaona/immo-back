package com.tsix.immocrm.domain.faisabilite;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entrées du recalcul d'une étude (FEATURE-patrimoine-faisabilite-workflow.md §2-3), lues en base ou saisies :
 * seule source de {@link BilanFaisabilite} et {@link TresorerieFaisabilite}.
 */
public record EtatFaisabilite(Parametres parametres, Map<UUID, Categorie> categories, Map<UUID, Relation> relations,
                              Map<UUID, Unite> unites, List<Tranche> tranches, List<Credit> credits,
                              List<Saisie> saisies) {

    /** Les 4 « relations avec les autres modules » : leur catégorie reçoit un montant imposé par l'étude. */
    public enum Relation {
        PRIX_TERRAIN("DFBB5301-F563-4D18-B2D8-B4A9C9F9B001"),
        ENREGISTREMENT("00580DEE-CB36-48E9-BA13-649D53C97002"),
        AUTRES_FRAIS("9CBF361E-B5C3-40A9-81E9-C75BBB9C7003"),
        INTERETS("FE642B2E-78D9-45DB-8706-7104CB6B8004");

        public final UUID pkey;

        Relation(String pkey) {
            this.pkey = UUID.fromString(pkey);
        }
    }

    /** Colonnes de l'étude lues par le calcul ; écarts-types null = 2 (constructeur OMRAN). */
    public record Parametres(BigDecimal pourcentageConcretisation, BigDecimal tauxIs, BigDecimal prixVenteM2,
                             BigDecimal plancherVendable,
                             BigDecimal coutTotalTerrain, boolean proportionTerrain, BigDecimal tvaDefaut,
                             BigDecimal tvaInterets, BigDecimal tvaTerrain, BigDecimal prixTerrain,
                             BigDecimal prixTerrainHt, BigDecimal enregistrement, BigDecimal enregistrementHt,
                             BigDecimal autresFrais, BigDecimal autresFraisHt, BigDecimal coutRevientStocke,
                             LocalDateTime debutTravaux, LocalDateTime finTravaux, LocalDateTime debutConcretisation,
                             LocalDateTime finConcretisation, LocalDateTime debutFinalisation,
                             LocalDateTime finFinalisation, BigDecimal ecartTypeTravaux,
                             BigDecimal ecartTypeConcretisation, BigDecimal ecartTypeFinalisation, int delaiRetourJours,
                             BigDecimal tauxRetenue, BigDecimal tauxApplicationRetenue, int nombreArticles) {

        /** Aperçu de trésorerie : écarts-types remplacés quand ils sont donnés. */
        public Parametres avecEcartsTypes(BigDecimal travaux, BigDecimal concretisation, BigDecimal finalisation) {
            return new Parametres(pourcentageConcretisation, tauxIs, prixVenteM2, plancherVendable, coutTotalTerrain, proportionTerrain,
                    tvaDefaut, tvaInterets, tvaTerrain, prixTerrain, prixTerrainHt, enregistrement, enregistrementHt,
                    autresFrais, autresFraisHt, coutRevientStocke, debutTravaux, finTravaux, debutConcretisation,
                    finConcretisation, debutFinalisation, finFinalisation, ou(travaux, ecartTypeTravaux),
                    ou(concretisation, ecartTypeConcretisation), ou(finalisation, ecartTypeFinalisation), delaiRetourJours,
                    tauxRetenue, tauxApplicationRetenue, nombreArticles);
        }

        private static BigDecimal ou(BigDecimal valeur, BigDecimal defaut) {
            return valeur != null ? valeur : defaut;
        }
    }

    /** {@code StlCategory} ; {@code recette} = IsCredit. */
    public record Categorie(UUID id, UUID parentId, String code, String libelle, boolean recette, int ordre) {
    }

    /** Ligne d'unité de l'étude ; {@code surfaceArticle} = unité surface portant une catégorie d'article. */
    public record Unite(UUID id, String code, BigDecimal quantite, boolean surfaceArticle) {
    }

    /** Tranche de l'étude ; {@code articles} = Σ quantités ventilées des unités à catégorie d'article, non surface. */
    public record Tranche(UUID id, String code, BigDecimal poids, LocalDateTime debutTravaux, LocalDateTime finTravaux,
                          LocalDateTime debutCommercialisation, LocalDateTime finConcretisation,
                          LocalDateTime debutFinalisation, LocalDateTime finCommercialisation, int articles) {
    }

    public record Credit(LocalDateTime date, BigDecimal montant, List<FinancementFaisabilite.Echeance> echeances) {
    }

    /** Ligne du bilan saisie ou relue ({@code FSPrevision}) : priorité ratio &gt; montantHt &gt; montant. */
    public record Saisie(UUID categorieId, UUID uniteId, UUID referenceId, BigDecimal ratio, BigDecimal montantHt,
                         BigDecimal montant, BigDecimal tva, Boolean pourProjet, LocalDateTime dateApplication) {
    }
}
