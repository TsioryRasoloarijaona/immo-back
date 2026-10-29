package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un article racine en vente (FEATURE-etat-des-ventes.md R1–R5), base unique de toutes les sorties. Dossier annulé ou
 * désisté : {@code dossier} NULL, donc Libre ; racine {@code IsUnderItem = 1} libre absente (FEATURE-etat-ventes-omran.md §1.1).
 * Prix : colonne du dossier, ou formule de la branche libre de {@code ComSaleMonitoringView} (FEATURE-etat-des-ventes-parite.md §3.2).
 */
@Entity
@Immutable
@Synchronize({"StkItem", "CfgTranche", "StkVocation", "ComFolder", "StrStatus"})
@Subselect("SELECT i.PKey AS ArticleId, i.Code AS ArticleCode, i.Description AS ArticleLibelle, "
        + "i.StkHierarchyId AS TrancheId, i.StkItemCategoryId AS CategorieId, i.StkItemTypeId AS TypeId, "
        + "i.StkFloorId AS EtageId, i.StkBuildingId AS ImmeubleId, i.IsUnderItem AS SousArticle, i.Area AS Surface, "
        + "f.PKey AS DossierId, "
        + "CASE WHEN f.PKey IS NULL THEN '" + StkItemEtatVentes.LIBRE + "' WHEN s.PKey IS NULL THEN '"
        + StkItemEtatVentes.SANS_STATUT + "' ELSE s.Code END AS StatutCode, "
        + "CASE WHEN f.PKey IS NULL THEN i.ComSalePrice ELSE f.Price END AS PrixVenteTTC, "
        + "CASE WHEN f.PKey IS NULL THEN i.SalePrice / (1 + i.Vat) ELSE f.ItemPriceAfterHT END AS PrixArticleHt, "
        + "CASE WHEN f.PKey IS NULL THEN i.SalePrice ELSE f.ItemPriceAfter END AS PrixArticleTtc, "
        + "CASE WHEN f.PKey IS NULL THEN (i.ComSalePrice - i.SalePrice) / (1 + i.Vat) ELSE f.UnderItemsPriceAfterHT END "
        + "AS PrixSousArticlesHt, "
        + "CASE WHEN f.PKey IS NULL THEN i.ComSalePrice - i.SalePrice ELSE f.UnderItemsPriceAfter END "
        + "AS PrixSousArticlesTtc, "
        + "CASE WHEN f.PKey IS NULL THEN i.ComSalePriceHT ELSE f.PriceHT END AS PrixHt, "
        + "CASE WHEN f.PKey IS NULL THEN 0 ELSE f.DiscountAmount END AS RemiseMontant, "
        + "CASE WHEN f.PKey IS NULL THEN i.ComSalePrice ELSE f.AgreementPrice END AS PrixContractuel "
        + "FROM StkItem i "
        + "JOIN CfgTranche t ON t.PKey = i.StkHierarchyId "
        + "JOIN StkVocation v ON v.PKey = i.StkVocationId "
        + "LEFT JOIN (ComFolder f LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId) ON f.PKey = i.ComFolderId "
        + "AND ISNULL(s.BoolField002, 0) = 0 AND ISNULL(s.BoolField003, 0) = 0 "
        + "WHERE i.ParentId IS NULL AND v.Code = 'VENTE' AND t.IsForSale = 1 "
        + "AND (ISNULL(i.IsUnderItem, 0) = 0 OR f.PKey IS NOT NULL)")
@Getter
public class StkItemEtatVentes {

    public static final String LIBRE = "LIBRE";
    public static final String SANS_STATUT = "SANS_STATUT";

    @Id
    @Column(name = "ArticleId")
    private UUID articleId;

    @Column(name = "ArticleCode")
    private String articleCode;

    @Column(name = "ArticleLibelle")
    private String articleLibelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TrancheId")
    private CfgTranche tranche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategorieId")
    private StkItemCategory categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TypeId")
    private StkItemType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EtageId")
    private StkFloor etage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ImmeubleId")
    private StkBuilding immeuble;

    /** {@code StkItem.IsUnderItem} : vrai pour un parking autonome (sans parent), compté comme OMRAN. */
    @Column(name = "SousArticle")
    private Boolean sousArticle;

    @Column(name = "Surface", precision = 26, scale = 6)
    private BigDecimal surface;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DossierId")
    private ComFolder dossier;

    @Column(name = "StatutCode")
    private String statutCode;

    @Column(name = "PrixVenteTTC", precision = 26, scale = 6)
    private BigDecimal prixVenteTTC;

    @Column(name = "PrixArticleHt", precision = 26, scale = 6)
    private BigDecimal prixArticleHt;

    @Column(name = "PrixArticleTtc", precision = 26, scale = 6)
    private BigDecimal prixArticleTtc;

    @Column(name = "PrixSousArticlesHt", precision = 26, scale = 6)
    private BigDecimal prixSousArticlesHt;

    @Column(name = "PrixSousArticlesTtc", precision = 26, scale = 6)
    private BigDecimal prixSousArticlesTtc;

    @Column(name = "PrixHt", precision = 26, scale = 6)
    private BigDecimal prixHt;

    @Column(name = "RemiseMontant", precision = 26, scale = 6)
    private BigDecimal remiseMontant;

    @Column(name = "PrixContractuel", precision = 26, scale = 6)
    private BigDecimal prixContractuel;
}
