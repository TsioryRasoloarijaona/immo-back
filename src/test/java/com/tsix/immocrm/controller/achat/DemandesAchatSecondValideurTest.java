package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
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
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.SECOND_VALIDEUR;
import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.TRANSITION_ID;
import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.secondValideur;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.TOUS_LES_DROITS;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6a R13, invariants 4 et 5 (FEATURE-n6-demandes-achat.md, D3) : transition « Valider » Préparation → Dépouillement à
 * {@code WithPassword = 1}. Le compte connecté (restreint) a un intervenant {@code WithPassword = 0} sur « Valider » ; les seconds
 * valideurs ont des identifiants Arpent ({@code AppUserCredential}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatSecondValideurTest {

    private static final String IDENTIFIANTS_INCORRECTS = "Login ou mot de passe incorrecte !";
    private static final String MEME_COMPTE = "Le 2ème utilisateur doit être différent de l'utilisateur connecté !";
    private static final String NON_HABILITE = "Le 2ème utilisateur n'a pas les habilitations requises pour cette action !";
    private static final String STATUT_ID = "StrStatusId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat fixture;

    private DemandesAchatAppels appels;
    private Cadre c;
    private UUID valider;
    private AdmUser utilisateur;
    private RequestPostProcessor qUtilisateur;
    private AdmUser second;
    private String motDePasse;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = fixture.cadre("SV");
        motDePasse = "Mdp-" + c.jeton() + "-secret";
        valider = fixture.transition(c.entite(), c.prep(), c.depouil(), "Valider " + c.jeton(), 10);
        fixture.exigences(valider, false, true, false);
        utilisateur = fixture.restreint(c, "n6.sv.u." + c.jeton(), TOUS_LES_DROITS);
        fixture.intervenantRole(c.entite(), fixture.roleDe(utilisateur), valider, false);
        fixture.identifiants(utilisateur, motDePasse);
        qUtilisateur = qui(utilisateur);
        second = fixture.restreint(c, "n6.sv.second." + c.jeton(), List.of());
        fixture.identifiants(second, motDePasse);
    }

    private UUID dia(String suffixe) {
        return fixture.demande(c, c.code(suffixe), c.prep());
    }

    private Reponse executer(UUID dia, CorpsJson valideur) throws Exception {
        CorpsJson corps = corps().avec(TRANSITION_ID, valider);
        if (valideur != null) {
            corps.avec(SECOND_VALIDEUR, valideur);
        }
        return appels.executer(qUtilisateur, dia, corps);
    }

    private String statutStocke(UUID dia) {
        return String.valueOf(fixture.demandeEnBase(dia).get(STATUT_ID)).toUpperCase();
    }

    private void resteEnPreparation(SoftAssertions s, UUID dia) {
        s.assertThat(statutStocke(dia)).isEqualTo(c.prep().toString().toUpperCase());
        s.assertThat(fixture.historique(dia)).isEmpty();
    }

    // ------------------------------------------------------------------ proposée, avec l'exigence

    @Test
    void r13_transitionAMotDePasse_proposee_avecSecondValideur() throws Exception {
        Reponse r = appels.transitions(qUtilisateur, dia("GET"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$[*].id")).containsExactly(valider.toString());
            s.assertThat(r.valeur("$[0].avecSecondValideur")).isEqualTo(true);
        });
    }

    // ------------------------------------------------------------------ 400 : absent, faux, désactivé

    @Test
    void r13_secondValideurAbsent_400() throws Exception {
        UUID dia = dia("ABS");

        Reponse r = executer(dia, null);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> resteEnPreparation(s, dia));
    }

    @Test
    void r13_motDePasseFaux_400() throws Exception {
        UUID dia = dia("FAUX");

        Reponse r = executer(dia, secondValideur(second.getCode(), motDePasse + "x"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL)).isEqualTo(IDENTIFIANTS_INCORRECTS);
            resteEnPreparation(s, dia);
        });
    }

    @Test
    void r13_loginInconnu_400() throws Exception {
        UUID dia = dia("INC");

        Reponse r = executer(dia, secondValideur("inconnu-" + c.jeton(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(r.valeur(DETAIL)).isEqualTo(IDENTIFIANTS_INCORRECTS);
    }

    @Test
    void r13_compteDesactive_400() throws Exception {
        fixture.desactiver(second);
        UUID dia = dia("DES");

        Reponse r = executer(dia, secondValideur(second.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL)).isEqualTo(IDENTIFIANTS_INCORRECTS);
            resteEnPreparation(s, dia);
        });
    }

    @Test
    void r13_compteSansIdentifiantsArpent_400() throws Exception {
        AdmUser sansIdentifiants = fixture.restreint(c, "n6.sv.sansid." + c.jeton(), List.of());
        UUID dia = dia("SID");

        Reponse r = executer(dia, secondValideur(sansIdentifiants.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(r.valeur(DETAIL)).isEqualTo(IDENTIFIANTS_INCORRECTS);
    }

    // ------------------------------------------------------------------ sans intervenant à mot de passe

    @Test
    void r13_sansIntervenantMotDePasse_memeCompte_403() throws Exception {
        UUID dia = dia("MEME");

        Reponse r = executer(dia, secondValideur(utilisateur.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL)).isEqualTo(MEME_COMPTE);
            resteEnPreparation(s, dia);
        });
    }

    @Test
    void r13_sansIntervenantMotDePasse_autreCompteValide_passe_historiqueAvecLesDeuxLogins() throws Exception {
        UUID dia = dia("AUTRE");

        Reponse r = executer(dia, secondValideur(second.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(statutStocke(dia)).isEqualTo(c.depouil().toString().toUpperCase());
            s.assertThat(fixture.historique(dia)).extracting(l -> l.get("Description"))
                    .containsExactly("Valider " + c.jeton() + ": PREP-" + c.jeton() + "=>DEP-" + c.jeton() + "("
                            + utilisateur.getCode() + " & " + second.getCode() + ")");
        });
    }

    // ------------------------------------------------------------------ avec intervenant à mot de passe sur un rôle R

    @Test
    void r13_intervenantMotDePasse_secondSansLeRole_403() throws Exception {
        AdmUser porteur = fixture.restreint(c, "n6.sv.r." + c.jeton(), List.of());
        fixture.intervenantRole(c.entite(), fixture.roleDe(porteur), valider, true);
        UUID dia = dia("NONHAB");

        Reponse r = executer(dia, secondValideur(second.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL)).isEqualTo(NON_HABILITE);
            resteEnPreparation(s, dia);
        });
    }

    @Test
    void r13_intervenantMotDePasse_secondAvecLeRole_passe() throws Exception {
        fixture.intervenantRole(c.entite(), fixture.roleDe(second), valider, true);
        UUID dia = dia("HAB");

        Reponse r = executer(dia, secondValideur(second.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(statutStocke(dia)).isEqualTo(c.depouil().toString().toUpperCase());
    }

    @Test
    void r13_intervenantMotDePasse_utilisateurLibre_estSonPropreSecond() throws Exception {
        AdmUser libre = fixture.libre(c, "n6.sv.libre." + c.jeton(), TOUS_LES_DROITS);
        fixture.identifiants(libre, motDePasse);
        fixture.intervenantRole(c.entite(), fixture.roleDe(second), valider, true);
        UUID dia = dia("LIBRE");

        Reponse r = appels.executer(qui(libre), dia, corps().avec(TRANSITION_ID, valider)
                .avec(SECOND_VALIDEUR, secondValideur(libre.getCode(), motDePasse)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(statutStocke(dia)).isEqualTo(c.depouil().toString().toUpperCase());
    }

    // ------------------------------------------------------------------ invariant 4 : le second ne contourne pas R10

    @Test
    void inv4_utilisateurSansLaTransition_secondHabilite_409() throws Exception {
        AdmUser sansTransition = fixture.restreint(c, "n6.sv.sans." + c.jeton(), TOUS_LES_DROITS);
        fixture.intervenantRole(c.entite(), fixture.roleDe(second), valider, true);
        UUID dia = dia("INV4");

        Reponse refus = appels.executer(qui(sansTransition), dia, corps().avec(TRANSITION_ID, valider)
                .avec(SECOND_VALIDEUR, secondValideur(second.getCode(), motDePasse)));
        Reponse temoin = executer(dia("INV4T"), secondValideur(second.getCode(), motDePasse));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            resteEnPreparation(s, dia);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ invariant 5 : le mot de passe ne ressort jamais

    @Test
    void inv5_motDePasse_absentDeLaReponseEtDeLHistorique() throws Exception {
        UUID dia = dia("MDP");

        Reponse succes = executer(dia, secondValideur(second.getCode(), motDePasse));
        Reponse refus = executer(dia("MDPR"), secondValideur(second.getCode(), motDePasse + "x"));
        Reponse historique = appels.historique(qUtilisateur, dia);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(succes.statut()).as(succes.corps()).isEqualTo(200);
            s.assertThat(succes.corps()).doesNotContain(motDePasse);
            s.assertThat(refus.corps()).doesNotContain(motDePasse);
            s.assertThat(historique.statut()).as(historique.corps()).isEqualTo(200);
            s.assertThat(historique.corps()).doesNotContain(motDePasse);
            s.assertThat(fixture.historique(dia).toString()).doesNotContain(motDePasse);
        });
    }
}
