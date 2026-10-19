package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.LignesJson.lignesAvec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglet Frais de vente ({@code CfgTrancheSettlementCategory}) — FEATURE-patrimoine-tranche-onglets.md §1, §2.4, §8.2
 * (TrancheFraisTest). Le spec ne fixe pas le niveau de {@code saisiEnTTC} (ligne ou corps) : il est envoyé aux deux,
 * Jackson ignore celui que le DTO n'a pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheFraisTest {

    private static final String FRAIS = "/api/tranches/{id}/frais";
    private static final String CATEGORIES_FRAIS = "/api/tranches/categories-frais";
    private static final String CATEGORIE_ID = "categorieReglementId";
    private static final String SAISI_EN_TTC = "saisiEnTTC";
    private static final String MONTANT_HT = "montantHT";
    private static final String MONTANT_TTC = "montantTTC";
    private static final String TAUX_TVA = "tauxTva";
    private static final String LIGNE_DE_LA_CATEGORIE =
            "SELECT COUNT(*) FROM CfgTrancheSettlementCategory WHERE CfgtrancheId = :id AND ComSettlementCategoryId = :autre";
    private static final String HT_DE_LA_TRANCHE =
            "SELECT AmountHT FROM CfgTrancheSettlementCategory WHERE CfgtrancheId = :id";
    private static final String TTC_DE_LA_TRANCHE =
            "SELECT AmountTTC FROM CfgTrancheSettlementCategory WHERE CfgtrancheId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private FixtureTrancheOnglets onglets;

    private AdmUser admin;
    private CfgTranche tranche;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1BF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private UUID categorieDeFrais(String prefixe) {
        return onglets.categorieReglement(prefixe + "-" + jeton, true, true);
    }

    private static CorpsJson ligne(UUID categorie, String ht, String tva, String ttc, boolean enTTC) {
        return corps().avec(CATEGORIE_ID, categorie).avec(MONTANT_HT, new BigDecimal(ht))
                .avec(TAUX_TVA, new BigDecimal(tva)).avec(MONTANT_TTC, new BigDecimal(ttc)).avec(SAISI_EN_TTC, enTTC);
    }

    private ResultActions envoyer(boolean enTTC, CorpsJson... lignes) throws Exception {
        return mockMvc.perform(put(FRAIS, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(lignesAvec(SAISI_EN_TTC, enTTC, lignes)));
    }

    // ------------------------------------------------------------------ §1 GET / PUT liste

    @Test
    void r1_get_lignesDeLaTranche() throws Exception {
        UUID c = categorieDeFrais("FG");
        onglets.ligneFrais(tranche.getId(), c, new BigDecimal("100"), new BigDecimal("0.2"), new BigDecimal("120"));

        mockMvc.perform(get(FRAIS, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.." + CATEGORIE_ID, hasItem(c.toString())));
    }

    @Test
    void r1_put_idConnuMaj_sansIdCree_absentSupprime() throws Exception {
        UUID c1 = categorieDeFrais("F1");
        UUID c2 = categorieDeFrais("F2");
        UUID c3 = categorieDeFrais("F3");
        UUID modifiee = onglets.ligneFrais(tranche.getId(), c1, new BigDecimal("100"), new BigDecimal("0.2"),
                new BigDecimal("120"));
        UUID retiree = onglets.ligneFrais(tranche.getId(), c2, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);

        envoyer(false, ligne(c1, "200", "0.2", "240", false).avec("id", modifiee), ligne(c3, "50", "0.1", "55", false))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(onglets.decimal("SELECT AmountHT FROM CfgTrancheSettlementCategory WHERE PKey = :id",
                        modifiee)).isEqualByComparingTo("200"),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTrancheSettlementCategory WHERE PKey = :id",
                        retiree)).isZero(),
                () -> assertThat(onglets.compter(LIGNE_DE_LA_CATEGORIE, tranche.getId(), c3)).isEqualTo(1));
    }

    @Test
    void r7_doublonDeCategorie_uneSeuleLigneGardee() throws Exception {
        UUID c = categorieDeFrais("FX");

        envoyer(false, ligne(c, "10", "0", "10", false), ligne(c, "10", "0", "10", false)).andExpect(status().isOk());

        assertThat(onglets.compter(LIGNE_DE_LA_CATEGORIE, tranche.getId(), c)).isEqualTo(1);
    }

    @Test
    void r1_400_categorieManquante_champIndexe() throws Exception {
        envoyer(false, ligne(categorieDeFrais("FV"), "10", "0", "10", false),
                corps().avec(MONTANT_HT, BigDecimal.TEN).avec(SAISI_EN_TTC, false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("lignes[1].categorieReglementId")));
    }

    // ------------------------------------------------------------------ §2.4 HT ↔ TTC calculé par le serveur

    @Test
    void r2_4_saisieHT_ttcCalcule_ttcEnvoyeIgnore() throws Exception {
        envoyer(false, ligne(categorieDeFrais("FHT"), "1000", "0.2", "999", false)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(onglets.decimal(TTC_DE_LA_TRANCHE, tranche.getId())).isEqualByComparingTo("1200"),
                () -> assertThat(onglets.decimal(HT_DE_LA_TRANCHE, tranche.getId())).isEqualByComparingTo("1000"),
                () -> assertThat(onglets.decimal("SELECT VatRatio FROM CfgTrancheSettlementCategory WHERE CfgtrancheId = :id",
                        tranche.getId())).isEqualByComparingTo("0.2"));
    }

    /** 100 / 1,07 = 93,45794392… → 93,457944 (échelle 6 HALF_UP de CalculPrixArticle). */
    @Test
    void r2_4_saisieTTC_htCalcule_arrondiEchelle6() throws Exception {
        envoyer(true, ligne(categorieDeFrais("FTTC"), "1", "0.07", "100", true)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(onglets.decimal(HT_DE_LA_TRANCHE, tranche.getId())).isEqualByComparingTo("93.457944"),
                () -> assertThat(onglets.decimal(TTC_DE_LA_TRANCHE, tranche.getId())).isEqualByComparingTo("100"));
    }

    @Test
    void r2_4_pourcentageDe_enregistre() throws Exception {
        envoyer(false, ligne(categorieDeFrais("FPC"), "0", "0", "0", false).avec("pourcentageDe", new BigDecimal("0.05")))
                .andExpect(status().isOk());

        assertThat(onglets.decimal("SELECT PercentOf FROM CfgTrancheSettlementCategory WHERE CfgtrancheId = :id",
                tranche.getId())).isEqualByComparingTo("0.05");
    }

    // ------------------------------------------------------------------ §2.4 liste de choix

    @Test
    void r2_4_categoriesFrais_creditEtFraisDeVenteSeulement() throws Exception {
        String retenue = "FOK-" + jeton;
        String pasFraisDeVente = "FNV-" + jeton;
        String pasCredit = "FNC-" + jeton;
        onglets.categorieReglement(retenue, true, true);
        onglets.categorieReglement(pasFraisDeVente, true, false);
        onglets.categorieReglement(pasCredit, false, true);

        mockMvc.perform(get(CATEGORIES_FRAIS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(retenue)))
                .andExpect(content().string(not(containsString(pasFraisDeVente))))
                .andExpect(content().string(not(containsString(pasCredit))));
    }

    @Test
    void r2_4_categoriesFrais_sansLesTroisCategoriesParDefaut() throws Exception {
        String transfertClient = "FDC-" + jeton;
        String transfertProduit = "FDP-" + jeton;
        String desistement = "FDD-" + jeton;
        onglets.parametreGuid("DefaultCustomerTransfertDocumentType",
                onglets.categorieReglement(transfertClient, true, true));
        onglets.parametreGuid("DefaultProductTransfertDocumentType",
                onglets.categorieReglement(transfertProduit, true, true));
        onglets.parametreGuid("DefaultCancellationDocumentType", onglets.categorieReglement(desistement, true, true));
        String temoin = "FDT-" + jeton;
        onglets.categorieReglement(temoin, true, true);

        mockMvc.perform(get(CATEGORIES_FRAIS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(temoin)))
                .andExpect(content().string(not(containsString(transfertClient))))
                .andExpect(content().string(not(containsString(transfertProduit))))
                .andExpect(content().string(not(containsString(desistement))));
    }
}
