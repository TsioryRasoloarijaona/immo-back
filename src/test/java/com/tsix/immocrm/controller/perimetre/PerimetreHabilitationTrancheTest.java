package com.tsix.immocrm.controller.perimetre;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FEATURE-patrimoine-ecriture.md §2.7 / §11 : la ligne AdmUserHabilitation « tranche seule » qu'insère la création
 * d'une tranche (comme OMRAN) ne change pas le périmètre projet lu par PerimetreCommercialService.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerimetreHabilitationTrancheTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private AppUserCredentialRepository credentialRepository;

    private CfgProject projetA;
    private CfgTranche trancheB;

    @BeforeEach
    void setUp() {
        CfgCompany entite = fixture.entite("ENT-PERIM-TR", true);
        projetA = fixture.projet("PRJ-PERIM-TR-A", entite);
        trancheB = fixture.tranche("TR-PERIM-TR-B", fixture.projet("PRJ-PERIM-TR-B", entite));
    }

    private AdmUser commercialAvecCompte() {
        AdmUser user = fixture.administrateur();
        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setTokenVersion(0);
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);
        return user;
    }

    private void habiliter(AdmUser user, CfgProject projet, CfgTranche tranche) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(user);
        habilitation.setCfgProject(projet);
        habilitation.setCfgTranche(tranche);
        habilitationRepository.save(habilitation);
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    @Test
    void r2_7_ligneTrancheSeule_nAjouteAucunProjetAuPerimetre() throws Exception {
        AdmUser user = commercialAvecCompte();
        habiliter(user, projetA, null);
        habiliter(user, null, trancheB);

        mockMvc.perform(get("/api/mon-compte").with(commercial(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perimetreRestreint").value(true))
                .andExpect(jsonPath("$.projetsAutorises.length()").value(1))
                .andExpect(jsonPath("$.projetsAutorises[0].code").value("PRJ-PERIM-TR-A"));
    }

    @Test
    void r2_7_ligneTrancheSeule_neRestreintPasUnCompteSansProjet() throws Exception {
        AdmUser user = commercialAvecCompte();
        habiliter(user, null, trancheB);

        mockMvc.perform(get("/api/mon-compte").with(commercial(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perimetreRestreint").value(false))
                .andExpect(jsonPath("$.projetsAutorises.length()").value(0));
    }
}
