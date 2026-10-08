package com.tsix.immocrm.controller.actionsrecentes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue de FEATURE-actions-recentes.md : cas limites de R6/R7 et R9 non couverts par l'auteur. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsRecentesRevueTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2032, 5, 1, 12, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComActionMessageRepository actionMessageRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private StrHistoryRepository historyRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private AdmUser moi;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-ARV-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-ARV-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-ARV-TEST");
        stkVocationRepository.save(vocation);

        CfgProject projetA = newProjet("ARV-A");
        CfgProject projetB = newProjet("ARV-B");
        trancheA = newTranche("TR-ARV-A", projetA);
        trancheB = newTranche("TR-ARV-B", projetB);

        moi = new AdmUser();
        moi.setId(UUID.randomUUID());
        moi.setCode("arv.moi." + UUID.randomUUID());
        moi.setDescription("Moi Revue");
        moi.setDisabled(false);
        userRepository.save(moi);

        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(moi);
        habilitation.setCfgProject(projetA);
        habilitationRepository.save(habilitation);
    }

    private CfgProject newProjet(String code) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        return projectRepository.save(projet);
    }

    private CfgTranche newTranche(String code, CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    private RequestPostProcessor connecte(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private ComProspection newProspection(String code, CfgTranche tranche) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-" + code);
        prospect.setLastName("Dupont " + code);
        tierRepository.save(prospect);

        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setCfgProspect(prospect);
        p.setStkHierarchy(tranche);
        return prospectionRepository.save(p);
    }

    private ComFolder newDossier(String code, CfgTranche tranche) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        folder.setTotalToPay(new BigDecimal("100000"));
        folderRepository.save(folder);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-" + code);
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setComFolderId(folder.getId());
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        folder.setStkItem(article);
        return folderRepository.save(folder);
    }

    private StrHistory historiqueProspection(ComProspection p, LocalDateTime date) {
        StrHistory h = new StrHistory();
        h.setId(UUID.randomUUID());
        h.setDocDate(date);
        h.setObjectId(p.getId());
        h.setDescription("Désarchivée");
        h.setAdmUserId(moi.getId());
        h.setAdmUserDescription(moi.getDescription());
        return historyRepository.save(h);
    }

    private ComActionMessage interaction(ComProspection p, ComFolder f, LocalDateTime date) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(date);
        m.setUserCreateDateTime(date);
        m.setComProspection(p);
        m.setComFolder(f);
        m.setAdmUser(moi);
        m.setAlertMessage("Note revue");
        return actionMessageRepository.save(m);
    }

    /**
     * R6/R7 : une interaction rattachée à une prospection du périmètre ET à un dossier hors périmètre
     * passe filtreInteractions() (projet = prospection d'abord) mais est servie avec la cible du dossier
     * (dossier d'abord) : id et code d'un dossier hors périmètre exposés. 3 lignes dans yesRems.
     */
    @Test
    void interactionProspectionDansPerimetre_dossierHorsPerimetre_nExposePasLeDossier() throws Exception {
        ComProspection pA = newProspection("PSN-ARV-MIX", trancheA);
        ComFolder fB = newDossier("DOS-ARV-HORS", trancheB);
        ComActionMessage m = interaction(pA, fB, T0);

        for (String portee : List.of("MOI", "EQUIPE")) {
            String json = mockMvc.perform(get("/api/actions-recentes").param("portee", portee)
                            .param("limite", "100").with(connecte(moi)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> cibles = JsonPath.read(json, "$[*].cibleId");
            List<String> libelles = JsonPath.read(json, "$[*].cibleLibelle");
            assertThat(cibles).as("portée %s : cibleId d'un dossier hors périmètre (interaction %s)", portee, m.getId())
                    .doesNotContain(fB.getId().toString());
            assertThat(libelles).as("portée %s : code d'un dossier hors périmètre", portee)
                    .doesNotContain("DOS-ARV-HORS");
        }
    }

    /** R9 : deux actions de même date à cheval sur deux pages : « Voir plus » ne doit rien perdre. */
    @Test
    void avant_actionsDeMemeDate_aChevalSurDeuxPages_aucuneNestPerdue() throws Exception {
        ComProspection p = newProspection("PSN-ARV-EGAL", trancheA);
        List<String> attendus = new ArrayList<>();
        attendus.add(historiqueProspection(p, T0).getId().toString());
        attendus.add(historiqueProspection(p, T0).getId().toString());
        attendus.add(interaction(p, null, T0).getId().toString());
        attendus.add(historiqueProspection(p, T0.minusMinutes(1)).getId().toString());

        List<String> servis = new ArrayList<>();
        String avant = null;
        String avantId = null;
        for (int page = 0; page < 6; page++) {
            var requete = get("/api/actions-recentes").param("limite", "2").with(connecte(moi));
            if (avant != null) {
                // Curseur composite de §10.2 : la date seule ne peut pas départager des dates égales.
                requete = requete.param("avant", avant).param("avantId", avantId);
            }
            String json = mockMvc.perform(requete).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> ids = JsonPath.read(json, "$[*].id");
            if (ids.isEmpty()) {
                break;
            }
            servis.addAll(ids);
            List<String> dates = JsonPath.read(json, "$[*].date");
            avant = dates.get(dates.size() - 1);
            avantId = ids.get(ids.size() - 1);
        }

        assertThat(servis).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(attendus);
    }

    // ---------------------------------------------------------------- 2e passe (§10.2)

    private List<String> paginer(String portee, int limite) throws Exception {
        List<String> servis = new ArrayList<>();
        String avant = null;
        String avantId = null;
        for (int page = 0; page < 40; page++) {
            var requete = get("/api/actions-recentes").param("portee", portee)
                    .param("limite", String.valueOf(limite)).with(connecte(moi));
            if (avant != null) {
                requete = requete.param("avant", avant).param("avantId", avantId);
            }
            String json = mockMvc.perform(requete).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> ids = JsonPath.read(json, "$[*].id");
            if (ids.isEmpty()) {
                break;
            }
            servis.addAll(ids);
            List<String> dates = JsonPath.read(json, "$[*].date");
            avant = dates.get(dates.size() - 1);
            avantId = ids.get(ids.size() - 1);
        }
        return servis;
    }

    private StrHistory historiqueProspection(UUID id, ComProspection p, LocalDateTime date) {
        StrHistory h = new StrHistory();
        h.setId(id);
        h.setDocDate(date);
        h.setObjectId(p.getId());
        h.setDescription("Désarchivée");
        h.setAdmUserId(moi.getId());
        h.setAdmUserDescription(moi.getDescription());
        return historyRepository.save(h);
    }

    private ComActionMessage interaction(UUID id, ComProspection p, LocalDateTime date) {
        ComActionMessage m = new ComActionMessage();
        m.setId(id);
        m.setDocDate(date);
        m.setUserCreateDateTime(date);
        m.setComProspection(p);
        m.setAdmUser(moi);
        m.setAlertMessage("Note revue 2");
        return actionMessageRepository.save(m);
    }

    /**
     * §10.2 : ids choisis pour que l'ordre uniqueidentifier de SQL Server (6 derniers octets d'abord),
     * UUID.compareTo (signé) et l'ordre texte divergent ; deux sources, même date, pages de 1 et 2.
     */
    @Test
    void curseurComposite_idsQuiDivergentSelonLOrdre_niTrouNiDoublon() throws Exception {
        ComProspection p = newProspection("PSN-ARV-UUID", trancheA);
        String[] idsHisto = {
                "00000000-0000-0000-0000-FFFFFFFFFFFF", "FFFFFFFF-0000-0000-0000-000000000000",
                "7FFFFFFF-0000-0000-0000-000000000001", "80000000-0000-0000-0000-000000000002",
                "0000000A-0000-0000-0000-000000000009"};
        String[] idsInter = {
                "00000001-0000-0000-0000-FFFFFFFFFFFE", "FFFFFFFE-0000-0000-0000-000000000003",
                "80000001-0000-0000-FFFF-000000000000", "00000009-0000-0000-0000-00000000000A",
                "A0000000-0000-0000-0000-000000000000"};
        List<String> attendus = new ArrayList<>();
        for (String id : idsHisto) {
            attendus.add(historiqueProspection(UUID.fromString(id), p, T0).getId().toString());
        }
        for (String id : idsInter) {
            attendus.add(interaction(UUID.fromString(id), p, T0).getId().toString());
        }
        // Même pas datetime (1/300 s) non entier en ms : le piège du ±1 ms (relu .003, stocké .00333).
        LocalDateTime tick = T0.minusHours(1).plusNanos(3_000_000);
        attendus.add(historiqueProspection(UUID.fromString("00000000-0000-0000-0000-0000000000F1"), p, tick).getId().toString());
        attendus.add(historiqueProspection(UUID.fromString("F0000000-0000-0000-0000-000000000000"), p, tick).getId().toString());

        List<String> ordreTexteDesc = attendus.stream().sorted((a, b) -> {
            boolean aTick = a.toUpperCase().endsWith("0000000000F1") || a.toUpperCase().startsWith("F0000000");
            boolean bTick = b.toUpperCase().endsWith("0000000000F1") || b.toUpperCase().startsWith("F0000000");
            if (aTick != bTick) {
                return aTick ? 1 : -1;
            }
            return b.toUpperCase().compareTo(a.toUpperCase());
        }).toList();

        for (String portee : List.of("MOI", "EQUIPE")) {
            for (int limite : new int[]{1, 2, 3}) {
                List<String> servis = paginer(portee, limite).stream()
                        .filter(id -> attendus.stream().anyMatch(id::equalsIgnoreCase)).toList();
                assertThat(servis).as("portée %s, limite %d", portee, limite)
                        .doesNotHaveDuplicates()
                        .containsExactlyElementsOf(ordreTexteDesc);
            }
        }
    }

    @Test
    void avantIdSansAvant_400() throws Exception {
        mockMvc.perform(get("/api/actions-recentes").param("avantId", UUID.randomUUID().toString())
                        .with(connecte(moi)))
                .andExpect(status().isBadRequest());
    }

    /** Sans avantId, avant reste un curseur strict : les actions de même date sont exclues, les antérieures servies. */
    @Test
    void avantSansAvantId_strictementAnterieur() throws Exception {
        ComProspection p = newProspection("PSN-ARV-SEUL", trancheA);
        String memeDate = historiqueProspection(p, T0).getId().toString();
        String anterieure = historiqueProspection(p, T0.minusSeconds(1)).getId().toString();
        String json = mockMvc.perform(get("/api/actions-recentes").param("avant", T0.toString())
                        .param("limite", "100").with(connecte(moi)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(json, "$[*].id");
        assertThat(ids).contains(anterieure).doesNotContain(memeDate);
    }
}
