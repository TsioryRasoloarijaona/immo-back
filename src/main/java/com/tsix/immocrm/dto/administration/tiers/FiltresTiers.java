package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.UUID;

/** Filtres de la liste d'une famille de tiers, tous facultatifs. */
public record FiltresTiers(
        @Parameter(description = "Contient, sur code, description, raison sociale, mobile, téléphone, e-mail, ICE.")
        String recherche,
        @Parameter(description = "Nature (CfgTierKind) ; absent = toutes, ignoré pour contacts.") UUID natureId) {
}
