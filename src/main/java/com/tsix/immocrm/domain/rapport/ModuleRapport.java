package com.tsix.immocrm.domain.rapport;

import com.tsix.immocrm.exceptions.ResourceNotFoundException;

import java.util.Arrays;
import java.util.List;

/** Module d'un rapport : slug d'URL et valeurs de {@code AdmReportData.Type} qu'il couvre. */
public enum ModuleRapport {
    VENTE("vente", List.of("Yes.Com.Front.Tdb.Reports"), true, false, true),
    PATRIMOINE("patrimoine", List.of("Yes.Stk.Front.Tdb.Reports"), false, false, true),
    /** Réservé à l'Administrateur par règle d'URL ({@code SecurityConfig}). */
    ADMINISTRATION("administration", List.of("Yes.Adm.Front.Tdb.Reports"), false, true, true),
    /** Écran + xlsx seulement, pas de PDF (FEATURE-n7-etats-achat.md Q3). */
    ACHAT("achat", List.of("Yes.Buy.Front.Tdb.Reports"), false, false, false),
    /** Écran + xlsx seulement (FEATURE-n5b-rapports-projet.md D1). */
    PROJET("projet", List.of("Yes.Prj.Front.Tdb.Reports"), false, false, false);

    private final String slug;
    private final List<String> types;
    private final boolean tranchesEnVenteSeulement;
    private final boolean listeNonImplementes;
    private final boolean pdf;

    ModuleRapport(String slug, List<String> types, boolean tranchesEnVenteSeulement, boolean listeNonImplementes,
                  boolean pdf) {
        this.slug = slug;
        this.types = types;
        this.tranchesEnVenteSeulement = tranchesEnVenteSeulement;
        this.listeNonImplementes = listeNonImplementes;
        this.pdf = pdf;
    }

    /** Faux : le descripteur n'annonce que xlsx et l'export pdf répond 400. */
    public boolean pdf() {
        return pdf;
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
