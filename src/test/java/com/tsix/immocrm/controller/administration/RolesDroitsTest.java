package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureRoles;
import com.tsix.immocrm.fixtures.FixtureRoles.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsRoles.codes;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODE;
import static com.tsix.immocrm.fixtures.FixtureRoles.DESCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PUT des droits — FEATURE-admin-roles.md, règles 8 à 11. */
@SpringBootTest
@AutoConfigureMockMvc
class RolesDroitsTest {

    private static final UUID INCONNU = UUID.fromString("2F3A4B5C-6D7E-4F80-91A2-B3C4D5E6F708");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AppelsRoles appels;
    private UUID role;
    private String gardeA;
    private String retireB;
    private String nouveauC;
    private String horsArbre;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsRoles(mockMvc, connecte(fixture.compte()));
        role = fixture.role("DRO-" + jeton);
        gardeA = "A-" + jeton;
        retireB = "B-" + jeton;
        nouveauC = "C-" + jeton;
        horsArbre = "HORS-" + jeton;
        fixture.noeud(gardeA);
        fixture.noeud(retireB);
        fixture.noeud(nouveauC);
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    // ------------------------------------------------------------------ règle 8

    @Test
    void r8_differenceAppliquee() throws Exception {
        fixture.droit(role, gardeA);
        fixture.droit(role, retireB);

        appels.droits(role, List.of(gardeA, nouveauC)).andExpect(status().isOk());

        assertThat(fixture.codesDroits(role)).containsExactly(gardeA, nouveauC);
    }

    @Test
    void r8_ligneConservee_gardeSaPKey() throws Exception {
        UUID ligne = fixture.droit(role, gardeA);
        fixture.droit(role, retireB);

        appels.droits(role, List.of(gardeA, nouveauC)).andExpect(status().isOk());

        assertThat(fixture.lignesDroit(role, gardeA)).containsExactly(ligne);
    }

    @Test
    void r8_descriptionInseree_vientDeLArbre() throws Exception {
        appels.droits(role, List.of(nouveauC)).andExpect(status().isOk());

        assertThat(fixture.descriptionDroit(role, nouveauC)).isEqualTo("Libelle " + nouveauC);
    }

    @Test
    void r8_memeCorpsRejoue_aucuneLigneRefaite() throws Exception {
        List<String> coches = List.of(gardeA, nouveauC);
        appels.droits(role, coches).andExpect(status().isOk());
        Etat apres = fixture.etat(role);

        appels.droits(role, coches).andExpect(status().isOk());

        assertThat(fixture.etat(role)).isEqualTo(apres);
    }

    @Test
    void r8_autreRoleMemeCode_intact() throws Exception {
        UUID autre = fixture.role("AUT-" + jeton);
        fixture.droit(autre, retireB);
        fixture.droit(role, retireB);
        Etat avant = fixture.etat(autre);

        appels.droits(role, List.of()).andExpect(status().isOk());

        assertThat(fixture.etat(autre)).isEqualTo(avant);
    }

    @Test
    void r8_ficheRelue_montreLesCodesCoches() throws Exception {
        appels.droits(role, List.of(gardeA, nouveauC)).andExpect(status().isOk());

        appels.fiche(role).andExpect(status().isOk())
                .andExpect(jsonPath("$.codesDroits", containsInAnyOrder(gardeA, nouveauC)));
    }

