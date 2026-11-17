package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureModelesSimulation;
import com.tsix.immocrm.fixtures.FixtureModelesSimulation.Ligne;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.saisieModele;
import static com.tsix.immocrm.fixtures.FixtureModelesSimulation.DROIT_MODELES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-lot-previsions-reel.md §4 M7 : une trace {@code AdmWebHistorics} par création / modification / suppression de modèle,
 * {@code ObjectType = 'Yes.Prj.Config.MarketPrevisionModel'}.
 * <p>Sans {@code @Transactional} : la trace n'est écrite qu'à la validation. Données validées, effacées en {@code @AfterEach}.
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class ModelesSimulationAuditTest {

    private static final String TYPE = "Yes.Prj.Config.MarketPrevisionModel";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final Ligne ZERO = new Ligne("0", "0", "0");
    private static final Ligne FIN = new Ligne("1", "1", "1");

    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;
    @Autowired private FixtureModelesSimulation modeles;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    private final Deque<String[]> creees = new ArrayDeque<>();
    private final List<UUID> modelesCrees = new ArrayList<>();

    private JdbcTemplate jdbc;
    private PrevisionsReelSupport appels;
    private RequestPostProcessor compte;

    @BeforeEach
    void setUp() {
        modeles.tables();
        jdbc = new JdbcTemplate(dataSource);
        appels = new PrevisionsReelSupport(mockMvc);
        UUID role = inserer("AdmRole", "Code, Description", "Modèles M7-" + suffixe, "Modèles M7");
        inserer("AdmRight", "Code, Description, AdmRoleId", DROIT_MODELES, DROIT_MODELES, role.toString());
        UUID utilisateur = inserer("AdmUser", "Code, Description, Disabled", "m7.audit." + suffixe, "Compte M7", false);
        inserer("AdmUserRole", "AdmUserId, AdmRoleId", utilisateur.toString(), role.toString());
        AdmUser principal = new AdmUser();
        principal.setId(utilisateur);
        principal.setCode("m7.audit." + suffixe);
        compte = FixtureDossierVente.connecte(principal, FixtureStatutsAchat.ROLE_JWT);
    }

    @AfterEach
    void nettoyer() {
        for (UUID id : modelesCrees) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id);
            effacer("DELETE FROM PrjMarketPrevisionModelLine WHERE PrjMarketPrevisionModelId = ?", id);
            effacer("DELETE FROM PrjMarketPrevisionModel WHERE PKey = ?", id);
        }
        while (!creees.isEmpty()) {
            String[] ligne = creees.pop();
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    /** Nettoyage au mieux : une FK de CI inattendue ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object parametre) {
        try {
            jdbc.update(sql, parametre.toString());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private UUID inserer(String table, String colonnes, Object... valeurs) {
        UUID id = UUID.randomUUID();
        Object[] parametres = new Object[valeurs.length + 1];
        parametres[0] = id.toString();
        System.arraycopy(valeurs, 0, parametres, 1, valeurs.length);
        String marques = String.join(",", java.util.Collections.nCopies(parametres.length, "?"));
        jdbc.update("INSERT INTO " + table + " (PKey, " + colonnes + ") VALUES (" + marques + ")", parametres);
        creees.push(new String[] {table, id.toString()});
        return id;
    }

    private String code() {
        return "M7-" + suffixe;
    }

    private CorpsJson corps(String description) {
        return saisieModele(code(), description, null, ZERO, FIN);
    }

    /** Crée le modèle par l'API ; le test s'arrête là si la route manque (aucune trace à attendre). */
    private UUID creer() throws Exception {
        Reponse r = appels.creer(compte, corps("Audit M7"));
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        UUID id = UUID.fromString(String.valueOf(r.valeur("$.id")));
        modelesCrees.add(id);
        return id;
    }

    private List<Map<String, Object>> traces(UUID objet, String action) {
        return jdbc.queryForList("SELECT Action, ObjectType, ObjectLabel FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?",
                objet.toString(), action);
    }

    @Test
    void m7_creation_uneTraceAdded() throws Exception {
        UUID id = creer();

        assertThat(traces(id, "Added")).singleElement().satisfies(t -> assertThat(t).containsEntry(OBJECT_TYPE, TYPE));
    }

    @Test
    void m7_modification_uneTraceModified() throws Exception {
        UUID id = creer();

        Reponse r = appels.modifier(compte, id, corps("Audit M7 modifié"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(traces(id, "Modified")).singleElement().satisfies(t -> assertThat(t).containsEntry(OBJECT_TYPE, TYPE));
    }

    @Test
    void m7_suppression_uneTraceDeleted() throws Exception {
        UUID id = creer();

        Reponse r = appels.supprimer(compte, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(traces(id, "Deleted")).singleElement().satisfies(t -> assertThat(t).containsEntry(OBJECT_TYPE, TYPE));
    }
}
