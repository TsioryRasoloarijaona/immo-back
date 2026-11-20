package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.DatesDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.DemandeAchatFiche;
import com.tsix.immocrm.dto.achat.demandes.ElementDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.FiltresDemandesAchat;
import com.tsix.immocrm.dto.achat.demandes.PhasesDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.StatutDemandeAchat;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.achat.RequetesDocumentsAchat;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.workflow.MoteurWorkflowStr;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Liste et fiche des demandes internes d'achat : une seule requête, société et projet visibles (D5) liés en SQL. */
@Component
class LectureDemandesAchat {

    /** Les 14 clés de {@code GetDatesFilter} ; les 5 dernières sont lues sur le lot. */
    static final Set<String> CHAMPS_DATE = Set.of("DocDate", "LaunchOfTenderDate", "WithdrawalTenderDocumentsDate",
            "TenderMaturityDate", "TendersResortDate", "BidOpeningDate", "TendersReportDate", "TenderDate", "ContractSignatureDate",
            "ServiceOrderDate", "WorkLaunchDate", "WorkCompletionDate", "ReattachmentDate", "CancellationDate");

    private static final String DATE_FILTREE = """
            CAST(CASE :champ WHEN 'DocDate' THEN d.DocDate WHEN 'LaunchOfTenderDate' THEN d.LaunchOfTenderDate
             WHEN 'WithdrawalTenderDocumentsDate' THEN d.WithdrawalTenderDocumentsDate WHEN 'TenderMaturityDate' THEN d.TenderMaturityDate
             WHEN 'TendersResortDate' THEN d.TendersResortDate WHEN 'BidOpeningDate' THEN d.BidOpeningDate
             WHEN 'TendersReportDate' THEN d.TendersReportDate WHEN 'TenderDate' THEN d.TenderDate
             WHEN 'ContractSignatureDate' THEN d.ContractSignatureDate WHEN 'ServiceOrderDate' THEN m.ServiceOrderDate
             WHEN 'WorkLaunchDate' THEN m.WorkLaunchDate WHEN 'WorkCompletionDate' THEN m.WorkCompletionDate
             WHEN 'ReattachmentDate' THEN m.ReattachmentDate WHEN 'CancellationDate' THEN m.CancellationDate END AS date)""";

    private static final String DEMANDES = """
            SELECT d.PKey, d.Code, d.DocDate, d.Description, d.DescriptionLong, d.DefaultVat, d.PriceEstimatedHT, d.PriceEstimatedTTC,
             d.PaymentDelay, d.Notes, d.TextLetterSelection, d.IsTheLast, d.LaunchOfTenderDate, d.WithdrawalTenderDocumentsDate,
             d.TenderMaturityDate, d.TendersResortDate, d.BidOpeningDate, d.TendersReportDate, d.TenderDate, d.ContractSignatureDate,
             s.PKey AS StatutId, s.Description AS StatutLib, s.ColorName, s.StrStatusTypeId, s.BoolField001, s.BoolField002,
             s.BoolField003, s.BoolField004, k.IsConfidential, m.IsMultiSupplier, k.IsMultiSupplier AS ProcedureMulti,
             k.PKey AS ProcedureId, k.Code AS ProcedureCode, k.Description AS ProcedureLib,
             c.PKey AS SocieteId, c.Code AS SocieteCode, c.Description AS SocieteLib,
             p.PKey AS ProjetId, p.Code AS ProjetCode, p.Description AS ProjetLib,
             m.PKey AS LotId, m.Code AS LotCode, m.Description AS LotLib,
             mt.PKey AS TypeLotId, mt.Code AS TypeLotCode, mt.Description AS TypeLotLib,
             sc.PKey AS CategorieId, sc.Code AS CategorieCode, sc.Description AS CategorieLib,
             cu.PKey AS DeviseId, cu.Code AS DeviseCode, cu.Description AS DeviseLib,
             am.PKey AS ModuleId, am.Code AS ModuleCode, am.Description AS ModuleLib,
             tr.PKey AS DeclencheurId, tr.Code AS DeclencheurCode, COALESCE(tr.SocialReason, tr.Description) AS DeclencheurLib,
             t.PKey AS FournisseurId, t.Code AS FournisseurCode, COALESCE(t.SocialReason, t.Description) AS FournisseurLib,
             st.PKey AS PaiementId, st.Code AS PaiementCode, st.Description AS PaiementLib
            FROM BuySelection d
            LEFT JOIN StrStatus s ON s.PKey = d.StrStatusId
            LEFT JOIN BuySelectionKind k ON k.PKey = d.BuySelectionKindId
            LEFT JOIN CfgCompany c ON c.PKey = d.CfgCompanyId
            LEFT JOIN PrjProject p ON p.PKey = d.PrjProjectId
            LEFT JOIN PrjMarket m ON m.PKey = d.PrjMarketId
            LEFT JOIN PrjMarketType mt ON mt.PKey = d.PrjMarketTypeId
            LEFT JOIN StlCategory sc ON sc.PKey = d.StlCategoryId
            LEFT JOIN StlCurrency cu ON cu.PKey = d.StlCurrencyId
            LEFT JOIN AdmModule am ON am.PKey = d.AdmModuleId
            LEFT JOIN CfgTier tr ON tr.PKey = d.CfgTierTriggerId
            LEFT JOIN CfgTier t ON t.PKey = d.CfgTierId
            LEFT JOIN StlSettlementType st ON st.PKey = d.StlSettlementTypeId
            WHERE\s""" + RequetesDocumentsAchat.SOCIETE_PROJET_VISIBLES;

