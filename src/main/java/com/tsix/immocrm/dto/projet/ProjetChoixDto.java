package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Projet proposé au sélecteur d'un tableau de bord : projets habilités de la société.")
public record ProjetChoixDto(
        @Schema(description = "Identifiant du projet (PrjProject.PKey).") UUID id,
        @Schema(description = "Code du projet (PrjProject.Code).") String code,
        @Schema(description = "Libellé du projet (PrjProject.Description).") String libelle) {
}
