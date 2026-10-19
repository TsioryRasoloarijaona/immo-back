package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.Tuple;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P2a — suppression (FEATURE-patrimoine-article-fiche.md §3, §11.4) : prix de vente du parent après la
 * suppression d'un sous-article, et 409 pour toute la suppression quand un sous-article rattaché est occupé / référencé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RevueSuppressionArticleTest {

    private static final String EXISTE = "SELECT COUNT(*) FROM StkItem WHERE PKey = :id";
    private static final String PRIX = "SELECT ComSalePrice, ComSalePriceHT FROM StkItem WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureArticleFiche fiche;
    @Autowired private StkItemRepository itemRepository;

    private AdmUser admin;
    private String jeton;
    private CfgTranche tranche;
    private StkItemCategory categorie;
    private StkItemType type;
    private StkItemCategory categorieSous;
    private StkItemType typeSous;

    @BeforeEach
    void setUp() {
        fixture.referentielArticle();
        admin = fixture.administrateur();
        jeton = "RVS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
        categorie = fixture.categorie("CAT-" + jeton, fixture.natureBat(), false);
        type = fixture.type("TYP-" + jeton, fixture.natureBat(), false);
        categorieSous = fixture.categorie("CATS-" + jeton, fixture.natureBat(), true);
        typeSous = fixture.type("TYPS-" + jeton, fixture.natureBat(), true);
    }

    private StkItem article(String code, StkItem parent, int prixTTC, int prixHT) {
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
        item.setSalePrice(BigDecimal.valueOf(prixTTC));
        item.setSalePriceHT(BigDecimal.valueOf(prixHT));
        item.setParent(parent);
        return itemRepository.saveAndFlush(item);
    }

    private ResultActions supprimer(UUID id) throws Exception {
        return mockMvc.perform(delete("/api/articles/{id}", id).with(connecte(admin)));
    }

    private static BigDecimal colonne(Tuple t, int i) {
        return new BigDecimal(t.get(i).toString());
    }

    /** Argent : le prix de vente du parent ne compte plus le sous-article supprimé. */
    @Test
    void suppressionDUnSousArticle_prixDeVenteDuParentRecalcule() throws Exception {
        StkItem parent = article("PAR", null, 100, 80);
        StkItem s1 = article("S1", parent, 30, 25);
        article("S2", parent, 20, 15);

        supprimer(s1.getId()).andExpect(status().isNoContent());

        Tuple prix = fiche.lignes(PRIX, parent.getId()).getFirst();
        assertAll(
                () -> assertThat(fiche.compter(EXISTE, s1.getId())).isZero(),
                () -> assertThat(colonne(prix, 0)).isEqualByComparingTo("120"),
                () -> assertThat(colonne(prix, 1)).isEqualByComparingTo("95"));
    }

    /** §11.4 : un sous-article rattaché loué bloque toute la suppression. */
    @Test
    void sousArticleRattacheLoue_409_rienNEstSupprime() throws Exception {
        StkItem parent = article("PAR-L", null, 100, 80);
        StkItem sous = article("SL", parent, 30, 25);
        fiche.occuper(sous.getId(), "RntFolderId", fiche.location("LOC-" + jeton));

        supprimer(parent.getId()).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(fiche.compter(EXISTE, parent.getId())).isEqualTo(1),
                () -> assertThat(fiche.compter(EXISTE, sous.getId())).isEqualTo(1));
    }

    /** §11.4 : un sous-article rattaché cité par une prospection bloque toute la suppression. */
    @Test
    void sousArticleRattacheCiteParUneProspection_409_rienNEstSupprime() throws Exception {
        StkItem parent = article("PAR-P", null, 100, 80);
        StkItem sous = article("SP", parent, 30, 25);
        fiche.prospection("PROS-" + jeton, sous.getId());

        supprimer(parent.getId()).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(fiche.compter(EXISTE, parent.getId())).isEqualTo(1),
                () -> assertThat(fiche.compter(EXISTE, sous.getId())).isEqualTo(1));
    }

    /** Sous-article d'un article vendu : occupé par son parent, 409 et prix du parent inchangé. */
    @Test
    void sousArticleDUnParentVendu_409() throws Exception {
        StkItem parent = article("PAR-V", null, 100, 80);
        StkItem sous = article("SV", parent, 30, 25);
        parent.setComFolderId(fixture.dossier("DOS-" + jeton).getId());
        itemRepository.saveAndFlush(parent);

        supprimer(sous.getId()).andExpect(status().isConflict());

        assertThat(fiche.compter(EXISTE, sous.getId())).isEqualTo(1);
    }
}
