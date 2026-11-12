package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.supprimer;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.transition;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-1 en transactions réelles : E-B7 (trace {@code AdmWebHistorics} écrite à la validation : {@code Modified} pour une
 * transition, {@code Deleted} pour une suppression, type par nature) et Vérification n° 4 (suppression atomique : rollback
 * prouvé par un déclencheur {@code THROW} qui ne vise que les pièces dont le code porte {@value #MARQUEUR}).
 * Sans {@code @Transactional} : tout est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PieceAchatAuditAtomiciteTest {

    private static final Logger log = LoggerFactory.getLogger(PieceAchatAuditAtomiciteTest.class);

    private static final String MARQUEUR = "PANNE-A2B1";
    private static final String TRIGGER = "TRG_TEST_A2B1_PANNE_SUPPRESSION";
    private static final String CREER_TRIGGER = """
            CREATE OR ALTER TRIGGER dbo.TRG_TEST_A2B1_PANNE_SUPPRESSION ON dbo.BuyDocument AFTER DELETE AS
            BEGIN
                SET NOCOUNT ON;
                IF EXISTS (SELECT 1 FROM deleted WHERE Code LIKE N'%PANNE-A2B1%')
                    THROW 50001, N'Panne de test a la suppression de la piece', 1;
            END""";
    private static final String TRACES = """
            SELECT ObjectType, ObjectTypeLabel, Action, ObjectLabel FROM AdmWebHistorics WHERE ObjectId = ?""";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final String OBJECT_TYPE_LABEL = "ObjectTypeLabel";
    private static final String ACTION = "Action";
    private static final String OBJECT_LABEL = "ObjectLabel";
    private static final String PIECE_PAR_ID = "SELECT COUNT(*) FROM BuyDocument WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureConfigurationAchat config;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private Cadre c;
    private AdmUser compte;
    private UUID employe;
    private UUID role;
    private UUID gestion;
    private UUID s0;
    private UUID s1;
    private UUID transitionS0S1;
    private final List<UUID> pieces = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(t -> {
            c = fixture.cadre("AAT");
            compte = fixture.compteHabilite(c, "AAT", ADMINISTRATEUR);
            employe = compte.getCfgEmployee() == null ? null : compte.getCfgEmployee().getId();
            role = fixture.roleDe(compte);
        });
        AuthenticatedUser principal = new AuthenticatedUser(compte.getId(), compte.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        transactions.executeWithoutResult(t -> {
            fixture.tablesComptables();
            gestion = config.gestion("G-" + c.jeton(), "Gestion " + c.jeton());
            s0 = config.statut(Statut.simple("S0-" + c.jeton(), 1));
            s1 = config.statut(Statut.simple("S1-" + c.jeton(), 2));
            transitionS0S1 = config.transition(gestion, 1, "Valider", s0, s1);
            config.lier(transitionS0S1, role);
        });
        SecurityContextHolder.clearContext();
    }

    /** Pièce validée en base, en gestion G au statut S0, une ligne et un historique. */
    private UUID piece(String suffixe, String nature) {
        UUID id = transactions.execute(t -> {
            UUID p = fixture.piece(c, suffixe, nature);
            fixture.poser(p, "StrEntityId", gestion);
            fixture.poser(p, "BuyDocumentStatusId", s0);
            fixture.ligne(p, 1, "Ciment", "SAC", BigDecimal.TEN, new BigDecimal("80"), new BigDecimal("0.2"));
            fixture.historique(p, s0, LocalDateTime.of(2026, 5, 4, 9, 0), compte, "Création");
            return p;
        });
        pieces.add(id);
        return id;
    }

    private String code(UUID piece) {
        return jdbc.queryForObject("SELECT Code FROM BuyDocument WHERE PKey = ?", String.class, piece.toString());
    }

    private MvcResult appeler(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete.with(connecte(compte, ROLE_JWT))).andReturn();
    }

    private int compter(String sql, UUID id) {
        Integer n = jdbc.queryForObject(sql, Integer.class, id.toString());
        return n == null ? 0 : n;
    }

    private List<Map<String, Object>> traces(UUID piece) {
        return jdbc.queryForList(TRACES, piece.toString());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        effacer("DROP TRIGGER IF EXISTS dbo." + TRIGGER);
        for (UUID piece : pieces) {
            String id = piece.toString();
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id);
            for (String enfant : List.of("BuyDocumentStatusHistory", "BuyVentilationCategory", "BuyDocumentLine")) {
                effacer("DELETE FROM " + enfant + " WHERE BuyDocumentId = ?", id);
            }
            effacer("DELETE FROM BuyDocument WHERE PKey = ?", id);
        }
        String compteId = compte.getId().toString();
        effacer("DELETE FROM BuyDocumentWorkFlowAdmRole WHERE WorkFlowId = ?", transitionS0S1.toString());
        effacer("DELETE FROM AdmRight WHERE AdmRoleId = ?", role.toString());
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", compteId);
        effacer("DELETE FROM AdmUserRole WHERE AdmUserId = ?", compteId);
        List<String[]> lignes = new ArrayList<>(List.of(new String[]{"BuyDocumentWorkFlow", transitionS0S1.toString()},
                new String[]{"BuyDocumentStatus", s1.toString()},
                new String[]{"BuyDocumentStatus", s0.toString()},
                new String[]{"StrEntity", gestion.toString()},
                new String[]{"AdmRole", role.toString()},
                new String[]{"AdmUser", compteId},
                new String[]{"PrjProject", c.projet().toString()},
                new String[]{"CfgCompany", c.societe().toString()},
                new String[]{"CfgTier", c.fournisseur().toString()}));
        if (employe != null) {
            lignes.add(new String[]{"CfgTier", employe.toString()});
        }
        for (String[] ligne : lignes) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", ligne[1], ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    /** Nettoyage au mieux : une ligne déjà absente ou une table manquante ne fait pas échouer un test jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ E-B7 audit

    /** E-B7 : une transition d'un B.C. est tracée Modified, type « Yes.Buy.Order » / « Bon de Commande », libellé = code. */
    @Test
    void eb7_transitionBonCommande_traceModified_typeOrder() throws Exception {
        UUID bc = piece("-BC", FixtureAchat.BON_COMMANDE);

        MvcResult r = appeler(transition(bc, s1, null));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        String code = code(bc);
        assertThat(traces(bc)).isNotEmpty().anySatisfy(l -> assertThat(l)
                .containsEntry(OBJECT_TYPE, "Yes.Buy.Order")
                .containsEntry(OBJECT_TYPE_LABEL, "Bon de Commande")
                .containsEntry(ACTION, "Modified")
                .containsEntry(OBJECT_LABEL, code));
    }

    /** E-B7 : la suppression d'un devis est tracée Deleted, type « Yes.Buy.Quote » / « Devis », libellé = code. */
    @Test
    void eb7_suppressionDevis_traceDeleted_typeQuote() throws Exception {
        UUID devis = piece("-DV", FixtureAchat.DEVIS);
        String code = code(devis);

        MvcResult r = appeler(supprimer(devis));

        assertThat(statut(r)).as(corps(r)).isEqualTo(204);
        assertThat(traces(devis)).isNotEmpty().anySatisfy(l -> assertThat(l)
                .containsEntry(OBJECT_TYPE, "Yes.Buy.Quote")
                .containsEntry(OBJECT_TYPE_LABEL, "Devis")
                .containsEntry(ACTION, "Deleted")
                .containsEntry(OBJECT_LABEL, code));
    }

    // ------------------------------------------------------------------ Vérification 4 : rollback

    /**
     * Vérification 4 : panne au DELETE de la pièce (après celui de ses lignes et de son historique) → jamais 2xx, la pièce,
     * ses lignes, son historique restent, aucune trace Deleted. Témoin : la même route supprime une pièce sans marqueur.
     */
    @Test
    void v4_panneALaSuppression_rollbackComplet() throws Exception {
        UUID temoin = piece("-DT", FixtureAchat.DEVIS);
        UUID panne = piece("-" + MARQUEUR, FixtureAchat.DEVIS);
        MvcResult t = appeler(supprimer(temoin));
        assertThat(statut(t)).as("témoin : " + corps(t)).isEqualTo(204);
        jdbc.execute(CREER_TRIGGER);

        MvcResult r = appeler(supprimer(panne));

        assertAll(
                () -> assertThat(statut(r)).as("la panne ne peut pas répondre 2xx").isGreaterThanOrEqualTo(400),
                () -> assertThat(compter(PIECE_PAR_ID, panne)).as("pièce").isEqualTo(1),
                () -> assertThat(compter("SELECT COUNT(*) FROM BuyDocumentLine WHERE BuyDocumentId = ?", panne))
                        .as("lignes").isEqualTo(1),
                () -> assertThat(compter("SELECT COUNT(*) FROM BuyDocumentStatusHistory WHERE BuyDocumentId = ?", panne))
                        .as("historique").isEqualTo(1),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND Action = N'Deleted'",
                        panne)).as("trace Deleted").isZero());
    }
}
