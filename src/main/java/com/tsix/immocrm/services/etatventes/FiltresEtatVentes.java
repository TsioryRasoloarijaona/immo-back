package com.tsix.immocrm.services.etatventes;

import com.tsix.immocrm.domain.ChampDateDossier;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres communs à la synthèse, à la liste, aux groupes, aux totaux et à l'export de l'état des ventes. */
public record FiltresEtatVentes(
        @Parameter(description = "Id(s) de tranche (CfgTranche.id), répétable.") List<UUID> trancheId,
        @Parameter(description = "Id du projet (CfgProject.id) de la tranche.") UUID projetId,
        @Parameter(description = "Code(s) de statut : LIBRE, SANS_STATUT ou StrStatus.Code ; inconnu = 400.",
                example = "LIBRE") List<String> statut,
        @Parameter(description = "Id(s) de catégorie d'article (StkItemCategory.id), répétable.") List<UUID> categorieId,
        @Parameter(description = "Id(s) de type d'article (StkItemType.id), répétable.") List<UUID> typeId,
        @Parameter(description = "TOUS (défaut), ARTICLES (IsUnderItem = 0) ou SOUS_ARTICLES (IsUnderItem = 1).")
        Nature nature,
        @Parameter(description = "Sous-chaîne du code article, de la catégorie, du type, du dossier, du nom, téléphone "
                + "ou CIN du client.") String search,
        @Parameter(description = "Date du dossier filtrée par du / au ; défaut RESERVATION.", example = "RESERVATION")
        ChampDateDossier champDate,
        @Parameter(description = "Borne basse incluse, ISO.", example = "2025-03-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Borne haute incluse, ISO.", example = "2025-03-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {

    public enum Nature { TOUS, ARTICLES, SOUS_ARTICLES }

    public FiltresEtatVentes {
        nature = nature != null ? nature : Nature.TOUS;
        champDate = champDate != null ? champDate : ChampDateDossier.RESERVATION;
    }

    public static FiltresEtatVentes synthese(List<UUID> trancheId, UUID projetId, List<UUID> categorieId) {
        return new FiltresEtatVentes(trancheId, projetId, null, categorieId, null, null, null, null, null, null);
    }
}
