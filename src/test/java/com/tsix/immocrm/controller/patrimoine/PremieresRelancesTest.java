package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePremieresRelances;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import com.tsix.immocrm.fixtures.LectureExport;
import com.tsix.immocrm.services.modele.DemarrageModeles;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePremieresRelances.DATE_RELANCE;
import static com.tsix.immocrm.fixtures.FixturePremieresRelances.ECHEANCE;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.pagesPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lot P9d — « Imprimer les premières relances » de la tranche : FEATURE-patrimoine-import-export.md §4.1 et §5 (R1–R4, R6),
 * FEATURE-patrimoine-relances-champs.md (décision 4 : pas de ré-impression sans {@code Revival1Date}). Écriture hors
 * patrimoine ({@code ComFolder}, {@code ComActionMessage}) et montants imprimés : règle 6. R5 (rollback) est dans
 * {@link PremieresRelancesAtomiciteTest}, les 403 dans {@link ChampsSpecifiquesSecuriteTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PremieresRelancesTest {

    private static final String APERCU = "/api/tranches/{id}/premieres-relances/apercu";
    private static final String RELANCES = "/api/tranches/{id}/premieres-relances";
    private static final String LETTRES = "/api/tranches/{id}/premieres-relances.pdf";
    private static final String MESSAGE_SANS_DATE = "Vous devez saisir une Date de 1ère relance!";
    private static final String SYSTEME = "Système";
    private static final LocalDateTime AUTRE_VALIDITE = LocalDateTime.of(2025, 12, 31, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private ApplicationContext contexte;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FixturePremieresRelances fixture;

    private AdmUser admin;
    private Jeu jeu;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeu = fixture.jeu();
        fixture.typeNote();
        dossiers.categoriesAction();
    }

    // ------------------------------------------------------------------ R1 : pas de Revival1Date → 400, rien écrit

    @Test
    void r1_trancheSansDateDeRelance_400_messageOmran_rienEcrit() throws Exception {
        ComFolder dossier = fixture.dossier(jeu, jeu.jeton() + "-R1", fixture.enCours(jeu, "RES"), true);
        fixture.sansDateDeRelance(jeu.tranche());
        fixture.relire();

        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MESSAGE_SANS_DATE));

        fixture.relire();
        ComFolder relu = fixture.relu(dossier);
        assertAll(
                () -> assertThat(relu.getRevival1()).isNull(),
                () -> assertThat(relu.getRevival1ValidityDate()).isNull(),
                () -> assertThat(fixture.alertesDeLaTranche(jeu)).isZero());
    }

    /** Décision 4 (25/10) : des dossiers déjà relancés, date de la tranche vidée → 400, dates et alertes intactes. */
    @Test
    void r1_reimpressionSansDateDeRelance_400_datesDejaPoseesIntactes() throws Exception {
        ComFolder dossier = fixture.dossier(jeu, jeu.jeton() + "-RE", fixture.enCours(jeu, "RES"), true);
        LocalDateTime anciennePose = LocalDateTime.of(2025, 6, 1, 0, 0);
        fixture.relance(dossier, anciennePose, AUTRE_VALIDITE);
        fixture.sansDateDeRelance(jeu.tranche());
        fixture.relire();

        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MESSAGE_SANS_DATE));

        fixture.relire();
        ComFolder relu = fixture.relu(dossier);
        assertAll(
                () -> assertThat(relu.getRevival1()).isEqualTo(anciennePose),
                () -> assertThat(relu.getRevival1ValidityDate()).isEqualTo(AUTRE_VALIDITE),
                () -> assertThat(fixture.alertesDeLaTranche(jeu)).isZero());
    }

    /** Réponses du manager 5 et 6 (25/10) : aperçu et lettre refusés comme le POST quand la tranche n'a pas de date. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, LETTRES})
    void r1_lectureSansDateDeRelance_400_memeMessage(String route) throws Exception {
        fixture.relance(fixture.dossier(jeu, jeu.jeton() + "-SD", fixture.enCours(jeu, "RES"), true),
                DATE_RELANCE, ECHEANCE);
        fixture.sansDateDeRelance(jeu.tranche());
        fixture.relire();

        mockMvc.perform(get(route, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MESSAGE_SANS_DATE));
    }

    /** Réponse 5 : aucun dossier de la sélection ne porte {@code Revival1} → 400 « Aucun dossier à imprimer ». */
    @Test
    void r6_lettre_aucunDossierRelance_400() throws Exception {
        fixture.dossier(jeu, jeu.jeton() + "-NR", fixture.enCours(jeu, "RES"), true);
        fixture.relire();

        mockMvc.perform(get(LETTRES, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Aucun dossier à imprimer"));
    }

    /** Réponse 7 : {@code AgreementFirstDelay} NULL = délai de 0 jour, échéance = date de relance. */
    @Test
    void r3_premierDelaiNul_echeanceEgaleALaDateDeRelance() throws Exception {
        jeu.tranche().setAgreementFirstDelay(null);
        em.merge(jeu.tranche());
        ComFolder dossier = fixture.dossier(jeu, jeu.jeton() + "-D0", fixture.enCours(jeu, "RES"), true);
        fixture.relire();

        mockMvc.perform(get(APERCU, jeu.tranche().getId()).with(connecte(admin))).andExpect(status().isOk());
        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin))).andExpect(status().isOk());

        fixture.relire();
        assertAll(
                () -> assertThat(fixture.relu(dossier).getRevival1()).isEqualTo(DATE_RELANCE),
                () -> assertThat(fixture.relu(dossier).getRevival1ValidityDate()).isEqualTo(DATE_RELANCE),
                () -> assertThat(fixture.alertes(dossier)).singleElement()
                        .satisfies(a -> assertThat(a.getAlertValidityDate()).isEqualTo(DATE_RELANCE)));
    }

    // ------------------------------------------------------------------ R2 : sélection

    private record Selection(ComFolder retenu, List<ComFolder> exclus) {
    }

    /** Un dossier retenu ; exclus : sous-article, annulé, désisté, sans statut, sans lien retour, autre tranche. */
    private Selection selection() {
        String j = jeu.jeton();
        ComFolder retenu = fixture.dossier(jeu, j + "-OK", fixture.enCours(jeu, "RES"), true);
        Jeu autre = fixture.jeu();
        List<ComFolder> exclus = List.of(
                fixture.dossierSousArticle(jeu, j + "-SOUS", fixture.enCours(jeu, "RES2")),
                fixture.dossier(jeu, j + "-ANN", fixture.annulation(jeu), true),
                fixture.dossier(jeu, j + "-DES", fixture.desistement(jeu), true),
                fixture.dossier(jeu, j + "-NUL", null, true),
                fixture.dossierSansLienRetour(jeu, j + "-LIEN", fixture.enCours(jeu, "RES3")),
                fixture.dossier(autre, j + "-AUT", fixture.enCours(autre, "RES"), true));
        fixture.relire();
        return new Selection(retenu, exclus);
    }

    private static boolean cite(String json, ComFolder dossier) {
        return json.contains(dossier.getId().toString()) || json.contains("\"" + dossier.getCode() + "\"");
    }

    @Test
    void r2_apercu_retientLArticleRacineVivant_exclutSousArticleAnnuleDesisteSansStatutEtAutreTranche() throws Exception {
        Selection s = selection();

        String json = mockMvc.perform(get(APERCU, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(cite(json, s.retenu())).as("dossier retenu cité").isTrue();
        assertThat(s.exclus()).isNotEmpty()
                .allSatisfy(d -> assertThat(cite(json, d)).as("exclu : " + d.getCode()).isFalse());
    }

    @Test
    void r2_ecriture_neModifieQueLeDossierRetenu_niDateNiAlerteSurLesExclus() throws Exception {
        Selection s = selection();

        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossiersModifies").value(1))
                .andExpect(jsonPath("$.alertesCreees").value(1));

        fixture.relire();
        assertThat(fixture.relu(s.retenu()).getRevival1()).isEqualTo(DATE_RELANCE);
        assertThat(s.exclus()).isNotEmpty().allSatisfy(d -> assertAll(
                () -> assertThat(fixture.relu(d).getRevival1()).as(d.getCode()).isNull(),
                () -> assertThat(fixture.relu(d).getRevival1ValidityDate()).as(d.getCode()).isNull(),
                () -> assertThat(fixture.alertes(d)).as(d.getCode()).isEmpty()));
    }

    @Test
    void apercu_nEcritRien() throws Exception {
        ComFolder dossier = fixture.dossier(jeu, jeu.jeton() + "-AP", fixture.enCours(jeu, "RES"), true);
        fixture.relire();

        mockMvc.perform(get(APERCU, jeu.tranche().getId()).with(connecte(admin))).andExpect(status().isOk());

        fixture.relire();
        assertAll(
                () -> assertThat(fixture.relu(dossier).getRevival1()).isNull(),
                () -> assertThat(fixture.alertesDeLaTranche(jeu)).isZero());
    }

    // ------------------------------------------------------------------ R3 : dates écrites, alerte seulement si l'échéance change

    @Test
    void r3_datesEcritesPartout_alerteSeulementSiEcheanceChangeEtProspectionPresente() throws Exception {
        String j = jeu.jeton();
        StrStatus statut = fixture.enCours(jeu, "RES");
        ComFolder change = fixture.relance(fixture.dossier(jeu, j + "-CHG", statut, true), null, AUTRE_VALIDITE);
        ComFolder inchange = fixture.relance(fixture.dossier(jeu, j + "-INC", statut, true), null, ECHEANCE);
        ComFolder sansProspection = fixture.dossier(jeu, j + "-SPR", statut, false);
        fixture.relire();

        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossiersModifies").value(3))
                .andExpect(jsonPath("$.alertesCreees").value(1));

        fixture.relire();
        assertThat(List.of(change, inchange, sansProspection)).allSatisfy(d -> assertAll(
                () -> assertThat(fixture.relu(d).getRevival1()).as(d.getCode()).isEqualTo(DATE_RELANCE),
                () -> assertThat(fixture.relu(d).getRevival1ValidityDate()).as(d.getCode()).isEqualTo(ECHEANCE)));
        assertAll(
                () -> assertThat(fixture.alertes(change)).hasSize(1),
                () -> assertThat(fixture.alertes(inchange)).isEmpty(),
                () -> assertThat(fixture.alertes(sansProspection)).isEmpty());
    }

    /** Conséquence de R3 : relancer deux fois avec la même date ne recrée pas d'alerte. */
    @Test
    void r3_relancerDeuxFois_pasDeSecondeAlerte() throws Exception {
        ComFolder dossier = fixture.dossier(jeu, jeu.jeton() + "-2X", fixture.enCours(jeu, "RES"), true);
        fixture.relire();
        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin))).andExpect(status().isOk());
        fixture.relire();

        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossiersModifies").value(1))
                .andExpect(jsonPath("$.alertesCreees").value(0));

        assertThat(fixture.alertes(dossier)).hasSize(1);
    }

    // ------------------------------------------------------------------ R4 : colonnes exactes de l'alerte

    @Test
    void r4_alerte_colonnesOmran_categorieReservationSiStatutDuParametre() throws Exception {
        StrStatus reservation = fixture.enCours(jeu, "RES");
        fixture.statutReservation(reservation);
        ComFolder dossier = fixture.dossier(jeu, jeu.jeton() + "-R4", reservation, true);
        fixture.relire();

        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin))).andExpect(status().isOk());

        fixture.relire();
        ComFolder relu = fixture.relu(dossier);
        ComActionMessage alerte = fixture.alertes(dossier).getFirst();
        String message = "Titre foncier: Echéance de la notification de la 1ère relance du dossier de vente N° "
                + relu.getCode() + ", date échéance: 15/04/2026";
        assertAll(
                () -> assertThat(alerte.getDocDate()).isNotNull(),
                () -> assertThat(alerte.getComProspection().getId()).isEqualTo(relu.getComProspection().getId()),
                () -> assertThat(alerte.getCfgTier().getId()).isEqualTo(relu.getCfgCustomer().getId()),
                () -> assertThat(alerte.getCfgEmployee().getId()).isEqualTo(relu.getCfgCommercial().getId()),
                () -> assertThat(alerte.getStkItem().getId()).isEqualTo(relu.getStkItem().getId()),
                () -> assertThat(alerte.getIsSystem()).isTrue(),
                () -> assertThat(alerte.getUserName()).isEqualTo(SYSTEME),
                () -> assertThat(alerte.getComActionMessageType().getId()).isEqualTo(FixturePremieresRelances.TYPE_NOTE),
                () -> assertThat(alerte.getStrFolderStatusId()).isEqualTo(reservation.getId()),
                () -> assertThat(alerte.getComActionMessageCategory().getId())
                        .isEqualTo(ActionsDossier.CATEGORIE_RESERVATION),
                () -> assertThat(alerte.getAlertValidityDate()).isEqualTo(ECHEANCE),
                () -> assertThat(alerte.getAlertMessage()).isEqualTo(message),
                () -> assertThat(alerte.getWithoutRevival()).isFalse(),
                () -> assertThat(alerteData(alerte)).hasSize(1));
    }

    @Test
    void r4_categorieLivraison_siStatutAutreQueLeParametreReservation() throws Exception {
        fixture.statutReservation(fixture.enCours(jeu, "RES"));
        ComFolder dossier = fixture.dossier(jeu, jeu.jeton() + "-LIV", fixture.enCours(jeu, "LIV"), true);
        fixture.relire();

        mockMvc.perform(post(RELANCES, jeu.tranche().getId()).with(connecte(admin))).andExpect(status().isOk());

        fixture.relire();
        assertThat(fixture.alertes(dossier)).singleElement()
                .satisfies(a -> assertThat(a.getComActionMessageCategory().getId())
                        .isEqualTo(ActionsDossier.CATEGORIE_LIVRAISON));
    }

    private List<AdmAlertData> alerteData(ComActionMessage alerte) {
        return em.createQuery("SELECT a FROM AdmAlertData a WHERE a.relatedKey = :id", AdmAlertData.class)
                .setParameter("id", alerte.getId()).getResultList();
    }

    // ------------------------------------------------------------------ 404 : tranche invisible ou inconnue

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, LETTRES})
    void lecture_trancheInvisible_404(String route) throws Exception {
        Jeu invisible = fixture.jeu(false);
        fixture.relire();

        int statut = executer(mockMvc, get(route, invisible.tranche().getId()).with(connecte(admin)))
                .getResponse().getStatus();

        assertThat(statut).isEqualTo(404);
    }

    @Test
    void ecriture_trancheInvisible_404_rienEcrit() throws Exception {
        Jeu invisible = fixture.jeu(false);
        ComFolder dossier = fixture.dossier(invisible, invisible.jeton() + "-INV", fixture.enCours(invisible, "RES"), true);
        fixture.relire();

        mockMvc.perform(post(RELANCES, invisible.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());

        fixture.relire();
        assertThat(fixture.relu(dossier).getRevival1()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, RELANCES, LETTRES})
    void trancheInconnue_404(String route) throws Exception {
        var requete = RELANCES.equals(route) ? post(route, UUID.randomUUID()) : get(route, UUID.randomUUID());

        int statut = executer(mockMvc, requete.with(connecte(admin))).getResponse().getStatus();

        assertThat(statut).isEqualTo(404);
    }

    // ------------------------------------------------------------------ R6 : lettre PDF

    /** Modèles livrés avec l'application (désactivés sous la suite de tests) : la catégorie RELANCE_TITRE_FONCIER y gagne le sien. */
    private void modelesInitiaux() throws Exception {
        contexte.getAutowireCapableBeanFactory().createBean(DemarrageModeles.class).run(null);
    }

    private List<String> lettres() throws Exception {
        MvcResult resultat = executer(mockMvc, get(LETTRES, jeu.tranche().getId()).with(connecte(admin)));
        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultat.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PDF_VALUE);
        byte[] contenu = resultat.getResponse().getContentAsByteArray();
        assertThat(estPdf(contenu)).isTrue();
        return pagesPdf(contenu).stream().map(LectureExport::compact).toList();
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void r6_unePageParDossierRelanceDeLaSelection_niExclusNiDossierSansRevival1() throws Exception {
        modelesInitiaux();
        String j = jeu.jeton();
        StrStatus statut = fixture.enCours(jeu, "RES");
        ComFolder premier = fixture.relance(fixture.dossier(jeu, j + "-L1", statut, true), DATE_RELANCE, ECHEANCE);
        ComFolder second = fixture.relance(fixture.dossier(jeu, j + "-L2", statut, false), DATE_RELANCE, ECHEANCE);
        ComFolder jamaisRelance = fixture.dossier(jeu, j + "-L3", statut, true);
        ComFolder sousArticle = fixture.relance(fixture.dossierSousArticle(jeu, j + "-L4", statut), DATE_RELANCE, ECHEANCE);
        ComFolder annule = fixture.relance(fixture.dossier(jeu, j + "-L5", fixture.annulation(jeu), true),
                DATE_RELANCE, ECHEANCE);
        fixture.relire();

        List<String> pages = lettres();

        assertThat(pages).hasSize(2);
        assertThat(pages).anySatisfy(p -> assertThat(p).contains(premier.getCode()))
                .anySatisfy(p -> assertThat(p).contains(second.getCode()))
                .noneSatisfy(p -> assertThat(p).contains(jamaisRelance.getCode()))
                .noneSatisfy(p -> assertThat(p).contains(sousArticle.getCode()))
                .noneSatisfy(p -> assertThat(p).contains(annule.getCode()));
    }

    /**
     * Restes dus = calcul partagé ({@code MontantRestant} sur {@code reglesParEcheance}) : prix 1 234 000 − 251 300 réglés
     * = 982 700 ; frais 31 500 − 10 250 = 21 250 ; total 1 003 950. Le règlement CANCELED (99 900) ne compte pas.
     */
    @Test
    @WithMockUser(roles = "Administrateur")
    void r6_restesDusDuDossier_prixFraisTotal_horsReglementAnnule() throws Exception {
        modelesInitiaux();
        ComFolder dossier = fixture.prix(fixture.relance(
                fixture.dossier(jeu, jeu.jeton() + "-MT", fixture.enCours(jeu, "RES"), true), DATE_RELANCE, ECHEANCE),
                "1234000");
        fixture.echeance(dossier, Nature.AVANCE, "434000", "251300", null);
        fixture.echeance(dossier, Nature.AUTOFINANCEMENT, "800000", null, "99900");
        fixture.echeance(dossier, Nature.FRAIS, "31500", "10250", null);
        String nom = dossier.getCfgCustomer().getLastName();
        fixture.relire();

        List<String> pages = lettres();

        assertThat(pages).singleElement().satisfies(p -> assertThat(p)
                .contains(dossier.getCode())
                .contains(compact(nom))
                .contains("982700.00")
                .contains("21250.00")
                .contains("1003950.00")
                .doesNotContain("882800.00"));
    }
}
