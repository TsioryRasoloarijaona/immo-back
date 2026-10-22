package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tsix.immocrm.domain.foncier.SurfacesTerrain;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de la liste des prospections terrain (LndLand). Références : {id, code, description}, null si absentes.")
public record ProspectionTerrainListeDto(
        @Schema(description = "LndLand.PKey.") UUID id,
        String code,
        String description,
        @Schema(description = "DocDate.") LocalDate date,
        @Schema(description = "Catégorie (nature PROS TER).") ReferentielItemDto categorie,
        @Schema(description = "Type (nature TER).") ReferentielItemDto type,
        @Schema(description = "Niveau (LndLevel).") ReferentielItemDto niveau,
        @Schema(description = "Vocation foncière (LndVocation).") ReferentielItemDto vocation,
        ReferentielItemDto pays,
        ReferentielItemDto ville,
        @Schema(description = "Statut (StrStatus de l'entité LndLand), changé par les transitions seules.")
        ReferentielItemDto statut,
        @Schema(description = "StrStatus.ColorName, ARGB hexadécimal (ex. ffff8000).") String statutCouleur,
        @Schema(description = "Code du type de statut (StrStatusType : Nouveau, Annulation…).") String statutType,
        @Schema(description = "StatusValidityDate.") LocalDate dateValiditeStatut,
        @Schema(description = "Courtier (CfgMiddlemanId).") ReferentielItemDto courtier,
        @Schema(description = "Adress.") String adresse,
        @Schema(description = "Owner : propriétaires (texte).") String proprietaires,
        String notes,
        String anomalies,
        @Schema(description = "IsInArchive.") Boolean estArchivee,
        @Schema(description = "IsTransformedToLand.") Boolean estTransformee,
        @Schema(description = "IsFinished : vrai = Aménagé, faux = Brut (zones).") Boolean estAmenage,
        @JsonUnwrapped SurfacesTerrain surfaces,
        @Schema(description = "AlinmentStop : arrêté d'alignement.") Integer arreteAlignement,
        @Schema(description = "MaxHeight.") Integer hauteurMax,
        @Schema(description = "ProspectionPriceProposed : prix proposé.") BigDecimal prixPropose,
        @Schema(description = "RegistryDate.") LocalDate dateEnregistrement,
        @Schema(description = "RegistryNumber.") String numeroEnregistrement,
        @Schema(description = "RegistryPlace.") String lieuEnregistrement,
        @Schema(description = "ImmatriculationCPF.") String cpfImmatriculation,
        @Schema(description = "ImmatriculationDate.") LocalDate dateImmatriculation,
        @Schema(description = "Nombre d'études de faisabilité liées (StkFeasibilityStudy.LndLandId).") long nbEtudesFaisabilite,
        @Schema(description = "UserCreate.") String creePar,
        @Schema(description = "UserCreateDateTime.") LocalDateTime creeLe,
        @Schema(description = "UserUpdate.") String modifiePar,
        @Schema(description = "UserUpdateDateTime.") LocalDateTime modifieLe
) {
}
