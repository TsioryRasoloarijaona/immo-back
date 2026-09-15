package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Bloc « ventes concrétisées » de {@link SyntheseAnnuelleDto}.
 *
 * <p>L'assiette est celle du légataire : les dossiers dont la <b>date d'avance</b>
 * ({@code ComFolder.AdvanceDate}) tombe dans l'année, au moins au statut CONCRETISATION, hors
 * annulation et désistement.
 */
@Schema(description = "Les ventes concrétisées de l'année, sur l'assiette exacte du logiciel de "
        + "bureau : dossiers dont la date d'avance tombe dans l'année et dont le statut a atteint "
        + "CONCRETISATION, hors sorties.")
public record VentesConcretiseesDto(

        @Schema(description = "Nombre de dossiers dans l'assiette.", example = "4")
        long nombreDossiers,

        @Schema(description = "Somme des prix de vente. `0` si l'assiette est vide.",
                example = "4081900.00")
        BigDecimal prixTotal,

        @Schema(description = "Ce qui a déjà été réglé sur ces dossiers — avances, crédits et "
                + "autofinancements, signés par le sens de la catégorie. Les frais n'y entrent "
                + "pas.", example = "1250000.00")
        BigDecimal montantRegle,

        @Schema(description = "**Dossiers écartés faute de date d'avance**, toutes années "
                + "confondues. 232 sur 387 en base réelle — 60 %. Ce compteur existe pour que ce "
                + "trou d'assiette soit affichable : `nombreDossiers` ne couvre pas tous les "
                + "dossiers, et l'ignorer ferait croire à un effondrement des ventes.",
                example = "232")
        long dossiersSansDateAvance
) {
}
