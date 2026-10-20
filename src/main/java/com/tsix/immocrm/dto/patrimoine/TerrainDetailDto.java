package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche terrain (StkItem de nature Terrain). Références : {id, code, description}, null si absentes.")
public record TerrainDetailDto(
        @Schema(description = "StkItem.PKey.") UUID id,
        String code,
        String description,
        @Schema(description = "Société (CfgCompanyId).") ReferentielItemDto entite,
        @Schema(description = "Pays (AdmCountryId).") ReferentielItemDto pays,
        @Schema(description = "Ville (AdmCityId).") ReferentielItemDto ville,
        @Schema(description = "Nature (Terrain).") ReferentielItemDto nature,
        @Schema(description = "Vocation (StkVocation).") ReferentielItemDto vocation,
        @Schema(description = "Catégorie (StkItemCategory).") ReferentielItemDto categorie,
        @Schema(description = "Type (StkItemType).") ReferentielItemDto type,
        @Schema(description = "Statut (StrStatus de l'entité LandProject), en lecture seule.") ReferentielItemDto statut,
        @Schema(description = "Niveau (LndLevel).") ReferentielItemDto niveau,
        @Schema(description = "Vocation foncière (LndVocation).") ReferentielItemDto vocationTerrain,
        @Schema(description = "Orientation (StkOrientation).") ReferentielItemDto orientation,
        @Schema(description = "Catégorie de charge (StkCostCategory).") ReferentielItemDto categorieCharge,
        @Schema(description = "Propriétaire (CfgSupplierId), fournisseur par défaut des frais.") ReferentielItemDto fournisseur,
        @Schema(description = "Terrain parent (ParentId) d'un sous-terrain, sinon null.") ReferentielItemDto parent,
        @Schema(description = "IsFinished.") Boolean estTermine,
        @Schema(description = "Vrai si le statut autorise les sous-terrains (StrStatus.BoolField001).")
        boolean sousTerrainsAutorises,
        @Schema(description = "Affecté à un dossier de vente ou de location : surfaces, prix et titre protégés.")
        boolean occupe,
        @Schema(description = "LndLandId : prospection foncière d'origine (lecture seule).") UUID prospectionFonciereId,
        @Schema(description = "AreaFloor.") BigDecimal surfaceProvisoire,
        @Schema(description = "ComSaleUnitCostFloorHT.") BigDecimal prixM2ProvisoireHT,
        @Schema(description = "ComSaleUnitCostFloor.") BigDecimal prixM2ProvisoireTTC,
        @Schema(description = "SalePriceFloorHT.") BigDecimal prixProvisoireHT,
        @Schema(description = "SalePriceFloor.") BigDecimal prixProvisoireTTC,
        @Schema(description = "Area.") BigDecimal surfaceDefinitive,
        @Schema(description = "ComSaleUnitCostHT.") BigDecimal prixM2DefinitifHT,
        @Schema(description = "ComSaleUnitCost.") BigDecimal prixM2DefinitifTTC,
        @Schema(description = "SalePriceHT.") BigDecimal prixDefinitifHT,
        @Schema(description = "SalePrice.") BigDecimal prixDefinitifTTC,
        @Schema(description = "Vat, fraction.") BigDecimal tauxTva,
        @Schema(description = "PurchaseUnitCost.") BigDecimal prixAchatM2,
        @Schema(description = "PurchaseCost.") BigDecimal prixAchat,
        @Schema(description = "PurchaseDate.") LocalDateTime dateAchat,
        @Schema(description = "BuildingPermissionDate.") LocalDateTime datePermisConstruire,
        @Schema(description = "Σ des montants des frais du terrain, calculé à la lecture (jamais stocké).") BigDecimal coutTotal,
        @Schema(description = "coutTotal / surface définitive, 0 si la surface est nulle ; calculé à la lecture.")
        BigDecimal coutUnitaireFinal,
        @Schema(description = "TitleNumber.") String titreFoncier,
        @Schema(description = "TitleName.") String intituleTitreFoncier,
        @Schema(description = "TitleDate.") LocalDateTime dateTitreFoncier,
        @Schema(description = "Address.") String adresse,
        @Schema(description = "GmapLink.") String lienGoogle,
        String notes,
        String anomalies,
        @Schema(description = "Sous-terrains (ParentId = ce terrain).") List<TerrainListeDto> sousTerrains,
        String userCreate,
        LocalDateTime userCreateDateTime,
        String userUpdate,
        LocalDateTime userUpdateDateTime
) {
}
