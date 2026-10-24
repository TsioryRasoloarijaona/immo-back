package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.controller.patrimoine.ChargerDepuisTrancheTest.CHARGER;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit du chargement — FEATURE-patrimoine-reliquat-v2.md §A.4 « Audit », §A.6 règle 9 : la tranche cible modifiée et les
 * lignes d'étage créées apparaissent dans {@code GET /api/audit?objetId=<cible>}.
 *
 * <p>Sans {@code @Transactional} : {@code AdmWebHistorics} n'est écrite qu'à la validation. Chaque test valide ses
 * écritures et les efface en {@code @AfterEach} (modèle : {@code TrancheOngletsAuditTest}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChargerDepuisTrancheRevueTest {

    private static final Logger log = LoggerFactory.getLogger(ChargerDepuisTrancheRevueTest.class);

    private static final String TYPE_TRANCHE = "Yes.Cfg.CfgTranche";
    private static final String TYPE_ETAGE = "Yes.Cfg.CfgTrancheLevel";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    private final Deque<String[]> creees = new ArrayDeque<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private CfgTranche source;
    private CfgTranche cible;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        admin = fixture.administrateur();
        suivre("AdmUser", admin.getId());
        CfgCompany entite = commeAdmin(() -> fixture.entite("ENT-P5BAU-" + suffixe, true));
        suivre("CfgCompany", entite.getId());
        CfgProject projet = commeAdmin(() -> fixture.projet("PRJ-P5BAU-" + suffixe, entite));
        suivre("CfgProject", projet.getId());
        source = commeAdmin(() -> fixture.tranche("TR-S-P5BAU-" + suffixe, projet));
        suivre("CfgTranche", source.getId());
        cible = commeAdmin(() -> fixture.tranche("TR-C-P5BAU-" + suffixe, projet));
        suivre("CfgTranche", cible.getId());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        for (UUID tranche : List.of(cible.getId(), source.getId())) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM CfgTrancheLevel WHERE CfgTrancheId = ?)",
                    tranche);
            effacer("DELETE FROM CfgTrancheLevel WHERE CfgTrancheId = ?", tranche);
        }
        while (!creees.isEmpty()) {
            String[] ligne = creees.pop();
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", ligne[1], ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
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
        creees.push(new String[] {table, id.toString()});
    }

    private <T> T commeAdmin(Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        return transactions.execute(statut -> ecriture.get());
    }

    private UUID niveau() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO LndLevel (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(),
                "NIV-P5BAU-" + suffixe, "Niveau " + suffixe);
        suivre("LndLevel", id);
        return id;
    }

    @Test
    void r9_chargement_trancheModifieeEtEtageCree_visiblesDansLAuditDeLaCible() throws Exception {
        jdbc.update("UPDATE CfgTranche SET Address = ? WHERE PKey = ?", "1 rue Source P5B", source.getId().toString());
        jdbc.update("INSERT INTO CfgTrancheLevel (PKey, CfgTrancheId, LndLevelId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), source.getId().toString(), niveau().toString());

        mockMvc.perform(post(CHARGER, cible.getId(), source.getId()).with(connecte(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit").param("objetId", cible.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.typeObjet=='" + TYPE_TRANCHE + "')].action", hasItem("Modified")))
                .andExpect(jsonPath("$.content[?(@.typeObjet=='" + TYPE_ETAGE + "')].action", hasItem("Added")));
    }
}
