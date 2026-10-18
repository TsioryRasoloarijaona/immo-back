package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.domain.LignesListeDossiers;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Filtres communs à la liste, aux groupes et à l'export des dossiers, liés depuis la chaîne de requête :
 * une seule définition pour que tous portent sur la même population.
 *
 * @param dateVide     {@code true} = date de {@code champDate} vide, {@code false} = renseignée, null = période
 * @param inclureSortis sans lui, annulés et résiliés sont exclus sauf filtre {@code statut} explicite
 */
public record FiltresListeDossiers(
        @Parameter(example = "CONCRETISATION") List<String> statut,
        @Parameter(description = "Id du commercial (CfgTier.id, cfgCommercial).") UUID commercial,
        @Parameter(description = "Id du projet (CfgProject.id), résolu par la tranche de l'article principal.")
        UUID projet,
        List<UUID> trancheId,
        @Parameter(example = "CONCRETISATION") ChampDateDossier champDate,
        @Parameter(description = "Borne inférieure (incluse) sur champDate, ISO-8601.", example = "2026-01-01T00:00:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
        @Parameter(description = "Borne supérieure (incluse) sur champDate, ISO-8601.", example = "2026-12-31T23:59:59")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
        Boolean dateVide,
        String search,
        // Boolean : un boolean absent de la requête est refusé (400) par la liaison par constructeur.
        @Parameter(example = "false", schema = @Schema(type = "boolean", defaultValue = "false")) Boolean inclureSortis,
        // Sans @Parameter ici : celui de la méthode (exemple, ou masqué sur /groupes) ne s'appliquerait plus.
        LignesListeDossiers lignes) {

    public FiltresListeDossiers {
        inclureSortis = Boolean.TRUE.equals(inclureSortis);
        lignes = lignes != null ? lignes : LignesListeDossiers.DOSSIERS;
    }

    /** Assiette du tableau de bord par tranche : tous les dossiers de la tranche, sortis compris. */
    public static FiltresListeDossiers deTranche(UUID trancheId) {
        return new FiltresListeDossiers(null, null, null, List.of(trancheId), null, null, null, null, null,
                true, LignesListeDossiers.DOSSIERS);
    }
}
