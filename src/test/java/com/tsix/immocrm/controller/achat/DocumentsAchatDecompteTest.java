package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet.Jeu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.champ;
import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lien facture → décompte sur la liste et la fiche d'A1 (FEATURE-achat-depenses-projet.md §3.5, §4.1 ; §6 test 6).
 * Jeu : {@link FixtureDepensesProjet} (FA1 ← D1 n° 1 « …-DZ1 », FA2 ← D2 n° 2 « …-DA2 », FH1 sans décompte).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatDecompteTest {

    private static final String DECOMPTE_ID = "decompteId";
    private static final String DECOMPTE_CODE = "decompteCode";
    private static final String DECOMPTE_NUMERO = "decompteNumero";
    private static final String PROJET_IDS = "projetIds";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDepensesProjet fixture;

    private Jeu jeu;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        jeu = fixture.jeu();
        a = connecte(jeu.compteA(), FixtureDepensesProjet.ROLE);
    }

    private String listeA(String... params) throws Exception {
        String[] tous = new String[params.length + 2];
        tous[0] = PROJET_IDS;
        tous[1] = jeu.projetA().toString();
        System.arraycopy(params, 0, tous, 2, params.length);
        return corps(mockMvc, liste(a, tous));
    }

    @Test
    void t6_liste_factureDuDecompte_porteDecompteIdCodeNumero() throws Exception {
        String json = listeA();
        String fa1 = jeu.code("-FA1");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(champ(json, fa1, DECOMPTE_ID)).isEqualTo(jeu.decompte1().toString());
            s.assertThat(champ(json, fa1, DECOMPTE_CODE)).isEqualTo(jeu.decompte1Code());
            s.assertThat(champ(json, fa1, DECOMPTE_NUMERO)).isEqualTo(1);
            s.assertThat(champ(json, jeu.code("-FA2"), DECOMPTE_NUMERO)).isEqualTo(2);
        });
    }

    @Test
    void t6_liste_factureSansDecompte_champsNuls() throws Exception {
        String json = listeA();
        String fh1 = jeu.code("-FH1");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(champ(json, fh1, DECOMPTE_ID)).isNull();
            s.assertThat(champ(json, fh1, DECOMPTE_CODE)).isNull();
            s.assertThat(champ(json, fh1, DECOMPTE_NUMERO)).isNull();
        });
    }

    @Test
    void t6_fiche_factureDuDecompte_porteDecompteIdCodeNumero() throws Exception {
        String json = corps(mockMvc, get(AppelsAchat.DOCUMENT, jeu.factureA1()).with(a));
        Map<String, Object> fiche = JsonPath.read(json, "$");

        SoftAssertions.assertSoftly(s -> s.assertThat(fiche)
                .containsEntry(DECOMPTE_ID, jeu.decompte1().toString())
                .containsEntry(DECOMPTE_CODE, jeu.decompte1Code())
                .containsEntry(DECOMPTE_NUMERO, 1));
    }

    @Test
    void t6_prjWorkingOutIdSansReciproque_decompteIdNul() throws Exception {
        UUID decompteAvance = fixture.decompte(jeu.lotA1(), jeu.code("-DAV"), 3, "100");
        String code = jeu.code("-AV");
        UUID avance = fixture.facture(code, LocalDate.of(2026, 3, 15), jeu.fournisseur(), jeu.fournisseurNom(),
                jeu.societe(), jeu.projetA(), null, "100");
        fixture.pointerDecompte(avance, decompteAvance);

        String json = listeA();
        Map<String, Object> fiche = JsonPath.read(corps(mockMvc, get(AppelsAchat.DOCUMENT, avance).with(a)), "$");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(champ(json, code, DECOMPTE_ID)).isNull();
            s.assertThat(fiche).containsEntry(DECOMPTE_ID, null);
        });
    }

    @Test
    void t6_triDecompteCode_accepte() throws Exception {
        String json = listeA("lotId", jeu.lotA1().toString(), "sort", "decompteCode,asc");

        List<String> ordre = codes(json);

        SoftAssertions.assertSoftly(s -> s.assertThat(ordre).as("…-DA2 avant …-DZ1 ; défaut (date décroissante) inverse")
                .containsExactly(jeu.code("-FA2"), jeu.code("-FA1")));
    }
}
