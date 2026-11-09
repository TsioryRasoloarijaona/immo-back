package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.controller.projet.AppelsProjet.REFERENTIELS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.RG;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue adversariale du lot P1 : visibilité par les filtres, fournisseur du décompte, régularisation, cumul, tri, 400. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetLotsRevueTest {

    private static final String CONTENT = "$.content[*].id";
    private static final String LOT_ID = "lotId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private JdbcTemplate jdbc;

    private AppelsProjet appels;
    private String jeton;
    private UUID projetA;
    private UUID fournisseurLot;
    private UUID lotA;
    private UUID lotB;
    private UUID decompteB;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        projetA = fixture.projet("RA-" + jeton);
        UUID projetB = fixture.projet("RB-" + jeton);
        fournisseurLot = fixture.fournisseur("RFL-" + jeton);
        lotA = fixture.lot(projetA, "RLA-" + jeton, fournisseurLot);
        lotB = fixture.lot(projetB, "RLB-" + jeton, fournisseurLot);
        decompteB = fixture.decompte(lotB, "RDB-" + jeton, 1, montants("100", "10", "90"));
        AdmUser compte = fixture.compte("prj.r." + jeton);
        fixture.habiliter(compte, projetA);
        a = connecte(compte, "Commercial");
    }

    private static Map<String, BigDecimal> montants(String ttc, String rg, String aPayer) {
        return Map.of(TTC, new BigDecimal(ttc), RG, new BigDecimal(rg), A_PAYER, new BigDecimal(aPayer));
    }

    private Reponse ok(String url, String... params) throws Exception {
        Reponse r = appels.lire(a, url, params);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    @Test
    void filtresPointantUnLotOuUnCodeHorsPerimetre_rienNeFuit() throws Exception {
        Reponse parLot = ok(DECOMPTES, LOT_ID,lotB.toString());
        Reponse parCodeDecompte = ok(DECOMPTES, "search", "RDB-" + jeton);
        Reponse parCodeLot = ok(LOTS, "search", "RLB-" + jeton);
        Reponse parFournisseur = ok(LOTS, "fournisseurId", fournisseurLot.toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(parLot.ids(CONTENT)).isEmpty();
            s.assertThat(parCodeDecompte.ids(CONTENT)).isEmpty();
            s.assertThat(parCodeLot.ids(CONTENT)).isEmpty();
            s.assertThat(parFournisseur.ids(CONTENT)).containsExactly(lotA.toString());
        });
    }

    @Test
    void fournisseurDuDecompte_primeSurCeluiDuLot_dansLeFiltreEtLeNom() throws Exception {
        UUID autre = fixture.fournisseur("RFD-" + jeton);
        UUID propre = fixture.decompte(lotA, "RD1-" + jeton, 1, montants("100", "10", "90"));
        UUID herite = fixture.decompte(lotA, "RD2-" + jeton, 2, montants("100", "10", "90"));
        jdbc.update("UPDATE PrjWorkingOut SET CfgTierId = ? WHERE PKey = ?", autre.toString(), propre.toString());

        Reponse parAutre = ok(DECOMPTES, LOT_ID,lotA.toString(), "fournisseurId", autre.toString());
        Reponse parLot = ok(DECOMPTES, LOT_ID,lotA.toString(), "fournisseurId", fournisseurLot.toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(parAutre.ids(CONTENT)).containsExactly(propre.toString());
            s.assertThat(parAutre.valeur("$.content[0].fournisseurId")).isEqualTo(autre.toString());
            s.assertThat(parLot.ids(CONTENT)).containsExactly(herite.toString());
        });
    }

    @Test
    void resteARegler_surLeNetDeLaFacture_regularisationComprise() throws Exception {
        UUID d = fixture.decompte(lotA, "RDR-" + jeton, 1, montants("1000", "100", "900"));
        UUID f = fixture.facture(d, "RFR-" + jeton, new BigDecimal("950"));
        fixture.reglement(f, "RRR-" + jeton, new BigDecimal("400"), null);

        Reponse liste = ok(DECOMPTES, LOT_ID,lotA.toString());
        Reponse lot = ok(LOTS + "/" + lotA);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.nombre("$.content[0].netAPayer")).isEqualByComparingTo("900");
            s.assertThat(liste.nombre("$.content[0].montantRegle")).isEqualByComparingTo("400");
            s.assertThat(liste.nombre("$.content[0].resteARegler")).isEqualByComparingTo("550");
            s.assertThat(lot.nombre("$.totalFacture")).isEqualByComparingTo("950");
            s.assertThat(lot.nombre("$.resteARegler")).isEqualByComparingTo("550");
        });
    }

    @Test
    void cumulTTC_neCompteNiLesDecomptesSuivantsNiCeuxDUnAutreLot() throws Exception {
        UUID autreLot = fixture.lot(projetA, "RLX-" + jeton, fournisseurLot);
        fixture.decompte(autreLot, "RX1-" + jeton, 1, montants("7000", "700", "6300"));
        fixture.decompte(lotA, "RC1-" + jeton, 1, montants("100", "10", "90"));
        UUID d2 = fixture.decompte(lotA, "RC2-" + jeton, 2, montants("200", "20", "180"));
        fixture.decompte(lotA, "RC3-" + jeton, 3, montants("400", "40", "360"));

        Reponse r = ok(DECOMPTES + "/" + d2);

        assertThat(r.nombre("$.cumulTTC")).isEqualByComparingTo("300");
    }

    @Test
    void statutsFacture_triesParSorting() throws Exception {
        UUID premier = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        jdbc.update("INSERT INTO BuyDocumentStatus (PKey, Code, Description, Sorting) VALUES (?, ?, ?, 1)",
                premier.toString(), "ZZ-" + jeton, "ZZ-" + jeton);
        jdbc.update("INSERT INTO BuyDocumentStatus (PKey, Code, Description, Sorting) VALUES (?, ?, ?, 999999)",
                second.toString(), "AA-" + jeton, "AA-" + jeton);

        Reponse r = ok(REFERENTIELS);
        List<String> ids = r.ids("$.statutsFacture[*].id");

        assertThat(ids).contains(premier.toString(), second.toString());
        assertThat(ids.indexOf(premier.toString())).isLessThan(ids.indexOf(second.toString()));
    }

    @Test
    void filtreMalForme_400() throws Exception {
        Reponse lots = appels.lire(a, LOTS, "projetIds", "pas-un-uuid");
        Reponse decomptes = appels.lire(a, DECOMPTES, "du", "31/12/2026");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lots.statut()).as(lots.corps()).isEqualTo(400);
            s.assertThat(decomptes.statut()).as(decomptes.corps()).isEqualTo(400);
        });
    }
}
