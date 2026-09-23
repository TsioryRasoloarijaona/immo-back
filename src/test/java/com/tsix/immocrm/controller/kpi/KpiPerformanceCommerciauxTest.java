package com.tsix.immocrm.controller.kpi;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/kpi/commerciaux[/moi]} — voir
 * {@code instructions/FEATURE-kpi-performance-commerciaux.md}.
 *
 * <p>Trois propriétés valent plus que le simple comptage, et sont ce que cette classe protège :
 * <ol>
 *   <li><b>l'activité est attribuée à son auteur</b>, pas au commercial assigné à la prospection —
 *       c'est la décision §3, prise sur mesure ({@code UserCreate} concorde avec le compte
 *       37 186/37 186 et avec {@code ComActionMessage.CfgEmployeeId} seulement 30 894 fois) ;</li>
 *   <li><b>un employé à deux comptes reste une seule ligne</b>, sinon ses chiffres se coupent en
 *       deux sans que rien ne le signale ;</li>
 *   <li><b>le filtre d'entité juridique s'applique</b> — {@code /api/kpi} n'est pas couverte par
 *       {@code PerimetreInterceptor}, donc rien ne le fait à la place du service.</li>
 * </ol>
 *
 * <p>Chaque test bâtit ses lignes par les vrais repositories ; le {@code @Transactional} de la
 * classe les annule ensuite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KpiPerformanceCommerciauxTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private AdmRoleRepository roleRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComActionMessageRepository messageRepository;
    @Autowired private ComActionMessageTypeRepository messageTypeRepository;
    @Autowired private ComActionMessageSubCategoryRepository messageSubCategoryRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;

    private CfgTranche trancheActive;
    private CfgTranche trancheInactive;
    private CfgProject projetActif;
    private CfgProject projetInactif;

    private ComActionMessageType typeAppelSortant;
    private ComActionMessageType typeVisite;
    private ComActionMessageSubCategory resultatVisiteDecrochee;
    private ComActionMessageSubCategory resultatRdvPris;

    /** Milieu de la fenêtre par défaut (30 jours) : toutes les fixtures y tombent. */
    private static final LocalDateTime MAINTENANT = LocalDateTime.now().minusDays(2);

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-KPI-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-KPI-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-KPI-TEST");
        stkVocationRepository.save(vocation);

        projetActif = newProjet("KPI-P-ACTIF", newSociete("KPI-ACTIVE", true));
        projetInactif = newProjet("KPI-P-INACTIF", newSociete("KPI-INACTIVE", false));
        trancheActive = newTranche("KPI-TR-ACTIVE", projetActif);
        trancheInactive = newTranche("KPI-TR-INACTIVE", projetInactif);

        // Les référentiels d'interaction ne sont pas dans ci/schema/040_seed_reference.sql : ils
        // sont créés ici avec les libellés exacts de la base réelle, ceux-là mêmes que porte
        // InteractionKpi. Un écart d'accent suffirait à mettre tous les compteurs à zéro.
        typeAppelSortant = newType(InteractionKpi.TYPE_APPEL_SORTANT);
        typeVisite = newType(InteractionKpi.TYPE_VISITE);
        resultatVisiteDecrochee = newSousCategorie(InteractionKpi.RESULTAT_VISITE_DECROCHEE);
        resultatRdvPris = newSousCategorie(InteractionKpi.RESULTAT_RDV_PRIS);
    }

    // ------------------------------------------------------------------ les compteurs demandés

    @Test
    void lesQuatreCompteursDemandes_sontRemontesPourLeCommercialConnecte() throws Exception {
        CfgTier commercial = newTier("KPI-COM");
        AdmUser compte = newUser("kpi.com", commercial);
        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT);

        appel(compte, prospection, null);
        appel(compte, prospection, null);
        appel(compte, prospection, resultatVisiteDecrochee);
        appel(compte, prospection, resultatRdvPris);
        visite(compte, prospection);
        newDossier(commercial, trancheActive, MAINTENANT, "1200000");

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialId").value(commercial.getId().toString()))
                .andExpect(jsonPath("$.appelsPasses").value(4))
                .andExpect(jsonPath("$.visitesDecrochees").value(1))
                .andExpect(jsonPath("$.rdvObtenus").value(1))
                .andExpect(jsonPath("$.visitesRealisees").value(1))
                .andExpect(jsonPath("$.interactionsTotal").value(5))
                .andExpect(jsonPath("$.prospectionsTraitees").value(1))
                .andExpect(jsonPath("$.prospectionsAssignees").value(1))
                .andExpect(jsonPath("$.dossiersCrees").value(1))
                .andExpect(jsonPath("$.montantTotal").value(1200000.00))
                .andExpect(jsonPath("$.panierMoyen").value(1200000.00));
    }

    /**
     * « Visite décrochée » et « visite réalisée » ne sont pas la même chose et ne doivent jamais
     * être additionnées : la première est un engagement obtenu au téléphone (5 715 en base), la
     * seconde une visite consignée (4 311). Les confondre ferait passer pour excellent un
     * commercial qui décroche beaucoup de rendez-vous et n'en concrétise aucun.
     */
    @Test
    void visiteDecrochee_etVisiteRealisee_sontComptesSeparement() throws Exception {
        CfgTier commercial = newTier("KPI-VIS");
        AdmUser compte = newUser("kpi.vis", commercial);
        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT);

        appel(compte, prospection, resultatVisiteDecrochee);
        appel(compte, prospection, resultatVisiteDecrochee);
        visite(compte, prospection);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitesDecrochees").value(2))
                .andExpect(jsonPath("$.visitesRealisees").value(1))
                .andExpect(jsonPath("$.appelsPasses").value(2));
    }

    // ------------------------------------------------------------------ §3 — l'attribution

    /**
     * <b>Le test central de la feature.</b> Un chef qui appelle sur la prospection d'un commercial
     * voit l'appel compté chez lui, et le commercial garde sa prospection au portefeuille. Une
     * implémentation qui attribuerait par {@code ComProspection.CfgCommercialId} — ou par
     * {@code ComActionMessage.CfgEmployeeId}, qui désigne l'employé <i>concerné</i> — inverse les
     * deux et passe tous les autres tests de cette classe.
     */
    @Test
    void appelSaisiParUnChef_estCompteChezLeChefEtPasChezLeCommercialAssigne() throws Exception {
        CfgTier commercial = newTier("KPI-TITULAIRE");
        AdmUser compteCommercial = newUser("kpi.titulaire", commercial);
        CfgTier chef = newTier("KPI-CHEF");
        AdmUser compteChef = newUser("kpi.chef", chef);

        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT);
        // L'appel porte explicitement le titulaire en "employé concerné" : c'est la configuration
        // qui piège une implémentation lisant CfgEmployeeId.
        ComActionMessage message = appel(compteChef, prospection, null);
        message.setCfgEmployee(commercial);
        messageRepository.save(message);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compteChef, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsPasses").value(1))
                .andExpect(jsonPath("$.prospectionsAssignees").value(0));

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compteCommercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsPasses").value(0))
                .andExpect(jsonPath("$.prospectionsAssignees").value(1))
                // La prospection a bien été prise en charge, même si c'est le chef qui a appelé :
                // ce qu'on mesure là est le délai avant que le prospect reçoive une réponse.
                .andExpect(jsonPath("$.prospectionsPrisesEnCharge").value(1));
    }

    /**
     * Un employé porte parfois deux comptes (mesuré : 1 cas en base réelle). Regrouper par compte
     * couperait ses chiffres en deux lignes sans que rien ne le signale — d'où la clé
     * {@code CfgTier} et non {@code AdmUser}.
     */
    @Test
    void employeAvecDeuxComptes_neFaitQuUneLigneEtSesChiffresSAdditionnent() throws Exception {
        CfgTier commercial = newTier("KPI-BICOMPTE");
        AdmUser premier = newUser("kpi.bicompte.a", commercial);
        AdmUser second = newUser("kpi.bicompte.b", commercial);
        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT);

        appel(premier, prospection, null);
        appel(second, prospection, null);
        appel(second, prospection, null);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(premier, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsPasses").value(3));
    }

    // ------------------------------------------------------------------ réactivité et taux

    @Test
    void priseEnCharge_distingueLesProspectionsJamaisContactees() throws Exception {
        CfgTier commercial = newTier("KPI-REACT");
        AdmUser compte = newUser("kpi.react", commercial);

        ComProspection contactee = newProspection(commercial, trancheActive, MAINTENANT.minusHours(10));
        newProspection(commercial, trancheActive, MAINTENANT);
        newProspection(commercial, trancheActive, MAINTENANT);
        appel(compte, contactee, null, MAINTENANT.minusHours(4));

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectionsAssignees").value(3))
                .andExpect(jsonPath("$.prospectionsPrisesEnCharge").value(1))
                .andExpect(jsonPath("$.tauxPriseEnCharge").value(0.333))
                .andExpect(jsonPath("$.delaiMedianPremiereReponseHeures").value(6));
    }

    /**
     * « Aucun appel passé » et « des appels passés, aucun rendez-vous » sont deux situations
     * opposées : rendre 0 dans le premier cas les confondrait au détriment de celui qui n'a rien
     * fait — et le front afficherait « 0 % » là où il faut « — ».
     */
    @Test
    void tauxSansDenominateur_valentNullEtNonZero() throws Exception {
        CfgTier commercial = newTier("KPI-NULL");
        AdmUser compte = newUser("kpi.null", commercial);
        newProspection(commercial, trancheActive, MAINTENANT);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                // Aucun appel passé ⇒ pas de taux de conversion d'appel.
                .andExpect(jsonPath("$.tauxAppelVersRdv").doesNotExist())
                .andExpect(jsonPath("$.panierMoyen").doesNotExist())
                .andExpect(jsonPath("$.delaiMedianPremiereReponseHeures").doesNotExist())
                // Une prospection assignée, donc le taux existe et vaut 0 — la nuance est là.
                .andExpect(jsonPath("$.tauxPriseEnCharge").value(0.0))
                .andExpect(jsonPath("$.tauxProspectionVersDossier").value(0.0));
    }

    // ------------------------------------------------------------------ période

    /**
     * {@code au} est inclusive jusqu'à la fin de la journée. La borner à minuit exclurait toute
     * l'activité du jour demandé, c'est-à-dire le cas le plus courant : « du 1er à aujourd'hui ».
     */
    @Test
    void interactionDuJourDeAu_estIncluse() throws Exception {
        CfgTier commercial = newTier("KPI-BORNE");
        AdmUser compte = newUser("kpi.borne", commercial);
        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT);

        LocalDateTime finDeJournee = LocalDate.now().atTime(23, 30);
        appel(compte, prospection, null, finDeJournee);

        mockMvc.perform(get("/api/kpi/commerciaux/moi")
                        .param("du", LocalDate.now().toString())
                        .param("au", LocalDate.now().toString())
                        .with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsPasses").value(1));
    }

    @Test
    void interactionHorsPeriode_nestPasComptee() throws Exception {
        CfgTier commercial = newTier("KPI-HORS");
        AdmUser compte = newUser("kpi.hors", commercial);
        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT.minusYears(2));

        appel(compte, prospection, null, MAINTENANT.minusYears(2));

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsPasses").value(0))
                .andExpect(jsonPath("$.prospectionsAssignees").value(0));
    }

    @Test
    void duPosterieureAAu_renvoie400() throws Exception {
        AdmUser compte = newUser("kpi.dates", newTier("KPI-DATES"));

        mockMvc.perform(get("/api/kpi/commerciaux/moi")
                        .param("du", "2026-09-08").param("au", "2026-09-01")
                        .with(connecte(compte, "Commercial")))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ visibilité

    /**
     * {@code /api/kpi} n'appartient pas aux collections scopées de {@code PerimetreInterceptor} :
     * si le service n'applique pas lui-même le filtre d'entité, rien ne le fait, et les KPI
     * exposent l'activité d'entités que tout le reste de l'application masque.
     */
    @Test
    void activiteSurUneEntiteInactive_nestCompteeNullePart() throws Exception {
        CfgTier commercial = newTier("KPI-ENTITE");
        AdmUser compte = newUser("kpi.entite", commercial);
        ComProspection masquee = newProspection(commercial, trancheInactive, MAINTENANT);

        appel(compte, masquee, resultatRdvPris);
        newDossier(commercial, trancheInactive, MAINTENANT, "900000");

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsPasses").value(0))
                .andExpect(jsonPath("$.rdvObtenus").value(0))
                .andExpect(jsonPath("$.prospectionsAssignees").value(0))
                .andExpect(jsonPath("$.dossiersCrees").value(0));
    }

    /** Un compte restreint ne voit que l'activité de son périmètre, y compris la sienne. */
    @Test
    void perimetreRestreint_masqueLActiviteHorsPerimetre() throws Exception {
        CfgProject autreProjetActif = newProjet("KPI-P-AUTRE", newSociete("KPI-ACTIVE-2", true));
        CfgTranche autreTranche = newTranche("KPI-TR-AUTRE", autreProjetActif);

        CfgTier commercial = newTier("KPI-PERIM");
        AdmUser compte = newUser("kpi.perim", commercial);
        affecter(compte, projetActif);

        ComProspection dansPerimetre = newProspection(commercial, trancheActive, MAINTENANT);
        ComProspection horsPerimetre = newProspection(commercial, autreTranche, MAINTENANT);
        appel(compte, dansPerimetre, null);
        appel(compte, horsPerimetre, null);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appelsPasses").value(1))
                .andExpect(jsonPath("$.prospectionsAssignees").value(1));
    }

    // ------------------------------------------------------------------ accès

    @Test
    void listeComplete_sansRoleAdministrateur_renvoie403() throws Exception {
        AdmUser compte = newUser("kpi.simple", newTier("KPI-SIMPLE"));

        mockMvc.perform(get("/api/kpi/commerciaux").with(connecte(compte, "Commercial")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listeComplete_administrateur_classeLesCommerciauxActifs() throws Exception {
        CfgTier vendeur = newTier("KPI-VENDEUR");
        AdmUser compteVendeur = newUser("kpi.vendeur", vendeur);
        CfgTier appelant = newTier("KPI-APPELANT");
        AdmUser compteAppelant = newUser("kpi.appelant", appelant);

        ComProspection p1 = newProspection(vendeur, trancheActive, MAINTENANT);
        ComProspection p2 = newProspection(appelant, trancheActive, MAINTENANT);
        appel(compteVendeur, p1, null);
        newDossier(vendeur, trancheActive, MAINTENANT, "500000");
        for (int i = 0; i < 5; i++) {
            appel(compteAppelant, p2, null);
        }

        AdmUser admin = newUser("kpi.admin", newTier("KPI-ADMIN"));
        donnerRole(admin, "Administrateur");

        // Celui qui a vendu passe devant celui qui a le plus appelé : c'est tout l'intérêt de
        // classer sur les dossiers d'abord.
        mockMvc.perform(get("/api/kpi/commerciaux").with(connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commercialId").value(vendeur.getId().toString()))
                .andExpect(jsonPath("$[?(@.commercialId=='" + appelant.getId() + "')].appelsPasses").value(5));
    }

    /**
     * Un commercial sans activité obtient 200 et des zéros, pas une erreur : un écran vide est une
     * information. Le 404 est réservé au compte sans employé, où il n'y a pas de commercial dont
     * parler.
     */
    @Test
    void moi_sansAucuneActivite_renvoie200AvecDesZeros() throws Exception {
        CfgTier commercial = newTier("KPI-VIDE");
        AdmUser compte = newUser("kpi.vide", commercial);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialId").value(commercial.getId().toString()))
                .andExpect(jsonPath("$.appelsPasses").value(0))
                .andExpect(jsonPath("$.dossiersCrees").value(0))
                .andExpect(jsonPath("$.montantTotal").value(0.00));
    }

    /**
     * Un compte sans employé (16 sur 75) a désormais sa propre ligne : ses actions n'étaient
     * comptées nulle part. Plus de 404 — voir {@code FEATURE-suivi-actions.md} §7.
     */
    @Test
    void moi_compteSansEmploye_aSaPropreActivite() throws Exception {
        AdmUser compte = newUser("kpi.sansemploye", null);
        ComProspection prospection = newProspection(newTier("KPI-AUTRE"), trancheActive, MAINTENANT);
        appel(compte, prospection, resultatRdvPris);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialId").doesNotExist())
                .andExpect(jsonPath("$.utilisateurId").value(compte.getId().toString()))
                .andExpect(jsonPath("$.commercialNom").value("kpi.sansemploye"))
                .andExpect(jsonPath("$.appelsPasses").value(1))
                .andExpect(jsonPath("$.rdvObtenus").value(1));
    }

    // ------------------------------------------------------------------ toutes les actions (§7)

    /**
     * L'activité compte toutes les actions, détaillées par type, et le taux de rendez-vous se lit
     * aussi sur l'ensemble de l'activité.
     */
    @Test
    void actionsParType_detailleToutesLesActions() throws Exception {
        CfgTier commercial = newTier("KPI-TYPES");
        AdmUser compte = newUser("kpi.types", commercial);
        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT);
        ComActionMessageType note = newType("Note");
        ComActionMessageType messageSortant = newType("Message sortant");

        appel(compte, prospection, resultatRdvPris);
        appel(compte, prospection, null);
        visite(compte, prospection);
        newInteraction(compte, prospection, note, null, MAINTENANT);
        newInteraction(compte, prospection, messageSortant, null, MAINTENANT);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interactionsTotal").value(5))
                .andExpect(jsonPath("$.actionsParType[0].type").value(InteractionKpi.TYPE_APPEL_SORTANT))
                .andExpect(jsonPath("$.actionsParType[0].nombre").value(2))
                .andExpect(jsonPath("$.actionsParType.length()").value(4))
                .andExpect(jsonPath("$.actionsParType[?(@.type=='Note')].nombre").value(org.hamcrest.Matchers.hasItem(1)))
                .andExpect(jsonPath("$.tauxAppelVersRdv").value(0.5))
                .andExpect(jsonPath("$.tauxActionVersRdv").value(0.2));
    }

    /**
     * Une action « Système » écrite sous le compte n'est pas celle de l'utilisateur : générée
     * pendant sa session, OMRAN l'affiche sous « Système ». Elle gonflait le total (212 des 228
     * interactions d'un compte réel sur 12 mois).
     */
    @Test
    void actionSysteme_nestComptee_pourPersonne() throws Exception {
        CfgTier commercial = newTier("KPI-SYS");
        AdmUser compte = newUser("kpi.sys", commercial);
        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT);
        ComActionMessageType note = newType("Note");

        appel(compte, prospection, null);
        ComActionMessage generee = newInteraction(compte, prospection, note, null, MAINTENANT);
        generee.setUserName("Système");
        messageRepository.save(generee);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(jsonPath("$.interactionsTotal").value(1))
                .andExpect(jsonPath("$.actionsParType.length()").value(1));
    }

    /**
     * Sans compte sur l'action, l'auteur est le compte <b>seul</b> à porter le nom écrit dessus —
     * le cas des actions sauvées par le correctif d'auteur. Un nom porté par deux comptes n'est
     * jamais deviné.
     */
    @Test
    void actionSansCompte_attribueeParLeNom_seulementSIlEstUnique() throws Exception {
        CfgTier commercial = newTier("KPI-NOM");
        AdmUser compte = newUser("kpi.nom", commercial);
        compte.setDescription("Nom Unique " + UUID.randomUUID());
        userRepository.save(compte);
        AdmUser homonyme1 = newUser("kpi.homo.a", null);
        AdmUser homonyme2 = newUser("kpi.homo.b", null);
        String partage = "Nom Partage " + UUID.randomUUID();
        homonyme1.setDescription(partage);
        homonyme2.setDescription(partage);
        userRepository.save(homonyme1);
        userRepository.save(homonyme2);
        ComProspection prospection = newProspection(commercial, trancheActive, MAINTENANT);

        ComActionMessage parNom = newInteraction(compte, prospection, typeAppelSortant, null, MAINTENANT);
        parNom.setAdmUser(null);
        parNom.setUserName(compte.getDescription());
        messageRepository.save(parNom);
        ComActionMessage ambigue = newInteraction(compte, prospection, typeAppelSortant, null, MAINTENANT);
        ambigue.setAdmUser(null);
        ambigue.setUserName(partage);
        messageRepository.save(ambigue);

        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(compte, "Commercial")))
                .andExpect(jsonPath("$.appelsPasses").value(1));
        mockMvc.perform(get("/api/kpi/commerciaux/moi").with(connecte(homonyme1, "Commercial")))
                .andExpect(jsonPath("$.appelsPasses").value(0));
    }

    // ------------------------------------------------------------------ fixtures

    private CfgCompany newSociete(String code, boolean active) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        societe.setDescription("Société " + code);
        societe.setIsActive(active);
        return companyRepository.save(societe);
    }

    private CfgProject newProjet(String code, CfgCompany societe) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        projet.setDescription("Projet " + code);
        projet.setCfgCompany(societe);
        return projectRepository.save(projet);
    }

    private CfgTranche newTranche(String code, CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    private CfgTier newTier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        tier.setDescription(code);
        return tierRepository.save(tier);
    }

    /** {@code employe} peut être {@code null} : c'est le compte non rattaché (16 sur 75 en base). */
    private AdmUser newUser(String code, CfgTier employe) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code + "." + UUID.randomUUID());
        user.setDescription(code);
        user.setDisabled(false);
        user.setCfgEmployee(employe);
        return userRepository.save(user);
    }

    private void affecter(AdmUser user, CfgProject projet) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(user);
        habilitation.setCfgProject(projet);
        habilitationRepository.save(habilitation);
    }

    private void donnerRole(AdmUser user, String code) {
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        roleRepository.save(role);

        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(UUID.randomUUID());
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        userRoleRepository.save(userRole);
    }

    /** {@code employeeId} du principal = ce que porte le JWT, et la clé de {@code /moi}. */
    private RequestPostProcessor connecte(AdmUser user, String... roles) {
        UUID employeId = user.getCfgEmployee() != null ? user.getCfgEmployee().getId() : null;
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getCode(), employeId, false, true, 0);
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private ComActionMessageType newType(String code) {
        ComActionMessageType type = new ComActionMessageType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription(code);
        return messageTypeRepository.save(type);
    }

    private ComActionMessageSubCategory newSousCategorie(String code) {
        ComActionMessageSubCategory sousCategorie = new ComActionMessageSubCategory();
        sousCategorie.setId(UUID.randomUUID());
        sousCategorie.setCode(code);
        sousCategorie.setDescription(code);
        return messageSubCategoryRepository.save(sousCategorie);
    }

    private ComProspection newProspection(CfgTier commercial, CfgTranche tranche, LocalDateTime date) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-KPI-" + UUID.randomUUID().toString().substring(0, 8));
        prospection.setCfgProspect(newTier("PRO-KPI"));
        prospection.setCfgCommercial(commercial);
        prospection.setStkHierarchy(tranche);
        prospection.setDocDate(date);
        return prospectionRepository.save(prospection);
    }

    private ComActionMessage appel(AdmUser auteur, ComProspection prospection,
                                   ComActionMessageSubCategory resultat) {
        return appel(auteur, prospection, resultat, MAINTENANT);
    }

    private ComActionMessage appel(AdmUser auteur, ComProspection prospection,
                                   ComActionMessageSubCategory resultat, LocalDateTime date) {
        return newInteraction(auteur, prospection, typeAppelSortant, resultat, date);
    }

    private ComActionMessage visite(AdmUser auteur, ComProspection prospection) {
        return newInteraction(auteur, prospection, typeVisite, null, MAINTENANT);
    }

    private ComActionMessage newInteraction(AdmUser auteur, ComProspection prospection,
                                            ComActionMessageType type,
                                            ComActionMessageSubCategory resultat, LocalDateTime date) {
        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setAdmUser(auteur);
        message.setComProspection(prospection);
        message.setComActionMessageType(type);
        message.setComActionMessageSubCategory(resultat);
        message.setDocDate(date);
        message.setUserCreate(auteur.getCode());
        return messageRepository.save(message);
    }

    private ComFolder newDossier(CfgTier commercial, CfgTranche tranche, LocalDateTime date, String montant) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-KPI-" + UUID.randomUUID().toString().substring(0, 8));
        folder.setStkVocation(vocation);
        folder.setCfgCommercial(commercial);
        folder.setDocDate(date);
        folder.setTotalToPay(new BigDecimal(montant));
        folder = folderRepository.save(folder);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-" + folder.getCode());
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setComFolderId(folder.getId());
        article.setStkKind(fixtureReferentielStk.natureParDefaut());

        // Reprendre l'instance renvoyée par save() : l'id étant assigné, Spring Data passe par
        // merge() et rend une autre instance gérée.
        folder.setStkItem(stkItemRepository.save(article));
        return folderRepository.save(folder);
    }
}
