package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.achat.PieceAnnulee;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport.Option;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.chaine;
import static com.tsix.immocrm.services.achat.RequetesDocumentsAchat.jour;

/**
 * SQL natif des rapports Achat (FEATURE-n7-etats-achat.md R12, R14, R15) : toute pièce, prêt ou prévision de projet passe
 * par les prédicats de visibilité de {@link RequetesDocumentsAchat} (D3) ; les prévisions par catégorie, sans projet, par la
 * seule société.
 */
@Component
class RequetesRapportsAchat {

    private static final String ANNEE = "annee";
    private static final String MONTANT = "Montant";

    private static final String PRETS = " FROM BuyLoanSchedule s JOIN BuySubLoan sl ON sl.PKey = s.BuySubLoanId "
            + "JOIN BuyLoan d ON d.PKey = sl.BuyLoanId WHERE YEAR(s.PaymentDate) = :annee AND "
            + RequetesDocumentsAchat.SOCIETE_PROJET_VISIBLES;

    // R12 : (colonne, mois, montant) ; P = prévu, R = réel, D = décaissements.
    private static final String DEPENSES = "SELECT 'P' AS Colonne, PrevisionMonth AS Mois, SUM(PrevisionAmount) AS Montant "
            + "FROM StlCategoryPrevision WHERE PrevisionYear = :annee AND CfgCompanyId IN (:societes) "
            + "GROUP BY PrevisionMonth "
            + "UNION ALL SELECT 'P', d.PrevisionMonth, SUM(d.PrevisionAmount) FROM PrjPrevision d WHERE d.PrevisionYear = :annee "
            + "AND " + RequetesDocumentsAchat.PROJET_VISIBLE + "GROUP BY d.PrevisionMonth "
            + "UNION ALL SELECT 'P', MONTH(s.PaymentDate), SUM(s.AmountToPay)" + PRETS + "GROUP BY MONTH(s.PaymentDate) "
            + "UNION ALL SELECT 'R', MONTH(ISNULL(d.MaturityDate, d.DocDate)), SUM(d.AmountToPay) FROM BuyDocument d "
            + "LEFT JOIN BuyDocumentStatus bs ON bs.PKey = d.BuyDocumentStatusId WHERE " + RequetesDocumentsAchat.VISIBLE
            + "AND d.CanBePayed = 1 AND CASE WHEN " + PieceAnnulee.SQL + " THEN 1 ELSE 0 END = 0 "
            + "AND YEAR(ISNULL(d.MaturityDate, d.DocDate)) = :annee GROUP BY MONTH(ISNULL(d.MaturityDate, d.DocDate)) "
            + "UNION ALL SELECT 'D', MONTH(ISNULL(s.EcheanceDate, s.SettlementDate)), "
            + "SUM(CASE WHEN s.IsCredit = 0 THEN s.Amount ELSE -s.Amount END) FROM StlSettlement s "
            + "JOIN AdmModule m ON m.PKey = s.AdmModuleId WHERE m.Code IN ('BUY', 'PRJ') AND "
            + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX + " AND s.CfgCompanyId IN (:societes) "
            + "AND YEAR(ISNULL(s.EcheanceDate, s.SettlementDate)) = :annee "
            + "GROUP BY MONTH(ISNULL(s.EcheanceDate, s.SettlementDate))";

    // R14 : reste = commandé − Σ livré des BL de ce BC de code ≤ code du BL courant (RANGE inclut les BL de même code).
    private static final String BONS = "SELECT lc.Code AS CodeArticle, lc.Description AS Designation, d.Code AS CodeBC, "
            + "d.DocDate AS DateBC, COALESCE(NULLIF(b.DocRef, ''), b.Code) AS CodeBL, b.DocDate AS DateBL, "
            + "lc.Quantity AS Commandee, lb.Quantity AS Livree, lc.Quantity - SUM(ISNULL(lb.Quantity, 0)) OVER "
            + "(PARTITION BY lc.PKey ORDER BY b.Code ASC RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS Reste "
            + "FROM BuyDocument d JOIN BuyDocumentLine lc ON lc.BuyDocumentId = d.PKey "
            + "LEFT JOIN (BuyDocument b JOIN BuyDocumentLine lb ON lb.BuyDocumentId = b.PKey) "
            + "ON b.BuyOrderId = d.PKey AND b.DocType = 'DELIVRY' AND lb.Code = lc.Code "
            + "WHERE d.DocType = 'ORDER' AND " + RequetesDocumentsAchat.VISIBLE
            + "AND d.DocDate >= :du AND d.DocDate < :lendemain "
            + "AND (:codeBC IS NULL OR d.Code LIKE :codeBC) AND (:codeArticle IS NULL OR lc.Code LIKE :codeArticle) "
            + "ORDER BY d.DocDate ASC, d.Code ASC, lc.Sorting ASC, b.Code ASC";

