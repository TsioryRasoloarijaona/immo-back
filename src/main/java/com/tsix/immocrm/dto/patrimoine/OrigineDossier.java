package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nature du dossier : vente (ComFolder), location (RntFolder) ou syndic (SynFolder).")
public enum OrigineDossier {
    VENTE, LOCATION, SYNDIC
}
