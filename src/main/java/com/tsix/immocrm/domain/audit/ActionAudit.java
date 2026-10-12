package com.tsix.immocrm.domain.audit;

/** Valeurs de {@code AdmWebHistorics.Action}, écrites telles quelles (anglais) comme OMRAN. */
public enum ActionAudit {
    AJOUT("Added"),
    MODIFICATION("Modified"),
    SUPPRESSION("Deleted");

    private final String code;

    ActionAudit(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
