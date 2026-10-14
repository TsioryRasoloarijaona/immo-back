package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.dto.dossier.edition.ModifierDossierRequest;
import com.tsix.immocrm.entities.com.ComFolder;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Un champ simple de la fiche : son nom sur le fil, son groupe de droits, sa lecture dans la requête et sur le
 * dossier, son écriture. {@code vidable} = accepté dans {@code champsAVider}.
 */
record Champ<T>(String nom, GroupeEdition groupe, Function<ModifierDossierRequest, T> saisie,
                Function<ComFolder, T> actuel, BiConsumer<ComFolder, T> ecriture, boolean vidable) {

    boolean present(ModifierDossierRequest r) {
        return saisie.apply(r) != null;
    }

    /** Écrit la valeur saisie (ou null si {@code vider}) ; vrai si la valeur du dossier a changé. */
    boolean appliquer(ModifierDossierRequest r, ComFolder f, boolean vider) {
        T nouvelle = vider ? null : saisie.apply(r);
        if (Objects.equals(nouvelle, actuel.apply(f))) {
            return false;
        }
        ecriture.accept(f, nouvelle);
        return true;
    }
}
