package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NatureSaisie;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.idCree;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.ligne;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.modifier;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2a E-C7.3 et Vérification n° 4 / 7 (FEATURE-achat-fiche-creation.md) en transactions réelles : {@code TracesAchat} trace
 * {@code Modified} au {@code PUT} (type par nature) et {@code Added} à la création ; modification atomique prouvée par un
 * déclencheur {@code THROW} sur l'insertion d'une ligne dont la désignation porte {@value #MARQUEUR}.
 * Sans {@code @Transactional} : la configuration partagée touchée est restaurée en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PieceAchatSaisieAuditTest {

    private static final Logger log = LoggerFactory.getLogger(PieceAchatSaisieAuditTest.class);

    private static final String MARQUEUR = "PANNE-A2B2A";
    private static final String TRIGGER = "TRG_TEST_A2B2A_PANNE_LIGNE";
    private static final String CREER_TRIGGER = """
            CREATE OR ALTER TRIGGER dbo.TRG_TEST_A2B2A_PANNE_LIGNE ON dbo.BuyDocumentLine AFTER INSERT AS
            BEGIN
                SET NOCOUNT ON;
                IF EXISTS (SELECT 1 FROM inserted WHERE Description LIKE N'%PANNE-A2B2A%')
                    THROW 50001, N'Panne de test a l insertion d une ligne', 1;
            END""";
    private static final String TRACES = """
            SELECT ObjectType, ObjectTypeLabel, Action, ObjectLabel FROM AdmWebHistorics WHERE ObjectId = ?""";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final String OBJECT_TYPE_LABEL = "ObjectTypeLabel";
    private static final String ACTION = "Action";
    private static final String OBJECT_LABEL = "ObjectLabel";
    private static final String MODIFIED = "Modified";
    private static final String LIGNES = "SELECT COUNT(*) FROM BuyDocumentLine WHERE BuyDocumentId = ?";
    private static final String CODE_DEVIS = "Devis entrant";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private Cadre c;
    private Saisie s;
    private AdmUser compte;
    private UUID employe;
    private UUID role;
    private final List<UUID> pieces = new ArrayList<>();
    private final List<Runnable> restaurations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        boolean moduleAbsent = compterCode("SELECT COUNT(*) FROM AdmModule WHERE Code = ?", "BUY") == 0;
        transactions.executeWithoutResult(t -> {
            c = fixture.cadre("ASA");
            compte = fixture.compteHabilite(c, "ASA", ADMINISTRATEUR);
            employe = compte.getCfgEmployee() == null ? null : compte.getCfgEmployee().getId();
            role = fixture.roleDe(compte);
            fixture.sansPersonnel(compte);
            s = new Saisie(c.jeton(), null, null, null, fixture.module("BUY"), null, null,
                    fixture.typeLot("TM-" + c.jeton(), "Travaux " + c.jeton()), fixture.categorie("CAT-" + c.jeton()),
                    Map.of());
        });
        if (moduleAbsent) {
            restaurations.add(() -> effacer("DELETE FROM AdmModule WHERE Code = ?", "BUY"));
        }
    }

    private int compterCode(String sql, String code) {
        Integer n = jdbc.queryForObject(sql, Integer.class, code);
        return n == null ? 0 : n;
    }

    private int compter(String sql, UUID id) {
        Integer n = jdbc.queryForObject(sql, Integer.class, id.toString());
        return n == null ? 0 : n;
    }

    /** Pièce validée en base (sans gestion ni statut), une ligne « Ciment ». */
    private UUID piece(String suffixe, String nature) {
        UUID id = transactions.execute(t -> {
            UUID p = fixture.piece(c, suffixe, nature);
            fixture.ligne(p, 1, "Ciment", "SAC", BigDecimal.TEN, new BigDecimal("80"), new BigDecimal("0.2"));
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

    private List<Map<String, Object>> traces(UUID piece) {
        return jdbc.queryForList(TRACES, piece.toString());
    }

    /** Gestion {@code BuyQuote} et séquence « Devis entrant » pour une création de devis ; restaurées après le test. */
    private void configurationDevis() {
        if (compterCode("SELECT COUNT(*) FROM StrEntity WHERE Code = ?", FixtureAchat.GESTION_DEVIS) == 0) {
            UUID gestion = UUID.fromString("A168074F-113A-4AAC-BD9E-41AAB4C82CEB");
            jdbc.update("INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)", gestion.toString(),
                    FixtureAchat.GESTION_DEVIS, "Devis");
            restaurations.add(() -> effacer("DELETE FROM StrEntity WHERE PKey = ?", gestion.toString()));
        }
        List<Integer> valeur = jdbc.queryForList("SELECT SeqValue FROM AdmSequence WHERE Code = ?", Integer.class, CODE_DEVIS);
        if (valeur.isEmpty()) {
            jdbc.update("""
                    INSERT INTO AdmSequence (PKey, Code, SeqYear, SeqYearNumber, SeqValue, SeqValueNumber, Separator,
                        BasedOnSystemYear, CfgCompanyId, SeqPrefix, SeqSuffix)
                    VALUES (?, ?, 2021, 2, 0, 5, N'/', 0, NULL, N'', N'')""", UUID.randomUUID().toString(), CODE_DEVIS);
            restaurations.add(() -> effacer("DELETE FROM AdmSequence WHERE Code = ?", CODE_DEVIS));
        } else {
            Integer avant = valeur.get(0);
            restaurations.add(() -> effacer("UPDATE AdmSequence SET SeqValue = ? WHERE Code = ?", avant, CODE_DEVIS));
        }
        if (compterCode("SELECT COUNT(*) FROM AdmParamsGuid WHERE Code = ?", "DefaultCurrency") == 0) {
            UUID devise = transactions.execute(t -> fixture.devise("DEV-" + c.jeton()));
            jdbc.update("INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (?, ?, ?)", "DefaultCurrency",
                    devise.toString(), UUID.randomUUID().toString());
            restaurations.add(() -> effacer("DELETE FROM StlCurrency WHERE PKey = ?", devise.toString()));
            restaurations.add(() -> effacer("DELETE FROM AdmParamsGuid WHERE Code = ?", "DefaultCurrency"));
        }
    }

    @AfterEach
    void nettoyer() {
        effacer("DROP TRIGGER IF EXISTS dbo." + TRIGGER);
        for (UUID piece : pieces) {
            String id = piece.toString();
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id);
            for (String enfant : List.of("BuyDocumentStatusHistory", "BuyVentilationCategory", "BuyVentilationExpense",
                    "BuyDocumentLine")) {
                effacer("DELETE FROM " + enfant + " WHERE BuyDocumentId = ?", id);
            }
            effacer("DELETE FROM BuyDocument WHERE PKey = ?", id);
        }
        Collections.reverse(restaurations);
        restaurations.forEach(Runnable::run);
        String compteId = compte.getId().toString();
        effacer("DELETE FROM AdmRight WHERE AdmRoleId = ?", role.toString());
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", compteId);
        effacer("DELETE FROM AdmUserRole WHERE AdmUserId = ?", compteId);
        List<String[]> lignes = new ArrayList<>(List.of(
                new String[]{"StlCategory", s.categorie().toString()},
                new String[]{"PrjMarketType", s.typeMarche().toString()},
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

    // ------------------------------------------------------------------ E-C7.3 audit

    /** E-C7.3 : la modification est tracée Modified, type et libellé de type par nature (A2b-1), libellé = code. */
    @ParameterizedTest
    @CsvSource({
            "DEVIS, Yes.Buy.Quote, Devis",
            "BC, Yes.Buy.Order, Bon de Commande",
            "BL, Yes.Buy.Delivery, Bon de Livraison",
            "FACTURE, Yes.Stl.Debit.Invoice, Facture"})
    void ec73_modification_traceModified_typeParNature(NatureSaisie n, String type, String libelle) throws Exception {
        UUID piece = piece("-" + n.name(), n.code);
        String code = code(piece);

        MvcResult r = appeler(modifier(piece, corpsValide(c, s, n.code).avec("notes", "modifiée")));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(traces(piece)).isNotEmpty().anySatisfy(l -> assertThat(l)
                .containsEntry(OBJECT_TYPE, type)
                .containsEntry(OBJECT_TYPE_LABEL, libelle)
                .containsEntry(ACTION, MODIFIED)
                .containsEntry(OBJECT_LABEL, code));
    }

    /** E-C7.3 : la création d'un devis est tracée Added, type « Yes.Buy.Quote » / « Devis », libellé = code. */
    @Test
    void ec73_creationDevis_traceAdded() throws Exception {
        configurationDevis();

        MvcResult r = appeler(creer(corpsValide(c, s, FixtureAchat.DEVIS)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        UUID devis = idCree(r);
        pieces.add(devis);
        String code = code(devis);
        assertThat(traces(devis)).isNotEmpty().anySatisfy(l -> assertThat(l)
                .containsEntry(OBJECT_TYPE, "Yes.Buy.Quote")
                .containsEntry(OBJECT_TYPE_LABEL, "Devis")
                .containsEntry(ACTION, "Added")
                .containsEntry(OBJECT_LABEL, code));
    }

    // ------------------------------------------------------------------ Vérification 4 : rollback

    /**
     * Vérification 4 : panne à l'insertion d'une ligne (après l'en-tête et la suppression des anciennes lignes) → jamais 2xx,
     * en-tête et lignes d'origine intacts, aucune trace Modified. Témoin : le même {@code PUT} sans marqueur → 200.
     */
    @Test
    void v4_panneALaModification_rollbackComplet() throws Exception {
        UUID temoin = piece("-TEM", FixtureAchat.BON_COMMANDE);
        UUID panne = piece("-PAN", FixtureAchat.BON_COMMANDE);
        MvcResult t = appeler(modifier(temoin, corpsValide(c, s, FixtureAchat.BON_COMMANDE)));
        assertThat(statut(t)).as("témoin : " + corps(t)).isEqualTo(200);
        jdbc.execute(CREER_TRIGGER);
        CorpsJson ligneEnPanne = ligne("ART-P", "1", "100", s.categorie()).avec("description", "Ligne " + MARQUEUR);

        MvcResult r = appeler(modifier(panne, corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("notes", "modifiée")
                .avec("lignes", List.of(ligneEnPanne))));

        assertAll(
                () -> assertThat(statut(r)).as("la panne ne peut pas répondre 2xx").isGreaterThanOrEqualTo(400),
                () -> assertThat(jdbc.queryForObject("SELECT Notes FROM BuyDocument WHERE PKey = ?", String.class,
                        panne.toString())).as("en-tête").isNull(),
                () -> assertThat(compter(LIGNES, panne)).as("ligne d'origine").isEqualTo(1),
                () -> assertThat(compter("SELECT COUNT(*) FROM BuyDocumentLine WHERE BuyDocumentId = ? AND Description = N'Ciment'",
                        panne)).as("ligne d'origine").isEqualTo(1),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND Action = N'Modified'",
                        panne)).as("trace Modified").isZero());
    }
}
