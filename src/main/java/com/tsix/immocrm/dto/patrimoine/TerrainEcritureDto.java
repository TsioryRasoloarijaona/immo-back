package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Création, modification (PATCH : absent ou null = inchangé) ou sous-terrain (code, description, "
        + "surfaces et prix seuls, le reste copié du parent). Taux en fraction (0,2 = 20 %).")
public record TerrainEcritureDto(
        @Schema(description = "Code, unique dans la société (409). Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Société (CfgCompanyId), active. Obligatoire.") UUID entiteId,
        @Schema(description = "Pays (AdmCountry). Obligatoire.") UUID paysId,
        @Schema(description = "Ville (AdmCity). Obligatoire.") UUID villeId,
        @Schema(description = "Nature (StkKind) : Terrain implicite, une autre nature → 400.") UUID natureId,
        @Schema(description = "Vocation (StkVocation). Obligatoire.") UUID vocationId,
        @Schema(description = "Catégorie de nature Terrain. Obligatoire.") UUID categorieId,
        @Schema(description = "Type de nature Terrain. Obligatoire.") UUID typeId,
        @Schema(description = "Niveau (LndLevel).") UUID niveauId,
        @Schema(description = "Vocation foncière (LndVocation).") UUID vocationTerrainId,
        @Schema(description = "Orientation (StkOrientation).") UUID orientationId,
        @Schema(description = "Catégorie de charge (StkCostCategory).") UUID categorieChargeId,
        @Schema(description = "IsFinished ; vrai à la création par défaut, verrouillé pour un sous-terrain (400).")
        Boolean estTermine,
        @Schema(description = "AreaFloor, > 0. Obligatoire.") BigDecimal surfaceProvisoire,
        @Schema(description = "Area, > 0, défaut = surface provisoire.") BigDecimal surfaceDefinitive,
        @Schema(description = "ComSaleUnitCostFloorHT ; exclusif avec prixM2ProvisoireTTC.") BigDecimal prixM2ProvisoireHT,
        @Schema(description = "ComSaleUnitCostFloor ; exclusif avec prixM2ProvisoireHT.") BigDecimal prixM2ProvisoireTTC,
        @Schema(description = "ComSaleUnitCostHT ; exclusif avec prixM2DefinitifTTC.") BigDecimal prixM2DefinitifHT,
        @Schema(description = "ComSaleUnitCost ; exclusif avec prixM2DefinitifHT.") BigDecimal prixM2DefinitifTTC,
        @Schema(description = "Vat, fraction entre 0 et 1, défaut 0.") BigDecimal tauxTva,
        @Schema(description = "PurchaseUnitCost ; donne prixAchat = prixAchatM2 × surface définitive.") BigDecimal prixAchatM2,
        @Schema(description = "PurchaseCost ; donne prixAchatM2 = prixAchat / surface définitive (surface ≠ 0).")
        BigDecimal prixAchat,
        @Schema(description = "Propriétaire du terrain (CfgSupplierId) : fournisseur par défaut de ses frais.")
        UUID fournisseurId,
        @Schema(description = "PurchaseDate : date d'achat.") LocalDateTime dateAchat,
        @Schema(description = "BuildingPermissionDate : date du permis de construire.") LocalDateTime datePermisConstruire,
        @Schema(description = "TitleNumber : n° du titre foncier.") String titreFoncier,
        @Schema(description = "TitleName : intitulé du titre foncier.") String intituleTitreFoncier,
        @Schema(description = "TitleDate : date du titre foncier.") LocalDateTime dateTitreFoncier,
        @Schema(description = "Address (500 caractères au plus).") String adresse,
        @Schema(description = "GmapLink : lien Google Maps (1000 caractères au plus).") String lienGoogle,
        @Schema(description = "Notes (1000 caractères au plus).") String notes,
        @Schema(description = "Anomalies (1000 caractères au plus).") String anomalies,
        @Schema(description = "PATCH : champs facultatifs à vider (référence ou date → NULL, texte → \"\").")
        List<String> champsAVider
) {
}
