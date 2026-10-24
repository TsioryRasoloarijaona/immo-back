package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.domain.LignesListeDossiers;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
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
        LignesListeDossiers lignes,
        @Parameter(description = "Total à payer minimum (inclus, ≥ 0) ; un dossier sans total est alors exclu.",
                example = "100000") BigDecimal totalMin,
        @Parameter(description = "Total à payer maximum (inclus, ≥ 0, ≥ totalMin) ; un dossier sans total est exclu.",
                example = "2000000") BigDecimal totalMax,
        @Parameter(description = "Id de la société (CfgCompany.id) du projet de l'article principal ; inconnue = vide.")
        UUID societeId,
        @Parameter(description = "Ajoute les dossiers sans statut au filtre statut ; seul, ne rend que ceux-là.",
                example = "false", schema = @Schema(type = "boolean", defaultValue = "false")) Boolean sansStatut) {

    public FiltresListeDossiers {
        inclureSortis = Boolean.TRUE.equals(inclureSortis);
        lignes = lignes != null ? lignes : LignesListeDossiers.DOSSIERS;
        sansStatut = Boolean.TRUE.equals(sansStatut);
    }

    /** Tous les dossiers, sortis compris, sans autre filtre que la tranche si elle est donnée. */
    public static FiltresListeDossiers tousStatuts(List<UUID> trancheId) {
        return new FiltresListeDossiers(null, null, null, trancheId, null, null, null, null, null,
                true, LignesListeDossiers.DOSSIERS, null, null, null, null);
    }

    /** Assiette du tableau de bord par tranche : tous les dossiers de la tranche, sortis compris. */
    public static FiltresListeDossiers deTranche(UUID trancheId) {
        return tousStatuts(List.of(trancheId));
    }

    /** Fourchette de total à payer : bornes positives et ordonnées, sinon 400 nommant les champs. */
    void exigerFourchetteTotal() {
        if (estNegatif(totalMin) || estNegatif(totalMax)) {
            throw new InvalidRequestException("totalMin et totalMax doivent être positifs ou nuls");
        }
        if (totalMin != null && totalMax != null && totalMin.compareTo(totalMax) > 0) {
            throw new InvalidRequestException(
                    "totalMin (%s) doit être inférieur ou égal à totalMax (%s)".formatted(totalMin, totalMax));
        }
    }

    boolean filtreLeTotal() {
        return totalMin != null || totalMax != null;
    }

    private static boolean estNegatif(BigDecimal borne) {
        return borne != null && borne.signum() < 0;
    }
}
