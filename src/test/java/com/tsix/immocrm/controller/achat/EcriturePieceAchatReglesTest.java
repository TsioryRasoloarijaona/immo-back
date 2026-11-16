package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.buy.BuyDocumentStatusHistory;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Compte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import jakarta.persistence.Column;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.DETAIL;
import static com.tsix.immocrm.fixtures.ApiCommissions.document;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.STATUT_REGLE;
import static com.tsix.immocrm.fixtures.FixtureTerrains.VALIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Invariants de l'écrivain commun {@code EcriturePieceAchat} (FEATURE-achat-regle-commune.md §3) prouvés par les deux appelants :
 * R2 transaction obligatoire, R4 statut de départ, R6 lignes, R7 ventilation projet à 3 projets, R8 historique, R11 mapping.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcriturePieceAchatReglesTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2027, 2, 10, 11, 30);
    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final BigDecimal PRIX = new BigDecimal("775000");
    private static final BigDecimal CENT = new BigDecimal("100");
    private static final String ECRIVAIN = "com.tsix.immocrm.services.achat.ecriture.EcriturePieceAchat";
    private static final String SANS_DEPART_COMMISSION =
            "Aucun statut de départ dans le workflow des documents de commission.";
    private static final String PIECES_DU_TIERS = "SELECT * FROM BuyDocument WHERE CfgTierId = :id";
    private static final String NB_PIECES_DU_TIERS = "SELECT COUNT(*) FROM BuyDocument WHERE CfgTierId = :id";
    private static final String PIECES_DU_FRAIS = "SELECT * FROM BuyDocument WHERE StkExpenseId = :id";
    private static final String LIGNES = "SELECT * FROM BuyDocumentLine WHERE BuyDocumentId = :id";
    private static final String PROJETS = "SELECT * FROM BuyVentilationExpense WHERE BuyDocumentId = :id";
    private static final String HISTORIQUE = "SELECT * FROM BuyDocumentStatusHistory WHERE BuyDocumentId = :id";
    private static final String DOCUMENT_DE_LA_LIGNE = "SELECT BuyDocumentId FROM ComCommissionLine WHERE PKey = :id";
    private static final String MASQUER_WORKFLOW = """
            UPDATE BuyDocumentWorkFlow SET StrEntityId = NULL
            WHERE StrEntityId IN (SELECT PKey FROM StrEntity WHERE Code = N'%s')""";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return Clock.fixed(MAINTENANT.atZone(FuseauMetier.ZONE).toInstant(), FuseauMetier.ZONE);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationContext contexte;
    @Autowired private FixtureCommissions commissions;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureConfigurationAchat configuration;

    private Contexte c;
    private Compte compte;
    private UUID commercial;

    @BeforeEach
    void setUp() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return; // R2 tourne hors transaction : rien à écrire (et rien à annuler)
        }
        c = commissions.contexte("A0R");
        commissions.configurationDocument();
        compte = commissions.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU);
        commercial = commissions.personne("COM-" + c.jeton(), false);
        commissions.chantier(c.tranche(), "CH-" + c.jeton());
    }

    private UUID ligne(Dossier d, BigDecimal montant) {
        return commissions.ligne(d, CONCRETISATION, commercial, montant, AVANCE, null);
    }

    private ResultActions documenter(List<UUID> lignes) throws Exception {
        return mockMvc.perform(document(lignes).with(compte.qui()));
    }

    private Tuple pieceDuCommercial() {
        List<Tuple> pieces = commissions.lignes(PIECES_DU_TIERS, commercial);
        assertThat(pieces).hasSize(1);
        return pieces.get(0);
    }

    private static UUID id(Tuple t) {
        return UUID.fromString(guid(t.get("PKey")));
    }

    // ------------------------------------------------------------------ R4 statut de départ

    /**
     * R4, amendé par A2b-2a (réponse 2 du manager, 16/11) : gestion des commissions sans transition → statut NULL et 201,
     * comme un devis ; la séquence avance d'un numéro.
     */
    @Test
    void r4_commission_gestionSansWorkflow_201_statutNull() throws Exception {
        UUID l = ligne(commissions.dossier(c, "D1", commercial, PRIX, AVANCE), new BigDecimal("300"));
        terrains.executer(MASQUER_WORKFLOW.formatted("ComInvoiceDebitCommission"));

        documenter(List.of(l)).andExpect(status().isCreated());

        Tuple p = pieceDuCommercial();
        assertAll(
                () -> assertThat(p.get("BuyDocumentStatusId")).isNull(),
                () -> assertThat(commissions.valeurSequence()).isEqualTo(3014),
                () -> assertThat(commissions.lignes(DOCUMENT_DE_LA_LIGNE, l))
                        .singleElement().satisfies(t -> assertThat(guid(t.get(0))).isEqualTo(guid(p.get("PKey")))));
    }

    /**
     * R4 / D4, puis A2b-2a E-C4.3 : boucle entre deux statuts « Intermédiaire » → aucun départ ni par l'ancienne règle
     * ni par la règle OMRAN (aucun départ de type « Nouveau ») → 500.
     */
    @Test
    void r4_commission_workflowEnBoucle_aucunDepart_500_rienEcrit() throws Exception {
        UUID l = ligne(commissions.dossier(c, "D1", commercial, PRIX, AVANCE), new BigDecimal("300"));
        terrains.executer(MASQUER_WORKFLOW.formatted("ComInvoiceDebitCommission"));
        UUID gestion = commissions.entiteDocument();
        UUID valide = configuration.statut(new FixtureConfigurationAchat.Statut("VALIDE-" + c.jeton(), "Validé", 15,
                FixtureTerrains.ETAT_INTERMEDIAIRE, false, false, false, null, null));
        configuration.transition(gestion, 10, "Aller", valide, STATUT_REGLE);
        configuration.transition(gestion, 20, "Retour", STATUT_REGLE, valide);

        documenter(List.of(l)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath(DETAIL).value(SANS_DEPART_COMMISSION));

        assertAll(
                () -> assertThat(commissions.valeurSequence()).isEqualTo(3013),
                () -> assertThat(commissions.compter(NB_PIECES_DU_TIERS, commercial)).isZero());
    }

    /** R4, amendé par A2b-2a (réponse 2 du manager, 16/11) : frais terrain, gestion sans transition → 200, facture à statut NULL. */
    @Test
    void r4_fraisTerrain_gestionSansWorkflow_200_statutNull() throws Exception {
        AdmUser admin = patrimoine.administrateur();
        FixtureTerrains.Contexte t = terrains.contexte("A0RF");
        UUID terrain = terrains.terrain(t, "TER-" + t.jeton());
        FixtureTerrains.TypeFrais type = terrains.typeFrais(t.jeton(), true, true);
        UUID frais = terrains.frais(terrain, type.id(), terrains.fournisseur("FRN-" + t.jeton(), null),
                new BigDecimal("8000"), false);
        terrains.executer(MASQUER_WORKFLOW.formatted("StkInvoiceDebitLand"));

        mockMvc.perform(post(VALIDER, terrain, frais).with(connecte(admin))).andExpect(status().isOk());

        assertThat(terrains.lignes(PIECES_DU_FRAIS, frais))
                .singleElement().satisfies(p -> assertThat(p.get("BuyDocumentStatusId")).isNull());
    }

    // ------------------------------------------------------------------ R6 lignes

    /** R6 : LinesCount = nombre de lignes, Σ quantité × PU TTC = AmountTTC, Sorting 1..n sans trou. */
    @Test
    void r6_troisLignes_linesCount_sommeEgaleTTC_sortingSansTrou() throws Exception {
        List<UUID> lignes = List.of(
                ligne(commissions.dossier(c, "D1", commercial, PRIX, AVANCE), new BigDecimal("300.25")),
                ligne(commissions.dossier(c, "D2", commercial, PRIX, AVANCE), new BigDecimal("150.5")),
                ligne(commissions.dossier(c, "D3", commercial, PRIX, AVANCE), new BigDecimal("49.000001")));

        documenter(lignes).andExpect(status().isCreated());

        Tuple p = pieceDuCommercial();
        List<Tuple> l = commissions.lignes(LIGNES, id(p));
        BigDecimal somme = l.stream().map(x -> decimal(x.get("Quantity")).multiply(decimal(x.get("UnitPriceTTC"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertAll(
                () -> assertThat(((Number) p.get("LinesCount")).intValue()).isEqualTo(3),
                () -> assertThat(l).hasSize(3),
                () -> assertThat(somme).isEqualByComparingTo(decimal(p.get("AmountTTC"))),
                () -> assertThat(decimal(p.get("AmountTTC"))).isEqualByComparingTo("499.750001"),
                () -> assertThat(l.stream().map(x -> ((Number) x.get("Sorting")).intValue()).toList())
                        .containsExactlyInAnyOrder(1, 2, 3));
    }

    // ------------------------------------------------------------------ R7 ventilation projet

    /** R7 : 3 projets à parts égales → 0,333333 ×2 et le dernier complète à 0,333334 ; Σ = 1 exactement. */
    @Test
    void r7_troisProjetsUnTiers_dernierComplete_sommeUn() throws Exception {
        CfgTranche t2 = commissions.autreTranche(c, "T2", true);
        CfgTranche t3 = commissions.autreTranche(c, "T3", true);
        commissions.chantier(t2, "CH2-" + c.jeton());
        commissions.chantier(t3, "CH3-" + c.jeton());
        List<UUID> lignes = List.of(
                ligne(commissions.dossier(c, "D1", commercial, PRIX, AVANCE), CENT),
                ligne(commissions.dossier(c, t2, "D2", commercial, PRIX, AVANCE), CENT),
                ligne(commissions.dossier(c, t3, "D3", commercial, PRIX, AVANCE), CENT));

        documenter(lignes).andExpect(status().isCreated());

        List<BigDecimal> taux = commissions.lignes(PROJETS, id(pieceDuCommercial())).stream()
                .map(v -> decimal(v.get("VentilationRate"))).toList();
        assertAll(
                () -> assertThat(taux).hasSize(3)
                        .filteredOn(x -> x.compareTo(new BigDecimal("0.333333")) == 0).hasSize(2),
                () -> assertThat(taux).anySatisfy(x -> assertThat(x).isEqualByComparingTo("0.333334")),
                () -> assertThat(taux.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1"));
    }

    // ------------------------------------------------------------------ R8 historique

    /** R8 : une seule ligne d'historique, statut de la pièce, compte courant, Notes '', DocDate = UserCreateDateTime. */
    @Test
    void r8_historique_uneLigne_statutDeLaPiece_compteCourant() throws Exception {
        documenter(List.of(ligne(commissions.dossier(c, "D1", commercial, PRIX, AVANCE), CENT)))
                .andExpect(status().isCreated());

        Tuple p = pieceDuCommercial();
        assertThat(commissions.lignes(HISTORIQUE, id(p))).singleElement().satisfies(h -> assertAll(
                () -> assertThat(guid(h.get("BuyDocumentStatusId"))).isEqualTo(guid(p.get("BuyDocumentStatusId"))),
                () -> assertThat(guid(h.get("AdmUserId"))).isEqualTo(guid(compte.compte().getId())),
                () -> assertThat(h.get("Notes")).isEqualTo(""),
                () -> assertThat(FixtureTerrains.horodatage(h.get("DocDate")))
                        .isEqualTo(FixtureTerrains.horodatage(p.get("UserCreateDateTime"))),
                () -> assertThat(h.get("buyDocumentWorkFlowId")).isNull()));
    }

    // ------------------------------------------------------------------ R2 transaction obligatoire

    /** R2 : {@code EcriturePieceAchat.creer} est MANDATORY ; appelée hors transaction → IllegalTransactionStateException. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void r2_creerHorsTransaction_illegalTransactionState() throws Exception {
        Object ecrivain = contexte.getBean(Class.forName(ECRIVAIN));
        Method creer = Arrays.stream(ecrivain.getClass().getMethods())
                .filter(m -> m.getName().equals("creer") && m.getParameterCount() == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("creer(PieceAchatACreer) absente de " + ECRIVAIN));

        Throwable erreur = catchThrowable(() -> creer.invoke(ecrivain, (Object) null));

        assertThat(erreur).isInstanceOf(InvocationTargetException.class)
                .cause().isInstanceOf(IllegalTransactionStateException.class);
    }

    // ------------------------------------------------------------------ R11 mapping

    /** R11 : l'entité d'historique mappe buyDocumentWorkFlowId (UUID, nom exact à minuscule initiale). */
    @Test
    void r11_historique_mappeBuyDocumentWorkFlowId() {
        List<Field> champs = Arrays.stream(BuyDocumentStatusHistory.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class)
                        && f.getAnnotation(Column.class).name().equals("buyDocumentWorkFlowId"))
                .toList();

        assertThat(champs).singleElement().satisfies(f -> assertThat(f.getType()).isEqualTo(UUID.class));
    }
}
