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
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.EXTRA;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.RG;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;

/** Montants des lots, bordereau, cumuls, fiche décompte et filtres (FEATURE-projet-chantier-lecture.md §5, §10). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsProjetMontantsTest {

    private static final String CONTENT = "$.content[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private JdbcTemplate jdbc;

    private AppelsProjet appels;
    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor habilite;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PM-" + jeton);
        lot = fixture.lot(projet, "LM-" + jeton, fixture.fournisseur("FOM-" + jeton));
        AdmUser compte = fixture.compte("prj.m." + jeton);
        fixture.habiliter(compte, projet);
        habilite = connecte(compte, "Commercial");
    }

    private Reponse ok(String url, String... params) throws Exception {
        Reponse r = appels.lire(habilite, url, params);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private UUID ligneMarche(UUID lotId, int ordre, String quantite, String puHT, String puTTC, boolean stock) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO PrjMarketLine (PKey, Code, Description, PrjMarketId, Sorting, Unity, Quantity, UnitPriceHT,"
                        + " VatRatio, UnitPriceTTC, IsStock) VALUES (?, ?, ?, ?, ?, 'U', ?, ?, 0.2, ?, ?)",
                id.toString(), "L" + ordre, "Ligne " + ordre, lotId.toString(), ordre, new BigDecimal(quantite),
                new BigDecimal(puHT), new BigDecimal(puTTC), stock);
        return id;
    }

    private void forfait(String ht, String tva, String ttc) {
        jdbc.update("UPDATE PrjMarket SET EstimatedPriceHTManual = ?, EstimatedVatAmountManual = ?,"
                + " EstimatedPriceTTCManual = ? WHERE PKey = ?", new BigDecimal(ht), new BigDecimal(tva), new BigDecimal(ttc),
                lot.toString());
    }

    private static Map<String, BigDecimal> montants(String ttc, String rg, String extra, String aPayer) {
        return Map.of(TTC, new BigDecimal(ttc), RG, new BigDecimal(rg), EXTRA, new BigDecimal(extra),
                A_PAYER, new BigDecimal(aPayer));
    }

    @Test
    void lotAvecBordereau_marcheEgalSommeDesLignesHorsStock_bordereauEtTotauxConcordent() throws Exception {
        ligneMarche(lot, 1, "2", "100", "120", false);
        ligneMarche(lot, 2, "1", "50", "60", false);
        ligneMarche(lot, 3, "10", "1000", "1200", true);
        forfait("9999", "0", "9999");

        Reponse fiche = ok(LOTS + "/" + lot);
        Reponse bordereau = ok(LOTS + "/" + lot + "/bordereau");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fiche.nombre("$.montantHT")).isEqualByComparingTo("250");
            s.assertThat(fiche.nombre("$.montantTVA")).isEqualByComparingTo("50");
            s.assertThat(fiche.nombre("$.montantTTC")).isEqualByComparingTo("300");
            s.assertThat(fiche.valeur("$.montantsDepuisBordereau")).isEqualTo(true);
            s.assertThat(fiche.nombre("$.montantForfaitaireHT")).isEqualByComparingTo("9999");
            s.assertThat(fiche.nombre("$.retenueGarantieTheorique")).isEqualByComparingTo("30");
            s.assertThat(fiche.nombre("$.tauxRetenueGarantie")).isEqualByComparingTo("10");
            s.assertThat((List<?>) bordereau.valeur("$.lignes")).hasSize(2);
            s.assertThat(bordereau.nombre("$.lignes[0].totalHT")).isEqualByComparingTo("200");
            s.assertThat(bordereau.nombre("$.lignes[0].totalTTC")).isEqualByComparingTo("240");
            s.assertThat(bordereau.nombre("$.lignes[0].tauxTVA")).isEqualByComparingTo("20");
            s.assertThat(bordereau.nombre("$.totaux.totalHT")).isEqualByComparingTo("250");
            s.assertThat(bordereau.nombre("$.totaux.totalTVA")).isEqualByComparingTo("50");
            s.assertThat(bordereau.nombre("$.totaux.totalTTC")).isEqualByComparingTo("300");
            s.assertThat(bordereau.nombre("$.totaux.nbLignes")).isEqualByComparingTo("2");
        });
    }

    @Test
    void lotSansLigne_repliSurLeForfait_bordereauVide() throws Exception {
        forfait("1000", "200", "1200");

        Reponse fiche = ok(LOTS + "/" + lot);
        Reponse bordereau = ok(LOTS + "/" + lot + "/bordereau");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fiche.nombre("$.montantHT")).isEqualByComparingTo("1000");
            s.assertThat(fiche.nombre("$.montantTTC")).isEqualByComparingTo("1200");
            s.assertThat(fiche.valeur("$.montantsDepuisBordereau")).isEqualTo(false);
            s.assertThat(fiche.nombre("$.estimationHT")).isEqualByComparingTo("1000");
            s.assertThat(fiche.valeur("$.estimationDepuisBordereau")).isEqualTo(false);
            s.assertThat(fiche.nombre("$.avancementPct")).isEqualByComparingTo("0");
            s.assertThat((List<?>) bordereau.valeur("$.lignes")).isEmpty();
            s.assertThat(bordereau.nombre("$.totaux.totalTTC")).isEqualByComparingTo("0");
        });
    }

    @Test
    void cumulsDesDecomptes_avancement_etFactureReglePourLeLot() throws Exception {
        forfait("1000", "200", "1200");
        UUID d1 = fixture.decompte(lot, "DM1-" + jeton, 1, montants("300", "30", "10", "280"));
        UUID d2 = fixture.decompte(lot, "DM2-" + jeton, 2, montants("300", "30", "0", "270"));
        UUID f1 = fixture.facture(d1, "FM1-" + jeton, new BigDecimal("1000"));
        fixture.facture(d2, "FM2-" + jeton, new BigDecimal("500"));
        fixture.reglement(f1, "RM1-" + jeton, new BigDecimal("600"), "POINTED");
        fixture.reglement(f1, "RM2-" + jeton, new BigDecimal("100"), "CANCELED");

        Reponse liste = ok(LOTS, "projetIds", projet.toString());
        Reponse fiche = ok(LOTS + "/" + lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.ids(CONTENT)).containsExactly(lot.toString());
            s.assertThat(liste.nombre("$.content[0].nbDecomptes")).isEqualByComparingTo("2");
            s.assertThat(liste.nombre("$.content[0].cumulDecomptesTTC")).isEqualByComparingTo("600");
            s.assertThat(liste.nombre("$.content[0].cumulRetenueGarantie")).isEqualByComparingTo("60");
            s.assertThat(liste.nombre("$.content[0].cumulTravauxSupplementaires")).isEqualByComparingTo("10");
            s.assertThat(liste.nombre("$.content[0].avancementPct")).isEqualByComparingTo("50");
            s.assertThat(liste.nombre("$.content[0].totalFacture")).isEqualByComparingTo("1500");
            s.assertThat(liste.nombre("$.content[0].totalRegle")).isEqualByComparingTo("600");
            s.assertThat(liste.nombre("$.content[0].resteARegler")).isEqualByComparingTo("900");
            s.assertThat(fiche.nombre("$.cumulNetAPayer")).isEqualByComparingTo("550");
            s.assertThat(fiche.nombre("$.totalRegle")).isEqualByComparingTo("600");
        });
    }

    @Test
    void ficheDecompte_cumulTTC_lignesExtrasRetenues() throws Exception {
        UUID ligne = ligneMarche(lot, 1, "2", "100", "120", false);
        fixture.decompte(lot, "DF1-" + jeton, 1, montants("300", "30", "0", "270"));
        UUID d2 = fixture.decompte(lot, "DF2-" + jeton, 2, montants("200", "20", "0", "180"));
        jdbc.update("INSERT INTO PrjWorkingOutLine (Pkey, Sorting, Description, Unity, Quantity, QuantityOld, Coefficient,"
                        + " UnitPriceHT, PrjWorkingOutId, PrjMarketLineId) VALUES (?, 1, 'Béton', 'U', 0.5, 1, 1, 100, ?, ?)",
                UUID.randomUUID().toString(), d2.toString(), ligne.toString());
        jdbc.update("INSERT INTO PrjWorkingOutLineExtra (Pkey, Sorting, Description, Quantity, UnitPriceHT, VatRatio,"
                + " UnitPriceTTC, PrjWorkingOutId) VALUES (?, 1, 'Extra', 2, 10, 0.2, 12, ?)", UUID.randomUUID().toString(), d2.toString());
        jdbc.update("INSERT INTO PrjWorkingOutRetainedLine (PKey, PrjWorkingOutId, Sorting, Description, Amount)"
                + " VALUES (?, ?, 1, 'Pénalité', 15)", UUID.randomUUID().toString(), d2.toString());

        Reponse r = ok(DECOMPTES + "/" + d2);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.cumulTTC")).isEqualByComparingTo("500");
            s.assertThat(r.valeur("$.tauxRetenueGarantie")).isNull();
            s.assertThat(r.nombre("$.lignes[0].quantiteMarche")).isEqualByComparingTo("2");
            s.assertThat(r.nombre("$.lignes[0].quantiteCumulee")).isEqualByComparingTo("1.5");
            s.assertThat(r.nombre("$.lignes[0].montantHTPeriode")).isEqualByComparingTo("50");
            s.assertThat(r.nombre("$.lignes[0].avancementLignePct")).isEqualByComparingTo("75");
            s.assertThat(r.nombre("$.extras[0].montantHT")).isEqualByComparingTo("20");
            s.assertThat(r.nombre("$.extras[0].montantTTC")).isEqualByComparingTo("24");
            s.assertThat(r.nombre("$.extras[0].tauxTVA")).isEqualByComparingTo("20");
            s.assertThat(r.valeur("$.retenues[0].libelle")).isEqualTo("Pénalité");
            s.assertThat(r.valeur("$.reglements")).isEqualTo(List.of());
        });
    }

    @Test
    void filtresDesDecomptes_lotSansFacturePeriodeRecherche() throws Exception {
        UUID d1 = fixture.decompte(lot, "DX1-" + jeton, 1, montants("100", "10", "0", "90"));
        UUID d2 = fixture.decompte(lot, "DX2-" + jeton, 5, montants("100", "10", "0", "90"));
        fixture.facture(d1, "FX1-" + jeton, new BigDecimal("90"));
        String lotId = lot.toString();

        Reponse sansFacture = ok(DECOMPTES, "lotId", lotId, "sansFacture", "true");
        Reponse periode = ok(DECOMPTES, "lotId", lotId, "du", "2026-03-04", "au", "2026-03-05");
        Reponse recherche = ok(DECOMPTES, "search", "DX1-" + jeton);
        Reponse rechercheLot = ok(LOTS, "search", "LM-" + jeton);
        Reponse autreLot = ok(DECOMPTES, "lotId", fixture.lot(projet, "LA-" + jeton, null).toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(sansFacture.ids(CONTENT)).containsExactly(d2.toString());
            s.assertThat(periode.ids(CONTENT)).containsExactly(d2.toString());
            s.assertThat(recherche.ids(CONTENT)).containsExactly(d1.toString());
            s.assertThat(rechercheLot.ids(CONTENT)).containsExactly(lotId);
            s.assertThat(autreLot.ids(CONTENT)).isEmpty();
        });
    }
}
