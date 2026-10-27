package com.tsix.immocrm.dto.commission;

import com.tsix.immocrm.domain.commission.TypeCommission;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Corps et filtres de l'écran Commissions. */
public final class DemandesCommission {

    public static final int MAX_IDS = 500;

    private DemandesCommission() {
    }

    @Schema(description = "Génération d'un type de commission pour des dossiers de la tranche.")
    public record GenererCommissionsRequest(@NotNull TypeCommission type,
                                            @Schema(description = "Dossiers de la tranche du chemin, 500 au plus.")
                                            @NotNull @Size(max = MAX_IDS) List<UUID> dossierIds) {
    }

    @Schema(description = "Nouveau montant : positif, ou négatif pour un désistement ; zéro refusé.")
    public record ModifierMontantCommissionRequest(@NotNull BigDecimal montant) {
    }

    @Schema(description = "Lignes de commission visées, 500 au plus.")
    public record LignesCommissionRequest(@NotNull @Size(max = MAX_IDS) List<UUID> ligneIds) {
    }

    @Schema(description = "Droits de l'appelant sur l'écran Commissions, lus dans AdmRight à l'appel.")
    public record DroitsCommissionDto(@Schema(description = "Générer, modifier un montant, document à payer.") boolean ecriture,
                                      @Schema(description = "Supprimer des lignes.") boolean suppression,
                                      @Schema(description = "Exporter.") boolean impression) {
    }

    /** Filtres des onglets Dossiers et Commissions ; absent = pas de filtre. */
    public record FiltresCommission(
            @Parameter(description = "Type : dossiers ayant la date du type, ou lignes du type.")
            TypeCommission type,
            @Parameter(description = "Date du type (ou de la ligne) à partir de ce jour inclus.", example = "2024-01-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date du type (ou de la ligne) jusqu'à ce jour inclus.", example = "2024-12-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = "Commercial, gestionnaire ou gestionnaire 2 du dossier ; pour une ligne, la personne.")
            UUID commercialId,
            @Parameter(description = "Partie du N° de dossier.") String dossier) {
    }
}
