package com.tsix.immocrm.domain.achat;

import java.util.Arrays;
import java.util.Optional;

/**
 * Nature d'une pièce d'achat ({@code BuyDocument.DocType}), dans l'ordre d'affichage ; {@code DELIVRY} est l'orthographe
 * OMRAN. Gestion et séquence de création renseignées pour les seules natures saisissables (FEATURE-achat-fiche-creation E-C4).
 */
public enum NatureDocumentAchat {
    INVOICE("Facture", "BuyInvoiceDebit", "Facture fournisseur"),
    ORDER("Bon de commande", "BuyOrder", "Bon de commande sortant"),
    QUOTE("Devis", "BuyQuote", "Devis entrant"),
    // séquence seule, sans gestion : créée depuis la fiche lot, jamais saisissable depuis la fiche Achat
    DOCUMENT_TO_PAY("Document à régler", null, "Document à régler entrant"),
    DELIVRY("Bon de livraison", "BuyDelivery", "Bon de livraison entrant"),
    REFUND("Avoir", null, null);

    private final String libelle;
    private final String gestion;
    private final String sequence;

    NatureDocumentAchat(String libelle, String gestion, String sequence) {
        this.libelle = libelle;
        this.gestion = gestion;
        this.sequence = sequence;
    }

    public String libelle() {
        return libelle;
    }

    /** Code {@code StrEntity} de la gestion d'une création depuis le module Achat. */
    public String gestion() {
        return gestion;
    }

    /** Séquence ({@code AdmSequence}) du numéro d'une création. */
    public String sequence() {
        return sequence;
    }

    /** Libellé d'un {@code DocType} stocké ; une valeur hors liste est rendue telle quelle. */
    public static String libelleDe(String docType) {
        return Arrays.stream(values()).filter(n -> n.name().equals(docType)).findFirst()
                .map(NatureDocumentAchat::libelle).orElse(docType);
    }

    /** Nature créable et modifiable depuis la fiche (devis, BC, BL, facture) ; vide sinon. */
    public static Optional<NatureDocumentAchat> saisie(String docType) {
        return Arrays.stream(values()).filter(n -> n.gestion != null && n.name().equals(docType)).findFirst();
    }

    public static boolean saisissable(String docType) {
        return saisie(docType).isPresent();
    }
}
