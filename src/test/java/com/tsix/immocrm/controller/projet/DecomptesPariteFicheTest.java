package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.InstanceOfAssertFactories;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.PRORATA;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fiche d'un décompte, champs de lecture OMRAN (FEATURE-decomptes-parite.md « Fiche », ECARTS §4). Seuls les champs dont
 * le spec nomme la colonne source ; noms du fil proposés par le test-writer, à recopier dans le spec avant verrouillage.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesPariteFicheTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private JdbcTemplate jdbc;

    private AppelsProjet appels;
    private String jeton;
    private UUID lot;
    private RequestPostProcessor habilite;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID projet = fixture.projet("PPF-" + jeton);
        lot = fixture.lot(projet, "LPF-" + jeton, fixture.fournisseur("FPF-" + jeton));
        AdmUser compte = fixture.compte("prj.pf." + jeton);
        fixture.habiliter(compte, projet);
        habilite = connecte(compte, "Commercial");
    }

    private UUID decompte(String code, String... colonneValeur) {
        Map<String, BigDecimal> m = new HashMap<>(Map.of(TTC, new BigDecimal("1000"), A_PAYER,
                new BigDecimal("1000")));
        for (int i = 0; i + 1 < colonneValeur.length; i += 2) {
            m.put(colonneValeur[i], new BigDecimal(colonneValeur[i + 1]));
        }
        return fixture.decompte(lot, code + jeton, 1, m);
    }

    private Reponse fiche(UUID decompte) throws Exception {
        Reponse r = appels.lire(habilite, DECOMPTES + "/" + decompte);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    @Test
    void fiche_note_egaleNotesDuDecompte() throws Exception {
        UUID d = decompte("DFN-");
        jdbc.update("UPDATE PrjWorkingOut SET Notes = ? WHERE PKey = ?", "Note fiche " + jeton, d.toString());

        assertThat(fiche(d).valeur("$.notes")).isEqualTo("Note fiche " + jeton);
    }

    /** Cases « basé sur montant » : Avance, RG, assurance décennale, TRC, compte prorata (colonnes *BasedOnAmount). */
    @Test
    void fiche_casesBaseSurMontant_lesCinq() throws Exception {
        UUID oui = decompte("DFC-");
        jdbc.update("""
                UPDATE PrjWorkingOut SET AdvanceBasedOnAmount = 1, WarrantyBasedOnAmount = 1, DECBasedOnAmount = 1,
                    TRCBasedOnAmount = 1, ProrataBasedOnAmount = 1 WHERE PKey = ?""", oui.toString());

        Reponse r = fiche(oui);

        SoftAssertions.assertSoftly(s -> List.of("avanceSurMontant", "retenueGarantieSurMontant",
                        "assuranceDECSurMontant", "assuranceTRCSurMontant", "compteProrataSurMontant")
                .forEach(champ -> s.assertThat(r.valeur("$." + champ)).as(champ).isEqualTo(true)));
    }

    @Test
    void fiche_casesBaseSurMontant_zero_faux() throws Exception {
        UUID non = decompte("DFZ-");
        jdbc.update("""
                UPDATE PrjWorkingOut SET AdvanceBasedOnAmount = 0, WarrantyBasedOnAmount = 0, DECBasedOnAmount = 0,
                    TRCBasedOnAmount = 0, ProrataBasedOnAmount = 0 WHERE PKey = ?""", non.toString());

        Reponse r = fiche(non);

        SoftAssertions.assertSoftly(s -> List.of("avanceSurMontant", "retenueGarantieSurMontant",
                        "assuranceDECSurMontant", "assuranceTRCSurMontant", "compteProrataSurMontant")
                .forEach(champ -> s.assertThat(r.valeur("$." + champ)).as(champ).isEqualTo(false)));
    }

    /** Taux en pourcentage, comme {@code tauxRetenueGarantie} (ratio stocké en fraction × 100). */
    @Test
    void fiche_tauxEnPourcentage_avanceAssurancesProrata() throws Exception {
        UUID d = decompte("DFT-");
        jdbc.update("""
                UPDATE PrjWorkingOut SET AdvanceRatio = 0.1, insuranceDECRatio = 0.01, insuranceTRCRatio = 0.02,
                    proportionRatio = 0.005 WHERE PKey = ?""", d.toString());

        Reponse r = fiche(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.tauxAvance")).isEqualByComparingTo("10");
            s.assertThat(r.nombre("$.tauxAssuranceDEC")).isEqualByComparingTo("1");
            s.assertThat(r.nombre("$.tauxAssuranceTRC")).isEqualByComparingTo("2");
            s.assertThat(r.nombre("$.tauxCompteProrata")).isEqualByComparingTo("0.5");
        });
    }

    @Test
    void fiche_remboursementCompteProrata_egaleProrataRefundAmount() throws Exception {
        UUID d = decompte("DFP-", PRORATA, "33.30", A_PAYER, "1033.30");

        assertThat(fiche(d).nombre("$.remboursementCompteProrata")).isEqualByComparingTo("33.30");
    }

    /** Onglet Approvisionnement (PrjWorkingOutStockLine) : affiché vide, jamais absent. */
    @Test
    void fiche_ongletApprovisionnement_tableauVide() throws Exception {
        UUID d = decompte("DFA-");

        assertThat(fiche(d).valeur("$.approvisionnements")).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
    }
}
