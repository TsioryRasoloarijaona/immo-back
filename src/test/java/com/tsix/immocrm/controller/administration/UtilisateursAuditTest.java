package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.controller.administration.ArbreHabilitations.Noeud;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.acceptee;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** R17, sans transaction de test : la trace n'existe qu'après validation. */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursAuditTest {

    private static final String AJOUT = "Added";
    private static final String MODIFICATION = "Modified";
    private static final String SUPPRESSION = "Deleted";
    private static final String TROIS = "(?<!\\d)3(?!\\d)";
    private static final String DEUX = "(?<!\\d)2(?!\\d)";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;

    private String jeton;
    private AppelsUtilisateurs appels;
    private UUID cible;
    private String codeCible;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
        AdmUser compte = fixture.compte();
        cible = compte.getId();
        codeCible = compte.getCode();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private String uneSeuleTrace() {
        return assertThat(fixture.traces(cible, MODIFICATION)).hasSize(1).actual().getFirst();
    }

    /** Sans le jeton ni le code du compte, dont les chiffres feraient passer n'importe quel résumé. */
    private String resume(String trace) {
        return trace.replace(codeCible, "").replace(jeton, "").replace(cible.toString(), "");
    }

    @Test
    void r17_creation_uneLigneAdded() throws Exception {
        String code = "Audit." + jeton;

        appels.creer(corps().avec(CODE, code).avec(DESCRIPTION, "Trace")).andExpect(status().isCreated());

        assertThat(fixture.traces(fixture.idDe(code), AJOUT)).hasSize(1);
    }

    @Test
    void r17_modification_uneLigneModified() throws Exception {
        acceptee(appels.modifier(cible, corps().avec(DESCRIPTION, "Trace")));

        assertThat(fixture.traces(cible, MODIFICATION)).hasSize(1);
    }

    /** Trois ajoutés, deux retirés : des nombres distincts, pour qu'un inversé ou un total ne passe pas pour le bon résumé. */
    @Test
    void r17_roles_uneSeuleLigne_nombresEtCodes() throws Exception {
        UUID[] ajoutes = {fixture.role("AJA-" + jeton), fixture.role("AJB-" + jeton), fixture.role("AJC-" + jeton)};
        fixture.lierRole(cible, fixture.role("REA-" + jeton));
        fixture.lierRole(cible, fixture.role("REB-" + jeton));

        acceptee(appels.roles(cible, ajoutes));

        String trace = uneSeuleTrace();
        assertThat(trace).contains("AJA-" + jeton, "AJB-" + jeton, "AJC-" + jeton, "REA-" + jeton, "REB-" + jeton);
        assertThat(resume(trace)).containsPattern(TROIS).containsPattern(DEUX);
    }

    @Test
    void r17_roles_aucuneTraceParLigneEnfant() throws Exception {
        UUID role = fixture.role("AJ-" + jeton);

        acceptee(appels.roles(cible, role));

        assertThat(fixture.lignesRole(cible, role)).singleElement()
                .satisfies(ligne -> assertThat(fixture.tracesObjet(ligne)).isZero());
    }

    @Test
    void r17_comptesDepot_uneSeuleLigne_nombres() throws Exception {
        UUID[] ajoutes = {fixture.depot("AJA-" + jeton), fixture.depot("AJB-" + jeton), fixture.depot("AJC-" + jeton)};
        fixture.lierDepot(cible, fixture.depot("REA-" + jeton));
        fixture.lierDepot(cible, fixture.depot("REB-" + jeton));

        acceptee(appels.comptesDepot(cible, ajoutes));

        assertThat(resume(uneSeuleTrace())).containsPattern(TROIS).containsPattern(DEUX);
    }

    @Test
    void r17_comptesDepot_aucuneTraceParLigneEnfant() throws Exception {
        UUID depot = fixture.depot("AJ-" + jeton);

        acceptee(appels.comptesDepot(cible, depot));

        assertThat(fixture.lignesDepot(cible, depot)).singleElement()
                .satisfies(ligne -> assertThat(fixture.tracesObjet(ligne)).isZero());
    }

    /** Réponse 1 du manager : une trace par ligne ajoutée ou retirée, aucune ligne résumé sur le compte. */
    @Test
    void r17_habilitations_uneTraceParLigne_sansResume() throws Exception {
        UUID societe = fixture.societe("SOC-" + jeton, true);
        UUID[] ajoutes = {fixture.projet("AJA-" + jeton, societe), fixture.projet("AJB-" + jeton, societe),
                fixture.projet("AJC-" + jeton, societe)};
        fixture.habiliter(cible, null, fixture.projet("REA-" + jeton, societe), null, null);
        fixture.habiliter(cible, null, fixture.projet("REB-" + jeton, societe), null, null);
        ArbreHabilitations arbre = appels.arbre(cible);

        acceptee(appels.habilitations(cible, arbre.noeud(ajoutes[0]), arbre.noeud(ajoutes[1]), arbre.noeud(ajoutes[2])));

        assertAll(
                () -> assertThat(fixture.tracesHabilitation(cible, AJOUT)).isEqualTo(3),
                () -> assertThat(fixture.tracesHabilitation(cible, SUPPRESSION)).isEqualTo(2),
                () -> assertThat(fixture.tracesObjet(cible)).isZero());
    }

    @Test
    void r17_putRolesSansEffet_rien() throws Exception {
        UUID role = fixture.role("GAR-" + jeton);
        fixture.lierRole(cible, role);

        acceptee(appels.roles(cible, role));

        assertThat(fixture.tracesObjet(cible)).isZero();
    }

    @Test
    void r17_putComptesSansEffet_rien() throws Exception {
        UUID depot = fixture.depot("GAR-" + jeton);
        fixture.lierDepot(cible, depot);

        acceptee(appels.comptesDepot(cible, depot));

        assertThat(fixture.tracesObjet(cible)).isZero();
    }

    @Test
    void r17_putHabilitationsSansEffet_rien() throws Exception {
        UUID societe = fixture.societe("SOC-" + jeton, true);
        UUID projet = fixture.projet("PRJ-" + jeton, societe);
        fixture.habiliter(cible, null, projet, null, null);
        Noeud lu = appels.arbre(cible).noeud(projet);

        acceptee(appels.habilitations(cible, lu));

        assertAll(
                () -> assertThat(fixture.tracesObjet(cible)).isZero(),
                () -> assertThat(fixture.tracesHabilitation(cible, AJOUT)).isZero(),
                () -> assertThat(fixture.tracesHabilitation(cible, SUPPRESSION)).isZero());
    }

    /** Témoin : voir {@link #r17_roles_uneSeuleLigne_nombresEtCodes}, la même écriture acceptée est tracée. */
    @Test
    void r17_refus_pasDeTrace() throws Exception {
        appels.roles(cible, UUID.fromString("5B6C7D8E-9FA0-4B1C-82D3-E4F506172839")).andExpect(status().isBadRequest());

        assertThat(fixture.tracesObjet(cible)).isZero();
    }
}
