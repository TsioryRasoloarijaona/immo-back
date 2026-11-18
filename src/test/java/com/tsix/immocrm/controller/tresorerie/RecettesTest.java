package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Cadre;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.AFFECTATIONS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.CREDIT;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.MODULE;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.RECETTES;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.SENS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R7 (FEATURE-n3-recettes-depenses.md) : les Recettes vides rendent {@code lignes = []} et des totaux à 0, pas 404.
 * La société du compte porte un décaissement affecté (débit) : il ne doit apparaître dans aucune liste de recettes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecettesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerie fixture;

    private AppelsTresorerie appels;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new AppelsTresorerie(mockMvc);
        Cadre c = fixture.cadre("TRR");
        UUID fa = fixture.piece(c.code("-FA"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "1000");
        UUID ra = fixture.reglement(Reglement.debit(c.code("-RA"), c, "300"));
        fixture.ligne(ra, fa, "300");
        a = connecte(fixture.compte(c, "TRRA", FixtureTresorerie.DROIT_PROFORMA, FixtureTresorerie.DROIT_FACTURES,
                FixtureTresorerie.DROIT_AVOIRS, FixtureTresorerie.DROIT_ENCAISSEMENTS,
                FixtureTresorerie.DROIT_AFFECTATIONS_RECETTE), FixtureTresorerie.ROLE);
    }

    private static void verifierVide(Reponse r) {
        List<?> lignes = (List<?>) r.valeur("$.lignes");
        Map<String, Object> totaux = asMap(r.valeur("$.totaux"));

        assertThat(lignes).as(r.corps()).isEmpty();
        assertThat(totaux.values()).as("totaux de %s", r.corps()).isNotEmpty()
                .allSatisfy(v -> assertThat(new BigDecimal(String.valueOf(v))).isEqualByComparingTo("0"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object valeur) {
        assertThat(valeur).isInstanceOf(Map.class);
        return (Map<String, Object>) valeur;
    }

    @ParameterizedTest(name = "{0} sous {1}")
    @CsvSource({
            "proforma, PRJ", "factures, PRJ", "avoirs, PRJ", "encaissements, PRJ",
            "proforma, BUY", "factures, BUY", "avoirs, BUY", "encaissements, BUY"
    })
    void r7_recetteVide_lignesVidesEtTotauxAZero(String vue, String module) throws Exception {
        verifierVide(appels.ok(a, RECETTES + vue, MODULE, module));
    }

    @ParameterizedTest(name = "affectations de recette sous {0}")
    @CsvSource({"PRJ", "BUY"})
    void r7_affectationsDeRecetteVides_lignesVidesEtTotauxAZero(String module) throws Exception {
        verifierVide(appels.ok(a, AFFECTATIONS, MODULE, module, SENS, CREDIT));
    }
}
