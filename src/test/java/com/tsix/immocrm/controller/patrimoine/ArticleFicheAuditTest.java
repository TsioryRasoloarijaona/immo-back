package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit des lignes d'article — FEATURE-patrimoine-article-fiche.md §0, §3, §9.8 (ArticleFicheAuditTest).
 *
 * <p>Types et libellés : aucune ligne {@code StkItemVue} / {@code StkItemContenance} dans {@code AdmWebHistorics} de
 * yesRemsProd ; OMRAN écrit {@code ObjectType = GetType().FullName} et {@code ObjectTypeLabel =}
 * le {@code DisplayName} de la classe (AdmWebHistorics.cs:326-327) : {@code Yes.Stk.Building.StkItemVue} « Vues »
 * (StkItemVue.cs:13-15), {@code Yes.Stk.Building.ItemContenance} « Contenances » (ItemContenance.cs:11-13).
 *
 * <p>Sans {@code @Transactional} : la ligne d'audit n'est écrite qu'à la validation (P0). Chaque test valide ses
 * écritures et les efface en {@code @AfterEach} ; codes suffixés d'un aléa (yesRemsTest est partagée).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ArticleFicheAuditTest {

    private static final Logger log = LoggerFactory.getLogger(ArticleFicheAuditTest.class);

    private static final String TYPE_ARTICLE = "Yes.Stk.Building.Item";
    private static final String TYPE_SOUS_ARTICLE = "Yes.Stk.Building.UnderItem";
    private static final String TYPE_VUE = "Yes.Stk.Building.StkItemVue";
    private static final String TYPE_CONTENANCE = "Yes.Stk.Building.ItemContenance";
    private static final String ADDED = "Added";
    private static final String DELETED = "Deleted";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final String PARENT_TYPE = "ParentObjectType";
    private static final String PARENT_ID = "ParentObjectId";
    private static final String STK_ITEM = "StkItem";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    private final Deque<LigneCreee> creees = new ArrayDeque<>();
    private final List<UUID> objets = new ArrayList<>();
    private final List<UUID> articles = new ArrayList<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private CfgTranche tranche;
    private StkBuilding immeuble;
    private StkFloor etage;
    private StkItemCategory categorie;
    private StkItemType type;

    private record LigneCreee(String table, UUID id) {
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        admin = fixture.administrateur();
        suivre("AdmUser", admin.getId());
        commeAdmin(() -> {
            fixture.referentielArticle();
            return null;
        });
        CfgCompany entite = commeAdmin(() -> fixture.entite("ENT-P2AAU-" + suffixe, true));
        suivre("CfgCompany", entite.getId());
        CfgProject projet = commeAdmin(() -> fixture.projet("PRJ-P2AAU-" + suffixe, entite));
        suivre("CfgProject", projet.getId());
        tranche = commeAdmin(() -> fixture.tranche("TR-P2AAU-" + suffixe, projet));
        suivre("CfgTranche", tranche.getId());
        immeuble = commeAdmin(() -> fixture.immeuble("IMM-P2AAU-" + suffixe));
        suivre("StkBuilding", immeuble.getId());
        etage = commeAdmin(() -> fixture.etage("ETG-P2AAU-" + suffixe));
        suivre("StkFloor", etage.getId());
        categorie = commeAdmin(() -> fixture.categorie("CAT-P2AAU-" + suffixe, fixture.natureBat(), false));
        suivre("StkItemCategory", categorie.getId());
        type = commeAdmin(() -> fixture.type("TYP-P2AAU-" + suffixe, fixture.natureBat(), false));
        suivre("StkItemType", type.getId());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        articles.forEach(id -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM StkItemVue WHERE StkItemId = ?)", id);
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM StkItemContenance WHERE StkItemId = ?)",
                    id);
            effacer("DELETE FROM StkItemVue WHERE StkItemId = ?", id);
            effacer("DELETE FROM StkItemContenance WHERE StkItemId = ?", id);
        });
        objets.forEach(id -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id);
            effacer("DELETE FROM AdmMessage WHERE CategoryId = ?", id);
            effacer("DELETE FROM AdmAttachedFile WHERE CategoryId = ?", id);
        });
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM AdmUserHabilitation WHERE AdmUserId = ?)",
                admin.getId());
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId());
        while (!creees.isEmpty()) {
            LigneCreee ligne = creees.pop();
            effacer("DELETE FROM " + ligne.table() + " WHERE PKey = ?", ligne.id());
        }
    }

    /** Nettoyage au mieux : une table annexe inconnue (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, Arrays.stream(parametres).map(Object::toString).toArray());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private void suivre(String table, UUID id) {
        creees.push(new LigneCreee(table, id));
        objets.add(id);
    }

    // ------------------------------------------------------------------ outils

    private <T> T commeAdmin(Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        return transactions.execute(statut -> ecriture.get());
    }

    private UUID inserer(String table, String colonnes, Object... valeurs) {
        UUID id = UUID.randomUUID();
        Object[] parametres = new Object[valeurs.length + 1];
        parametres[0] = id.toString();
        System.arraycopy(valeurs, 0, parametres, 1, valeurs.length);
        String marques = String.join(",", Collections.nCopies(parametres.length, "?"));
        jdbc.update("INSERT INTO " + table + " (PKey, " + colonnes + ") VALUES (" + marques + ")", parametres);
        suivre(table, id);
        return id;
    }

    private UUID creerArticle(String code) throws Exception {
        String json = mockMvc.perform(post("/api/articles").with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("trancheId", tranche.getId()).avec("code", code)
                                .avec("description", "Article " + code).avec("categorieId", categorie.getId())
                                .avec("typeId", type.getId()).avec("immeubleId", immeuble.getId())
                                .avec("etageId", etage.getId()).avec("surfaceProvisoire", BigDecimal.TEN)
                                .avec("surfaceDefinitive", BigDecimal.TEN).json()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        suivre(STK_ITEM, id);
        articles.add(id);
        return id;
    }

    /** Sous-article rattaché, écrit hors API (aucune ligne Added). */
    private UUID sousArticleRattache(UUID parentId, String code) {
        StkItemCategory categorieSous = commeAdmin(() -> fixture.categorie("CATS-" + code, fixture.natureBat(), true));
        suivre("StkItemCategory", categorieSous.getId());
        StkItemType typeSous = commeAdmin(() -> fixture.type("TYPS-" + code, fixture.natureBat(), true));
        suivre("StkItemType", typeSous.getId());
        UUID id = inserer(STK_ITEM, "Code, Description, StkItemTypeId, StkItemCategoryId, StkVocationId, StkKindId, "
                        + "StkHierarchyId, ParentId, IsUnderItem, IsUnderItemOrigin",
                code, "Annexe " + code, typeSous.getId().toString(), categorieSous.getId().toString(),
                FixturePatrimoine.VOCATION_VENTE.toString(), FixturePatrimoine.NATURE_BAT.toString(),
                tranche.getId().toString(), parentId.toString(), true, true);
        articles.add(id);
        return id;
    }

    private UUID seuleLigne(String sql, UUID articleId) {
        List<String> ids = jdbc.queryForList(sql, String.class, articleId.toString());
        assertThat(ids).as(sql).hasSize(1);
        UUID id = UUID.fromString(ids.get(0));
        objets.add(id);
        return id;
    }

    private List<Map<String, Object>> lignesAudit(UUID objetId, String action) {
        return jdbc.queryForList("""
                        SELECT Action, ObjectType, ObjectTypeLabel, ParentObjectType,
                               CONVERT(varchar(36), ParentObjectId) AS ParentObjectId
                        FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?""", objetId.toString(), action);
    }

    private Map<String, Object> seuleLigneAudit(UUID objetId, String action) {
        List<Map<String, Object>> trouvees = lignesAudit(objetId, action);
        assertThat(trouvees).as("lignes %s de %s", action, objetId).hasSize(1);
        return trouvees.get(0);
    }

    private static void verifier(Map<String, Object> ligne, String type, String libelle, UUID parent) {
        assertAll(
                () -> assertThat(ligne).containsEntry(OBJECT_TYPE, type).containsEntry("ObjectTypeLabel", libelle)
                        .containsEntry(PARENT_TYPE, TYPE_ARTICLE),
                () -> assertThat(guid(ligne.get(PARENT_ID))).isEqualTo(guid(parent)));
    }

    // ------------------------------------------------------------------ §9.8 une ligne Added par ligne enfant écrite

    @Test
    void r9_8_vueAjoutee_ligneAdded_enfantDeLArticle() throws Exception {
        UUID article = creerArticle("ART-P2AAU-V-" + suffixe);
        UUID vue = inserer("StkVue", "Code, Description", "VUE-P2AAU-" + suffixe, "Vue " + suffixe);

        mockMvc.perform(patch("/api/articles/{id}", article).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("vueIds", List.of(vue)).json()))
                .andExpect(status().isOk());

        UUID ligne = seuleLigne("SELECT CONVERT(varchar(36), PKey) FROM StkItemVue WHERE StkItemId = ?", article);
        verifier(seuleLigneAudit(ligne, ADDED), TYPE_VUE, "Vues", article);
    }

    @Test
    void r9_8_contenanceAjoutee_ligneAdded_enfantDeLArticle() throws Exception {
        UUID article = creerArticle("ART-P2AAU-C-" + suffixe);
        UUID contenance = inserer("StkContenance", "Code, Description", "CONT-P2AAU-" + suffixe, "Contenance " + suffixe);

        mockMvc.perform(put("/api/articles/{id}/contenances", article).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lignes(corps().avec("contenanceId", contenance).avec("nombre", 1))))
                .andExpect(status().isOk());

        UUID ligne = seuleLigne("SELECT CONVERT(varchar(36), PKey) FROM StkItemContenance WHERE StkItemId = ?", article);
        verifier(seuleLigneAudit(ligne, ADDED), TYPE_CONTENANCE, "Contenances", article);
    }

    // ------------------------------------------------------------------ §3 une ligne Deleted par objet supprimé

    @Test
    void r3_suppression_lignesDeleted_articleSousArticleEtVue() throws Exception {
        UUID article = creerArticle("ART-P2AAU-D-" + suffixe);
        UUID sous = sousArticleRattache(article, "SA-P2AAU-D-" + suffixe);
        UUID vue = inserer("StkVue", "Code, Description", "VUE-P2AAU-D-" + suffixe, "Vue " + suffixe);
        UUID ligneVue = inserer("StkItemVue", "StkItemId, StkVueId", article.toString(), vue.toString());

        mockMvc.perform(delete("/api/articles/{id}", article).with(connecte(admin))).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(seuleLigneAudit(article, DELETED)).containsEntry(OBJECT_TYPE, TYPE_ARTICLE),
                () -> assertThat(seuleLigneAudit(sous, DELETED)).containsEntry(OBJECT_TYPE, TYPE_SOUS_ARTICLE),
                () -> assertThat(seuleLigneAudit(ligneVue, DELETED)).containsEntry(OBJECT_TYPE, TYPE_VUE));
    }
}
