package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.tresorerie.ReferentielsDecaissementDto.CompteDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.ReferentielsDecaissementDto.SocieteDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.ReferentielsDecaissementDto.TypeDecaissementDto;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Lectures de la saisie d'un décaissement (FEATURE-n3bis-saisie-reglements.md) : pièces visibles, unicité, liens. */
@Component
public class RequetesSaisieDecaissement {

    /** Pièce d'achat visible, avec son réglé hors annulés et hors le règlement en cours. */
    public record PieceSaisie(UUID id, String code, LocalDate dateDocument, LocalDate dateEcheance, String description,
                              UUID fournisseurId, UUID societeId, UUID deviseId, String deviseCode, BigDecimal aPayer,
                              BigDecimal regle, UUID moduleId, String moduleCode, boolean okPourPaiement) {

        public BigDecimal resteAPayer() {
            return SoldeDocumentAchat.de(aPayer, regle).resteARegler();
        }
    }

    private static final UUID AUCUN = new UUID(0, 0);
    private static final String REGLEMENT = "reglement";
    private static final String PKEY = "PKey";
    private static final String CODE = "Code";
    private static final String DESCRIPTION = "Description";

    private static final String PIECES = "SELECT d.PKey, d.Code, d.DocDate, d.MaturityDate, d.Description, d.CfgTierId, "
            + "d.CfgCompanyId, d.AdmModuleId, mo.Code AS ModuleCode, bs.IsOkForPayement, d.StlCurrencyId, "
            + "cur.Code AS DeviseCode, d.AmountToPay, COALESCE(("
            + SoldeDocumentAchat.SQL_REGLE + " AND l.StlSettlementId <> :reglement), 0) AS Regle "
            + "FROM BuyDocument d LEFT JOIN StlCurrency cur ON cur.PKey = d.StlCurrencyId "
            + "LEFT JOIN AdmModule mo ON mo.PKey = d.AdmModuleId "
            + "LEFT JOIN BuyDocumentStatus bs ON bs.PKey = d.BuyDocumentStatusId WHERE " + RequetesDocumentsAchat.VISIBLE;

    private static final String PIECES_PAR_ID = PIECES + "AND d.PKey IN (:ids)";

    // Verrou de mise à jour tenu jusqu'au commit : deux saisies sur une même pièce relisent son reste l'une après l'autre.
    private static final String VERROUILLER = "SELECT PKey FROM BuyDocument WITH (UPDLOCK, ROWLOCK) "
            + "WHERE PKey IN (:ids) ORDER BY PKey ASC";

    private static final String PIECES_A_AFFECTER = PIECES + "AND bs.IsOkForPayement = 1 AND d.CfgTierId = :fournisseur "
            + "AND d.CfgCompanyId = :societe AND mo.Code = :module ORDER BY d.DocDate, d.Code";

    private static final String VIREMENT_INTERNE = "SELECT COUNT(*) FROM StlSettlement "
            + "WHERE PKey = :reglement AND StlInternalTransferId IS NOT NULL";

    // Unicité OMRAN de la référence d'un débit : Code + type + sens + compte (compte absent = même « compte »).
    private static final String REFERENCE_PRISE = "SELECT COUNT(*) FROM StlSettlement WHERE Code = :code "
            + "AND StlSettlementTypeId = :type AND IsCredit = 0 AND PKey <> :reglement "
            + "AND (StlAccountId = :compte OR (StlAccountId IS NULL AND :compte IS NULL))";

    private static final List<String> TABLES_LIEES = List.of("StlAccountingEntry", "ActEntry",
            "StlTransmissionSheetLine", "StlSlipLine", "StlSettlementDocumentToAttach", "StlDeduction");

    private static final String TABLES_PRESENTES = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS "
            + "WHERE COLUMN_NAME = 'StlSettlementId' AND TABLE_NAME IN (:tables)";

    private static final String MODULE = "SELECT PKey FROM AdmModule WHERE Code = :code";

    private static final String TYPES = "SELECT PKey, Code, Description, IsReferenceMandatory, IsReferenceUnique, "
            + "IsBankMandatory, IsLawyerMandatory, IsTierUnique, IsNotContabilized FROM StlSettlementType ORDER BY Code";

    private static final String SOCIETES = "SELECT PKey, Code, Description FROM CfgCompany "
            + "WHERE PKey IN (:societes) ORDER BY Code";

    private static final String COMPTES = "SELECT a.PKey, a.Code, a.CfgCompanyId, COALESCE(cur.Code, a.CurrencyCode) "
            + "AS Devise FROM StlAccount a LEFT JOIN StlCurrency cur ON cur.PKey = a.StlCurrencyId "
            + "WHERE a.CfgCompanyId IN (:societes) ORDER BY a.Code";

    private static final String DEVISE = "SELECT Code FROM StlCurrency WHERE PKey = :devise";

    private final NamedParameterJdbcTemplate jdbc;

