package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
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

import static com.tsix.immocrm.fixtures.ApiCommissions.DOSSIERS;
import static com.tsix.immocrm.fixtures.ApiCommissions.FACTURES;
import static com.tsix.immocrm.fixtures.ApiCommissions.LIGNES;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_DESISTEMENT;
import static com.tsix.immocrm.fixtures.ApiCommissions.TRANCHES;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.ApiCommissions.nombre;
import static com.tsix.immocrm.fixtures.ApiCommissions.tranche;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixtureCommissions.ECRAN;
import static com.tsix.immocrm.fixtures.FixtureCommissions.FINALISATION_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Soldes et listes — FEATURE-vente-commissions.md §3.1 C7 (une seule agrégation : généré / réglé / reste, mêmes totaux en
 * liste, en-tête et lignes), Q5 décidé « prorata » (réglé d'un document réparti au prorata du montant des lignes), §6
 * (règlements hors {@code CANCELED}), Q8 (dossiers sans statut listés), §4 (filtres {@code type}, {@code du}, {@code au},
 * {@code dossier}), §8.1 « Soldes (C7) ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionSoldesTest {

    private static final LocalDate JOUR = LocalDate.of(2024, 2, 23);
    private static final BigDecimal PRIX = new BigDecimal("775000");
    private static final BigDecimal TROIS_CENTS = new BigDecimal("300");
    private static final BigDecimal CENT = new BigDecimal("100");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;

    private Contexte c;
    private RequestPostProcessor qui;
    private UUID commercial;
    private Dossier d1;
    private Dossier d2;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMSO");
        qui = fixture.compte(c.jeton(), ECRAN).qui();
        commercial = fixture.personne("COM-" + c.jeton(), false);
        d1 = fixture.dossier(c, "D1", commercial, PRIX, JOUR);
        d2 = fixture.dossier(c, "D2", commercial, PRIX, JOUR);
    }

    private ResultActions entete(CfgTranche t) throws Exception {
        return mockMvc.perform(tranche(t.getId()).with(qui)).andExpect(status().isOk());
    }

    private ResultActions lignes(CfgTranche t, String... filtres) throws Exception {
        var requete = get(LIGNES, t.getId()).with(qui);
        for (int i = 0; i + 1 < filtres.length; i += 2) {
            requete.param(filtres[i], filtres[i + 1]);
        }
        return mockMvc.perform(requete).andExpect(status().isOk());
    }

    private ResultActions dossiers(String... filtres) throws Exception {
        var requete = get(DOSSIERS, c.tranche().getId()).with(qui);
        for (int i = 0; i + 1 < filtres.length; i += 2) {
            requete.param(filtres[i], filtres[i + 1]);
        }
        return mockMvc.perform(requete).andExpect(status().isOk());
    }

    private static BigDecimal deLaLigne(ResultActions r, UUID ligne, String champ) throws Exception {
        List<Object> v = lire(r, "$[?(@.id == '" + ligne + "')]." + champ);
        assertThat(v).as("%s de la ligne %s", champ, ligne).hasSize(1);
        return new BigDecimal(String.valueOf(v.get(0)));
    }

    /** Document de 400 (300 + 100) réglé 200, plus un règlement annulé de 50 (ignoré). */
    private List<UUID> documentRegle(Dossier dA, Dossier dB) {
        UUID doc = fixture.document(commercial, "DOC-" + c.jeton(), new BigDecimal("400"));
        List<UUID> l = List.of(fixture.ligne(dA, CONCRETISATION, commercial, TROIS_CENTS, JOUR, doc),
                fixture.ligne(dB, CONCRETISATION, commercial, CENT, JOUR, doc));
        fixture.reglement(doc, new BigDecimal("200"), "IN_WORK");
        fixture.reglement(doc, new BigDecimal("50"), "CANCELED");
        return l;
    }

    // ------------------------------------------------------------------ C7 / Q5

    @Test
    void c7_enTete_genereRegleReste_annuleExclu() throws Exception {
        documentRegle(d1, d2);

        ResultActions r = entete(c.tranche());

        assertAll(
                () -> assertThat(nombre(r, "$.montantGenere")).isEqualByComparingTo("400"),
                () -> assertThat(nombre(r, "$.montantRegle")).isEqualByComparingTo("200"),
                () -> assertThat(nombre(r, "$.montantReste")).isEqualByComparingTo("200"),
                () -> assertThat((String) lire(r, "$.trancheId")).isEqualTo(c.tranche().getId().toString()),
                () -> assertThat((String) lire(r, "$.code")).isEqualTo(c.tranche().getCode()));
    }

    @Test
    void q5_regleRepartiAuProrataDesLignes() throws Exception {
        List<UUID> l = documentRegle(d1, d2);

        ResultActions r = lignes(c.tranche());

        assertAll(
                () -> assertThat(deLaLigne(r, l.get(0), "regle")).isEqualByComparingTo("150"),
                () -> assertThat(deLaLigne(r, l.get(0), "reste")).isEqualByComparingTo("150"),
                () -> assertThat(deLaLigne(r, l.get(1), "regle")).isEqualByComparingTo("50"),
                () -> assertThat(deLaLigne(r, l.get(1), "reste")).isEqualByComparingTo("50"));
    }

    @Test
    void c4_ligneDocumentee_nonModifiable_codeDuDocument() throws Exception {
        List<UUID> l = documentRegle(d1, d2);
        UUID libre = fixture.ligne(d1, FINALISATION_1, commercial, CENT, JOUR, null);

        ResultActions r = lignes(c.tranche());

        assertAll(
                () -> assertThat((List<Object>) lire(r, "$[?(@.id == '" + l.get(0) + "')].modifiable")).containsExactly(false),
                () -> assertThat((List<Object>) lire(r, "$[?(@.id == '" + l.get(0) + "')].documentCode"))
                        .containsExactly("DOC-" + c.jeton()),
                () -> assertThat((List<Object>) lire(r, "$[?(@.id == '" + libre + "')].modifiable")).containsExactly(true),
                () -> assertThat(deLaLigne(r, libre, "regle")).isEqualByComparingTo("0"),
                () -> assertThat(deLaLigne(r, libre, "reste")).isEqualByComparingTo("100"));
    }

    /** C7 : deux tranches sur un même document — chacune reçoit sa part du réglé. */
    @Test
    void c7_documentSurDeuxTranches_regleParTranche() throws Exception {
        CfgTranche autre = fixture.autreTranche(c, "T2", true);
        Dossier ailleurs = fixture.dossier(c, autre, "D3", commercial, PRIX, JOUR);
        documentRegle(d1, ailleurs);

        ResultActions t1 = entete(c.tranche());
        ResultActions t2 = entete(autre);

        assertAll(
                () -> assertThat(nombre(t1, "$.montantGenere")).isEqualByComparingTo("300"),
                () -> assertThat(nombre(t1, "$.montantRegle")).isEqualByComparingTo("150"),
                () -> assertThat(nombre(t2, "$.montantGenere")).isEqualByComparingTo("100"),
                () -> assertThat(nombre(t2, "$.montantRegle")).isEqualByComparingTo("50"),
                () -> assertThat(nombre(t2, "$.montantReste")).isEqualByComparingTo("50"));
    }

    @Test
    void c7_documentNonRegle_regleZero_resteMontant() throws Exception {
        UUID doc = fixture.document(commercial, "DOC-" + c.jeton(), TROIS_CENTS);
        fixture.ligne(d1, CONCRETISATION, commercial, TROIS_CENTS, JOUR, doc);

        ResultActions factures = mockMvc.perform(get(FACTURES, c.tranche().getId()).with(qui)).andExpect(status().isOk());
        ResultActions r = entete(c.tranche());

        assertAll(
                () -> assertThat(nombre(r, "$.montantRegle")).isEqualByComparingTo("0"),
                () -> assertThat(nombre(r, "$.montantReste")).isEqualByComparingTo("300"),
                () -> assertThat(nombre(factures, "$[0].montant")).isEqualByComparingTo("300"),
                () -> assertThat(nombre(factures, "$[0].regle")).isEqualByComparingTo("0"),
                () -> assertThat(nombre(factures, "$[0].reste")).isEqualByComparingTo("300"));
    }

    /** C7 : la liste des tranches et l'en-tête donnent les mêmes soldes ; désistements compris dans le généré. */
    @Test
    void c7_listeEgaleEnTete_desistementCompris() throws Exception {
        documentRegle(d1, d2);
        fixture.ligne(d1, DESISTEMENT, commercial, new BigDecimal("-30"), JOUR, null);

        ResultActions liste = mockMvc.perform(get(TRANCHES).with(qui)).andExpect(status().isOk());
        ResultActions r = entete(c.tranche());

        String filtre = "$[?(@.trancheId == '" + c.tranche().getId() + "')].";
        List<Object> genere = lire(liste, filtre + "montantGenere");
        List<Object> regle = lire(liste, filtre + "montantRegle");
        List<Object> reste = lire(liste, filtre + "montantReste");
        assertAll(
                () -> assertThat(nombre(r, "$.montantGenere")).isEqualByComparingTo("370"),
                () -> assertThat(genere).hasSize(1),
                () -> assertThat(new BigDecimal(String.valueOf(genere.get(0)))).isEqualByComparingTo(nombre(r, "$.montantGenere")),
                () -> assertThat(new BigDecimal(String.valueOf(regle.get(0)))).isEqualByComparingTo(nombre(r, "$.montantRegle")),
                () -> assertThat(new BigDecimal(String.valueOf(reste.get(0)))).isEqualByComparingTo(nombre(r, "$.montantReste")));
    }

    @Test
    void p4_facturesDeLaTranche_regleHorsAnnules() throws Exception {
        documentRegle(d1, d2);

        ResultActions r = mockMvc.perform(get(FACTURES, c.tranche().getId()).with(qui)).andExpect(status().isOk());

        assertAll(
                () -> assertThat((List<Object>) lire(r, "$")).hasSize(1),
                () -> assertThat(nombre(r, "$[0].montant")).isEqualByComparingTo("400"),
                () -> assertThat(nombre(r, "$[0].regle")).isEqualByComparingTo("200"),
                () -> assertThat(nombre(r, "$[0].reste")).isEqualByComparingTo("200"),
                () -> assertThat((String) lire(r, "$[0].commercialId")).isEqualTo(commercial.toString()));
    }

    // ------------------------------------------------------------------ onglet Dossiers

    @Test
    void p4_dossiers_sommesParType() throws Exception {
        fixture.ligne(d1, CONCRETISATION, commercial, TROIS_CENTS, JOUR, null);
        fixture.ligne(d1, FINALISATION_1, commercial, new BigDecimal("500"), JOUR, null);
        fixture.ligne(d1, DESISTEMENT, commercial, TROIS_CENTS.negate(), JOUR, null);

        ResultActions r = dossiers();

        String filtre = "$[?(@.dossierId == '" + d1.id() + "')].";
        assertAll(
                () -> assertThat(un(r, filtre + "commissionConcretisation")).isEqualByComparingTo("300"),
                () -> assertThat(un(r, filtre + "commissionFinalisation1")).isEqualByComparingTo("500"),
                () -> assertThat(un(r, filtre + "commissionDesistement")).isEqualByComparingTo("-300"),
                () -> assertThat(un(r, filtre + "prixVenteTtc")).isEqualByComparingTo("775000"));
    }

    private static BigDecimal un(ResultActions r, String chemin) throws Exception {
        List<Object> v = lire(r, chemin);
        assertThat(v).as(chemin).hasSize(1);
        return new BigDecimal(String.valueOf(v.get(0)));
    }

    /** Q8 : un dossier sans statut est listé. */
    @Test
    void q8_dossierSansStatut_liste() throws Exception {
        Dossier sansStatut = fixture.dossier(c, c.tranche(), "DSS", null, commercial, PRIX, JOUR);

        ResultActions r = dossiers();

        assertThat((List<String>) lire(r, "$[*].dossierId")).contains(sansStatut.id().toString(), d1.id().toString());
    }

    /** §5 / §4 : {@code type} ne garde que les dossiers qui ont la date du type ; {@code du} / {@code au} bornes incluses. */
    @Test
    void p4_dossiers_filtreTypeEtPeriode_bornesIncluses() throws Exception {
        Dossier sansDate = fixture.dossier(c, "DND", commercial, PRIX, null);
        Dossier avant = fixture.dossier(c, "DAV", commercial, PRIX, LocalDate.of(2024, 2, 22));
        Dossier fin = fixture.dossier(c, "DFI", commercial, PRIX, LocalDate.of(2024, 3, 31));

        ResultActions r = dossiers("type", T_CONCRETISATION, "du", "2024-02-23", "au", "2024-03-31");

        assertThat((List<String>) lire(r, "$[*].dossierId"))
                .contains(d1.id().toString(), d2.id().toString(), fin.id().toString())
                .doesNotContain(sansDate.id().toString(), avant.id().toString());
    }

    @Test
    void p4_dossiers_rechercheParCodeDeDossier() throws Exception {
        ResultActions r = dossiers("dossier", d1.code());

        assertThat((List<String>) lire(r, "$[*].dossierId")).contains(d1.id().toString())
                .doesNotContain(d2.id().toString());
    }

    @Test
    void p4_lignes_filtreType() throws Exception {
        UUID concret = fixture.ligne(d1, CONCRETISATION, commercial, TROIS_CENTS, JOUR, null);
        UUID desist = fixture.ligne(d1, DESISTEMENT, commercial, TROIS_CENTS.negate(), JOUR, null);

        ResultActions r = lignes(c.tranche(), "type", T_DESISTEMENT);

        assertThat((List<String>) lire(r, "$[*].id")).contains(desist.toString()).doesNotContain(concret.toString());
    }

    @Test
    void p4_listeDesTranches_rechercheSurLeCode() throws Exception {
        CfgTranche autre = fixture.autreTranche(c, "ZZ", true);

        ResultActions r = mockMvc.perform(get(TRANCHES).param("recherche", c.tranche().getCode()).with(qui))
                .andExpect(status().isOk());

        assertThat((List<String>) lire(r, "$[*].trancheId")).contains(c.tranche().getId().toString())
                .doesNotContain(autre.getId().toString());
    }
}