    private static final String FICHE = DEMANDES + " AND d.PKey = :id";

    private static final String LISTE = DEMANDES + """
             AND (:procedure IS NULL OR d.BuySelectionKindId = :procedure)
             AND (:module IS NULL OR d.AdmModuleId = :module)
             AND (:tousStatuts = 1 OR d.StrStatusId IN (:statuts))
             AND (:sansProjetSeul = 0 OR d.PrjProjectId IS NULL)
             AND (:du IS NULL OR\s""" + DATE_FILTREE + " >= :du) AND (:au IS NULL OR " + DATE_FILTREE + """
             <= :au)
             AND (:fournisseur IS NULL OR COALESCE(t.SocialReason, t.Description) LIKE :fournisseur)
             AND (:code IS NULL OR d.Code LIKE :code)
             AND (:montantMin IS NULL OR d.PriceEstimatedTTC >= :montantMin)
             AND (:montantMax IS NULL OR d.PriceEstimatedTTC <= :montantMax)
             AND (:recherche IS NULL OR d.Description LIKE :recherche OR d.Notes LIKE :recherche)
            ORDER BY d.DocDate DESC, d.Code DESC""";

    private final NamedParameterJdbcTemplate jdbc;

    LectureDemandesAchat(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<DemandeAchatFiche> fiche(UUID id, PerimetreAchat perimetre) {
        return jdbc.query(FICHE, RequetesDocumentsAchat.parametres(perimetre).addValue("id", id.toString(), Types.VARCHAR),
                LectureDemandesAchat::fiche).stream().findFirst();
    }

    /** Champ de date inconnu → 400. */
    List<DemandeAchatFiche> liste(PerimetreAchat perimetre, FiltresDemandesAchat f) {
        if (!CHAMPS_DATE.contains(f.champDate())) {
            throw new InvalidRequestException("champDate : une des clés " + CHAMPS_DATE.stream().sorted().toList());
        }
        MapSqlParameterSource p = RequetesDocumentsAchat.parametres(perimetre)
                .addValue("procedure", f.procedureId() == null ? null : f.procedureId().toString(), Types.VARCHAR)
                .addValue("module", f.moduleId() == null ? null : f.moduleId().toString(), Types.VARCHAR)
                .addValue("tousStatuts", f.statutIds().isEmpty() ? 1 : 0)
                .addValue("statuts", f.statutIds().isEmpty() ? List.of(new UUID(0, 0).toString()) : f.statutIds().stream().map(UUID::toString).toList())
                .addValue("sansProjetSeul", Boolean.TRUE.equals(f.sansProjet()) && f.projetIds().isEmpty() ? 1 : 0)
                .addValue("champ", f.champDate(), Types.VARCHAR)
                .addValue("du", f.du() == null ? null : Date.valueOf(f.du()), Types.DATE)
                .addValue("au", f.au() == null ? null : Date.valueOf(f.au()), Types.DATE)
                .addValue("fournisseur", contient(f.fournisseur()), Types.NVARCHAR)
                .addValue("code", contient(f.code()), Types.NVARCHAR)
                .addValue("montantMin", f.montantMin(), Types.DECIMAL)
                .addValue("montantMax", f.montantMax(), Types.DECIMAL)
                .addValue("recherche", contient(f.recherche()), Types.NVARCHAR);
        return jdbc.query(LISTE, p, LectureDemandesAchat::fiche);
    }

    private static String contient(String texte) {
        return texte == null || texte.isBlank() ? null : "%" + texte.trim() + "%";
    }

    private static DemandeAchatFiche fiche(ResultSet rs, int rang) throws SQLException {
        boolean derniere = rs.getBoolean("IsTheLast");
        boolean lotMulti = rs.getBoolean("IsMultiSupplier");
        String statut = rs.getString("StatutId");
        boolean offres = derniere && rs.getBoolean("BoolField003");
        boolean selection = derniere && rs.getBoolean("BoolField004");
        boolean commandeParLot = lotMulti || rs.getString("LotId") == null;
        PhasesDemandeAchat phases = new PhasesDemandeAchat(derniere && rs.getBoolean("BoolField001"),
                derniere && rs.getBoolean("BoolField002"), offres, selection && !lotMulti,
                selection && rs.getBoolean("ProcedureMulti") && commandeParLot, offres || selection);
        DatesDemandeAchat dates = new DatesDemandeAchat(jour(rs, "LaunchOfTenderDate"), jour(rs, "WithdrawalTenderDocumentsDate"),
                jour(rs, "TenderMaturityDate"), jour(rs, "TendersResortDate"), jour(rs, "BidOpeningDate"),
                jour(rs, "TendersReportDate"), jour(rs, "TenderDate"), jour(rs, "ContractSignatureDate"));
        boolean nouveau = MoteurWorkflowStr.TYPE_NOUVEAU.toString().equalsIgnoreCase(rs.getString("StrStatusTypeId"));
        return new DemandeAchatFiche(UUID.fromString(rs.getString("PKey")), rs.getString("Code"), jour(rs, "DocDate"),
                rs.getString("Description"), rs.getString("DescriptionLong"), element(rs, "Procedure"),
                statut == null ? null : new StatutDemandeAchat(UUID.fromString(statut), rs.getString("StatutLib"),
                        rs.getString("ColorName")),
                element(rs, "Societe"), element(rs, "Projet"), element(rs, "Lot"), element(rs, "TypeLot"),
                element(rs, "Categorie"), element(rs, "Devise"), element(rs, "Module"), element(rs, "Declencheur"),
                element(rs, "Fournisseur"), rs.getBigDecimal("DefaultVat"), rs.getBigDecimal("PriceEstimatedHT"),
                rs.getBigDecimal("PriceEstimatedTTC"), element(rs, "Paiement"), rs.getObject("PaymentDelay", Integer.class), dates,
                rs.getString("Notes"), rs.getString("TextLetterSelection"), derniere, nouveau && derniere, phases,
                rs.getBoolean("IsConfidential"), lotMulti);
    }

    private static ElementDemandeAchat element(ResultSet rs, String prefixe) throws SQLException {
        String id = rs.getString(prefixe + "Id");
        return id == null ? null
                : new ElementDemandeAchat(UUID.fromString(id), rs.getString(prefixe + "Code"), rs.getString(prefixe + "Lib"));
    }

    private static LocalDate jour(ResultSet rs, String colonne) throws SQLException {
        Timestamp t = rs.getTimestamp(colonne);
        return t == null ? null : t.toLocalDateTime().toLocalDate();
    }
}
