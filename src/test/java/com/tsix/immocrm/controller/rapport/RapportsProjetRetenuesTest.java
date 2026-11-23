package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.PENALITE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.RESTE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.RESTITUEE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.RETENUE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.codes;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.RETENUES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** R1 {@code PRJWarrantyStatus} « ETAT DES RETENUES DE GARANTIE » et R5e (FEATURE-n5b-rapports-projet.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetRetenuesTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 3, 5);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat comptes;
    @Autowired private FixtureRapportsProjet fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;

    private AppelsRapports appels;
    private Cadre c;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
    }

    private UUID lotMono(UUID projet, String code) {
        UUID lot = parite.lot(projet, c.code(code), ENTREPRENEURS, c.statutMono());
        fixture.fournisseur(lot, c.fournisseur());
        return lot;
    }

    /** Lot décompté {@code decompte} (RG des décomptes), facturé {@code facture} (RG des factures). */
    private UUID lotAvecRetenues(UUID lot, String code, String decompte, String facture) {
        fixture.decompte(lot, c.code(code) + "-D", 1, decompte, "0", "0", JOUR);
        fixture.facture(lot, c.code(code) + "-F", facture, "0", "0");
        return lot;
    }

    private Map<String, Object> section(UUID... projets) throws Exception {
        return premiere(appels.sections(requete(RETENUES, c.compte(), projets)));
    }

    @Test
    void r1_lotMono_retenueDecomptee_resteSurRetenueFacturee() throws Exception {
        lotAvecRetenues(lotMono(c.pa1(), "LM"), "LM", "100", "80");

        Map<String, Object> l = ligne(section(c.pa1()), c.code("LM"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, RETENUE)).as("mono : retenue décomptée").isEqualByComparingTo("100");
            s.assertThat(montant(l, RESTE)).as("reste sur la retenue facturée").isEqualByComparingTo("80");
            s.assertThat(montant(l, RESTITUEE)).isEqualByComparingTo("0");
            s.assertThat(montant(l, PENALITE)).isEqualByComparingTo("0");
            s.assertThat(l.get("bloque")).isEqualTo(false);
        });
    }

    @Test
    void r1_colonnes_codeDescriptionFournisseurProjet() throws Exception {
        lotAvecRetenues(lotMono(c.pa1(), "LC"), "LC", "100", "80");

        Map<String, Object> l = ligne(section(c.pa1()), c.code("LC"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(l).containsEntry("description", "Lot " + c.code("LC"));
            s.assertThat(l).containsEntry("fournisseur", c.nom());
            s.assertThat(l).containsEntry("projet", "PA1-" + c.jeton());
        });
    }

    @Test
    void r1_lotMulti_retenueFacturee() throws Exception {
        UUID lot = parite.lot(c.pa1(), c.code("LX"), FOURNISSEURS, c.statutMulti());
        lotAvecRetenues(lot, "LX", "100", "80");

        Map<String, Object> l = ligne(section(c.pa1()), c.code("LX"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, RETENUE)).as("multi : retenue facturée").isEqualByComparingTo("80");
            s.assertThat(montant(l, RESTE)).isEqualByComparingTo("80");
        });
    }

    @Test
    void r1_lotBloque_resteZero_bloqueVrai() throws Exception {
        UUID lot = lotAvecRetenues(lotMono(c.pa1(), "LB"), "LB", "100", "80");
        fixture.marche(lot, "BlokWarrantyRestitution", true);

        Map<String, Object> l = ligne(section(c.pa1()), c.code("LB"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, RESTE)).as("bloqué : reste 0").isEqualByComparingTo("0");
            s.assertThat(l.get("bloque")).isEqualTo(true);
            s.assertThat(montant(l, RETENUE)).isEqualByComparingTo("100");
        });
    }

    @Test
    void r1_penaliteEtRestituee_pieceDeRestitution_resteDiminue() throws Exception {
        UUID lot = lotAvecRetenues(lotMono(c.pa1(), "LR"), "LR", "100", "80");
        fixture.restitution(lot, c.code("LR") + "-RS", "-10", "30");

        Map<String, Object> l = ligne(section(c.pa1()), c.code("LR"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, PENALITE)).as("pénalité = −AmountRegul, positive").isEqualByComparingTo("10");
            s.assertThat(montant(l, RESTITUEE)).as("Σ lignes de règlement de la restitution").isEqualByComparingTo("30");
            s.assertThat(montant(l, RESTE)).as("80 − 10 − 30").isEqualByComparingTo("40");
        });
    }

    @Test
    void r1_lotSansAucunMontant_absent() throws Exception {
        lotAvecRetenues(lotMono(c.pa1(), "LA"), "LA", "100", "80");
        lotMono(c.pa1(), "LZ");

        assertThat(codes(section(c.pa1()))).contains(c.code("LA")).doesNotContain(c.code("LZ"));
    }

    @Test
    void r1_lotAvecSeulementRestitution_present() throws Exception {
        UUID lot = lotMono(c.pa1(), "LS");
        fixture.restitution(lot, c.code("LS") + "-RS", "0", "30");

        Map<String, Object> l = ligne(section(c.pa1()), c.code("LS"));

        assertThat(montant(l, RESTITUEE)).isEqualByComparingTo("30");
    }

    @Test
    void r1_parametreProjets_neGardeQueLesLotsDeCesProjets() throws Exception {
        lotAvecRetenues(lotMono(c.pa1(), "L1"), "L1", "100", "80");
        lotAvecRetenues(lotMono(c.pa2(), "L2"), "L2", "100", "80");

        assertThat(codes(section(c.pa1()))).contains(c.code("L1")).doesNotContain(c.code("L2"));
        assertThat(codes(section(c.pa1(), c.pa2()))).contains(c.code("L1"), c.code("L2"));
    }

    @Test
    void r1_tri_projetPuisLot() throws Exception {
        lotAvecRetenues(lotMono(c.pa2(), "L1"), "L1", "10", "10");
        lotAvecRetenues(lotMono(c.pa1(), "L3"), "L3", "10", "10");
        lotAvecRetenues(lotMono(c.pa1(), "L2"), "L2", "10", "10");

        List<Object> ordre = codes(section(c.pa1(), c.pa2()));

        assertThat(ordre).containsExactly(c.code("L2"), c.code("L3"), c.code("L1"));
    }

    @Test
    void r1_totaux_sommeDesQuatreMontants_sansMarche() throws Exception {
        lotAvecRetenues(lotMono(c.pa1(), "T1"), "T1", "100", "80");
        UUID multi = parite.lot(c.pa1(), c.code("T2"), FOURNISSEURS, c.statutMulti());
        lotAvecRetenues(multi, "T2", "70", "50");
        fixture.restitution(multi, c.code("T2") + "-RS", "-5", "20");

        Map<String, Object> section = section(c.pa1());
        Map<String, Object> t = totaux(section);
        List<Map<String, Object>> lignes = lignes(section);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).hasSize(2);
            s.assertThat(nombre(t.get(RETENUE))).as("100 + 50").isEqualByComparingTo("150");
            s.assertThat(nombre(t.get(RESTITUEE))).isEqualByComparingTo("20");
            s.assertThat(nombre(t.get(PENALITE))).isEqualByComparingTo("5");
            s.assertThat(nombre(t.get(RESTE))).as("80 + (50 − 5 − 20)").isEqualByComparingTo("105");
            s.assertThat(nombre(t.get(RESTE))).isEqualByComparingTo(somme(lignes, RESTE));
            s.assertThat(t.get("marcheAjusteTtc")).as("pas de Σ du marché").isNull();
        });
    }

    @Test
    void r5e_marcheAjusteTtc_egalAuMarcheAjusteDeLaFicheLot() throws Exception {
        UUID lot = lotAvecRetenues(lotMono(c.pa1(), "LF"), "LF", "100", "80");
        fiche.devisRetenu(lot, c.code("LF") + "-Q", "900", "1080");
        String corps = mockMvc.perform(get("/api/projet/lots/" + lot)
                        .with(connecte(parite.administrateur("n5b.adm." + c.jeton(), c.pa1()), "Administrateur")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        BigDecimal ficheTtc = nombre(JsonPath.read(corps, "$.general.marcheAjuste.ttc"));

        Map<String, Object> l = ligne(section(c.pa1()), c.code("LF"));

        assertThat(nombre(l.get("marcheAjusteTtc"))).isNotNull().isEqualByComparingTo(ficheTtc);
    }
}
