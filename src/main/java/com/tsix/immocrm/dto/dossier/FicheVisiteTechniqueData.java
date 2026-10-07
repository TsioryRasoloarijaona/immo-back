package com.tsix.immocrm.dto.dossier;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Données de la « Fiche Visite Technique » (check-list avant livraison), hors JSON. Les colonnes
 * Localisation/Date/Observations restent vides : remplies à la main, aucune table ne les porte.
 * Le texte arabe du modèle bilingue n'est pas repris.
 */
public record FicheVisiteTechniqueData(
        String dossierCode,
        LocalDateTime dateRdv,
        List<String> clientNoms,
        String articleCode,

        // -- Clause "Service Après-Vente" --
        String bureauSav,
        String contactSav,
        Integer dureeSavMois
) {
}
