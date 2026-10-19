package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Champs de la fiche article (FEATURE-patrimoine-article-fiche.md §1), à plat dans {@link ArticleDetailDto}. */
@Schema(description = "Champs de la fiche article : mêmes noms et colonnes que dans ArticleEcritureDto.")
public record FicheArticleDto(
        UUID orientationId,
        UUID categorieChargeId,
        UUID facadeId,
        UUID entiteSyndicId,
        @Schema(description = "Vues de l'article (StkItemVue.StkVueId).") List<UUID> vueIds,
        BigDecimal avanceMinimum,
        BigDecimal loyerMensuelHT,
        Integer nombreCautions,
        String anomalies,
        String contenanceLibre,
        Boolean disponibleVenteWeb,
        Boolean disponibleLocationWeb,
        LocalDateTime dateEnregistrement,
        String numeroEnregistrement,
        String lieuEnregistrement,
        String cpfImmatriculation,
        LocalDateTime dateImmatriculation,
        String titreFoncierMere,
        String intituleTitreFoncier,
        LocalDateTime dateTitreFoncier,
        String proprieteDite,
        String fractionDivise,
        String fractionDiviseTerrain,
        BigDecimal partiesDivises,
        BigDecimal partiesIndivises,
        String quotaTerrainGroupe,
        String notesTitreFoncier,
        String referenceElectricite,
        String policeElectricite,
        String compteurElectricite,
        String releveElectricite,
        String referenceEau,
        String policeEau,
        String compteurEau,
        String releveEau,
        String referenceGaz,
        String policeGaz,
        String compteurGaz,
        String releveGaz,
        @Schema(description = "Calculée à la lecture (AreaNet de StkItemView) : surface + Σ sous-articles rattachés ; "
                + "terrain : surface − surface des équipements.") BigDecimal superficieNette,
        @Schema(description = "Calculée à la lecture (TitledArea de StkItemView) : surface si titré, + Σ sous-articles "
                + "rattachés titrés.") BigDecimal superficieTitree
) {
}
