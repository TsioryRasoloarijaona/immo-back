package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.services.rapport.vente.flux.FluxParReflexion.Flux;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.FixtureFluxVente.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rapprochement avec la vue {@code ComSalePrevision} (FEATURE-tdb-vente-rapports-flux.md §12.3 test 1, R-F1, §3.4 E1–E3).
 * yesRemsTest et la CI n'ont pas la vue : chaque bloc « réel » est rejoué par une requête de référence écrite ici (mêmes
 * jointures internes, vocation par identifiant, seuils par {@code AdmParamsGuid}, signe {@code IIF(IsCredit = 1, 1, -1)}).
 * Zéro écart attendu partout, sauf les encaissements : exactement E1, E2, E3 sur des lignes dédiées de 2026.
 */
@SpringBootTest
@Transactional
class FluxVenteMensuelRapprochementTest {

    private static final int DEBUT = 2021;
    private static final int FIN = 2026;
    private static final String ENCAISSEMENTS = "ENCAISSEMENTS";
    private static final String SEUIL = "s.Sorting >= (SELECT x.Sorting FROM StrStatus x WHERE x.PKey = "
            + "(SELECT g.CodeValue FROM AdmParamsGuid g WHERE g.Code = '%s'))";
    private static final String UN = "1";
    private static final String NI_ANNULE_NI_DESISTE = " AND s.BoolField002 = 0 AND s.BoolField003 = 0";
    private static final String DOSSIERS = """
             FROM ComFolder f
             JOIN StkItem i ON f.StkItemId = i.PKey
             JOIN CfgTranche t ON i.StkHierarchyId = t.PKey
             JOIN CfgProject p ON t.CfgProjectId = p.PKey
             JOIN CfgCompany c ON p.CfgCompanyId = c.PKey
             JOIN StrStatus s ON f.StrStatusId = s.PKey
             WHERE f.StkVocationId = ? AND t.IsForSale = 1 AND t.PKey = ?""";
    private static final String REGLEMENTS = """
             FROM StlSettlementLine l
             JOIN StlSettlement st ON l.StlSettlementId = st.PKey
             JOIN StlSettlementType ty ON st.StlSettlementTypeId = ty.PKey
             JOIN ComDocument d ON l.ComDocumentId = d.PKey
             JOIN ComSettlementCategory k ON d.ComSettlementCategoryId = k.PKey
             JOIN ComFolder f ON d.ComFolderId = f.PKey
             JOIN StkItem i ON f.StkItemId = i.PKey
             JOIN CfgTranche t ON i.StkHierarchyId = t.PKey
             JOIN CfgProject p ON t.CfgProjectId = p.PKey
             JOIN CfgCompany c ON p.CfgCompanyId = c.PKey
             JOIN StrStatus s ON f.StrStatusId = s.PKey
             WHERE f.StkVocationId = ? AND t.IsForSale = 1 AND t.PKey = ?
               AND ty.IsNotContabilized = 0 AND i.IsUnderItem = 0""";
    private static final String MOUVEMENT = "IIF(st.SettlementDate IS NULL, st.EcheanceDate, st.SettlementDate)";
    private static final String SIGNE_VUE = "ISNULL(l.Amount, 0) * IIF(st.IsCredit = 1, 1, -1)";
    private static final String DU_RESILIATION = "ISNULL((SELECT SUM(e.Amount) FROM ComDocument e JOIN ComSettlementCategory q "
            + "ON e.ComSettlementCategoryId = q.PKey WHERE q.IsSaleResiliation = 1 AND q.IsCredit = 0 "
            + "AND e.ComFolderId = f.PKey), 0)";

    @Autowired private ApplicationContext contexte;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private JdbcTemplate jdbc;

    private Jeu jeu;
    private Statuts st;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        jeu = fixture.jeu("RAP");
        st = jeu.statuts();
        tranche = jeu.tranche("T", jeu.projet("P", true));
        jeuDeDonnees();
        fixture.relire();
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private static BigDecimal m(String valeur) {
        return new BigDecimal(valeur);
    }

