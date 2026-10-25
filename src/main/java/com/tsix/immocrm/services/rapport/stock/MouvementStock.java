package com.tsix.immocrm.services.rapport.stock;

import java.time.LocalDate;
import java.util.UUID;

/** Un mouvement du stock d'une tranche en vente : +n entrée (lancement, retour), −1 sortie (concrétisation). */
public record MouvementStock(UUID tranche, String article, String dossier, String libelle, LocalDate date, long quantite,
                             String libelleTranche) {

    public static final String LANCEMENT = "LANCEMENT DE LA COMMERCIALISATION";

    public boolean lancement() {
        return LANCEMENT.equals(libelle);
    }
}
