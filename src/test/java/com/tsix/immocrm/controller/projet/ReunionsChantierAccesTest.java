package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsReunions.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureReunionsChantier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_PLAGES;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.DROIT_TYPES;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.PLAGES;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.REUNIONS;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.STATUTS;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.TYPES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Visibilité D4 / R8 (projets habilités) et droits D7 (403 sans le droit OMRAN). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReunionsChantierAccesTest {

    private static final LocalDateTime LE_10_MARS = LocalDateTime.of(2026, 3, 10, 9, 30);
    private static final String DU_AU = "du=2026-03-01&au=2026-03-31";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReunionsChantier fixture;

    private AppelsReunions appels;
    private String jeton;
    private UUID projet;
    private UUID projetNonHabilite;
    private UUID type;
    private UUID visible;
    private UUID invisible;

    @BeforeEach
    void setUp() {
        fixture.tables();
        appels = new AppelsReunions(mockMvc, fixture);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PRA-" + jeton);
        projetNonHabilite = fixture.projet("PRN-" + jeton);
        type = fixture.type("RA-" + jeton);
        visible = fixture.reunion(projet, type, 1, "VIS-" + jeton, LE_10_MARS);
        invisible = fixture.reunion(projetNonHabilite, type, 1, "INV-" + jeton, LE_10_MARS);
    }

    private List<String> idsListe(String filtres, RequestPostProcessor qui) throws Exception {
        Reponse r = appels.appeler(get(REUNIONS + "?" + filtres), qui);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r.lire("$[*].id");
    }

    // ------------------------------------------------------------------ D4 / R8 visibilité

    @Test
    void d4_liste_projetNonHabilite_absent() throws Exception {
        RequestPostProcessor lecteur = fixture.compte(projet, "bul.v." + jeton, DROIT_LIRE);

        List<String> ids = idsListe("typeId=" + type + "&" + DU_AU, lecteur);

        assertThat(ids).contains(visible.toString()).doesNotContain(invisible.toString());
    }

    @Test
    void d4_liste_projetNonHabiliteDemandeExplicitement_absent() throws Exception {
        RequestPostProcessor lecteur = fixture.compte(projet, "bul.e." + jeton, DROIT_LIRE);

        List<String> ids = idsListe("projetIds=" + projet + "," + projetNonHabilite + "&typeId=" + type + "&" + DU_AU,
                lecteur);

        assertThat(ids).contains(visible.toString()).doesNotContain(invisible.toString());
    }

    @Test
    void d4_liste_sansType_resteRestreinteAuxProjetsHabilites() throws Exception {
        RequestPostProcessor lecteur = fixture.compte(projet, "bul.t." + jeton, DROIT_LIRE);

        List<String> ids = idsListe("projetIds=" + projet + "," + projetNonHabilite + "&" + DU_AU, lecteur);

        assertThat(ids).contains(visible.toString()).doesNotContain(invisible.toString());
    }

    @Test
    void d4_liste_ligneAvecChampsDuSpec() throws Exception {
        RequestPostProcessor lecteur = fixture.compte(projet, "bul.l." + jeton, DROIT_LIRE);

        Reponse r = appels.appeler(get(REUNIONS + "?typeId=" + type + "&" + DU_AU), lecteur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        String ligne = "$[?(@.id == '" + visible + "')]";
        assertThat(r.<List<String>>lire(ligne + ".reference")).containsExactly("VIS-" + jeton);
        assertThat(r.<List<Integer>>lire(ligne + ".ordre")).containsExactly(1);
    }

    @Test
    void d4_fiche_projetNonHabilite_404() throws Exception {
        RequestPostProcessor lecteur = fixture.compte(projet, "bul.f." + jeton, DROIT_LIRE);

        Reponse temoin = appels.appeler(get(REUNIONS + "/" + visible), lecteur);
        Reponse r = appels.appeler(get(REUNIONS + "/" + invisible), lecteur);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void d4_fiche_inconnue_404() throws Exception {
        RequestPostProcessor admin = fixture.admin(jeton, projet);

        Reponse r = appels.appeler(get(REUNIONS + "/" + UUID.randomUUID()), admin);
        Reponse temoin = appels.appeler(get(REUNIONS + "/" + visible), admin);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    // ------------------------------------------------------------------ D7 droits

    private void refuse(MockHttpServletRequestBuilder requete, String json, RequestPostProcessor qui) throws Exception {
        Reponse r = json == null ? appels.appeler(requete, qui) : appels.envoyer(requete, json, qui);
        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void d7_liste_sansDroitMeetingList_403() throws Exception {
        RequestPostProcessor sansDroit = fixture.compte(projet, "bul.d0." + jeton);

        refuse(get(REUNIONS + "?" + DU_AU), null, sansDroit);
    }

    @Test
    void d7_fiche_sansDroitMeetingList_403() throws Exception {
        RequestPostProcessor sansDroit = fixture.compte(projet, "bul.d1." + jeton);

        refuse(get(REUNIONS + "/" + visible), null, sansDroit);
    }

    @Test
    void d7_creation_sansToolNew_403() throws Exception {
        RequestPostProcessor lecteur = fixture.compte(projet, "bul.d2." + jeton, DROIT_LIRE);

        Reponse r = appels.creer(projet, type, null, LocalDate.of(2026, 3, 17), null, lecteur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeeting WHERE PrjProjectId = ?", projet)).isEqualTo(1);
    }

    @Test
    void d7_sousListe_sansToolNew_403() throws Exception {
        RequestPostProcessor lecteur = fixture.compte(projet, "bul.d3." + jeton, DROIT_LIRE);
        UUID contact = fixture.tiers("MO-" + jeton);

        refuse(post(REUNIONS + "/" + visible + "/maitres-ouvrage"), "{\"contactIds\":[\"" + contact + "\"]}", lecteur);
    }

    @Test
    void d7_suppression_sansToolDelete_403() throws Exception {
        RequestPostProcessor editeur = fixture.compte(projet, "bul.d4." + jeton, DROIT_LIRE, DROIT_MODIFIER);

        refuse(delete(REUNIONS + "/" + visible), null, editeur);

        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeeting WHERE PKey = ?", visible)).isEqualTo(1);
    }

    @Test
    void d7_temoin_suppressionAvecToolDelete_2xx() throws Exception {
        RequestPostProcessor complet = fixture.compte(projet, "bul.d5." + jeton, DROIT_LIRE, DROIT_MODIFIER,
                DROIT_SUPPRIMER);

        Reponse r = appels.appeler(delete(REUNIONS + "/" + visible), complet);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
    }

    @Test
    void d7_configTypes_sansDroit_403() throws Exception {
        RequestPostProcessor autre = fixture.compte(projet, "bul.c1." + jeton, DROIT_STATUTS, DROIT_PLAGES, DROIT_LIRE);

        refuse(delete(TYPES + "/" + fixture.type("RZ-" + jeton)), null, autre);
    }

    @Test
    void d7_configStatutsReserves_sansDroit_403() throws Exception {
        RequestPostProcessor autre = fixture.compte(projet, "bul.c2." + jeton, DROIT_TYPES, DROIT_PLAGES, DROIT_LIRE);

        refuse(delete(STATUTS + "/" + fixture.statut("SZ-" + jeton, false)), null, autre);
    }

    @Test
    void d7_configPlages_sansDroit_403() throws Exception {
        RequestPostProcessor autre = fixture.compte(projet, "bul.c3." + jeton, DROIT_TYPES, DROIT_STATUTS, DROIT_LIRE);

        refuse(delete(PLAGES + "/" + fixture.plage("PZ-" + jeton)), null, autre);
    }
}
