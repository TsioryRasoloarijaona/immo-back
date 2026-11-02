package com.tsix.immocrm.domain;

/** Colonne {@code AdmWebLockEntities.State} : toute valeur autre que 1 se lit « Non verrouillée », comme OMRAN. */
public enum EtatFicheVerrouillee {
    VERROUILLEE(1),
    NON_VERROUILLEE(2);

    private final int state;

    EtatFicheVerrouillee(int state) {
        this.state = state;
    }

    public int state() {
        return state;
    }

    public static EtatFicheVerrouillee depuis(int state) {
        return state == VERROUILLEE.state ? VERROUILLEE : NON_VERROUILLEE;
    }
}
