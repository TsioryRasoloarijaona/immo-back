package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureLotAvenants;
import com.tsix.immocrm.fixtures.FixtureLotAvenants.Avenant;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.AVENANTS;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DATE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DROIT_NOUVEL_AVENANT;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DROIT_RETENUES_PAR_MONTANT;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.DROIT_SUPPRIMER_AVENANT;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.GARANTIE;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.avenant;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.ligne;
import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.retenue;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Avenants (FEATURE-lot-avenants-avances.md §4 codes de retour, R1 droits, R4 retenues par montant) et leur reflet à la lecture
 * ({@code modifiable}, {@code supprimable}, {@code retenuesParMontantAutorisees}). Ordre : 404 lot → 404 avenant → 403 → 400 → 409.
 * Toute attente 404 a un témoin 2xx / 403 dans le même test (route absente = 404 avant le code).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotAvenantsDroitsTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String MODIFIABLE = "$.modifiable";
    private static final String SUPPRIMABLE = "$.supprimable";
    private static final String PAR_MONTANT_AUTORISE = "$.retenuesParMontantAutorisees";
    private static final LocalDate MARS = LocalDate.of(2026, 3, 5);
    private static final CorpsJson PAR_MONTANT_15 = retenue(true, "0", "15");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureBordereauxEcriture b;
    @Autowired private FixtureLotAvenants av;

    private AvenantsLotSupport appels;
    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID statut;
    private UUID categorie;
    private RequestPostProcessor admin;
    private int comptes;
    private int rang;

    @BeforeEach
    void setUp() {
        av.tables();
        appels = new AvenantsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        av.sequenceAvenant(0);
        societe = parite.entiteJuridique("ADE-" + jeton);
        projet = parite.projet("ADP-" + jeton, societe);
        statut = parite.statut(ENTREPRENEURS, "ADS-" + jeton);
        categorie = b.categorie("ADC-" + jeton);
        admin = connecte(parite.administrateur("lot.ad." + jeton, projet), "Administrateur");
    }

    private UUID lot() {
        rang++;
        return parite.lot(projet, "AD" + rang + "-" + jeton, ENTREPRENEURS, statut);
    }

    /** Compte non Administrateur, exempté de la clause de statut, habilité sur le projet, portant exactement ces droits. */
    private RequestPostProcessor avec(String... droits) {
        comptes++;
        return connecte(parite.avecDroits("lot.ad" + comptes + "." + jeton, List.of(droits), projet), COMMERCIAL);
    }

    /** Avenant en base, non facturé, une ligne. */
    private UUID avenantEnBase(UUID lot) {
        rang++;
        UUID a = av.avenant(new Avenant(lot, 1, "AV" + rang + "-" + jeton, MARS, "200", "40", "240", "216", "0.1", "24"));
        av.ligneAvenant(a, 1, "1", "2", "100", "0.2", categorie, null);
        return a;
    }

    private CorpsJson corpsValide() {
        return avenant(MARS, List.of(ligne(UUID.randomUUID(), 1, "1", "2", "100", "20", "0", categorie)));
    }

    // ------------------------------------------------------------------ 404 lot d'abord

    // Ordre 1 : lot inconnu → 404 LOT_INTROUVABLE, même détail que la lecture existante des avenants.
    @Test
    void ordre1_lotInconnu_404_memeDetailQueLaLecture() throws Exception {
        UUID inconnu = UUID.randomUUID();
        Reponse lecture = appels.lire(admin, inconnu, AVENANTS);

        Reponse creation = appels.creer(admin, inconnu, AVENANTS, corpsValide());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(creation.statut()).as(creation.corps()).isEqualTo(404);
            s.assertThat(detail(creation)).isNotBlank().isEqualTo(detail(lecture));
        });
    }

    // Ordre 1 avant 3 : lot d'un projet non habilité, compte sans aucun droit → 404 ; témoin : 403 sur un lot visible.
    @Test
    void ordre1_lotHorsPerimetre_compteSansDroit_404AvantLe403() throws Exception {
        UUID autreProjet = parite.projet("ADX-" + jeton, parite.entiteJuridique("ADY-" + jeton));
        UUID invisible = parite.lot(autreProjet, "ADH-" + jeton, ENTREPRENEURS, statut);
        UUID avenantInvisible = avenantEnBase(invisible);
        UUID visible = lot();
        UUID avenantVisible = avenantEnBase(visible);
        RequestPostProcessor sansDroit = avec();

        Reponse temoin = appels.supprimer(sansDroit, visible, AVENANTS, avenantVisible);
        Reponse creation = appels.creer(sansDroit, invisible, AVENANTS, corpsValide());
        Reponse suppression = appels.supprimer(sansDroit, invisible, AVENANTS, avenantInvisible);
        Reponse lecture = appels.lire(sansDroit, invisible, AVENANTS, avenantInvisible);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(403);
            s.assertThat(creation.statut()).as(creation.corps()).isEqualTo(404);
            s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(404);
            s.assertThat(lecture.statut()).as(lecture.corps()).isEqualTo(404);
        });
    }

    // Ordre 2 : avenant d'un autre lot → 404 en GET, PUT, DELETE ; témoin : GET sur son propre lot → 200.
    @Test
    void ordre2_avenantDUnAutreLot_404_getPutDelete() throws Exception {
        UUID lot = lot();
        UUID autreLot = lot();
        UUID etranger = avenantEnBase(autreLot);

        Reponse temoin = appels.lire(admin, autreLot, AVENANTS, etranger);
        Reponse lecture = appels.lire(admin, lot, AVENANTS, etranger);
        Reponse modification = appels.modifier(admin, lot, AVENANTS, etranger, corpsValide());
        Reponse suppression = appels.supprimer(admin, lot, AVENANTS, etranger);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
            s.assertThat(lecture.statut()).as(lecture.corps()).isEqualTo(404);
            s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(404);
            s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(404);
            s.assertThat(av.parId(FixtureLotAvenants.AVENANTS, etranger)).as("avenant étranger intact").hasSize(1);
        });
    }

    // Ordre 2 avant 3 : avenant d'un autre lot, compte sans toolDelete → 404 ; témoin : 403 sur le bon lot.
    @Test
    void ordre2_avenantDUnAutreLot_compteSansDroit_404AvantLe403() throws Exception {
        UUID lot = lot();
        UUID autreLot = lot();
        UUID etranger = avenantEnBase(autreLot);
        RequestPostProcessor sansDroit = avec();

        Reponse temoin = appels.supprimer(sansDroit, autreLot, AVENANTS, etranger);
        Reponse r = appels.supprimer(sansDroit, lot, AVENANTS, etranger);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(403);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        });
    }

    // Avenant inconnu sur un lot visible → 404 ; témoin : avenant du lot → 200.
    @Test
    void ordre2_avenantInconnu_404() throws Exception {
        UUID lot = lot();
        UUID present = avenantEnBase(lot);

        Reponse temoin = appels.lire(admin, lot, AVENANTS, present);
        Reponse r = appels.lire(admin, lot, AVENANTS, UUID.randomUUID());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        });
    }

    // ------------------------------------------------------------------ R1 : droits

    @Test
    void r1_post_sansToolNew_403_rienNEstCree() throws Exception {
        UUID lot = lot();

        Reponse r = appels.creer(avec(DROIT_SUPPRIMER_AVENANT), lot, AVENANTS, corpsValide());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(av.compter("SELECT COUNT(*) FROM PrjClause WHERE PrjMarketId = ?", lot.toString())).isZero();
        });
    }

    @Test
    void r1_post_avecToolNew_201() throws Exception {
        Reponse r = appels.creer(avec(DROIT_NOUVEL_AVENANT), lot(), AVENANTS, corpsValide());

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    @Test
    void r1_delete_sansToolDelete_403_avenantConserve() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot);

        Reponse r = appels.supprimer(avec(DROIT_NOUVEL_AVENANT), lot, AVENANTS, a);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(av.parId(FixtureLotAvenants.AVENANTS, a)).hasSize(1);
        });
    }

    @Test
    void r1_delete_avecToolDelete_204() throws Exception {
        UUID lot = lot();

        Reponse r = appels.supprimer(avec(DROIT_SUPPRIMER_AVENANT), lot, AVENANTS, avenantEnBase(lot));

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
    }

    // R1 : PUT et GET sans droit propre (parité) → 200.
    @Test
    void r1_put_compteSansAucunDroit_200() throws Exception {
        UUID lot = lot();

        Reponse r = appels.modifier(avec(), lot, AVENANTS, avenantEnBase(lot), corpsValide());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    @Test
    void r1_get_compteSansAucunDroit_200() throws Exception {
        UUID lot = lot();

        Reponse r = appels.lire(avec(), lot, AVENANTS, avenantEnBase(lot));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // Ordre 3 avant 4 : corps invalide (date manquante) sans toolNew → 403.
    @Test
    void ordre3_post_corpsInvalide_sansToolNew_403() throws Exception {
        Reponse r = appels.creer(avec(), lot(), AVENANTS, corpsValide().avec(DATE, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    // ------------------------------------------------------------------ R4 : retenue par montant

    @Test
    void r4_put_parMontant_sansCanEditWorkingOutRetainByAmount_403() throws Exception {
        UUID lot = lot();

        Reponse r = appels.modifier(avec(), lot, AVENANTS, avenantEnBase(lot),
                corpsValide().avec(GARANTIE, PAR_MONTANT_15));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void r4_post_toolNewSansCanEdit_parMontant_403() throws Exception {
        Reponse r = appels.creer(avec(DROIT_NOUVEL_AVENANT), lot(), AVENANTS,
                corpsValide().avec(GARANTIE, PAR_MONTANT_15));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void r4_put_parMontant_avecCanEditWorkingOutRetainByAmount_200() throws Exception {
        UUID lot = lot();

        Reponse r = appels.modifier(avec(DROIT_RETENUES_PAR_MONTANT), lot, AVENANTS, avenantEnBase(lot),
                corpsValide().avec(GARANTIE, PAR_MONTANT_15));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ lecture : drapeaux = mêmes règles que l'écriture

    @Test
    void lecture_administrateur_avenantNonFacture_toutAutorise() throws Exception {
        UUID lot = lot();

        Reponse r = appels.lire(admin, lot, AVENANTS, avenantEnBase(lot));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.valeur(MODIFIABLE)).as(r.corps()).isEqualTo(true);
            s.assertThat(r.valeur(SUPPRIMABLE)).isEqualTo(true);
            s.assertThat(r.valeur(PAR_MONTANT_AUTORISE)).isEqualTo(true);
        });
    }

    @Test
    void lecture_compteSansDroit_nonSupprimable_parMontantInterdit_modifiable() throws Exception {
        UUID lot = lot();

        Reponse r = appels.lire(avec(), lot, AVENANTS, avenantEnBase(lot));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.valeur(MODIFIABLE)).as(r.corps()).isEqualTo(true);
            s.assertThat(r.valeur(SUPPRIMABLE)).isEqualTo(false);
            s.assertThat(r.valeur(PAR_MONTANT_AUTORISE)).isEqualTo(false);
        });
    }

    @Test
    void lecture_toolDeleteEtCanEdit_supprimableEtParMontantAutorise() throws Exception {
        UUID lot = lot();

        Reponse r = appels.lire(avec(DROIT_SUPPRIMER_AVENANT, DROIT_RETENUES_PAR_MONTANT), lot, AVENANTS, avenantEnBase(lot));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.valeur(SUPPRIMABLE)).as(r.corps()).isEqualTo(true);
            s.assertThat(r.valeur(PAR_MONTANT_AUTORISE)).isEqualTo(true);
        });
    }

    // R5 + R6 : avenant facturé → ni modifiable (lignes, retenues) ni supprimable, même pour l'Administrateur.
    @Test
    void lecture_avenantFacture_administrateur_nonModifiableNonSupprimable_facture() throws Exception {
        UUID lot = lot();
        UUID a = avenantEnBase(lot);
        av.facturer(a, lot, projet, societe, "ADF-" + jeton);

        Reponse r = appels.lire(admin, lot, AVENANTS, a);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.valeur("$.facture")).as(r.corps()).isEqualTo(true);
            s.assertThat(r.valeur("$.factureCode")).isEqualTo("ADF-" + jeton);
            s.assertThat(r.valeur(MODIFIABLE)).isEqualTo(false);
            s.assertThat(r.valeur(SUPPRIMABLE)).isEqualTo(false);
        });
    }
}
