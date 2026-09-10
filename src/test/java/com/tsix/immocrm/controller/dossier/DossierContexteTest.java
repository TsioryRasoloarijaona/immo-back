package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.BeforeEach;
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
 * Contexte patrimonial, piste d'audit et origine du dossier — voir
 * {@code instructions/FEATURE-contexte-dossier.md}.
 *
 * <p><b>Le test qui compte est {@link #projet_vientDeLaTrancheEtNonDeLArticle()}.</b> Sur les 358
 * articles de dossier réels, {@code StkItem.CfgProjectId} est {@code null} <em>partout</em> : le
 * projet passe obligatoirement par la tranche. Un mapping direct sur l'article compilerait,
 * passerait un test naïf où la fixture renseigne les deux, et rendrait {@code null} en production
 * sans lever la moindre erreur. La fixture ci-dessous reproduit donc la base réelle — elle ne pose
 * <b>jamais</b> {@code CfgProjectId} sur l'article.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierContexteTest {

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
    private StkFloorRepository stkFloorRepository;
    @Autowired
    private StkBuildingRepository stkBuildingRepository;
    @Autowired
    private CfgTrancheRepository trancheRepository;
    @Autowired
    private CfgProjectRepository projectRepository;
    @Autowired
    private CfgCompanyRepository companyRepository;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComProspectionKindRepository prospectionKindRepository;
    @Autowired
    private ComProspectionOriginRepository prospectionOriginRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-CTX");
        itemType.setDescription("Type contexte");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-CTX");
        itemCategory.setDescription("Categorie contexte");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-CTX");
        vocation.setDescription("Vocation contexte");
        stkVocationRepository.save(vocation);

        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-CTX");
        societe.setDescription("Societe contexte");
        // OBLIGATOIRE, et verifie par isolation : sans ce drapeau, 8 des 9 tests de cette classe
        // echouent, la fiche repondant 404. Le perimetre commercial ne rend visibles que les
        // dossiers dont la societe du projet est en exploitation (VisibiliteEntites.depuisProjet,
        // predicat "actif OU non resolu") — et il s'applique a la FICHE autant qu'a la liste, ce
        // que documente l'@Operation de DossierController#findById : hors perimetre => 404, jamais
        // 403, pour ne pas confirmer l'existence du dossier.
        //
        // Consequence pour toute fixture de dossier rattache a un projet : creer la societe sans
        // IsActive rend le dossier introuvable, et l'echec ressemble a un bug de mapping.
        societe.setIsActive(true);
        companyRepository.save(societe);

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-CTX");
        projet.setDescription("Projet contexte");
        projet.setCfgCompany(societe);
        projectRepository.save(projet);

        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TRA-CTX");
        tranche.setDescription("Tranche contexte");
        tranche.setCfgProject(projet);
        trancheRepository.save(tranche);
    }

    private ComFolder dossier() {
        ComFolder f = new ComFolder();
        f.setId(UUID.randomUUID());
        f.setCode("DOS-CTX-" + UUID.randomUUID().toString().substring(0, 8));
        f.setDocDate(LocalDateTime.now());
        f.setStkVocation(vocation);
        f.setUserCreate("createur-test");
        f.setUserCreateDateTime(LocalDateTime.now());
        f.setUserUpdate("modificateur-test");
        f.setUserUpdateDateTime(LocalDateTime.now());
        return f;
    }

    /**
     * Article du dossier, rattaché à la tranche.
     *
     * <p><b>{@code setCfgProject} n'est volontairement jamais appelé</b> : c'est l'état de la base
     * réelle, où les 358 articles de dossier ont un {@code CfgProjectId} nul.
     */
    private StkItem article(String code, ComFolder folder, StkFloor etage, StkBuilding immeuble) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        item.setComFolderId(folder.getId());
        item.setStkHierarchy(tranche);
        item.setStkFloor(etage);
        item.setStkBuilding(immeuble);
        return stkItemRepository.save(item);
    }

    private StkFloor etage() {
        StkFloor e = new StkFloor();
        e.setId(UUID.randomUUID());
        e.setCode("RDC-CTX");
        e.setDescription("Rez de chaussee");
        return stkFloorRepository.save(e);
    }

    private StkBuilding immeuble() {
        StkBuilding b = new StkBuilding();
        b.setId(UUID.randomUUID());
        b.setCode("IMM-CTX");
        b.setDescription("Immeuble contexte");
        return stkBuildingRepository.save(b);
    }

    /** Dossier complet : article principal rattaché, étage et immeuble posés. */
    private ComFolder dossierComplet() {
        ComFolder f = folderRepository.saveAndFlush(dossier());
        StkItem item = article("LOT-CTX-" + UUID.randomUUID().toString().substring(0, 6), f,
                etage(), immeuble());
        f.setStkItem(item);
        return folderRepository.saveAndFlush(f);
    }

    // ------------------------------------------------------------------
    // Contexte patrimonial
    // ------------------------------------------------------------------

    @Test
    void fiche_exposeLeContextePatrimonial() throws Exception {
        ComFolder f = dossierComplet();

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheCode").value("TRA-CTX"))
                .andExpect(jsonPath("$.trancheLibelle").value("Tranche contexte"))
                .andExpect(jsonPath("$.projetCode").value("PRJ-CTX"))
                .andExpect(jsonPath("$.projetLibelle").value("Projet contexte"))
                .andExpect(jsonPath("$.societeCode").value("SOC-CTX"))
                .andExpect(jsonPath("$.societeLibelle").value("Societe contexte"))
                .andExpect(jsonPath("$.vocationCode").value("VOC-CTX"))
                .andExpect(jsonPath("$.vocationLibelle").value("Vocation contexte"));
    }

    /**
     * Le cœur du lot. La fixture ne pose jamais {@code CfgProjectId} sur l'article — comme la base
     * réelle — donc ce test échoue si le projet est lu depuis l'article au lieu de la tranche.
     */
    @Test
    void projet_vientDeLaTrancheEtNonDeLArticle() throws Exception {
        ComFolder f = dossierComplet();

        // Précondition explicite : l'article n'a pas de projet, seule la tranche en a un.
        StkItem item = stkItemRepository.findByComFolderId(f.getId()).getFirst();
        org.junit.jupiter.api.Assertions.assertNull(item.getCfgProject(),
                "la fixture doit reproduire la base reelle : article sans projet");

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projetCode").value("PRJ-CTX"))
                .andExpect(jsonPath("$.societeCode").value("SOC-CTX"));
    }

    /** Un dossier sans article n'a ni tranche ni projet : les champs sont nuls, sans exception. */
    @Test
    void dossierSansArticle_neCassePas() throws Exception {
        ComFolder f = folderRepository.saveAndFlush(dossier());

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.trancheId").doesNotExist())
                .andExpect(jsonPath("$.projetId").doesNotExist())
                .andExpect(jsonPath("$.societeId").doesNotExist())
                // La vocation est portée par le dossier lui-même, elle reste servie.
                .andExpect(jsonPath("$.vocationCode").value("VOC-CTX"));
    }

    // ------------------------------------------------------------------
    // Piste d'audit
    // ------------------------------------------------------------------

    @Test
    void fiche_exposeLaPisteDaudit() throws Exception {
        ComFolder f = dossierComplet();

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creePar").value("createur-test"))
                .andExpect(jsonPath("$.creeLe").exists())
                .andExpect(jsonPath("$.modifiePar").value("modificateur-test"))
                .andExpect(jsonPath("$.modifieLe").exists());
    }

    // ------------------------------------------------------------------
    // Origine (prospection)
    // ------------------------------------------------------------------

    @Test
    void fiche_exposeLOrigineDeLaProspection() throws Exception {
        ComProspectionKind etape = new ComProspectionKind();
        etape.setId(UUID.randomUUID());
        etape.setCode("ETP-CTX");
        etape.setDescription("Negociation");
        prospectionKindRepository.save(etape);

        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORI-CTX");
        origine.setDescription("Facebook");
        prospectionOriginRepository.save(origine);

        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-CTX-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDocDate(LocalDateTime.now());
        p.setComProspectionKind(etape);
        p.setComProspectionOrigin(origine);
        prospectionRepository.saveAndFlush(p);

        ComFolder f = dossierComplet();
        f.setComProspection(p);
        folderRepository.saveAndFlush(f);

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapeProspection").value("Negociation"))
                .andExpect(jsonPath("$.origineProspection").value("Facebook"))
                .andExpect(jsonPath("$.dateProspection").exists());
    }

    /** 3 dossiers sur 358 n'ont pas de prospection — cas réel, pas une anomalie. */
    @Test
    void dossierSansProspection_rendLesChampsNuls() throws Exception {
        ComFolder f = dossierComplet();

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapeProspection").doesNotExist())
                .andExpect(jsonPath("$.origineProspection").doesNotExist())
                .andExpect(jsonPath("$.dateProspection").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Contexte de l'article
    // ------------------------------------------------------------------

    @Test
    void article_exposeSonContextePatrimonial() throws Exception {
        ComFolder f = dossierComplet();

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles.length()").value(1))
                .andExpect(jsonPath("$.articles[0].categorieCode").value("CAT-CTX"))
                .andExpect(jsonPath("$.articles[0].categorieLibelle").value("Categorie contexte"))
                .andExpect(jsonPath("$.articles[0].typeCode").value("TYPE-CTX"))
                .andExpect(jsonPath("$.articles[0].typeLibelle").value("Type contexte"))
                .andExpect(jsonPath("$.articles[0].etageCode").value("RDC-CTX"))
                .andExpect(jsonPath("$.articles[0].etageLibelle").value("Rez de chaussee"))
                .andExpect(jsonPath("$.articles[0].immeubleCode").value("IMM-CTX"))
                .andExpect(jsonPath("$.articles[0].immeubleLibelle").value("Immeuble contexte"));
    }

    /** Étage et immeuble sont en LEFT JOIN dans la vue : leur absence ne doit rien casser. */
    @Test
    void articleSansEtageNiImmeuble_neCassePas() throws Exception {
        ComFolder f = folderRepository.saveAndFlush(dossier());
        StkItem item = article("LOT-NU-" + UUID.randomUUID().toString().substring(0, 6), f, null, null);
        f.setStkItem(item);
        folderRepository.saveAndFlush(f);

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles[0].categorieCode").value("CAT-CTX"))
                .andExpect(jsonPath("$.articles[0].etageId").doesNotExist())
                .andExpect(jsonPath("$.articles[0].immeubleId").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Non-régression
    // ------------------------------------------------------------------

    /** La liste des dossiers n'est pas touchée par ce lot. */
    @Test
    void listeDesDossiers_resteInchangee() throws Exception {
        ComFolder f = dossierComplet();

        mockMvc.perform(get("/api/dossiers").param("search", f.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(f.getCode()))
                .andExpect(jsonPath("$.content[0].projetCode").doesNotExist())
                .andExpect(jsonPath("$.content[0].creePar").doesNotExist());
    }
}
