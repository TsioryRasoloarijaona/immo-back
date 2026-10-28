package com.tsix.immocrm.services.modele;

/**
 * Droit propre à certains modèles, en plus de l'accès à la fiche (FEATURE-vente-formulaire-barid.md §6) : appliqué à la liste
 * des modèles actifs et à la génération. Un modèle qu'aucune implémentation ne vise garde le droit commun.
 */
public interface ControleAccesModele {

    /** Faux seulement si ce contrôle vise {@code codeModele} et que l'appelant n'y a pas droit (relu à chaque appel). */
    boolean autoriser(String codeModele);

    /** Vrai si {@code codeModele} ne se génère qu'en PDF. */
    boolean pdfSeulement(String codeModele);
}
