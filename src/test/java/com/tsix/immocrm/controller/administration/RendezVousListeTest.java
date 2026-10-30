package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureRendezVous;
import com.tsix.immocrm.fixtures.FixtureTaches;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.FICHE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.RACINE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_DEBUT;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_FIN;
import static com.tsix.immocrm.fixtures.FixtureTaches.GESTIONNAIRE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.TEXTE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lecture des rendez-vous — FEATURE-admin-rendez-vous.md, règles 1, 2, 3, filtre {@code gestionnaireId}, {@code RendezVousDto}.
 * Sauf test de {@code texte}, chaque appel filtre sur le jeton du test : aucune ligne d'un autre test n'entre en compte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RendezVousListeTest {

    private static final String TOTAL = "$.totalElements";
    private static final String CODES = "$.content[*].code";
    private static final String IDS = "$.content[*].id";
    private static final String CONTENU = "$.content[0].";
    private static final String RACINE_JSON = "$.";
    private static final String COLONNE_DECLENCHEUR = "CfgTierTriggerId";
    private static final String COLONNE_DESCRIPTION = "Description";
    private static final String COLONNE_LIBELLE_SOURCE = "ObjectLabel";
    private static final String COLONNE_DEBUT = "StartDatePrev";
    private static final String COLONNE_FIN = "DueDatePrev";
    private static final String MOTIF = "MOTIF";
    private static final String LE_10 = "2031-03-10";
    private static final String LE_12 = "2031-03-12";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);

    /** Colonnes de la règle 2. */
    enum Colonne { CODE, DESCRIPTION, DECLENCHEUR_CODE, DECLENCHEUR_NOM, LIBELLE_SOURCE }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches taches;
    @Autowired private FixtureRendezVous fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        taches.assurer();
        admin = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
    }

    private static String nouveauJeton() {
        return FixtureReferentielsAdmin.jeton();
    }

    private ResultActions appeler(String texte, String... paires) throws Exception {
        MockHttpServletRequestBuilder requete = get(RACINE).with(connecte(admin)).param(TEXTE, texte);
        for (int i = 0; i < paires.length; i += 2) {
            requete.param(paires[i], paires[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    private ResultActions lister(String... paires) throws Exception {
        return appeler(jeton, paires).andExpect(status().isOk());
    }

    private String code(String suffixe) {
        return suffixe + "-" + jeton;
    }

    private void seul(String texte, UUID attendu) throws Exception {
        appeler(texte).andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(IDS, contains(attendu.toString())));
    }

    /** Rendez-vous dont seule la colonne {@code c} contient {@code valeur}. */
    private UUID portant(Colonne c, String valeur) {
        UUID id = fixture.rendezVous(c == Colonne.CODE ? valeur : "RDV-" + nouveauJeton(), JOUR);
        switch (c) {
            case CODE -> { /* posé à la création */ }
            case DESCRIPTION -> taches.definir(id, COLONNE_DESCRIPTION, valeur);
            case DECLENCHEUR_CODE -> taches.definir(id, COLONNE_DECLENCHEUR, fixture.tier(valeur, "Nom " + nouveauJeton()));
            case DECLENCHEUR_NOM -> taches.definir(id, COLONNE_DECLENCHEUR, fixture.tier("TIE-" + nouveauJeton(), valeur));
            case LIBELLE_SOURCE -> taches.definir(id, COLONNE_LIBELLE_SOURCE, valeur);
        }
        return id;
    }

    // ------------------------------------------------------------------ règle 1

    @Test
    void r1_liste_neRenvoieQueLEntiteRendezVous_jamaisUneTache() throws Exception {
        UUID rendezVous = fixture.rendezVous(code("RDV"), JOUR);
        taches.definir(taches.tache(code("TAC")), COLONNE_DEBUT, JOUR);

        lister().andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(IDS, contains(rendezVous.toString())));
    }

    @Test
    void r1_liste_trieeParDatePrevue_puisCode() throws Exception {
        fixture.rendezVous(code("B"), JOUR.plusDays(1));
        fixture.rendezVous(code("D"), JOUR.plusDays(2));
        fixture.rendezVous(code("C"), JOUR);
        fixture.rendezVous(code("A"), JOUR.plusDays(1));

        lister().andExpect(jsonPath(CODES, contains(code("C"), code("A"), code("B"), code("D"))));
    }

    @Test
    void liste_pagineeParPageEtSize() throws Exception {
        fixture.rendezVous(code("A"), JOUR);
        fixture.rendezVous(code("B"), JOUR.plusDays(1));

        lister("page", "1", "size", "1")
                .andExpect(jsonPath(TOTAL).value(2))
                .andExpect(jsonPath(CODES, contains(code("B"))));
    }

    // ------------------------------------------------------------------ règle 2

    @ParameterizedTest(name = "{0}")
    @EnumSource(Colonne.class)
    void r2_texte_chercheDansLaColonne(Colonne c) throws Exception {
        String motif = MOTIF + nouveauJeton();
        UUID retenu = portant(c, motif);
        fixture.rendezVous("HORS-" + nouveauJeton(), JOUR);

        seul(motif, retenu);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Colonne.class)
    void r2_espacesDeLaSaisie_ignores(Colonne c) throws Exception {
        String suite = nouveauJeton();
        UUID retenu = portant(c, MOTIF + suite);

        seul(" MO TIF " + suite + " ", retenu);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Colonne.class)
    void r2_espacesDeLaColonne_ignores(Colonne c) throws Exception {
        String suite = nouveauJeton();
        UUID retenu = portant(c, "MO TIF " + suite);

        seul(MOTIF + suite, retenu);
    }

    /** La liste du spec est fermée. */
    @Test
    void r2_texte_neCherchePasDansLesNotes() throws Exception {
        String motif = MOTIF + nouveauJeton();
        taches.definir(fixture.rendezVous(code("RDV"), JOUR), "Notes", motif);

        appeler(motif).andExpect(status().isOk()).andExpect(jsonPath(TOTAL).value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "["})
    void r2_joker_prisALaLettre(String joker) throws Exception {
        String suite = nouveauJeton();
        String motif = "A" + joker + "B" + suite;
        UUID litteral = portant(Colonne.DESCRIPTION, motif);
        portant(Colonne.DESCRIPTION, "AxB" + suite);

        seul(motif, litteral);
    }

    // ------------------------------------------------------------------ règle 3

    @Test
    void r3_bornesIncluses_surToutLeDernierJour() throws Exception {
        fixture.rendezVous(code("AVANT"), LocalDateTime.of(2031, 3, 9, 23, 59));
        fixture.rendezVous(code("DEBUT"), LocalDateTime.of(2031, 3, 10, 0, 0));
        fixture.rendezVous(code("FIN"), LocalDateTime.of(2031, 3, 12, 23, 59));
        fixture.rendezVous(code("APRES"), LocalDateTime.of(2031, 3, 13, 0, 0));

        lister(DATE_DEBUT, LE_10, DATE_FIN, LE_12).andExpect(jsonPath(CODES, contains(code("DEBUT"), code("FIN"))));
    }

    @Test
    void r3_dateDebutSeule() throws Exception {
        fixture.rendezVous(code("AVANT"), JOUR.minusDays(1));
        fixture.rendezVous(code("RETENU"), JOUR);

        lister(DATE_DEBUT, LE_10).andExpect(jsonPath(CODES, contains(code("RETENU"))));
    }

    @Test
    void r3_dateFinSeule() throws Exception {
        fixture.rendezVous(code("RETENU"), JOUR);
        fixture.rendezVous(code("APRES"), JOUR.plusDays(1));

        lister(DATE_FIN, LE_10).andExpect(jsonPath(CODES, contains(code("RETENU"))));
    }

    @Test
    void r3_filtrePorteSurStartDatePrev_pasSurDueDatePrev() throws Exception {
        UUID ecarte = fixture.rendezVous(code("ECARTE"), JOUR.minusDays(1));
        taches.definir(ecarte, COLONNE_FIN, JOUR.plusDays(1));
        fixture.rendezVous(code("RETENU"), JOUR);

        lister(DATE_DEBUT, LE_10, DATE_FIN, LE_12).andExpect(jsonPath(CODES, contains(code("RETENU"))));
    }

    @Test
    void r3_dateFinAvantDateDebut_400() throws Exception {
        appeler(jeton, DATE_DEBUT, LE_12, DATE_FIN, LE_10).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ gestionnaireId

    @Test
    void filtre_gestionnaireId() throws Exception {
        UUID gestionnaire = fixture.tier(code("GES"), "Gestionnaire");
        UUID autre = fixture.tier(code("GET"), "Autre");
        taches.definir(fixture.rendezVous(code("A"), JOUR), "CfgTierManagerId", autre);
        UUID retenu = fixture.rendezVous(code("B"), JOUR);
        taches.definir(retenu, "CfgTierManagerId", gestionnaire);

        lister(GESTIONNAIRE_ID, gestionnaire.toString())
                .andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(IDS, contains(retenu.toString())));
    }

    // ------------------------------------------------------------------ RendezVousDto

    private void exposeTousLesChamps(ResultActions reponse, String racine, UUID id, UUID declencheur, UUID gestionnaire,
                                     UUID tier) throws Exception {
        reponse.andExpect(jsonPath(racine + "id").value(id.toString()))
                .andExpect(jsonPath(racine + "code").value(code("RDV")))
                .andExpect(jsonPath(racine + "description").value("Tache " + code("RDV")))
                .andExpect(jsonPath(racine + "declencheurId").value(declencheur.toString()))
                .andExpect(jsonPath(racine + "declencheurCode").value(code("DEC")))
                .andExpect(jsonPath(racine + "declencheurNom").value("Declencheur " + jeton))
                .andExpect(jsonPath(racine + "datePrevue", startsWith("2031-03-10T09:30")))
                .andExpect(jsonPath(racine + "dureePrevue").value(3))
                .andExpect(jsonPath(racine + "dateFinPrevue", startsWith("2031-03-10T11:45")))
                .andExpect(jsonPath(racine + "gestionnaireId").value(gestionnaire.toString()))
                .andExpect(jsonPath(racine + "gestionnaireCode").value(code("GES")))
                .andExpect(jsonPath(racine + "gestionnaireNom").value("Gestionnaire " + jeton))
                .andExpect(jsonPath(racine + "tierId").value(tier.toString()))
                .andExpect(jsonPath(racine + "tierCode").value(code("TIE")))
                .andExpect(jsonPath(racine + "tierNom").value("Tier " + jeton))
                .andExpect(jsonPath(racine + "sourceId").value(tier.toString()))
                .andExpect(jsonPath(racine + "sourceLibelle").value("Prospection : " + jeton))
                .andExpect(jsonPath(racine + "sourceEntite").value("Entite " + jeton))
                .andExpect(jsonPath(racine + "notes").value("Notes " + jeton));
    }

    private UUID[] jeuComplet() {
        UUID declencheur = fixture.tier(code("DEC"), "Declencheur " + jeton);
        UUID gestionnaire = fixture.tier(code("GES"), "Gestionnaire " + jeton);
        UUID tier = fixture.tier(code("TIE"), "Tier " + jeton);
        UUID entite = fixture.entite(code("ENT"), "Entite " + jeton);
        UUID id = fixture.rendezVous(code("RDV"), JOUR);
        taches.definir(id, COLONNE_DECLENCHEUR, declencheur);
        taches.definir(id, "CfgTierManagerId", gestionnaire);
        taches.definir(id, "CfgProspectId", tier);
        taches.definir(id, "DelayPrev", 3);
        taches.definir(id, COLONNE_FIN, LocalDateTime.of(2031, 3, 10, 11, 45));
        taches.definir(id, "ObjectId", tier);
        taches.definir(id, COLONNE_LIBELLE_SOURCE, "Prospection : " + jeton);
        taches.definir(id, "ObjectStrEntityId", entite);
        taches.definir(id, "Notes", "Notes " + jeton);
        return new UUID[]{id, declencheur, gestionnaire, tier};
    }

    @Test
    void fiche_exposeLesChampsDeRendezVousDto() throws Exception {
        UUID[] j = jeuComplet();

        exposeTousLesChamps(mockMvc.perform(get(FICHE, j[0]).with(connecte(admin))).andExpect(status().isOk()),
                RACINE_JSON, j[0], j[1], j[2], j[3]);
    }

    @Test
    void liste_exposeLesChampsDeRendezVousDto() throws Exception {
        UUID[] j = jeuComplet();

        exposeTousLesChamps(lister(), CONTENU, j[0], j[1], j[2], j[3]);
    }
}
