package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.Categories;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seconde revue du correctif 8bc4f59 (P8b-1) : le contrôle « sans prévision » quitté du calcul pur ne doit ouvrir ni un
 * choix de catégorie exclue, ni un contournement du montant imposé, ni un audit « Modified » d'ancêtres inchangés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsCorrectifSansPrevisionRevueTest {

    private static final String TVA = "0.2";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;
    @Autowired private EntityManager entityManager;

    private ApiFaisabilite api;
    private Categories cat;
    private UUID etude;
    private UUID unitePv;
    private String jeton;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        cat = finance.categoriesDeTest(jeton);
        etude = api.etude(yestest(fiche("CSP-" + jeton, societe.getId())));
        UUID uniteBase = api.unite("PV-" + jeton, false, null);
        api.unites(etude, lignes(ligneUniteCorps(1, uniteBase, d("2970")))).andExpect(status().isOk());
        unitePv = fixture.ligneUnite(etude, uniteBase);
    }

    private CorpsJson grosOeuvres() {
        return parUnite(cat.grosOeuvres(), unitePv, "2000", TVA);
    }

    /** Une catégorie « sans prévision » cochée SANS montant reste non sélectionnable ; rien n'est écrit. */
    @Test
    void sansPrevisionCocheeSansMontant_400_lignesEnregistreesIntactes() throws Exception {
        api.remplacer(PREVISIONS, etude, lignes(grosOeuvres())).andExpect(status().isOk());

        api.remplacer(PREVISIONS, etude, lignes(grosOeuvres(), corps().avec(CATEGORIE_ID, cat.sansPrevision())))
                .andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(finance.prevision(etude, cat.sansPrevision())).isNull(),
                () -> assertThat(decimal(finance.prevision(etude, cat.grosOeuvres()).get("AmountHT")))
                        .isEqualByComparingTo("5940000"));
    }

    /** L'aperçu applique le même refus. */
    @Test
    void sansPrevision_apercu_400() throws Exception {
        api.envoyer(PREVISIONS + "/calcul", etude, lignes(grosOeuvres(), parUnite(cat.sansPrevision(), unitePv, "1", TVA)))
                .andExpect(status().isBadRequest());
    }

    /** Ligne de relation renvoyée sans montant (mais avec une TVA) : acceptée, montant et TVA restent imposés. */
    @Test
    void ligneVerrouilleeRenvoyeeSansMontant_montantEtTvaImposes() throws Exception {
        UUID terrain = finance.depense("TER-" + jeton, RACINE_DEPENSE);
        finance.relier(RELATION_PRIX_TERRAIN, terrain);

        api.remplacer(PREVISIONS, etude, lignes(grosOeuvres(), corps().avec(CATEGORIE_ID, terrain)
                .avec(TAUX_TVA, new BigDecimal("0.5")))).andExpect(status().isOk());

        assertAll(
                () -> assertThat(decimal(finance.prevision(etude, terrain).get("AmountHT"))).isEqualByComparingTo("4500000"),
                () -> assertThat(decimal(finance.prevision(etude, terrain).get("VatRatio"))).isNotEqualByComparingTo("0.5"));
    }

    /** Deux enregistrements identiques avec une feuille cochée sans montant : ni doublon, ni ancêtre ré-horodaté. */
    @Test
    void feuilleCocheeSansMontant_reenregistree_niDoublonNiAncetreRetrace() throws Exception {
        CorpsJson divers = corps().avec(CATEGORIE_ID, cat.divers());
        api.remplacer(PREVISIONS, etude, lignes(grosOeuvres(), divers)).andExpect(status().isOk());
        Object avant = horodatage(RACINE_DEPENSE);
        Object diversAvant = horodatage(cat.divers());

        api.remplacer(PREVISIONS, etude, lignes(grosOeuvres(), divers)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM FSPrevision WHERE FeasibilityStudyId = :id", etude))
                        .isEqualTo(4),
                () -> assertThat(horodatage(RACINE_DEPENSE)).isEqualTo(avant),
                () -> assertThat(horodatage(cat.divers())).isEqualTo(diversAvant));
    }

    private Object horodatage(UUID categorie) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createNativeQuery("SELECT CONVERT(varchar(30), UserUpdateDateTime, 126) FROM FSPrevision "
                        + "WHERE FeasibilityStudyId = :etude AND StlCategoryId = :categorie")
                .setParameter("etude", etude).setParameter("categorie", categorie).getSingleResult();
    }
}
