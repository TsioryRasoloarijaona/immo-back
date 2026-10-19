package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Liste complète d'un onglet de la fiche tranche : corps du PUT et réponse du GET / PUT.")
public record LignesTrancheDto<T extends LignesTrancheDto.Ligne>(
        @Schema(description = "Lignes de l'onglet ; au PUT, une ligne absente est supprimée.") List<T> lignes
) {

    /** Ligne d'onglet : {@code id} connu = mise à jour, absent = création. */
    public interface Ligne {
        UUID id();
    }
}
