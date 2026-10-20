package com.tsix.immocrm.dto.etatventes;

import com.tsix.immocrm.domain.FamilleArticle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Tableau tranche × famille en Libres / Réservés / Vendus, totaux et pourcentages calculés au serveur.")
public record SyntheseVendusLibresDto(
        @Schema(description = "Familles du filtre, ordre fixe : Appartements, Parkings, Magasins, Autres.")
        List<FamilleVendusLibres> familles,
        @Schema(description = "Une ligne par tranche × famille non vide, triées par code de tranche puis ordre de famille.")
        List<CompteVendusLibres> lignes,
        @Schema(description = "Toutes familles d'une tranche, même ordre que les lignes ; familleCode null.")
        List<CompteVendusLibres> totalTranche,
        @Schema(description = "Toutes tranches d'une famille, familles à total nul omises ; champs de tranche null.")
        List<CompteVendusLibres> totauxFamille,
        @Schema(description = "Total général du filtre ; champs de tranche et de famille null.")
        CompteVendusLibres general) {

    public record FamilleVendusLibres(
            @Schema(example = "APPARTEMENT") FamilleArticle code,
            @Schema(example = "Appartements") String libelle,
            @Schema(description = "Rang de la famille, croissant.") int ordre) {

        public static FamilleVendusLibres de(FamilleArticle famille) {
            return new FamilleVendusLibres(famille, famille.libelle(), famille.ordinal());
        }
    }

    @Schema(description = "Comptes d'articles racines ; libres + reserves + vendus = total.")
    public record CompteVendusLibres(
            @Schema(description = "Id de tranche (CfgTranche.id) ; null dans totauxFamille et general.") UUID trancheId,
            @Schema(description = "Code de tranche ; null dans totauxFamille et general.") String trancheCode,
            @Schema(description = "Libellé de tranche ; null dans totauxFamille et general.") String trancheLibelle,
            @Schema(description = "Famille ; null dans totalTranche et general.", example = "APPARTEMENT")
            FamilleArticle familleCode,
            @Schema(description = "Sans dossier actif (dossier annulé ou désisté compris).") long libres,
            @Schema(description = "Dossier en RESERVATION ou sans statut.") long reserves,
            @Schema(description = "Dossier à tout autre statut hors sortie.") long vendus,
            @Schema(description = "Dossiers sans statut, déjà comptés dans reserves.") long sansStatut,
            long total,
            @Schema(description = "Part des libres dans total, en %, 1 décimale ; 0 si total nul.", example = "33.3")
            double pctLibres,
            @Schema(description = "Part des réservés dans total, en %, 1 décimale ; 0 si total nul.") double pctReserves,
            @Schema(description = "Part des vendus dans total, en %, 1 décimale ; 0 si total nul.") double pctVendus) {
    }
}
