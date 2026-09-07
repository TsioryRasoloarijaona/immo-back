package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers} — filtres projet/tranche et coût en requêtes de la liste.
 *
 * <p>Les trois cas couverts ici ont été trouvés en interrogeant la base réelle, pas en lisant le
 * code : le filtre projet ne renvoyait jamais rien, le filtre tranche manquait 30 dossiers sur
 * 358, et la liste exécutait une requête par association et par ligne.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeFiltresEtRequetesTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private StkItemRepository stkItemRepository;

    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;

    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-FILTRE-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-FILTRE-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-FILTRE-TEST");
        stkVocationRepository.save(vocation);
    }

    private CfgTier newTier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName("Nom-" + code);
        return tierRepository.save(tier);
    }

    private CfgTranche newTranche(String code, CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    private CfgProject newProjet(String code) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        return projectRepository.save(projet);
    }

    /** @param folderPourLienInverse {@code null} = article NON rattaché par {@code StkItem.ComFolderId}. */
    private StkItem newItem(String code, CfgTranche tranche, ComFolder folderPourLienInverse) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkHierarchy(tranche);
        if (folderPourLienInverse != null) {
            item.setComFolderId(folderPourLienInverse.getId());
        }
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        return stkItemRepository.save(item);
    }

    private ComFolder newFolder(String code) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    // -- Filtre projet : passait par StkItem.CfgProjectId, colonne morte (0/1068 en réel) --

    @Test
    void filtreProjet_resoutLeProjetParLaTrancheDeLArticlePrincipal() throws Exception {
        CfgProject projetVise = newProjet("PRJ-FILTRE-A");
        CfgProject autreProjet = newProjet("PRJ-FILTRE-B");

        ComFolder attendu = newFolder("DOS-PRJ-A");
        attendu.setStkItem(newItem("LOT-PRJ-A", newTranche("TR-PRJ-A", projetVise), attendu));
        folderRepository.save(attendu);

        ComFolder exclu = newFolder("DOS-PRJ-B");
        exclu.setStkItem(newItem("LOT-PRJ-B", newTranche("TR-PRJ-B", autreProjet), exclu));
        folderRepository.save(exclu);

        mockMvc.perform(get("/api/dossiers").param("projet", projetVise.getId().toString()))
                .andExpect(status().isOk())
                // Avant correctif : liste vide, StkItem.CfgProjectId n'étant jamais renseigné.
                .andExpect(jsonPath("$.content[?(@.id=='" + attendu.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + exclu.getId() + "')]").doesNotExist());
    }

    // -- Filtre tranche : l'EXISTS seul rate les dossiers sans lien inverse (30/358 en réel) --

    @Test
    void filtreTranche_trouveUnDossierRattacheParSonSeulArticlePrincipal() throws Exception {
        CfgTranche tranche = newTranche("TR-PRINCIPAL", newProjet("PRJ-PRINCIPAL"));

        ComFolder folder = newFolder("DOS-SANS-LIEN-INVERSE");
        // StkItem.ComFolderId volontairement NULL : seul ComFolder.StkItemId rattache l'article.
        folder.setStkItem(newItem("LOT-SANS-LIEN-INVERSE", tranche, null));
        folderRepository.save(folder);

        mockMvc.perform(get("/api/dossiers").param("trancheId", tranche.getId().toString()))
                .andExpect(status().isOk())
                // Avant correctif : absent, l'EXISTS ne regardant que StkItem.ComFolderId.
                .andExpect(jsonPath("$.content[?(@.id=='" + folder.getId() + "')]").exists());
    }

    @Test
    void filtreTranche_trouveEncoreUnDossierRattacheParLeLienInverse() throws Exception {
        CfgTranche tranche = newTranche("TR-LIEN-INVERSE", newProjet("PRJ-LIEN-INVERSE"));

        // Article annexe rattaché par StkItem.ComFolderId sans être l'article principal :
        // c'est le cas que l'EXISTS couvrait déjà, et qui ne doit pas régresser.
        ComFolder folder = newFolder("DOS-AVEC-LIEN-INVERSE");
        newItem("LOT-ANNEXE", tranche, folder);

        mockMvc.perform(get("/api/dossiers").param("trancheId", tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + folder.getId() + "')]").exists());
    }

    // -- N+1 : le coût de la liste ne doit pas dépendre du nombre de lignes rendues --

    @Test
    void listerPlusDeLignesNAugmentePasLeNombreDeRequetes() throws Exception {
        // Chaque dossier porte un client ET un commercial distincts : sans graphe de chargement,
        // ce sont deux SELECT de plus par ligne, déclenchés par DossierService.toListItem.
        String marqueur = "dosn1" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 5; i++) {
            ComFolder folder = newFolder(marqueur + "-" + i);
            folder.setCfgCustomer(newTier("CLI-" + marqueur + "-" + i));
            folder.setCfgCommercial(newTier("COM-" + marqueur + "-" + i));
            folderRepository.save(folder);
        }

        long pourUneLigne = compterRequetes(marqueur, 1);
        long pourCinqLignes = compterRequetes(marqueur, 5);

        // Avant correctif : 5 lignes coûtaient ~8 requêtes de plus qu'une seule.
        assertThat(pourCinqLignes)
                .as("rendre 5 lignes au lieu d'1 ne doit déclencher aucune requête supplémentaire "
                        + "(%d requêtes pour 1 ligne, %d pour 5)", pourUneLigne, pourCinqLignes)
                .isEqualTo(pourUneLigne);
    }

    /** Requêtes JDBC réellement émises par un appel de la liste, fixtures exclues. */
    private long compterRequetes(String marqueur, int taillePage) throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        // Les INSERT de fixture ne doivent pas être comptés, et surtout le cache de premier
        // niveau doit être vidé : sans ce clear(), les associations LAZY seraient déjà en mémoire
        // et le N+1 resterait invisible — le test passerait sans rien vérifier.
        entityManager.flush();
        entityManager.clear();
        stats.clear();

        mockMvc.perform(get("/api/dossiers")
                        .param("search", marqueur)
                        .param("size", String.valueOf(taillePage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        return stats.getPrepareStatementCount();
    }
}
