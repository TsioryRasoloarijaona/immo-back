package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne de la liste d'une famille de tiers.")
public record LigneTiersDto(
        @Schema(description = "PKey du tiers.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Raison sociale.") String raisonSociale,
        @Schema(description = "Libellés des contacts liés (CfgTierContact), séparés par « , ».") String contacts,
        @Schema(description = "Téléphone.") String telephone,
        @Schema(description = "Fax.") String fax,
        @Schema(description = "Mobile.") String mobile,
        @Schema(description = "E-mail.") String email,
        @Schema(description = "Site web.") String siteWeb,
        @Schema(description = "Adresse, ligne 1.") String adresse,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Est un prestataire (IsExtern).") Boolean estPrestataire,
        @Schema(description = "ICE.") String ice,
        @Schema(description = "Libellé de la nature.") String nature,
        @Schema(description = "Libellé du pays.") String pays,
        @Schema(description = "Libellé du pays de résidence.") String paysResidence,
        @Schema(description = "Agence bancaire.") String agence,
        @Schema(description = "RIB.") String rib,
        @Schema(description = "IBAN.") String iban,
        @Schema(description = "Matricule fiscal.") String matriculeFiscal,
        @Schema(description = "Registre de commerce.") String registreCommerce,
        @Schema(description = "Douane.") String douane,
        @Schema(description = "Code comptable.") String codeComptable,
        @Schema(description = "Code comptable collectif.") String codeComptableCollectif,
        @Schema(description = "Libellé de l'assujettissement.") String assujettissement) {
}
