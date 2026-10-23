package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.sousTotaux;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.REMISE_PIECES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 32 REMISE DE PIÈCES BANCAIRES (FEATURE-tdb-vente-rapports-dossiers.md §6, §9 « N° 32 »). Période du 01/03/2026 au
 * 31/03/2026, module COM, encaissements dont la pièce est remise au centre :
 * <pre>
 *   r1  : d1 (T, client C1 + co-acquéreur C2), 300 000, remis le 01/03 00:00, échéance 10/02, article -R1
 *   r2  : d2 (T, client C3),                   250 000, remis le 31/03 15:00, échéance 05/02, article -R2
 *   r10 : 500 000 = 200 000 sur d3 (T, client C4) + 300 000 sur dx (TI entité INACTIVE, client CX),
 *         remis le 15/03, échéance 01/03, article -R10
 *   exclus (sur d2) : -E1 décaissement, -E2 pièce non remise, -E3 date de remise vide, -E4 autre module,
 *         -E5 CANCELED, -E6 remis le 01/04 00:00, -E7 remis le 28/02 23:00
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RemisePiecesBancairesRapportTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String SECTION = "remises";
    private static final String ARTICLE = "article";
    private static final String MONTANT = "montant";
    private static final String CLIENTS = "clients";
    private static final String TRANCHE = "tranche";
    private static final String CANCELED = "CANCELED";
    private static final BigDecimal PARASITE = new BigDecimal("1777");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsDossiers rd;

    private AppelsRapports appels;
    private String prefixe;
    private RequestPostProcessor libre;
    private CfgTranche t;
    private CfgTranche ti;
    private StkItemCategory categorie;
    private StkItemType type;
    private StrStatus enCours;
    private UUID moduleCom;
    private int numero;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("RPB");
        fixture.tables();
        fixture.rapportVente(REMISE_PIECES, "REMISE DE PIÈCES BANCAIRES", "VENTE", 2100);
        libre = connecte(fixture.compte("rpb." + prefixe, fixture.role("Libre " + prefixe, false)));

        t = ev.tranche("T-" + prefixe, ev.projet("P-" + prefixe, true), true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        enCours = ev.statutEnCours("ENC-" + prefixe);
        moduleCom = rd.moduleVente();

        ComFolder d1 = dossier(t, "C1");
        rd.coAcquereur(d1, ev.client("C2-" + prefixe));
        ComDocument e2 = echeance(dossier(t, "C3"));
        ComDocument e3 = echeance(dossier(t, "C4"));
        ComDocument ex = echeance(dossier(ti, "CX"));

        remise(echeance(d1), "R1", "300000", jour(1, 0), date(2, 10));
        remise(e2, "R2", "250000", LocalDateTime.of(2026, 3, 31, 15, 0), date(2, 5));
        StlSettlement r10 = entete("R10", "500000", jour(15, 0), date(3, 1));
        rd.ligne(r10, e3, new BigDecimal("200000"));
        rd.ligne(r10, ex, new BigDecimal("300000"));

        remise(e2, "E1", PARASITE.toPlainString(), jour(10, 0), date(2, 1)).setIsCredit(false);
        remise(e2, "E2", PARASITE.toPlainString(), jour(10, 0), date(2, 1)).setIsDcoumentDeliveredToCenter(false);
        remise(e2, "E3", PARASITE.toPlainString(), jour(10, 0), date(2, 1)).setDeliveredToCenterDate(null);
        remise(e2, "E4", PARASITE.toPlainString(), jour(10, 0), date(2, 1))
                .setAdmModuleId(rd.autreModule("ZZ-" + prefixe));
        remise(e2, "E5", PARASITE.toPlainString(), jour(10, 0), date(2, 1)).setStatus(CANCELED);
        remise(e2, "E6", PARASITE.toPlainString(), LocalDateTime.of(2026, 4, 1, 0, 0), date(2, 1));
        remise(e2, "E7", PARASITE.toPlainString(), LocalDateTime.of(2026, 2, 28, 23, 0), date(2, 1));
        fixture.relire();
    }

    private static LocalDateTime date(int mois, int jour) {
        return LocalDateTime.of(2026, mois, jour, 0, 0);
    }

    private static LocalDateTime jour(int jourDeMars, int heure) {
        return LocalDateTime.of(2026, 3, jourDeMars, heure, 0);
    }

    private ComFolder dossier(CfgTranche tranche, String client) {
        numero++;
        CfgTier acquereur = ev.client(client + "-" + prefixe);
        return ev.vendre(ev.article(prefixe + "-A" + numero, tranche, categorie, type),
                "DOS-" + prefixe + "-" + numero, enCours, acquereur, null);
    }

    private ComDocument echeance(ComFolder dossier) {
        return ev.echeance(dossier, Nature.AVANCE, true, new BigDecimal("900000"));
    }

    private String article(String suffixe) {
        return prefixe + "-" + suffixe;
    }

    /** Encaissement COM, pièce remise au centre le {@code remiseLe}, sans ligne. */
    private StlSettlement entete(String suffixe, String montant, LocalDateTime remiseLe, LocalDateTime echeance) {
        StlSettlement s = rd.reglement(new BigDecimal(montant), true, null, remiseLe, echeance);
        s.setIsDcoumentDeliveredToCenter(true);
        s.setDeliveredToCenterDate(remiseLe);
        s.setAdmModuleId(moduleCom);
        s.setItemNumber(article(suffixe));
        s.setStatus("POINTED");
        return s;
    }

    private StlSettlement remise(ComDocument echeance, String suffixe, String montant, LocalDateTime remiseLe,
                                 LocalDateTime dateEcheance) {
        StlSettlement s = entete(suffixe, montant, remiseLe, dateEcheance);
        rd.ligne(s, echeance, new BigDecimal(montant));
        return s;
    }

    private MockHttpServletRequestBuilder mars(RequestPostProcessor qui) {
        return get(resultat(VENTE, REMISE_PIECES)).param("du", "2026-03-01").param("au", "2026-03-31").with(qui);
    }

    private Map<String, Object> remises(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), SECTION);
    }

    private static List<Object> articles(Map<String, Object> section) {
        return lignes(section).stream().map(l -> l.get(ARTICLE)).toList();
    }

    private Map<String, Object> ligne(Map<String, Object> section, String suffixe) {
        List<Map<String, Object>> trouvees = lignesOu(section, ARTICLE, article(suffixe));
        assertThat(trouvees).as("ligne %s", suffixe).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ forme, période, tri

    @Test
    void n32_colonnes_dontDateDeRemise() throws Exception {
        Map<String, Object> section = remises(mars(libre));

        assertThat(colonnes(section).stream().map(c -> c.get("nom")).toList())
                .contains(TRANCHE, ARTICLE, "echeance", CLIENTS, MONTANT, "dateRemise");
    }

    @Test
    void n32_bornesIncluses_premierJourAMinuit_dernierJourA15h() throws Exception {
        assertThat(articles(remises(mars(libre)))).contains(article("R1"), article("R2"));
    }

    @Test
    void n32_triParEcheancePuisArticle_seulesLesRemisesRetenues() throws Exception {
        assertThat(articles(remises(mars(libre)))).containsExactly(article("R2"), article("R1"), article("R10"));
    }

    @ParameterizedTest(name = "{0} exclu")
    @ValueSource(strings = {"E1", "E2", "E3", "E4", "E5", "E6", "E7"})
    void n32_exclusions_decaissement_nonRemis_sansDate_autreModule_canceled_horsPeriode(String suffixe)
            throws Exception {
        assertThat(articles(remises(mars(libre)))).isNotEmpty().doesNotContain(article(suffixe));
    }

    // ------------------------------------------------------------------ clients, règlement multi-dossiers

    @Test
    void n32_clients_principalEtCoAcquereur() throws Exception {
        String clients = String.valueOf(ligne(remises(mars(libre)), "R1").get(CLIENTS));

        assertThat(clients).contains("C1-" + prefixe, "C2-" + prefixe);
    }

    @Test
    void rv_r4a_multiDossiers_clientsDesSeulesLignesVisibles() throws Exception {
        String clients = String.valueOf(ligne(remises(mars(libre)), "R10").get(CLIENTS));

        assertThat(clients).contains("C4-" + prefixe).doesNotContain("CX-" + prefixe);
    }

    @Test
    void rv_r4a_partiellementVisible_sommeDesLignesVisibles_etAvertissement() throws Exception {
        String corps = appels.corps(mars(libre));

        List<Object> montants = JsonPath.read(corps,
                "$.sections[?(@.id == 'remises')].lignes[?(@.article == '" + article("R10") + "')].montant");
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");
        assertThat(montants).hasSize(1);
        assertThat(nombre(montants.get(0))).isEqualByComparingTo("200000");
        assertThat(avertissements).anySatisfy(a -> assertThat(a).containsIgnoringCase("partiellement visible"));
    }

    @Test
    void rv_r4a_tranche_seulementLesTranchesVisibles() throws Exception {
        Map<String, Object> section = remises(mars(libre));

        assertThat(ligne(section, "R10").get(TRANCHE)).isNotNull().isEqualTo(ligne(section, "R1").get(TRANCHE));
    }

    // ------------------------------------------------------------------ totaux, filtre tranche, refus

    @Test
    void n32_totalEtSousTotalDeTranche() throws Exception {
        Map<String, Object> section = remises(mars(libre));

        assertThat(section).containsEntry("groupePar", TRANCHE);
        assertThat(nombre(totaux(section).get(MONTANT))).isEqualByComparingTo("750000");
        Map<String, Map<String, Object>> sousTotaux = sousTotaux(section);
        assertThat(sousTotaux).hasSize(1);
        assertThat(sousTotaux.values()).singleElement()
                .satisfies(v -> assertThat(nombre(v.get(MONTANT))).isEqualByComparingTo("750000"));
    }

    @Test
    void n32_trancheChoisie_pasDErreur_filtrePropre() throws Exception {
        Map<String, Object> section = remises(mars(libre).param(TRANCHE_ID, t.getId().toString()));

        assertThat(articles(section)).containsExactly(article("R2"), article("R1"), article("R10"));
    }

    @Test
    void rv_r4a_societeInactive_aucuneRemise() throws Exception {
        assertThat(lignes(remises(mars(libre).param(TRANCHE_ID, ti.getId().toString())))).isEmpty();
    }

    @Test
    void rv_r4a_projetHorsPerimetre_aucuneRemise() throws Exception {
        AdmUser compte = fixture.compte("rpb.hp." + prefixe, fixture.role("Hors " + prefixe, false));
        ev.affecter(compte, ev.projet("PX-" + prefixe, true));
        fixture.relire();

        assertThat(lignes(remises(mars(connecte(compte))))).isEmpty();
    }
}
