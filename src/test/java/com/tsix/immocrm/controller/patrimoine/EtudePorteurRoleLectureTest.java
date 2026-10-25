package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureFaisabilite.RACINE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P8c (règle 6) : la fiche d'étude est réservée à Administrateur / Direction Générale (403 Commercial) ;
 * le porteur « etude » ne doit pas rouvrir sa lecture par {@code /api/audit} et {@code /api/notes}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudePorteurRoleLectureTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite faisabilite;
    @Autowired private FixtureProspectionsTerrain prospections;

    private UUID etude;
    private RequestPostProcessor commercial;

    @BeforeEach
    void setUp() throws Exception {
        faisabilite.parametrage();
        AdmUser admin = patrimoine.administrateur();
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        etude = new ApiFaisabilite(mockMvc, admin).etude(fiche("PRL-" + jeton, societe.getId()));
        prospections.note(etude);
        AdmUser compte = patrimoine.administrateur();
        AuthenticatedUser principal = new AuthenticatedUser(compte.getId(), "commercial.revue." + jeton, null, false, true, 0);
        commercial = authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    @Test
    void temoin_ficheEtude_commercial_403() throws Exception {
        mockMvc.perform(get(RACINE + "/" + etude).with(commercial)).andExpect(status().isForbidden());
    }

    @Test
    void audit_etude_commercial_refuse() throws Exception {
        mockMvc.perform(get("/api/audit").param("objetId", etude.toString()).with(commercial))
                .andExpect(status().is(anyOf(is(403), is(404))));
    }

    @Test
    void notes_etude_commercial_refuse() throws Exception {
        mockMvc.perform(get("/api/notes").param("porteur", "etude").param("id", etude.toString()).with(commercial))
                .andExpect(status().is(anyOf(is(403), is(404))));
    }
}
