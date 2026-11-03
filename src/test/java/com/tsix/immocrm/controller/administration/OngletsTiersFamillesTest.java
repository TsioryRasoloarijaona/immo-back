package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureOngletsTiers;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.CONGES;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.DEPENSES;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.ONGLETS_COMMUNS;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.TOUS_ONGLETS;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.onglet;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.BANQUES;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.ORGANISMES;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglets par famille — FEATURE-admin-onglets-tiers.md, règle testable 3 et réponse R5 (banques et organismes sans
 * onglet). Les 404 sont verts avant le code (route absente) : chaque cas a son témoin 200 sur la même fiche.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OngletsTiersFamillesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers tiers;
    @Autowired private FixtureOngletsTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = tiers.jeu();
        fixture.preparer();
    }

    private void attendre(TypeTiers famille, UUID id, String nom, int statut) throws Exception {
        mockMvc.perform(onglet(famille.famille(), id, nom, connecte(j.admin()))).andExpect(status().is(statut));
    }

    private static Stream<Arguments> ongletsPrevus() {
        Stream<Arguments> communs = Stream.of(FOURNISSEURS, CONTACTS, PERSONNELS)
                .flatMap(type -> ONGLETS_COMMUNS.stream().map(o -> Arguments.of(type, o)));
        return Stream.concat(communs, Stream.of(Arguments.of(PERSONNELS, CONGES)));
    }

    private static Stream<Arguments> banquesEtOrganismes() {
        return Stream.of(BANQUES, ORGANISMES).flatMap(type -> TOUS_ONGLETS.stream().map(o -> Arguments.of(type, o)));
    }

    // ------------------------------------------------------------------ témoins : onglets prévus, état vide compris

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ongletsPrevus")
    void decision2_ongletPrevu_200_memeVide(TypeTiers type, String nom) throws Exception {
        attendre(type, j.tiers(type, "V").inserer(), nom, 200);
    }

    // ------------------------------------------------------------------ règle 3 : onglet hors famille

    @ParameterizedTest
    @ValueSource(strings = {"FOURNISSEURS", "CONTACTS"})
    void regle3_conges_horsPersonnels_404_temoinDepenses200(String famille) throws Exception {
        TypeTiers type = TypeTiers.valueOf(famille);
        UUID id = j.tiers(type, "C").inserer();
        attendre(type, id, DEPENSES, 200);

        attendre(type, id, CONGES, 404);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("banquesEtOrganismes")
    void reponseR5_banquesEtOrganismes_aucunOnglet_404_temoinFiche200(TypeTiers type, String nom) throws Exception {
        UUID id = j.tiers(type, "B").inserer();
        mockMvc.perform(lire(type.famille(), id, connecte(j.admin())))
                .andExpect(status().isOk());

        attendre(type, id, nom, 404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"inconnu", "contacts", "notes"})
    void regle3_ongletInconnu_404_temoin200(String nom) throws Exception {
        UUID id = j.tiers(FOURNISSEURS, "O").inserer();
        attendre(FOURNISSEURS, id, DEPENSES, 200);

        attendre(FOURNISSEURS, id, nom, 404);
    }

    // ------------------------------------------------------------------ règle 3 : tiers d'une autre famille, id inconnu

    @ParameterizedTest
    @ValueSource(strings = {"CONTACTS", "PERSONNELS", "BANQUES"})
    void regle3_tiersDUneAutreFamille_404_temoinSaFamille200(String autre) throws Exception {
        UUID fournisseur = j.tiers(FOURNISSEURS, "F").inserer();
        attendre(FOURNISSEURS, fournisseur, DEPENSES, 200);

        attendre(TypeTiers.valueOf(autre), fournisseur, DEPENSES, 404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOURNISSEURS", "CONTACTS", "PERSONNELS"})
    void regle3_idInconnu_404_temoin200(String famille) throws Exception {
        TypeTiers type = TypeTiers.valueOf(famille);
        attendre(type, j.tiers(type, "I").inserer(), DEPENSES, 200);

        attendre(type, UUID.randomUUID(), DEPENSES, 404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"prospects", "inconnue"})
    void regle3_familleInconnue_404_temoin200(String famille) throws Exception {
        UUID id = j.tiers(FOURNISSEURS, "U").inserer();
        attendre(FOURNISSEURS, id, DEPENSES, 200);

        mockMvc.perform(onglet(famille, id, DEPENSES, connecte(j.admin()))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOURNISSEURS", "CONTACTS", "PERSONNELS"})
    void regle3_tiersSansType_404_temoin200(String famille) throws Exception {
        TypeTiers type = TypeTiers.valueOf(famille);
        attendre(type, j.tiers(type, "W").inserer(), DEPENSES, 200);

        attendre(type, j.tiersSansType("N"), DEPENSES, 404);
    }
}