    @Test
    void r8_roleInconnu_404() throws Exception {
        appels.droits(role, List.of(gardeA)).andExpect(status().isOk());

        appels.droits(INCONNU, List.of(gardeA)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ règle 9

    @Test
    void r9_horsArbre_preserve_surPutPartiel() throws Exception {
        UUID ligne = fixture.droit(role, horsArbre);
        fixture.droit(role, retireB);

        appels.droits(role, List.of(gardeA)).andExpect(status().isOk());

        assertThat(fixture.lignesDroit(role, horsArbre)).containsExactly(ligne);
    }

    @Test
    void r9_horsArbre_preserve_surListeVide() throws Exception {
        UUID ligne = fixture.droit(role, horsArbre);
        fixture.droit(role, gardeA);

        appels.droits(role, List.of()).andExpect(status().isOk());

        assertThat(fixture.lignesDroit(role, horsArbre)).containsExactly(ligne);
    }

    @Test
    void r9_horsArbre_nombreStable_etCompteParLaFiche() throws Exception {
        fixture.droit(role, horsArbre);
        fixture.droit(role, horsArbre + "-2");

        appels.droits(role, List.of(gardeA)).andExpect(status().isOk());
        appels.droits(role, List.of(gardeA)).andExpect(status().isOk());

        appels.fiche(role).andExpect(status().isOk())
                .andExpect(jsonPath("$.droitsHorsArbre").value(2))
                .andExpect(jsonPath("$.codesDroits", containsInAnyOrder(gardeA)));
    }

    // ------------------------------------------------------------------ règle 10

    private void refuse400SansChanger(com.tsix.immocrm.fixtures.CorpsJson corps) throws Exception {
        fixture.droit(role, retireB);
        fixture.droit(role, horsArbre);
        Etat avant = fixture.etat(role);

        appels.droits(role, corps).andExpect(status().isBadRequest());

        assertThat(fixture.etat(role)).isEqualTo(avant);
    }

    @Test
    void r10_codeAbsentDeLArbre_400() throws Exception {
        refuse400SansChanger(codes("INCONNU-" + jeton));
    }

    @Test
    void r10_codeValideEtCodeInconnu_400_riendEcrit() throws Exception {
        refuse400SansChanger(codes(nouveauC, "INCONNU-" + jeton));
    }

    /** Un code hors arbre que le rôle porte déjà reste absent de l'arbre : il ne s'envoie pas. */
    @Test
    void r10_codeHorsArbreDuRole_400() throws Exception {
        refuse400SansChanger(codes(gardeA, horsArbre));
    }

    @Test
    void r10_codeVide_400() throws Exception {
        refuse400SansChanger(codes(gardeA, ""));
    }

    @Test
    void r10_codeNull_400() throws Exception {
        refuse400SansChanger(codes(gardeA, null));
    }

    @Test
    void r10_doublons_traitesUneFois() throws Exception {
        appels.droits(role, List.of(nouveauC, nouveauC, gardeA, nouveauC)).andExpect(status().isOk());

        assertThat(fixture.codesDroits(role)).containsExactly(gardeA, nouveauC);
    }

    @Test
    void r10_listeVide_retireTousLesDroitsDeLArbre() throws Exception {
        fixture.droit(role, gardeA);
        fixture.droit(role, retireB);
        fixture.droit(role, horsArbre);

        appels.droits(role, List.of()).andExpect(status().isOk());

        assertThat(fixture.codesDroits(role)).containsExactly(horsArbre);
    }

    // ------------------------------------------------------------------ règle 11

    @Test
    void r11_put_arbreIntact() throws Exception {
        fixture.droit(role, retireB);
        fixture.droit(role, horsArbre);
        Map<String, Object> avant = fixture.empreinteArbre();

        appels.droits(role, List.of(gardeA, nouveauC)).andExpect(status().isOk());

        assertThat(fixture.empreinteArbre()).isEqualTo(avant);
    }

    @Test
    void r11_cycleComplet_arbreIntact() throws Exception {
        String code = "CYC-" + jeton;
        Map<String, Object> avant = fixture.empreinteArbre();

        appels.creer(corps().avec(CODE, code)).andExpect(status().isCreated());
        UUID cree = fixture.idDe(code);
        appels.modifier(cree, corps().avec(DESCRIPTION, "Cycle")).andExpect(status().isOk());
        appels.droits(cree, List.of(gardeA)).andExpect(status().isOk());
        appels.supprimer(cree).andExpect(status().isNoContent());

        assertThat(fixture.empreinteArbre()).isEqualTo(avant);
    }

    @Test
    void r11_refus_arbreIntact() throws Exception {
        Map<String, Object> avant = fixture.empreinteArbre();

        appels.droits(role, codes("INCONNU-" + jeton)).andExpect(status().isBadRequest());

        assertThat(fixture.empreinteArbre()).isEqualTo(avant);
    }
}
