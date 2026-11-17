package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
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
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.saisieModele;
import static com.tsix.immocrm.fixtures.FixtureModelesSimulation.DROIT_MODELES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue L3a-ter, M7 : une modification du modèle qui ne touche que ses lignes (code, description, catégorie inchangés)
 * reste une modification de modèle → une trace {@code Modified}.
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class ModelesSimulationAuditRevueTest {

    private static final Ligne ZERO = new Ligne("0", "0", "0");
    private static final Ligne MILIEU = new Ligne("0.5", "0.5", "0.5");
    private static final Ligne FIN = new Ligne("1", "1", "1");
    private static final Ligne FIN_MOITIE = new Ligne("1", "1", "0.5");

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
        UUID role = inserer("AdmRole", "Code, Description", "Modèles R7-" + suffixe, "Modèles R7");
        inserer("AdmRight", "Code, Description, AdmRoleId", DROIT_MODELES, DROIT_MODELES, role.toString());
        UUID utilisateur = inserer("AdmUser", "Code, Description, Disabled", "r7.audit." + suffixe, "Compte R7", false);
        inserer("AdmUserRole", "AdmUserId, AdmRoleId", utilisateur.toString(), role.toString());
        AdmUser principal = new AdmUser();
        principal.setId(utilisateur);
        principal.setCode("r7.audit." + suffixe);
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
        String marques = String.join(",", Collections.nCopies(parametres.length, "?"));
        jdbc.update("INSERT INTO " + table + " (PKey, " + colonnes + ") VALUES (" + marques + ")", parametres);
        creees.push(new String[] {table, id.toString()});
        return id;
    }

    @Test
    void m7_modificationDesSeulesLignes_uneTraceModified() throws Exception {
        Reponse cree = appels.creer(compte, saisieModele("R7-" + suffixe, "Audit R7", null, ZERO, FIN));
        assertThat(cree.statut()).as(cree.corps()).isEqualTo(201);
        UUID id = UUID.fromString(String.valueOf(cree.valeur("$.id")));
        modelesCrees.add(id);

        Reponse r = appels.modifier(compte, id, saisieModele("R7-" + suffixe, "Audit R7", null, ZERO, MILIEU, FIN_MOITIE));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Integer traces = jdbc.queryForObject("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND Action = 'Modified'",
                Integer.class, id.toString());
        assertThat(traces).as("lignes modifiées, en-tête identique").isEqualTo(1);
    }
}
