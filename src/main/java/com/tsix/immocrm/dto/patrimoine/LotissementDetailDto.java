package com.tsix.immocrm.dto.patrimoine;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Références nommées comme {@link ArticleDetailDto} ; champs saisis aux noms de {@link LotissementEcritureDto}. */
@Schema(description = "Fiche d'un lot de terrain. Les champs de la fiche article (foncier, compteurs, orientation…) sont "
        + "à plat ; facadeId et vueIds n'y servent pas.")
public record LotissementDetailDto(
        UUID id,
        String code,
        String description,
        UUID trancheId,
        String trancheCode,
        String trancheDescription,
        @Schema(description = "Programme de la tranche.") UUID programmeId,
        String programmeCode,
        String programmeDescription,
        @Schema(description = "Entité (société) du programme : en-tête de la fiche PDF.") UUID entiteId,
        String entiteCode,
        String entiteDescription,
        UUID vocationId,
        String vocationCode,
        String vocationLibelle,
        UUID categorieId,
        String categorieCode,
        String categorieLibelle,
        UUID typeId,
        String typeCode,
        String typeLibelle,
        @Schema(description = "StkPresentationType, propre à la tranche.") UUID presentationTypeId,
        String presentationTypeCode,
        String presentationTypeLibelle,
        @Schema(description = "Libellé de orientationId.") String orientationLibelle,
        @Schema(description = "Libellé de categorieChargeId.") String categorieChargeLibelle,
        @Schema(description = "Libellé de entiteSyndicId.") String entiteSyndicLibelle,
        String adresse,
        String notes,
        String titreFoncier,
        Boolean estTitre,
        @Schema(description = "Affecté à un dossier de vente ou de location : surfaces, prix, classification, titre et code "
                + "ne se modifient plus.") boolean estOccupe,
        @Schema(description = "AreaFloor.") BigDecimal surfaceProvisoire,
        @Schema(description = "Area.") BigDecimal surfaceDefinitive,
        BigDecimal prixM2ProvisoireHT,
        BigDecimal prixM2ProvisoireTTC,
        BigDecimal prixM2DefinitifHT,
        BigDecimal prixM2DefinitifTTC,
        @Schema(description = "SalePriceFloorHT.") BigDecimal prixProvisoireHT,
        @Schema(description = "SalePriceFloor.") BigDecimal prixProvisoireTTC,
        @Schema(description = "SalePriceHT.") BigDecimal prixDefinitifHT,
        @Schema(description = "SalePrice.") BigDecimal prixDefinitifTTC,
        @Schema(description = "Vat, fraction (0,2 = 20 %).") BigDecimal tauxTva,
        BigDecimal remiseMax1,
        BigDecimal remiseMax2,
        BigDecimal cos,
        BigDecimal cuf,
        Integer arreteAlignement,
        Integer retraitVoisin,
        Integer hauteurMax,
        BigDecimal latitude,
        BigDecimal longitude,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe,
        @JsonUnwrapped FicheArticleDto fiche
) {
}
