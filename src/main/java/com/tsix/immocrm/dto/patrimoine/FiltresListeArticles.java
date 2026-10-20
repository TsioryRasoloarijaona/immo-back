package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.domain.EtatArticle;
import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.services.patrimoine.FiltresArticle;
import io.swagger.v3.oas.annotations.Parameter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Filtres de {@code GET /api/articles} et {@code GET /api/sous-articles}, liés depuis la chaîne de requête ; absent = pas de filtre. */
public record FiltresListeArticles(
        @Parameter(description = "Recherche sans casse ni espaces : code, description, code parent, tranche, N° dossier, notes…",
                example = "A-204") String search,
        @Parameter(description = "Id(s) de CfgTranche. Répétable, correspondance OR.") List<UUID> trancheId,
        @Parameter(description = "Disponibilité : dossier de vente ou de location de la ligne. Absent = tous.") StatutArticle statut,
        @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR.") List<UUID> categorieId,
        @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR.") List<UUID> typeId,
        @Parameter(description = "Id de CfgCompany (entité juridique), parmi les entités actives.") UUID entiteId,
        @Parameter(description = "Id de StkKind (nature du bien).") UUID natureId,
        @Parameter(description = "Id de StkVocation.") UUID vocationId,
        @Parameter(description = "Id de StkFloor (étage).") UUID etageId,
        @Parameter(description = "Borne basse (incluse) sur le prix de vente (StkItem.ComSalePrice).") BigDecimal prixDe,
        @Parameter(description = "Borne haute (incluse) sur le prix de vente (StkItem.ComSalePrice).") BigDecimal prixA,
        @Parameter(description = "Borne basse (incluse) sur la superficie (StkItem.Area).") BigDecimal surfaceDe,
        @Parameter(description = "Borne haute (incluse) sur la superficie (StkItem.Area).") BigDecimal surfaceA,
        @Parameter(description = "Sous-chaîne du code de l'article parent, espaces ignorés des deux côtés.", example = "B 12")
        String codeParent,
        @Parameter(description = "État OMRAN : dossier de vente ou de location de la ligne elle-même.") EtatArticle etat) {

    /** {@code sousArticlesSeuls} : {@code IsUnderItem = 1 AND IsUnderItemOrigin = 1} ; entités actives toujours. */
    public FiltresArticle versFiltres(boolean sousArticlesSeuls) {
        return new FiltresArticle(trancheId, null, false, statut, categorieId, typeId, search, true, entiteId, natureId,
                vocationId, etageId, prixDe, prixA, surfaceDe, surfaceA, sousArticlesSeuls, codeParent, etat);
    }
}
