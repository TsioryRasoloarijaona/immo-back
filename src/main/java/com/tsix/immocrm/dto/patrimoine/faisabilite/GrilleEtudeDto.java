package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Corps d'un PUT de grille : liste complète des lignes (convention des onglets de tranche). */
@Schema(description = "Liste complète des lignes : id connu = mise à jour, sans id = création, absente = suppression.")
public record GrilleEtudeDto<T>(@Schema(description = "Lignes de la grille ; [] vide la grille.") List<T> lignes) {
}
