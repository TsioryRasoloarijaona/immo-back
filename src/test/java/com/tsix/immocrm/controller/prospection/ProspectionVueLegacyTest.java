package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Enrichissement de la fiche prospection et du journal d'interactions par la base légataire —
 * voir {@code instructions/FEATURE-vues-legacy-prospection.md}.
 *
 * <p><b>Deux pièges propres à ce lot, verrouillés ici.</b>
 *
 * <p><b>1. {@code saveAndFlush} est obligatoire.</b> {@code ComSaleProspectionView} est une vue :
 * Hibernate ne connaît pas les tables qu'elle lit, donc son auto-flush ne se déclenche pas avant
 * de l'interroger. Sans flush explicite, la vue ne verrait pas la fixture encore en attente dans
 * le contexte de persistance et {@code activite} vaudrait {@code null} — le test passerait en
 * vérifiant le mauvais comportement.
 *
 * <p><b>2. {@code StrEntityId} décide de tout.</b> La vue fait un {@code INNER JOIN} sur
 * {@code StrEntity}. Une prospection sans entité structurelle n'apparaît dans <em>aucune</em>
 * vue, donc dans aucun écran du logiciel de bureau. C'est le cas de 4 lignes en base réelle, dont
 * une créée par notre propre API — d'où le test dédié ci-dessous, qui fige le comportement
 * observable tant que {@code creerProspection} ne renseigne pas ce champ.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionVueLegacyTest {

    /** {@code StrEntity.PKey} de « Prospection vente » — semé par {@code ci/schema/050_views.sql}. */
    private static final UUID ENTITE_PROSPECTION =
            UUID.fromString("e7c73f70-1111-40d9-aa78-a2825c42b601");

    /** {@code StrEntity.PKey} de « Pré-Prospection », que la vue exclut par construction. */
    private static final UUID ENTITE_PRE_PROSPECTION =
            UUID.fromString("b92df3d8-158c-4e1e-827a-70feb172a264");

    /** {@code ComActionMessageCategory.PKey} de « Prospection » (46 666 lignes réelles). */
    private static final UUID CATEGORIE_PROSPECTION =
            UUID.fromString("69f8e7d6-1111-1111-1111-1aa8b1f02008");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    @Autowired
    private ComActionMessageCategoryRepository actionMessageCategoryRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    private ComProspection prospection(UUID entite) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-VUE-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDocDate(LocalDateTime.now());
        p.setStrEntityId(entite);
        // Flush explicite : sans lui la vue ne verrait pas cette ligne (voir javadoc de classe).
        return prospectionRepository.saveAndFlush(p);
    }

    @Test
    void ficheProspection_exposeLeBlocActivite() throws Exception {
        ComProspection p = prospection(ENTITE_PROSPECTION);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activite").exists())
                // Calculé par la vue et jamais null : 100 % des lignes réelles le portent.
                .andExpect(jsonPath("$.activite.joursInactivite").exists());
    }

    @Test
    void ficheProspection_sansInteraction_neCompteAucuneInteraction() throws Exception {
        ComProspection p = prospection(ENTITE_PROSPECTION);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activite.nbInteractions").value(0))
                // Jamais transformée : le lien de conversion doit rester absent, pas valoir 0.
                .andExpect(jsonPath("$.activite.dossierId").doesNotExist())
                .andExpect(jsonPath("$.activite.dossierCode").doesNotExist());
    }

    /**
     * Une chaîne vide rendue par la vue doit ressortir en {@code null}, jamais en {@code ""} :
     * trois colonnes sont non-{@code NULL} sur 100 % des lignes mais vides sur 10 % à 75 %.
     */
    @Test
    void ficheProspection_normaliseLesChainesVidesEnNull() throws Exception {
        ComProspection p = prospection(ENTITE_PROSPECTION);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activite.derniereCommunicationResultat").doesNotExist())
                .andExpect(jsonPath("$.activite.derniereCommunicationMessage").doesNotExist())
                .andExpect(jsonPath("$.activite.alerteActiveRaison").doesNotExist());
    }

    /**
     * Une pré-prospection reste servie normalement — 200, pas 404 — mais sans bloc activité : la
     * vue qui le produit exclut les 3 817 pré-prospections, qui ont la leur.
     */
    @Test
    void preProspection_estServieSansBlocActivite() throws Exception {
        ComProspection p = prospection(ENTITE_PRE_PROSPECTION);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId().toString()))
                .andExpect(jsonPath("$.activite").doesNotExist());
    }

    /**
     * Fige le défaut connu : sans {@code StrEntityId}, la prospection est invisible de toutes les
     * vues du légataire — donc de tous les écrans du logiciel de bureau — et l'API ne peut rendre
     * aucun indicateur. C'est l'état des prospections que crée aujourd'hui
     * {@code POST /api/prospections}. Ce test devra être inversé le jour où
     * {@code creerProspection} renseignera le champ.
     */
    @Test
    void prospectionSansEntiteStructurelle_nAAucunBlocActivite() throws Exception {
        ComProspection p = prospection(null);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activite").doesNotExist());
    }

    @Test
    void interaction_porteSaCategorieEtSonContexteProjetTranche() throws Exception {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-" + UUID.randomUUID().toString().substring(0, 6));
        projet.setDescription("Projet de test");
        projectRepository.save(projet);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-" + UUID.randomUUID().toString().substring(0, 6));
        tranche.setDescription("Tranche de test");
        tranche.setCfgProject(projet);
        trancheRepository.save(tranche);

        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-VUE-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDocDate(LocalDateTime.now());
        p.setStrEntityId(ENTITE_PROSPECTION);
        p.setStkHierarchy(tranche);
        prospectionRepository.saveAndFlush(p);

        ComActionMessageCategory categorie = actionMessageCategoryRepository
                .findById(CATEGORIE_PROSPECTION)
                .orElseGet(() -> {
                    ComActionMessageCategory c = new ComActionMessageCategory();
                    c.setId(CATEGORIE_PROSPECTION);
                    c.setCode("Prospection");
                    c.setDescription("Prospection");
                    return actionMessageCategoryRepository.save(c);
                });

        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(LocalDateTime.now());
        m.setAlertMessage("le prospect rappelle demain");
        m.setComProspection(p);
        m.setComActionMessageCategory(categorie);
        actionMessageRepository.saveAndFlush(m);

        mockMvc.perform(get("/api/prospections/{id}/interactions", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categorie").value("Prospection"))
                // Catégorie « Prospection » : la tranche vient de la prospection, pas du lot.
                .andExpect(jsonPath("$.content[0].trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.content[0].trancheCode").value(tranche.getCode()))
                .andExpect(jsonPath("$.content[0].projetId").value(projet.getId().toString()))
                .andExpect(jsonPath("$.content[0].projetCode").value(projet.getCode()));
    }

    /**
     * Hors catégorie « Prospection », la règle du légataire bascule sur la tranche du <em>lot</em>.
     * Sans lot, le contexte est simplement absent — et surtout, il n'emprunte pas celui de la
     * prospection.
     */
    @Test
    void interactionHorsProspection_nHeritePasDeLaTrancheDeLaProspection() throws Exception {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-" + UUID.randomUUID().toString().substring(0, 6));
        projectRepository.save(projet);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-" + UUID.randomUUID().toString().substring(0, 6));
        tranche.setCfgProject(projet);
        trancheRepository.save(tranche);

        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-VUE-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDocDate(LocalDateTime.now());
        p.setStrEntityId(ENTITE_PROSPECTION);
        p.setStkHierarchy(tranche);
        prospectionRepository.saveAndFlush(p);

        ComActionMessageCategory reclamation = new ComActionMessageCategory();
        reclamation.setId(UUID.randomUUID());
        reclamation.setCode("Reclamation-TEST");
        reclamation.setDescription("Réclamation");
        actionMessageCategoryRepository.save(reclamation);

        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(LocalDateTime.now());
        m.setAlertMessage("fuite constatée");
        m.setComProspection(p);
        m.setComActionMessageCategory(reclamation);
        actionMessageRepository.saveAndFlush(m);

        mockMvc.perform(get("/api/prospections/{id}/interactions", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categorie").value("Réclamation"))
                .andExpect(jsonPath("$.content[0].trancheId").doesNotExist())
                .andExpect(jsonPath("$.content[0].projetId").doesNotExist());
    }
}
