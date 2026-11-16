package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Compte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.document;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.STATUT_EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTerrains.STATUT_REGLE;
import static com.tsix.immocrm.fixtures.FixtureTerrains.VALIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A2b-2a E-C4.3 / E-C7.1 (FEATURE-achat-fiche-creation.md, Vérification n° 5) : la règle OMRAN du statut de départ
 * (1re transition par {@code Sorting} dont le départ est de type « Nouveau ») et l'écrivain généralisé donnent aux
 * commissions et aux frais de terrain le même statut (EN COURS) et les mêmes lignes qu'avant (U × 1, TVA 0).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcriturePieceAchatEquivalenceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2027, 2, 10, 11, 30);
    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final BigDecimal PRIX = new BigDecimal("775000");
    private static final String PIECES_DU_TIERS = "SELECT * FROM BuyDocument WHERE CfgTierId = :id";
    private static final String FACTURE_DU_FRAIS = "SELECT * FROM BuyDocument WHERE StkExpenseId = :id";
    private static final String LIGNES = "SELECT * FROM BuyDocumentLine WHERE BuyDocumentId = :id ORDER BY Sorting ASC";
    private static final String MASQUER_WORKFLOW = """
            UPDATE BuyDocumentWorkFlow SET StrEntityId = NULL
            WHERE StrEntityId IN (SELECT PKey FROM StrEntity WHERE Code = N'%s')""";
    private static final String STATUT = "BuyDocumentStatusId";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return Clock.fixed(MAINTENANT.atZone(FuseauMetier.ZONE).toInstant(), FuseauMetier.ZONE);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions commissions;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureConfigurationAchat configuration;

    private Contexte c;
    private Compte compte;
    private UUID commercial;

    @BeforeEach
    void setUp() {
        c = commissions.contexte("A2E");
        commissions.configurationDocument();
        compte = commissions.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU);
        commercial = commissions.personne("COM-" + c.jeton(), false);
        commissions.chantier(c.tranche(), "CH-" + c.jeton());
    }

    private UUID ligne(String dossier, BigDecimal montant) {
        return commissions.ligne(commissions.dossier(c, dossier, commercial, PRIX, AVANCE), CONCRETISATION, commercial,
                montant, AVANCE, null);
    }

    private Tuple pieceDuCommercial() {
        List<Tuple> pieces = commissions.lignes(PIECES_DU_TIERS, commercial);
        assertThat(pieces).hasSize(1);
        return pieces.get(0);
    }

    private static UUID id(Tuple t) {
        return UUID.fromString(guid(t.get("PKey")));
    }

    /** Colonnes de ligne écrites avant A2b-2a pour un montant : U × 1, PU HT = PU TTC = montant, taux nuls. */
    private static void assertLigneCommeAvant(Tuple l, String montant) {
        assertAll(
                () -> assertThat(l.get("Unity")).isEqualTo("U"),
                () -> assertThat(decimal(l.get("Quantity"))).isEqualByComparingTo("1"),
                () -> assertThat(decimal(l.get("UnitPriceHT"))).isEqualByComparingTo(montant),
                () -> assertThat(decimal(l.get("UnitPriceTTC"))).isEqualByComparingTo(montant),
                () -> assertThat(decimal(l.get("VatRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(l.get("DiscountRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(l.get("FODECRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(l.get("Coefficient"))).isEqualByComparingTo("1"),
                () -> assertThat(decimal(l.get("QuantityOld"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(l.get("QuantityOrigin"))).isEqualByComparingTo("0"));
    }

    // E-C4.3 + E-C7.1 : commission — statut EN COURS (départ « Nouveau »), lignes et montants comme avant.
    @Test
    void ec43_commission_statutEnCours_lignesCommeAvant() throws Exception {
        mockMvc.perform(document(List.of(ligne("D1", new BigDecimal("300.25")), ligne("D2", new BigDecimal("150.5"))))
                .with(compte.qui())).andExpect(status().isCreated());

        Tuple p = pieceDuCommercial();
        List<Tuple> lignes = commissions.lignes(LIGNES, id(p));
        assertAll(
                () -> assertThat(guid(p.get(STATUT))).isEqualTo(guid(STATUT_EN_COURS)),
                () -> assertThat(decimal(p.get("AmountHT"))).isEqualByComparingTo("450.75"),
                () -> assertThat(decimal(p.get("AmountVat"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(p.get("AmountTTC"))).isEqualByComparingTo("450.75"),
                () -> assertThat(decimal(p.get("AmountToPay"))).isEqualByComparingTo("450.75"),
                () -> assertThat(lignes).hasSize(2));
        assertLigneCommeAvant(lignes.get(0), "300.25");
        assertLigneCommeAvant(lignes.get(1), "150.5");
    }

    // E-C4.3 + E-C7.1 : frais de terrain — statut EN COURS, une ligne U × 1 au montant du frais.
    @Test
    void ec43_fraisTerrain_statutEnCours_ligneCommeAvant() throws Exception {
        AdmUser admin = patrimoine.administrateur();
        FixtureTerrains.Contexte t = terrains.contexte("A2EF");
        terrains.configurationFacture();
        UUID terrain = terrains.terrain(t, "TER-" + t.jeton());
        FixtureTerrains.TypeFrais type = terrains.typeFrais(t.jeton(), true, true);
        UUID frais = terrains.frais(terrain, type.id(), terrains.fournisseur("FRN-" + t.jeton(), null),
                new BigDecimal("8000.123456"), false);

        mockMvc.perform(post(VALIDER, terrain, frais).with(connecte(admin))).andExpect(status().isOk());

        Tuple p = terrains.ligne(FACTURE_DU_FRAIS, frais);
        List<Tuple> lignes = terrains.lignes(LIGNES, id(p));
        assertAll(
                () -> assertThat(guid(p.get(STATUT))).isEqualTo(guid(STATUT_EN_COURS)),
                () -> assertThat(decimal(p.get("AmountTTC"))).isEqualByComparingTo("8000.123456"),
                () -> assertThat(lignes).hasSize(1));
        assertLigneCommeAvant(lignes.get(0), "8000.123456");
    }

    /**
     * E-C4.3 : la règle OMRAN prend la 1re transition (par {@code Sorting}) au départ « Nouveau », même en cycle :
     * EN COURS ↔ REGLÉ (l'ancienne règle n'y trouvait aucun départ) → 201, statut EN COURS.
     */
    @Test
    void ec43_commission_cycleAvecDepartNouveau_201_statutEnCours() throws Exception {
        UUID l = ligne("D1", new BigDecimal("300"));
        terrains.executer(MASQUER_WORKFLOW.formatted("ComInvoiceDebitCommission"));
        UUID gestion = commissions.entiteDocument();
        configuration.transition(gestion, 10, "Aller", STATUT_EN_COURS, STATUT_REGLE);
        configuration.transition(gestion, 20, "Retour", STATUT_REGLE, STATUT_EN_COURS);

        mockMvc.perform(document(List.of(l)).with(compte.qui())).andExpect(status().isCreated());

        assertThat(guid(pieceDuCommercial().get(STATUT))).isEqualTo(guid(STATUT_EN_COURS));
    }

    /**
     * E-C4.3 : la 1re transition au départ « Nouveau » par {@code Sorting}, pas la 1re transition tout court : un départ
     * « Intermédiaire » jamais atteint, de {@code Sorting} plus petit, est ignoré (l'ancienne règle le prenait).
     */
    @Test
    void ec43_commission_premiereTransitionParSorting_departNonNouveau_ignoree() throws Exception {
        UUID l = ligne("D1", new BigDecimal("300"));
        terrains.executer(MASQUER_WORKFLOW.formatted("ComInvoiceDebitCommission"));
        UUID gestion = commissions.entiteDocument();
        UUID valide = configuration.statut(new FixtureConfigurationAchat.Statut("VALIDE-" + c.jeton(), "Validé", 5,
                FixtureTerrains.ETAT_INTERMEDIAIRE, false, false, false, null, null));
        configuration.transition(gestion, 5, "Validé → Réglé", valide, STATUT_REGLE);
        configuration.transition(gestion, 10, "En cours → Réglé", STATUT_EN_COURS, STATUT_REGLE);

        mockMvc.perform(document(List.of(l)).with(compte.qui())).andExpect(status().isCreated());

        assertThat(guid(pieceDuCommercial().get(STATUT))).isEqualTo(guid(STATUT_EN_COURS));
    }
}
