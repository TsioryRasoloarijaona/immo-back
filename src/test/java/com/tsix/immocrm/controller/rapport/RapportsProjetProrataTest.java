package com.tsix.immocrm.controller.rapport;

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
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.CODE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.codes;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.PRORATA;
import static org.assertj.core.api.Assertions.assertThat;

/** R4 {@code PrjProrataState} « ETAT DES COMPTES PRORATA », sections {@code prorata} et {@code remboursements}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetProrataTest {

    private static final String RETENUES = "prorata";
    private static final String REMBOURSEMENTS = "remboursements";
    private static final String DECOMPTE = "decompte";
    private static final String REMBOURSE = "rembourse";
    private static final String PREVISIONNEL = "previsionnel";

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

    private UUID lot(UUID projet, String code) {
        UUID lot = parite.lot(projet, c.code(code), ENTREPRENEURS, c.statutMono());
        fixture.fournisseur(lot, c.fournisseur());
        return lot;
    }

    private List<Map<String, Object>> sections(UUID... projets) throws Exception {
        return appels.sections(requete(PRORATA, c.compte(), projets));
    }

    private static LocalDate jour(int j) {
        return LocalDate.of(2026, 3, j);
    }

    @Test
    void r4_lotSansAucunMontant_presentDansRetenues() throws Exception {
        lot(c.pa1(), "LZ");

        Map<String, Object> l = ligne(section(sections(c.pa1()), RETENUES), c.code("LZ"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, DECOMPTE)).isEqualByComparingTo("0");
            s.assertThat(l).containsEntry("description", "Lot " + c.code("LZ"));
            s.assertThat(l).containsEntry("fournisseur", c.nom());
        });
    }

    @Test
    void r4_taux_fois100_decompte_sommeProrataDesDecomptes() throws Exception {
        UUID lot = lot(c.pa1(), "LT");
        fixture.marche(lot, "ProportionRatio", new BigDecimal("0.01"));
        fixture.decompte(lot, c.code("LT") + "-D1", 1, "0", "30", "0", jour(1));
        fixture.decompte(lot, c.code("LT") + "-D2", 2, "0", "20", "0", jour(2));

        Map<String, Object> l = ligne(section(sections(c.pa1()), RETENUES), c.code("LT"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, "taux")).as("fraction 0,01 → 1 sur le fil").isEqualByComparingTo("1");
            s.assertThat(montant(l, DECOMPTE)).as("30 + 20").isEqualByComparingTo("50");
        });
    }

    @Test
    void r4_tri_codeProjetPuisCodeLot() throws Exception {
        lot(c.pa2(), "L1");
        lot(c.pa1(), "L3");
        lot(c.pa1(), "L2");

        List<Object> ordre = codes(section(sections(c.pa1(), c.pa2()), RETENUES));

        assertThat(ordre).containsExactly(c.code("L2"), c.code("L3"), c.code("L1"));
    }

    @Test
    void r4_totauxRetenues_previsionnelEtDecompte() throws Exception {
        UUID lot = lot(c.pa1(), "T1");
        fixture.marche(lot, "ProportionRatio", new BigDecimal("0.01"));
        fixture.decompte(lot, c.code("T1") + "-D", 1, "0", "30", "0", jour(1));
        UUID autre = lot(c.pa1(), "T2");
        fixture.decompte(autre, c.code("T2") + "-D", 1, "0", "15", "0", jour(1));

        Map<String, Object> section = section(sections(c.pa1()), RETENUES);
        Map<String, Object> t = totaux(section);
        List<Map<String, Object>> lignes = lignes(section);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).hasSize(2);
            s.assertThat(nombre(t.get(DECOMPTE))).as("30 + 15").isEqualByComparingTo("45");
            s.assertThat(nombre(t.get(PREVISIONNEL))).isEqualByComparingTo(somme(lignes, PREVISIONNEL));
        });
    }

    @Test
    void r4_remboursement_uneLigneParDecompte_dateNulleResteNulle() throws Exception {
        UUID lot = lot(c.pa1(), "LR");
        fixture.decompte(lot, c.code("LR") + "-D1", 1, "0", "0", "70", null);
        fixture.decompte(lot, c.code("LR") + "-D2", 2, "0", "0", "0", jour(2));

        List<Map<String, Object>> l = lignesOu(section(sections(c.pa1()), REMBOURSEMENTS), CODE, c.code("LR"));

        assertThat(l).as("le décompte à remboursement nul est absent").hasSize(1);
        Map<String, Object> r = l.getFirst();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(r, REMBOURSE)).isEqualByComparingTo("70");
            s.assertThat(r).containsEntry(DECOMPTE, "Decompte " + c.code("LR") + "-D1");
            s.assertThat(r.get("date")).as("null reste null, jamais 1900-01-01").isNull();
            s.assertThat(r).containsEntry("projet", "PA1-" + c.jeton());
            s.assertThat(r).containsEntry("fournisseur", c.nom());
        });
    }

    @Test
    void r4_remboursements_triDate_etTotal() throws Exception {
        UUID lot = lot(c.pa1(), "LD");
        fixture.decompte(lot, c.code("LD") + "-D9", 1, "0", "0", "40", jour(9));
        fixture.decompte(lot, c.code("LD") + "-D3", 2, "0", "0", "25", jour(3));

        Map<String, Object> section = section(sections(c.pa1()), REMBOURSEMENTS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes(section).stream().map(l -> l.get(DECOMPTE)).toList())
                    .containsExactly("Decompte " + c.code("LD") + "-D3", "Decompte " + c.code("LD") + "-D9");
            s.assertThat(nombre(totaux(section).get(REMBOURSE))).isEqualByComparingTo("65");
        });
    }
}
