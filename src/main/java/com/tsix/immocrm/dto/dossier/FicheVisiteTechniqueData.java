package com.tsix.immocrm.dto.dossier;

import java.time.LocalDateTime;
import java.util.List;

/**
 * All fields needed to render the "Fiche Visite Technique" document (PDF), modeled on the legacy
 * "FICHE VISITE TECHNIQUE" Stimulsoft template — the pre-delivery inspection checklist
 * ("FICHE D'INTERVENTION AVANT LIVRAISON") referenced by the "SERVICE APRES VENTE" clause of the
 * Acte de réservation. The checklist's Localisation/Date/Observations columns are intentionally
 * left blank in the rendered PDF — they're filled by hand during the physical inspection, no
 * table in the schema backs them. Arabic text present in the legacy bilingual template is not
 * reproduced — this app is French-only throughout. Not exposed as JSON — feeds
 * {@code FicheVisiteTechniquePdfRenderer} only.
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