    /** Dossiers 2021–2026 couvrant chaque bloc « réel » de la vue, plus les lignes E1–E3 de 2026. */
    private void jeuDeDonnees() {
        ComFolder a = jeu.dater(jeu.dossier(tranche, st.concretisation()), j(2021, 3, 5), null, null, null);
        ComDocument avanceA = jeu.echeance(a, jeu.categorie(Genre.AVANCE, true), m("100000"), j(2021, 3, 5));
        jeu.echeance(a, jeu.categorie(Genre.AVANCE, false), m("3000"), j(2021, 3, 5));
        jeu.regler(avanceA, m("60000"), true, IN_WORK, null, j(2021, 4, 2));

        ComFolder b = jeu.dater(jeu.dossier(tranche, st.minute()), j(2022, 1, 10), j(2022, 6, 15), null, null);
        jeu.prix(b, m("900000"), m("750000"));
        jeu.encaisser(b, "300000", j(2022, 7, 1));

        jeu.dater(jeu.dossier(tranche, st.remiseClefs()), j(2022, 2, 10), j(2022, 8, 1), j(2023, 3, 1), null);

        ComFolder d = jeu.dater(jeu.dossier(tranche, st.annulation()), j(2023, 5, 5), j(2023, 6, 6), null,
                j(2024, 2, 2));
        jeu.echeance(d, jeu.categorie(Genre.RESILIATION, false), m("4000"), j(2024, 2, 2));
        jeu.regler(jeu.echeance(d, jeu.categorie(Genre.AVANCE, true), m("1000"), j(2023, 5, 20)), m("1000"), true,
                IN_WORK, null, j(2023, 5, 20));

        ComFolder e = jeu.dater(jeu.dossier(tranche, st.desistement()), j(2023, 7, 7), null, j(2023, 8, 8),
                j(2024, 9, 9));
        jeu.echeance(e, jeu.categorie(Genre.RESILIATION, false), m("2500"), j(2024, 9, 9));

        ComFolder sous = jeu.dater(jeu.dossierSousArticle(tranche, st.concretisation()), j(2024, 4, 4), null, null,
                null);
        jeu.encaisser(sous, "7000", j(2024, 4, 10));
        jeu.dater(jeu.dossier(tranche, null), j(2024, 1, 1), j(2024, 1, 2), null, null);
        jeu.dater(jeu.dossier(tranche, st.reservation()), j(2025, 1, 1), null, null, null);
        jeu.prix(jeu.dater(jeu.dossier(tranche, st.minute()), null, j(2025, 3, 3), null, null), null, null);

        ComFolder k = jeu.dater(jeu.dossier(tranche, st.remiseClefs()), j(2026, 1, 15), j(2026, 2, 15),
                j(2026, 3, 15), null);
        jeu.encaisser(k, "1000", j(2026, 4, 1));
        ComDocument autoK = jeu.echeance(k, jeu.categorie(Genre.AUTOFINANCEMENT, true), m("700"), j(2026, 4, 1));
        jeu.regler(autoK, m("50"), false, IN_WORK, null, j(2026, 4, 1));
        jeu.regler(autoK, m("539.84"), true, CANCELED, null, j(2026, 4, 1));
        jeu.regler(autoK, m("34"), null, null, null, j(2026, 5, 1));
        jeu.regler(autoK, m("20"), null, null, null, null);
    }

    // ------------------------------------------------------------------ requêtes de référence (blocs de la vue)

    private String seuil(String parametre) {
        return SEUIL.formatted(parametre);
    }

    /** Une requête de bloc « dossier » : {@code valeur} (par dossier, sous-requête permise) sommée par année et mois de {@code date}. */
    private Map<String, BigDecimal> parDossier(String date, String valeur, String filtre) {
        return lire("SELECT YEAR(src.dt) AS a, MONTH(src.dt) AS m, SUM(src.x) AS v FROM (SELECT " + date + " AS dt, "
                + valeur + " AS x " + DOSSIERS + " AND " + date + " IS NOT NULL AND " + filtre
                + ") src GROUP BY YEAR(src.dt), MONTH(src.dt)");
    }

    private Map<String, BigDecimal> parReglement(String valeur, String filtre) {
        return lire("SELECT YEAR(" + MOUVEMENT + ") AS a, MONTH(" + MOUVEMENT + ") AS m, SUM(" + valeur + ") AS v "
                + REGLEMENTS + " AND " + filtre + " GROUP BY YEAR(" + MOUVEMENT + "), MONTH(" + MOUVEMENT + ")");
    }

    private Map<String, BigDecimal> lire(String sql) {
        Map<String, BigDecimal> valeurs = new HashMap<>();
        jdbc.query(sql, rs -> {
            valeurs.put(cle(rs.getObject("a"), rs.getObject("m")), rs.getBigDecimal("v"));
        }, FixturePatrimoine.VOCATION_VENTE.toString(), tranche.getId().toString());
        return valeurs;
    }

    private static String cle(Object annee, Object mois) {
        return annee + "-" + mois;
    }

