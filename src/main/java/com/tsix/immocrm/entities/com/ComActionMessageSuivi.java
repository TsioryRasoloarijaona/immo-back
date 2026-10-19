package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.domain.InteractionKpi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une interaction attribuée à son auteur (FEATURE-suivi-commerciaux.md R1–R6, R9) : base unique de la synthèse, de la
 * liste, des groupes, des totaux, de l'export et des visites du mensuel. Auteur = compte, sinon le seul compte portant
 * le nom écrit ({@code UserName}), comme le KPI ; sans auteur résolu ou « Système », l'interaction est absente.
 */
@Entity
@Immutable
@Synchronize({"ComActionMessage", "ComActionMessageType", "ComActionMessageSubCategory", "AdmUser", "AdmEvent",
        "ComProspection", "ComFolder", "StkItem", "CfgTranche", "CfgProject", "CfgTier"})
@Subselect("SELECT m.PKey AS Id, CAST(m.DocDate AS date) AS Jour, m.DocDate AS DateSaisie, "
        + "ISNULL(u.CfgEmployeeId, u.PKey) AS CommercialCle, "
        + "ISNULL(NULLIF(LTRIM(RTRIM(u.Description)), ''), u.Code) AS CommercialNom, "
        + "mt.Code AS Type, sc.Code AS Resultat, "
        + "CAST(CASE WHEN mt.Code IN (N'" + InteractionKpi.TYPE_APPEL_SORTANT + "', N'"
        + InteractionKpi.TYPE_APPEL_ENTRANT + "') THEN CASE WHEN sc.Code IS NOT NULL AND sc.Code NOT IN (N'"
        + InteractionKpi.NON_JOINT_NE_REPOND_PAS + "', N'" + InteractionKpi.NON_JOINT_INJOIGNABLE + "', N'"
        + InteractionKpi.NON_JOINT_OCCUPE + "', N'" + InteractionKpi.NON_JOINT_NUMERO_ERRONE + "', N'"
        + InteractionKpi.NON_JOINT_CLIENT_INJOIGNABLE + "', N'" + InteractionKpi.NON_JOINT_CLIENT_NE_REPOND_PAS
        + "', N'" + InteractionKpi.NON_JOINT_A_RAPPELER + "') THEN 1 ELSE 0 END END AS bit) AS Joint, "
        + "ISNULL(m.CfgTierId, m.ComProspectionId) AS Personne, "
        + "COALESCE(NULLIF(t.SocialReason, ''), NULLIF(t.Description, ''), "
        + "NULLIF(LTRIM(RTRIM(ISNULL(t.FirstName, '') + ' ' + ISNULL(t.LastName, ''))), ''), p.Code) AS PersonneNom, "
        + "m.ComProspectionId AS ProspectionId, p.Code AS ProspectionCode, "
        + "ISNULL(tp.CfgProjectId, tf.CfgProjectId) AS ProjetId, ISNULL(prp.Description, prf.Description) AS Projet, "
        + "ISNULL(p.StkHierarchyId, i.StkHierarchyId) AS TrancheId, "
        + "CASE WHEN m.ComActionMessageCategoryId = '" + InteractionKpi.CATEGORIE_PROSPECTION + "' THEN '"
        + ComActionMessageSuivi.PROSPECTION + "' WHEN m.ComActionMessageCategoryId IN ('"
        + InteractionKpi.CATEGORIE_VENTE_RESERVATION + "', '" + InteractionKpi.CATEGORIE_VENTE_LIVRAISON + "', '"
        + InteractionKpi.CATEGORIE_VENTE_ANNULATION + "') THEN '" + ComActionMessageSuivi.VENTE + "' END AS Categorie, "
        + "CAST(ISNULL(e.StartHour, m.AlertValidityDate) AS date) AS JourPrevu, "
        + "CAST(CASE WHEN EXISTS (SELECT 1 FROM ComActionMessage v "
        + "JOIN ComActionMessageType vt ON vt.PKey = v.ComActionMessageTypeId "
        + "WHERE vt.Code = N'" + InteractionKpi.TYPE_VISITE + "' AND v.ComProspectionId = m.ComProspectionId "
        + "AND CAST(v.DocDate AS date) = CAST(ISNULL(e.StartHour, m.AlertValidityDate) AS date)) "
        + "THEN 1 ELSE 0 END AS bit) AS Consignee "
        + "FROM ComActionMessage m "
        + "OUTER APPLY (SELECT TOP 1 x.PKey FROM AdmUser x WHERE m.AdmUserId IS NULL "
        + "AND LTRIM(RTRIM(x.Description)) = LTRIM(RTRIM(m.UserName)) AND (SELECT COUNT(*) FROM AdmUser y "
        + "WHERE LTRIM(RTRIM(y.Description)) = LTRIM(RTRIM(m.UserName))) = 1) parNom "
        + "JOIN AdmUser u ON u.PKey = ISNULL(m.AdmUserId, parNom.PKey) "
        + "LEFT JOIN ComActionMessageType mt ON mt.PKey = m.ComActionMessageTypeId "
        + "LEFT JOIN ComActionMessageSubCategory sc ON sc.PKey = m.ComActionMessageSubCategoryId "
        + "LEFT JOIN AdmEvent e ON e.PKey = m.AdmEventId "
        + "LEFT JOIN ComProspection p ON p.PKey = m.ComProspectionId "
        + "LEFT JOIN CfgTranche tp ON tp.PKey = p.StkHierarchyId "
        + "LEFT JOIN CfgProject prp ON prp.PKey = tp.CfgProjectId "
        + "LEFT JOIN ComFolder f ON f.PKey = m.ComFolderId "
        + "LEFT JOIN StkItem i ON i.PKey = f.StkItemId "
        + "LEFT JOIN CfgTranche tf ON tf.PKey = i.StkHierarchyId "
        + "LEFT JOIN CfgProject prf ON prf.PKey = tf.CfgProjectId "
        + "LEFT JOIN CfgTier t ON t.PKey = ISNULL(m.CfgTierId, p.CfgProspectId) "
        + "WHERE m.UserName IS NULL OR m.UserName <> N'Système'")
