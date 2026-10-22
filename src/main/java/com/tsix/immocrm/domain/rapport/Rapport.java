package com.tsix.immocrm.domain.rapport;

import java.util.List;

/**
 * Un rapport du hub « Rapports » : un {@code @Component} par fichier, découvert par le registre
 * (FEATURE-tdb-vente-rapports-socle.md §3.1). Droits et visibilité sont appliqués par le socle avant {@link #executer}.
 */
public interface Rapport {

    /** {@code AdmReportData.Code}, unique à l'intérieur du module. */
    String code();

    ModuleRapport module();

    /** Paramètres acceptés, dans l'ordre d'affichage ; relus à chaque requête. */
    List<ParametreRapport> parametres();

    /** Lecture seule ; ne lit que les tranches de {@link ContexteRapport#tranchesVisibles()}. */
    ResultatRapport executer(ContexteRapport contexte);
}
