package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.domain.PorteeCleApi;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Corps de {@code POST /api/cles-api} — {@code instructions/FEATURE-cle-api-n8n.md} §5.
 */
@Schema(description = "Création d'une clé d'API pour un client machine (n8n, agent).")
public record CreerCleApiRequest(
        @Schema(description = "Nom du client, unique, 100 caractères max — p.ex. `n8n`. Sert aussi "
                + "de `Code` et de `Description` au compte de service créé, donc d'auteur "
                + "(`UserCreate`) à tout ce que la clé écrit.", example = "n8n",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String nom,
        @Schema(description = "Portées accordées, au moins une. Une route hors de toutes les portées "
                + "est refusée en 403. Pour n8n : `PRE_PROSPECTIONS_ECRIRE` + `REFERENTIELS_LIRE`.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "[\"PRE_PROSPECTIONS_ECRIRE\", \"REFERENTIELS_LIRE\"]")
        List<PorteeCleApi> portees,
        @Schema(description = "Code d'un compte `AdmUser` **existant** à utiliser comme compte de "
                + "service. Absent : un compte est créé au nom de la clé, sans employé ni rôle, "
                + "donc sans restriction de périmètre — la portée est alors sa seule limite.",
                example = "n8n")
        String compteCode
) {
}
