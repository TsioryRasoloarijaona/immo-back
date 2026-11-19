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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TRAVAUX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Droits d'écran D4 / R8 de Travaux chantier : 403 sans le code {@code AdmRight}, chaque refus avec son témoin 200. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierSecuriteTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String MARS = "?du=2077-03-01&au=2077-03-31";
    private static final String PAR_CODE = "SELECT COUNT(*) FROM GrhWorkedDay WHERE Code = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID zone;
    private UUID type;
    private UUID jour;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GRD-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        type = fixture.type("TD-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
        jour = semer("JD-" + jeton);
    }

    private UUID semer(String code) {
        return fixture.jour(new Jour(chantier.societe(), chantier.projet(), type, zone, EN_COURS, DATE.atStartOfDay(),
                code, null));
    }

    private RequestPostProcessor avec(String suffixe, String... droits) {
        return fixture.compte(jeton + suffixe, false, List.of(chantier.projet()), droits).connexion();
    }

    private Map<String, Object> corps(String code) {
        return AppelsTravauxChantier.ecriture(chantier.societe(), chantier.projet(), type, zone, DATE, code);
    }

    private static void refuse(Reponse temoin, int attenduTemoin, Reponse r) {
        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(attenduTemoin);
        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void d4_liste_sansGrhWorkedDayView_403() throws Exception {
        Reponse temoin = appels.appeler(get(TRAVAUX + MARS), avec("a", DROIT_LIRE));
        Reponse r = appels.appeler(get(TRAVAUX + MARS), avec("b", DROIT_MODIFIER, DROIT_SUPPRIMER));

        refuse(temoin, 200, r);
    }

    @Test
    void d4_fiche_sansGrhWorkedDayView_403() throws Exception {
        Reponse temoin = appels.appeler(get(TRAVAUX + "/" + jour), avec("c", DROIT_LIRE));
        Reponse r = appels.appeler(get(TRAVAUX + "/" + jour), avec("d", DROIT_MODIFIER, DROIT_SUPPRIMER));

        refuse(temoin, 200, r);
    }

    @Test
    void d4_creation_sansToolNew_403_rienEcrit() throws Exception {
        Reponse temoin = appels.creer(corps("CT-" + jeton), avec("e", DROIT_LIRE, DROIT_MODIFIER));
        Reponse r = appels.creer(corps("CR-" + jeton), avec("f", DROIT_LIRE, DROIT_SUPPRIMER));

        refuse(temoin, 201, r);
        assertThat(fixture.compter(PAR_CODE, "CR-" + jeton)).isZero();
    }

    @Test
    void d4_modification_sansToolNew_403_inchange() throws Exception {
        Reponse temoin = appels.modifier(jour, corps("JD-" + jeton), avec("g", DROIT_LIRE, DROIT_MODIFIER));
        Reponse r = appels.modifier(jour, corps("JD-MODIFIE-" + jeton), avec("h", DROIT_LIRE, DROIT_SUPPRIMER));

        refuse(temoin, 200, r);
        assertThat(fixture.compter(PAR_CODE, "JD-" + jeton)).isEqualTo(1);
    }

    @Test
    void d4_suppression_sansToolDelete_403_conserve() throws Exception {
        UUID autre = semer("JDT-" + jeton);

        Reponse temoin = appels.appeler(delete(TRAVAUX + "/" + autre), avec("i", DROIT_LIRE, DROIT_SUPPRIMER));
        Reponse r = appels.appeler(delete(TRAVAUX + "/" + jour), avec("j", DROIT_LIRE, DROIT_MODIFIER));

        refuse(temoin, 204, r);
        assertThat(fixture.compter(PAR_CODE, "JD-" + jeton)).isEqualTo(1);
    }
}
