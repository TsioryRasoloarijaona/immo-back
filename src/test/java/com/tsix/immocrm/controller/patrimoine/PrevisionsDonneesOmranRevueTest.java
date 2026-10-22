package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
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
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P8b-1 : lignes FSPrevision telles qu'OMRAN les a écrites en production (YESTEST, yesRemsProd 22/10) —
 * « Architecte Décorateur » porte IsWithoutPrevision = 1 ET une référence (2,5 % de « Gros oeuvres ») ; 15 feuilles
 * cochées sans montant (« Caissons Cuisine »…). Le recalcul relit ces lignes : il ne doit ni refuser l'étude ni les effacer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsDonneesOmranRevueTest {

    private static final String AUCUNE_LIGNE = "{\"lignes\":[]}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;
    @Autowired private EntityManager entityManager;

    private ApiFaisabilite api;
    private Categories cat;
    private UUID etude;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        cat = finance.categoriesDeTest(jeton);
        etude = api.etude(yestest(fiche("OMR-" + jeton, societe.getId())));
        UUID uniteBase = api.unite("PV-" + jeton, false, null);
        api.unites(etude, lignes(ligneUniteCorps(1, uniteBase, d("2970")))).andExpect(status().isOk());
        UUID unitePv = fixture.ligneUnite(etude, uniteBase);
        api.remplacer(PREVISIONS, etude, lignes(parUnite(cat.grosOeuvres(), unitePv, "2000", "0.2")))
                .andExpect(status().isOk());
    }

    /** Ligne écrite par OMRAN, hors CRM (même forme que les lignes de yesRemsProd). */
    private void ligneOmran(UUID categorie, UUID reference, String ratio, String ht, String ttc) {
        entityManager.flush();
        entityManager.createNativeQuery("""
                        INSERT INTO FSPrevision (PKey, FeasibilityStudyId, StlCategoryId, ParentId, Ratio, AmountHT, Amount,
                                                 VatRatio, GlobalRatio, Note, Undivided, IsForProject)
                        VALUES (:id, :etude, :categorie, :reference, :ratio, :ht, :ttc, 0.2, 0, N'', 0, 1)""")
                .setParameter("id", UUID.randomUUID()).setParameter("etude", etude).setParameter("categorie", categorie)
                .setParameter("reference", reference).setParameter("ratio", new BigDecimal(ratio))
                .setParameter("ht", new BigDecimal(ht)).setParameter("ttc", new BigDecimal(ttc)).executeUpdate();
        entityManager.clear();
    }

    /** YESTEST en prod : la catégorie « Architecte Décorateur » est IsWithoutPrevision = 1 et porte 148 500 HT. */
    @Test
    void yestest_ligneOmranSurCategorieSansPrevision_lectureDuBilan200_etCoutTotal6088500() throws Exception {
        ligneOmran(cat.sansPrevision(), cat.grosOeuvres(), "0.025", "148500", "178200");

        String json = api.lire(PREVISIONS, etude).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(new BigDecimal(String.valueOf((Object) JsonPath.read(json, "$.totaux.depenseHt"))))
                .isCloseTo(d("6088500"), within(d("0.00001")));
    }

    /** Toute écriture de l'étude relance le recalcul, qui relit la même ligne : elle ne doit pas rendre l'étude inmodifiable. */
    @Test
    void yestest_ligneOmranSurCategorieSansPrevision_ecritureDeLEtudeNonBloquee() throws Exception {
        ligneOmran(cat.sansPrevision(), cat.grosOeuvres(), "0.025", "148500", "178200");

        api.remplacer(FINANCEMENTS, etude, AUCUNE_LIGNE).andExpect(status().isOk());
    }

    /** Feuille cochée sans montant (15 lignes de ce type sur YESTEST) : le recalcul la garde (§2.1 une ligne par catégorie de l'arbre). */
    @Test
    void feuilleCocheeSansMontant_conserveeParLeRecalcul() throws Exception {
        ligneOmran(cat.divers(), null, "0", "0", "0");

        api.remplacer(FINANCEMENTS, etude, AUCUNE_LIGNE).andExpect(status().isOk());

        assertThat(finance.prevision(etude, cat.divers())).isNotNull();
    }
}
