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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.creees;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.ApiCommissions.nombre;
import static com.tsix.immocrm.fixtures.ApiCommissions.raisons;
import static com.tsix.immocrm.fixtures.ApiCommissions.suppression;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.horodatage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Génération des commissions de concrétisation — FEATURE-vente-commissions.md §2 R-S / R-P / R-M, §3.1 C1, C2, C8, C9,
 * C10, §4 (generation), §8.1 « Calcul » et « C1 ». Personne = {@code ComFolder.CfgCommercialId}, date du fait =
 * {@code AdvanceDate}, base = {@code PriceAfterDiscount}, pourcentage stocké en ratio. Les niveaux « action commerciale »
 * de la priorité ne sont pas testés (source de l'action du dossier absente du spec, voir le rapport).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionGenerationTest {

    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final BigDecimal PRIX = new BigDecimal("775000");
    private static final String MONTANT_FIXE = "SaleCommissionBasedOnAmount";
    private static final String MONTANT = "SaleCommissionAmount";
    private static final String DEBUT = "StartDate";
    private static final String FIN = "EndDate";
    private static final String COMMERCIAL = "CfgTierId";
    private static final String CATEGORIE = "StkItemCategoryId";
    private static final String COMMISSION_AMOUNT = "CommissionAmount";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;

    private Contexte c;
    private RequestPostProcessor gestionnaire;
    private UUID commercial;
    private UUID tranche;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMG");
        tranche = c.tranche().getId();
        gestionnaire = fixture.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU,
                FixtureCommissions.SUPPRIMER, FixtureCommissions.IMPRIMER).qui();
        commercial = fixture.personne("COM-" + c.jeton(), false);
    }

    // ------------------------------------------------------------------ outils

    private Dossier dossier(String code) {
        return fixture.dossier(c, code, commercial, PRIX, AVANCE);
    }

    private ResultActions generer(Dossier... dossiers) throws Exception {
        return mockMvc.perform(generation(tranche, T_CONCRETISATION, Stream.of(dossiers).map(Dossier::id).toList())
                .with(gestionnaire));
    }

    private BigDecimal seulMontant(Dossier d) {
        List<Tuple> lignes = fixture.lignesDuDossier(d.id());
        assertThat(lignes).as("lignes du dossier %s", d.code()).hasSize(1);
        return decimal(lignes.get(0).get(COMMISSION_AMOUNT));
    }

    private void montantFixe(Object... criteresPuisMontant) {
        List<Object> colonnes = new ArrayList<>(List.of(criteresPuisMontant).subList(0, criteresPuisMontant.length - 1));
        colonnes.addAll(List.of(MONTANT_FIXE, true, MONTANT, criteresPuisMontant[criteresPuisMontant.length - 1]));
        fixture.regle(tranche, colonnes.toArray());
    }

    // ------------------------------------------------------------------ R-M : montant

    /** §1 / §2 R-M : 0,03 % (ratio 0,0003) × 775 000 = 232,50 ; mode « 0.03 % du prix de vente TTC » ; ligne complète. */
    @Test
    void rm_pourcentage_ligneDeLaProd_232_50_modeCalculEtColonnes() throws Exception {
        fixture.regle(tranche, MONTANT_FIXE, false, "SaleCommissionPercent", new BigDecimal("0.0003"));
        Dossier d = dossier("D1");

        ResultActions r = generer(d).andExpect(status().isOk());

        Tuple l = fixture.lignesDuDossier(d.id()).getFirst();
        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).hasSize(1),
                () -> assertThat(l.get("CommissionType")).isEqualTo(CONCRETISATION),
                () -> assertThat(decimal(l.get(COMMISSION_AMOUNT))).isEqualByComparingTo("232.5"),
                () -> assertThat(l.get("CalculationMethod")).isEqualTo("0.03 % du prix de vente TTC"),
                () -> assertThat(horodatage(l.get("DocDate"))).isEqualTo(AVANCE.atStartOfDay()),
                () -> assertThat(guid(l.get("CfgEmployeeId"))).isEqualTo(guid(commercial)),
                () -> assertThat(guid(l.get("CfgTrancheId"))).isEqualTo(guid(tranche)),
                () -> assertThat(guid(l.get("StkItemId"))).isEqualTo(guid(d.articleId())),
                () -> assertThat(guid(l.get("StkItemCategoryId"))).isEqualTo(guid(d.categorieId())),
                () -> assertThat(l.get("BuyDocumentId")).isNull(),
                () -> assertThat(creees(r)).hasSize(1),
                () -> assertThat(nombre(r, "$.creees[0].montant")).isEqualByComparingTo("232.5"),
                () -> assertThat((String) lire(r, "$.creees[0].modeCalcul")).isEqualTo("0.03 % du prix de vente TTC"));
    }

    /** R-M « Aucun arrondi » : 0,0003 × 1 000 001 = 300,0003. */
    @Test
    void rm_pourcentage_sansArrondi() throws Exception {
        fixture.regle(tranche, MONTANT_FIXE, false, "SaleCommissionPercent", new BigDecimal("0.0003"));
        Dossier d = fixture.dossier(c, "D1", commercial, new BigDecimal("1000001"), AVANCE);

        generer(d).andExpect(status().isOk());

        assertThat(seulMontant(d)).isEqualByComparingTo("300.0003");
    }

    @Test
    void rm_montantFixe_modeCalculMontant() throws Exception {
        montantFixe(new BigDecimal("1500"));
        Dossier d = dossier("D1");

        generer(d).andExpect(status().isOk());

        Tuple l = fixture.lignesDuDossier(d.id()).getFirst();
        assertAll(
                () -> assertThat(decimal(l.get(COMMISSION_AMOUNT))).isEqualByComparingTo("1500"),
                () -> assertThat(l.get("CalculationMethod")).isEqualTo("Montant"));
    }

    /** R-M : personne {@code IsExtern} → colonnes « Extern » de la règle ; interne → colonnes normales. */
    @Test
    void rm_externe_colonnesExtern_interne_colonnesNormales() throws Exception {
        fixture.regle(tranche, MONTANT_FIXE, true, MONTANT, new BigDecimal("1000"),
                "SaleCommissionExternBasedOnAmount", true, "SaleCommissionAmountExtern", new BigDecimal("2500"));
        Dossier interne = dossier("DI");
        Dossier externe = fixture.dossier(c, "DE", fixture.personne("EXT-" + c.jeton(), true), PRIX, AVANCE);

        generer(interne, externe).andExpect(status().isOk());

        assertAll(
                () -> assertThat(seulMontant(interne)).isEqualByComparingTo("1000"),
                () -> assertThat(seulMontant(externe)).isEqualByComparingTo("2500"));
    }

    /** R-M / C2 : « Une ligne n'est écrite que si le montant > 0 ». */
    @Test
    void rm_montantNul_aucuneLigne_MONTANT_NUL() throws Exception {
        montantFixe(BigDecimal.ZERO);
        Dossier d = dossier("D1");

        ResultActions r = generer(d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("MONTANT_NUL"));
    }

    /** §8.1 « prix NULL » : pourcentage d'un prix absent = montant nul, aucune ligne. */
    @Test
    void rm_prixNull_pourcentage_aucuneLigne_MONTANT_NUL() throws Exception {
        fixture.regle(tranche, MONTANT_FIXE, false, "SaleCommissionPercent", new BigDecimal("0.0003"));
        Dossier d = fixture.dossier(c, "D1", commercial, null, AVANCE);

        ResultActions r = generer(d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("MONTANT_NUL"));
    }

    // ------------------------------------------------------------------ R-P : personne, date

    @Test
    void rp_sansCommercial_aucuneLigne_SANS_PERSONNE() throws Exception {
        montantFixe(new BigDecimal("100"));
        Dossier d = fixture.dossier(c, "D1", null, PRIX, AVANCE);

        ResultActions r = generer(d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("SANS_PERSONNE"));
    }

    @Test
    void rs_sansDateDeConcretisation_aucuneLigne_SANS_DATE() throws Exception {
        montantFixe(new BigDecimal("100"));
        Dossier d = fixture.dossier(c, "D1", commercial, PRIX, null);

        ResultActions r = generer(d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("SANS_DATE"));
    }

    // ------------------------------------------------------------------ R-S : priorité (niveaux sans action commerciale)

    @Test
    void rs_priorite_commercialEtCategorie_avantCommercial_avantCategorie_avantRien() throws Exception {
        montantFixe(new BigDecimal("100"));
        montantFixe(CATEGORIE, c.categorie().getId(), new BigDecimal("200"));
        montantFixe(COMMERCIAL, commercial, new BigDecimal("300"));
        montantFixe(COMMERCIAL, commercial, CATEGORIE, c.categorie().getId(), new BigDecimal("400"));
        Dossier d = dossier("D1");

        generer(d).andExpect(status().isOk());

        assertThat(seulMontant(d)).isEqualByComparingTo("400");
    }

    @Test
    void rs_priorite_commercialSeul_avantCategorieSeule() throws Exception {
        montantFixe(new BigDecimal("100"));
        montantFixe(CATEGORIE, c.categorie().getId(), new BigDecimal("200"));
        montantFixe(COMMERCIAL, commercial, new BigDecimal("300"));
        Dossier d = dossier("D1");

        generer(d).andExpect(status().isOk());

        assertThat(seulMontant(d)).isEqualByComparingTo("300");
    }

    @Test
    void rs_priorite_categorieSeule_avantRegleSansCritere() throws Exception {
        montantFixe(new BigDecimal("100"));
        montantFixe(CATEGORIE, c.categorie().getId(), new BigDecimal("200"));
        Dossier d = dossier("D1");

        generer(d).andExpect(status().isOk());

        assertThat(seulMontant(d)).isEqualByComparingTo("200");
    }

    /** Une règle d'un autre commercial ou d'une autre catégorie ne s'applique pas : repli sur la règle sans critère. */
    @Test
    void rs_regleDUnAutreCommercialOuDUneAutreCategorie_ignoree() throws Exception {
        montantFixe(new BigDecimal("100"));
        montantFixe(COMMERCIAL, fixture.personne("AUTRE-" + c.jeton(), false), new BigDecimal("900"));
        montantFixe(CATEGORIE, fixture.contexte("COMX").categorie().getId(), new BigDecimal("800"));
        Dossier d = dossier("D1");

        generer(d).andExpect(status().isOk());

        assertThat(seulMontant(d)).isEqualByComparingTo("100");
    }

    /** R-S : seules les règles sans {@code ComEmployeeId} (support, hors lot). */
    @Test
    void rs_regleAvecComEmployeeId_ignoree_SANS_REGLE() throws Exception {
        montantFixe("ComEmployeeId", fixture.personne("SUP-" + c.jeton(), false), new BigDecimal("100"));
        Dossier d = dossier("D1");

        ResultActions r = generer(d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("SANS_REGLE"));
    }

    // ------------------------------------------------------------------ R-S : fenêtre de dates (bornes incluses)

    @Test
    void rs_fenetre_deuxBornes_incluses() throws Exception {
        montantFixe(DEBUT, LocalDate.of(2024, 1, 1), FIN, LocalDate.of(2024, 12, 31), new BigDecimal("100"));
        Dossier debut = fixture.dossier(c, "DD", commercial, PRIX, LocalDate.of(2024, 1, 1));
        Dossier fin = fixture.dossier(c, "DF", commercial, PRIX, LocalDate.of(2024, 12, 31));

        generer(debut, fin).andExpect(status().isOk());

        assertAll(
                () -> assertThat(seulMontant(debut)).isEqualByComparingTo("100"),
                () -> assertThat(seulMontant(fin)).isEqualByComparingTo("100"));
    }

    @Test
    void rs_fenetre_horsDesBornes_SANS_REGLE() throws Exception {
        montantFixe(DEBUT, LocalDate.of(2024, 1, 1), FIN, LocalDate.of(2024, 12, 31), new BigDecimal("100"));
        Dossier avant = fixture.dossier(c, "DA", commercial, PRIX, LocalDate.of(2023, 12, 31));
        Dossier apres = fixture.dossier(c, "DP", commercial, PRIX, LocalDate.of(2025, 1, 1));

        ResultActions r = generer(avant, apres).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(avant.id())).isEmpty(),
                () -> assertThat(fixture.lignesDuDossier(apres.id())).isEmpty(),
                () -> assertThat(raisons(r, avant.id())).containsExactly("SANS_REGLE"),
                () -> assertThat(raisons(r, apres.id())).containsExactly("SANS_REGLE"));
    }

    @Test
    void rs_fenetre_debutSeul_ouverteApres() throws Exception {
        montantFixe(DEBUT, LocalDate.of(2024, 6, 1), new BigDecimal("100"));
        Dossier dedans = fixture.dossier(c, "DIN", commercial, PRIX, LocalDate.of(2030, 1, 1));
        Dossier avant = fixture.dossier(c, "DAV", commercial, PRIX, LocalDate.of(2024, 5, 31));

        ResultActions r = generer(dedans, avant).andExpect(status().isOk());

        assertAll(
                () -> assertThat(seulMontant(dedans)).isEqualByComparingTo("100"),
                () -> assertThat(raisons(r, avant.id())).containsExactly("SANS_REGLE"));
    }

    @Test
    void rs_fenetre_finSeule_ouverteAvant() throws Exception {
        montantFixe(FIN, LocalDate.of(2024, 6, 1), new BigDecimal("100"));
        Dossier dedans = fixture.dossier(c, "DIN", commercial, PRIX, LocalDate.of(2000, 1, 1));
        Dossier apres = fixture.dossier(c, "DAP", commercial, PRIX, LocalDate.of(2024, 6, 2));

        ResultActions r = generer(dedans, apres).andExpect(status().isOk());

        assertAll(
                () -> assertThat(seulMontant(dedans)).isEqualByComparingTo("100"),
                () -> assertThat(raisons(r, apres.id())).containsExactly("SANS_REGLE"));
    }

    // ------------------------------------------------------------------ R-S : départage à priorité égale

    @Test
    void rs_departage_deuxBornes_avantDebutSeul_avantFinSeule_avantAucune() throws Exception {
        montantFixe(new BigDecimal("100"));
        montantFixe(FIN, LocalDate.of(2030, 1, 1), new BigDecimal("200"));
        montantFixe(DEBUT, LocalDate.of(2000, 1, 1), new BigDecimal("300"));
        montantFixe(DEBUT, LocalDate.of(2000, 1, 1), FIN, LocalDate.of(2030, 1, 1), new BigDecimal("400"));
        Dossier d = dossier("D1");

        generer(d).andExpect(status().isOk());

        assertThat(seulMontant(d)).isEqualByComparingTo("400");
    }

    @Test
    void rs_departage_debutSeul_avantFinSeule() throws Exception {
        montantFixe(FIN, LocalDate.of(2030, 1, 1), new BigDecimal("200"));
        montantFixe(DEBUT, LocalDate.of(2000, 1, 1), new BigDecimal("300"));
        Dossier d = dossier("D1");

        generer(d).andExpect(status().isOk());

        assertThat(seulMontant(d)).isEqualByComparingTo("300");
    }

    @Test
    void rs_departage_finSeule_avantAucuneBorne() throws Exception {
        montantFixe(new BigDecimal("100"));
        montantFixe(FIN, LocalDate.of(2030, 1, 1), new BigDecimal("200"));
        Dossier d = dossier("D1");

        generer(d).andExpect(status().isOk());

        assertThat(seulMontant(d)).isEqualByComparingTo("200");
    }

    // ------------------------------------------------------------------ C1 : une commission d'un type par dossier et personne

    @Test
    void c1_doubleGeneration_uneSeuleLigne_DEJA_GENERE() throws Exception {
        montantFixe(new BigDecimal("100"));
        Dossier d = dossier("D1");
        generer(d).andExpect(status().isOk());

        ResultActions r = generer(d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).hasSize(1),
                () -> assertThat(creees(r)).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("DEJA_GENERE"));
    }

    @Test
    void c1_suppressionPuisRegeneration_nouvelleLigne() throws Exception {
        montantFixe(new BigDecimal("100"));
        Dossier d = dossier("D1");
        generer(d).andExpect(status().isOk());
        UUID premiere = UUID.fromString(guid(fixture.lignesDuDossier(d.id()).getFirst().get("PKey")));
        mockMvc.perform(suppression(List.of(premiere)).with(gestionnaire)).andExpect(status().isNoContent());

        ResultActions r = generer(d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(creees(r)).hasSize(1),
                () -> assertThat(fixture.lignesDuDossier(d.id())).singleElement()
                        .satisfies(l -> assertThat(guid(l.get("PKey"))).isNotEqualTo(guid(premiere))));
    }

    /** C1 : l'unicité porte sur (dossier, type, personne) — une autre personne sur le même type reste permise. */
    @Test
    void c1_autrePersonneDejaCommissionnee_memeType_ligneCreeePourLeCommercial() throws Exception {
        montantFixe(new BigDecimal("100"));
        Dossier d = dossier("D1");
        fixture.ligne(d, CONCRETISATION, fixture.personne("ANC-" + c.jeton(), false), new BigDecimal("50"), AVANCE, null);

        generer(d).andExpect(status().isOk());

        assertThat(fixture.lignesDuDossier(d.id(), CONCRETISATION)).hasSize(2)
                .anySatisfy(l -> assertThat(guid(l.get("CfgEmployeeId"))).isEqualTo(guid(commercial)));
    }

    // ------------------------------------------------------------------ C8, C9, §4 : lot de dossiers

    /** C9 : les ignorés ne sont pas des erreurs ; ils sont listés avec leur code, les autres sont écrits. */
    @Test
    void c9_lotMixte_creeesEtIgnoresListes() throws Exception {
        montantFixe(new BigDecimal("100"));
        Dossier ok = dossier("DOK");
        Dossier sansPersonne = fixture.dossier(c, "DSP", null, PRIX, AVANCE);

        ResultActions r = generer(ok, sansPersonne).andExpect(status().isOk())
                .andExpect(jsonPath("$.creees.length()").value(1))
                .andExpect(jsonPath("$.ignores.length()").value(1));

        assertAll(
                () -> assertThat(seulMontant(ok)).isEqualByComparingTo("100"),
                () -> assertThat((String) lire(r, "$.ignores[0].dossierCode")).isEqualTo(sansPersonne.code()),
                () -> assertThat((String) lire(r, "$.ignores[0].dossierId")).isEqualTo(sansPersonne.id().toString()));
    }

    /** C8 : chaque {@code dossierId} doit appartenir à la tranche du chemin, sinon 400 et rien d'écrit. */
    @Test
    void c8_dossierDUneAutreTranche_400_rienEcrit() throws Exception {
        montantFixe(new BigDecimal("100"));
        Dossier ici = dossier("DICI");
        Dossier ailleurs = fixture.dossier(c, fixture.autreTranche(c, "AUT", true), "DAIL", commercial, PRIX, AVANCE);

        generer(ici, ailleurs).andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(ici.id())).isEmpty(),
                () -> assertThat(fixture.lignesDuDossier(ailleurs.id())).isEmpty());
    }

    /** §4 : {@code dossierIds} ≤ 500. */
    @Test
    void p4_plusDe500Dossiers_400() throws Exception {
        List<UUID> ids = Stream.generate(UUID::randomUUID).limit(501).toList();

        mockMvc.perform(generation(tranche, T_CONCRETISATION, ids).with(gestionnaire))
                .andExpect(status().isBadRequest());
    }

    /** C10 : la génération n'écrit jamais {@code ComSalesCommission}. */
    @Test
    void c10_reglesInchangees() throws Exception {
        montantFixe(new BigDecimal("100"));
        Dossier d = dossier("D1");
        String sql = "SELECT COUNT(*) FROM ComSalesCommission WHERE CfgTrancheId = :id AND UserUpdateDateTime IS NULL "
                + "AND SaleCommissionAmount = 100";

        generer(d).andExpect(status().isOk());

        assertThat(fixture.compter(sql, tranche)).isEqualTo(1);
    }
}
