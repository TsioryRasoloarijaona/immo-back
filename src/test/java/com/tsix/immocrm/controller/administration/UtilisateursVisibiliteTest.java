package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.controller.administration.ArbreHabilitations.Noeud;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.acceptee;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R15 : un commercial avant et après un PUT d'habilitations, dossiers et prospections. {@code @Transactional} : le jeu de dossiers
 * est annulé, et le test relit dans la transaction du PUT, comme le ferait la requête suivante.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UtilisateursVisibiliteTest {

    private static final String DOSSIERS = "/api/dossiers";
    private static final String PROSPECTIONS = "/api/prospections";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private AdmUserHabilitationRepository habilitations;
    @Autowired private ComProspectionRepository prospections;
    @Autowired private EntityManager em;

    private AppelsUtilisateurs appels;
    private AdmUser commercial;
    private RequestPostProcessor connexion;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private ComFolder dossierA;
    private ComFolder dossierB;
    private ComProspection prospectionA;
    private ComProspection prospectionB;

    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 8);
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte("admin-" + jeton)));
        commercial = fixture.compte("commercial-" + jeton, "Commercial");
        connexion = FixtureDossierVente.connecte(commercial, "Commercial");
        trancheA = fixture.tranche("TRA-" + jeton, true);
        trancheB = fixture.tranche("TRB-" + jeton, true);
        dossierA = fixture.dossier("DOSA-" + jeton, trancheA, null);
        dossierB = fixture.dossier("DOSB-" + jeton, trancheB, null);
        prospectionA = prospection("PSNA-" + jeton, trancheA);
        prospectionB = prospection("PSNB-" + jeton, trancheB);
        habiliter(trancheA);
        em.flush();
    }

    private ComProspection prospection(String code, CfgTranche tranche) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code);
        prospection.setCfgProspect(fixture.tier("CLI-" + code));
        prospection.setStkHierarchy(tranche);
        return prospections.save(prospection);
    }

    private void habiliter(CfgTranche tranche) {
        AdmUserHabilitation ligne = new AdmUserHabilitation();
        ligne.setId(UUID.randomUUID());
        ligne.setAdmUser(commercial);
        ligne.setCfgProject(tranche.getCfgProject());
        habilitations.save(ligne);
    }

    private void voit(String route, UUID id) throws Exception {
        mockMvc.perform(get(route).with(connexion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]").exists());
    }

    private void neVoitPas(String route, UUID id) throws Exception {
        mockMvc.perform(get(route).with(connexion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]").doesNotExist());
    }

    private void perimetreA() throws Exception {
        voit(DOSSIERS, dossierA.getId());
        neVoitPas(DOSSIERS, dossierB.getId());
        voit(PROSPECTIONS, prospectionA.getId());
        neVoitPas(PROSPECTIONS, prospectionB.getId());
    }

    private void remplacerPar(CfgTranche tranche) throws Exception {
        Noeud projet = appels.arbre(commercial.getId()).noeud(tranche.getCfgProject().getId());
        acceptee(appels.habilitations(commercial.getId(), projet));
    }

    @Test
    void r15_apresPut_dossiersDuNouveauProjetSeulement() throws Exception {
        perimetreA();

        remplacerPar(trancheB);

        voit(DOSSIERS, dossierB.getId());
        neVoitPas(DOSSIERS, dossierA.getId());
    }

    @Test
    void r15_apresPut_prospectionsDuNouveauProjetSeulement() throws Exception {
        perimetreA();

        remplacerPar(trancheB);

        voit(PROSPECTIONS, prospectionB.getId());
        neVoitPas(PROSPECTIONS, prospectionA.getId());
    }

    @Test
    void r15_ajoutDUnProjet_lesDeuxVisibles() throws Exception {
        perimetreA();
        Noeud projetA = appels.arbre(commercial.getId()).noeud(trancheA.getCfgProject().getId());
        Noeud projetB = appels.arbre(commercial.getId()).noeud(trancheB.getCfgProject().getId());

        acceptee(appels.habilitations(commercial.getId(), projetA, projetB));

        voit(DOSSIERS, dossierA.getId());
        voit(DOSSIERS, dossierB.getId());
    }

    /** Dernière habilitation projet retirée : le compte repasse sans restriction, l'écran doit le signaler. */
    @Test
    void r15_derniereHabilitationProjetRetiree_sansRestriction() throws Exception {
        perimetreA();

        acceptee(appels.habilitations(commercial.getId()));

        voit(DOSSIERS, dossierB.getId());
        voit(PROSPECTIONS, prospectionB.getId());
    }

    /** Un nœud tranche seul ne fait pas un périmètre : seule la portée projet restreint. */
    @Test
    void r15_noeudTrancheSeul_neRestreintPas() throws Exception {
        perimetreA();
        Noeud tranche = appels.arbre(commercial.getId()).noeud(trancheA.getId());

        acceptee(appels.habilitations(commercial.getId(), tranche));

        voit(DOSSIERS, dossierB.getId());
    }
}
