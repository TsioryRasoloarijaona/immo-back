package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.NATURES;
import static com.tsix.immocrm.fixtures.AppelsTiers.corpsPhysique;
import static com.tsix.immocrm.fixtures.AppelsTiers.ids;
import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.AppelsTiers.lister;
import static com.tsix.immocrm.fixtures.AppelsTiers.modifier;
import static com.tsix.immocrm.fixtures.AppelsTiers.supprimer;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePersonnels.DATE_NAISSANCE;
import static com.tsix.immocrm.fixtures.FixturePersonnels.IBAN;
import static com.tsix.immocrm.fixtures.FixturePersonnels.PIECE;
import static com.tsix.immocrm.fixtures.FixturePersonnels.RIB;
import static com.tsix.immocrm.fixtures.FixturePersonnels.employe;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Liste et fiche — FEATURE-admin-personnels.md, règles 1 et 3, section Back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonnelsListeTest {

    private static final String EMP = PERSONNELS.famille();
    private static final String NATURE_ID = "natureId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;
    @Autowired private DataSource dataSource;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
    }

    private String reponse(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private String duJeton(String famille) throws Exception {
        return reponse(lister(famille, connecte(j.admin())).param("recherche", j.jeton()));
    }

    // ------------------------------------------------------------------ règle 1

    @Test
    void r1_liste_neRendQueLesEmployes() throws Exception {
        UUID personne = employe(j, "E1").inserer();
        j.tiers(FOURNISSEURS, "F1").inserer();
        j.tiers(CONTACTS, "C1").inserer();

        assertThat(ids(duJeton(EMP))).containsExactly(personne.toString());
    }

    @Test
    void r1_ligneSansType_deNatureEmploye_absente() throws Exception {
        UUID personne = employe(j, "E2").inserer();
        UUID sansType = j.tiersSansType("N1");
        new JdbcTemplate(dataSource).update("UPDATE CfgTier SET CfgTierKindId = ? WHERE PKey = ?",
                j.nature(PERSONNELS).toString(), sansType.toString());

        assertThat(ids(duJeton(EMP))).containsExactly(personne.toString());
    }

    @Test
    void r1_natureId_ignore() throws Exception {
        UUID avecNature = employe(j, "E3").avec("CfgTierKindId", j.nature(PERSONNELS)).inserer();
        UUID sansNature = employe(j, "E4").inserer();
        String autreNature = j.nature(PERSONNELS).toString();

        String page = reponse(lister(EMP, connecte(j.admin())).param("recherche", j.jeton()).param(NATURE_ID, autreNature));

        assertThat(ids(page)).containsExactlyInAnyOrder(avecNature.toString(), sansNature.toString());
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS", "BANQUES", "ORGANISMES"})
    void r1_idDUnAutreType_404_surLecture_modification_suppression(TypeTiers type) throws Exception {
        // Témoin : la route existe pour un employé.
        reponse(lire(EMP, employe(j, "E5").inserer(), connecte(j.admin())));
        UUID autre = j.tiers(type, "A1").inserer();
        Map<String, Object> avant = fixture.ligne(autre);

        mockMvc.perform(lire(EMP, autre, connecte(j.admin()))).andExpect(status().isNotFound());
        mockMvc.perform(modifier(EMP, autre, corpsPhysique(j, "Amal", "Benali"), connecte(j.admin())))
                .andExpect(status().isNotFound());
        mockMvc.perform(supprimer(EMP, autre, connecte(j.admin()))).andExpect(status().isNotFound());

        assertThat(fixture.ligne(autre)).isEqualTo(avant);
    }

    @Test
    void r1_employeLuParUneAutreFamille_404() throws Exception {
        UUID personne = employe(j, "E6").inserer();
        reponse(lire(EMP, personne, connecte(j.admin())));

        mockMvc.perform(lire(CONTACTS.famille(), personne, connecte(j.admin()))).andExpect(status().isNotFound());
        mockMvc.perform(lire(FOURNISSEURS.famille(), personne, connecte(j.admin()))).andExpect(status().isNotFound());
    }

    @Test
    void natures_seulementCellesDuTypeEmploye() throws Exception {
        UUID nature = j.nature(PERSONNELS);
        UUID autre = j.nature(CONTACTS);

        String corps = reponse(get(NATURES, EMP).with(connecte(j.admin())));

        List<String> natures = JsonPath.read(corps, "$[*].id");
        assertThat(natures).contains(nature.toString()).doesNotContain(autre.toString());
    }

    // ------------------------------------------------------------------ règle 3 et section Back

    @Test
    void r3_ligne_ribEtIbanNull_memeSiLaBaseLesPorte() throws Exception {
        employe(j, "E7").inserer();

        String page = duJeton(EMP);

        Map<String, Object> ligne = JsonPath.read(page, "$.content[0]");
        assertAll(
                () -> assertThat(ligne.get("rib")).isNull(),
                () -> assertThat(ligne.get("iban")).isNull(),
                () -> assertThat(page).doesNotContain(RIB, IBAN));
    }

    @Test
    void r3_temoin_fournisseur_gardeSonRibEtSonIban() throws Exception {
        j.tiers(FOURNISSEURS, "F2").avec("Rib", RIB).avec("Iban", IBAN).inserer();

        Map<String, Object> ligne = JsonPath.read(duJeton(FOURNISSEURS.famille()), "$.content[0]");

        assertThat(ligne).containsEntry("rib", RIB).containsEntry("iban", IBAN);
    }

    @Test
    void back_ligne_niPieceNiNaissance() throws Exception {
        employe(j, "E9").inserer();

        String page = duJeton(EMP);

        Map<String, Object> ligne = JsonPath.read(page, "$.content[0]");
        assertAll(
                () -> assertThat(ligne.get("numeroPiece")).isNull(),
                () -> assertThat(ligne.get("dateNaissance")).isNull(),
                () -> assertThat(page).doesNotContain(PIECE, DATE_NAISSANCE));
    }

    @Test
    void back_fiche_portePieceRibIbanNaissance() throws Exception {
        UUID personne = employe(j, "E10").inserer();

        String fiche = reponse(lire(EMP, personne, connecte(j.admin())));

        Map<String, Object> champs = JsonPath.read(fiche, "$");
        assertAll(
                () -> assertThat(champs).containsEntry("numeroPiece", PIECE).containsEntry("rib", RIB)
                        .containsEntry("iban", IBAN).containsEntry("personneMorale", false),
                () -> assertThat(String.valueOf(champs.get("dateNaissance"))).startsWith(DATE_NAISSANCE));
    }
}
