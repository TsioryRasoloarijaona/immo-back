package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les tiers (1000) et les lots de marché (260) ; référentiels, réunion, puis ses lignes (FEATURE-n1-reunions-chantier.md). */
public class EcrituresReunionsChantier implements TablesEcrites {

    @Override
    public int rang() {
        return 1190;
    }

    @Override
    public List<String> tables() {
        return List.of("BulMeetingType", "BulObservationStatus", "PrjMeetingConfigColor", "BulMeeting", "BulMeetingLine",
                "BulMeetingContact", "BulDelegate", "BulDelegateContact", "BulMeetingPrjProjectProgressCriteriaLine");
    }
}
