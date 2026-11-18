package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Cadre;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.AFFECTATIONS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DEBIT;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DECAISSEMENTS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.MODULE;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.PRJ;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.SENS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seconde revue N3 (règle 6, D4). Règlement R de la société S habilitée : une ligne sur la facture d'achat FA (projet
 * habilité), une ligne sur une pièce de recette {@code StlDocument} de la société T non habilitée. La ligne sur la pièce
 * de T ne doit fuir ni par Affectations ni par la fiche. Nécessite {@code StlDocument.CfgCompanyId} / {@code AdmModuleId}
 * (présents en CI et en prod, absents de yesRemsTest : échec local pour cette seule raison).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieSecondeRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerie fixture;
    @Autowired private FixtureAchat achat;
    @Autowired private EntityManager em;

    private AppelsTresorerie appels;
    private RequestPostProcessor a;
    private UUID reglement;
    private UUID ligneVisible;
    private UUID ligneSocieteT;

    @BeforeEach
    void setUp() {
        appels = new AppelsTresorerie(mockMvc);
        Cadre c = fixture.cadre("TSR");
        UUID societeT = achat.societe("SOCT-" + c.jeton());
        UUID fa = fixture.piece(c.code("-FA"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "1000");
        UUID pieceT = UUID.randomUUID();
        em.flush();
        em.createNativeQuery("INSERT INTO StlDocument (PKey, Code, DocType, CfgTierId, CfgCompanyId, AdmModuleId, "
                        + "AmountToPay) VALUES (:id, :code, 'INVOICE', :tiers, :societe, :module, 500)")
                .setParameter("id", pieceT).setParameter("code", c.code("-DT")).setParameter("tiers", c.fournisseur())
                .setParameter("societe", societeT).setParameter("module", c.modulePrj()).executeUpdate();

        reglement = fixture.reglement(Reglement.debit(c.code("-R"), c, "700"));
        ligneVisible = fixture.ligne(reglement, fa, "200");
        ligneSocieteT = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO StlSettlementLine (PKey, StlSettlementId, StlDocumentId, Amount) "
                        + "VALUES (:id, :reglement, :piece, :montant)")
                .setParameter("id", ligneSocieteT).setParameter("reglement", reglement).setParameter("piece", pieceT)
                .setParameter("montant", new BigDecimal("500")).executeUpdate();

        a = connecte(fixture.compte(c, "TSRA", FixtureTresorerie.DROIT_DECAISSEMENTS,
                FixtureTresorerie.DROIT_AFFECTATIONS), FixtureTresorerie.ROLE);
    }

    /** DÉFAUT D4 : la ligne vers une pièce de recette d'une société non habilitée apparaît dans Affectations. */
    @Test
    void affectations_lignePieceRecetteSocieteNonHabilitee_absente() throws Exception {
        List<String> ids = appels.ok(a, AFFECTATIONS, MODULE, PRJ, SENS, DEBIT).ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ids).contains(ligneVisible.toString());
            s.assertThat(ids).doesNotContain(ligneSocieteT.toString());
        });
    }

    /** DÉFAUT D4 : la fiche liste la ligne de la société T au lieu de la compter dans lignesMasquees. */
    @Test
    void fiche_lignePieceRecetteSocieteNonHabilitee_masquee() throws Exception {
        Reponse r = appels.lire(a, DECAISSEMENTS + "/" + reglement);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<String> lignes = JsonLignes.ids(r);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).containsExactly(ligneVisible.toString());
            s.assertThat(r.nombre("$.lignesMasquees")).isEqualByComparingTo("1");
            s.assertThat(r.nombre("$.decaissement.montantAffecte")).isEqualByComparingTo("700");
        });
    }

    private static final class JsonLignes {
        private JsonLignes() {
        }

        static List<String> ids(Reponse r) {
            return com.jayway.jsonpath.JsonPath.read(r.corps(), "$.lignes[*].id");
        }
    }
}