    /** Requête de référence de chaque mesure, hors encaissements. */
    private Map<String, Map<String, BigDecimal>> references() {
        String concretisation = seuil(FixtureFluxVente.SEUIL_CONCRETISATION);
        String finalisation = seuil(FixtureFluxVente.SEUIL_FINALISATION) + NI_ANNULE_NI_DESISTE;
        String duAvance = "ISNULL((SELECT SUM(e.Amount) FROM ComDocument e JOIN ComSettlementCategory q "
                + "ON e.ComSettlementCategoryId = q.PKey WHERE q.IsSaleAdvance = 1 AND q.IsCredit = 1 "
                + "AND e.ComFolderId = f.PKey), 0)";
        Map<String, Map<String, BigDecimal>> r = new LinkedHashMap<>();
        r.put("CONCRETISATIONS", parDossier("f.AdvanceDate", UN, concretisation));
        r.put("CONCRETISATIONS_NETTES", parDossier("f.AdvanceDate", UN, concretisation + NI_ANNULE_NI_DESISTE));
        r.put("CONCRETISATIONS_MONTANT", parDossier("f.AdvanceDate", duAvance, concretisation));
        r.put("CA_QUANTITE", parDossier("f.AgreementSavingDate", UN, finalisation));
        r.put("CA_TTC", parDossier("f.AgreementSavingDate", "ISNULL(f.Price, 0)", finalisation));
        r.put("CA_HT", parDossier("f.AgreementSavingDate", "ISNULL(f.PriceHT, 0)", finalisation));
        r.put("LIVRAISONS", parDossier("f.DeliveryDateReal", UN, seuil(FixtureFluxVente.SEUIL_LIVRAISON)));
        r.put("DESISTEMENTS", parDossier("f.CancelDate", UN, "s.BoolField003 = 1"));
        r.put("DESISTEMENTS_MONTANT", parDossier("f.CancelDate", DU_RESILIATION, "s.BoolField003 = 1"));
        r.put("ANNULATIONS", parDossier("f.CancelDate", UN, "s.BoolField002 = 1"));
        r.put("ANNULATIONS_MONTANT", parDossier("f.CancelDate", DU_RESILIATION, "s.BoolField002 = 1"));
        r.put("AVANCES_ENCAISSEES", parReglement(SIGNE_VUE, "k.IsSaleAdvance = 1 AND k.IsInAgrementContext = 1 "
                + "AND f.AdvanceDate IS NOT NULL AND " + concretisation + NI_ANNULE_NI_DESISTE));
        return r;
    }

    private Map<String, BigDecimal> encaissementsDeLaVue() {
        return parReglement(SIGNE_VUE, "k.IsSaleExpense = 0");
    }

    private Flux service(String... mesures) {
        return new FluxParReflexion(contexte).calculer(List.of(tranche.getId()), DEBUT, FIN, mesures);
    }

    // ------------------------------------------------------------------ R-F1 : mesures « dossier » et avances

    @Test
    void r_f1_chaqueMesureHorsEncaissements_egalLaRequeteDeReference_parAnneeEtMois() {
        Map<String, Map<String, BigDecimal>> references = references();
        Flux f = service(references.keySet().toArray(String[]::new));

        SoftAssertions soft = new SoftAssertions();
        references.forEach((mesure, ref) -> {
            soft.assertThat(ref).as("référence %s non vide", mesure).isNotEmpty();
            for (int annee = DEBUT; annee <= FIN; annee++) {
                for (int mois = 1; mois <= 12; mois++) {
                    soft.assertThat(f.valeur(mesure, tranche.getId(), annee, mois)).as("%s %d-%d", mesure, annee, mois)
                            .isEqualByComparingTo(ref.getOrDefault(cle(annee, mois), BigDecimal.ZERO));
                }
            }
        });
        soft.assertAll();
    }

    // ------------------------------------------------------------------ §3.4 : encaissements, écarts E1–E3 seulement

    @Test
    void e1_e2_encaissements_egalLaVue_saufAnnuleExcluEtIsCreditNullPositif() {
        Map<String, BigDecimal> vue = encaissementsDeLaVue();
        Map<String, BigDecimal> ecarts = Map.of(cle(2026, 4), m("-539.84"), cle(2026, 5), m("68"));
        Flux f = service(ENCAISSEMENTS);

        SoftAssertions soft = new SoftAssertions();
        for (int annee = DEBUT; annee <= FIN; annee++) {
            for (int mois = 1; mois <= 12; mois++) {
                BigDecimal attendu = vue.getOrDefault(cle(annee, mois), BigDecimal.ZERO)
                        .add(ecarts.getOrDefault(cle(annee, mois), BigDecimal.ZERO));
                soft.assertThat(f.valeur(ENCAISSEMENTS, tranche.getId(), annee, mois)).as("%d-%d", annee, mois)
                        .isEqualByComparingTo(attendu);
            }
        }
        soft.assertThat(vue.get(cle(2026, 4))).as("vue 2026-04 : 1000 − 50 + 539,84").isEqualByComparingTo("1489.84");
        soft.assertThat(vue.get(cle(2026, 5))).as("vue 2026-05 : IsCredit NULL compté en négatif")
                .isEqualByComparingTo("-34");
        soft.assertAll();
    }

    @Test
    void e3_ligneSansDate_horsDeToutTableau_dansLesAnomalies() {
        Map<String, BigDecimal> vue = encaissementsDeLaVue();
        Flux f = service(ENCAISSEMENTS);

        assertThat(vue.get(cle(null, null))).as("la vue la garde sans année").isEqualByComparingTo("-20");
        assertThat(f.cles()).isNotEmpty().allMatch(c -> c.annee() >= DEBUT && c.annee() <= FIN);
        assertThat(f.anomalies().reglementsSansDate()).usingElementComparator(Comparator.naturalOrder())
                .contains(BigDecimal.ONE, m("20"));
    }
}
