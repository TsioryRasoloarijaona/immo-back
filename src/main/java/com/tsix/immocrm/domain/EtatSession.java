package com.tsix.immocrm.domain;

import java.util.Arrays;

/** Colonne {@code AdmWebSessions.State} : toute valeur hors 2 et 8 se lit « Fermée », comme OMRAN. */
public enum EtatSession {
    OUVERTE(2),
    FERMEE(4),
    VERROUILLEE(8);

    private final int state;

    EtatSession(int state) {
        this.state = state;
    }

    public int state() {
        return state;
    }

    public static EtatSession depuis(int state) {
        return Arrays.stream(values()).filter(e -> e.state == state).findFirst().orElse(FERMEE);
    }
}
