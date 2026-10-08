package com.tsix.immocrm.domain;

import java.time.ZoneId;

/**
 * Fuseau de l'heure murale stockée par OMRAN et des « jours » métier ; le serveur, lui, tourne en UTC.
 * Voir FIX-horloge-casablanca.md.
 */
public final class FuseauMetier {

    public static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");

    private FuseauMetier() {
    }
}
