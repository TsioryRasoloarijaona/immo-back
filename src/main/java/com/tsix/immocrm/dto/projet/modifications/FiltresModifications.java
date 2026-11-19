package com.tsix.immocrm.dto.projet.modifications;

import com.tsix.immocrm.domain.EtatModification;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres de la liste Modifications ; absent = pas de filtre, la liste reste limitée aux tranches habilitées. */
public record FiltresModifications(
        @Parameter(description = "Tranche (CfgTranche.PKey) ; non habilitée = liste vide ; absente = toutes les habilitées.")
        UUID trancheId,
        @Parameter(description = "États cochés ; vide = tous les états.") List<EtatModification> etats,
        @Parameter(description = "Date de demande minimale (incluse).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date de demande maximale (incluse).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Description, notes, code du bien ou acquéreur (contient).") String recherche) {

    public FiltresModifications {
        etats = etats == null ? List.of() : etats;
    }

    /** Aucun filtre : sert à relire une seule demande avec la même règle de visibilité que la liste. */
    public static FiltresModifications aucun() {
        return new FiltresModifications(null, null, null, null, null);
    }
}
