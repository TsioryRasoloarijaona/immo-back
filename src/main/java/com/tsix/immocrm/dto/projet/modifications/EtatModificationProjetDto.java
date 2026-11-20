package com.tsix.immocrm.dto.projet.modifications;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "État d'une demande de modification (ComModification.Status).")
public record EtatModificationProjetDto(
        @Schema(description = "Code (nom d'EtatModification), tel que stocké.", example = "ENCOURS") String code,
        @Schema(description = "Libellé ; le code brut si inconnu.", example = "En cours") String libelle) {
}
