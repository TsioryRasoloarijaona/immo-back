package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
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

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.detail;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-b2a-retenue.md §6 RS-m, RS-n, RS-o et réponses Q3/Q4/T2/T3 : {@code POST /decaissements/{id}/code-feuille-paiement}
 * et {@code …/code-feuille-rs}, sans corps, rendent 200 {@code {code}} ; code attribué une seule fois, jamais par le corps.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementCodesFeuilleTest {

    private static final String PP = "code-feuille-paiement";
    private static final String CRAS = "code-feuille-rs";
    private static final String CODE = "$.code";
    private static final String CODE_PP = "codePp";
    private static final String CODE_CRAS = "codeCras";
    private static final String PP_EXISTANT = "PP-EXIST";
    private static final String CRAS_EXISTANT = "CRAS-EXIST";
    private static final Configuration TOLERANTE =
            Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;
    @Autowired private FixtureAchat achat;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private AdmUser compte;
    private RequestPostProcessor qui;
    private UUID reglement;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DCF");
        fixture.tablesRetenue();
        fixture.sequencePp(5);
        fixture.sequenceCras(s.c().societe(), 3);
        compte = fixture.compte(s, "DCFA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU);
        qui = connecte(compte, FixtureTresorerie.ROLE);
        reglement = tresorerie.reglement(Reglement.debit(s.code("-R"), s.c(), "500"));
    }

    private static String code(Reponse r) {
        return JsonPath.using(TOLERANTE).parse(r.corps()).read(CODE);
    }

    private Reponse attribuerOk(UUID id, String route) throws Exception {
        Reponse r = appels.attribuer(qui, id, route);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    // RS-m / T3 : SeqValue 5 → 21/00006, séquence à 6.
    @Test
    void rsM_premierAppel_attribueLeCodeSuivant() throws Exception {
        Reponse r = attribuerOk(reglement, PP);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(code(r)).isEqualTo("21/00006");
        sa.assertThat(fixture.valeurSequencePp()).as("SeqValue").isEqualTo(6);
        sa.assertThat(fixture.codesEnBase(reglement)).containsEntry(CODE_PP, "21/00006");
        sa.assertAll();
    }

    // RS-m : le second appel rend le même code sans incrémenter.
    @Test
    void rsM_secondAppel_memeCodeSansIncrement() throws Exception {
        attribuerOk(reglement, PP);

        Reponse r = attribuerOk(reglement, PP);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(code(r)).isEqualTo("21/00006");
        sa.assertThat(fixture.valeurSequencePp()).as("SeqValue").isEqualTo(6);
        sa.assertAll();
    }

    // RS-m / Q4 : un code existant hors séquence n'est jamais réécrit.
    @Test
    void rsM_codeExistant_renduInchange_sequenceIntacte() throws Exception {
        fixture.poserCodes(reglement, "26/00001", null);

        Reponse r = attribuerOk(reglement, PP);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(code(r)).isEqualTo("26/00001");
        sa.assertThat(fixture.valeurSequencePp()).as("SeqValue").isEqualTo(5);
        sa.assertAll();
    }

    // RS-n : une séquence par société (A à 3 → 21004, B à 7 → 21008), second appel sans incrément.
    @Test
    void rsN_parSociete_deuxCompteurs() throws Exception {
        fixture.sequenceCras(s.autreSociete(), 7);
        achat.habiliterSociete(compte, s.autreSociete());
        UUID reglementB = tresorerie.reglement(Reglement.debit(s.code("-RB"), s.c(), "500").societe(s.autreSociete()));

        Reponse a = attribuerOk(reglement, CRAS);
        Reponse b = attribuerOk(reglementB, CRAS);
        Reponse a2 = attribuerOk(reglement, CRAS);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(code(a)).as("société A").isEqualTo("21004");
        sa.assertThat(code(b)).as("société B").isEqualTo("21008");
        sa.assertThat(code(a2)).as("A, second appel").isEqualTo("21004");
        sa.assertThat(fixture.valeurSequenceCras(s.c().societe())).as("SeqValue A").isEqualTo(4);
        sa.assertThat(fixture.valeurSequenceCras(s.autreSociete())).as("SeqValue B").isEqualTo(8);
        sa.assertThat(fixture.codesEnBase(reglement)).containsEntry(CODE_CRAS, "21004");
        sa.assertAll();
    }

    // RS-m / Q4 : sans ligne P.P → 409.
    @Test
    void rsM_sansSequence_409() throws Exception {
        fixture.sansSequencePp();

        Reponse r = appels.attribuer(qui, reglement, PP);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("Aucune séquence de code n'est configurée.");
    }

    // RS-n / Q4 : sans ligne C.R.A.S pour la société → 409.
    @Test
    void rsN_sansSequenceDeLaSociete_409() throws Exception {
        fixture.sansSequenceCras(s.c().societe());

        Reponse r = appels.attribuer(qui, reglement, CRAS);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("Aucune séquence de code n'est configurée pour cette société.");
    }

    // §4 / Q4 : règlement inconnu → 404 (témoin 200 sur le règlement du cadre), sur les deux routes.
    @Test
    void reglementInconnu_404() throws Exception {
        UUID inconnu = UUID.randomUUID();

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(appels.attribuer(qui, inconnu, PP).statut()).as("P.P inconnu").isEqualTo(404);
        sa.assertThat(appels.attribuer(qui, inconnu, CRAS).statut()).as("C.R.A.S inconnu").isEqualTo(404);
        sa.assertThat(appels.attribuer(qui, reglement, PP).statut()).as("témoin").isEqualTo(200);
        sa.assertAll();
    }

    // §4 / Q4 : sans toolNew → 403 (témoin 200 avec le droit), sur les deux routes.
    @Test
    void sansDroitNouveau_403() throws Exception {
        RequestPostProcessor lecteur = connecte(fixture.compte(s, "DCFL", FixtureDecaissements.DROIT_LIRE),
                FixtureTresorerie.ROLE);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(appels.attribuer(lecteur, reglement, PP).statut()).as("P.P lecteur").isEqualTo(403);
        sa.assertThat(appels.attribuer(lecteur, reglement, CRAS).statut()).as("C.R.A.S lecteur").isEqualTo(403);
        sa.assertThat(appels.attribuer(qui, reglement, CRAS).statut()).as("témoin").isEqualTo(200);
        sa.assertAll();
    }

    private Map<String, Object> corpsAvecCodes(String suffixe, UUID piece) {
        Map<String, Object> c = corps(s, s.code(suffixe), "1000", List.of(ligne(piece, "1000")));
        c.put("codeFeuillePaiement", "PIRATE");
        c.put("codeFeuilleRs", "PIRATE");
        return c;
    }

    // RS-o / Q3 : un PUT qui envoie des codes ne change pas les codes existants.
    @Test
    void rsO_put_codesDuCorpsIgnores() throws Exception {
        UUID piece = fixture.piece(s, "-FA", s.c().projet(), "2000");
        UUID id = appels.creerOk(qui, corps(s, s.code("-O"), "1000", List.of(ligne(piece, "1000"))));
        fixture.poserCodes(id, PP_EXISTANT, CRAS_EXISTANT);

        Reponse r = appels.modifier(qui, id, corpsAvecCodes("-O", piece));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.codesEnBase(id)).containsEntry(CODE_PP, PP_EXISTANT).containsEntry(CODE_CRAS, CRAS_EXISTANT);
    }

    // RS-o : un POST qui envoie des codes les laisse NULL en base.
    @Test
    void rsO_post_codesDuCorpsIgnores() throws Exception {
        UUID piece = fixture.piece(s, "-FB", s.c().projet(), "2000");

        UUID id = appels.creerOk(qui, corpsAvecCodes("-OP", piece));

        assertThat(fixture.codesEnBase(id)).containsEntry(CODE_PP, null).containsEntry(CODE_CRAS, null);
    }
}