    RequetesSaisieDecaissement(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PieceSaisie> pieces(PerimetreAchat perimetre, Collection<UUID> ids, UUID reglement) {
        if (perimetre.vide() || ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query(PIECES_PAR_ID, RequetesDocumentsAchat.parametres(perimetre)
                .addValue("ids", RequetesDocumentsAchat.textes(ids)).addValue(REGLEMENT, texte(reglement)),
                (rs, n) -> piece(rs));
    }

    public void verrouiller(Collection<UUID> ids) {
        if (!ids.isEmpty()) {
            jdbc.queryForList(VERROUILLER, Map.of("ids", RequetesDocumentsAchat.textes(ids)), String.class);
        }
    }

    public List<PieceSaisie> piecesAAffecter(PerimetreAchat perimetre, String module, UUID fournisseur, UUID societe,
                                             UUID reglement) {
        if (perimetre.vide()) {
            return List.of();
        }
        return jdbc.query(PIECES_A_AFFECTER, RequetesDocumentsAchat.parametres(perimetre)
                .addValue("fournisseur", texte(fournisseur)).addValue("societe", texte(societe))
                .addValue("module", module).addValue(REGLEMENT, texte(reglement)), (rs, n) -> piece(rs));
    }

    public boolean virementInterne(UUID reglement) {
        return compte(VIREMENT_INTERNE, new MapSqlParameterSource(REGLEMENT, reglement.toString())) > 0;
    }

    public boolean referencePrise(String code, UUID type, UUID compte, UUID reglement) {
        return compte(REFERENCE_PRISE, new MapSqlParameterSource("code", code).addValue("type", type.toString())
                .addValue("compte", compte == null ? null : compte.toString(), Types.VARCHAR)
                .addValue(REGLEMENT, texte(reglement))) > 0;
    }

    /** Tables qui désignent encore le règlement (pièces comptables, bordereaux…), parmi celles du schéma. */
    public List<String> tablesQuiReferencent(UUID reglement) {
        List<String> presentes = jdbc.queryForList(TABLES_PRESENTES, Map.of("tables", TABLES_LIEES), String.class);
        MapSqlParameterSource parametres = new MapSqlParameterSource(REGLEMENT, reglement.toString());
        return TABLES_LIEES.stream().filter(presentes::contains)
                .filter(t -> compte("SELECT COUNT(*) FROM " + t + " WHERE StlSettlementId = :reglement", parametres) > 0)
                .toList();
    }

    public UUID module(String code) {
        return jdbc.query(MODULE, Map.of("code", code), (rs, n) -> uuid(rs, PKEY)).stream().findFirst().orElse(null);
    }

    public List<TypeDecaissementDto> types() {
        return jdbc.query(TYPES, (rs, n) -> new TypeDecaissementDto(uuid(rs, PKEY), rs.getString(CODE),
                rs.getString(DESCRIPTION), rs.getBoolean("IsReferenceMandatory"), rs.getBoolean("IsReferenceUnique"),
                rs.getBoolean("IsBankMandatory"), rs.getBoolean("IsLawyerMandatory"), rs.getBoolean("IsTierUnique"),
                rs.getBoolean("IsNotContabilized")));
    }

    public List<SocieteDecaissementDto> societes(Collection<UUID> societes) {
        if (societes.isEmpty()) {
            return List.of();
        }
        return jdbc.query(SOCIETES, Map.of("societes", RequetesDocumentsAchat.textes(societes)),
                (rs, n) -> new SocieteDecaissementDto(uuid(rs, PKEY), rs.getString(CODE), rs.getString(DESCRIPTION)));
    }

    public List<CompteDecaissementDto> comptes(Collection<UUID> societes) {
        if (societes.isEmpty()) {
            return List.of();
        }
        return jdbc.query(COMPTES, Map.of("societes", RequetesDocumentsAchat.textes(societes)),
                (rs, n) -> new CompteDecaissementDto(uuid(rs, PKEY), rs.getString(CODE), uuid(rs, "CfgCompanyId"),
                        rs.getString("Devise")));
    }

    public String devise(UUID devise) {
        return devise == null ? null : jdbc.queryForList(DEVISE, Map.of("devise", devise.toString()), String.class)
                .stream().findFirst().orElse(null);
    }

    private int compte(String sql, MapSqlParameterSource parametres) {
        Integer n = jdbc.queryForObject(sql, parametres, Integer.class);
        return n == null ? 0 : n;
    }

    private static PieceSaisie piece(ResultSet rs) throws SQLException {
        return new PieceSaisie(uuid(rs, PKEY), rs.getString(CODE), jour(rs, "DocDate"), jour(rs, "MaturityDate"),
                rs.getString(DESCRIPTION), uuid(rs, "CfgTierId"), uuid(rs, "CfgCompanyId"), uuid(rs, "StlCurrencyId"),
                rs.getString("DeviseCode"), Objects.requireNonNullElse(rs.getBigDecimal("AmountToPay"), BigDecimal.ZERO),
                Objects.requireNonNullElse(rs.getBigDecimal("Regle"), BigDecimal.ZERO), uuid(rs, "AdmModuleId"),
                rs.getString("ModuleCode"), rs.getBoolean("IsOkForPayement"));
    }

    private static String texte(UUID id) {
        return Objects.requireNonNullElse(id, AUCUN).toString();
    }

    private static UUID uuid(ResultSet rs, String colonne) throws SQLException {
        String valeur = rs.getString(colonne);
        return valeur == null ? null : UUID.fromString(valeur);
    }

    private static LocalDate jour(ResultSet rs, String colonne) throws SQLException {
        Date date = rs.getDate(colonne);
        return date == null ? null : date.toLocalDate();
    }
}
