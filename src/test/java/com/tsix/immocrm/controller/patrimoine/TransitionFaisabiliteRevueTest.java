package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.MSG_TRANCHES;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.ligneTrancheCorps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.lignes;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.tranche;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.ROLE_DG;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.TRANSITIONS;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.TRANSITION_ID;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue P8b-2 : Verif de l'étude (P8a) à la transition (§4.1 contrôle 4) et décision 3 (rôle non lecteur sans effet). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionFaisabiliteRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteWorkflow wf;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private String jeton;
    private CfgCompany societe;
    private UUID etude;
    private UUID enEtude;
    private UUID validee;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        wf.typesEtat();
        admin = patrimoine.administrateur();
        jeton = jeton();
        societe = patrimoine.entite("ENR-" + jeton, true);
        etude = new ApiFaisabilite(mockMvc, admin).etude(fiche("TRV-" + jeton, societe.getId()));
        wf.roleAdministrateur();
        wf.roleDirectionGenerale();
        enEtude = wf.statut("ETR-" + jeton, 911);
        validee = wf.statut("VAR-" + jeton, 912);
        wf.placer(etude, enEtude);
    }

    private ResultActions executer(UUID transitionId, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post(TRANSITIONS, etude).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec(TRANSITION_ID, transitionId).json()));
    }

    /**
     * §4.1 contrôle 4 : « statut mis à jour, puis Verif de l'étude (P8a) » — OMRAN statusBtn_Click → Commit → Verif
     * (StkFeasibilityStudy.cs l.2408-2414 : poids des tranches ≠ 1 refusé). Une étude dont la ventilation des tranches
     * est fausse ne doit pas changer de statut.
     */
    @Test
    void transition_verifDeLEtude_poidsDesTranchesFaux_400_statutInchange() throws Exception {
        UUID t = wf.transition(enEtude, validee, "En cours " + jeton, "", wf.roleAdministrateur());
        UUID cfgTranche = patrimoine.tranche("TRV-" + jeton, patrimoine.projet("PRV-" + jeton, societe)).getId();
        new ApiFaisabilite(mockMvc, admin).tranches(etude, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, cfgTranche,
                BigDecimal.ONE)))).andExpect(status().isOk());
        em.flush();
        em.createNativeQuery("UPDATE StkFeasibilityStudyCfgTranche SET Weight = 0.5 WHERE FeasibilityStudyId = :id")
                .setParameter("id", etude).executeUpdate();
        em.clear();

        executer(t, connecte(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MSG_TRANCHES)));

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude));
    }

    /**
     * Décision 3 (22/10) : « transitions limitées aux lecteurs ; un rôle non lecteur listé sur une transition est sans
     * effet ». Un compte Direction Générale qui porte aussi un rôle non lecteur ne doit pas emprunter une transition
     * réservée à ce seul rôle non lecteur.
     */
    @Test
    void transition_reserveeAUnRoleNonLecteur_lecteurQuiLePorteAussi_409() throws Exception {
        String code = "CTR-" + jeton;
        UUID t = wf.transition(enEtude, validee, "Reservee CT " + jeton, "", wf.role(code));
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        RequestPostProcessor dgEtCt = authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_DG), new SimpleGrantedAuthority("ROLE_" + code))));

        executer(t, dgEtCt).andExpect(status().isConflict());

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude));
    }
}
