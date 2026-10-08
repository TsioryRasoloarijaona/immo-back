package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Données du PDF « Fiche Prospection » (modèle Stimulsoft « FICHE »), sourcées comme
 * {@code ProspectionDetailDto} ; la section « PROFIL » du modèle n'est pas reproduite. Pas exposé en JSON.
 */
public record ProspectionFicheData(
        String code,
        LocalDateTime dateDocument,
        String prospectNom,
        String commercialNom,
        String origine,
        String origineSupport,
        String actionCommerciale,
        String vocation,
        String projetNom,
        String articleCode,
        BigDecimal superficieDe,
        BigDecimal superficieJusqua,
        BigDecimal prixDe,
        BigDecimal prixJusqua,
        BigDecimal avance,
        BigDecimal fondsPropres,
        BigDecimal credit,
        LocalDateTime dateAlerte,
        String messageAlerte,
        Boolean fermee,
        String notes,
        List<Action> actions
) {
    public record Action(LocalDateTime date, String description, String actionType, String resultat) {
    }
}
