package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;

/**
 * Seule définition d'un dossier sorti (annulé ou désisté) : drapeaux « Annulation » ({@code BoolField002}) et
 * « Résiliation » ({@code BoolField003}) de son statut, comme {@code IsCancellation} de {@code ComSaleFolderView}.
 */
public final class DossierSorti {

    private DossierSorti() {
    }

    /** Sans statut, ou drapeaux NULL (lus 0 par la vue) : le dossier est en cours. {@code dossier} non nul. */
    public static boolean estSorti(ComFolder dossier) {
        StrStatus statut = dossier.getStrStatus();
        return statut != null && !statut.estActif();
    }
}
