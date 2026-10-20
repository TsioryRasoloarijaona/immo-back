package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
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
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une ligne du suivi des affectations = une {@code StlSettlementLine} vers une échéance de vente
 * (FEATURE-suivi-affectations.md R1–R4, R8), base unique de la liste, des groupes, des totaux et de l'export (R7).
 */
@Entity
@Immutable
@Synchronize({"StlSettlementLine", "StlSettlement", "ComDocument", "ComSettlementCategory", "ComFolder", "StrStatus",
        "StkItem", "CfgTranche", "CfgProject", "CfgCompany", "StlSettlementType", "StlAccount", "CfgTier"})
@Subselect("SELECT l.PKey AS Id, " + StatutReglement.SQL_MONTANT_SIGNE + " AS MontantAffecte, "
        + "CASE WHEN s.IsCredit = 0 THEN 'DEBIT' ELSE 'CREDIT' END AS Sens, "
        + "CAST(CASE WHEN s.IsCredit = 1 THEN 1 ELSE 0 END AS bit) AS PieceOuvrable, "
        + "ISNULL(s.SettlementDate, s.EcheanceDate) AS DateMouvement, "
        + "c.PKey AS CategorieId, c.Code AS CategorieCode, c.Description AS CategorieLibelle, "
        + "co.PKey AS EntiteId, co.Code AS EntiteCode, co.Description AS EntiteLibelle, "
        + "p.PKey AS ProjetId, p.Description AS ProjetLibelle, "
        + "t.PKey AS TrancheId, t.Code AS TrancheCode, t.Description AS TrancheLibelle, i.Code AS ArticleCode, "
        + "s.TierId AS TiersId, f.PKey AS DossierId, f.Code AS DossierCode, "
        + "CASE WHEN st.PKey IS NULL THEN '" + StlSettlementLineSuivi.SANS_STATUT + "' ELSE st.Code END "
        + "AS StatutDossierCode, "
        + "CASE WHEN st.PKey IS NULL THEN 'Sans statut' ELSE st.Description END AS StatutDossierLibelle, "
        + "s.PKey AS PieceId, s.Description AS PieceLibelle, s.Code AS PieceReference, s.ReceiptCode AS RecuReference, "
        + "s.DocDate AS DatePiece, s.EcheanceDate AS DateEcheancePiece, s.SettlementDate AS DateOperationPiece, "
        + "ISNULL(s.Status, '" + StlSettlementLineSuivi.SANS_ETAT + "') AS EtatPiece, s.Amount AS MontantPiece, "
        + "ty.PKey AS TypePieceId, ty.Code AS TypePieceCode, ty.Description AS TypePieceLibelle, "
        + "s.CfgBankId AS BanqueId, a.Code AS CompteCode, a.Description AS CompteLibelle, s.Notes AS NotesPiece, "
        + "d.DocDate AS DateEcheance, d.Amount AS MontantEcheance, d.Notes AS NotesEcheance, "
        // R8 : un seul champ cherché, espaces ôtés comme la saisie ; '|' empêche une correspondance à cheval.
        + "REPLACE(CONCAT(co.Code, '|', co.Description, '|', p.Code, '|', p.Description, '|', t.Code, '|', "
        + "t.Description, '|', i.Code, '|', ti.SocialReason, '|', ti.Description, '|', ti.FirstName, '|', "
        + "ti.LastName, '|', c.Code, '|', c.Description, '|', f.Code, '|', st.Code, '|', st.Description, '|', "
        + "s.Description, '|', s.Code, '|', s.ReceiptCode, '|', s.Status, '|', ty.Code, '|', ty.Description, '|', "
        + "b.SocialReason, '|', b.Description, '|', a.Code, '|', a.Description, '|', s.Notes, '|', d.Notes), ' ', '') "
        + "AS Recherche "
        + "FROM StlSettlementLine l "
        + "JOIN StlSettlement s ON s.PKey = l.StlSettlementId "
        + "LEFT JOIN ComDocument d ON d.PKey = l.ComDocumentId "
        + "LEFT JOIN ComSettlementCategory c ON c.PKey = d.ComSettlementCategoryId "
        + "LEFT JOIN ComFolder f ON f.PKey = d.ComFolderId "
        + "LEFT JOIN StrStatus st ON st.PKey = f.StrStatusId "
        + "LEFT JOIN StkItem i ON i.PKey = f.StkItemId "
        + "LEFT JOIN CfgTranche t ON t.PKey = i.StkHierarchyId "
        + "LEFT JOIN CfgProject p ON p.PKey = t.CfgProjectId "
        + "LEFT JOIN CfgCompany co ON co.PKey = p.CfgCompanyId "
        + "LEFT JOIN StlSettlementType ty ON ty.PKey = s.StlSettlementTypeId "
        + "LEFT JOIN StlAccount a ON a.PKey = s.StlAccountId "
        + "LEFT JOIN CfgTier ti ON ti.PKey = s.TierId "
        + "LEFT JOIN CfgTier b ON b.PKey = s.CfgBankId "
        + "WHERE l.ComDocumentId IS NOT NULL AND ISNULL(t.IsForSale, 1) = 1")
