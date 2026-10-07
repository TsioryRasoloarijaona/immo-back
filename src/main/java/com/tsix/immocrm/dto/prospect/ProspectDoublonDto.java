package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Voir instructions/FEATURE-doublons-prospect-projet.md §2. */
public record ProspectDoublonDto(
        UUID id,
        @Schema(description = "Code de la fiche, p.ex. `26/14428`.", example = "26/14428")
        String code,
        @Schema(description = "Nom affiché (raison sociale, sinon `Description`, sinon prénom + nom).",
                example = "Badr ARH")
        String nom,
        @Schema(description = "Prénom (`FirstName`).") String prenom,
        @Schema(description = "Nom de famille (`LastName`).") String nomFamille,
        @Schema(description = "Raison sociale (`SocialReason`), null pour un particulier.") String raisonSociale,
        @Schema(description = "Téléphone fixe (`Phone`), tel que stocké.") String telephone,
        @Schema(description = "Mobile (`Mobile`), tel que stocké : `6 65 68 09 64`.", example = "6 65 68 09 64")
        String mobile,
        @Schema(description = "Email (`EMail`), tel que stocké.") String email,
        @Schema(description = "Ville (`City`).") String ville,
        @Schema(description = "Code du type de tiers : `PRO`, `PRORNT`, `CLI` ou `CLIRNT`.", example = "PRO")
        String type,
        @Schema(description = "Date de création de la fiche (`UserCreateDateTime`), null sur des lignes anciennes.")
        LocalDateTime dateCreation,
        List<CritereDoublon> criteres
) {
}
