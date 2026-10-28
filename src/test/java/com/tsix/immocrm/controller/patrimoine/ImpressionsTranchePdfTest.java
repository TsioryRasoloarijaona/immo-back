package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureImpressionsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import com.tsix.immocrm.fixtures.LectureExport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.MEILLEURTAUX;
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
 * Lot P9f — lettres PDF de la Relance 2 (FEATURE-patrimoine-impressions-tranche.md §1.1, §1.3, §2, §4 : T1, T2, D1, P3, ordre
 * des pages ; « Réponses du manager » 27/10 : pas de 409 sans modèle actif (repli sur le contenu livré), champs de P9d,
 * reste imprimé = {@code reste.prix}, reste ≤ 0 sans lettre). Le PDF est relu par OpenPDF ; le moteur de modèles n'est pas testé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpressionsTranchePdfTest {

    private static final String LETTRES = "/api/tranches/{id}/relances-2.pdf";
    private static final String APERCU = "/api/tranches/{id}/relances-2/apercu";
    private static final String MODELES = "/api/modeles-documents";
    private static final String CATEGORIE = "RELANCE_TITRE_FONCIER";
    private static final String STATUT = "statut";
    private static final String MARQUE_RELANCE_2 = "LETTREPERSORELANCEDEUX";
    private static final String MARQUE_RELANCE_1 = "LETTREPERSORELANCEUNE";
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureImpressionsTranche fixture;
    @Autowired private Clock clock;

    private AdmUser admin;
    private Jeu jeu;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeu = fixture.jeu();
    }

    // ------------------------------------------------------------------ outils

    private MvcResult pdf(Jeu j, String... statuts) throws Exception {
        MockHttpServletRequestBuilder requete = get(LETTRES, j.tranche().getId()).with(connecte(admin));
        if (statuts.length > 0) {
            requete = requete.param(STATUT, statuts);
        }
        return executer(mockMvc, requete);
    }

    /** Pages du PDF, sans aucun blanc ; échoue si la réponse n'est pas un PDF 200. */
    private List<String> pages(Jeu j, String... statuts) throws Exception {
        MvcResult resultat = pdf(j, statuts);
        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultat.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PDF_VALUE);
        byte[] contenu = resultat.getResponse().getContentAsByteArray();
        assertThat(estPdf(contenu)).isTrue();
        return pagesPdf(contenu).stream().map(LectureExport::compact).toList();
    }

    /** Montant au centime, séparateur décimal point ou virgule, séparateurs de milliers blancs (retirés par {@code compact}). */
    private static boolean porteMontant(String page, String entier, String centimes) {
        return Pattern.compile("(?<![0-9])" + entier + "[.,]" + centimes + "(?![0-9])").matcher(page).find();
    }

    private static String nom(ComFolder dossier) {
        return compact(dossier.getCfgCustomer().getLastName());
    }

    private void modele(String code, String marque) throws Exception {
        String contenu = "<p>" + marque + " <span data-champ=\\\"client.nom\\\">Nom</span> "
                + "<span data-champ=\\\"reste.prix\\\">Reste</span></p>";
        String corps = "{\"categorie\":\"" + CATEGORIE + "\",\"code\":\"" + code + "\",\"libelle\":\"Lettre " + code
                + "\",\"ordre\":10,\"contenu\":\"" + contenu + "\"}";
        mockMvc.perform(post(MODELES).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ T1 : une page par lettre, contenu livré sans modèle actif

    /**
     * Prix 1 234 567,89 ; avance réglée 250 000,45 ; frais 30 000 non réglés ⇒ reste.prix 984 567,44 imprimé ; reste.total
     * (frais compris, 1 014 567,44) jamais imprimé.
     */
    @Test
    void t1_sansModeleActif_contenuLivre_200_nomArticleEtResteDuPrixAuCentime() throws Exception {
        ComFolder dossier = fixture.prix(fixture.dossier(jeu, jeu.jeton() + "-T1", fixture.statut(CONCRETISATION)),
                "1234567.89");
        fixture.echeance(dossier, Nature.AVANCE, "600000", "250000.45", null);
        fixture.echeance(dossier, Nature.FRAIS, "30000", null, null);
        fixture.relire();

        List<String> pages = pages(jeu);

        assertThat(pages).singleElement().satisfies(p -> assertAll(
                () -> assertThat(p).contains(nom(dossier)),
                () -> assertThat(p).contains(compact(dossier.getStkItem().getCode())),
                () -> assertThat(porteMontant(p, "984567", "44")).as("reste.prix au centime").isTrue(),
                () -> assertThat(porteMontant(p, "1014567", "44")).as("reste.total jamais imprimé").isFalse()));
    }

    @Test
    void t1_modeleRelance2Actif_utilisePourLaLettre_etSignaleDansLApercu() throws Exception {
        ComFolder dossier = fixture.prix(fixture.dossier(jeu, jeu.jeton() + "-MA", fixture.statut(CONCRETISATION)),
                "500000.30");
        fixture.echeance(dossier, Nature.AVANCE, "500000.30", "100000.10", null);
        modele("RELANCE_2", MARQUE_RELANCE_2);
        fixture.relire();

        mockMvc.perform(get(APERCU, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modeleActif").value(true));
        List<String> pages = pages(jeu);

        assertThat(pages).singleElement().satisfies(p -> assertAll(
                () -> assertThat(p).contains(MARQUE_RELANCE_2),
                () -> assertThat(p).contains(nom(dossier)),
                () -> assertThat(porteMontant(p, "400000", "20")).isTrue()));
    }

    /** Le modèle de la 1re relance (même catégorie) ne sert jamais à la Relance 2. */
    @Test
    void t1_seulLeModeleDeLaPremiereRelance_nEstPasUtilise() throws Exception {
        fixture.dossier(jeu, jeu.jeton() + "-M1", fixture.statut(CONCRETISATION));
        modele("PREMIERE_RELANCE", MARQUE_RELANCE_1);
        fixture.relire();

        mockMvc.perform(get(APERCU, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modeleActif").value(false));
        List<String> pages = pages(jeu);

        assertThat(pages).singleElement().satisfies(p -> assertThat(p).doesNotContain(MARQUE_RELANCE_1));
    }

    // ------------------------------------------------------------------ P3 / M2 : une page par acquéreur, même reste

    @Test
    void p3_deuxCoAcquereurs_troisPages_memeResteDuPrix() throws Exception {
        ComFolder dossier = fixture.prix(fixture.dossier(jeu, jeu.jeton() + "-CO", fixture.statut(CONCRETISATION)),
                "900000.50");
        fixture.echeance(dossier, Nature.AVANCE, "900000.50", "300000.25", null);
        String coA = "COA" + jeu.jeton();
        String coB = "COB" + jeu.jeton();
        fixture.coAcquereur(dossier, coA);
        fixture.coAcquereur(dossier, coB);
        fixture.relire();

        List<String> pages = pages(jeu);

        assertThat(pages).hasSize(3)
                .allSatisfy(p -> assertThat(porteMontant(p, "600000", "25")).as("reste.prix 600 000,25").isTrue())
                .anySatisfy(p -> assertThat(p).contains(nom(dossier)))
                .anySatisfy(p -> assertThat(p).contains(coA))
                .anySatisfy(p -> assertThat(p).contains(coB));
    }

    // ------------------------------------------------------------------ §1.1 : ordre des pages

    @Test
    void ordre_articleEnTriNaturel_puisPrincipalAvantCoAcquereursParNom() throws Exception {
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);
        ComFolder dix = fixture.dossier(jeu, j + "-10", statut);
        ComFolder deux = fixture.dossier(jeu, j + "-2", statut);
        String zed = "ZED" + j;
        String alpha = "ALPHA" + j;
        fixture.coAcquereur(deux, zed);
        fixture.coAcquereur(deux, alpha);
        fixture.relire();

        List<String> pages = pages(jeu);

        assertThat(pages).hasSize(4);
        assertAll(
                () -> assertThat(pages.get(0)).contains(nom(deux)),
                () -> assertThat(pages.get(1)).contains(alpha),
                () -> assertThat(pages.get(2)).contains(zed),
                () -> assertThat(pages.get(3)).contains(nom(dix)));
    }

    // ------------------------------------------------------------------ Q3 : reste ≤ 0, aucune page

    @Test
    void q3_resteNulOuNegatif_aucunePage() throws Exception {
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);
        ComFolder solde = fixture.prix(fixture.dossier(jeu, j + "-SOL", statut), "200000");
        fixture.echeance(solde, Nature.AVANCE, "200000", "200000", null);
        ComFolder tropPercu = fixture.prix(fixture.dossier(jeu, j + "-TP", statut), "100000");
        fixture.echeance(tropPercu, Nature.AVANCE, "100000", "100000.01", null);
        fixture.coAcquereur(tropPercu, "COT" + j);
        ComFolder du = fixture.dossier(jeu, j + "-DU", statut);
        fixture.relire();

        List<String> pages = pages(jeu);

        assertThat(pages).singleElement().satisfies(p -> assertAll(
                () -> assertThat(p).contains(nom(du)),
                () -> assertThat(p).doesNotContain(nom(solde)),
                () -> assertThat(p).doesNotContain(nom(tropPercu))));
    }

    // ------------------------------------------------------------------ D1 : date de la lettre

    @Test
    void d1_dateDeLettre_relance2SinonAujourdhui_precedeeDeLaVilleDeSignature() throws Exception {
        String j = jeu.jeton();
        fixture.villeSignature(jeu, "Tanger");
        StrStatus statut = fixture.statut(CONCRETISATION);
        ComFolder avec = fixture.relance2(fixture.dossier(jeu, j + "-1", statut), LocalDateTime.of(2026, 2, 14, 0, 0));
        ComFolder sans = fixture.dossier(jeu, j + "-2", statut);
        fixture.relire();
        String aujourdhui = LocalDate.now(clock).format(JJ_MM_AAAA);

        List<String> pages = pages(jeu);

        assertThat(pages).hasSize(2);
        assertAll(
                () -> assertThat(pages).filteredOn(p -> p.contains(nom(avec))).singleElement()
                        .satisfies(p -> assertThat(p).contains("Tanger,le14/02/2026")),
                () -> assertThat(pages).filteredOn(p -> p.contains(nom(sans))).singleElement()
                        .satisfies(p -> assertThat(p).contains("Tanger,le" + aujourdhui)));
    }

    // ------------------------------------------------------------------ T2 : 0 lettre

    @Test
    void t2_aucunDossierAUnStatutARelancer_409() throws Exception {
        fixture.dossier(jeu, jeu.jeton() + "-RS", fixture.statut("RESERVATION"));
        fixture.relire();

        mockMvc.perform(get(LETTRES, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Aucun dossier de la tranche n'a un statut à relancer."));
    }

    // ------------------------------------------------------------------ §2 : en-têtes

    @Test
    void enTetes_inline_relance2_codeTranche_dateDuJour() throws Exception {
        fixture.dossier(jeu, jeu.jeton() + "-ET", fixture.statut(CONCRETISATION));
        fixture.relire();
        String attendu = "relance2_" + jeu.tranche().getCode() + "_"
                + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";

        MvcResult resultat = pdf(jeu);

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultat.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("inline").contains(attendu);
    }

    // ------------------------------------------------------------------ P5 : filtre statut sur le PDF

    @Test
    void p5_pdf_statutMeilleurTauxSeul_uneSeulePage() throws Exception {
        String j = jeu.jeton();
        ComFolder concretisation = fixture.dossier(jeu, j + "-C", fixture.statut(CONCRETISATION));
        ComFolder meilleurTaux = fixture.dossier(jeu, j + "-M", fixture.statut(MEILLEURTAUX));
        fixture.relire();

        List<String> pages = pages(jeu, MEILLEURTAUX);

        assertThat(pages).singleElement().satisfies(p -> assertAll(
                () -> assertThat(p).contains(nom(meilleurTaux)),
                () -> assertThat(p).doesNotContain(nom(concretisation))));
    }

    @Test
    void p5_pdf_statutHorsListe_400() throws Exception {
        fixture.dossier(jeu, jeu.jeton() + "-H", fixture.statut(CONCRETISATION));
        fixture.relire();

        mockMvc.perform(get(LETTRES, jeu.tranche().getId()).param(STATUT, "RESERVATION").with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }
}