@Getter
public class ComActionMessageSuivi {

    public static final String PROSPECTION = "PROSPECTION";
    public static final String VENTE = "VENTE";

    @Id
    @Column(name = "Id")
    private UUID id;

    /** Date de {@code DocDate}, déjà l'heure murale de Casablanca (§9.1). */
    @Column(name = "Jour")
    private LocalDate jour;

    @Column(name = "DateSaisie")
    private LocalDateTime dateSaisie;

    /** Employé du compte auteur, sinon le compte (§9). */
    @Column(name = "CommercialCle")
    private UUID commercialCle;

    @Column(name = "CommercialNom")
    private String commercialNom;

    @Column(name = "Type")
    private String type;

    @Column(name = "Resultat")
    private String resultat;

    /** Appels seulement (R2) ; {@code null} pour tout autre type. */
    @Column(name = "Joint")
    private Boolean joint;

    @Column(name = "Personne")
    private UUID personne;

    @Column(name = "PersonneNom")
    private String personneNom;

    @Column(name = "ProspectionId")
    private UUID prospectionId;

    @Column(name = "ProspectionCode")
    private String prospectionCode;

    @Column(name = "ProjetId")
    private UUID projetId;

    @Column(name = "Projet")
    private String projet;

    @Column(name = "TrancheId")
    private UUID trancheId;

    /** {@link #PROSPECTION}, {@link #VENTE} ou {@code null} selon la catégorie de l'interaction (R5). */
    @Column(name = "Categorie")
    private String categorie;

    /** {@code AdmEvent.StartHour}, à défaut {@code AlertValidityDate} (R6) ; lu seulement pour un résultat planifié. */
    @Column(name = "JourPrevu")
    private LocalDate jourPrevu;

    /** Une Visite existe sur la même prospection le jour prévu (R6). */
    @Column(name = "Consignee")
    private Boolean consignee;
}
