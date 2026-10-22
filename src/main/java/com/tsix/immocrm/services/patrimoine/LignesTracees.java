package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.LigneOmran;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/** Mise à jour en place d'une ligne OMRAN : trace (et donc audit « Modified ») seulement si une valeur change. */
final class LignesTracees {

    private LignesTracees() {
    }

    /** Vrai si la ligne est nouvelle ou modifiée ; une ligne sans id en reçoit un et sa trace de création. */
    static <L extends LigneOmran> boolean appliquer(L ligne, Function<L, List<Object>> valeurs, Consumer<L> modifier,
                                                    TracePatrimoine.Trace trace) {
        boolean nouvelle = ligne.getId() == null;
        List<Object> avant = valeurs.apply(ligne);
        modifier.accept(ligne);
        if (!nouvelle && avant.equals(valeurs.apply(ligne))) {
            return false;
        }
        if (nouvelle) {
            ligne.setId(UUID.randomUUID());
            ligne.setUserCreate(trace.auteur());
            ligne.setUserCreateDateTime(trace.le());
        }
        ligne.setUserUpdate(trace.auteur());
        ligne.setUserUpdateDateTime(trace.le());
        return true;
    }
}
