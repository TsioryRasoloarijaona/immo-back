package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsTiers;
import com.tsix.immocrm.fixtures.FixtureReferentielsTiers.ListeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_FORMES_SOCIALES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_PAYS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_SEXES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_TITRES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_TYPES_RELATION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_VILLES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 5 de FEATURE-admin-referentiels-tiers.md : suppression d'une valeur utilisée → 409, rien modifié ; libre → 204.
 * Gardes du § « Suppression, lecture seule, code verrouillé » : CASCADE Pays → Villes et Villes → ComProspectionCity,
 * {@code AdmParamsGuid} (pays, formes sociales), {@code CfgTierContact} (types de relation, sans FK) ; une FK NO_ACTION
 * représentative (Titre → Sexe) posée par la fixture quand yesRemsTest ne l'a pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminTiersSuppressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsTiers fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
    }

    static Stream<ListeTiers> modifiables() {
        return FixtureReferentielsTiers.modifiables();
    }

    private ResultActions supprimer(ListeTiers l, UUID id) throws Exception {
        return mockMvc.perform(delete(LIGNE, l.slug(), id).with(connecte(admin)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modifiables")
    void r5_suppression_204_valeurLibre(ListeTiers l) throws Exception {
        UUID id = fixture.ligne(l, "DEL-" + jeton);

        supprimer(l, id).andExpect(status().isNoContent());

        assertThat(fixture.lignes(l, id)).isZero();
    }

    /** {@code AdmCity → AdmCountry} est en CASCADE : sans garde, les villes partiraient en silence. */
    @Test
    void r5_pays_409_porteDesVilles_paysEtVilleConserves() throws Exception {
        UUID pays = fixture.pays("PV-" + jeton);
        UUID ville = fixture.ville("V-" + jeton, pays);

        supprimer(L_PAYS, pays).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(fixture.lignes(L_PAYS, pays)).isEqualTo(1),
                () -> assertThat(fixture.lignes(L_VILLES, ville)).isEqualTo(1));
    }

    /** Décision 2 : pays par défaut ({@code AdmParamsGuid}, sans FK). */
    @Test
    void r5_pays_409_valeurDUnParametre_conserve() throws Exception {
        UUID pays = fixture.pays("PG-" + jeton);
        fixture.parametre("TestPays" + jeton, pays);

        supprimer(L_PAYS, pays).andExpect(status().isConflict());

        assertThat(fixture.lignes(L_PAYS, pays)).isEqualTo(1);
    }

    /** {@code ComProspectionCity → AdmCity} est en CASCADE. */
    @Test
    void r5_ville_409_villeDUneProspection_villeEtLienConserves() throws Exception {
        UUID ville = fixture.ville("VP-" + jeton, fixture.pays("P-" + jeton));
        UUID lien = fixture.villeDeProspection(ville);

        supprimer(L_VILLES, ville).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(fixture.lignes(L_VILLES, ville)).isEqualTo(1),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM ComProspectionCity WHERE PKey = :id", lien))
                        .isEqualTo(1));
    }

    /** Décision 2 : forme sociale par défaut ({@code DefaultSocialFormFor*}, sans FK). */
    @Test
    void r5_formeSociale_409_valeurDUnParametre_conservee() throws Exception {
        UUID forme = fixture.ligne(L_FORMES_SOCIALES, "FG-" + jeton);
        fixture.parametre("TestFormeSociale" + jeton, forme);

        supprimer(L_FORMES_SOCIALES, forme).andExpect(status().isConflict());

        assertThat(fixture.lignes(L_FORMES_SOCIALES, forme)).isEqualTo(1);
    }

    /** {@code CfgTierContact.CfgRelationTypeId} n'a aucune FK, ni en prod ni en CI. */
    @Test
    void r5_typeDeRelation_409_porteParUnContact_conserve() throws Exception {
        UUID relation = fixture.ligne(L_TYPES_RELATION, "TR-" + jeton);
        UUID contact = fixture.contactDeRelation(relation);

        supprimer(L_TYPES_RELATION, relation).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(fixture.lignes(L_TYPES_RELATION, relation)).isEqualTo(1),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTierContact WHERE PKey = :id "
                        + "AND CfgRelationTypeId IS NOT NULL", contact)).isEqualTo(1));
    }

    /** FK NO_ACTION : refus par {@code flushOuConflit}. */
    @Test
    void r5_sexe_409_porteParUnTitre_sexeEtTitreConserves() throws Exception {
        fixture.cleTitreVersSexe();
        UUID sexe = fixture.sexe("SX-" + jeton);
        UUID titre = fixture.titre("TI-" + jeton, sexe);

        supprimer(L_SEXES, sexe).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(fixture.lignes(L_SEXES, sexe)).isEqualTo(1),
                () -> assertThat(fixture.guid(L_TITRES, titre, "AdmSexTypeId")).isEqualTo(sexe));
    }
}
