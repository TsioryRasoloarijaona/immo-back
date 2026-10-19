package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Actions d'un article ({@code ComActionMessage.StkItemId}) — FEATURE-patrimoine-audit-actions.md §3, §11.7, §8
 * (ActionsArticleTest). Les formes de réponse sont celles de {@code ActionDossierDto} ; la liste est lue par
 * {@code $..id} / {@code $..note} pour ne pas présumer de l'enveloppe (§11.7 ne la fixe pas).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsArticleTest {

    private static final String ACTIONS = "/api/articles/{id}/actions";
    private static final String TYPE_ID = "typeId";
    private static final String NOTE = "note";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private ComActionMessageRepository actionRepository;
    @Autowired private ComActionMessageTypeRepository typeRepository;
    @Autowired private EntityManager entityManager;

    private AdmUser admin;
    private CfgTranche tranche;
    private StkItem article;
    private ComActionMessageType type;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        tranche = fixture.tranche("TR-ACT-ART", fixture.projet("PRJ-ACT-ART", fixture.entite("ENT-ACT-ART", true)));
        article = article("ART-ACT");
        type = new ComActionMessageType();
        type.setId(UUID.randomUUID());
        type.setCode("TEL-ACT-ART");
        type.setDescription("Telephone sortant");
        type = typeRepository.save(type);
    }

    private StkItem article(String code) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkHierarchy(tranche);
        item.setStkKind(fixture.natureBat());
        item.setStkVocation(fixture.vocationVente());
        item.setStkItemCategory(fixture.categorie("CAT-" + code, fixture.natureBat(), false));
        item.setStkItemType(fixture.type("TYP-" + code, fixture.natureBat(), false));
        return stkItemRepository.saveAndFlush(item);
    }

    private ComActionMessage action(StkItem sur, LocalDateTime date, String note) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setStkItem(sur);
        m.setComActionMessageType(type);
        m.setDocDate(date);
        m.setAlertMessage(note);
        m.setUserName("Auteur OMRAN");
        return actionRepository.saveAndFlush(m);
    }

    private CorpsJson corpsAction(String note) {
        return corps().avec(TYPE_ID, type.getId()).avec(NOTE, note);
    }

    private ResultActions creer(UUID articleId, RequestPostProcessor auteur, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(ACTIONS, articleId).with(auteur)
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> lignesDeLArticle() {
        entityManager.flush();
        return entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), PKey) AS PKey, DocDate, Description, AlertMessage, AlertValidityDate,
                               UserName, UserCreate, UserCreateDateTime, CONVERT(varchar(36), AdmUserId) AS AdmUserId,
                               CONVERT(varchar(36), StkItemId) AS StkItemId, CONVERT(varchar(36), ComFolderId) AS ComFolderId,
                               CONVERT(varchar(36), ComActionMessageTypeId) AS ComActionMessageTypeId,
                               IsSystem, IsUnpaid, WithoutRevival
                        FROM ComActionMessage WHERE StkItemId = :id""", Tuple.class)
                .setParameter("id", article.getId())
                .getResultList();
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    // ------------------------------------------------------------------ §3 GET

    @Test
    void r3_lecture_actionsDeLArticleSeulement_plusRecentesDAbord() throws Exception {
        ComActionMessage janvier = action(article, LocalDateTime.of(2026, 1, 5, 9, 0), "Appel janvier");
        ComActionMessage mars = action(article, LocalDateTime.of(2026, 3, 5, 9, 0), "Appel mars");
        action(article("ART-ACT-AUTRE"), LocalDateTime.of(2026, 2, 5, 9, 0), "Autre article");

        mockMvc.perform(get(ACTIONS, article.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..id").value(contains(mars.getId().toString(), janvier.getId().toString())))
                .andExpect(jsonPath("$..note").value(contains("Appel mars", "Appel janvier")));
    }

    @Test
    void r0_3_lectureParUnNonAdministrateur_200() throws Exception {
        ComActionMessage a = action(article, LocalDateTime.of(2026, 1, 5, 9, 0), "Visible");

        mockMvc.perform(get(ACTIONS, article.getId()).with(commercial(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..id").value(contains(a.getId().toString())));
    }

    @Test
    void r3_lecture_articleInconnu_404() throws Exception {
        mockMvc.perform(get(ACTIONS, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §3 / §11.7 POST

    @Test
    void r3_creation_201_relueDansLaListe() throws Exception {
        creer(article.getId(), connecte(admin), corpsAction("Visite du lot"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("Visite du lot"))
                .andExpect(jsonPath("$.typeId").value(type.getId().toString()));

        mockMvc.perform(get(ACTIONS, article.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..note").value(hasItem("Visite du lot")));
    }

    /** §3 : colonnes non NULL de la ligne modèle prod (action d'article, ComFolderId NULL), auteur du compte connecté. */
    @Test
    void r3_creation_colonnesCommeLaLigneModele() throws Exception {
        creer(article.getId(), connecte(admin), corpsAction("  Rappeler le client  ")
                .avec("dateAlerte", "2026-12-01T00:00:00")).andExpect(status().isCreated());

        assertThat(lignesDeLArticle()).singleElement().satisfies(l -> assertAll(
                () -> assertThat(guid(l.get("StkItemId"))).isEqualTo(guid(article.getId())),
                () -> assertThat(l.get("ComFolderId")).isNull(),
                () -> assertThat(guid(l.get("ComActionMessageTypeId"))).isEqualTo(guid(type.getId())),
                () -> assertThat(l.get("AlertMessage")).isEqualTo("Rappeler le client"),
                () -> assertThat(l.get("AlertValidityDate")).isNotNull(),
                () -> assertThat(l.get("DocDate")).isNotNull(),
                () -> assertThat(l.get("Description")).isNotNull(),
                () -> assertThat(l.get("UserName")).isEqualTo(AUTEUR),
                () -> assertThat(l.get("UserCreate")).isEqualTo(AUTEUR),
                () -> assertThat(l.get("UserCreateDateTime")).isNotNull(),
                () -> assertThat(guid(l.get("AdmUserId"))).isEqualTo(guid(admin.getId())),
                () -> assertThat(l.get("IsSystem")).isEqualTo(false),
                () -> assertThat(l.get("IsUnpaid")).isEqualTo(false),
                () -> assertThat(l.get("WithoutRevival")).isEqualTo(false)));
    }

    @Test
    void r3_creation_sansAlerte_withoutRevivalVrai() throws Exception {
        creer(article.getId(), connecte(admin), corpsAction("Sans alerte")).andExpect(status().isCreated());

        assertThat(lignesDeLArticle()).singleElement()
                .satisfies(l -> assertThat(l.get("WithoutRevival")).isEqualTo(true));
    }

    // ------------------------------------------------------------------ 400, 403, 404

    @Test
    void r3_creation_typeAbsent_400() throws Exception {
        creer(article.getId(), connecte(admin), corpsAction("Sans type").sans(TYPE_ID))
                .andExpect(status().isBadRequest());
        assertThat(lignesDeLArticle()).isEmpty();
    }

    @ParameterizedTest(name = "note \"{0}\"")
    @ValueSource(strings = {"", "   "})
    void r3_creation_noteVide_400(String note) throws Exception {
        creer(article.getId(), connecte(admin), corpsAction(note)).andExpect(status().isBadRequest());
        assertThat(lignesDeLArticle()).isEmpty();
    }

    @Test
    void r0_3_creationParUnNonAdministrateur_403() throws Exception {
        creer(article.getId(), commercial(admin), corpsAction("Note commerciale")).andExpect(status().isForbidden());
        assertThat(lignesDeLArticle()).isEmpty();
    }

    @Test
    void r3_creation_articleInconnu_404() throws Exception {
        creer(UUID.randomUUID(), connecte(admin), corpsAction("Article fantome")).andExpect(status().isNotFound());
    }
}
