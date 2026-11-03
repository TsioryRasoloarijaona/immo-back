package com.tsix.immocrm.domain.administration;

import java.util.Arrays;
import java.util.List;

/**
 * Catégorie normalisée d'une pièce ({@code AdmAttachedFile.Category}, en partie texte libre) : classes OMRAN regroupées
 * et écran Arpent de l'objet rattaché. Voir FEATURE-admin-fichiers-attaches.md « Base ».
 */
public enum CategorieFichierAttache {
    DOSSIER(Constantes.PREFIXE_DOSSIER, "dossier", "Yes.Com.Sales.Sale"),
    ARTICLE("Article", "article", "Yes.Stk.Building.Item"),
    ENCAISSEMENT("Encaissement", "reglement", "Yes.Stl.Credit.Settlement"),
    DECAISSEMENT("Décaissement", null, "Yes.Stl.Debit.Settlement"),
    FACTURE_ACHAT("Facture d'achat", null, "Yes.Stl.Debit.Invoice"),
    ACHAT("Achat", null, "Yes.Buy.Order", "Yes.Buy.Delivery", "Yes.Buy.CallOfTender.BuySelection"),
    EMAIL("E-mail", null, "YesBasics.History.AdmEMailHistory"),
    DOCUMENT_VENTE("Document de vente", null, "Yes.Com.Sales.ComDocument"),
    RESSOURCES_HUMAINES("Ressources humaines", null, "Yes.Grh.GrhVacation", "Yes.Grh.GrhEmployee"),
    PROSPECTION_FONCIERE("Prospection foncière", null, "Yes.Lnd.Prospection"),
    AUTRE("Autre", null);

    /** OMRAN range aussi des pièces de dossier sous le libellé « Dossier de vente N° … du … » au lieu de la classe. */
    public static final String PREFIXE_DOSSIER = Constantes.PREFIXE_DOSSIER;

    /** Un enum ne peut pas lire ses propres constantes dans ses arguments : la valeur vit ici. */
    private static final class Constantes {
        static final String PREFIXE_DOSSIER = "Dossier de vente";
    }

    private final String libelle;
    private final String objetCle;
    private final List<String> classes;

    CategorieFichierAttache(String libelle, String objetCle, String... classes) {
        this.libelle = libelle;
        this.objetCle = objetCle;
        this.classes = List.of(classes);
    }

    public String libelle() {
        return libelle;
    }

    /** Écran Arpent de l'objet rattaché ({@code dossier}, {@code article}, {@code reglement}) ou null. */
    public String objetCle() {
        return objetCle;
    }

    public List<String> classes() {
        return classes;
    }

    public static List<String> toutesLesClasses() {
        return Arrays.stream(values()).flatMap(c -> c.classes.stream()).toList();
    }

    public static CategorieFichierAttache de(String category) {
        if (category == null) {
            return AUTRE;
        }
        if (category.startsWith(PREFIXE_DOSSIER)) {
            return DOSSIER;
        }
        return Arrays.stream(values()).filter(c -> c.classes.contains(category)).findFirst().orElse(AUTRE);
    }
}
