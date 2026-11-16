package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.services.droits.DroitsOmran;

import java.util.Set;

/** Seule règle de droits des prévisions d'un lot (FEATURE-lot-previsions.md R1, R2), lue par le graphe, la saisie et le PUT. */
record DroitsPrevisionsLot(boolean saisir, boolean base) {

    static final String SAISIR = "Yes.Prj.Front.Tdb.ProjectPrevision.toolNew";

    static DroitsPrevisionsLot de(DroitsOmran droits) {
        Set<String> portes = droits.parmi(SAISIR, ModificationLot.DROIT_ESTIMATION);
        boolean saisir = portes.contains(SAISIR);
        return new DroitsPrevisionsLot(saisir, saisir && portes.contains(ModificationLot.DROIT_ESTIMATION));
    }

    DroitsPrevisionsLot exigerSaisie() {
        if (!saisir) {
            throw interdit();
        }
        return this;
    }

    void exigerBase(boolean baseModifiee) {
        if (baseModifiee && !base) {
            throw interdit();
        }
    }

    private static ForbiddenException interdit() {
        return new ForbiddenException("Vous n'avez pas le droit d'effectuer cette action.");
    }
}
