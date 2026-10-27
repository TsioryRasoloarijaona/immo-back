package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ApiCommissions.DETAIL;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_FINALISATION_1;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_FINALISATION_2;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.raisons;
import static com.tsix.immocrm.fixtures.FixtureCommissions.FINALISATION_1;
import static com.tsix.immocrm.fixtures.FixtureCommissions.FINALISATION_2;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.horodatage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Commissions de finalisation — FEATURE-vente-commissions.md §2 R-P (finalisation 1 = {@code CfgSaleManagerId},
 * 2 = {@code CfgSaleManager2Id}), R-F (date lue dynamiquement : {@code CfgTrancheFolderEntity} → {@code StrStatus.StrFieldDateId}
 * → {@code StrField.Code} = colonne de {@code ComFolder}), C9 (erreur de configuration = 400 et aucune ligne), §8.1
 * « Finalisation ». Règles : colonnes {@code DeliveryCommission…} (finalisation 1) et {@code DeliveryCommission…2} (2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionFinalisationTest {

    private static final String MESSAGE_PARAMETRAGE_1 = "Il faut faire le paramétrage du statut de la Finalisation 1 dans "
            + "la tranche et s'assurer que ce statut est bien associé à une date !";
    private static final String MESSAGE_PARAMETRAGE_2 = "Il faut faire le paramétrage du statut de la Finalisation 2 dans "
            + "la tranche et s'assurer que ce statut est bien associé à une date !";
    private static final String MESSAGE_SANS_DATE_1 = "Il faut s'assurer que le statut de la Finalisation 1 est bien associé "
            + "à une date !";
    private static final BigDecimal PRIX = new BigDecimal("775000");
    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final LocalDate TITRE = LocalDate.of(2024, 5, 10);
    private static final LocalDate LIVRAISON = LocalDate.of(2024, 9, 15);
    private static final String GESTIONNAIRE = "CfgSaleManagerId";
    private static final String GESTIONNAIRE_2 = "CfgSaleManager2Id";
    private static final String TITRE_PROPRIETE = "OwnershipCertificateDate";
    private static final String LIVRAISON_REELLE = "DeliveryDateReal";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;

    private Contexte c;
    private RequestPostProcessor qui;
    private UUID commercial;
    private UUID gestionnaire;
    private UUID gestionnaire2;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMF");
        qui = fixture.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU).qui();
        commercial = fixture.personne("COM-" + c.jeton(), false);
        gestionnaire = fixture.personne("GES-" + c.jeton(), false);
        gestionnaire2 = fixture.personne("GE2-" + c.jeton(), false);
        fixture.regle(c.tranche().getId(),
                "SaleCommissionBasedOnAmount", true, "SaleCommissionAmount", new BigDecimal("100"),
                "DeliveryCommissionBasedOnAmount", true, "DeliveryCommissionAmount", new BigDecimal("500"),
                "DeliveryCommission2BasedOnAmount", true, "DeliveryCommissionAmount2", new BigDecimal("700"));
    }

    private void configurer() {
        fixture.finalisations(c.tranche(), fixture.statutFinalisation(c, "FORMALISE", TITRE_PROPRIETE),
                fixture.statutFinalisation(c, "REMISE DES CLEFS", LIVRAISON_REELLE));
    }

    private Dossier dossierFinalise(String code) {
        Dossier d = fixture.dossier(c, code, commercial, PRIX, AVANCE);
        fixture.poser(d, GESTIONNAIRE, gestionnaire);
        fixture.poser(d, GESTIONNAIRE_2, gestionnaire2);
        fixture.poser(d, TITRE_PROPRIETE, TITRE);
        fixture.poser(d, LIVRAISON_REELLE, LIVRAISON);
        return d;
    }

    private ResultActions generer(String type, Dossier... dossiers) throws Exception {
        return mockMvc.perform(generation(c.tranche().getId(), type, Stream.of(dossiers).map(Dossier::id).toList())
                .with(qui));
    }

    // ------------------------------------------------------------------ R-F : configuration absente → 400, rien d'écrit

    @Test
    void rf_trancheSansConfiguration_finalisation1_400MessageExact_aucuneLigne() throws Exception {
        Dossier d1 = dossierFinalise("D1");
        Dossier d2 = dossierFinalise("D2");

        generer(T_FINALISATION_1, d1, d2).andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_PARAMETRAGE_1));

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d1.id())).isEmpty(),
                () -> assertThat(fixture.lignesDuDossier(d2.id())).isEmpty());
    }

    @Test
    void rf_trancheSansConfiguration_finalisation2_400MessageExact() throws Exception {
        Dossier d = dossierFinalise("D1");

        generer(T_FINALISATION_2, d).andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_PARAMETRAGE_2));

        assertThat(fixture.lignesDuDossier(d.id())).isEmpty();
    }

    @Test
    void rf_statutFinalisation1SansDate_400MessageExact_aucuneLigne() throws Exception {
        fixture.finalisations(c.tranche(), fixture.statutFinalisation(c, "FORMALISE", null),
                fixture.statutFinalisation(c, "REMISE DES CLEFS", LIVRAISON_REELLE));
        Dossier d = dossierFinalise("D1");

        generer(T_FINALISATION_1, d).andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_SANS_DATE_1));

        assertThat(fixture.lignesDuDossier(d.id())).isEmpty();
    }

    // ------------------------------------------------------------------ R-F / R-P : date dynamique, personne

    @Test
    void rf_finalisation1_dateOwnershipCertificateDate_gestionnaire_colonnesDelivery() throws Exception {
        configurer();
        Dossier d = dossierFinalise("D1");

        generer(T_FINALISATION_1, d).andExpect(status().isOk());

        List<Tuple> lignes = fixture.lignesDuDossier(d.id());
        assertThat(lignes).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get("CommissionType")).isEqualTo(FINALISATION_1),
                () -> assertThat(horodatage(l.get("DocDate"))).isEqualTo(TITRE.atStartOfDay()),
                () -> assertThat(guid(l.get("CfgEmployeeId"))).isEqualTo(guid(gestionnaire)),
                () -> assertThat(decimal(l.get("CommissionAmount"))).isEqualByComparingTo("500")));
    }

    @Test
    void rf_finalisation2_dateDeliveryDateReal_gestionnaire2_colonnesDelivery2() throws Exception {
        configurer();
        Dossier d = dossierFinalise("D1");

        generer(T_FINALISATION_2, d).andExpect(status().isOk());

        List<Tuple> lignes = fixture.lignesDuDossier(d.id());
        assertThat(lignes).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get("CommissionType")).isEqualTo(FINALISATION_2),
                () -> assertThat(horodatage(l.get("DocDate"))).isEqualTo(LIVRAISON.atStartOfDay()),
                () -> assertThat(guid(l.get("CfgEmployeeId"))).isEqualTo(guid(gestionnaire2)),
                () -> assertThat(decimal(l.get("CommissionAmount"))).isEqualByComparingTo("700")));
    }

    /** La colonne vient du paramétrage, pas d'un nom figé : le statut 1 pointé sur DeliveryDateReal date la ligne au 15/09. */
    @Test
    void rf_colonneLueDynamiquement_finalisation1SurDeliveryDateReal() throws Exception {
        fixture.finalisations(c.tranche(), fixture.statutFinalisation(c, "LIVRE", LIVRAISON_REELLE),
                fixture.statutFinalisation(c, "TITRE", TITRE_PROPRIETE));
        Dossier d = dossierFinalise("D1");

        generer(T_FINALISATION_1, d).andExpect(status().isOk());

        assertThat(fixture.lignesDuDossier(d.id())).singleElement()
                .satisfies(l -> assertThat(horodatage(l.get("DocDate"))).isEqualTo(LIVRAISON.atStartOfDay()));
    }

    /** R-S : la fenêtre de la règle s'applique à la date de finalisation, pas à la concrétisation. */
    @Test
    void rs_fenetreSurLaDateDeFinalisation() throws Exception {
        configurer();
        fixture.regle(c.tranche().getId(), "CfgTierId", gestionnaire, "EndDate", LocalDate.of(2024, 3, 31),
                "DeliveryCommissionBasedOnAmount", true, "DeliveryCommissionAmount", new BigDecimal("9000"));
        Dossier d = dossierFinalise("D1");

        generer(T_FINALISATION_1, d).andExpect(status().isOk());

        assertThat(fixture.lignesDuDossier(d.id())).singleElement()
                .satisfies(l -> assertThat(decimal(l.get("CommissionAmount"))).as("règle du gestionnaire close au 31/03")
                        .isEqualByComparingTo("500"));
    }

    @Test
    void rf_dossierSansLaDateDuStatut_SANS_DATE() throws Exception {
        configurer();
        Dossier d = dossierFinalise("D1");
        fixture.poser(d, TITRE_PROPRIETE, null);

        ResultActions r = generer(T_FINALISATION_1, d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("SANS_DATE"));
    }

    @Test
    void rp_finalisation2SansGestionnaire2_SANS_PERSONNE() throws Exception {
        configurer();
        Dossier d = dossierFinalise("D1");
        fixture.poser(d, GESTIONNAIRE_2, null);

        ResultActions r = generer(T_FINALISATION_2, d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("SANS_PERSONNE"));
    }

    @Test
    void rp_finalisation1SansGestionnaire_SANS_PERSONNE() throws Exception {
        configurer();
        Dossier d = dossierFinalise("D1");
        fixture.poser(d, GESTIONNAIRE, null);

        ResultActions r = generer(T_FINALISATION_1, d).andExpect(status().isOk());

        assertThat(raisons(r, d.id())).containsExactly("SANS_PERSONNE");
    }
}
