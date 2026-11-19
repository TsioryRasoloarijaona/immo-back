package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.IDS;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_EXPORTER;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_LECTURE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.TOUS_LES_DROITS;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6a liste, export et fiche : R8 (phases), R9 / D5 (visibilité société, projet, statut ; liste toujours filtrée par statut, Q4),
 * D6 (confidentialité), D7 (droits de lecture et d'export), invariant 3 (404 avant 403) — FEATURE-n6-demandes-achat.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatLectureTest {

    private static final String CHAMP_DATE = "champDate";
    private static final String DOC_DATE = "DocDate";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat fixture;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = fixture.cadre("LE");
        qLibre = qui(fixture.libre(c, "n6.le.libre." + c.jeton(), TOUS_LES_DROITS));
    }

    private UUID dia(String suffixe, UUID statut) {
        return fixture.demande(c, c.code(suffixe), statut);
    }

    private static String id(UUID u) {
        return u.toString();
    }

    private static String ligne(UUID dia, String champ) {
        return "$[?(@.id == '" + dia + "')]." + champ;
    }

    // ------------------------------------------------------------------ liste : ligne et droits

    @Test
    void liste_ligneDeLaDia() throws Exception {
        UUID dia = dia("L1", c.depouil());
        fixture.poser(DEMANDE, dia, "Notes", "Note " + c.jeton());
        fixture.poser(DEMANDE, dia, "PriceEstimatedTTC", new BigDecimal("1234.50"));

        Reponse r = appels.lister(qLibre, "societeIds", id(c.societe()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).contains(id(dia));
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(ligne(dia, "code"))).containsExactly(c.code("L1"));
            s.assertThat(r.ids(ligne(dia, "description"))).containsExactly("DIA " + c.code("L1"));
            s.assertThat(r.ids(ligne(dia, "statut.id"))).containsExactly(id(c.depouil()));
            s.assertThat(r.ids(ligne(dia, "statut.libelle"))).containsExactly("DEP-" + c.jeton());
            s.assertThat(r.ids(ligne(dia, "statut.couleur"))).singleElement().isNotNull();
            s.assertThat(r.ids(ligne(dia, "notes"))).containsExactly("Note " + c.jeton());
            s.assertThat(((List<?>) r.valeur(ligne(dia, "montantEstimeTTC"))).stream()
                            .map(m -> new BigDecimal(String.valueOf(m))).toList())
                    .usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("1234.50"));
            s.assertThat(String.valueOf(r.valeur(ligne(dia, "estDerniere")))).isEqualTo("[true]");
        });
    }

    @Test
    void d7_liste_sansDroitDeLecture_403() throws Exception {
        dia("L2", c.prep());
        AdmUser sansDroit = fixture.libre(c, "n6.le.sans." + c.jeton(), List.of(DROIT_NOUVEAU));

        Reponse r = appels.lister(qui(sansDroit));
        Reponse temoin = appels.lister(qLibre);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void d7_export_sansToolExport_403() throws Exception {
        dia("L3", c.prep());
        AdmUser lecteur = fixture.libre(c, "n6.le.lect." + c.jeton(), List.of(DROIT_LECTURE));
        AdmUser exportateur = fixture.libre(c, "n6.le.exp." + c.jeton(), List.of(DROIT_LECTURE, DROIT_EXPORTER));

        Reponse r = appels.exporter(qui(lecteur));
        Reponse temoin = appels.exporter(qui(exportateur));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ D5 visibilité (liste et fiche)

    @Test
    void d5_societeNonHabilitee_absenteDeLaListe_ficheEn404() throws Exception {
        Cadre ailleurs = fixture.cadre("LEX");
        UUID invisible = fixture.demande(ailleurs, ailleurs.code("X"), ailleurs.prep());
        UUID visible = dia("L4", c.prep());

        Reponse liste = appels.lister(qLibre);
        Reponse fiche = appels.fiche(qLibre, invisible);
        Reponse temoin = appels.fiche(qLibre, visible);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
            s.assertThat(liste.ids(IDS)).contains(id(visible)).doesNotContain(id(invisible));
            s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(404);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void d5_projetNonHabilite_absent_maisSansProjetVisible() throws Exception {
        UUID projetFerme = fixture.projetDe(c, "FERME-" + c.jeton());
        UUID horsProjet = dia("L5", c.prep());
        fixture.poser(DEMANDE, horsProjet, "PrjProjectId", projetFerme);
        UUID sansProjet = dia("L6", c.prep());
        fixture.sansProjet(sansProjet);

        Reponse liste = appels.lister(qLibre);
        Reponse fiche = appels.fiche(qLibre, horsProjet);
        Reponse temoin = appels.fiche(qLibre, sansProjet);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
            s.assertThat(liste.ids(IDS)).contains(id(sansProjet)).doesNotContain(id(horsProjet));
            s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(404);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void q4_statutNonVisible_absentDeLaListeSansFiltreDeProcedure_ficheEn404() throws Exception {
        AdmUser restreint = fixture.restreintSansStatut(c, "n6.le.restr." + c.jeton(), TOUS_LES_DROITS);
        fixture.voitStatuts(c.entite(), fixture.roleDe(restreint), List.of(c.prep(), c.depouil()));
        UUID visible = dia("L7", c.prep());
        UUID cachee = dia("L8", c.clot());

        Reponse liste = appels.lister(qui(restreint));
        Reponse fiche = appels.fiche(qui(restreint), cachee);
        Reponse temoin = appels.fiche(qui(restreint), visible);
        Reponse libre = appels.lister(qLibre);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
            s.assertThat(liste.ids(IDS)).contains(id(visible)).doesNotContain(id(cachee));
            s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(404);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
            s.assertThat(libre.ids(IDS)).contains(id(visible), id(cachee));
        });
    }

    @Test
    void inv3_fiche_horsPerimetreSansDroit_404_visibleSansDroit_403() throws Exception {
        Cadre ailleurs = fixture.cadre("LEY");
        UUID invisible = fixture.demande(ailleurs, ailleurs.code("Y"), ailleurs.prep());
        UUID visible = dia("L9", c.prep());
        AdmUser sansDroit = fixture.libre(c, "n6.le.nd." + c.jeton(), List.of());

        Reponse horsPerimetre = appels.fiche(qui(sansDroit), invisible);
        Reponse dansPerimetre = appels.fiche(qui(sansDroit), visible);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(horsPerimetre.statut()).as(horsPerimetre.corps()).isEqualTo(404);
            s.assertThat(dansPerimetre.statut()).as(dansPerimetre.corps()).isEqualTo(403);
        });
    }

    // ------------------------------------------------------------------ D6 confidentialité

    @Test
    void d6_procedureConfidentielle_absenteSansLeDroit_presenteAvec_ficheLisible() throws Exception {
        fixture.confidentielle(c.procedure());
        UUID dia = dia("L10", c.prep());
        AdmUser sansConfidentiel = fixture.libre(c, "n6.le.conf." + c.jeton(), List.of(DROIT_LECTURE));

        Reponse liste = appels.lister(qui(sansConfidentiel));
        Reponse fiche = appels.fiche(qui(sansConfidentiel), dia);
        Reponse avecDroit = appels.lister(qLibre);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
            s.assertThat(liste.ids(IDS)).doesNotContain(id(dia));
            s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(200);
            s.assertThat(fiche.corps()).contains("\"confidentielle\":true");
            s.assertThat(avecDroit.ids(IDS)).contains(id(dia));
        });
    }

    // ------------------------------------------------------------------ filtres

    @Test
    void filtre_champDateInconnu_400() throws Exception {
        Reponse r = appels.lister(qLibre, CHAMP_DATE, "PasUneDate");
        Reponse temoin = appels.lister(qLibre, CHAMP_DATE, DOC_DATE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void filtre_periodeSurDocDate() throws Exception {
        UUID mars = dia("L11", c.prep());
        UUID avril = fixture.demande(c, c.code("L12"), c.prep(), c.procedure(), LocalDate.of(2026, 4, 15));

        Reponse r = appels.lister(qLibre, CHAMP_DATE, DOC_DATE, "du", "2026-04-01", "au", "2026-04-30");

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).contains(id(avril)).doesNotContain(id(mars));
    }

    @Test
    void filtre_procedureEtStatuts() throws Exception {
        UUID autreProcedure = fixture.procedure("PAUTRE-" + c.jeton(), c.entite());
        UUID cible = dia("L13", c.depouil());
        UUID autreStatut = dia("L14", c.prep());
        UUID autreProc = fixture.demande(c, c.code("L15"), c.depouil(), autreProcedure, LocalDate.of(2026, 3, 10));

        Reponse r = appels.lister(qLibre, "procedureId", id(c.procedure()), "statutIds", id(c.depouil()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).contains(id(cible)).doesNotContain(id(autreStatut), id(autreProc));
    }

    @Test
    void filtre_rechercheDansDescriptionOuNotes() throws Exception {
        UUID parDescription = dia("L16", c.prep());
        UUID parNotes = dia("L17", c.prep());
        fixture.poser(DEMANDE, parNotes, "Notes", "aiguille-" + c.jeton());
        fixture.poser(DEMANDE, parDescription, "Description", "Achat aiguille-" + c.jeton());
        UUID autre = dia("L18", c.prep());

        Reponse r = appels.lister(qLibre, "recherche", "aiguille-" + c.jeton());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).contains(id(parDescription), id(parNotes)).doesNotContain(id(autre));
    }

    @Test
    void filtre_fournisseurSurLaRaisonSociale() throws Exception {
        UUID cible = dia("L19", c.prep());
        fixture.fournisseur(cible, "F-" + c.jeton(), "Fournisseur Zeta " + c.jeton());
        UUID autre = dia("L20", c.prep());

        Reponse r = appels.lister(qLibre, "fournisseur", "Zeta " + c.jeton());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).contains(id(cible)).doesNotContain(id(autre));
    }

    // ------------------------------------------------------------------ fiche : R8 phases, estDerniere, lot multi

    @Test
    void r8_phases_suiventLeStatutStocke() throws Exception {
        fixture.phases(c.depouil(), true, true, true, true);
        UUID dia = dia("L21", c.depouil());

        Reponse r = appels.fiche(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.phases.bordereau")).isEqualTo(true);
            s.assertThat(r.valeur("$.phases.preselection")).isEqualTo(true);
            s.assertThat(r.valeur("$.phases.offres")).isEqualTo(true);
            s.assertThat(r.valeur("$.phases.selection")).isEqualTo(true);
            s.assertThat(r.valeur("$.confidentielle")).isEqualTo(false);
        });
    }

    @Test
    void r8_phases_fermeesSiStatutSansDrapeau() throws Exception {
        fixture.phases(c.prep(), true, false, false, false);
        UUID dia = dia("L22", c.prep());

        Reponse r = appels.fiche(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.phases.bordereau")).isEqualTo(true);
            s.assertThat(r.valeur("$.phases.preselection")).isEqualTo(false);
            s.assertThat(r.valeur("$.phases.offres")).isEqualTo(false);
            s.assertThat(r.valeur("$.phases.selection")).isEqualTo(false);
        });
    }

    @Test
    void r7_r8_diaNonDerniere_phasesFermees() throws Exception {
        fixture.phases(c.depouil(), true, true, true, true);
        UUID dia = dia("L23", c.depouil());
        fixture.poser(DEMANDE, dia, "IsTheLast", false);

        Reponse r = appels.fiche(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.estDerniere")).isEqualTo(false);
            s.assertThat(r.valeur("$.phases.bordereau")).isEqualTo(false);
            s.assertThat(r.valeur("$.phases.preselection")).isEqualTo(false);
            s.assertThat(r.valeur("$.phases.offres")).isEqualTo(false);
            s.assertThat(r.valeur("$.phases.selection")).isEqualTo(false);
        });
    }

    @Test
    void fiche_lotMultiFournisseur_luSurLeLot() throws Exception {
        UUID multi = dia("L24", c.prep());
        fixture.poser(DEMANDE, multi, "PrjMarketId", fixture.lot(c, "LM-" + c.jeton(), true));
        UUID simple = dia("L25", c.prep());
        fixture.poser(DEMANDE, simple, "PrjMarketId", fixture.lot(c, "LS-" + c.jeton(), false));

        Reponse rMulti = appels.fiche(qLibre, multi);
        Reponse rSimple = appels.fiche(qLibre, simple);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(rMulti.statut()).as(rMulti.corps()).isEqualTo(200);
            s.assertThat(rMulti.valeur("$.lotMultiFournisseur")).isEqualTo(true);
            s.assertThat(rSimple.valeur("$.lotMultiFournisseur")).isEqualTo(false);
            s.assertThat(rMulti.valeur("$.code")).isEqualTo(c.code("L24"));
        });
    }
}
