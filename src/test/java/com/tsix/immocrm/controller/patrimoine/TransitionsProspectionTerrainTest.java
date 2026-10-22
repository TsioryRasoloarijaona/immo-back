package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Options;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Statut;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static com.tsix.immocrm.fixtures.FixtureTerrains.horodatage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Statuts et transitions — FEATURE-patrimoine-prospections-terrain.md §1.2 et §4 (TransitionsProspectionTerrainTest) :
 * transition permise (statut, StrHistory, validité, archivage), 409 ProblemDetail listant les transitions permises,
 * commentaire obligatoire, intervenants, seconde identification, statut en lecture seule, suppression interdite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionsProspectionTerrainTest {

    private static final String HISTORIQUE_SQL =
            "SELECT * FROM StrHistory WHERE ObjectId = :id AND StrWorkflowId IS NOT NULL ORDER BY DocDate ASC";
    private static final String TRANSITIONS_AUTORISEES = "$.transitionsAutorisees";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;

    private AdmUser admin;
    private ApiProspectionsTerrain api;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        api = new ApiProspectionsTerrain(mockMvc, admin);
        c = fixture.contexte("P7W");
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    private Tuple ligne(UUID id) {
        return fixture.ligne(id);
    }

    private List<Tuple> historique(UUID id) {
        return fixture.lignes(HISTORIQUE_SQL, id);
    }

    // ------------------------------------------------------------------ transition permise

    /** §4 : En cours → Validé → 200, statut posé, fiche renvoyée au nouveau statut. */
    @Test
    void r4_transitionPermise_200_statutPose() throws Exception {
        UUID id = fixture.prospection(c, code("OK"));

        api.executer(id, T_EN_COURS_VALIDE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut.code").value(VALIDE));

        assertThat(guid(ligne(id).get("StrStatusId"))).isEqualTo(guid(STATUT_VALIDE));
    }

    /**
     * §1.2 / §4 : StrHistory de la transition — description « {transition}: {transition}({login} & ) » (code = libellé dans
     * la configuration de production), login, nom affiché, transition, statut d'arrivée, commentaire, entité LndLand.
     */
    @Test
    void r4_transition_strHistory_auFormatOmran() throws Exception {
        UUID id = fixture.prospection(c, code("HIS"));

        api.executer(id, corps().avec(TRANSITION_ID, T_EN_COURS_NON_ABOUTIE).avec(COMMENTAIRE, "Voisinage difficile"))
                .andExpect(status().isOk());

        List<Tuple> lignes = historique(id);
        assertThat(lignes).hasSize(1);
        Tuple h = lignes.get(0);
        String libelle = EN_COURS + "=>" + NON_ABOUTIE;
        assertAll(
                () -> assertThat(h.get("Description")).isEqualTo(libelle + ": " + libelle + "(" + admin.getCode() + " & )"),
                () -> assertThat(h.get("Notes")).isEqualTo("Voisinage difficile"),
                () -> assertThat(guid(h.get("StrWorkflowId"))).isEqualTo(guid(T_EN_COURS_NON_ABOUTIE)),
                () -> assertThat(h.get("StrWorkflowDescription")).isEqualTo(libelle),
                () -> assertThat(guid(h.get("StrStatusId"))).isEqualTo(guid(STATUT_NON_ABOUTIE)),
                () -> assertThat(h.get("StrStatusDescription")).isEqualTo(NON_ABOUTIE),
                () -> assertThat(guid(h.get("AdmUserId"))).isEqualTo(guid(admin.getId())),
                () -> assertThat(h.get("AdmUserDescription")).isEqualTo(FixturePatrimoine.AUTEUR),
                () -> assertThat(guid(h.get("StrConfigEntityId"))).isEqualTo(guid(fixture.entiteLndLand())),
                () -> assertThat(h.get("UserDate")).isNotNull());
    }

    /** §4 : WithUserDate → dateUtilisateur du corps devient StrHistory.UserDate. */
    @Test
    void r4_withUserDate_dateUtilisateurDansLHistorique() throws Exception {
        UUID cible = fixture.statut(code("UD"), 260, TYPE_SUCCES, Statut.ordinaire());
        UUID t = fixture.transition(STATUT_EN_COURS, cible, code("T-UD"), new Options(false, false, true));
        UUID id = fixture.prospection(c, code("WUD"));

        api.executer(id, corps().avec(TRANSITION_ID, t).avec(DATE_UTILISATEUR, "2026-09-15T10:30:00"))
                .andExpect(status().isOk());

        assertThat(horodatage(historique(id).get(0).get("UserDate"))).isEqualTo(LocalDateTime.of(2026, 9, 15, 10, 30));
    }

    // ------------------------------------------------------------------ validité du statut

    /** §4 / §13 décision 2 : statut cible avec Delay > 0 → StatusValidityDate = DocDate + Delay jours. */
    @Test
    void r4_validite_cibleAvecDuree_docDatePlusDuree() throws Exception {
        UUID cible = fixture.statut(code("D10"), 270, TYPE_SUCCES, new Statut(false, true, false, 10));
        UUID t = fixture.transition(STATUT_EN_COURS, cible, code("T-D10"), Options.aucune());
        UUID id = fixture.prospection(c, code("VDD"));

        api.executer(id, t).andExpect(status().isOk());

        assertThat(horodatage(ligne(id).get("StatusValidityDate")))
                .isEqualTo(DATE_PROSPECTION.plusDays(10));
    }

    /** §4 : statut cible avec Delay = 0 → date de validité inchangée (jamais effacée). */
    @Test
    void r4_validite_cibleSansDuree_inchangee() throws Exception {
        UUID id = fixture.prospection(c, code("VSD"));
        fixture.definir(id, "StatusValidityDate", LocalDate.of(2026, 12, 1).atStartOfDay());

        api.executer(id, T_EN_COURS_VALIDE).andExpect(status().isOk());

        assertThat(horodatage(ligne(id).get("StatusValidityDate"))).isEqualTo(LocalDate.of(2026, 12, 1).atStartOfDay());
    }

    // ------------------------------------------------------------------ archivage

    /** §4 : cible « Terminé avec échec » → IsInArchive = 1. */
    @Test
    void r4_archivage_versEchec() throws Exception {
        UUID id = fixture.prospection(c, code("AEC"));

        api.executer(id, corps().avec(TRANSITION_ID, T_EN_COURS_NON_ABOUTIE).avec(COMMENTAIRE, "Echec"))
                .andExpect(status().isOk());

        assertThat(ligne(id).get("IsInArchive")).isEqualTo(true);
    }

    /** §4 : cible « Annulation » → IsInArchive = 1. */
    @Test
    void r4_archivage_versAnnulation() throws Exception {
        UUID id = fixture.prospection(c, code("AAN"));

        api.executer(id, corps().avec(TRANSITION_ID, T_EN_COURS_REJETE).avec(COMMENTAIRE, "Rejet"))
                .andExpect(status().isOk());

        assertThat(ligne(id).get("IsInArchive")).isEqualTo(true);
    }

    /** §4 : cible « Terminé avec succès » → pas d'archivage. */
    @Test
    void r4_archivage_versSucces_nonArchivee() throws Exception {
        UUID id = fixture.prospection(c, code("ASU"));

        api.executer(id, T_EN_COURS_VALIDE).andExpect(status().isOk());

        assertThat(ligne(id).get("IsInArchive")).isEqualTo(false);
    }

    /** §4 : jamais remis à 0 automatiquement — Non aboutie → En cours laisse la prospection archivée. */
    @Test
    void r4_retourEnCours_resteArchivee() throws Exception {
        UUID id = fixture.prospection(c, code("RAR"), STATUT_NON_ABOUTIE);
        fixture.definir(id, "IsInArchive", true);

        api.executer(id, T_NON_ABOUTIE_EN_COURS).andExpect(status().isOk());

        Tuple t = ligne(id);
        assertAll(
                () -> assertThat(guid(t.get("StrStatusId"))).isEqualTo(guid(STATUT_EN_COURS)),
                () -> assertThat(t.get("IsInArchive")).isEqualTo(true));
    }

    // ------------------------------------------------------------------ 409 passage interdit

    /** §4 : depuis « Rejeté » (aucune sortie) → 409 ProblemDetail, liste des transitions permises vide, statut inchangé. */
    @Test
    void r4_depuisRejete_409_aucuneTransitionPermise() throws Exception {
        UUID id = fixture.prospection(c, code("REJ"), STATUT_REJETE);

        api.executer(id, T_EN_COURS_VALIDE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, empty()));

        assertThat(guid(ligne(id).get("StrStatusId"))).isEqualTo(guid(STATUT_REJETE));
    }

    /** §4 : transition hors graphe depuis « En cours » → 409 listant les cibles permises (Validé, Non aboutie, Rejeté). */
    @Test
    void r4_cibleHorsGraphe_409_listeLesTransitionsPermises() throws Exception {
        UUID id = fixture.prospection(c, code("HG"));

        api.executer(id, T_NON_ABOUTIE_EN_COURS)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, hasItems(VALIDE, NON_ABOUTIE, REJETE)))
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, not(hasItem(EN_COURS))));

        assertThat(guid(ligne(id).get("StrStatusId"))).isEqualTo(guid(STATUT_EN_COURS));
    }

    // ------------------------------------------------------------------ commentaire, seconde identification

    /** §4 : WithComment et commentaire absent → 400 « Veuillez saisir un commentaire obligatoirement ! », rien d'écrit. */
    @Test
    void r4_commentaireObligatoire_absent_400() throws Exception {
        UUID id = fixture.prospection(c, code("COM"));

        api.executer(id, T_EN_COURS_REJETE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_COMMENTAIRE)));

        assertAll(
                () -> assertThat(guid(ligne(id).get("StrStatusId"))).isEqualTo(guid(STATUT_EN_COURS)),
                () -> assertThat(historique(id)).isEmpty());
    }

    /** §4 : commentaire vide (blancs) → 400 également. */
    @Test
    void r4_commentaireObligatoire_blanc_400() throws Exception {
        UUID id = fixture.prospection(c, code("CBL"));

        api.executer(id, corps().avec(TRANSITION_ID, T_EN_COURS_NON_ABOUTIE).avec(COMMENTAIRE, "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_COMMENTAIRE)));
    }

    /** §4 : WithPassword → 409 « Cette transition exige un second utilisateur : non prise en charge. ». */
    @Test
    void r4_withPassword_409() throws Exception {
        UUID cible = fixture.statut(code("PWD"), 280, TYPE_SUCCES, Statut.ordinaire());
        UUID t = fixture.transition(STATUT_EN_COURS, cible, code("T-PWD"), new Options(false, true, false));
        UUID id = fixture.prospection(c, code("WPW"));

        api.executer(id, t)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_SECOND_UTILISATEUR)));

        assertThat(guid(ligne(id).get("StrStatusId"))).isEqualTo(guid(STATUT_EN_COURS));
    }

    // ------------------------------------------------------------------ intervenants

    /** §4 : transition paramétrée (intervenant rôle) → l'Administrateur passe partout (TransitionsDossier.autorisees). */
    @Test
    void r4_intervenant_administrateurPasse() throws Exception {
        fixture.intervenantRole(T_EN_COURS_VALIDE, "ROLE-" + c.jeton());
        UUID id = fixture.prospection(c, code("IAD"));

        api.transitions(id)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(T_EN_COURS_VALIDE.toString())));
        api.executer(id, T_EN_COURS_VALIDE).andExpect(status().isOk());
    }

    /** §4 : non-Administrateur sans le rôle de l'intervenant → 403, statut inchangé. */
    @Test
    void r4_intervenant_nonAdministrateurSansLeRole_403() throws Exception {
        fixture.intervenantRole(T_EN_COURS_VALIDE, "ROLE-" + c.jeton());
        UUID id = fixture.prospection(c, code("INA"));
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);

        mockMvc.perform(post(TRANSITION, id).with(authentication(new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_Commercial")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(TRANSITION_ID, T_EN_COURS_VALIDE).json()))
                .andExpect(status().isForbidden());

        assertThat(guid(ligne(id).get("StrStatusId"))).isEqualTo(guid(STATUT_EN_COURS));
    }

    // ------------------------------------------------------------------ GET transitions, historique

    /** §4 : GET transitions = sortantes du statut courant (En cours : 10, 20, 100 ; pas 30 ni 101). */
    @Test
    void r4_transitions_sortantesDuStatutCourant() throws Exception {
        UUID id = fixture.prospection(c, code("LST"));

        api.transitions(id)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(T_EN_COURS_VALIDE.toString())))
                .andExpect(content().string(containsString(T_EN_COURS_NON_ABOUTIE.toString())))
                .andExpect(content().string(containsString(T_EN_COURS_REJETE.toString())))
                .andExpect(content().string(not(containsString(T_NON_ABOUTIE_EN_COURS.toString()))))
                .andExpect(content().string(not(containsString(T_VALIDE_EN_COURS.toString()))));
    }

    /** §1.2 : GET historique rend les lignes StrHistory de la prospection. */
    @Test
    void r1_2_historique_200_lignesDeLaProspection() throws Exception {
        UUID id = fixture.prospection(c, code("HST"));
        fixture.historique(id, "Création");

        api.historique(id)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Création")));
    }

    // ------------------------------------------------------------------ lecture seule, suppression

    /** §4 : statut courant IsReadOnly → PUT zones 409. */
    @Test
    void r4_statutLectureSeule_zones_409() throws Exception {
        UUID statut = fixture.statut(code("RO"), 290, TYPE_SUCCES, new Statut(true, true, false, 0));
        UUID id = fixture.prospection(c, code("ZRO"), statut);

        api.zones(id, List.of(corps().avec(IDENTIFICATION, "Z1").avec(SUPERFICIE, d("100"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_LECTURE_SEULE)));

        assertThat(fixture.compter("SELECT COUNT(*) FROM LndLandZone WHERE ProspectionId = :id", id)).isZero();
    }

    /** §4 : CanDelete = 0 → DELETE 409 « Vous ne pouvez pas supprimer cette entité dans le statut : [X] ! ». */
    @Test
    void r4_canDeleteFaux_suppression_409() throws Exception {
        UUID statut = fixture.statut(code("ND"), 295, TYPE_SUCCES, new Statut(false, false, false, 0));
        UUID id = fixture.prospection(c, code("SND"), statut);

        api.supprimer(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_SUPPRESSION)));

        assertThat(fixture.prospectionsDeCode(code("SND"))).isEqualTo(1);
    }
}
