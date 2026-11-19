package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Compte;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DEMANDE_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.MESSAGE_DEPART;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TRAVAUX;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.VALIDEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Règle 6 (FEATURE-n2-travaux-chantier.md) : droit de statut de création D1 (403 + message OMRAN), statut visible D2
 * (absent / 404), périmètre projet D3 (projet non habilité → 404 en lecture et en écriture, jour sans projet visible).
 * Visibilité des statuts réelle ({@code IntervenantCourant}), jamais simulée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierVisibiliteTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String MARS = "du=2077-03-01&au=2077-03-31";
    private static final String[] TOUS_LES_DROITS = {DROIT_LIRE, DROIT_MODIFIER, DROIT_SUPPRIMER};
    private static final String PAR_PKEY = "SELECT COUNT(*) FROM GrhWorkedDay WHERE Pkey = ?";
    private static final String PAR_CODE = "SELECT COUNT(*) FROM GrhWorkedDay WHERE Code = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID projetNonHabilite;
    private UUID zone;
    private UUID type;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GRV-" + jeton);
        projetNonHabilite = fixture.projet(chantier.societe(), "GRN-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        type = fixture.type("TV-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
    }

    private UUID jour(UUID projet, UUID statut, String code) {
        return fixture.jour(new Jour(chantier.societe(), projet, type, zone, statut, DATE.atStartOfDay(), code, null));
    }

    private Map<String, Object> corps(UUID projet, String code) {
        return AppelsTravauxChantier.ecriture(chantier.societe(), projet, type, zone, DATE, code);
    }

    /** Compte restreint ({@code WithSequenceFreeRestriction = 1}) qui voit seulement ces statuts, habilité sur le projet. */
    private RequestPostProcessor restreint(String suffixe, UUID... statutsVisibles) {
        Compte c = fixture.compte(jeton + suffixe, true, List.of(chantier.projet()), TOUS_LES_DROITS);
        for (UUID statut : statutsVisibles) {
            fixture.intervenant(statut, c.role());
        }
        return c.connexion();
    }

    /** Compte exempté de la clause de statut, habilité sur le seul projet du chantier. */
    private RequestPostProcessor libre(String suffixe) {
        return fixture.compte(jeton + suffixe, false, List.of(chantier.projet()), TOUS_LES_DROITS).connexion();
    }

    // ------------------------------------------------------------------ D1 droit de statut de création

    @Test
    void d1_restreint_departVisible_201_statutEnCours() throws Exception {
        String code = "D1V-" + jeton;

        Reponse r = appels.creer(corps(chantier.projet(), code), restreint("a", EN_COURS));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(fixture.lire("SELECT CAST(StrStatusId AS varchar(36)) FROM GrhWorkedDay WHERE Code = ?", code))
                .isEqualToIgnoringCase(EN_COURS.toString());
    }

    @Test
    void d1_restreint_seulementUnStatutNonDepart_403_messageOmran() throws Exception {
        String code = "D1N-" + jeton;

        Reponse r = appels.creer(corps(chantier.projet(), code), restreint("b", DEMANDE_VALIDATION));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        assertThat(r.<String>lire("$.detail")).isEqualTo(MESSAGE_DEPART);
        assertThat(fixture.compter(PAR_CODE, code)).isZero();
    }

    @Test
    void d1_restreint_aucunStatutVisible_403_messageOmran() throws Exception {
        String code = "D1Z-" + jeton;

        Reponse r = appels.creer(corps(chantier.projet(), code), restreint("c"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        assertThat(r.<String>lire("$.detail")).isEqualTo(MESSAGE_DEPART);
        assertThat(fixture.compter(PAR_CODE, code)).isZero();
    }

    // ------------------------------------------------------------------ D2 statut visible en lecture

    @Test
    void d2_liste_statutNonVisible_absent() throws Exception {
        UUID visible = jour(chantier.projet(), EN_COURS, "D2A-" + jeton);
        UUID masque = jour(chantier.projet(), VALIDEE, "D2B-" + jeton);

        List<String> ids = appels.ids(MARS, restreint("d", EN_COURS));

        assertThat(ids).contains(visible.toString()).doesNotContain(masque.toString());
    }

    @Test
    void d2_fiche_statutNonVisible_404() throws Exception {
        UUID visible = jour(chantier.projet(), EN_COURS, "D2C-" + jeton);
        UUID masque = jour(chantier.projet(), VALIDEE, "D2D-" + jeton);
        RequestPostProcessor qui = restreint("e", EN_COURS);

        Reponse temoin = appels.appeler(get(TRAVAUX + "/" + visible), qui);
        Reponse r = appels.appeler(get(TRAVAUX + "/" + masque), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void d2_put_statutNonVisible_404_inchange() throws Exception {
        UUID visible = jour(chantier.projet(), EN_COURS, "D2E-" + jeton);
        UUID masque = jour(chantier.projet(), VALIDEE, "D2F-" + jeton);
        RequestPostProcessor qui = restreint("f", EN_COURS);

        Reponse temoin = appels.modifier(visible, corps(chantier.projet(), "D2E-" + jeton), qui);
        Reponse r = appels.modifier(masque, corps(chantier.projet(), "D2F-MODIFIE-" + jeton), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.compter(PAR_CODE, "D2F-" + jeton)).isEqualTo(1);
    }

    /** R5 : suppression permise seulement si le statut courant est visible. */
    @Test
    void r5_delete_statutNonVisible_404_conserve() throws Exception {
        UUID visible = jour(chantier.projet(), EN_COURS, "D2G-" + jeton);
        UUID masque = jour(chantier.projet(), VALIDEE, "D2H-" + jeton);
        RequestPostProcessor qui = restreint("g", EN_COURS);

        Reponse temoin = appels.appeler(delete(TRAVAUX + "/" + visible), qui);
        Reponse r = appels.appeler(delete(TRAVAUX + "/" + masque), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(204);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.compter(PAR_PKEY, masque)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ D3 périmètre projet

    @Test
    void d3_liste_projetNonHabilite_absent_sansProjet_present() throws Exception {
        UUID habilite = jour(chantier.projet(), EN_COURS, "D3A-" + jeton);
        UUID horsPerimetre = jour(projetNonHabilite, EN_COURS, "D3B-" + jeton);
        UUID sansProjet = jour(null, EN_COURS, "D3C-" + jeton);

        List<String> ids = appels.ids(MARS, libre("h"));

        assertThat(ids).contains(habilite.toString(), sansProjet.toString()).doesNotContain(horsPerimetre.toString());
    }

    @Test
    void d3_liste_projetNonHabiliteDemandeExplicitement_absent() throws Exception {
        UUID habilite = jour(chantier.projet(), EN_COURS, "D3D-" + jeton);
        UUID horsPerimetre = jour(projetNonHabilite, EN_COURS, "D3E-" + jeton);

        List<String> ids = appels.ids(MARS + "&projetIds=" + chantier.projet() + "," + projetNonHabilite, libre("i"));

        assertThat(ids).contains(habilite.toString()).doesNotContain(horsPerimetre.toString());
    }

    @Test
    void d3_fiche_projetNonHabilite_404_sansProjet_200() throws Exception {
        UUID horsPerimetre = jour(projetNonHabilite, EN_COURS, "D3F-" + jeton);
        UUID sansProjet = jour(null, EN_COURS, "D3G-" + jeton);
        RequestPostProcessor qui = libre("j");

        Reponse temoin = appels.appeler(get(TRAVAUX + "/" + sansProjet), qui);
        Reponse r = appels.appeler(get(TRAVAUX + "/" + horsPerimetre), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void d3_post_projetNonHabilite_404_rienEcrit() throws Exception {
        RequestPostProcessor qui = libre("k");

        Reponse temoin = appels.creer(corps(chantier.projet(), "D3H-" + jeton), qui);
        Reponse r = appels.creer(corps(projetNonHabilite, "D3I-" + jeton), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(201);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.compter(PAR_CODE, "D3I-" + jeton)).isZero();
    }

    @Test
    void d3_put_jourDUnProjetNonHabilite_404_inchange() throws Exception {
        UUID habilite = jour(chantier.projet(), EN_COURS, "D3J-" + jeton);
        UUID horsPerimetre = jour(projetNonHabilite, EN_COURS, "D3K-" + jeton);
        RequestPostProcessor qui = libre("l");

        Reponse temoin = appels.modifier(habilite, corps(chantier.projet(), "D3J-" + jeton), qui);
        Reponse r = appels.modifier(horsPerimetre, corps(chantier.projet(), "D3K-MODIFIE-" + jeton), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.compter(PAR_CODE, "D3K-" + jeton)).isEqualTo(1);
    }

    @Test
    void d3_put_versUnProjetNonHabilite_404_inchange() throws Exception {
        UUID habilite = jour(chantier.projet(), EN_COURS, "D3L-" + jeton);
        RequestPostProcessor qui = libre("m");

        Reponse temoin = appels.modifier(habilite, corps(chantier.projet(), "D3L-" + jeton), qui);
        Reponse r = appels.modifier(habilite, corps(projetNonHabilite, "D3L-" + jeton), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.lire("SELECT CAST(PrjProjectId AS varchar(36)) FROM GrhWorkedDay WHERE Pkey = ?", habilite))
                .isEqualToIgnoringCase(chantier.projet().toString());
    }

    @Test
    void d3_delete_projetNonHabilite_404_conserve() throws Exception {
        UUID habilite = jour(chantier.projet(), EN_COURS, "D3M-" + jeton);
        UUID horsPerimetre = jour(projetNonHabilite, EN_COURS, "D3N-" + jeton);
        RequestPostProcessor qui = libre("n");

        Reponse temoin = appels.appeler(delete(TRAVAUX + "/" + habilite), qui);
        Reponse r = appels.appeler(delete(TRAVAUX + "/" + horsPerimetre), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(204);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.compter(PAR_PKEY, horsPerimetre)).isEqualTo(1);
    }
}
