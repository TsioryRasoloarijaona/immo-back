package com.tsix.immocrm.services.rapport.patrimoine;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TEXTE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHE;
import static com.tsix.immocrm.services.rapport.patrimoine.ColonnesStock.TRANCHES;

/**
 * MOUVEMENTS DU STOCK (FEATURE-patrimoine-rapports.md §3), recalculés sur tables : lancement, concrétisation,
 * désistement, annulation et transferts inter-tranches (jointure du modèle corrigée, décision 3). Tranches en vente seules.
 */
@Component
public class MouvementsStockRapport implements Rapport {

    static final String CODE = "StkTdbMotion";

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String MOUVEMENT = "mouvement";
    private static final String STOCK_INITIAL = "stockInitial";

    // Un seuil AdmParamsGuid absent (ou sans statut) vide son bloc par jointure interne ; l'avertissement le signale.
    private static final String SQL_MOUVEMENTS = """
            SELECT t.Description AS Tranche, m.Article, m.Dossier, m.Libelle, m.DateMouvement, m.Quantite
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
                FROM ComFolder f JOIN StkItem i ON i.PKey = f.StkItemId
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
                SELECT x.TrancheId, x.Article, f.Code, x.Libelle, CAST(w.ValidityDate AS date), x.Quantite
                FROM ComSaleWithdrawalProduct w
                JOIN ComFolder f ON f.PKey = w.ComSaleId AND f.AdvanceDate IS NOT NULL AND f.CancelDate IS NULL
                JOIN StkItem io ON io.PKey = w.StkOldItemId
                JOIN StkItem inv ON inv.PKey = w.StkItemId
                CROSS APPLY (VALUES (io.StkHierarchyId, io.Code, N'TRANSFERT SORTIE', 1),
                                    (inv.StkHierarchyId, inv.Code, N'TRANSFERT ENTREE', -1)) x(TrancheId, Article, Libelle, Quantite)
                WHERE w.ValidityDate IS NOT NULL AND io.StkHierarchyId <> inv.StkHierarchyId
                  AND EXISTS (SELECT 1 FROM StrFolderStatusHistory h
                              JOIN StrStatus sh ON sh.PKey = h.StrFolderStatusId
                              JOIN AdmParamsGuid p ON p.Code = 'DefaultComFolderStatusConcretisation'
                              JOIN StrStatus sc ON sc.PKey = p.CodeValue
                              WHERE h.ComSaleWithdrawalProductId = w.PKey AND sh.Sorting >= sc.Sorting)
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

    private static final Comparator<Mouvement> ORDRE = Comparator.comparing(Mouvement::date)
            .thenComparing(Mouvement::tranche, TEXTE).thenComparing(Mouvement::article, TEXTE)
            .thenComparing(Mouvement::libelle, TEXTE);

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public MouvementsStockRapport(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    record Mouvement(String tranche, String article, String dossier, String libelle, LocalDate date, long quantite) {
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.PATRIMOINE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        LocalDate aujourdhui = LocalDate.now(clock);
        return List.of(ParametreRapport.date(DU, "Du", aujourdhui.minusMonths(1)),
                ParametreRapport.date(AU, "Au", aujourdhui), ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        LocalDate aujourdhui = LocalDate.now(clock);
        LocalDate du = Objects.requireNonNullElse(contexte.date(DU), aujourdhui.minusMonths(1));
        LocalDate au = Objects.requireNonNullElse(contexte.date(AU), aujourdhui);
        List<Mouvement> tous = LotsTranches.lire(contexte.tranchesVisibles(), lot -> jdbc.query(SQL_MOUVEMENTS,
                Map.of(TRANCHES, LotsTranches.textes(lot)), (rs, n) -> new Mouvement(rs.getString("Tranche"),
                        rs.getString("Article"), rs.getString("Dossier"), rs.getString("Libelle"),
                        rs.getObject("DateMouvement", LocalDate.class), rs.getLong("Quantite"))));
        long initial = tous.stream().filter(m -> m.date().isBefore(du)).mapToLong(Mouvement::quantite).sum();
        List<Mouvement> periode = tous.stream().filter(m -> !m.date().isBefore(du) && !m.date().isAfter(au))
                .sorted(ORDRE).toList();
        long entrees = periode.stream().mapToLong(Mouvement::quantite).filter(q -> q > 0).sum();
        long sorties = periode.stream().mapToLong(Mouvement::quantite).filter(q -> q < 0).sum();
        return new ResultatRapport(List.of(synthese(initial, entrees, sorties), mouvements(initial, periode)),
                avertissements());
    }

    private static SectionRapport synthese(long initial, long entrees, long sorties) {
        List<ColonneRapport> colonnes = List.of(ColonnesStock.compteur(STOCK_INITIAL, "Stock initial"),
                ColonnesStock.compteur("entrees", "Entrées"), ColonnesStock.compteur("sorties", "Sorties"),
                ColonnesStock.compteur("stockFinal", "Stock final"));
        Map<String, Object> ligne = ColonnesStock.ligne(STOCK_INITIAL, initial, "entrees", entrees, "sorties", sorties,
                "stockFinal", initial + entrees + sorties);
        return new SectionRapport("synthese", "Synthèse", colonnes, List.of(ligne), null, null, null);
    }

    private static SectionRapport mouvements(long initial, List<Mouvement> periode) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        long cumul = initial;
        for (Mouvement m : periode) {
            cumul += m.quantite();
            lignes.add(ColonnesStock.ligne(TRANCHE, m.tranche(), "article", m.article(), "dossier", m.dossier(),
                    "libelle", m.libelle(), "date", m.date(), MOUVEMENT, m.quantite(), "cumul", cumul));
        }
        List<ColonneRapport> colonnes = List.of(ColonnesStock.COLONNE_TRANCHE,
                ColonneRapport.texte("article", "Article"), ColonnesStock.texteMasque("dossier", "Dossier"),
                ColonneRapport.texte("libelle", "Description"),
                new ColonneRapport("date", "Date", TypeColonne.DATE, null, null, true, 0),
                ColonneRapport.effectif(MOUVEMENT, "Mouv.", null), ColonnesStock.compteur("cumul", "Cumul"));
        return new SectionRapport("mouvements", "Mouvements du stock", colonnes, lignes, null, null, null);
    }

    private List<String> avertissements() {
        List<String> poses = jdbc.queryForList(SEUILS_POSES, Map.of(), String.class);
        return BLOCS_PAR_SEUIL.entrySet().stream()
                .filter(e -> !poses.contains(e.getKey()))
                .map(e -> "Paramètre « %s » absent : %s non comptés.".formatted(e.getKey(), e.getValue()))
                .sorted().toList();
    }
}
