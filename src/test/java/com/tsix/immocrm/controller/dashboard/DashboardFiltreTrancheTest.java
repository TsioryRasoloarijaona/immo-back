package com.tsix.immocrm.controller.dashboard;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import net.minidev.json.JSONArray;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtre par tranche du tableau de bord — {@code instructions/FEATURE-dashboard-filtre-tranche.md}.
 *
 * <p>Jeu de données, bâti par test et annulé par le {@code @Transactional} de la classe :
 * <pre>
 *   tranche T (société active)        tranche U (société active)       tranche X (société inactive)
 *   ─ dossier A  RESERVATION 500 000   ─ dossier C  RESERVATION 300 000  ─ dossier D RESERVATION 700 000
 *     article principal dans T            article principal dans U          article principal dans X
 *   ─ dossier B  ANNULATION 999 999 : article principal dans U, article annexe (lien inverse) dans T
 *     → B appartient à T ET à U, mais n'entre dans aucun pipeline (statut de sortie)
 *   ─ prospections : p1 T ouverte, p2 T archivée, p3 U ouverte — relance du jour sur p1 et p3
 * </pre>
 * Les tranches étant neuves, les assertions sous filtre sont <b>exactes</b>.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardFiltreTrancheTest {

    /** Année sans aucune autre donnée : les montants de la synthèse filtrée sont exacts. */
    private static final int ANNEE = 2077;

    private static final List<String> ENDPOINTS = List.of(
            "/api/dashboard", "/api/dashboard/relances", "/api/dashboard/annuel", "/api/clients");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComActionMessageRepository messageRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComSettlementCategoryRepository categoryRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private StrStatusRepository strStatusRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;
    private StrStatus reservation;
    private StrStatus annulation;

    private CfgTranche trancheT;
    private CfgTranche trancheU;
    private CfgTranche trancheX;

    private ComFolder dossierA;
    private ComFolder dossierB;
    private ComFolder dossierC;
    private ComProspection p1;
    private ComProspection p3;

    private RequestPostProcessor admin;
    private RequestPostProcessor commercial;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-FT-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-FT-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-FT-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        // Sorting 0 : sans statut CONCRETISATION en base, le seuil de la synthèse vaut 0.
        reservation = newStatut(StatutDossier.RESERVATION.getCode());
        annulation = newStatut(StatutDossier.ANNULATION.getCode());

        CfgCompany active = newSociete(true);
        trancheT = newTranche("FT-T", newProjet("FT-PT", active));
        trancheU = newTranche("FT-U", newProjet("FT-PU", active));
        trancheX = newTranche("FT-X", newProjet("FT-PX", newSociete(false)));

        dossierA = newDossier(reservation, "500000", trancheT, LocalDateTime.of(ANNEE, 3, 1, 0, 0));
        dossierB = newDossier(annulation, "999999", trancheU, null);
        newArticle(trancheT, dossierB); // annexe de B dans T, par le seul lien inverse
        dossierC = newDossier(reservation, "300000", trancheU, LocalDateTime.of(ANNEE, 4, 1, 0, 0));
        newDossier(reservation, "700000", trancheX, LocalDateTime.of(ANNEE, 3, 1, 0, 0));

        p1 = newProspection(trancheT, false);
        newProspection(trancheT, true);
        p3 = newProspection(trancheU, false);
        newRelanceDuJour(p1);
        newRelanceDuJour(p3);

        admin = connecte(newUser("ft.admin"), "Administrateur");
        commercial = connecte(newUser("ft.com"), "Commercial");
    }

    // ------------------------------------------------------------------ R1 — autorisation

    @Test
    void r1_commercialAvecTranche_403SurLesQuatreEndpoints() throws Exception {
        for (String url : ENDPOINTS) {
            mockMvc.perform(get(url).param("trancheId", trancheT.getId().toString()).with(commercial))
                    .andExpect(status().isForbidden());
        }
    }

    /** Un appelant sans aucun rôle — l'absence de « Commercial » ne doit rien ouvrir. */
    @Test
    void r1_appelantSansRoleAvecTranche_403() throws Exception {
        RequestPostProcessor sansRole = connecte(newUser("ft.nobody"));
        for (String url : ENDPOINTS) {
            mockMvc.perform(get(url).param("trancheId", trancheT.getId().toString()).with(sansRole))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void r1_commercialSansTranche_200() throws Exception {
        for (String url : ENDPOINTS) {
            mockMvc.perform(get(url).with(commercial)).andExpect(status().isOk());
        }
    }

    @Test
    void r1_adminAvecTranche_200() throws Exception {
        for (String url : ENDPOINTS) {
            mockMvc.perform(get(url).param("trancheId", trancheT.getId().toString()).with(admin))
                    .andExpect(status().isOk());
        }
    }

    // ------------------------------------------------------------------ R3 — liste = tableau de bord

    @Test
    void r3_compteursDuTableauDeBord_egauxAuxTotauxDesListesFiltrees() throws Exception {
        String t = trancheT.getId().toString();
        String dashboard = getJson("/api/dashboard?trancheId=" + t);

        long sommeParEtape = sommeDossiersParEtape(dashboard);
        long totalDossiers = totalElements("/api/dossiers?trancheId=" + t);
        assertThat(sommeParEtape).isEqualTo(totalDossiers).isEqualTo(2); // A et B

        long ouvertes = JsonPath.parse(dashboard).read("$.prospectionsOuvertes", Long.class);
        assertThat(ouvertes)
                .isEqualTo(totalElements("/api/prospections?ouvertes=true&trancheId=" + t))
                .isEqualTo(1);
        long total = JsonPath.parse(dashboard).read("$.prospectionsTotal", Long.class);
        assertThat(total)
                .isEqualTo(totalElements("/api/prospections?trancheId=" + t))
                .isEqualTo(2);
    }

    /** Un dossier à cheval sur deux tranches compte dans chacune — sémantique du filtre liste. */
    @Test
    void r3_dossierACheval_compteDansSesDeuxTranches() throws Exception {
        String dashboardU = getJson("/api/dashboard?trancheId=" + trancheU.getId());
        assertThat(sommeDossiersParEtape(dashboardU))
                .isEqualTo(totalElements("/api/dossiers?trancheId=" + trancheU.getId()))
                .isEqualTo(2); // B et C
    }

    // ------------------------------------------------------------------ R4 — ne fait que restreindre

    @Test
    void r4_trancheDUneEntiteInactive_resteInvisible() throws Exception {
        String dashboard = getJson("/api/dashboard?trancheId=" + trancheX.getId());
        assertThat(new BigDecimal(JsonPath.parse(dashboard).read("$.valeurPipeline").toString()))
                .isEqualByComparingTo("0");
        assertThat(sommeDossiersParEtape(dashboard)).isZero();
    }

    // ------------------------------------------------------------------ R5 — inconnue / invalide

    @Test
    void r5_trancheInconnue_200AvecZerosEtListesVides() throws Exception {
        String inconnue = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/dashboard").param("trancheId", inconnue).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valeurPipeline").value(0))
                .andExpect(jsonPath("$.panierMoyen").value(0))
                .andExpect(jsonPath("$.dossiersParEtape").isEmpty())
                .andExpect(jsonPath("$.relancesEnRetard").isEmpty())
                .andExpect(jsonPath("$.prospectionsOuvertes").value(0))
                .andExpect(jsonPath("$.prospectionsTotal").value(0));
        mockMvc.perform(get("/api/dashboard/relances").param("trancheId", inconnue)
                        .param("portee", "EQUIPE").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/dashboard/annuel").param("trancheId", inconnue)
                        .param("annee", String.valueOf(ANNEE)).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echeancier.length()").value(12))
                .andExpect(jsonPath("$.ventesConcretisees.nombreDossiers").value(0))
                .andExpect(jsonPath("$.ventesConcretisees.dossiersSansDateAvance").value(0))
                .andExpect(jsonPath("$.encaissements.nombreLignes").value(0));
        mockMvc.perform(get("/api/clients").param("trancheId", inconnue).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void r5_trancheNonParsable_400() throws Exception {
        for (String url : ENDPOINTS) {
            mockMvc.perform(get(url).param("trancheId", "abc").with(admin))
                    .andExpect(status().isBadRequest());
        }
    }

    // ------------------------------------------------------------------ R6 — argent

    @Test
    void r6_valeurPipeline_egaleLaSommeDesDossiersActifsDeLaListe_etBorneeParLeTotal() throws Exception {
        String t = trancheT.getId().toString();
        BigDecimal pipelineT = new BigDecimal(
                JsonPath.parse(getJson("/api/dashboard?trancheId=" + t)).read("$.valeurPipeline").toString());
        BigDecimal pipelineTotal = new BigDecimal(
                JsonPath.parse(getJson("/api/dashboard")).read("$.valeurPipeline").toString());

        List<Map<String, Object>> dossiers = JsonPath.parse(getJson("/api/dossiers?size=100&trancheId=" + t))
                .read("$.content");
        BigDecimal sommeActifs = dossiers.stream()
                .filter(d -> !StatutDossier.fromCode((String) d.get("statut")).isSortie())
                .map(d -> new BigDecimal(d.get("totalAPayer").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(pipelineT).isEqualByComparingTo(sommeActifs).isEqualByComparingTo("500000");
        assertThat(pipelineT).isLessThanOrEqualTo(pipelineTotal);
        // B (annulé) appartient à T mais n'entre pas dans le pipeline ; panier = 500 000 / 1 actif.
        assertThat(new BigDecimal(JsonPath.parse(getJson("/api/dashboard?trancheId=" + t))
                .read("$.panierMoyen").toString())).isEqualByComparingTo("500000");
    }

    @Test
    void r6_syntheseAnnuelle_neSommeQueLesDossiersDeLaTranche() throws Exception {
        ComSettlementCategory avance = newCategorieAvance();
        ComDocument echeanceA = newEcheance(dossierA, avance, LocalDateTime.of(ANNEE, 5, 10, 0, 0), "100000");
        newEcheance(dossierC, avance, LocalDateTime.of(ANNEE, 5, 20, 0, 0), "70000");
        newReglement(echeanceA, LocalDateTime.of(ANNEE, 6, 1, 0, 0), "40000");

        String t = trancheT.getId().toString();
        mockMvc.perform(get("/api/dashboard/annuel").param("annee", String.valueOf(ANNEE))
                        .param("trancheId", t).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echeancier[4].montant").value(100000.0))
                .andExpect(jsonPath("$.echeancier[4].lignes").value(1))
                .andExpect(jsonPath("$.ventesConcretisees.nombreDossiers").value(1))
                .andExpect(jsonPath("$.ventesConcretisees.prixTotal").value(500000.0))
                .andExpect(jsonPath("$.ventesConcretisees.montantRegle").value(40000.0))
                // B (dans T, sans date d'avance) — et lui seul.
                .andExpect(jsonPath("$.ventesConcretisees.dossiersSansDateAvance").value(1))
                .andExpect(jsonPath("$.encaissements.nombreLignes").value(1))
                .andExpect(jsonPath("$.encaissements.montantTotal").value(40000.0));

        mockMvc.perform(get("/api/dashboard/annuel").param("annee", String.valueOf(ANNEE))
                        .param("trancheId", trancheU.getId().toString()).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echeancier[4].montant").value(70000.0))
                .andExpect(jsonPath("$.ventesConcretisees.nombreDossiers").value(1))
                .andExpect(jsonPath("$.ventesConcretisees.prixTotal").value(300000.0))
                .andExpect(jsonPath("$.encaissements.nombreLignes").value(0));

        // Sans tranche : les deux tranches actives, X (entité inactive) exclue.
        mockMvc.perform(get("/api/dashboard/annuel").param("annee", String.valueOf(ANNEE)).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echeancier[4].montant").value(170000.0))
                .andExpect(jsonPath("$.ventesConcretisees.nombreDossiers").value(2))
                .andExpect(jsonPath("$.ventesConcretisees.prixTotal").value(800000.0));
    }

    // ------------------------------------------------------------------ relances

    @Test
    void relances_neServentQueLesProspectionsDeLaTranche() throws Exception {
        String json = mockMvc.perform(get("/api/dashboard/relances")
                        .param("portee", "EQUIPE").param("trancheId", trancheT.getId().toString()).with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> prospections = JsonPath.parse(json).read("$.content[*].prospectionId");
        assertThat(prospections).containsExactly(p1.getId().toString());

        String sansFiltre = mockMvc.perform(get("/api/dashboard/relances").param("portee", "EQUIPE").with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> toutes = JsonPath.parse(sansFiltre).read("$.content[*].prospectionId");
        assertThat(toutes).contains(p1.getId().toString(), p3.getId().toString());
    }

    // ------------------------------------------------------------------ clients

    @Test
    void clients_neRenvoientQueCeuxAyantUnDossierDansLaTranche() throws Exception {
        String json = mockMvc.perform(get("/api/clients").param("size", "100")
                        .param("trancheId", trancheT.getId().toString()).with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Set<String> ids = new HashSet<>(JsonPath.parse(json).<List<String>>read("$.content[*].id"));
        assertThat(ids).containsExactlyInAnyOrder(
                dossierA.getCfgCustomer().getId().toString(),
                dossierB.getCfgCustomer().getId().toString()); // B par son seul article annexe
        assertThat(ids).doesNotContain(dossierC.getCfgCustomer().getId().toString());
    }

    // ------------------------------------------------------------------ outils

    private String getJson(String url) throws Exception {
        return mockMvc.perform(get(url).with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long totalElements(String url) throws Exception {
        return JsonPath.parse(getJson(url)).read("$.totalElements", Long.class);
    }

    private static long sommeDossiersParEtape(String dashboardJson) {
        JSONArray nombres = JsonPath.parse(dashboardJson).read("$.dossiersParEtape[*].nombre");
        return nombres.stream().mapToLong(n -> ((Number) n).longValue()).sum();
    }

    private RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private AdmUser newUser(String code) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code + "." + UUID.randomUUID());
        user.setDescription(code);
        user.setDisabled(false);
        return userRepository.save(user);
    }

    private StrStatus newStatut(String code) {
        StrStatus statut = new StrStatus();
        statut.setId(UUID.randomUUID());
        statut.setCode(code);
        statut.setSorting(0);
        statut.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        return strStatusRepository.save(statut);
    }

    private CfgCompany newSociete(boolean active) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("FT-SOC-" + UUID.randomUUID().toString().substring(0, 8));
        societe.setDescription("Société FT");
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
        tier.setSocialReason(code);
        return tierRepository.save(tier);
    }

    /** Dossier dont l'article principal est dans {@code tranche} (et y est aussi lié en retour). */
    private ComFolder newDossier(StrStatus statut, String montant, CfgTranche tranche, LocalDateTime dateAvance) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-FT-" + UUID.randomUUID().toString().substring(0, 8));
        folder.setStkVocation(vocation);
        folder.setStrStatus(statut);
        folder.setCfgCustomer(newTier("CLI-FT"));
        folder.setDocDate(LocalDateTime.now());
        folder.setTotalToPay(new BigDecimal(montant));
        folder.setPrice(new BigDecimal(montant));
        folder.setAdvanceDate(dateAvance);
        folder = folderRepository.save(folder);
        folder.setStkItem(newArticle(tranche, folder));
        return folderRepository.save(folder);
    }

    private StkItem newArticle(CfgTranche tranche, ComFolder folder) {
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-FT-" + UUID.randomUUID().toString().substring(0, 8));
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setComFolderId(folder.getId());
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        return stkItemRepository.save(article);
    }

    private ComProspection newProspection(CfgTranche tranche, boolean archivee) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-FT-" + UUID.randomUUID().toString().substring(0, 8));
        prospection.setCfgProspect(newTier("PRO-FT"));
        prospection.setStkHierarchy(tranche);
        prospection.setIsClosed(archivee);
        prospection.setDocDate(LocalDateTime.now());
        return prospectionRepository.save(prospection);
    }

    private void newRelanceDuJour(ComProspection prospection) {
        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setAlertValidityDate(LocalDate.now().atStartOfDay());
        message.setWithoutRevival(false);
        message.setDocDate(LocalDateTime.now());
        messageRepository.save(message);
    }

    private ComSettlementCategory newCategorieAvance() {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode("FT-CAT-AV-" + UUID.randomUUID());
        c.setIsSaleAdvance(true);
        c.setIsSaleExpense(false);
        c.setIsCredit(true);
        return categoryRepository.save(c);
    }

    private ComDocument newEcheance(ComFolder dossier, ComSettlementCategory categorie,
                                    LocalDateTime date, String montant) {
        ComDocument d = new ComDocument();
        d.setId(UUID.randomUUID());
        d.setComFolder(dossier);
        d.setComSettlementCategory(categorie);
        d.setDocDate(date);
        d.setAmount(new BigDecimal(montant));
        return documentRepository.save(d);
    }

    private void newReglement(ComDocument echeance, LocalDateTime date, String montant) {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("FT-TYP-" + UUID.randomUUID());
        type.setIsNotContabilized(false);
        type = settlementTypeRepository.save(type);

        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("FT-REG-" + UUID.randomUUID());
        s.setStlSettlementType(type);
        s.setTier(newTier("TIER-REG-FT"));
        s.setIsCredit(true);
        s.setEcheanceDate(date);
        s.setSettlementDate(date);
        s.setAmount(new BigDecimal(montant));
        s = settlementRepository.save(s);

        StlSettlementLine l = new StlSettlementLine();
        l.setId(UUID.randomUUID());
        l.setStlSettlement(s);
        l.setComDocument(echeance);
        l.setAmount(new BigDecimal(montant));
        settlementLineRepository.save(l);
    }
}
