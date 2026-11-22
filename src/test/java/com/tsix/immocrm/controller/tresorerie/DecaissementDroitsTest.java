package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.PIECES_A_AFFECTER;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.REFERENTIELS;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.MODULE;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.PRJ;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.nombre;
import static com.tsix.immocrm.fixtures.FixtureDecaissements.DROIT_CHANGER_SOCIETE;
import static com.tsix.immocrm.fixtures.FixtureDecaissements.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureDecaissements.DROIT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureDecaissements.DROIT_SANS_AFFECTATION;
import static com.tsix.immocrm.fixtures.FixtureDecaissements.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md D13 : droits OMRAN de la saisie ({@code toolNew}, {@code toolDelete},
 * {@code CanSaveWithoutAffectation}, lecture), {@code referentiels.droits}, pièces à affecter, non connecté.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementDroitsTest {

    private static final String FOURNISSEUR_ID = "fournisseurId";
    private static final String SOCIETE_ID = "societeId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private UUID pa;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DDR");
        pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
    }

    private RequestPostProcessor qui(String code, String... droits) {
        return connecte(fixture.compte(s, code, droits), FixtureTresorerie.ROLE);
    }

    private Map<String, Object> corpsSurPa(String suffixe) {
        return corps(s, s.code(suffixe), "300", List.of(ligne(pa, "300")));
    }

    private UUID reglementSansLigne(String suffixe) {
        return tresorerie.reglement(Reglement.debit(s.code(suffixe), s.c(), "300"));
    }

    // ------------------------------------------------------------------ POST

    @Test
    void post_403_sansToolNew() throws Exception {
        Reponse r = appels.creer(qui("DDRA", DROIT_LIRE), corpsSurPa("-R1"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void post_403_sansDroitDeLecture() throws Exception {
        Reponse r = appels.creer(qui("DDRB", DROIT_NOUVEAU), corpsSurPa("-R2"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void post_201_temoin_lectureEtToolNew() throws Exception {
        Reponse r = appels.creer(qui("DDRC", DROIT_LIRE, DROIT_NOUVEAU), corpsSurPa("-R3"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    @Test
    void post_201_sansLigne_avecCanSaveWithoutAffectation() throws Exception {
        Reponse r = appels.creer(qui("DDRD", DROIT_LIRE, DROIT_NOUVEAU, DROIT_SANS_AFFECTATION),
                corps(s, s.code("-R4"), "300", List.of()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    @Test
    void post_401_nonConnecte() throws Exception {
        Reponse r = appels.creer(requete -> requete, corpsSurPa("-R5"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(401);
    }

    // ------------------------------------------------------------------ PUT / DELETE

    @Test
    void put_403_sansToolNew() throws Exception {
        UUID id = reglementSansLigne("-R6");

        Reponse r = appels.modifier(qui("DDRE", DROIT_LIRE, DROIT_SUPPRIMER), id, corpsSurPa("-R6"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void delete_403_sansToolDelete() throws Exception {
        UUID id = reglementSansLigne("-R7");

        Reponse r = appels.supprimer(qui("DDRF", DROIT_LIRE, DROIT_NOUVEAU), id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        assertThat(fixture.nombre("StlSettlement", "PKey", id)).as("règlement conservé").isEqualTo(1);
    }

    @Test
    void delete_204_temoin_avecToolDelete() throws Exception {
        UUID id = reglementSansLigne("-R8");

        Reponse r = appels.supprimer(qui("DDRG", DROIT_LIRE, DROIT_SUPPRIMER), id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
    }

    // ------------------------------------------------------------------ /referentiels

    @Test
    void referentiels_403_sansDroitDeLecture() throws Exception {
        Reponse r = appels.lire(qui("DDRH", DROIT_NOUVEAU), REFERENTIELS, MODULE, PRJ);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void referentiels_droits_tousFaux_enLectureSeule() throws Exception {
        Reponse r = appels.lire(qui("DDRI", DROIT_LIRE), REFERENTIELS, MODULE, PRJ);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertDroits(r, false);
    }

    @Test
    void referentiels_droits_tousVrais_avecTousLesDroits() throws Exception {
        Reponse r = appels.lire(qui("DDRJ", DROIT_LIRE, DROIT_NOUVEAU, DROIT_SUPPRIMER, DROIT_SANS_AFFECTATION,
                DROIT_CHANGER_SOCIETE), REFERENTIELS, MODULE, PRJ);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertDroits(r, true);
    }

    @Test
    void referentiels_moduleInvalide_400_modulePrj_200() throws Exception {
        RequestPostProcessor lecteur = qui("DDRK", DROIT_LIRE);

        Reponse invalide = appels.lire(lecteur, REFERENTIELS, MODULE, "XYZ");
        Reponse prj = appels.lire(lecteur, REFERENTIELS, MODULE, PRJ);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(invalide.statut()).as(invalide.corps()).isEqualTo(400);
            soft.assertThat(prj.statut()).as(prj.corps()).isEqualTo(200);
        });
    }

    private static void assertDroits(Reponse r, boolean attendu) {
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(r.valeur("$.droits.peutCreer")).as("peutCreer").isEqualTo(attendu);
            soft.assertThat(r.valeur("$.droits.peutSupprimer")).as("peutSupprimer").isEqualTo(attendu);
            soft.assertThat(r.valeur("$.droits.peutEnregistrerSansAffectation")).as("peutEnregistrerSansAffectation")
                    .isEqualTo(attendu);
            soft.assertThat(r.valeur("$.droits.peutChangerSociete")).as("peutChangerSociete").isEqualTo(attendu);
        });
    }

    // ------------------------------------------------------------------ /pieces-a-affecter

    @Test
    void piecesAAffecter_403_sansDroitDeLecture() throws Exception {
        Reponse r = appels.lire(qui("DDRL", DROIT_NOUVEAU), PIECES_A_AFFECTER, MODULE, PRJ,
                FOURNISSEUR_ID, s.c().fournisseur().toString(), SOCIETE_ID, s.c().societe().toString());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void piecesAAffecter_reglementId_resteSansLesLignesDeCeReglement() throws Exception {
        UUID modifie = reglementSansLigne("-R9");
        tresorerie.ligne(modifie, pa, "400");
        UUID autre = reglementSansLigne("-R10");
        tresorerie.ligne(autre, pa, "100");

        Reponse r = appels.lire(qui("DDRM", DROIT_LIRE), PIECES_A_AFFECTER, MODULE, PRJ,
                FOURNISSEUR_ID, s.c().fournisseur().toString(), SOCIETE_ID, s.c().societe().toString(),
                "reglementId", modifie.toString());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> trouvees = JsonPath.read(r.corps(), "$[?(@.id == '" + pa + "')]");
        assertThat(trouvees).as("pièce %s dans %s", pa, r.corps()).hasSize(1);
        assertThat(nombre(trouvees.get(0), "resteAPayer")).as("1000 − 100 (lignes de l'autre règlement seules)")
                .isEqualByComparingTo("900");
    }
}
