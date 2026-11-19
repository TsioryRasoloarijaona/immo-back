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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.IDS;
import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.NOTES;
import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.TRANSITION_ID;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.TOUS_LES_DROITS;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6a R10, R11, R12, invariants 1 et 2 (FEATURE-n6-demandes-achat.md). Entité de test : Préparation (Nouveau) → Dépouillement
 * (Intermédiaire) → Clôturé (Fin succès) / Annulé ; « Rouvrir » Clôturé → Préparation est la seule transition vers Préparation.
 * Compte restreint : intervenant de son rôle sur « Lancer » seulement ; compte libre : rôle à {@code WithSequenceFreeRestriction = 0}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatTransitionsTest {

    private static final String STATUT_ID = "StrStatusId";
    private static final String DESCRIPTION = "Description";
    private static final String NOTES_COL = "Notes";
    private static final String COMMENTAIRE_OBLIGATOIRE = "Veuillez saisir un commentaire obligatoirement !";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat fixture;

    private DemandesAchatAppels appels;
    private Cadre c;
    private UUID lancer;
    private UUID cloturer;
    private UUID annuler;
    private UUID rouvrir;
    private AdmUser restreint;
    private AdmUser libre;
    private RequestPostProcessor qRestreint;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = fixture.cadre("TR");
        lancer = fixture.transition(c.entite(), c.prep(), c.depouil(), "Lancer " + c.jeton(), 10);
        cloturer = fixture.transition(c.entite(), c.depouil(), c.clot(), "Cloturer " + c.jeton(), 10);
        annuler = fixture.transition(c.entite(), c.depouil(), c.annul(), "Annuler " + c.jeton(), 10);
        rouvrir = fixture.transition(c.entite(), c.clot(), c.prep(), "Rouvrir " + c.jeton(), 10);
        restreint = fixture.restreint(c, "n6.tr.restr." + c.jeton(), TOUS_LES_DROITS);
        fixture.intervenantRole(c.entite(), fixture.roleDe(restreint), lancer, false);
        qRestreint = qui(restreint);
        libre = fixture.libre(c, "n6.tr.libre." + c.jeton(), TOUS_LES_DROITS);
        qLibre = qui(libre);
    }

    private UUID dia(String suffixe, UUID statut) {
        return fixture.demande(c, c.code(suffixe), statut);
    }

    private Object statutDe(UUID dia) {
        return fixture.demandeEnBase(dia).get(STATUT_ID);
    }

    private static String id(UUID u) {
        return u.toString();
    }

    private static String enMajuscules(UUID u) {
        return u.toString().toUpperCase();
    }

    private String statutStocke(UUID dia) {
        return String.valueOf(statutDe(dia)).toUpperCase();
    }

    // ------------------------------------------------------------------ R10 utilisateur restreint

    @Test
    void r10_restreint_voitSeulementLaTransitionDeSonIntervenant() throws Exception {
        UUID dia = dia("R10A", c.prep());

        Reponse r = appels.transitions(qRestreint, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactly(id(lancer));
    }

    @Test
    void r10_transition_porteSaCibleEtSesExigences() throws Exception {
        fixture.exigences(lancer, true, false, true);
        UUID dia = dia("R10B", c.prep());

        Reponse r = appels.transitions(qRestreint, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$[0].libelle")).isEqualTo("Lancer " + c.jeton());
            s.assertThat(r.valeur("$[0].statutCible.id")).isEqualTo(id(c.depouil()));
            s.assertThat(r.valeur("$[0].statutCible.code")).isEqualTo("DEP-" + c.jeton());
            s.assertThat(r.valeur("$[0].statutCible.libelle")).isEqualTo("DEP-" + c.jeton());
            s.assertThat(r.valeur("$[0].statutCible.couleur")).isNotNull();
            s.assertThat(r.valeur("$[0].avecCommentaire")).isEqualTo(true);
            s.assertThat(r.valeur("$[0].avecDateUtilisateur")).isEqualTo(true);
            s.assertThat(r.valeur("$[0].avecSecondValideur")).isEqualTo(false);
        });
    }

    @Test
    void r10_restreint_intervenantCompte_suffit() throws Exception {
        AdmUser autre = fixture.restreint(c, "n6.tr.cpt." + c.jeton(), TOUS_LES_DROITS);
        fixture.intervenantCompte(c.entite(), autre, lancer, false);
        UUID dia = dia("R10C", c.prep());

        Reponse r = appels.transitions(qui(autre), dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactly(id(lancer));
    }

    @Test
    void r10_restreint_sansIntervenant_aucuneTransition() throws Exception {
        AdmUser seul = fixture.restreint(c, "n6.tr.seul." + c.jeton(), TOUS_LES_DROITS);
        UUID dia = dia("R10D", c.prep());

        Reponse r = appels.transitions(qui(seul), dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).isEmpty();
    }

    @Test
    void r10_intervenantAvecMotDePasseSeul_neDonnePasLaTransition() throws Exception {
        UUID role = fixture.roleDe(restreint);
        fixture.intervenantRole(c.entite(), role, cloturer, true);
        fixture.intervenantRole(c.entite(), role, annuler, false);
        UUID dia = dia("R10E", c.depouil());

        Reponse r = appels.transitions(qRestreint, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactly(id(annuler));
    }

    @Test
    void r10_transitionVersLeStatutCourant_exclue() throws Exception {
        UUID boucle = fixture.transition(c.entite(), c.depouil(), c.depouil(), "Boucle " + c.jeton(), 10);
        UUID role = fixture.roleDe(restreint);
        fixture.intervenantRole(c.entite(), role, boucle, false);
        fixture.intervenantRole(c.entite(), role, annuler, false);
        UUID dia = dia("R10F", c.depouil());

        Reponse r = appels.transitions(qRestreint, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactly(id(annuler));
    }

    // ------------------------------------------------------------------ R11 séquence libre

    @Test
    void r11_libre_depuisPreparation_unRepresentantParArrivee_horsStatutCourant() throws Exception {
        UUID dia = dia("R11A", c.prep());

        Reponse r = appels.transitions(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactlyInAnyOrder(id(lancer), id(cloturer), id(annuler));
    }

    @Test
    void r11_libre_clotureVersPreparation_proposeeDepuisDepouillement() throws Exception {
        UUID dia = dia("R11B", c.depouil());

        Reponse r = appels.transitions(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactlyInAnyOrder(id(rouvrir), id(cloturer), id(annuler));
    }

    @Test
    void r11_libre_executeRouvrir_historiqueEcritLeDepartDeLaTransition() throws Exception {
        UUID dia = dia("R11C", c.depouil());

        Reponse r = appels.executer(qLibre, dia, rouvrir);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.prep()));
        assertThat(fixture.historique(dia)).extracting(l -> l.get(DESCRIPTION))
                .containsExactly("Rouvrir " + c.jeton() + ": CLO-" + c.jeton() + "=>PREP-" + c.jeton()
                        + "(" + libre.getCode() + " & )");
    }

    @Test
    void r11_representant_premierParSorting() throws Exception {
        UUID tardive = fixture.transition(c.entite(), c.depouil(), c.clot(), "A-tard " + c.jeton(), 20);
        UUID dia = dia("R11D", c.depouil());

        Reponse r = appels.transitions(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).contains(id(cloturer)).doesNotContain(id(tardive));
    }

    @Test
    void r11_representant_aSortingEgal_premierParCode() throws Exception {
        UUID premiere = fixture.transition(c.entite(), c.annul(), c.clot(), "A-tot " + c.jeton(), 10);
        UUID dia = dia("R11E", c.depouil());

        Reponse r = appels.transitions(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).contains(id(premiere)).doesNotContain(id(cloturer));
    }

    @Test
    void inv1_r11_transitionNonRepresentante_nonProposee_409() throws Exception {
        UUID tardive = fixture.transition(c.entite(), c.depouil(), c.clot(), "A-tard " + c.jeton(), 20);
        UUID dia = dia("R11F", c.depouil());

        Reponse refus = appels.executer(qLibre, dia, tardive);
        Reponse temoin = appels.executer(qLibre, dia("R11G", c.depouil()), cloturer);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.depouil()));
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ invariant 1 : proposée = exécutable

    @Test
    void inv1_chaqueTransitionProposee_auRestreint_estExecutable() throws Exception {
        UUID role = fixture.roleDe(restreint);
        fixture.intervenantRole(c.entite(), role, cloturer, false);
        fixture.intervenantRole(c.entite(), role, annuler, false);
        Reponse proposees = appels.transitions(qRestreint, dia("INV1A", c.depouil()));
        assertThat(proposees.statut()).as(proposees.corps()).isEqualTo(200);
        List<String> ids = proposees.ids(IDS);
        assertThat(ids).hasSize(2);

        SoftAssertions s = new SoftAssertions();
        for (String t : ids) {
            Reponse r = appels.executer(qRestreint, dia("INV1-" + t.substring(0, 6), c.depouil()), UUID.fromString(t));
            s.assertThat(r.statut()).as(t + " " + r.corps()).isEqualTo(200);
        }
        s.assertAll();
    }

    @Test
    void inv1_chaqueTransitionProposee_auLibre_estExecutable() throws Exception {
        Reponse proposees = appels.transitions(qLibre, dia("INV1B", c.prep()));
        assertThat(proposees.statut()).as(proposees.corps()).isEqualTo(200);
        List<String> ids = proposees.ids(IDS);
        assertThat(ids).hasSize(3);

        SoftAssertions s = new SoftAssertions();
        for (String t : ids) {
            Reponse r = appels.executer(qLibre, dia("INV1-" + t.substring(0, 6), c.prep()), UUID.fromString(t));
            s.assertThat(r.statut()).as(t + " " + r.corps()).isEqualTo(200);
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R12 exécution

    @Test
    void r12_transitionHorsListe_409_avecLesTransitionsAutorisees_rienNEstEcrit() throws Exception {
        UUID dia = dia("R12A", c.prep());

        Reponse r = appels.executer(qRestreint, dia, cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.corps()).contains("transitionsAutorisees");
            s.assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.prep()));
            s.assertThat(fixture.historique(dia)).isEmpty();
        });
    }

    @Test
    void r12_execution_statutEtLigneDHistorique() throws Exception {
        UUID dia = dia("R12B", c.prep());

        Reponse r = appels.executer(qRestreint, dia, lancer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.depouil()));
        List<Map<String, Object>> historique = fixture.historique(dia);
        assertThat(historique).hasSize(1);
        Map<String, Object> ligne = historique.get(0);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ligne.get(DESCRIPTION)).isEqualTo("Lancer " + c.jeton() + ": PREP-" + c.jeton() + "=>DEP-"
                    + c.jeton() + "(" + restreint.getCode() + " & )");
            s.assertThat(String.valueOf(ligne.get("StrWorkflowId"))).isEqualToIgnoringCase(id(lancer));
            s.assertThat(String.valueOf(ligne.get(STATUT_ID))).isEqualToIgnoringCase(id(c.depouil()));
            s.assertThat(String.valueOf(ligne.get("StrConfigEntityId"))).isEqualToIgnoringCase(id(c.entite()));
            s.assertThat(String.valueOf(ligne.get("AdmUserId"))).isEqualToIgnoringCase(id(restreint.getId()));
            s.assertThat(ligne.get(NOTES_COL)).isEqualTo(""); // décision 14 : '' et non NULL
        });
    }

    @Test
    void r12_historique_lisibleParLaRoute() throws Exception {
        UUID dia = dia("R12C", c.prep());
        assertThat(appels.executer(qRestreint, dia, lancer).statut()).isEqualTo(200);

        Reponse r = appels.historique(qRestreint, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids("$[*].description")).contains("Lancer " + c.jeton() + ": PREP-" + c.jeton() + "=>DEP-" + c.jeton()
                + "(" + restreint.getCode() + " & )");
    }

    @Test
    void r12_commentaireExige_notesAbsentes_400_rienNEstEcrit() throws Exception {
        fixture.exigences(lancer, true, false, false);
        UUID dia = dia("R12D", c.prep());

        Reponse r = appels.executer(qRestreint, dia, lancer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL)).isEqualTo(COMMENTAIRE_OBLIGATOIRE);
            s.assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.prep()));
            s.assertThat(fixture.historique(dia)).isEmpty();
        });
    }

    @Test
    void r12_commentaireExige_notesVides_400() throws Exception {
        fixture.exigences(lancer, true, false, false);
        UUID dia = dia("R12E", c.prep());

        Reponse r = appels.executer(qRestreint, dia, corps().avec(TRANSITION_ID, lancer).avec(NOTES, ""));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(r.valeur(DETAIL)).isEqualTo(COMMENTAIRE_OBLIGATOIRE);
    }

    @Test
    void r12_commentaireExige_notesFournies_enregistrees() throws Exception {
        fixture.exigences(lancer, true, false, false);
        UUID dia = dia("R12F", c.prep());

        Reponse r = appels.executer(qRestreint, dia, corps().avec(TRANSITION_ID, lancer).avec(NOTES, "Motif " + c.jeton()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.historique(dia)).extracting(l -> l.get(NOTES_COL)).containsExactly("Motif " + c.jeton());
    }

    @Test
    void r12_notesIgnorees_sansCommentaireNiDateUtilisateur() throws Exception {
        UUID dia = dia("R12G", c.prep());

        Reponse r = appels.executer(qRestreint, dia, corps().avec(TRANSITION_ID, lancer).avec(NOTES, "Ignoree " + c.jeton()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.historique(dia)).hasSize(1).extracting(l -> l.get(NOTES_COL)).containsExactly(""); // décision 14
    }

    @Test
    void r12_dateUtilisateur_notesEnregistrees_etDateParDefaut() throws Exception {
        fixture.exigences(lancer, false, false, true);
        UUID dia = dia("R12H", c.prep());

        Reponse r = appels.executer(qRestreint, dia, corps().avec(TRANSITION_ID, lancer).avec(NOTES, "Daté " + c.jeton()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> historique = fixture.historique(dia);
        assertThat(historique).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(historique.get(0).get(NOTES_COL)).isEqualTo("Daté " + c.jeton());
            s.assertThat(historique.get(0).get("UserDate")).isNotNull();
        });
    }

    @Test
    void r12_finSucces_lotNonMulti_409_enN6a_rienNEstEcrit() throws Exception {
        UUID dia = dia("R12I", c.depouil());
        fixture.poser(FixtureDemandesAchat.DEMANDE, dia, "PrjMarketId", fixture.lot(c, "LNM-" + c.jeton(), false));

        Reponse r = appels.executer(qLibre, dia, cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.depouil()));
            s.assertThat(fixture.historique(dia)).isEmpty();
        });
    }

    @Test
    void r12_finSucces_sansLot_passe() throws Exception {
        UUID dia = dia("R12J", c.depouil());

        Reponse r = appels.executer(qLibre, dia, cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.clot()));
    }

    @Test
    void r12_finSucces_lotMulti_passe() throws Exception {
        UUID dia = dia("R12K", c.depouil());
        fixture.poser(FixtureDemandesAchat.DEMANDE, dia, "PrjMarketId", fixture.lot(c, "LM-" + c.jeton(), true));

        Reponse r = appels.executer(qLibre, dia, cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.clot()));
    }

    // ------------------------------------------------------------------ invariant 2 : le corps ment

    @Test
    void inv2_statutEtEntiteDuCorps_ignores_leStatutStockeDecide() throws Exception {
        fixture.intervenantRole(c.entite(), fixture.roleDe(restreint), cloturer, false);
        UUID dia = dia("INV2A", c.prep());
        UUID autreEntite = fixture.entite("AUTRE-" + c.jeton());

        Reponse refus = appels.executer(qRestreint, dia, corps().avec(TRANSITION_ID, cloturer)
                .avec("statutId", c.depouil()).avec("statutActuelId", c.depouil()).avec("entiteId", autreEntite)
                .avec("statut", corps().avec("id", c.depouil())));
        Reponse temoin = appels.executer(qRestreint, dia("INV2B", c.depouil()), cloturer);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(statutStocke(dia)).isEqualTo(enMajuscules(c.prep()));
            s.assertThat(fixture.historique(dia)).isEmpty();
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ R9 visibilité des routes de transition

    @Test
    void r9_transitionsEtHistorique_diaHorsPerimetre_404() throws Exception {
        Cadre ailleurs = fixture.cadre("TRX");
        UUID invisible = fixture.demande(ailleurs, ailleurs.code("INV"), ailleurs.prep());
        UUID visible = dia("VIS", c.prep());

        Reponse temoin = appels.transitions(qLibre, visible);
        Reponse lecture = appels.transitions(qLibre, invisible);
        Reponse execution = appels.executer(qLibre, invisible, corps().avec(TRANSITION_ID, lancer)
                .avec("projetId", c.projet()).avec("societeId", c.societe()));
        Reponse historique = appels.historique(qLibre, invisible);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
            s.assertThat(lecture.statut()).isEqualTo(404);
            s.assertThat(execution.statut()).isEqualTo(404);
            s.assertThat(historique.statut()).isEqualTo(404);
            s.assertThat(statutStocke(invisible)).isEqualTo(enMajuscules(ailleurs.prep()));
        });
    }
}
