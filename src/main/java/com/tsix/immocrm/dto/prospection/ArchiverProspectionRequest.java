package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/** Au moins un de {@code raisonId}/{@code raisonTexte} est requis. */
public record ArchiverProspectionRequest(
        UUID raisonId,
        String raisonTexte
) {
}
