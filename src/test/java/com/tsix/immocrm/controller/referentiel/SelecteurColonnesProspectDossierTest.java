package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
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
 * Sélecteur de colonnes des listes prospect et dossier — voir
 * {@code instructions/FEATURE-selecteur-colonnes-prospect-dossier.md}.
 *
 * <p><b>Le test central est {@link #chaqueListeAppliqueSonPropreDefaut()}.</b> La plomberie
 * d'origine ne portait qu'une sélection, résolue par le filtre servlet contre le catalogue de la
 * prospection, et déposée dans un {@code ThreadLocal} partagé. Ajouter un {@code @JsonFilter} sur
 * une seconde liste sans y toucher aurait fait revenir celle-ci <b>presque vide</b> : aucun de ses
 * noms ({@code prenom}, {@code ville}…) ne figure au catalogue de la prospection, donc aucune de
 * ses propriétés n'aurait passé le filtre. Le {@code ThreadLocal} ne porte désormais que la
 * demande brute, et chaque filtre — un par catalogue — applique son propre défaut.
 *
 * <p>Le second point tenu ici est le <b>400 qui désigne le bon écran</b> : une faute de frappe sur
 * une colonne de prospect doit répondre les colonnes de prospect, pas celles de prospection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SelecteurColonnesProspectDossierTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private CfgTierTypeRepository tierTypeRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private StkItemRepository stkItemRepository;
    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;
    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired
    private StkVocationRepository stkVocationRepository;
    @Autowired
    private CfgTrancheRepository trancheRepository;
    @Autowired
    private CfgProjectRepository projectRepository;
    @Autowired
    private CfgCompanyRepository companyRepository;

    /** Prospect visible de {@code GET /api/prospects} : le type {@code PRO} est obligatoire. */
    private CfgTier prospect() {
        CfgTierType type = new CfgTierType();
        type.setId(UUID.randomUUID());
        type.setCode("PRO");
        type.setDescription("PROSPECT");
        tierTypeRepository.saveAndFlush(type);

        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("SEL-TIER-" + UUID.randomUUID());
        t.setFirstName("Selecteur");
        t.setLastName("TEST");
        t.setCity("Casablanca");
        t.setCfgTierType(type);
        t.setIDNumber("AB123456");
        t.setUserCreate("createur-test");
        t.setUserCreateDateTime(LocalDateTime.now());
        return tierRepository.saveAndFlush(t);
    }

    /**
     * Dossier visible : la société du projet doit être en exploitation, sinon le périmètre
     * commercial le masque et la liste répond vide.
     */
    private ComFolder dossier() {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-SEL");
        societe.setDescription("Societe selecteur");
        societe.setIsActive(true);
        companyRepository.save(societe);

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-SEL");
        projet.setDescription("Projet selecteur");
        projet.setCfgCompany(societe);
        projectRepository.save(projet);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TRA-SEL");
        tranche.setDescription("Tranche selecteur");
        tranche.setCfgProject(projet);
        trancheRepository.save(tranche);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SEL");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-SEL");
        categorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(categorie);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SEL");
        vocation.setDescription("Vocation selecteur");
        stkVocationRepository.save(vocation);

        ComFolder f = new ComFolder();
        f.setId(UUID.randomUUID());
        f.setCode("DOS-SEL-" + UUID.randomUUID().toString().substring(0, 8));
        f.setDocDate(LocalDateTime.now());
        f.setStkVocation(vocation);
        f.setUserCreate("createur-test");
        f.setUserCreateDateTime(LocalDateTime.now());
        folderRepository.saveAndFlush(f);

        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode("LOT-SEL-" + UUID.randomUUID().toString().substring(0, 6));
        item.setStkItemType(itemType);
        item.setStkItemCategory(categorie);
        item.setStkVocation(vocation);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        item.setComFolderId(f.getId());
        item.setStkHierarchy(tranche);
        stkItemRepository.save(item);

        f.setStkItem(item);
        return folderRepository.saveAndFlush(f);
    }

    // ------------------------------------------------------------------
    // Le point structurel : chaque liste a son propre défaut
    // ------------------------------------------------------------------

    @Test
    void chaqueListeAppliqueSonPropreDefaut() throws Exception {
        CfgTier p = prospect();
        ComFolder d = dossier();

        // Prospect : ses 10 colonnes historiques, et aucune des nouvelles.
        mockMvc.perform(get("/api/prospects").param("search", p.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(p.getCode()))
                .andExpect(jsonPath("$.content[0].prenom").value("Selecteur"))
                .andExpect(jsonPath("$.content[0].ville").value("Casablanca"))
                .andExpect(jsonPath("$.content[0].cin").doesNotExist())
                .andExpect(jsonPath("$.content[0].creePar").doesNotExist());

        // Dossier : ses 13 colonnes historiques, et aucune des nouvelles.
        mockMvc.perform(get("/api/dossiers").param("search", d.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(d.getCode()))
                .andExpect(jsonPath("$.content[0].dateDocument").exists())
                .andExpect(jsonPath("$.content[0].projetCode").doesNotExist())
                .andExpect(jsonPath("$.content[0].creePar").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Prospect
    // ------------------------------------------------------------------

    @Test
    void prospect_selectionExplicite() throws Exception {
        CfgTier p = prospect();

        mockMvc.perform(get("/api/prospects")
                        .param("colonnes", "code,cin,creePar")
                        .param("search", p.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(p.getCode()))
                .andExpect(jsonPath("$.content[0].cin").value("AB123456"))
                .andExpect(jsonPath("$.content[0].creePar").value("createur-test"))
                // id est toujours servi, meme non demande : sans lui la ligne n'est plus cliquable.
                .andExpect(jsonPath("$.content[0].id").exists())
                // Non demandees : absentes, pas nulles.
                .andExpect(jsonPath("$.content[0].prenom").doesNotExist())
                .andExpect(jsonPath("$.content[0].ville").doesNotExist());
    }

    @Test
    void prospect_colonneInconnue_rend400AvecSesPropresNoms() throws Exception {
        mockMvc.perform(get("/api/prospects").param("colonnes", "colonneQuiNexistePas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.colonne").value("colonneQuiNexistePas"))
                // Les noms proposes doivent etre ceux du prospect...
                .andExpect(jsonPath("$.colonnesValides[?(@ == 'prenom')]").isNotEmpty())
                // ...et surtout pas ceux de la prospection, qui designeraient le mauvais ecran.
                .andExpect(jsonPath("$.colonnesValides[?(@ == 'joursInactivite')]").isEmpty());
    }

    @Test
    void catalogueProspect_publieLesColonnes() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/prospects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == 'code')].parDefaut").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'cin')].parDefaut").value(false))
                .andExpect(jsonPath("$[?(@.nom == 'creePar')]").isNotEmpty())
                // Les tranches d'interet exigent une requete par prospect : hors catalogue.
                .andExpect(jsonPath("$[?(@.nom == 'tranchesInteret')]").isEmpty());
    }

    // ------------------------------------------------------------------
    // Dossier
    // ------------------------------------------------------------------

    @Test
    void dossier_selectionExplicite() throws Exception {
        ComFolder d = dossier();

        mockMvc.perform(get("/api/dossiers")
                        .param("colonnes", "code,projetCode,trancheCode,creePar")
                        .param("search", d.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(d.getCode()))
                .andExpect(jsonPath("$.content[0].projetCode").value("PRJ-SEL"))
                .andExpect(jsonPath("$.content[0].trancheCode").value("TRA-SEL"))
                .andExpect(jsonPath("$.content[0].creePar").value("createur-test"))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].statut").doesNotExist())
                .andExpect(jsonPath("$.content[0].prix").doesNotExist());
    }

    /**
     * Le contexte patrimonial n'est traversé que s'il est demandé. Demander une colonne d'un autre
     * groupe ne doit donc pas le remplir — c'est ce qui garantit que la liste par défaut ne paie
     * pas trois jointures par ligne.
     */
    @Test
    void dossier_patrimoineNonDemande_nEstPasResolu() throws Exception {
        ComFolder d = dossier();

        mockMvc.perform(get("/api/dossiers")
                        .param("colonnes", "code,creePar")
                        .param("search", d.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].creePar").value("createur-test"))
                .andExpect(jsonPath("$.content[0].projetCode").doesNotExist())
                .andExpect(jsonPath("$.content[0].trancheCode").doesNotExist());
    }

    @Test
    void dossier_colonneInconnue_rend400AvecSesPropresNoms() throws Exception {
        mockMvc.perform(get("/api/dossiers").param("colonnes", "pasUneColonne"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.colonne").value("pasUneColonne"))
                .andExpect(jsonPath("$.colonnesValides[?(@ == 'projetCode')]").isNotEmpty())
                .andExpect(jsonPath("$.colonnesValides[?(@ == 'prenom')]").isEmpty());
    }

    @Test
    void catalogueDossier_publieLesColonnes() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == 'statut')].parDefaut").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'projetCode')].parDefaut").value(false))
                .andExpect(jsonPath("$[?(@.nom == 'projetCode')].groupe").value("PATRIMOINE"))
                // Ecartees deliberement : articles (N+1), notes (0 ligne sur 358).
                .andExpect(jsonPath("$[?(@.nom == 'articles')]").isEmpty())
                .andExpect(jsonPath("$[?(@.nom == 'notes')]").isEmpty());
    }

    // ------------------------------------------------------------------
    // Non-regression sur la prospection, dont la plomberie a ete refondue
    // ------------------------------------------------------------------

    @Test
    void prospection_conserveSonPropreDefaut() throws Exception {
        // Fixture propre plutot que de compter sur les donnees deja presentes : la base de test
        // est partagee entre classes, et un test qui suppose une ligne existante passe ou echoue
        // selon l'ordre d'execution.
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("SEL-PSN-TIER-" + UUID.randomUUID());
        prospect.setLastName("PROSPECTION");
        tierRepository.save(prospect);

        String code = "PSN-SEL-" + UUID.randomUUID();
        ComProspection psn = new ComProspection();
        psn.setId(UUID.randomUUID());
        psn.setCode(code);
        psn.setDocDate(LocalDateTime.now());
        psn.setCfgProspect(prospect);
        prospectionRepository.saveAndFlush(psn);

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                // Colonnes historiques de la prospection : toujours servies.
                .andExpect(jsonPath("$.content[0].code").value(code))
                // Groupe activite : toujours hors defaut.
                .andExpect(jsonPath("$.content[0].joursInactivite").doesNotExist());
    }
}
