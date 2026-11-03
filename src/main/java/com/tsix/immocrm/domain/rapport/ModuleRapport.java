package com.tsix.immocrm.domain.rapport;

import com.tsix.immocrm.exceptions.ResourceNotFoundException;

import java.util.Arrays;
import java.util.List;

/** Module d'un rapport : slug d'URL et valeurs de {@code AdmReportData.Type} qu'il couvre. */
public enum ModuleRapport {
    VENTE("vente", List.of("Yes.Com.Front.Tdb.Reports"), true, false),
    PATRIMOINE("patrimoine", List.of("Yes.Stk.Front.Tdb.Reports"), false, false),
    /** Réservé à l'Administrateur par règle d'URL ({@code SecurityConfig}). */
    ADMINISTRATION("administration", List.of("Yes.Adm.Front.Tdb.Reports"), false, true);

    private final String slug;
    private final List<String> types;
    private final boolean tranchesEnVenteSeulement;
    private final boolean listeNonImplementes;

    ModuleRapport(String slug, List<String> types, boolean tranchesEnVenteSeulement, boolean listeNonImplementes) {
        this.slug = slug;
        this.types = types;
        this.tranchesEnVenteSeulement = tranchesEnVenteSeulement;
        this.listeNonImplementes = listeNonImplementes;
    }

    /** Vrai : la liste montre aussi les rapports OMRAN sans implémentation, marqués non disponibles. */
    public boolean listeNonImplementes() {
        return listeNonImplementes;
    }

    public String slug() {
        return slug;
    }

    public List<String> types() {
        return types;
    }

    public boolean tranchesEnVenteSeulement() {
        return tranchesEnVenteSeulement;
    }

    /** @throws ResourceNotFoundException module inconnu (404) */
    public static ModuleRapport parSlug(String slug) {
        return Arrays.stream(values()).filter(m -> m.slug.equals(slug)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Module de rapports inconnu : " + slug));
    }
}
