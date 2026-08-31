package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * All fields needed to render the "Fiche Prospection" document (PDF), modeled on the legacy
 * "FICHE" (Yes.Com.Front.Prospection.Prospection) Stimulsoft template — a printable summary of a
 * prospection. Reuses the same field set as {@code GET /api/prospections/{id}} — most of this
 * document's content is already exposed via {@code ProspectionDetailDto}, so
 * {@code ProspectionFicheService} sources it the same way rather than re-deriving. The "PROFIL"
 * section (ComProspectionProfile/Category/Type) seen in the real template is not reproduced here
 * — out of scope for this pass, no dedicated read path for it exists yet on the prospection
 * detail. Not exposed as JSON — feeds {@code ProspectionFichePdfRenderer} only.
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
