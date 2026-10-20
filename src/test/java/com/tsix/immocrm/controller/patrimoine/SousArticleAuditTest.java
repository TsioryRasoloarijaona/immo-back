package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit P0 d'un sous-article libre créé par {@code POST /api/sous-articles} — recalage R1 (« Audit P0 ») : type
 * {@code Yes.Stk.Building.UnderItem} « Sous article » déjà enregistré, une ligne {@code Added}.
 * Sans {@code @Transactional} : la ligne d'audit n'est écrite qu'à la validation ; tout est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SousArticleAuditTest {

    private static final Logger log = LoggerFactory.getLogger(SousArticleAuditTest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureSousArticles sousArticles;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final List<String[]> lignes = new ArrayList<>();
    private JdbcTemplate jdbc;
    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        admin = transactions.execute(statut -> fixture.administrateur());
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        c = transactions.execute(statut -> sousArticles.contexte("P4A"));
        SecurityContextHolder.clearContext();
        suivre("AdmUser", admin.getId());
        suivre("CfgCompany", c.entite().getId());
        suivre("CfgProject", c.projet().getId());
        suivre("CfgTranche", c.tranche().getId());
        suivre("StkBuilding", c.immeuble().getId());
        suivre("StkFloor", c.etage().getId());
        suivre("StkItemCategory", c.categorie().getId());
        suivre("StkItemType", c.type().getId());
        suivre("StkItemCategory", c.categorieSous().getId());
        suivre("StkItemType", c.typeSous().getId());
    }

    private void suivre(String table, UUID id) {
        lignes.add(new String[]{table, id.toString()});
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM StkItem WHERE StkHierarchyId = ?)",
                c.tranche().getId().toString());
        effacer("DELETE FROM StkItem WHERE StkHierarchyId = ?", c.tranche().getId().toString());
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM AdmUserHabilitation WHERE AdmUserId = ?)",
                admin.getId().toString());
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        for (int i = lignes.size() - 1; i >= 0; i--) {
            String[] ligne = lignes.get(i);
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", ligne[1], ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    /** Nettoyage au mieux : une table annexe (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    @Test
    void r1_creationLibre_uneLigneAdded_sousArticle() throws Exception {
        String code = "AUD-" + c.jeton();

        mockMvc.perform(post("/api/sous-articles").with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("trancheId", c.tranche().getId()).avec("code", code)
                                .avec("description", "Parking " + code)
                                .avec("categorieId", c.categorieSous().getId()).avec("typeId", c.typeSous().getId())
                                .avec("immeubleId", c.immeuble().getId()).avec("etageId", c.etage().getId())
                                .avec("surfaceProvisoire", BigDecimal.TEN).avec("surfaceDefinitive", BigDecimal.TEN)
                                .json()))
                .andExpect(status().isCreated());

        List<Map<String, Object>> audit = jdbc.queryForList("""
                        SELECT h.ObjectType, h.ObjectTypeLabel FROM AdmWebHistorics h
                        JOIN StkItem i ON i.PKey = h.ObjectId
                        WHERE i.Code = ? AND i.StkHierarchyId = ? AND h.Action = 'Added'""",
                code, c.tranche().getId().toString());
        assertThat(audit).singleElement().satisfies(ligne -> assertThat(ligne)
                .containsEntry("ObjectType", "Yes.Stk.Building.UnderItem")
                .containsEntry("ObjectTypeLabel", "Sous article"));
    }
}
