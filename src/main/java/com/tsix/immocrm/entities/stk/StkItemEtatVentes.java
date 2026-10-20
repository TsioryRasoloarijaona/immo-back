package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.domain.EtatVente;
import com.tsix.immocrm.domain.FamilleArticle;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Une ligne de l'état des ventes = un article racine en vente (FEATURE-etat-des-ventes.md R1–R3), base unique de la
 * synthèse, de la liste, des groupes et des totaux (R5). Dossier annulé ou désisté : {@code dossier} NULL, donc Libre.
 */
@Entity
@Immutable
@Synchronize({"StkItem", "CfgTranche", "StkVocation", "ComFolder", "StrStatus", "StkItemCategory"})
@Subselect("SELECT i.PKey AS ArticleId, i.Code AS ArticleCode, i.Description AS ArticleLibelle, "
        + "i.StkHierarchyId AS TrancheId, i.StkItemCategoryId AS CategorieId, i.StkItemTypeId AS TypeId, "
        + "i.StkFloorId AS EtageId, i.StkBuildingId AS ImmeubleId, i.IsUnderItem AS SousArticle, i.Area AS Surface, "
        + "f.PKey AS DossierId, "
        + "CASE WHEN f.PKey IS NULL THEN '" + StkItemEtatVentes.LIBRE + "' WHEN s.PKey IS NULL THEN '"
        + StkItemEtatVentes.SANS_STATUT + "' ELSE s.Code END AS StatutCode, "
        + "CASE WHEN f.PKey IS NULL THEN 'LIBRE' WHEN s.PKey IS NULL OR s.Code = '" + StatutDossier.CODE_RESERVATION
        + "' THEN 'RESERVE' ELSE 'VENDU' END AS EtatVente, "
        + "CASE c.Code WHEN 'APP' THEN 'APPARTEMENT' WHEN 'PAR' THEN 'PARKING' WHEN 'COM' THEN 'MAGASIN' "
        + "ELSE 'AUTRE' END AS Famille, "
        + "CASE WHEN f.PKey IS NULL THEN i.ComSalePrice ELSE f.Price END AS PrixVenteTTC "
        + "FROM StkItem i "
        + "JOIN CfgTranche t ON t.PKey = i.StkHierarchyId "
        + "JOIN StkVocation v ON v.PKey = i.StkVocationId "
        + "LEFT JOIN StkItemCategory c ON c.PKey = i.StkItemCategoryId "
        + "LEFT JOIN (ComFolder f LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId) ON f.PKey = i.ComFolderId "
        + "AND ISNULL(s.BoolField002, 0) = 0 AND ISNULL(s.BoolField003, 0) = 0 "
        + "WHERE i.ParentId IS NULL AND v.Code = 'VENTE' AND t.IsForSale = 1")
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

    /** Noms des constantes = valeurs écrites par le {@code CASE} du {@code @Subselect}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "EtatVente")
    private EtatVente etatVente;

    @Enumerated(EnumType.STRING)
    @Column(name = "Famille")
    private FamilleArticle famille;

    @Column(name = "PrixVenteTTC", precision = 26, scale = 6)
    private BigDecimal prixVenteTTC;
}
