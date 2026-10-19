package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureArticleFiche.VUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suppression d'article — FEATURE-patrimoine-article-fiche.md §3, §7 (SuppressionArticleTest) : 409 si l'article est
 * occupé (vente, location, syndic) ou référencé par une FK NO ACTION ; sinon 204 et cascades faites par l'application
 * (yesRemsTest n'a pas de FK) : sous-articles rattachés, vues, contenances, pièces jointes, notes, tracés de plan.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuppressionArticleTest {

    private static final String ARTICLE = "/api/articles/{id}";
    private static final String ARTICLE_EXISTE = "SELECT COUNT(*) FROM StkItem WHERE PKey = :id";
    private static final String COLONNE_LOCATION = "RntFolderId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureArticleFiche fiche;
    @Autowired private FixtureTrancheFiche ficheTranche;
    @Autowired private FixtureEntites entites;
    @Autowired private StkItemRepository itemRepository;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgTranche tranche;
    private StkItemCategory categorie;
    private StkItemType type;
    private StkItemCategory categorieSous;
    private StkItemType typeSous;

    @BeforeEach
    void setUp() {
        fixture.referentielArticle();
        admin = fixture.administrateur();
        jeton = "P2AS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = fixture.entite("ENT-" + jeton, true);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, entite));
        categorie = fixture.categorie("CAT-" + jeton, fixture.natureBat(), false);
        type = fixture.type("TYP-" + jeton, fixture.natureBat(), false);
        categorieSous = fixture.categorie("CATS-" + jeton, fixture.natureBat(), true);
        typeSous = fixture.type("TYPS-" + jeton, fixture.natureBat(), true);
    }

    /** {@code parent} non nul : sous-article rattaché. */
    private StkItem article(String code, StkItem parent) {
        boolean sous = parent != null;
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code + "-" + jeton);
        item.setDescription("Article " + code);
        item.setStkItemCategory(sous ? categorieSous : categorie);
        item.setStkItemType(sous ? typeSous : type);
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

    private ResultActions supprimer(UUID id) throws Exception {
        return mockMvc.perform(delete(ARTICLE, id).with(connecte(admin)));
    }

    private int compter(String sql, UUID id) {
        return fiche.compter(sql, id);
    }

    // ------------------------------------------------------------------ §3 204

    @Test
    void r3_204_articleLibre_ligneSupprimee() throws Exception {
        StkItem a = article("ART", null);

        supprimer(a.getId()).andExpect(status().isNoContent());

        assertThat(compter(ARTICLE_EXISTE, a.getId())).isZero();
    }

    @Test
    void r3_204_sousArticlesRattachesSupprimes_sousArticleLibreGarde() throws Exception {
        StkItem a = article("ART-SA", null);
        StkItem rattache = article("SR", a);
        StkItem libre = article("SL", null);
        libre.setIsUnderItem(true);
        libre.setStkItemCategory(categorieSous);
        libre.setStkItemType(typeSous);
        itemRepository.saveAndFlush(libre);

        supprimer(a.getId()).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(compter(ARTICLE_EXISTE, rattache.getId())).isZero(),
                () -> assertThat(compter(ARTICLE_EXISTE, libre.getId())).isEqualTo(1));
    }

    @Test
    void r3_204_vuesEtContenancesSupprimees() throws Exception {
        StkItem a = article("ART-VC", null);
        fiche.vueArticle(a.getId(), fiche.reference(VUE, "VUE-" + jeton));
        fiche.contenanceArticle(a.getId(), fiche.contenance("CONT-" + jeton, "", false), 2);

        supprimer(a.getId()).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(compter("SELECT COUNT(*) FROM StkItemVue WHERE StkItemId = :id", a.getId())).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM StkItemContenance WHERE StkItemId = :id", a.getId()))
                        .isZero());
    }

    @Test
    void r3_204_piecesJointesEtNotes_deLArticleEtDeSesSousArticles_supprimees() throws Exception {
        StkItem a = article("ART-PJ", null);
        StkItem rattache = article("SR-PJ", a);
        ficheTranche.pieceJointe(a.getId());
        ficheTranche.message(a.getId());
        ficheTranche.pieceJointe(rattache.getId());
        ficheTranche.message(rattache.getId());

        supprimer(a.getId()).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmAttachedFile WHERE CategoryId = :id", a.getId())).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmMessage WHERE CategoryId = :id", a.getId())).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmAttachedFile WHERE CategoryId = :id", rattache.getId()))
                        .isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmMessage WHERE CategoryId = :id", rattache.getId()))
                        .isZero());
    }

    @Test
    void r3_204_tracesDePlanSupprimes() throws Exception {
        StkItem a = article("ART-PL", null);
        fiche.cheminPlan(a.getId());

        supprimer(a.getId()).andExpect(status().isNoContent());

        assertThat(compter("SELECT COUNT(*) FROM StkHierarchyPath WHERE StkItemId = :id", a.getId())).isZero();
    }

    // ------------------------------------------------------------------ §3 409 occupé

    @Test
    void r3_409_articleOccupeParUnDossierDeVente() throws Exception {
        StkItem a = article("ART-OV", null);
        a.setComFolderId(fixture.dossier("DOS-" + jeton).getId());
        itemRepository.saveAndFlush(a);

        supprimer(a.getId()).andExpect(status().isConflict());

        assertThat(compter(ARTICLE_EXISTE, a.getId())).isEqualTo(1);
    }

    @Test
    void r3_409_articleOccupeParUnDossierDeLocation() throws Exception {
        StkItem a = article("ART-OL", null);
        fiche.occuper(a.getId(), COLONNE_LOCATION, fiche.location("LOC-" + jeton));

        supprimer(a.getId()).andExpect(status().isConflict());

        assertThat(compter(ARTICLE_EXISTE, a.getId())).isEqualTo(1);
    }

    @Test
    void r3_409_articleOccupeParUnDossierDeSyndic() throws Exception {
        StkItem a = article("ART-OS", null);
        fiche.occuper(a.getId(), "SynFolderId", fiche.syndic("SYN-" + jeton));

        supprimer(a.getId()).andExpect(status().isConflict());

        assertThat(compter(ARTICLE_EXISTE, a.getId())).isEqualTo(1);
    }

    // ------------------------------------------------------------------ §3 409 référencé (FK NO ACTION)

    @Test
    void r3_409_articleCiteParUneProspection_messageNommeLeBloquant() throws Exception {
        StkItem a = article("ART-PR", null);
        fiche.prospection("PROS-" + jeton, a.getId());

        supprimer(a.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(matchesPattern("(?is).*(ComProspection|prospection).*")));

        assertThat(compter(ARTICLE_EXISTE, a.getId())).isEqualTo(1);
    }

    // ------------------------------------------------------------------ refus

    @Test
    void r3_404_articleInconnu() throws Exception {
        supprimer(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    /** R4 : article d'une entité juridique inactive introuvable, suppression comprise. */
    @Test
    void r4_404_entiteInactive_articleGarde() throws Exception {
        StkItem a = article("ART-R4", null);
        entites.actif(entite.getId(), false);

        supprimer(a.getId()).andExpect(status().isNotFound());

        assertThat(compter(ARTICLE_EXISTE, a.getId())).isEqualTo(1);
    }
}
