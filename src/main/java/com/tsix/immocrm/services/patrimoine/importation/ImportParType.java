package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import com.tsix.immocrm.web.export.FeuilleExport;

import java.util.List;
import java.util.Set;

/**
 * Un type d'import (FEATURE-patrimoine-import-autres.md §2) : traduction des lignes et écriture par les services de
 * l'écran, dans la transaction du moteur ({@link ImportPatrimoineService}) ; {@code C} = contexte d'un fichier.
 */
interface ImportParType<C> {

    /** Valeurs de {@code {type}} servies. */
    Set<String> types();

    /** Vrai : {@code trancheId} obligatoire (400) et visible (404) ; faux : la tranche passée au contexte est nulle. */
    boolean trancheExigee();

    C contexte(String type, CfgTranche tranche, Set<String> entetes, List<String> avertissements);

    /** En-têtes lus ; les autres donnent l'avertissement « colonne ignorée ». */
    Set<String> entetesConnus(C ctx);

    List<LigneBilanDto> ecrire(List<LigneLue> lignes, C ctx);

    Modele modele(String type, CfgTranche tranche);

    /** {@code prefixe} du nom de fichier (l'horodatage suit) ; la première feuille porte les données. */
    record Modele(String prefixe, List<FeuilleExport> feuilles) {
    }
}
