package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.CATEGORIE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.bordereau;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ligne;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.vide;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Écritures des bordereaux (FEATURE-lot-ecritures.md §4 codes de retour, R1 visibilité, R2 droits, R3 cadenas) et leur reflet à la
 * lecture ({@code verrouille}, {@code modifiable}). Ordre : 404 → 403 → 409 cadenas → 400 → 409 suppression décomptée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotBordereauxDroitsTest {

    private static final String DROIT_LIGNE = "Yes.Prj.Front.Market.MarketMonitoring.toolAddMarketLine";
    private static final String MSG_CADENAS = "Le bordereau estimatif est verrouillé par le statut du lot";
    private static final String MSG_MULTI = "Bordereau estimatif multi-fournisseurs : non géré";
    private static final String MSG_LIGNE = "Vous ne pouvez pas supprimer cette ligne";
    private static final String VERROUILLE = "$.verrouille";
    private static final String MODIFIABLE = "$.modifiable";
    private static final String COMMERCIAL = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureBordereauxEcriture b;

    private BordereauxEcritureSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statutOuvert;
    private UUID statutFerme;
    private UUID categorie;
    private RequestPostProcessor admin;
    private int comptes;

    @BeforeEach
    void setUp() {
        appels = new BordereauxEcritureSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        b.tableVentilation();
        projet = parite.projet("BDP-" + jeton, parite.entiteJuridique("BDE-" + jeton));
        statutOuvert = parite.statut(ENTREPRENEURS, "BDO-" + jeton);
        b.ouvrirEstimatif(statutOuvert);
        statutFerme = parite.statut(ENTREPRENEURS, "BDF-" + jeton);
        categorie = b.categorie("BDC-" + jeton);
        admin = connecte(parite.administrateur("lot.bd." + jeton, projet), "Administrateur");
    }

    private UUID lot(String code, UUID statut) {
        return parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statut);
    }

    /** Compte non Administrateur, exempté de la clause de statut, habilité sur le projet, portant exactement ces droits. */
    private RequestPostProcessor avec(String... droits) {
        comptes++;
        return connecte(parite.avecDroits("lot.bd" + comptes + "." + jeton, List.of(droits), projet), COMMERCIAL);
    }

    private CorpsJson uneLigne() {
        return bordereau(List.of(), List.of(ligne(UUID.randomUUID(), null, 1, "1", "2", "100", categorie)));
    }

    // ------------------------------------------------------------------ R1 : 404 d'abord

    @Test
    void r1_lotInconnu_404_lesDeuxBordereaux_memeMessageQueLaLecture() throws Exception {
        UUID inconnu = UUID.randomUUID();
        Reponse lecture = appels.lire(admin, inconnu, ESTIMATIF);

        Reponse e = appels.ecrire(admin, inconnu, ESTIMATIF, vide());
        Reponse d = appels.ecrire(admin, inconnu, DEFINITIF, vide());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(e.statut()).as(e.corps()).isEqualTo(404);
            s.assertThat(d.statut()).as(d.corps()).isEqualTo(404);
            s.assertThat(detail(e)).isNotBlank().isEqualTo(detail(lecture));
            s.assertThat(detail(d)).isNotBlank().isEqualTo(detail(lecture));
        });
    }

    // R1 : lot d'un projet non habilité, compte sans aucun droit → 404 avant le 403, ni verrou ni droit révélé.
    @Test
    void r1_lotHorsPerimetre_compteSansDroit_404AvantLe403() throws Exception {
        UUID autreProjet = parite.projet("BDX-" + jeton, parite.entiteJuridique("BDY-" + jeton));
        UUID lot = parite.lot(autreProjet, "BDH-" + jeton, ENTREPRENEURS, statutFerme);
        RequestPostProcessor sansDroit = avec();

        Reponse e = appels.ecrire(sansDroit, lot, ESTIMATIF, vide());
        Reponse d = appels.ecrire(sansDroit, lot, DEFINITIF, vide());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(e.statut()).as(e.corps()).isEqualTo(404);
            s.assertThat(d.statut()).as(d.corps()).isEqualTo(404);
        });
    }

    // ------------------------------------------------------------------ R2 : droits

    @Test
    void r2_estimatif_sansToolNew_403() throws Exception {
        Reponse r = appels.ecrire(avec(DROIT_LIGNE), lot("E1", statutOuvert), ESTIMATIF, uneLigne());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void r2_estimatif_toolNewSeul_200() throws Exception {
        Reponse r = appels.ecrire(avec(DROIT_NOUVEAU), lot("E2", statutOuvert), ESTIMATIF, uneLigne());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    @Test
    void r2_definitif_sansToolNew_403() throws Exception {
        Reponse r = appels.ecrire(avec(DROIT_LIGNE), lot("D1", statutOuvert), DEFINITIF, uneLigne());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void r2_definitif_toolNewSansToolAddMarketLine_403() throws Exception {
        Reponse r = appels.ecrire(avec(DROIT_NOUVEAU), lot("D2", statutOuvert), DEFINITIF, uneLigne());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void r2_definitif_lesDeuxDroits_200() throws Exception {
        Reponse r = appels.ecrire(avec(DROIT_NOUVEAU, DROIT_LIGNE), lot("D3", statutOuvert), DEFINITIF, uneLigne());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // R2 + R3 à la lecture : verrouille = !(écriture estimative acceptée).
    @Test
    void r2_lectureEstimatif_toolNewSurStatutOuvert_nonVerrouille() throws Exception {
        Reponse r = appels.lire(avec(DROIT_NOUVEAU), lot("L1", statutOuvert), ESTIMATIF);

        assertThat(r.valeur(VERROUILLE)).as(r.corps()).isEqualTo(false);
    }

    @Test
    void r2_lectureEstimatif_sansToolNew_verrouille() throws Exception {
        Reponse r = appels.lire(avec(DROIT_LIGNE), lot("L2", statutOuvert), ESTIMATIF);

        assertThat(r.valeur(VERROUILLE)).as(r.corps()).isEqualTo(true);
    }

    @Test
    void r2_lectureDefinitif_lesDeuxDroits_modifiable() throws Exception {
        Reponse r = appels.lire(avec(DROIT_NOUVEAU, DROIT_LIGNE), lot("L3", statutFerme), DEFINITIF);

        assertThat(r.valeur(MODIFIABLE)).as(r.corps()).isEqualTo(true);
    }

    @Test
    void r2_lectureDefinitif_toolNewSeul_nonModifiable() throws Exception {
        Reponse r = appels.lire(avec(DROIT_NOUVEAU), lot("L4", statutOuvert), DEFINITIF);

        assertThat(r.valeur(MODIFIABLE)).as(r.corps()).isEqualTo(false);
    }

    @Test
    void r2_lectureDefinitif_administrateur_modifiable() throws Exception {
        Reponse r = appels.lire(admin, lot("L5", statutFerme), DEFINITIF);

        assertThat(r.valeur(MODIFIABLE)).as(r.corps()).isEqualTo(true);
    }

    // ------------------------------------------------------------------ R3 : cadenas de l'estimatif

    @Test
    void r3_estimatif_statutSansBoolField004_409_message() throws Exception {
        Reponse r = appels.ecrire(admin, lot("C1", statutFerme), ESTIMATIF, uneLigne());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).contains(MSG_CADENAS);
        });
    }

    @Test
    void r3_estimatif_lotMultiFournisseurs_409_message() throws Exception {
        UUID statutMulti = parite.statut(FOURNISSEURS, "BDM-" + jeton);
        b.ouvrirEstimatif(statutMulti);
        UUID lot = parite.lot(projet, "C2-" + jeton, FOURNISSEURS, statutMulti);

        Reponse r = appels.ecrire(admin, lot, ESTIMATIF, uneLigne());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).contains(MSG_MULTI);
        });
    }

    @Test
    void r3_definitif_statutSansBoolField004_pasDeVerrou_200() throws Exception {
        Reponse r = appels.ecrire(admin, lot("C3", statutFerme), DEFINITIF, uneLigne());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    @Test
    void r3_lectureEstimatif_administrateurStatutOuvert_nonVerrouille() throws Exception {
        Reponse r = appels.lire(admin, lot("C4", statutOuvert), ESTIMATIF);

        assertThat(r.valeur(VERROUILLE)).as(r.corps()).isEqualTo(false);
    }

    @Test
    void r3_lectureEstimatif_administrateurStatutFerme_verrouille() throws Exception {
        Reponse r = appels.lire(admin, lot("C5", statutFerme), ESTIMATIF);

        assertThat(r.valeur(VERROUILLE)).as(r.corps()).isEqualTo(true);
    }

    // R3 : la lecture d'un lot multi ne change pas, son écriture est refusée → verrouillé.
    @Test
    void r3_lectureEstimatif_lotMultiFournisseurs_verrouille() throws Exception {
        UUID statutMulti = parite.statut(FOURNISSEURS, "BDN-" + jeton);
        b.ouvrirEstimatif(statutMulti);
        UUID lot = parite.lot(projet, "C6-" + jeton, FOURNISSEURS, statutMulti);

        Reponse r = appels.lire(admin, lot, ESTIMATIF);

        assertThat(r.valeur(VERROUILLE)).as(r.corps()).isEqualTo(true);
    }

    // ------------------------------------------------------------------ ordre d'évaluation

    @Test
    void ordre_403AvantCadenas() throws Exception {
        Reponse r = appels.ecrire(avec(DROIT_LIGNE), lot("O1", statutFerme), ESTIMATIF, uneLigne());

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void ordre_cadenasAvant400() throws Exception {
        CorpsJson invalide = bordereau(List.of(),
                List.of(ligne(UUID.randomUUID(), null, 1, "1", "2", "100", categorie).avec(CATEGORIE, null)));

        Reponse r = appels.ecrire(admin, lot("O2", statutFerme), ESTIMATIF, invalide);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).contains(MSG_CADENAS);
        });
    }

    @Test
    void ordre_400AvantSuppressionDecomptee() throws Exception {
        UUID lot = lot("O3", statutOuvert);
        UUID decomptee = b.ligne(FixtureBordereauxEcriture.LIGNES_DEFINITIF,
                new FixtureBordereauxEcriture.Ligne(lot, null, 1, "1", "2", "100", categorie, false));
        b.decompter(lot, decomptee, "BDW-" + jeton);
        CorpsJson invalide = bordereau(List.of(),
                List.of(ligne(UUID.randomUUID(), null, 2, "2", "1", "10", categorie).avec(CATEGORIE, null)));

        Reponse r = appels.ecrire(admin, lot, DEFINITIF, invalide);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).doesNotContain(MSG_LIGNE);
        });
    }
}
