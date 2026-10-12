package com.tsix.immocrm.domain;

/**
 * Type d'une action récente. {@code StrHistory} n'a pas de colonne type : il se lit au début de
 * {@code Description}, d'où des préfixes partagés avec les écritures (FEATURE-actions-recentes.md R3).
 */
public enum TypeActionRecente {

    PRE_PROSPECTION_CREEE("Pré-prospection créée : "),
    PROSPECT_CREE("Prospect créé et lié : "),
    PROSPECT_RATTACHE("Prospect existant rattaché : "),
    TRANSFORMEE_EN_PROSPECTION("Transformée en prospection : "),
    PROSPECTION_CREEE("Prospection créée : "),
    ETAPE_CHANGEE("Étape changée : "),
    ARCHIVEE("Archivée : "),
    DESARCHIVEE("Désarchivée"),
    COMMERCIAL_REAFFECTE("Commercial réaffecté : "),
    DOSSIER_STATUT(null),
    INTERACTION(null),
    AUTRE(null);

    private final String prefixe;

    TypeActionRecente(String prefixe) {
        this.prefixe = prefixe;
    }

    public String prefixe() {
        return prefixe;
    }

    /** {@code Description} à écrire dans {@code StrHistory} : préfixe puis {@code suite}. */
    public String description(String suite) {
        if (prefixe == null) {
            throw new IllegalStateException(name() + " ne s'écrit pas dans l'historique de prospection");
        }
        return suite == null ? prefixe : prefixe + suite;
    }

    /** Ligne d'historique de prospection ; préfixe inconnu ⇒ {@link #AUTRE}, jamais une erreur. */
    public static TypeActionRecente depuisHistoriqueProspection(String description) {
        if (description != null) {
            for (TypeActionRecente type : values()) {
                if (type.prefixe != null && description.startsWith(type.prefixe)) {
                    return type;
                }
            }
        }
        return AUTRE;
    }
}
