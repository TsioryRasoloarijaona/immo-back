package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import jakarta.persistence.EntityManager;
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
 * Colonnes ajoutées après confrontation à la trace d'audit du logiciel de bureau — article
 * principal, responsable de vente, délais de cycle.
 *
 * <p>Voir {@code instructions/FEATURE-colonnes-dossier-ecran-legataire.md}. Ce qui est vérifié ici,
 * et que la seule lecture du code ne donne pas : que les {@code @Formula} se rendent réellement en
 * SQL, et que chaque colonne annoncée par le référentiel soit à la fois <strong>servie</strong> par
 * la liste et <strong>acceptée</strong> par {@code /groupes}. Le catalogue {@code ColonneDossier}
 * pilote les deux ; une entrée sans champ correspondant dans le DTO ferait annoncer au sélecteur
 * une colonne absente de la réponse.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierColonnesEcranLegataireTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private StkItemRepository itemRepository;
    @Autowired
    private StkItemTypeRepository itemTypeRepository;
    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private EntityManager entityManager;

    private StkVocation vocation;
    private StkItemType itemType;
    private StkItemCategory itemCategory;

    @BeforeEach
    void setUp() {
        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-ECR-TEST");
        vocation = vocationRepository.save(vocation);

        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-ECR");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemType = itemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-ECR");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategory = itemCategoryRepository.save(itemCategory);
    }

    /**
     * Vide le contexte de persistance avant l'appel HTTP.
     *
     * <p><strong>Indispensable dès qu'un test touche une {@code @Formula}</strong> : une formule
     * n'est peuplée qu'au <em>chargement</em> depuis la base. Sans ce vidage, la requête rendrait
     * l'instance encore en session — celle que le test vient de construire en Java, dont les délais
     * n'ont jamais été lus — et les assertions verraient {@code null} en croyant avoir testé le
     * calcul. C'est exactement ce qui est arrivé à la première version de ces tests.
     */
    private void relireDepuisLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private ComFolder newFolder(String code) {
        ComFolder f = new ComFolder();
        f.setId(UUID.randomUUID());
        f.setCode(code);
        f.setStkVocation(vocation);
        return folderRepository.save(f);
    }

    private StkItem newArticle(String code, String libelle) {
        StkItem i = new StkItem();
        i.setId(UUID.randomUUID());
        i.setCode(code);
        i.setDescription(libelle);
        i.setStkItemType(itemType);
        i.setStkItemCategory(itemCategory);
        i.setStkVocation(vocation);
        i.setStkKind(fixtureReferentielStk.natureParDefaut());
        return itemRepository.save(i);
    }

    // ------------------------------------------------------------------
    // Le défaut ne bouge pas
    // ------------------------------------------------------------------

    /**
     * Les colonnes ajoutées sont toutes {@code parDefaut = false}. Une intégration qui ignore
     * {@code ?colonnes=} — le front d'aujourd'hui — ne doit voir apparaître aucune clé nouvelle.
     */
    @Test
    void sansSelection_aucuneNouvelleCleNApparait() throws Exception {
        newFolder("ECR-DEF-1");
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers").param("search", "ECR-DEF-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].articleCode").doesNotExist())
                .andExpect(jsonPath("$.content[0].responsableVenteNom").doesNotExist())
                .andExpect(jsonPath("$.content[0].delaiConcretisation").doesNotExist())
                .andExpect(jsonPath("$.content[0].bloc").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Article principal — la clé de tri de l'écran légataire
    // ------------------------------------------------------------------

    @Test
    void article_estServiQuandDemande() throws Exception {
        ComFolder f = newFolder("ECR-ART-1");
        f.setStkItem(newArticle("A-203", "Appartement 203"));
        folderRepository.save(f);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "ECR-ART-")
                        .param("colonnes", "code,articleCode,articleLibelle,categorieArticle,typeArticle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].articleCode").value("A-203"))
                .andExpect(jsonPath("$.content[0].articleLibelle").value("Appartement 203"))
                .andExpect(jsonPath("$.content[0].categorieArticle").value("CAT-ECR"))
                .andExpect(jsonPath("$.content[0].typeArticle").value("TYPE-ECR"));
    }

    // ------------------------------------------------------------------
    // Responsable de vente — l'acteur manquant à 61 %
    // ------------------------------------------------------------------

    @Test
    void responsableVente_estDistinctDuCommercial() throws Exception {
        CfgTier commercial = new CfgTier();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("TIER-ECR-COM");
        commercial.setLastName("COMMERCIAL");
        commercial = tierRepository.save(commercial);

        CfgTier responsable = new CfgTier();
        responsable.setId(UUID.randomUUID());
        responsable.setCode("TIER-ECR-RESP");
        responsable.setLastName("ATCHANY");
        responsable.setFirstName("Hamza");
        responsable = tierRepository.save(responsable);

        ComFolder f = newFolder("ECR-RESP-1");
        f.setCfgCommercial(commercial);
        f.setCfgSaleManager(responsable);
        folderRepository.save(f);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "ECR-RESP-")
                        .param("colonnes", "code,commercialNom,responsableVenteNom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commercialNom").value("COMMERCIAL"))
                .andExpect(jsonPath("$.content[0].responsableVenteNom").value("Hamza ATCHANY"));
    }

    // ------------------------------------------------------------------
    // Délais — des @Formula, donc du SQL réellement émis
    // ------------------------------------------------------------------

    /** 31 jours entre le 1ᵉʳ janvier et le 1ᵉʳ février, calculés par la base et non par nous. */
    @Test
    void delaiConcretisation_estCalculeParLaBase() throws Exception {
        ComFolder f = newFolder("ECR-DEL-1");
        f.setDocDate(LocalDateTime.of(2025, 1, 1, 9, 0));
        f.setAdvanceDate(LocalDateTime.of(2025, 2, 1, 9, 0));
        folderRepository.save(f);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "ECR-DEL-1")
                        .param("colonnes", "code,delaiConcretisation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].delaiConcretisation").value(31));
    }

    @Test
    void delaisNotaireEtLivraison_sontCalculesParLaBase() throws Exception {
        ComFolder f = newFolder("ECR-DEL-2");
        f.setAdvanceDate(LocalDateTime.of(2025, 1, 1, 9, 0));
        f.setAgreementDate(LocalDateTime.of(2025, 3, 2, 9, 0));      // +60 j
        f.setDeliveryDateReal(LocalDateTime.of(2025, 4, 1, 9, 0));   // +30 j
        folderRepository.save(f);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "ECR-DEL-2")
                        .param("colonnes", "code,delaiNotaire,delaiLivraison,dateEnvoiNotaire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].delaiNotaire").value(60))
                .andExpect(jsonPath("$.content[0].delaiLivraison").value(30))
                .andExpect(jsonPath("$.content[0].dateEnvoiNotaire").value("2025-03-02T09:00:00"));
    }

    /** B1 (FEATURE-vente-socle.md) : chez OMRAN le compromis est CreditApprovalDate, AgreementDate l'envoi au notaire. */
    @Test
    void dateCompromis_litCreditApprovalDate_etDateEnvoiNotaire_AgreementDate() throws Exception {
        ComFolder f = newFolder("ECR-DEL-B1");
        f.setCreditApprovalDate(LocalDateTime.of(2025, 2, 10, 0, 0));
        f.setAgreementDate(LocalDateTime.of(2025, 3, 2, 0, 0));
        folderRepository.save(f);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "ECR-DEL-B1")
                        .param("colonnes", "code,dateCompromis,dateEnvoiNotaire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dateCompromis").value("2025-02-10T00:00:00"))
                .andExpect(jsonPath("$.content[0].dateEnvoiNotaire").value("2025-03-02T00:00:00"));
    }

    /** Sans jalon, le délai est nul — pas zéro. Un zéro dirait « concrétisé le jour même ». */
    @Test
    void delai_estNulTantQueLeJalonNEstPasAtteint() throws Exception {
        ComFolder f = newFolder("ECR-DEL-3");
        f.setDocDate(LocalDateTime.of(2025, 1, 1, 9, 0));
        folderRepository.save(f);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "ECR-DEL-3")
                        .param("colonnes", "code,delaiConcretisation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].delaiConcretisation").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Cohérence sélecteur ↔ groupement
    // ------------------------------------------------------------------

    @Test
    void nouvellesColonnes_sontAnnonceesConformementAuComportement() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='articleCode')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='responsableVenteNom')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='delaiNotaire')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='delaiNotaire')].agregeable").value(true))
                // Non groupable, et le référentiel doit le dire — voir le test du 400 plus bas.
                .andExpect(jsonPath("$[?(@.nom=='delaiConcretisation')].groupable").value(false))
                .andExpect(jsonPath("$[?(@.nom=='delaiConcretisation')].agregeable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='articleCode')].groupe").value("ARTICLE"))
                .andExpect(jsonPath("$[?(@.nom=='delaiNotaire')].groupe").value("DELAIS"));
    }

    @Test
    void groupementParArticle_fonctionne() throws Exception {
        ComFolder a = newFolder("ECR-GRP-1");
        a.setStkItem(newArticle("A-500", "Lot 500"));
        folderRepository.save(a);
        ComFolder b = newFolder("ECR-GRP-2");
        b.setStkItem(newArticle("A-501", "Lot 501"));
        folderRepository.save(b);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "articleCode")
                        .param("search", "ECR-GRP-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='A-500')].nombre").value(1));
    }

    /** Un délai est un nombre : il doit pouvoir servir de clé de groupe ET de total. */
    @Test
    void groupementParDelai_fonctionneEtSAdditionne() throws Exception {
        ComFolder a = newFolder("ECR-GRPD-1");
        a.setAdvanceDate(LocalDateTime.of(2025, 1, 1, 9, 0));
        a.setAgreementDate(LocalDateTime.of(2025, 1, 11, 9, 0));   // 10 j
        folderRepository.save(a);
        ComFolder b = newFolder("ECR-GRPD-2");
        b.setAdvanceDate(LocalDateTime.of(2025, 2, 1, 9, 0));
        b.setAgreementDate(LocalDateTime.of(2025, 2, 11, 9, 0));   // 10 j
        folderRepository.save(b);
        ComFolder c = newFolder("ECR-GRPD-3");
        c.setAdvanceDate(LocalDateTime.of(2025, 3, 1, 9, 0));
        c.setAgreementDate(LocalDateTime.of(2025, 3, 21, 9, 0));   // 20 j
        folderRepository.save(c);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers/groupes")
                        .param("groupBy", "delaiNotaire")
                        .param("agregats", "delaiNotaire")
                        .param("search", "ECR-GRPD-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==10)].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==10)].agregats.delaiNotaire").value(20))
                .andExpect(jsonPath("$.content[?(@.cle==20)].nombre").value(1));
    }

    /**
     * {@code delaiConcretisation} est sommable mais <strong>pas</strong> groupable : sa formule
     * contient une sous-requête corrélée (la date de la prospection d'origine) et SQL Server refuse
     * « un agrégat ou une sous-requête dans une expression utilisée dans la liste GROUP BY ».
     *
     * <p>Le refus doit être un 400 lisible, pas la {@code SQLGrammarException} en 500 qu'on
     * obtenait avant de le déclarer non groupable.
     */
    @Test
    void groupementParDelaiConcretisation_rend400() throws Exception {
        mockMvc.perform(get("/api/dossiers/groupes").param("groupBy", "delaiConcretisation"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void depliageParDelai_rendLesLignesComptees() throws Exception {
        ComFolder dix = newFolder("ECR-DEP-1");
        dix.setAdvanceDate(LocalDateTime.of(2025, 1, 1, 9, 0));
        dix.setAgreementDate(LocalDateTime.of(2025, 1, 11, 9, 0));
        folderRepository.save(dix);
        ComFolder vingt = newFolder("ECR-DEP-2");
        vingt.setAdvanceDate(LocalDateTime.of(2025, 1, 1, 9, 0));
        vingt.setAgreementDate(LocalDateTime.of(2025, 1, 21, 9, 0));
        folderRepository.save(vingt);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers")
                        .param("search", "ECR-DEP-")
                        .param("groupeColonne", "delaiNotaire")
                        .param("groupeValeur", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + dix.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + vingt.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------
    // La fiche
    // ------------------------------------------------------------------

    @Test
    void fiche_sertLeResponsableDeVenteEtLesDelais() throws Exception {
        CfgTier responsable = new CfgTier();
        responsable.setId(UUID.randomUUID());
        responsable.setCode("TIER-ECR-FICHE");
        responsable.setLastName("BOUTANE");
        responsable.setFirstName("Asmaa");
        responsable = tierRepository.save(responsable);

        ComFolder f = newFolder("ECR-FICHE-1");
        f.setCfgSaleManager(responsable);
        f.setDocDate(LocalDateTime.of(2025, 1, 1, 9, 0));
        f.setAdvanceDate(LocalDateTime.of(2025, 1, 16, 9, 0));
        folderRepository.save(f);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/dossiers/" + f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsableVenteNom").value("Asmaa BOUTANE"))
                .andExpect(jsonPath("$.responsableVenteId").value(responsable.getId().toString()))
                .andExpect(jsonPath("$.delaiConcretisation").value(15));
    }
}
