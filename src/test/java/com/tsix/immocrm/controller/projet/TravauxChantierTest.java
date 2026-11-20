package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Jour;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.ValeursType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TRAVAUX;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.VALIDEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Liste et fiche Travaux chantier (FEATURE-n2-travaux-chantier.md § Endpoints, D7) : filtres, champs de ligne, total calculé. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierTest {

    private static final String MARS = "du=2077-03-01&au=2077-03-31";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private UUID typeB;
    private UUID employe;
    private UUID le10Mars;
    private UUID le31MarsApresMidi;
    private UUID le1erAvril;
    private UUID le28Fevrier;
    private UUID autreType;
    private UUID avecEmploye;
    private RequestPostProcessor lecteur;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        Chantier c = fixture.chantier("GRL-" + jeton);
        UUID zone = fixture.zone(c.cfgProjet(), "Z-" + jeton);
        UUID typeA = fixture.type("TA-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
        typeB = fixture.type("TB-" + jeton, new ValeursType("M2", "80", "0.2", "U", "5", "0.14"));
        employe = fixture.tiers("EMP-" + jeton);
        le10Mars = fixture.jour(new Jour(c.societe(), c.projet(), typeA, zone, EN_COURS,
                LocalDateTime.of(2077, 3, 10, 0, 0), "J10-" + jeton, null));
        le31MarsApresMidi = fixture.jour(new Jour(c.societe(), c.projet(), typeA, zone, VALIDEE,
                LocalDateTime.of(2077, 3, 31, 15, 0), "J31-" + jeton, null));
        le1erAvril = fixture.jour(new Jour(c.societe(), c.projet(), typeA, zone, EN_COURS,
                LocalDateTime.of(2077, 4, 1, 0, 0), "J01-" + jeton, null));
        le28Fevrier = fixture.jour(new Jour(c.societe(), c.projet(), typeA, zone, EN_COURS,
                LocalDateTime.of(2077, 2, 28, 0, 0), "J28-" + jeton, null));
        autreType = fixture.jour(new Jour(c.societe(), c.projet(), typeB, zone, EN_COURS,
                LocalDateTime.of(2077, 3, 12, 0, 0), "JTB-" + jeton, null));
        avecEmploye = fixture.jour(new Jour(c.societe(), c.projet(), typeA, zone, EN_COURS,
                LocalDateTime.of(2077, 3, 14, 0, 0), "JEM-" + jeton, employe));
        lecteur = fixture.compte(jeton, false, List.of(c.projet()), DROIT_LIRE).connexion();
    }

    private static String[] ids(UUID... jours) {
        return java.util.Arrays.stream(jours).map(UUID::toString).toArray(String[]::new);
    }

    // ------------------------------------------------------------------ période (du / au)

    @Test
    void periode_absente_aucunFiltreDeDate() throws Exception {
        List<String> liste = appels.ids("", lecteur);

        assertThat(liste).contains(ids(le10Mars, le31MarsApresMidi, le1erAvril, le28Fevrier));
    }

    @Test
    void periode_auInclusifALaJournee_duInclusif() throws Exception {
        List<String> liste = appels.ids(MARS, lecteur);

        assertThat(liste).contains(ids(le10Mars, le31MarsApresMidi)).doesNotContain(ids(le1erAvril, le28Fevrier));
    }

    // ------------------------------------------------------------------ statuts (rien coché = tout)

    @Test
    void statuts_rienCoche_tousLesStatuts() throws Exception {
        List<String> liste = appels.ids(MARS, lecteur);

        assertThat(liste).contains(ids(le10Mars, le31MarsApresMidi));
    }

    @Test
    void statuts_coches_restreignent() throws Exception {
        List<String> liste = appels.ids(MARS + "&statutIds=" + VALIDEE, lecteur);

        assertThat(liste).contains(ids(le31MarsApresMidi)).doesNotContain(ids(le10Mars, autreType));
    }

    // ------------------------------------------------------------------ type, personnel, recherche

    @Test
    void filtre_type() throws Exception {
        List<String> liste = appels.ids(MARS + "&typeId=" + typeB, lecteur);

        assertThat(liste).contains(ids(autreType)).doesNotContain(ids(le10Mars, avecEmploye));
    }

    @Test
    void filtre_personnel() throws Exception {
        List<String> liste = appels.ids(MARS + "&employeId=" + employe, lecteur);

        assertThat(liste).contains(ids(avecEmploye)).doesNotContain(ids(le10Mars, autreType));
    }

    @Test
    void recherche_parCode() throws Exception {
        List<String> liste = appels.ids(MARS + "&recherche=JTB-" + jeton, lecteur);

        assertThat(liste).containsExactly(ids(autreType));
    }

    @Test
    void recherche_parNomDuPersonnel() throws Exception {
        List<String> liste = appels.ids(MARS + "&recherche=EMP-" + jeton, lecteur);

        assertThat(liste).containsExactly(ids(avecEmploye));
    }

    @Test
    void listeVide_200_tableauVide() throws Exception {
        Reponse r = appels.appeler(get(TRAVAUX + "?" + MARS + "&recherche=AUCUN-" + jeton), lecteur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.<List<Object>>lire("$")).isEmpty();
    }

    // ------------------------------------------------------------------ ligne

    @Test
    void ligne_statutEtCode() throws Exception {
        Reponse r = appels.appeler(get(TRAVAUX + "?" + MARS), lecteur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        String ligne = "$[?(@.id == '" + le10Mars + "')]";
        assertThat(r.<List<String>>lire(ligne + ".statut.id")).containsExactly(EN_COURS.toString());
        assertThat(r.<List<String>>lire(ligne + ".statut.libelle")).containsExactly("En cours");
        assertThat(r.<List<String>>lire(ligne + ".code")).containsExactly("J10-" + jeton);
    }

    /** D7 : TotalHT = UnitPriceHT × Quantity, calculé au fil (3 × 150,5). */
    @Test
    void d7_totalHT_calculeAuFil() throws Exception {
        Reponse r = appels.appeler(get(TRAVAUX + "?" + MARS), lecteur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        String ligne = "$[?(@.id == '" + le10Mars + "')]";
        assertThat(r.<List<Number>>lire(ligne + ".totalHT")).singleElement()
                .satisfies(t -> assertThat(t.doubleValue()).isEqualTo(451.5));
        assertThat(r.<List<Number>>lire(ligne + ".quantite")).singleElement()
                .satisfies(q -> assertThat(q.doubleValue()).isEqualTo(3.0));
        assertThat(r.<List<Number>>lire(ligne + ".prixUnitaireHT")).singleElement()
                .satisfies(p -> assertThat(p.doubleValue()).isEqualTo(150.5));
    }

    // ------------------------------------------------------------------ fiche

    @Test
    void fiche_inconnue_404() throws Exception {
        Reponse temoin = appels.appeler(get(TRAVAUX + "/" + le10Mars), lecteur);
        Reponse r = appels.appeler(get(TRAVAUX + "/" + UUID.randomUUID()), lecteur);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }
}