@Getter
public class StlSettlementLineSuivi {

    public static final String SANS_STATUT = "SANS_STATUT";
    public static final String SANS_ETAT = "SANS_ETAT";

    @Id
    @Column(name = "Id")
    private UUID id;

    @Column(name = "MontantAffecte", precision = 26, scale = 6)
    private BigDecimal montantAffecte;

    /** {@code CREDIT} ou {@code DEBIT}. */
    @Column(name = "Sens")
    private String sens;

    /** Vrai seulement si {@code IsCredit = 1} : la fiche encaissement répond 404 pour un débit ou un NULL. */
    @Column(name = "PieceOuvrable")
    private Boolean pieceOuvrable;

    @Column(name = "DateMouvement")
    private LocalDateTime dateMouvement;

    @Column(name = "CategorieId")
    private UUID categorieId;

    @Column(name = "CategorieCode")
    private String categorieCode;

    @Column(name = "CategorieLibelle")
    private String categorieLibelle;

    @Column(name = "EntiteId")
    private UUID entiteId;

    @Column(name = "EntiteCode")
    private String entiteCode;

    @Column(name = "EntiteLibelle")
    private String entiteLibelle;

    @Column(name = "ProjetId")
    private UUID projetId;

    @Column(name = "ProjetLibelle")
    private String projetLibelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TrancheId")
    private CfgTranche tranche;

    @Column(name = "TrancheCode")
    private String trancheCode;

    @Column(name = "TrancheLibelle")
    private String trancheLibelle;

    @Column(name = "ArticleCode")
    private String articleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TiersId")
    private CfgTier tiers;

    @Column(name = "DossierId")
    private UUID dossierId;

    @Column(name = "DossierCode")
    private String dossierCode;

    @Column(name = "StatutDossierCode")
    private String statutDossierCode;

    @Column(name = "StatutDossierLibelle")
    private String statutDossierLibelle;

    @Column(name = "PieceId")
    private UUID pieceId;

    @Column(name = "PieceLibelle")
    private String pieceLibelle;

    @Column(name = "PieceReference")
    private String pieceReference;

    @Column(name = "RecuReference")
    private String recuReference;

    @Column(name = "DatePiece")
    private LocalDateTime datePiece;

    @Column(name = "DateEcheancePiece")
    private LocalDateTime dateEcheancePiece;

    @Column(name = "DateOperationPiece")
    private LocalDateTime dateOperationPiece;

    @Column(name = "EtatPiece")
    private String etatPiece;

    @Column(name = "MontantPiece", precision = 26, scale = 6)
    private BigDecimal montantPiece;

    @Column(name = "TypePieceId")
    private UUID typePieceId;

    @Column(name = "TypePieceCode")
    private String typePieceCode;

    @Column(name = "TypePieceLibelle")
    private String typePieceLibelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BanqueId")
    private CfgTier banque;

    @Column(name = "CompteCode")
    private String compteCode;

    @Column(name = "CompteLibelle")
    private String compteLibelle;

    @Column(name = "NotesPiece")
    private String notesPiece;

    @Column(name = "DateEcheance")
    private LocalDateTime dateEcheance;

    @Column(name = "MontantEcheance", precision = 26, scale = 6)
    private BigDecimal montantEcheance;

    @Column(name = "NotesEcheance")
    private String notesEcheance;

    @Column(name = "Recherche")
    private String recherche;
}
