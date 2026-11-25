package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tsix.immocrm.controller.projet.LotModificationTest.CHAINE;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R5 (D4) — FEATURE-lc1-creation-lot.md : le 400 d'une date antérieure à la précédente de la chaîne porte le message OMRAN
 * « La date 'A' doit être supérieure ou égale à la date 'B' » (A = date fautive, B = date précédente ; libellés Arpent).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotDatesMessagesTest {

    // Les libellés peuvent contenir une apostrophe (« Lancement de l'appel d'offres ») ; point final toléré.
    private static final Pattern GABARIT = Pattern.compile("La date '(.+?)' doit être supérieure ou égale à la date '(.+)'\\.?");
    private static final String TOT = "2026-03-11";
    private static final String TARD = "2026-03-10";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;

    private LotFicheSupport appels;
    private UUID lot;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        String jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        UUID projet = parite.projet("DMP-" + jeton, parite.entiteJuridique("DME-" + jeton));
        UUID statut = parite.statut(ENTREPRENEURS, "DMS-" + jeton);
        lot = parite.lot(projet, "DML-" + jeton, ENTREPRENEURS, statut);
        qui = connecte(parite.avecDroits("lots.dm." + jeton, List.of(), projet), "Commercial");
    }

    // R5 : chacune des 11 paires consécutives ; A = libellé de la date fautive (rang i+1), B = celui de la précédente (rang i).
    @Test
    void r5_pairesConsecutives_400_detailSuitLeGabarit_libellesNonVidesDistincts() throws Exception {
        List<String> fautives = new ArrayList<>();
        List<String> precedentes = new ArrayList<>();
        SoftAssertions s = new SoftAssertions();
        for (int i = 0; i + 1 < CHAINE.size(); i++) {
            String couple = CHAINE.get(i) + " > " + CHAINE.get(i + 1);
            Reponse r = appels.modifier(qui, lot, corps().avec(CHAINE.get(i), TOT).avec(CHAINE.get(i + 1), TARD));
            s.assertThat(r.statut()).as(couple + " : " + r.corps()).isEqualTo(400);
            String detail = detail(r);
            Matcher m = GABARIT.matcher(detail == null ? "" : detail);
            s.assertThat(m.matches()).as(couple + " : detail = " + detail).isTrue();
            if (m.matches()) {
                s.assertThat(m.group(1).isBlank() || m.group(1).equals(m.group(2))).as(couple + " : A et B non vides, distincts")
                        .isFalse();
                fautives.add(m.group(1));
                precedentes.add(m.group(2));
            }
        }
        s.assertAll();
        // Un champ garde le même libellé qu'il soit A (date fautive) ou B (date précédente) ; les 12 libellés sont distincts.
        List<String> libelles = new ArrayList<>(precedentes);
        libelles.add(fautives.get(fautives.size() - 1));
        SoftAssertions.assertSoftly(t -> {
            t.assertThat(fautives.subList(0, fautives.size() - 1)).as("A(i) = B(i+1)").isEqualTo(precedentes.subList(1, precedentes.size()));
            t.assertThat(libelles).hasSize(CHAINE.size()).doesNotHaveDuplicates();
        });
    }

    private static String detail(Reponse r) {
        try {
            return JsonPath.read(r.corps(), "$.detail");
        } catch (RuntimeException absent) {
            return null;
        }
    }
}
