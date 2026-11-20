package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Compte;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Jour;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.ValeursType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DEMANDE_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TOUS_LES_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.majuscules;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N2b R-N2b-5 (FEATURE-n2-travaux-chantier.md, {@code RessourceWithWorkFlow.cs:ExecuteWorkflow} l.208-300) : commentaire
 * obligatoire, date utilisateur et second valideur. 0 / 4 transition à exigence en prod : la fixture les pose sur
 * « Demande de validation ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierStatutSecondValideurTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String DETAIL = "$.detail";
    private static final String NOTES = "notes";
    private static final String SECOND = "secondValideur";
    private static final String COMMENTAIRE_OBLIGATOIRE = "Veuillez saisir un commentaire obligatoirement !";
    private static final String IDENTIFIANTS_INCORRECTS = "Login ou mot de passe incorrecte !";
    private static final String MEME_COMPTE = "Le 2ème utilisateur doit être différent de l'utilisateur connecté !";
    private static final String NON_HABILITE = "Le 2ème utilisateur n'a pas les habilitations requises pour cette action !";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID zone;
    private UUID type;
    private String motDePasse;
    private AdmUser utilisateur;
    private RequestPostProcessor qUtilisateur;
    private AdmUser second;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GSV-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        type = fixture.type("TV-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
        motDePasse = "Mdp-" + jeton + "-secret";
        Compte c = fixture.restreint(jeton + "u", chantier.projet(), TOUS_LES_STATUTS, List.of(T_DEMANDE), DROIT_LIRE);
        utilisateur = c.user();
        qUtilisateur = c.connexion();
        fixture.identifiants(utilisateur, motDePasse);
        second = fixture.restreint(jeton + "s", chantier.projet(), TOUS_LES_STATUTS, List.of(), DROIT_LIRE).user();
        fixture.identifiants(second, motDePasse);
    }

    private UUID jour(String code) {
        return fixture.jour(new Jour(chantier.societe(), chantier.projet(), type, zone, EN_COURS, DATE.atStartOfDay(),
                code + "-" + jeton, null));
    }

    private Reponse executer(UUID jour, Map<String, Object> valideur) throws Exception {
        Map<String, Object> corps = AppelsTravauxChantier.execution(T_DEMANDE);
        if (valideur != null) {
            corps.put(SECOND, valideur);
        }
        return appels.executer(jour, corps, qUtilisateur);
    }

    private void resteEnCours(SoftAssertions s, UUID jour) {
        s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(EN_COURS));
        s.assertThat(fixture.historique(jour)).isEmpty();
    }

    // ------------------------------------------------------------------ commentaire obligatoire (WithComment)

    @Test
    void r5_commentaireExige_proposeAvecCommentaire() throws Exception {
        fixture.exigences(T_DEMANDE, true, false, true);

        Reponse r = appels.transitions(jour("C0"), qUtilisateur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<List<Boolean>>lire("$[*].avecCommentaire")).containsExactly(true);
            s.assertThat(r.<List<Boolean>>lire("$[*].avecDateUtilisateur")).containsExactly(true);
            s.assertThat(r.<List<Boolean>>lire("$[*].avecSecondValideur")).containsExactly(false);
        });
    }

    @Test
    void r5_commentaireExige_notesAbsentes_400_rienNEstEcrit() throws Exception {
        fixture.exigences(T_DEMANDE, true, false, false);
        UUID jour = jour("C1");

        Reponse r = executer(jour, null);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<String>lire(DETAIL)).isEqualTo(COMMENTAIRE_OBLIGATOIRE);
            resteEnCours(s, jour);
        });
    }

    @Test
    void r5_commentaireExige_notesBlanches_400() throws Exception {
        fixture.exigences(T_DEMANDE, true, false, false);
        UUID jour = jour("C2");
        Map<String, Object> corps = AppelsTravauxChantier.execution(T_DEMANDE);
        corps.put(NOTES, "   ");

        Reponse r = appels.executer(jour, corps, qUtilisateur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<String>lire(DETAIL)).isEqualTo(COMMENTAIRE_OBLIGATOIRE);
            resteEnCours(s, jour);
        });
    }

    @Test
    void r5_commentaireExige_notesFournies_200_notesEnregistrees() throws Exception {
        fixture.exigences(T_DEMANDE, true, false, false);
        UUID jour = jour("C3");
        Map<String, Object> corps = AppelsTravauxChantier.execution(T_DEMANDE);
        corps.put(NOTES, "Motif " + jeton);

        Reponse r = appels.executer(jour, corps, qUtilisateur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.historique(jour)).extracting(l -> l.get("Notes")).containsExactly("Motif " + jeton);
    }

    @Test
    void r5_dateUtilisateur_enregistreeDansLHistorique() throws Exception {
        fixture.exigences(T_DEMANDE, false, false, true);
        UUID jour = jour("D1");
        Map<String, Object> corps = AppelsTravauxChantier.execution(T_DEMANDE);
        corps.put("dateUtilisateur", "2077-03-15T10:30:00");

        Reponse r = appels.executer(jour, corps, qUtilisateur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.historique(jour)).extracting(l -> String.valueOf(l.get("UserDate")))
                .containsExactly("2077-03-15 10:30:00.0");
    }

    // ------------------------------------------------------------------ second valideur (WithPassword)

    @Test
    void r5_motDePasseExige_proposeAvecSecondValideur() throws Exception {
        fixture.exigences(T_DEMANDE, false, true, false);

        Reponse r = appels.transitions(jour("P0"), qUtilisateur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.<List<Boolean>>lire("$[*].avecSecondValideur")).containsExactly(true);
    }

    @Test
    void r5_secondValideurAbsent_400_rienNEstEcrit() throws Exception {
        fixture.exigences(T_DEMANDE, false, true, false);
        UUID jour = jour("P1");

        Reponse r = executer(jour, null);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> resteEnCours(s, jour));
    }

    @Test
    void r5_motDePasseFaux_400_messageOmran_rienNEstEcrit() throws Exception {
        fixture.exigences(T_DEMANDE, false, true, false);
        UUID jour = jour("P2");

        Reponse r = executer(jour, AppelsTravauxChantier.secondValideur(second.getCode(), motDePasse + "x"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<String>lire(DETAIL)).isEqualTo(IDENTIFIANTS_INCORRECTS);
            s.assertThat(r.corps()).doesNotContain(motDePasse);
            resteEnCours(s, jour);
        });
    }

    @Test
    void r5_loginInconnu_400() throws Exception {
        fixture.exigences(T_DEMANDE, false, true, false);
        UUID jour = jour("P3");

        Reponse r = executer(jour, AppelsTravauxChantier.secondValideur("inconnu-" + jeton, motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(r.<String>lire(DETAIL)).isEqualTo(IDENTIFIANTS_INCORRECTS);
    }

    @Test
    void r5_memeCompte_403_rienNEstEcrit() throws Exception {
        fixture.exigences(T_DEMANDE, false, true, false);
        UUID jour = jour("P4");

        Reponse r = executer(jour, AppelsTravauxChantier.secondValideur(utilisateur.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<String>lire(DETAIL)).isEqualTo(MEME_COMPTE);
            resteEnCours(s, jour);
        });
    }

    @Test
    void r5_autreCompteValide_200_historiqueAvecLesDeuxLogins_sansMotDePasse() throws Exception {
        fixture.exigences(T_DEMANDE, false, true, false);
        UUID jour = jour("P5");

        Reponse r = executer(jour, AppelsTravauxChantier.secondValideur(second.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(DEMANDE_VALIDATION));
            s.assertThat(fixture.historique(jour)).extracting(l -> l.get("Description"))
                    .containsExactly("Demande de validation: En cours=>Demande de validation(" + utilisateur.getCode() + " & "
                            + second.getCode() + ")");
            s.assertThat(r.corps()).doesNotContain(motDePasse);
            s.assertThat(fixture.historique(jour).toString()).doesNotContain(motDePasse);
        });
    }

    @Test
    void r5_intervenantMotDePasse_secondSansLeRole_403() throws Exception {
        fixture.exigences(T_DEMANDE, false, true, false);
        Compte porteur = fixture.restreint(jeton + "p", chantier.projet(), TOUS_LES_STATUTS, List.of(), DROIT_LIRE);
        fixture.intervenantTransition(T_DEMANDE, porteur.role(), true);
        UUID jour = jour("P6");

        Reponse r = executer(jour, AppelsTravauxChantier.secondValideur(second.getCode(), motDePasse));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<String>lire(DETAIL)).isEqualTo(NON_HABILITE);
            resteEnCours(s, jour);
        });
    }

    @Test
    void r5_secondValideurNeContournePasLesIntervenants_409() throws Exception {
        fixture.exigences(T_DEMANDE, false, true, false);
        Compte sansTransition = fixture.restreint(jeton + "x", chantier.projet(), TOUS_LES_STATUTS, List.of(), DROIT_LIRE);
        UUID jour = jour("P7");
        UUID temoin = jour("P7T");
        Map<String, Object> corps = AppelsTravauxChantier.execution(T_DEMANDE);
        corps.put(SECOND, AppelsTravauxChantier.secondValideur(second.getCode(), motDePasse));

        Reponse refus = appels.executer(jour, corps, sansTransition.connexion());
        Reponse ok = executer(temoin, AppelsTravauxChantier.secondValideur(second.getCode(), motDePasse));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            resteEnCours(s, jour);
            s.assertThat(ok.statut()).as("témoin : " + ok.corps()).isEqualTo(200);
        });
    }
}
