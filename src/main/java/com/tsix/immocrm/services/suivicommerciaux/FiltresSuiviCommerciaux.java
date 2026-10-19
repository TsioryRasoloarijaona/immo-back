package com.tsix.immocrm.services.suivicommerciaux;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/** Filtres communs à la liste, aux groupes, aux totaux et à l'export du suivi des commerciaux. */
public record FiltresSuiviCommerciaux(
        @Parameter(description = "Borne basse incluse (jour de DocDate), ISO.", example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Borne haute incluse, ISO.", example = "2026-10-16")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Un seul jour (remplace du / au), ISO.", example = "2026-10-16")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate jour,
        @Parameter(description = "Clé du commercial (employé, sinon compte) ; hors de portée = 403.") UUID commercialCle,
        @Parameter(description = "true = appels joints, false = appels non joints ; les autres types n'y entrent pas.")
        Boolean joint,
        @Parameter(description = "Code du type d'interaction (ComActionMessageType.Code).", example = "Téléphone Sortant")
        String type,
        @Parameter(description = "Sous-chaîne du nom de la personne, du résultat ou du code de prospection.")
        String search) {

    public LocalDate debut() {
        return jour != null ? jour : du;
    }

    public LocalDate fin() {
        return jour != null ? jour : au;
    }
}
