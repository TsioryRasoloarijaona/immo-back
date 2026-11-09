package com.tsix.immocrm.domain.achat;

import java.util.Arrays;

/** Nature d'une pièce d'achat ({@code BuyDocument.DocType}), dans l'ordre d'affichage ; {@code DELIVRY} est l'orthographe OMRAN. */
public enum NatureDocumentAchat {
    INVOICE("Facture"),
    ORDER("Bon de commande"),
    QUOTE("Devis"),
    DOCUMENT_TO_PAY("Document à régler"),
    DELIVRY("Bon de livraison"),
    REFUND("Avoir");

    private final String libelle;

    NatureDocumentAchat(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /** Libellé d'un {@code DocType} stocké ; une valeur hors liste est rendue telle quelle. */
    public static String libelleDe(String docType) {
        return Arrays.stream(values()).filter(n -> n.name().equals(docType)).findFirst()
                .map(NatureDocumentAchat::libelle).orElse(docType);
    }
}
