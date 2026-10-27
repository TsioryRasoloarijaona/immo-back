package com.tsix.immocrm.services.rapport.stock;

import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seul calcul des mouvements du stock (recalcul de {@code StkMotionView} sur tables, FEATURE-patrimoine-rapports.md §3,
 * FEATURE-tdb-vente-rapports-rythme.md §3.5) : lancement, concrétisation, désistement, annulation, transferts inter-tranches.
 */
@Component
public class MouvementsStockTranche {

    // Un seuil AdmParamsGuid absent (ou sans statut) vide son bloc par jointure interne ; l'avertissement le signale.
    // Concrétisation sur l'article tenu à cette date : ancien article du plus ancien transfert retenu (§3, décision 24/10).
    private static final String SQL_MOUVEMENTS = """
            WITH Transferts AS (
                SELECT w.PKey, w.ComSaleId, w.StkOldItemId, w.StkItemId, w.ValidityDate, f.Code AS Dossier
                FROM ComSaleWithdrawalProduct w
                JOIN ComFolder f ON f.PKey = w.ComSaleId AND f.AdvanceDate IS NOT NULL AND f.CancelDate IS NULL
                WHERE w.ValidityDate IS NOT NULL
                  AND EXISTS (SELECT 1 FROM StrFolderStatusHistory h
                              JOIN StrStatus sh ON sh.PKey = h.StrFolderStatusId
                              JOIN AdmParamsGuid p ON p.Code = 'DefaultComFolderStatusConcretisation'
                              JOIN StrStatus sc ON sc.PKey = p.CodeValue
                              WHERE h.ComSaleWithdrawalProductId = w.PKey AND sh.Sorting >= sc.Sorting)
            )
            SELECT m.TrancheId AS Tranche, t.Description AS LibelleTranche, m.Article, m.Dossier, m.Libelle,
                   m.DateMouvement, m.Quantite
            FROM (
                SELECT l.PKey AS TrancheId, CAST(NULL AS nvarchar(255)) AS Article,
                       CAST(NULL AS nvarchar(255)) AS Dossier, N'LANCEMENT DE LA COMMERCIALISATION' AS Libelle,
                       CAST(l.MarketingStartDate AS date) AS DateMouvement,
                       (SELECT COUNT(*) FROM StkItem i JOIN StkVocation v ON v.PKey = i.StkVocationId AND v.Code = 'VENTE'
                        WHERE i.StkHierarchyId = l.PKey AND ISNULL(i.IsUnderItem, 0) = 0) AS Quantite
                FROM CfgTranche l
                WHERE l.MarketingStartDate IS NOT NULL
                UNION ALL
                SELECT i.StkHierarchyId, i.Code, f.Code, N'CONCRETISATION', CAST(f.AdvanceDate AS date), -1
                FROM ComFolder f
                OUTER APPLY (SELECT TOP 1 r.StkOldItemId FROM Transferts r WHERE r.ComSaleId = f.PKey
                             ORDER BY r.ValidityDate ASC, r.PKey ASC) premier
                JOIN StkItem i ON i.PKey = COALESCE(premier.StkOldItemId, f.StkItemId)
                WHERE f.AdvanceDate IS NOT NULL
                UNION ALL
                SELECT i.StkHierarchyId, i.Code, f.Code, s.Libelle, CAST(f.CancelDate AS date), 1
                FROM ComFolder f
                JOIN StkItem i ON i.PKey = f.StkItemId
                CROSS JOIN (VALUES (N'DÉSISTEMENT', 'DefaultComFolderStatusResiliation'),
                                   (N'ANNULATION', 'DefaultComFolderStatusCancellation')) s(Libelle, Seuil)
                JOIN AdmParamsGuid p ON p.Code = s.Seuil AND p.CodeValue = f.StrStatusId
                WHERE f.AdvanceDate IS NOT NULL AND f.CancelDate IS NOT NULL
                UNION ALL
                SELECT x.TrancheId, x.Article, w.Dossier, x.Libelle, CAST(w.ValidityDate AS date), x.Quantite
                FROM Transferts w
                JOIN StkItem io ON io.PKey = w.StkOldItemId
                JOIN StkItem inv ON inv.PKey = w.StkItemId
                CROSS APPLY (VALUES (io.StkHierarchyId, io.Code, N'TRANSFERT SORTIE', 1),
                                    (inv.StkHierarchyId, inv.Code, N'TRANSFERT ENTREE', -1)) x(TrancheId, Article, Libelle, Quantite)
                WHERE io.StkHierarchyId <> inv.StkHierarchyId
            ) m
            JOIN CfgTranche t ON t.PKey = m.TrancheId
            WHERE t.PKey IN (:tranches) AND t.IsForSale = 1""";

    private static final String SEUILS_POSES = """
            SELECT p.Code FROM AdmParamsGuid p JOIN StrStatus s ON s.PKey = p.CodeValue
            WHERE p.Code IN ('DefaultComFolderStatusConcretisation', 'DefaultComFolderStatusResiliation',
                             'DefaultComFolderStatusCancellation')""";

    private static final Map<String, String> BLOCS_PAR_SEUIL = Map.of(
            "DefaultComFolderStatusConcretisation", "transferts",
            "DefaultComFolderStatusResiliation", "désistements",
            "DefaultComFolderStatusCancellation", "annulations");

    private final NamedParameterJdbcTemplate jdbc;

    public MouvementsStockTranche(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mouvements des tranches en vente parmi {@code tranches} (visibles), toutes dates, non triés. */
    public List<MouvementStock> lire(List<UUID> tranches) {
        return LotsTranches.lire(tranches, lot -> jdbc.query(SQL_MOUVEMENTS,
                Map.of("tranches", LotsTranches.textes(lot)), (rs, n) -> new MouvementStock(
                        UUID.fromString(rs.getString("Tranche")), rs.getString("Article"), rs.getString("Dossier"),
                        rs.getString("Libelle"), rs.getObject("DateMouvement", LocalDate.class), rs.getLong("Quantite"),
                        rs.getString("LibelleTranche"))));
    }

    /** Stock de {@code tranche} au matin de {@code jour} : Σ des quantités de ses mouvements datés strictement avant. */
    public long stockAvant(List<MouvementStock> mouvements, UUID tranche, LocalDate jour) {
        return mouvements.stream().filter(m -> tranche.equals(m.tranche()) && m.date().isBefore(jour))
                .mapToLong(MouvementStock::quantite).sum();
    }

    /** Un avertissement par paramètre de statut absent (son bloc de mouvements est vide). */
    public List<String> avertissements() {
        List<String> poses = jdbc.queryForList(SEUILS_POSES, Map.of(), String.class);
        return BLOCS_PAR_SEUIL.entrySet().stream()
                .filter(e -> !poses.contains(e.getKey()))
                .map(e -> "Paramètre « %s » absent : %s non comptés.".formatted(e.getKey(), e.getValue()))
                .sorted().toList();
    }
}
