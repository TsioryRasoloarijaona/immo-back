package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionRepository;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginSupportRepository;
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
 * Enrichissement de {@code GET /api/prospections/{id}} — voir
 * {@code instructions/FEATURE-enrichissement-fiche-prospection.md}.
 *
 * <p><b>Ce que cette classe verrouille, et pourquoi.</b> Les champs ajoutés ne viennent
 * <em>pas</em> de {@code ComSaleProspectionView} mais des associations JPA de
 * {@code ComProspection}. C'est un choix de conception, pas un détail d'implémentation : mapper
 * une colonne de plus sur l'entité de vue dégraderait l'élagage des sous-requêtes corrélées par
 * SQL Server. La conséquence observable — et c'est le test le plus important d'ici — est qu'une
 * <b>pré-prospection porte ces champs</b> alors que son bloc {@code activite} vaut {@code null}.
 * Si quelqu'un rebranche un jour ces champs sur la vue, c'est ce test-là qui rougira.
 *
 * <p><b>Pas de {@code exists()} sur une valeur potentiellement nulle</b> : {@code jsonPath} échoue
 * sur {@code null}, ce qui fait rougir des tests dont le code est correct. Chaque assertion pose
 * donc une valeur dans la fixture et la compare avec {@code value(...)}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionFicheEnrichieTest {

    /** {@code StrEntity.PKey} de « Prospection vente » — semé par {@code ci/schema/050_views.sql}. */
    private static final UUID ENTITE_PROSPECTION =
            UUID.fromString("e7c73f70-1111-40d9-aa78-a2825c42b601");

    /** {@code StrEntity.PKey} de « Pré-Prospection », que la vue exclut par construction. */
    private static final UUID ENTITE_PRE_PROSPECTION =
            UUID.fromString("b92df3d8-158c-4e1e-827a-70feb172a264");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private CfgCompanyRepository companyRepository;
    @Autowired
    private CfgProjectRepository projectRepository;
    @Autowired
    private CfgTrancheRepository trancheRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private CfgTierTypeRepository tierTypeRepository;
    @Autowired
    private AdmCountryRepository countryRepository;
    @Autowired
    private StkItemRepository itemRepository;
    @Autowired
    private StkItemTypeRepository itemTypeRepository;
    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComProspectionOriginRepository originRepository;
    @Autowired
    private ComProspectionOriginSupportRepository originSupportRepository;
    @Autowired
    private ComProspectionKindRepository kindRepository;
    @Autowired
    private ComActionMessageTypeRepository messageTypeRepository;
    @Autowired
    private ComActionRepository actionRepository;
    @Autowired
    private ComProspectionClosingReasonRepository closingReasonRepository;
    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** Tranche rattachée à un projet lui-même rattaché à une société — le chemin de la société. */
    private CfgTranche trancheAvecSociete(String codeSociete) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode(codeSociete);
        societe.setDescription("Société " + codeSociete);
        societe.setIsActive(true);
        companyRepository.save(societe);

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-ENR-" + suffixe());
        projet.setDescription("Projet enrichi");
        projet.setCfgCompany(societe);
        projectRepository.save(projet);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-ENR-" + suffixe());
        tranche.setDescription("Tranche enrichie");
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    private ComProspection prospection(UUID entite) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-ENR-" + suffixe());
        p.setDocDate(LocalDateTime.now());
        p.setStrEntityId(entite);
        return p;
    }

    @Test
    void ficheProspection_exposeLaSocieteDeriveeDeLaTranche() throws Exception {
        CfgTranche tranche = trancheAvecSociete("ADMOD-ENR");
        ComProspection p = prospection(ENTITE_PROSPECTION);
        p.setStkHierarchy(tranche);
        prospectionRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.societeCode").value("ADMOD-ENR"))
                .andExpect(jsonPath("$.societeLibelle").value("Société ADMOD-ENR"))
                .andExpect(jsonPath("$.societeId").value(
                        tranche.getCfgProject().getCfgCompany().getId().toString()));
    }

    /**
     * La société n'est pas saisie sur la prospection : sans tranche, il n'y a pas de chemin vers
     * elle. L'absence doit se traduire par un champ nul, pas par une erreur — 0,2 % des
     * prospections réelles sont dans ce cas.
     */
    @Test
    void prospectionSansTranche_naPasDeSocieteEtNeCassePas() throws Exception {
        ComProspection p = prospection(ENTITE_PROSPECTION);
        prospectionRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.societeId").doesNotExist())
                .andExpect(jsonPath("$.societeCode").doesNotExist())
                .andExpect(jsonPath("$.societeLibelle").doesNotExist());
    }

    @Test
    void ficheProspection_exposeLesCoordonneesEtLeTypeDuProspect() throws Exception {
        CfgTierType type = new CfgTierType();
        type.setId(UUID.randomUUID());
        type.setCode("PRO-ENR");
        type.setDescription("Prospect");
        tierTypeRepository.save(type);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("TIER-ENR-" + suffixe());
        prospect.setDescription("Prospect Enrichi");
        prospect.setMobile("6 61 83 60 90");
        // Stocké sans « + » en base réelle : c'est « 212 », jamais « +212 ». Le front compose.
        prospect.setMobileIndicatif("212");
        prospect.setEMail("prospect.enrichi@test.local");
        prospect.setCfgTierType(type);
        tierRepository.save(prospect);

        ComProspection p = prospection(ENTITE_PROSPECTION);
        p.setCfgProspect(prospect);
        prospectionRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectCode").value(prospect.getCode()))
                .andExpect(jsonPath("$.mobileProspect").value("6 61 83 60 90"))
                .andExpect(jsonPath("$.indicatifMobile").value("212"))
                .andExpect(jsonPath("$.emailFiche").value("prospect.enrichi@test.local"))
                .andExpect(jsonPath("$.typeProspectId").value(type.getId().toString()))
                .andExpect(jsonPath("$.typeProspect").value("PRO-ENR"))
                .andExpect(jsonPath("$.typeProspectLibelle").value("Prospect"));
    }

    /**
     * {@code typeProspect} qualifie le <em>tiers</em>, {@code typeProspectionCode} le
     * <em>document</em>. Un caractère d'écart pour deux notions distinctes : le test fige les deux
     * ensemble pour qu'une confusion de mapping ne puisse pas passer inaperçue.
     */
    @Test
    void typeDuTiers_etTypeDuDocument_sontDeuxChampsDistincts() throws Exception {
        CfgTierType type = new CfgTierType();
        type.setId(UUID.randomUUID());
        type.setCode("PRORNT-ENR");
        type.setDescription("Prospect location");
        tierTypeRepository.save(type);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("TIER-ENR-" + suffixe());
        prospect.setDescription("Prospect Distinct");
        prospect.setCfgTierType(type);
        tierRepository.save(prospect);

        ComProspection p = prospection(ENTITE_PROSPECTION);
        p.setCfgProspect(prospect);
        prospectionRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeProspect").value("PRORNT-ENR"))
                .andExpect(jsonPath("$.typeProspectionCode").value("ComProspection"));
    }

    @Test
    void ficheProspection_exposeLesCodesDesReferentiels() throws Exception {
        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORI-ENR");
        origine.setDescription("Salon immobilier");
        originRepository.save(origine);

        ComProspectionOriginSupport support = new ComProspectionOriginSupport();
        support.setId(UUID.randomUUID());
        support.setCode("SUP-ENR");
        support.setDescription("Affichage");
        originSupportRepository.save(support);

        ComProspectionKind etape = new ComProspectionKind();
        etape.setId(UUID.randomUUID());
        etape.setCode("3 QUALIFIE-ENR");
        etape.setDescription("Qualifié");
        kindRepository.save(etape);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VENTE-ENR");
        vocation.setDescription("Vente");
        vocationRepository.save(vocation);

        ComActionMessageType typeContact = new ComActionMessageType();
        typeContact.setId(UUID.randomUUID());
        typeContact.setCode("TEL-ENR");
        typeContact.setDescription("Téléphone");
        messageTypeRepository.save(typeContact);

        ComAction action = new ComAction();
        action.setId(UUID.randomUUID());
        action.setCode("CAMP-ENR");
        action.setDescription("Campagne printemps");
        actionRepository.save(action);

        ComProspectionClosingReason raison = new ComProspectionClosingReason();
        raison.setId(UUID.randomUUID());
        raison.setCode("ABANDON-ENR");
        raison.setDescription("Abandon du projet");
        closingReasonRepository.save(raison);

        ComProspection p = prospection(ENTITE_PROSPECTION);
        p.setComProspectionOrigin(origine);
        p.setComProspectionOriginSupport(support);
        p.setComProspectionKind(etape);
        p.setStkVocation(vocation);
        p.setComActionMessageType(typeContact);
        p.setComAction(action);
        p.setComProspectionClosingReason(raison);
        prospectionRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                // Le code s'ajoute au libellé déjà servi, il ne le remplace pas.
                .andExpect(jsonPath("$.origineCode").value("ORI-ENR"))
                .andExpect(jsonPath("$.origine").value("Salon immobilier"))
                .andExpect(jsonPath("$.origineSupportCode").value("SUP-ENR"))
                .andExpect(jsonPath("$.etapeCode").value("3 QUALIFIE-ENR"))
                .andExpect(jsonPath("$.etape").value("Qualifié"))
                .andExpect(jsonPath("$.vocationCode").value("VENTE-ENR"))
                .andExpect(jsonPath("$.typeContactCode").value("TEL-ENR"))
                .andExpect(jsonPath("$.actionCommercialeCode").value("CAMP-ENR"))
                .andExpect(jsonPath("$.raisonFermetureCode").value("ABANDON-ENR"));
    }

    @Test
    void ficheProspection_exposeLaCategorieEtLeTypeDeLArticle() throws Exception {
        CfgTranche tranche = trancheAvecSociete("SOC-ART-ENR");

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-ENR-" + suffixe());
        type.setDescription("2 CHAMBRES + SALON");
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-ENR-" + suffixe());
        categorie.setDescription("APPARTEMENT");
        categorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(categorie);

        // StkItem.StkVocationId est NOT NULL en base : un lot existe toujours au sein d'une
        // vocation (vente, location). La fiche n'en dit rien, mais la fixture doit la poser.
        StkVocation vocationArticle = new StkVocation();
        vocationArticle.setId(UUID.randomUUID());
        vocationArticle.setCode("VENTE-ART-" + suffixe());
        vocationArticle.setDescription("Vente");
        vocationRepository.save(vocationArticle);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("ART-ENR-" + suffixe());
        article.setDescription("Lot enrichi");
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(vocationArticle);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemRepository.save(article);

        ComProspection p = prospection(ENTITE_PROSPECTION);
        p.setStkHierarchy(tranche);
        p.setStkItem(article);
        prospectionRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleCode").value(article.getCode()))
                .andExpect(jsonPath("$.articleCategorie").value("APPARTEMENT"))
                .andExpect(jsonPath("$.articleType").value("2 CHAMBRES + SALON"));
    }

    @Test
    void ficheProspection_exposeLePaysEtLAudit() throws Exception {
        AdmCountry pays = new AdmCountry();
        pays.setId(UUID.randomUUID());
        pays.setCode("MA-ENR");
        pays.setDescription("Maroc");
        countryRepository.save(pays);

        ComProspection p = prospection(ENTITE_PROSPECTION);
        p.setAdmCountry(pays);
        p.setUserCreate("agent.creation");
        p.setUserCreateDateTime(LocalDateTime.of(2026, 1, 15, 9, 30));
        p.setUserUpdate("agent.modification");
        p.setUserUpdateDateTime(LocalDateTime.of(2026, 3, 2, 14, 5));
        prospectionRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paysId").value(pays.getId().toString()))
                .andExpect(jsonPath("$.paysCode").value("MA-ENR"))
                .andExpect(jsonPath("$.pays").value("Maroc"))
                .andExpect(jsonPath("$.creePar").value("agent.creation"))
                .andExpect(jsonPath("$.creeLe").value("2026-01-15T09:30:00"))
                .andExpect(jsonPath("$.modifiePar").value("agent.modification"))
                .andExpect(jsonPath("$.modifieLe").value("2026-03-02T14:05:00"));
    }

    /**
     * <b>Le test qui garde la décision de conception.</b> La vue exclut les pré-prospections, donc
     * {@code activite} y vaut {@code null} — mais les champs ajoutés par cette feature viennent des
     * associations de {@code ComProspection}, pas de la vue, et doivent donc rester servis. Ce test
     * rougirait si on rebranchait un jour ces champs sur {@code ComSaleProspectionView}.
     */
    @Test
    void preProspection_porteLesChampsDEntite_bienQueLeBlocActiviteSoitNul() throws Exception {
        CfgTranche tranche = trancheAvecSociete("SOC-PRE-ENR");

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("TIER-PRE-" + suffixe());
        prospect.setDescription("Pré-prospect");
        prospect.setMobile("6 11 48 54 37");
        prospect.setMobileIndicatif("212");
        tierRepository.save(prospect);

        ComProspection p = prospection(ENTITE_PRE_PROSPECTION);
        p.setStkHierarchy(tranche);
        p.setCfgProspect(prospect);
        p.setUserCreate("agent.pre");
        prospectionRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                // La vue ne rend rien sur une pré-prospection : « non disponible », pas « zéro ».
                .andExpect(jsonPath("$.activite").doesNotExist())
                // ...mais tout ce qui vient de l'entité est là.
                .andExpect(jsonPath("$.societeCode").value("SOC-PRE-ENR"))
                .andExpect(jsonPath("$.prospectCode").value(prospect.getCode()))
                .andExpect(jsonPath("$.mobileProspect").value("6 11 48 54 37"))
                .andExpect(jsonPath("$.indicatifMobile").value("212"))
                .andExpect(jsonPath("$.creePar").value("agent.pre"));
    }
}
