package com.tsix.immocrm.dto.dossier;

import java.util.List;
import java.util.UUID;

/** Corps des {@code POST /api/dossiers/documents/{type}} : un seul PDF, documents concaténés dans l'ordre donné. */
public record GenererDocumentsLotRequest(
        List<UUID> dossierIds
) {
}
