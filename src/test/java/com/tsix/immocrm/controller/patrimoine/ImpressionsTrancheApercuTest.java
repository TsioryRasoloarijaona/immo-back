package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureImpressionsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CINQ_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CREDIT_ACCORDE;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CREDIT_EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.ENVOI_NOTAIRE;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.MEILLEURTAUX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lot P9f — aperçu de la Relance 2 par statut (FEATURE-patrimoine-impressions-tranche.md §1.1, §1.2, §2, §4 : P2-P5, M1, M2,
 * Q3 (reste ≤ 0 écarté, « Réponses du manager » 27/10), D1, Q1). Montants = {@code MontantsDossier} (règle 6) : comparés au
 * centime à {@code GET /api/dossiers/{id}/montants} et à des valeurs posées.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpressionsTrancheApercuTest {

    private static final String APERCU = "/api/tranches/{id}/relances-2/apercu";
    private static final String MONTANTS = "/api/dossiers/{id}/montants";
    private static final String STATUT = "statut";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureImpressionsTranche fixture;
    @Autowired private EntityManagerFactory emf;
    @Autowired private Clock clock;

    private AdmUser admin;
    private Jeu jeu;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeu = fixture.jeu();
    }

    // ------------------------------------------------------------------ outils

    private String apercu(Jeu j, String... statuts) throws Exception {
        MockHttpServletRequestBuilder requete = get(APERCU, j.tranche().getId()).with(connecte(admin));
        if (statuts.length > 0) {
            requete = requete.param(STATUT, statuts);
        }
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static boolean cite(String json, ComFolder dossier) {
        return json.contains(dossier.getId().toString()) || json.contains("\"" + dossier.getCode() + "\"");
    }

    private static List<Map<String, Object>> lignes(String json, ComFolder dossier) {
        return JsonPath.read(json, "$.lignes[?(@.dossierId == '" + dossier.getId() + "')]");
    }

    private static BigDecimal montant(Object valeur) {
        return new BigDecimal(String.valueOf(valeur));
    }

    private static long nombre(String json, String chemin) {
        return montant(JsonPath.read(json, chemin)).longValueExact();
    }

    /** {@code prixVente} de l'onglet Montants du dossier : la seule source des montants imprimés (§1.2). */
    private Map<String, Object> prixVenteDuDossier(ComFolder dossier) throws Exception {
        String json = mockMvc.perform(get(MONTANTS, dossier.getId()).with(connecte(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.prixVente");
    }

    // ------------------------------------------------------------------ P2 : les 5 statuts, sous-articles inclus

    private record Selection(List<ComFolder> retenus, List<ComFolder> exclus) {
    }

    private Selection selection() {
        String j = jeu.jeton();
        StrStatus concretisation = fixture.statut(CONCRETISATION);
        List<ComFolder> retenus = List.of(
                fixture.dossier(jeu, j + "-CON", concretisation),
                fixture.dossier(jeu, j + "-MT", fixture.statut(MEILLEURTAUX)),
                fixture.dossier(jeu, j + "-NOT", fixture.statut(ENVOI_NOTAIRE)),
                fixture.dossier(jeu, j + "-CEC", fixture.statut(CREDIT_EN_COURS)),
                fixture.dossier(jeu, j + "-CAC", fixture.statut(CREDIT_ACCORDE)),
                fixture.dossierSousArticle(jeu, j + "-SOUS", concretisation));
        List<ComFolder> exclus = List.of(
                fixture.dossier(jeu, j + "-ANN", fixture.annulation(jeu)),
                fixture.dossier(jeu, j + "-DES", fixture.desistement(jeu)),
                fixture.dossier(jeu, j + "-NUL", null),
                fixture.dossier(jeu, j + "-FOR", fixture.statut("FORMALISE")),
                fixture.dossier(jeu, j + "-RES", fixture.statut("RESERVATION")));
        fixture.relire();
        return new Selection(retenus, exclus);
    }

    @Test
    void p2_retientLesCinqStatutsEtLeSousArticle_exclutAnnuleDesisteSansStatutFormaliseReservation() throws Exception {
        Selection s = selection();

        String json = apercu(jeu);

        assertThat(s.retenus()).isNotEmpty()
                .allSatisfy(d -> assertThat(cite(json, d)).as("retenu : " + d.getCode()).isTrue());
        assertThat(s.exclus()).isNotEmpty()
                .allSatisfy(d -> assertThat(cite(json, d)).as("exclu : " + d.getCode()).isFalse());
    }

    @Test
    void p2_compteurs_entete_etParStatut() throws Exception {
        selection();

        String json = apercu(jeu);

        List<Object> concretisations = JsonPath.read(json, "$.parStatut[?(@.statut.code == 'CONCRETISATION')].dossiers");
        List<Object> lettresConcretisation =
                JsonPath.read(json, "$.parStatut[?(@.statut.code == 'CONCRETISATION')].lettres");
        List<String> retenus = JsonPath.read(json, "$.statutsRetenus");
        assertAll(
                () -> assertThat(nombre(json, "$.dossiers")).isEqualTo(6),
                () -> assertThat(nombre(json, "$.lettres")).isEqualTo(6),
                () -> assertThat(retenus).containsExactlyInAnyOrderElementsOf(CINQ_STATUTS),
                () -> assertThat(concretisations).singleElement().satisfies(n -> assertThat(montant(n)).isEqualByComparingTo("2")),
                () -> assertThat(lettresConcretisation).singleElement()
                        .satisfies(n -> assertThat(montant(n)).isEqualByComparingTo("2")),
                () -> assertThat(String.valueOf((Object) JsonPath.read(json, "$.trancheId")))
                        .isEqualTo(jeu.tranche().getId().toString()),
                () -> assertThat(String.valueOf((Object) JsonPath.read(json, "$.trancheCode")))
                        .isEqualTo(jeu.tranche().getCode()),
                () -> assertThat(String.valueOf((Object) JsonPath.read(json, "$.dateImpression")))
                        .startsWith(LocalDate.now(clock).toString()));
    }

    // ------------------------------------------------------------------ P3 / M2 : une lettre par acquéreur, mêmes montants

    @Test
    void p3_deuxCoAcquereurs_troisLignes_principalPuisCoAcquereurs_memesMontants() throws Exception {
        ComFolder dossier = fixture.prix(fixture.dossier(jeu, jeu.jeton() + "-CO", fixture.statut(CONCRETISATION)),
                "900000.50");
        fixture.echeance(dossier, Nature.AVANCE, "900000.50", "300000.25", null);
        fixture.coAcquereur(dossier, "COA" + jeu.jeton());
        fixture.coAcquereur(dossier, "COB" + jeu.jeton());
        fixture.relire();

        String json = apercu(jeu);

        List<Map<String, Object>> lignes = lignes(json, dossier);
        assertThat(nombre(json, "$.lettres")).isEqualTo(3);
        assertThat(lignes).hasSize(3);
        assertThat(lignes).filteredOn(l -> Boolean.FALSE.equals(l.get("estCoAcquereur"))).hasSize(1);
        assertThat(lignes).filteredOn(l -> Boolean.TRUE.equals(l.get("estCoAcquereur"))).hasSize(2);
        assertThat(lignes).allSatisfy(l -> assertAll(
                () -> assertThat(montant(l.get("prixVente"))).isEqualByComparingTo("900000.50"),
                () -> assertThat(montant(l.get("prixRegle"))).isEqualByComparingTo("300000.25"),
                () -> assertThat(montant(l.get("reste"))).isEqualByComparingTo("600000.25")));
        assertThat(lignes.stream().map(l -> String.valueOf(l.get("acquereur"))).toList())
                .anySatisfy(a -> assertThat(a).contains("COA" + jeu.jeton()))
                .anySatisfy(a -> assertThat(a).contains("COB" + jeu.jeton()))
                .anySatisfy(a -> assertThat(a).contains(dossier.getCfgCustomer().getLastName()));
    }

    // ------------------------------------------------------------------ P4 : la tranche de l'article, jamais celle de la prospection

    @Test
    void p4_tranchesAEtB_seulLArticleDecide_prospectionDUneAutreTrancheIgnoree() throws Exception {
        Jeu autre = fixture.jeu();
        ComFolder articleA = fixture.prospectionDans(
                fixture.dossier(jeu, jeu.jeton() + "-PA", fixture.statut(CONCRETISATION)), autre.tranche());
        ComFolder articleB = fixture.prospectionDans(
                fixture.dossier(autre, autre.jeton() + "-PB", fixture.statut(CONCRETISATION)), jeu.tranche());
        fixture.relire();

        String jsonA = apercu(jeu);
        String jsonB = apercu(autre);

        assertAll(
                () -> assertThat(cite(jsonA, articleA)).isTrue(),
                () -> assertThat(cite(jsonA, articleB)).isFalse(),
                () -> assertThat(cite(jsonB, articleB)).isTrue(),
                () -> assertThat(cite(jsonB, articleA)).isFalse());
    }

    // ------------------------------------------------------------------ P5 : filtre statut

    @Test
    void p5_statutConcretisationSeul_neRetientQueCeStatut() throws Exception {
        Selection s = selection();
        ComFolder meilleurTaux = s.retenus().get(1);

        String json = apercu(jeu, CONCRETISATION);

        List<String> retenus = JsonPath.read(json, "$.statutsRetenus");
        assertAll(
                () -> assertThat(retenus).containsExactly(CONCRETISATION),
                () -> assertThat(cite(json, s.retenus().get(0))).isTrue(),
                () -> assertThat(cite(json, s.retenus().get(5))).as("sous-article CONCRETISATION").isTrue(),
                () -> assertThat(cite(json, meilleurTaux)).isFalse(),
                () -> assertThat(nombre(json, "$.dossiers")).isEqualTo(2));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"RESERVATION", "FORMALISE", "INCONNU"})
    void p5_statutHorsDeLaListe_400(String statut) throws Exception {
        mockMvc.perform(get(APERCU, jeu.tranche().getId()).param(STATUT, CONCRETISATION, statut).with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ M1 : un seul calcul, au centime

    /**
     * Prix 1 234 567,89 ; avance 600 000 réglée 250 000,45 ; autofinancement réglé par un règlement CANCELED (99 999,99, ne
     * compte pas) ; frais 30 000 réglés 10 000 (hors prix) ⇒ réglé 250 000,45, reste 984 567,44.
     */
    @Test
    void m1_montantsAuCentime_fraisEtReglementAnnuleHorsPrix() throws Exception {
        ComFolder dossier = fixture.prix(fixture.dossier(jeu, jeu.jeton() + "-M1", fixture.statut(CONCRETISATION)),
                "1234567.89");
        fixture.echeance(dossier, Nature.AVANCE, "600000", "250000.45", null);
        fixture.echeance(dossier, Nature.AUTOFINANCEMENT, "634567.89", null, "99999.99");
        fixture.echeance(dossier, Nature.FRAIS, "30000", "10000", null);
        fixture.relire();

        String json = apercu(jeu);

        assertThat(lignes(json, dossier)).singleElement().satisfies(l -> assertAll(
                () -> assertThat(montant(l.get("prixVente"))).isEqualByComparingTo("1234567.89"),
                () -> assertThat(montant(l.get("prixRegle"))).isEqualByComparingTo("250000.45"),
                () -> assertThat(montant(l.get("reste"))).isEqualByComparingTo("984567.44")));
    }

    @Test
    void m1_reglePlusResteEgalPrix_etEgauxAuxMontantsDuDossier() throws Exception {
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);
        ComFolder a = fixture.prix(fixture.dossier(jeu, j + "-A", statut), "750000.10");
        fixture.echeance(a, Nature.AVANCE, "200000", "123456.78", null);
        fixture.echeance(a, Nature.CREDIT, "550000.10", "50000.01", "1000");
        ComFolder b = fixture.prix(fixture.dossier(jeu, j + "-B", statut), "480000");
        fixture.echeance(b, Nature.AUTOFINANCEMENT, "480000", "0.99", null);
        fixture.echeance(b, Nature.FRAIS, "12000", "12000", null);
        fixture.coAcquereur(b, "COB" + j);
        fixture.relire();

        String json = apercu(jeu);

        for (ComFolder dossier : List.of(a, b)) {
            Map<String, Object> attendu = prixVenteDuDossier(dossier);
            assertThat(lignes(json, dossier)).isNotEmpty().allSatisfy(l -> assertAll(
                    () -> assertThat(montant(l.get("prixRegle")).add(montant(l.get("reste"))))
                            .isEqualByComparingTo(montant(l.get("prixVente"))),
                    () -> assertThat(montant(l.get("prixVente"))).isEqualByComparingTo(montant(attendu.get("montant"))),
                    () -> assertThat(montant(l.get("prixRegle"))).isEqualByComparingTo(montant(attendu.get("regle"))),
                    () -> assertThat(montant(l.get("reste"))).isEqualByComparingTo(montant(attendu.get("reste")))));
        }
    }

    // ------------------------------------------------------------------ Q3 (prime sur M3) : reste ≤ 0 → pas de lettre, dans « ecartes »

    @Test
    void q3_resteNulOuNegatif_pasDeLettre_dossierListeDansEcartes() throws Exception {
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);
        ComFolder solde = fixture.prix(fixture.dossier(jeu, j + "-SOL", statut), "200000");
        fixture.echeance(solde, Nature.AVANCE, "200000", "200000", null);
        ComFolder tropPercu = fixture.prix(fixture.dossier(jeu, j + "-TP", statut), "100000");
        fixture.echeance(tropPercu, Nature.AVANCE, "100000", "100000.01", null);
        fixture.coAcquereur(tropPercu, "COT" + j);
        ComFolder du = fixture.dossier(jeu, j + "-DU", statut);
        fixture.relire();

        String json = apercu(jeu);

        String ecartes = String.valueOf((Object) JsonPath.read(json, "$.ecartes"));
        assertAll(
                () -> assertThat(nombre(json, "$.lettres")).isEqualTo(1),
                () -> assertThat(lignes(json, du)).hasSize(1),
                () -> assertThat(lignes(json, solde)).isEmpty(),
                () -> assertThat(lignes(json, tropPercu)).isEmpty(),
                () -> assertThat(nomme(ecartes, solde)).as("soldé listé dans ecartes").isTrue(),
                () -> assertThat(nomme(ecartes, tropPercu)).as("trop-perçu listé dans ecartes").isTrue(),
                () -> assertThat(nomme(ecartes, du)).isFalse());
    }

    /** Texte (non JSON) qui porte l'identifiant ou le code du dossier. */
    private static boolean nomme(String texte, ComFolder dossier) {
        return texte.contains(dossier.getId().toString()) || texte.contains(dossier.getCode());
    }

    // ------------------------------------------------------------------ D1 : date de relance 2

    @Test
    void d1_dateRelance2_renseigneeOuVide_dansLaLigne() throws Exception {
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);
        ComFolder avec = fixture.relance2(fixture.dossier(jeu, j + "-R2", statut), LocalDateTime.of(2026, 2, 14, 0, 0));
        ComFolder sans = fixture.dossier(jeu, j + "-SR", statut);
        fixture.relire();

        String json = apercu(jeu);

        assertAll(
                () -> assertThat(lignes(json, avec)).singleElement()
                        .satisfies(l -> assertThat(String.valueOf(l.get("dateRelance2"))).startsWith("2026-02-14")),
                () -> assertThat(lignes(json, sans)).singleElement()
                        .satisfies(l -> assertThat(l.get("dateRelance2")).isNull()));
    }

    // ------------------------------------------------------------------ T1 (réponse du manager 27/10) : sans modèle, aperçu 200

    @Test
    void t1_sansModeleActif_apercu200_modeleActifFaux() throws Exception {
        fixture.dossier(jeu, jeu.jeton() + "-T1", fixture.statut(CONCRETISATION));
        fixture.relire();

        mockMvc.perform(get(APERCU, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modeleActif").value(false))
                .andExpect(jsonPath("$.lettres").value(1));
    }

    // ------------------------------------------------------------------ Q1 : pas de requête par ligne

    @Test
    void q1_coutDe50Dossiers_borneParCeluiDe2() throws Exception {
        Jeu petit = jeu;
        Jeu grand = fixture.jeu();
        peupler(petit, 2);
        peupler(grand, 50);

        long coutPetit = requetes(petit);
        long coutGrand = requetes(grand);

        assertThat(coutGrand).isLessThanOrEqualTo(coutPetit + 5);
    }

    private void peupler(Jeu j, int nombre) {
        StrStatus statut = fixture.statut(CONCRETISATION);
        for (int i = 0; i < nombre; i++) {
            ComFolder d = fixture.dossier(j, j.jeton() + "-Q" + i, statut);
            fixture.echeance(d, Nature.AVANCE, "1000", "100", null);
            fixture.coAcquereur(d, "COQ" + i + j.jeton());
        }
        fixture.relire();
    }

    private long requetes(Jeu j) throws Exception {
        Statistics statistiques = emf.unwrap(SessionFactory.class).getStatistics();
        boolean active = statistiques.isStatisticsEnabled();
        statistiques.setStatisticsEnabled(true);
        try {
            fixture.relire();
            statistiques.clear();
            apercu(j);
            return statistiques.getPrepareStatementCount();
        } finally {
            statistiques.setStatisticsEnabled(active);
        }
    }
}
