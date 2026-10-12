package com.tsix.immocrm.domain.audit;

/**
 * Classe OMRAN d'un objet tracé : {@code ObjectType} (nom complet .NET) et {@code ObjectTypeLabel}, mesurés en
 * production — voir FEATURE-patrimoine-audit-actions.md §1.
 */
public record TypeObjetAudit(String objectType, String libelle) {

    /** Nom court de la classe (dernier segment), premier élément du détail d'une suppression. */
    public String nomCourt() {
        return objectType.substring(objectType.lastIndexOf('.') + 1);
    }
}
