package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.Ligne;
import com.tsix.immocrm.fixtures.FixtureDecompteExtras;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.champs;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.corps;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.nombre;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** LC6 R1-R3, R5 : écriture des travaux supplémentaires par {@code PUT /api/projet/decomptes/{id}} (liste = état final). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesExtrasTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String EXTRAS = "extras";
    private static final String DESIGNATION = "designation";
    private static final String QUANTITE = "quantite";
    private static final String PU = "prixUnitaireHT";
    private static final String TVA = "tauxTVA";
    private static final String LIGNE_MARCHE = "ligneMarcheId";
    private static final String DESCRIPTION = "Description";
    private static final String QUANTITY = "Quantity";
    private static final String FODEC = "FODECRatio";
    private static final String TAUX_FODEC = "tauxFODEC";
    private static final String CATEGORIE = "categorie";
    private static final String GARDEE = "Gardée";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private FixtureDecompteExtras extras;
    @Autowired private FixtureBordereauxEcriture bordereaux;

    private String jeton;
    private UUID projet;
    private UUID lot;
    private UUID d;
    private RequestPostProcessor admin;
    private RequestPostProcessor modifier;

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PXE-" + jeton);
        lot = fixture.lot(projet, "LXE-" + jeton, fixture.fournisseur("FXE-" + jeton));
        AdmUser compte = vente.compte("prj.xe.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
        modifier = decomptes.compte(projet, jeton, DROIT_MODIFIER);
        d = decompte(lot, "XE1-", 1);
    }

    private UUID decompte(UUID lotId, String code, int numero) {
        return fixture.decompte(lotId, code + jeton, numero,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
    }

    private MockHttpServletResponse executer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r;
    }

    private MockHttpServletResponse enregistrer(UUID decompte, String corps) throws Exception {
        return executer(put(DECOMPTES + "/" + decompte).contentType(MediaType.APPLICATION_JSON).content(corps), modifier);
    }

    private int enregistrerExtras(UUID decompte, Map<?, ?>... lignes) throws Exception {
        return enregistrer(decompte, corps(EXTRAS, List.of(lignes))).getStatus();
    }

    private static Map<String, Object> libre(String designation, String quantite) {
        return champs(DESIGNATION, designation, QUANTITE, new BigDecimal(quantite), PU, new BigDecimal("100"),
                TVA, new BigDecimal("20"));
    }

    private Map<String, Object> seuleExtra(UUID decompte) {
        List<Map<String, Object>> lignes = extras.extras(decompte);
        assertThat(lignes).hasSize(1);
        return lignes.getFirst();
    }

    private UUID ligneDefinitive(UUID categorie) {
        return bordereaux.ligne(LIGNES_DEFINITIF, new Ligne(lot, null, 1, "B1", "10", "250.50", categorie, false));
    }

    // ------------------------------------------------------------------ R1 liste = état final

    @Test
    void r1_extraSansId_creee() throws Exception {
        int statut = enregistrerExtras(d, libre("Peinture", "2"));

        assertThat(statut).isEqualTo(200);
        Map<String, Object> e = seuleExtra(d);
        assertThat(e).containsEntry(DESCRIPTION, "Peinture");
        assertThat(nombre(e, QUANTITY)).isEqualByComparingTo("2");
    }

    @Test
    void r1_extraAvecId_miseAJour_memeLigne() throws Exception {
        UUID e1 = extras.extra(d, 1, "Ancienne");
        Map<String, Object> ligne = libre("Nouvelle", "3");
        ligne.put("id", e1.toString());

        int statut = enregistrerExtras(d, ligne);

        assertThat(statut).isEqualTo(200);
        Map<String, Object> e = seuleExtra(d);
        assertThat(e).containsEntry("id", e1.toString()).containsEntry(DESCRIPTION, "Nouvelle");
        assertThat(nombre(e, QUANTITY)).isEqualByComparingTo("3");
    }

    @Test
    void r1_extraAbsenteDeLaListe_supprimee() throws Exception {
        UUID e1 = extras.extra(d, 1, GARDEE);
        extras.extra(d, 2, "Retirée");
        Map<String, Object> ligne = libre(GARDEE, "1");
        ligne.put("id", e1.toString());

        int statut = enregistrerExtras(d, ligne);

        assertThat(statut).isEqualTo(200);
        assertThat(seuleExtra(d)).containsEntry("id", e1.toString());
    }

    @Test
    void r1_listeVide_supprimeToutesLesExtras() throws Exception {
        extras.extra(d, 1, "Retirée");

        int statut = enregistrerExtras(d);

        assertThat(statut).isEqualTo(200);
        assertThat(extras.extras(d)).isEmpty();
    }

    /** R1 : null = inchangées (non-régression, passe déjà). */
    @Test
    void r1_extrasNull_inchangees() throws Exception {
        extras.extra(d, 1, GARDEE);

        int statut = enregistrer(d, "{\"notes\":\"x\"}").getStatus();

        assertThat(statut).isEqualTo(200);
        assertThat(seuleExtra(d)).containsEntry(DESCRIPTION, GARDEE);
    }

    @Test
    void r1_idDuneExtraDunAutreDecompte_400_rienEcrit() throws Exception {
        UUID autreLot = fixture.lot(projet, "LXF-" + jeton, fixture.fournisseur("FXF-" + jeton));
        UUID dAutre = decompte(autreLot, "XF1-", 1);
        UUID eAutre = extras.extra(dAutre, 1, "Autre");
        Map<String, Object> pirate = libre("Pirate", "1");
        pirate.put("id", eAutre.toString());

        int statut = enregistrerExtras(d, pirate);

        assertThat(statut).isEqualTo(400);
        assertThat(seuleExtra(dAutre)).containsEntry(DESCRIPTION, "Autre");
        assertThat(extras.extras(d)).isEmpty();
        assertThat(enregistrerExtras(d, libre("Pirate", "1"))).as("témoin sans id étranger").isEqualTo(200);
    }

    // ------------------------------------------------------------------ R2 ligne libre

    @Test
    void r2_ligneLibre_tousLesChampsEcrits_tauxEnRatio_puTTC() throws Exception {
        UUID categorie = bordereaux.categorie("CX" + jeton);

        int statut = enregistrerExtras(d, champs("code", "T1", DESIGNATION, "Peinture", "unite", "M2",
                QUANTITE, new BigDecimal("2"), PU, new BigDecimal("150.25"), TVA, new BigDecimal("20"),
                TAUX_FODEC, new BigDecimal("10"), "notes", "Note T1", "categorieId", categorie.toString()));

        assertThat(statut).isEqualTo(200);
        Map<String, Object> e = seuleExtra(d);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(e).containsEntry("Code", "T1").containsEntry(DESCRIPTION, "Peinture").containsEntry("Unity", "M2")
                    .containsEntry("Notes", "Note T1").containsEntry(CATEGORIE, categorie.toString())
                    .containsEntry("ligneMarche", null);
            s.assertThat(nombre(e, QUANTITY)).isEqualByComparingTo("2");
            s.assertThat(nombre(e, "UnitPriceHT")).isEqualByComparingTo("150.25");
            s.assertThat(nombre(e, "VatRatio")).isEqualByComparingTo("0.2");
            s.assertThat(nombre(e, FODEC)).isEqualByComparingTo("0.1");
            s.assertThat(nombre(e, "UnitPriceTTC")).as("D2 : PU HT × (1 + TVA)").isEqualByComparingTo("180.30");
        });
    }

    @Test
    void r2_categorieAbsente_categorieDuLot() throws Exception {
        UUID categorieLot = bordereaux.categorie("CL" + jeton);
        bordereaux.defauts(lot, null, categorieLot);

        int statut = enregistrerExtras(d, libre("Sans catégorie", "1"));

        assertThat(statut).isEqualTo(200);
        assertThat(seuleExtra(d)).containsEntry(CATEGORIE, categorieLot.toString());
    }

    /** R2 + réponse du manager : désignation faite d'espaces = vide. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void r2_designationObligatoire_400_rienEcrit(String designation) throws Exception {
        int statut = enregistrerExtras(d, libre(designation, "1"));

        assertThat(statut).isEqualTo(400);
        assertThat(extras.extras(d)).isEmpty();
        assertThat(enregistrerExtras(d, libre("Valide", "1"))).as("témoin avec désignation").isEqualTo(200);
    }

    // ------------------------------------------------------------------ R3 basée sur le BDP

    @Test
    void r3_ligneMarche_copieLesChampsDuBordereauDefinitif_quantiteSaisie() throws Exception {
        UUID categorie = bordereaux.categorie("CB" + jeton);
        UUID lm = ligneDefinitive(categorie);

        int statut = enregistrerExtras(d, champs(LIGNE_MARCHE, lm.toString(), QUANTITE, new BigDecimal("2")));

        assertThat(statut).isEqualTo(200);
        Map<String, Object> e = seuleExtra(d);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(e).containsEntry("Code", "B1").containsEntry(DESCRIPTION, "Ligne B1").containsEntry("Unity", "U")
                    .containsEntry("Notes", "Note B1").containsEntry(CATEGORIE, categorie.toString())
                    .containsEntry("ligneMarche", lm.toString());
            s.assertThat(nombre(e, "UnitPriceHT")).isEqualByComparingTo("250.50");
            s.assertThat(nombre(e, "VatRatio")).isEqualByComparingTo("0.2");
            s.assertThat(nombre(e, QUANTITY)).isEqualByComparingTo("2");
        });
    }

    @Test
    void r3_fodecDuBordereauNonCopie_zeroParDefaut() throws Exception {
        UUID lm = ligneDefinitive(bordereaux.categorie("CF" + jeton));
        bordereaux.fodec(LIGNES_DEFINITIF, lm, "0.1");

        int statut = enregistrerExtras(d, champs(LIGNE_MARCHE, lm.toString(), QUANTITE, BigDecimal.ONE));

        assertThat(statut).isEqualTo(200);
        assertThat(nombre(seuleExtra(d), FODEC)).isEqualByComparingTo("0");
    }

    @Test
    void r3_fodecSaisi_garde() throws Exception {
        UUID lm = ligneDefinitive(bordereaux.categorie("CS" + jeton));
        bordereaux.fodec(LIGNES_DEFINITIF, lm, "0.1");

        int statut = enregistrerExtras(d, champs(LIGNE_MARCHE, lm.toString(), QUANTITE, BigDecimal.ONE,
                TAUX_FODEC, new BigDecimal("5")));

        assertThat(statut).isEqualTo(200);
        assertThat(nombre(seuleExtra(d), FODEC)).isEqualByComparingTo("0.05");
    }

    /** R3 + réponse du manager : extra existante, même ligneMarcheId → champs saisis gardés (recopie seulement à la création). */
    @Test
    void r3_extraExistante_memeLigneMarche_champsModifiesGardes() throws Exception {
        UUID categorie = bordereaux.categorie("CM" + jeton);
        UUID lm = ligneDefinitive(categorie);
        UUID e1 = extras.extraComplet(d, lm, categorie);
        Map<String, Object> ligne = champs("id", e1.toString(), LIGNE_MARCHE, lm.toString(), "code", "XM",
                DESIGNATION, "Modifiée", "unite", "M3", QUANTITE, new BigDecimal("2"), PU, new BigDecimal("300"),
                TVA, new BigDecimal("10"), "notes", "Note XM", "categorieId", categorie.toString());

        int statut = enregistrerExtras(d, ligne);

        assertThat(statut).isEqualTo(200);
        Map<String, Object> e = seuleExtra(d);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(e).containsEntry("id", e1.toString()).containsEntry("Code", "XM").containsEntry(DESCRIPTION, "Modifiée")
                    .containsEntry("Unity", "M3").containsEntry("Notes", "Note XM").containsEntry("ligneMarche", lm.toString());
            s.assertThat(nombre(e, "UnitPriceHT")).isEqualByComparingTo("300");
            s.assertThat(nombre(e, "VatRatio")).isEqualByComparingTo("0.1");
        });
    }

    /** R3 + réponse du manager : autre lot, inconnue ou ligne d'estimatif → 400 « Ligne de bordereau inconnue ». */
    @ParameterizedTest
    @ValueSource(strings = {"autreLot", "inconnue", "estimatif"})
    void r3_ligneMarcheHorsBordereauDefinitifDuLot_400(String cas) throws Exception {
        UUID categorie = bordereaux.categorie("CI" + jeton);
        UUID mauvaise = switch (cas) {
            case "autreLot" -> bordereaux.ligne(LIGNES_DEFINITIF, new Ligne(
                    fixture.lot(projet, "LXG-" + jeton, fixture.fournisseur("FXG-" + jeton)), null, 1, "G1", "1", "10",
                    categorie, false));
            case "estimatif" -> bordereaux.ligne(LIGNES_ESTIMATIF, new Ligne(lot, null, 1, "E1", "1", "10", categorie, false));
            default -> UUID.randomUUID();
        };

        MockHttpServletResponse r = enregistrer(d, corps(EXTRAS, List.of(champs(LIGNE_MARCHE, mauvaise.toString(),
                QUANTITE, BigDecimal.ONE))));

        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(r.getContentAsString(StandardCharsets.UTF_8)).contains("Ligne de bordereau inconnue");
        assertThat(extras.extras(d)).isEmpty();
        UUID bonne = ligneDefinitive(categorie);
        assertThat(enregistrerExtras(d, champs(LIGNE_MARCHE, bonne.toString(), QUANTITE, BigDecimal.ONE)))
                .as("témoin ligne du bordereau définitif du lot").isEqualTo(200);
    }

    // ------------------------------------------------------------------ R5 quantité

    @Test
    void r5_quantiteNegative_acceptee() throws Exception {
        int statut = enregistrerExtras(d, libre("Moins-value", "-2"));

        assertThat(statut).isEqualTo(200);
        assertThat(nombre(seuleExtra(d), QUANTITY)).isEqualByComparingTo("-2");
    }

    @Test
    void r5_quantiteAbsente_zero() throws Exception {
        int statut = enregistrerExtras(d, champs(DESIGNATION, "Sans quantité", PU, new BigDecimal("100"),
                TVA, new BigDecimal("20")));

        assertThat(statut).isEqualTo(200);
        assertThat(nombre(seuleExtra(d), QUANTITY)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ Contrat : lecture ExtraDecompteDto

    @Test
    void contrat_lectureExtra_exposeCodeNotesCategorieLigneMarcheFodec() throws Exception {
        UUID lm = decomptes.ligneMarche(lot, 1, "10", "100", "0.2");
        UUID categorie = bordereaux.categorie("CR" + jeton);
        extras.extraComplet(d, lm, categorie);

        MockHttpServletResponse r = executer(get(DECOMPTES + "/" + d), admin);

        assertThat(r.getStatus()).isEqualTo(200);
        String json = r.getContentAsString(StandardCharsets.UTF_8);
        Map<String, Object> e = JsonPath.read(json, "$.extras[0]");
        assertThat(e).containsEntry("code", "XC").containsEntry("notes", "Note XC")
                .containsEntry("categorieId", categorie.toString()).containsEntry("ligneMarcheId", lm.toString());
        assertThat(nombre(e, TAUX_FODEC)).as("pourcentage 0-100").isEqualByComparingTo("5");
    }
}