    private static final String ECHEANCES = "SELECT d.Description, MONTH(s.PaymentDate) AS Mois, SUM(s.AmountToPay) AS Montant"
            + PRETS + "GROUP BY d.Description, MONTH(s.PaymentDate) ORDER BY d.Description ASC, Mois ASC";

    /** Montant d'une source R12 pour un mois (1–12). */
    record MontantMois(String colonne, int mois, BigDecimal montant) {
    }

    /** Échéances d'un prêt sommées sur un mois (R15). */
    record Echeance(String pret, int mois, BigDecimal montant) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final RequetesDocumentsAchat documents;
    private final PerimetreProjetService perimetre;

    RequetesRapportsAchat(NamedParameterJdbcTemplate jdbc, RequetesDocumentsAchat documents,
                          PerimetreProjetService perimetre) {
        this.jdbc = jdbc;
        this.documents = documents;
        this.perimetre = perimetre;
    }

    /** Pièces, prêts et prévisions visibles de l'appelant ; un filtre de projet exclut ceux sans projet. */
    PerimetreAchat visibles(List<UUID> projets) {
        return perimetre.achat(projets);
    }

    private PerimetreAchat visibles() {
        return visibles(List.of());
    }

    /** {@code CHOIX} facultatif dont les options sont les projets visibles (un projet hors périmètre = 400). */
    ParametreRapport projets(String nom, String libelle, boolean multiple) {
        Set<UUID> ids = perimetre.projetsVisibles();
        List<Option> options = ids.isEmpty() ? List.of() : documents.projets(ids).stream()
                .map(p -> new Option(p.id().toString(), Objects.toString(p.description(), p.code()))).toList();
        return new ParametreRapport(nom, libelle, TypeParametre.CHOIX, false, multiple, null, options, null, null);
    }

    /** {@code CHOIX} facultatif dont les options sont les sociétés habilitées. */
    ParametreRapport societes(String nom, String libelle) {
        Set<UUID> ids = visibles().societes();
        List<Option> options = ids.isEmpty() ? List.of() : documents.societes(ids).stream()
                .map(s -> new Option(s.id().toString(), Objects.toString(s.description(), s.code()))).toList();
        return new ParametreRapport(nom, libelle, TypeParametre.CHOIX, false, false, null, options, null, null);
    }

    List<MontantMois> depenses(int annee) {
        return jdbc.query(DEPENSES, RequetesDocumentsAchat.parametres(visibles()).addValue(ANNEE, annee),
                (rs, n) -> new MontantMois(rs.getString("Colonne"), rs.getInt("Mois"), rs.getBigDecimal(MONTANT)));
    }

    /** Lignes R14, clés = noms des colonnes du rapport. */
    List<Map<String, Object>> bons(PerimetreAchat visibles, LocalDate du, LocalDate au, String codeBC,
                                   String codeArticle) {
        return jdbc.query(BONS, RequetesDocumentsAchat.parametres(visibles)
                .addValue("du", du, Types.DATE).addValue("lendemain", au.plusDays(1), Types.DATE)
                .addValue("codeBC", motif(codeBC), Types.NVARCHAR)
                .addValue("codeArticle", motif(codeArticle), Types.NVARCHAR), (rs, n) -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put("codeArticle", chaine(rs, "CodeArticle"));
                    ligne.put("designation", chaine(rs, "Designation"));
                    ligne.put("codeBC", chaine(rs, "CodeBC"));
                    ligne.put("dateBC", jour(rs, "DateBC"));
                    ligne.put("codeBL", chaine(rs, "CodeBL"));
                    ligne.put("dateBL", jour(rs, "DateBL"));
                    ligne.put("quantiteCommandee", rs.getBigDecimal("Commandee"));
                    ligne.put("quantiteLivree", rs.getBigDecimal("Livree"));
                    ligne.put("reste", rs.getBigDecimal("Reste"));
                    return ligne;
                });
    }

    List<Echeance> echeances(int annee) {
        return jdbc.query(ECHEANCES, RequetesDocumentsAchat.parametres(visibles()).addValue(ANNEE, annee),
                (rs, n) -> new Echeance(chaine(rs, "Description"), rs.getInt("Mois"), rs.getBigDecimal(MONTANT)));
    }

    /** Recherche « contient » ; vide = pas de filtre. */
    private static String motif(String texte) {
        return texte == null || texte.isBlank() ? null : "%" + texte.trim() + "%";
    }
}
