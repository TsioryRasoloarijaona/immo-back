package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Décision du manager (règle 6, argent) : un article porteur d'une dépense ({@code StkExpense}) n'est pas supprimé. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuppressionArticleDepensesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureArticleFiche fiche;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private EntityManager em;

    private StkItem article(String jeton, CfgTranche tranche, StkItem parent) {
        boolean sous = parent != null;
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode((sous ? "SA-" : "ART-") + jeton);
        item.setDescription("Article " + jeton);
        item.setStkItemCategory(fixture.categorie((sous ? "CATS-" : "CAT-") + jeton, fixture.natureBat(), sous));
        item.setStkItemType(fixture.type((sous ? "TYPS-" : "TYP-") + jeton, fixture.natureBat(), sous));
        item.setStkKind(fixture.natureBat());
        item.setStkVocation(fixture.vocationVente());
        item.setStkHierarchy(tranche);
        item.setIsUnderItem(sous);
        item.setIsUnderItemOrigin(sous);
        item.setIsTitled(!sous);
        item.setAlwaysAffected(false);
        item.setArea(BigDecimal.TEN);
        item.setAreaFloor(BigDecimal.TEN);
        item.setParent(parent);
        return itemRepository.saveAndFlush(item);
    }

    private UUID depense(UUID articleId) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("""
                        INSERT INTO StkExpense (Pkey, [Date], Amount, IsValidated, StkItemId)
                        VALUES (:id, :date, :montant, 0, :article)""")
                .setParameter("id", id).setParameter("date", LocalDate.of(2026, 1, 15))
                .setParameter("montant", new BigDecimal("12500.000000")).setParameter("article", articleId)
                .executeUpdate();
        return id;
    }

    private void verifierRefus(StkItem article, StkItem porteur, UUID depense, AdmUser admin) throws Exception {
        mockMvc.perform(delete("/api/articles/{id}", article.getId()).with(connecte(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("1 dépense(s)")));

        assertAll(
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM StkItem WHERE PKey = :id", article.getId()))
                        .isEqualTo(1),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM StkItem WHERE PKey = :id", porteur.getId()))
                        .isEqualTo(1),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM StkExpense WHERE Pkey = :id", depense))
                        .isEqualTo(1));
    }

    @Test
    void depenseSurLArticle_409_articleEtDepenseIntacts() throws Exception {
        fixture.referentielArticle();
        String jeton = "P2AD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgTranche tranche = fixture.tranche("TR-" + jeton,
                fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
        StkItem a = article(jeton, tranche, null);

        verifierRefus(a, a, depense(a.getId()), fixture.administrateur());
    }

    @Test
    void depenseSurUnSousArticleRattache_409_toutIntact() throws Exception {
        fixture.referentielArticle();
        String jeton = "P2AE" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgTranche tranche = fixture.tranche("TR-" + jeton,
                fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
        StkItem a = article(jeton, tranche, null);
        StkItem sous = article(jeton, tranche, a);

        verifierRefus(a, sous, depense(sous.getId()), fixture.administrateur());
    }
}
