package com.tsix.immocrm.web.export;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

/** {@code ?format=&colonnes=} d'un export de liste patrimoine, lié une fois par {@code @ModelAttribute}. */
public record DemandeExport(
        @Parameter(description = "xlsx (défaut) ou pdf ; autre valeur = 400.", example = "pdf") String format,
        @Parameter(description = "Colonnes du tableau dans l'ordre voulu ; absent = celles affichées par défaut.",
                example = "code,description") List<String> colonnes) {
}
