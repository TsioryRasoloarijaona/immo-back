package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.CATEGORIE_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.GENRE;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.PROJET_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.TYPE_ID;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.GENRES;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.SEQUENCE_MARCHE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.TYPE_STATUT_NOUVEAU;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit {@code TracesLots} — FEATURE-lots-liste-parite.md E5, E6 et Vérification 10 : création et suppression écrivent une
 * ligne {@code AdmWebHistorics} du lot. Sans {@code @Transactional} : l'audit n'est écrit qu'à la validation. Tout ce qui est
 * validé est effacé en {@code @AfterEach} ; la séquence 'Market' retrouve sa valeur.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotsAuditTest {

    private static final Logger log = LoggerFactory.getLogger(LotsAuditTest.class);
    private static final String AUDIT = "SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;

    private final String jeton = FixtureProjetLots.jeton();
    private final List<UUID> lotsCrees = new ArrayList<>();
    private final List<Runnable> nettoyages = new ArrayList<>();

    private JdbcTemplate jdbc;
    private LotsPariteSupport appels;
    private RequestPostProcessor admin;
    private UUID projet;
    private UUID categorie;
    private UUID type;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        appels = new LotsPariteSupport(mockMvc);
        UUID genre = GENRES.get(ENTREPRENEURS);
        siAbsent("StrEntity", genre, "INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'PrjMarket', N'Lot Entrepreneurs')");
        siAbsent("StrStatusType", TYPE_STATUT_NOUVEAU,
                "INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES (?, N'Nouveau', N'Nouveau', 0)");
        inserer("StrStatus", """
                INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId)
                VALUES (?, ?, ?, 1, ?, ?)""", "AUD-" + jeton, "AUD-" + jeton, genre.toString(), TYPE_STATUT_NOUVEAU.toString());
        projet = inserer("PrjProject", "INSERT INTO PrjProject (PKey, Code, Description) VALUES (?, ?, ?)",
                "AUP-" + jeton, "AUP-" + jeton);
        categorie = inserer("StlCategory", """
                INSERT INTO StlCategory (PKey, Code, Description, IsCredit, IsCharge, IsProduct) VALUES (?, ?, ?, 0, 1, 1)""",
                "AUC-" + jeton, "AUC-" + jeton);
        type = inserer("PrjMarketType", "INSERT INTO PrjMarketType (PKey, Code, Description) VALUES (?, ?, ?)",
                "AUT-" + jeton, "AUT-" + jeton);
        sequence();
        admin = connecte(administrateur());
    }

    private UUID inserer(String table, String sql, Object... autres) {
        UUID id = UUID.randomUUID();
        Object[] params = new Object[autres.length + 1];
        params[0] = id.toString();
        System.arraycopy(autres, 0, params, 1, autres.length);
        jdbc.update(sql, params);
        nettoyages.add(0, () -> effacer("DELETE FROM " + table + " WHERE PKey = ?", id));
        return id;
    }

    private void siAbsent(String table, UUID id, String insertion) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE PKey = ?", Integer.class, id.toString());
        if (n != null && n == 0) {
            jdbc.update(insertion, id.toString());
            nettoyages.add(0, () -> effacer("DELETE FROM " + table + " WHERE PKey = ?", id));
        }
    }

    private void sequence() {
        List<Integer> avant = jdbc.queryForList("SELECT SeqValue FROM AdmSequence WHERE Code = ?", Integer.class, SEQUENCE_MARCHE);
        if (avant.isEmpty()) {
            inserer("AdmSequence", """
                    INSERT INTO AdmSequence (PKey, Code, SeqValue, SeqYearNumber, SeqValueNumber, Separator, BasedOnSystemYear)
                    VALUES (?, ?, 70000, 2, 5, N'/', 1)""", SEQUENCE_MARCHE);
        } else {
            Integer valeur = avant.get(0);
            nettoyages.add(0, () -> {
                try {
                    jdbc.update("UPDATE AdmSequence SET SeqValue = ? WHERE Code = ?", valeur, SEQUENCE_MARCHE);
                } catch (DataAccessException e) {
                    log.warn("Séquence non rétablie : {}", e.getMessage());
                }
            });
        }
    }

    private AdmUser administrateur() {
        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        compte.setCode("lots.aud." + jeton);
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (?, ?, ?, 0)",
                compte.getId().toString(), compte.getCode(), "Compte audit lots");
        nettoyages.add(0, () -> effacer("DELETE FROM AdmUser WHERE PKey = ?", compte.getId()));
        UUID role = inserer("AdmRole", """
                INSERT INTO AdmRole (PKey, Code, Description, WithSequenceFreeRestriction) VALUES (?, N'Administrateur', ?, 0)""",
                "Administrateur audit " + jeton);
        inserer("AdmUserRole", "INSERT INTO AdmUserRole (PKey, AdmUserId, AdmRoleId) VALUES (?, ?, ?)",
                compte.getId().toString(), role.toString());
        inserer("AdmUserHabilitation", "INSERT INTO AdmUserHabilitation (PKey, AdmUserId, PrjProjectId) VALUES (?, ?, ?)",
                compte.getId().toString(), projet.toString());
        return compte;
    }

    @AfterEach
    void nettoyer() {
        lotsCrees.forEach(id -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id);
            effacer("DELETE FROM StrHistory WHERE ObjectId = ?", id);
            effacer("DELETE FROM PrjMarket WHERE PKey = ?", id);
        });
        nettoyages.forEach(Runnable::run);
    }

    /** Nettoyage au mieux : une table annexe (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        Object[] textes = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            textes[i] = parametres[i].toString();
        }
        try {
            jdbc.update(sql, textes);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private UUID creer() throws Exception {
        Creation c = appels.creer(admin, corps().avec(GENRE, ENTREPRENEURS).avec(DESCRIPTION, "Lot audit " + jeton)
                .avec(PROJET_ID, projet).avec(CATEGORIE_ID, categorie).avec(TYPE_ID, type));
        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        UUID id = c.id();
        lotsCrees.add(id);
        return id;
    }

    private int audit(UUID lot, String action) {
        Integer n = jdbc.queryForObject(AUDIT, Integer.class, lot.toString(), action);
        return n == null ? 0 : n;
    }

    @Test
    void e5_creation_ligneDAuditAdded() throws Exception {
        UUID lot = creer();

        assertThat(audit(lot, "Added")).isPositive();
    }

    @Test
    void e6_suppression_ligneDAuditDeleted() throws Exception {
        UUID lot = creer();

        Reponse r = appels.supprimer(admin, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(audit(lot, "Deleted")).isPositive();
    }
}
