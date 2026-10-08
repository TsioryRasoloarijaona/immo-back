package com.tsix.immocrm.controller.actionsrecentes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Partie B de FEATURE-actions-recentes.md : {@code GET /api/actions-recentes} (R4–R9). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsRecentesControllerTest {

    // Dans le futur : ces lignes passent avant toute donnée déjà présente dans la base de test.
    private static final LocalDateTime T0 = LocalDateTime.of(2031, 3, 1, 12, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private AdmRoleRepository roleRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComActionMessageRepository actionMessageRepository;
    @Autowired private ComActionMessageTypeRepository actionMessageTypeRepository;
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
    private AdmUser collegue;
    private CfgProject projetA;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-AR-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-AR-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-AR-TEST");
        stkVocationRepository.save(vocation);

        projetA = newProjet("AR-A");
        CfgProject projetB = newProjet("AR-B");
        trancheA = newTranche("TR-AR-A", projetA);
        trancheB = newTranche("TR-AR-B", projetB);

        moi = newUser("ar.moi.", "Moi Commercial");
        collegue = newUser("ar.collegue.", "Collègue Commercial");
        affecter(moi, projetA);
        affecter(collegue, projetA);
    }

    // ------------------------------------------------------------------ fixtures

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

    private AdmUser newUser(String prefixe, String nom) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(prefixe + UUID.randomUUID());
        user.setDescription(nom);
        user.setDisabled(false);
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

    private StrHistory historiqueProspection(ComProspection p, AdmUser auteur, LocalDateTime date, String description) {
        return historique(p.getId(), null, auteur, date, description);
    }

    private StrHistory historiqueDossier(ComFolder f, AdmUser auteur, LocalDateTime date) {
        return historique(f.getId(), StatutDossier.CONFIG_ENTITY_ID, auteur, date, "Passage en réservation");
    }

    private StrHistory historique(UUID objectId, UUID configEntityId, AdmUser auteur, LocalDateTime date,
                                  String description) {
        StrHistory h = new StrHistory();
        h.setId(UUID.randomUUID());
        h.setDocDate(date);
        h.setObjectId(objectId);
        h.setStrConfigEntityId(configEntityId);
        h.setDescription(description);
        if (auteur != null) {
            h.setAdmUserId(auteur.getId());
            h.setAdmUserDescription(auteur.getDescription());
        }
        return historyRepository.save(h);
    }

    private ComActionMessage interaction(ComProspection p, ComFolder f, AdmUser auteur, LocalDateTime date,
                                         String note) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(date.minusDays(3));
        m.setUserCreateDateTime(date);
        m.setComProspection(p);
        m.setComFolder(f);
        m.setAdmUser(auteur);
        m.setAlertMessage(note);
        return actionMessageRepository.save(m);
    }

    private static String present(UUID id) {
        return "$[?(@.id=='" + id + "')]";
    }

    // ------------------------------------------------------------------ R5 — MOI

    @Test
    void moi_neRenvoieQueMesLignes_trieesDuPlusRecent() throws Exception {
        ComProspection p = newProspection("PSN-AR-MOI", trancheA);
        ComFolder f = newDossier("DOS-AR-MOI", trancheA);
        StrHistory mienne = historiqueProspection(p, moi, T0.plusMinutes(3), "Étape changée : — → A");
        StrHistory duCollegue = historiqueProspection(p, collegue, T0.plusMinutes(2), "Désarchivée");
        StrHistory sansAuteur = historiqueProspection(p, null, T0.plusMinutes(1), "Pré-prospection créée : X");
        ComActionMessage note = interaction(p, null, moi, T0.plusMinutes(4), "Rappeler lundi");
        StrHistory statut = historiqueDossier(f, moi, T0.plusMinutes(5));

        mockMvc.perform(get("/api/actions-recentes").with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(statut.getId().toString()))
                .andExpect(jsonPath("$[0].type").value("DOSSIER_STATUT"))
                .andExpect(jsonPath("$[0].cibleType").value("DOSSIER"))
                .andExpect(jsonPath("$[0].cibleId").value(f.getId().toString()))
                .andExpect(jsonPath("$[0].cibleLibelle").value("DOS-AR-MOI"))
                .andExpect(jsonPath("$[1].id").value(note.getId().toString()))
                .andExpect(jsonPath("$[1].type").value("INTERACTION"))
                .andExpect(jsonPath("$[1].date").value(org.hamcrest.Matchers.startsWith("2031-03-01T12:04")))
                .andExpect(jsonPath("$[1].libelle").value("Rappeler lundi"))
                .andExpect(jsonPath("$[1].auteurNom").value("Moi Commercial"))
                .andExpect(jsonPath("$[2].id").value(mienne.getId().toString()))
                .andExpect(jsonPath("$[2].type").value("ETAPE_CHANGEE"))
                .andExpect(jsonPath("$[2].cibleType").value("PROSPECTION"))
                .andExpect(jsonPath("$[2].cibleLibelle").value("Dupont PSN-AR-MOI"))
                .andExpect(jsonPath("$[2].prospectId").value(p.getCfgProspect().getId().toString()))
                .andExpect(jsonPath("$[2].auteurId").value(moi.getId().toString()))
                .andExpect(jsonPath(present(duCollegue.getId())).doesNotExist())
                .andExpect(jsonPath(present(sansAuteur.getId())).doesNotExist());
    }

    @Test
    void equipe_contientLesLignesDesAutresEtSansAuteur() throws Exception {
        ComProspection p = newProspection("PSN-AR-EQ", trancheA);
        StrHistory duCollegue = historiqueProspection(p, collegue, T0.plusMinutes(2), "Désarchivée");
        StrHistory sansAuteur = historiqueProspection(p, null, T0.plusMinutes(1), "Saisie inconnue");

        mockMvc.perform(get("/api/actions-recentes").param("portee", "EQUIPE").with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(present(duCollegue.getId()) + ".auteurNom").value("Collègue Commercial"))
                .andExpect(jsonPath(present(sansAuteur.getId()) + ".type").value("AUTRE"))
                .andExpect(jsonPath(present(sansAuteur.getId()) + ".auteurId").value(org.hamcrest.Matchers.contains((Object) null)));
    }

    // ------------------------------------------------------------------ R6/R7 — refus

    @Test
    void horsPerimetre_absentDeEquipeEtDeMoi_pourLesTroisSources() throws Exception {
        ComProspection pB = newProspection("PSN-AR-HORS", trancheB);
        ComFolder fB = newDossier("DOS-AR-HORS", trancheB);
        StrHistory hProspection = historiqueProspection(pB, moi, T0.plusMinutes(1), "Désarchivée");
        StrHistory hDossier = historiqueDossier(fB, moi, T0.plusMinutes(2));
        ComActionMessage iProspection = interaction(pB, null, moi, T0.plusMinutes(3), "Hors périmètre");
        ComActionMessage iDossier = interaction(null, fB, moi, T0.plusMinutes(4), "Hors périmètre dossier");
        List<UUID> horsPerimetre = List.of(hProspection.getId(), hDossier.getId(), iProspection.getId(), iDossier.getId());

        for (String portee : List.of("MOI", "EQUIPE")) {
            var resultat = mockMvc.perform(get("/api/actions-recentes").param("portee", portee)
                            .param("limite", "100").with(connecte(moi)))
                    .andExpect(status().isOk());
            for (UUID id : horsPerimetre) {
                resultat.andExpect(jsonPath(present(id)).doesNotExist());
            }
        }

        AdmUser directeur = newUser("ar.dg.", "Directeur");
        affecter(directeur, projetA);
        donnerRole(directeur, "Direction Générale");
        var exempte = mockMvc.perform(get("/api/actions-recentes").param("portee", "EQUIPE")
                        .param("limite", "100").with(connecte(directeur)))
                .andExpect(status().isOk());
        for (UUID id : horsPerimetre) {
            exempte.andExpect(jsonPath(present(id)).exists());
        }
    }

    @Test
    void ficheSansProjet_visibleDansMoiEtEquipe() throws Exception {
        ComProspection sansProjet = newProspection("PSN-AR-SANS", null);
        StrHistory h = historiqueProspection(sansProjet, moi, T0.plusMinutes(1), "Désarchivée");
        ComActionMessage i = interaction(sansProjet, null, moi, T0.plusMinutes(2), "Sans projet");

        for (String portee : List.of("MOI", "EQUIPE")) {
            mockMvc.perform(get("/api/actions-recentes").param("portee", portee).with(connecte(moi)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(present(h.getId())).exists())
                    .andExpect(jsonPath(present(i.getId())).exists());
        }
    }

    // ------------------------------------------------------------------ R8

    @Test
    void ligneOrphelineEtInteractionSansFiche_ignorees() throws Exception {
        StrHistory orpheline = historique(UUID.randomUUID(), null, moi, T0.plusMinutes(1), "Désarchivée");
        StrHistory dossierOrphelin = historique(UUID.randomUUID(), StatutDossier.CONFIG_ENTITY_ID, moi,
                T0.plusMinutes(2), "Statut");
        ComActionMessage sansFiche = interaction(null, null, moi, T0.plusMinutes(3), "Sans fiche");

        mockMvc.perform(get("/api/actions-recentes").with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(jsonPath(present(orpheline.getId())).doesNotExist())
                .andExpect(jsonPath(present(dossierOrphelin.getId())).doesNotExist())
                .andExpect(jsonPath(present(sansFiche.getId())).doesNotExist());
    }

    // ------------------------------------------------------------------ R4/R9 — tri, limite, avant

    @Test
    void avant_paginationSansDoublonNiTrou_surLesTroisSources() throws Exception {
        ComProspection p = newProspection("PSN-AR-PAGE", trancheA);
        ComFolder f = newDossier("DOS-AR-PAGE", trancheA);
        List<String> attendus = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDateTime date = T0.plusSeconds(i * 7L);
            UUID id = switch (i % 3) {
                case 0 -> historiqueProspection(p, moi, date, "Désarchivée").getId();
                case 1 -> historiqueDossier(f, moi, date).getId();
                default -> interaction(p, null, moi, date, "Note " + i).getId();
            };
            attendus.add(0, id.toString());
        }

        List<String> servis = new ArrayList<>();
        String avant = null;
        String avantId = null;
        for (int page = 0; page < 5; page++) {
            var requete = get("/api/actions-recentes").param("limite", "3").with(connecte(moi));
            if (avant != null) {
                requete = requete.param("avant", avant).param("avantId", avantId);
            }
            String json = mockMvc.perform(requete).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> ids = JsonPath.read(json, "$[*].id");
            assertThat(ids).hasSizeLessThanOrEqualTo(3);
            if (ids.isEmpty()) {
                break;
            }
            servis.addAll(ids);
            List<String> dates = JsonPath.read(json, "$[*].date");
            avant = dates.get(dates.size() - 1);
            avantId = ids.get(ids.size() - 1);
        }

        assertThat(servis).containsExactlyElementsOf(attendus);
    }

    @Test
    void limiteHorsBornes_ou_porteeInconnue_400() throws Exception {
        mockMvc.perform(get("/api/actions-recentes").param("limite", "101").with(connecte(moi)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/actions-recentes").param("limite", "0").with(connecte(moi)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/actions-recentes").param("portee", "FOO").with(connecte(moi)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/actions-recentes").param("limite", "100").with(connecte(moi)))
                .andExpect(status().isOk());
    }

    @Test
    void sansAuthentification_401() throws Exception {
        mockMvc.perform(get("/api/actions-recentes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void limite_respectee() throws Exception {
        ComProspection p = newProspection("PSN-AR-LIM", trancheA);
        for (int i = 0; i < 4; i++) {
            historiqueProspection(p, moi, T0.plusMinutes(i), "Désarchivée");
            interaction(p, null, moi, T0.plusMinutes(i).plusSeconds(30), "N" + i);
        }

        mockMvc.perform(get("/api/actions-recentes").param("limite", "5").with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    // ------------------------------------------------------------------ cibleType actuel, interactions

    @Test
    void cibleType_suitLeTypeActuelDeLaFiche() throws Exception {
        ComProspection pre = new ComProspection();
        pre.setId(UUID.randomUUID());
        pre.setCode("PSN-AR-PRE");
        pre.setTempDescription("Contact Formulaire");
        pre.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        prospectionRepository.save(pre);
        StrHistory creation = historiqueProspection(pre, moi, T0, "Pré-prospection créée : Contact Formulaire");

        mockMvc.perform(get("/api/actions-recentes").with(connecte(moi)))
                .andExpect(jsonPath(present(creation.getId()) + ".cibleType").value(org.hamcrest.Matchers.contains("PRE_PROSPECTION")))
                .andExpect(jsonPath(present(creation.getId()) + ".cibleLibelle").value(org.hamcrest.Matchers.contains("Contact Formulaire")))
                .andExpect(jsonPath(present(creation.getId()) + ".type").value(org.hamcrest.Matchers.contains("PRE_PROSPECTION_CREEE")));

        pre.setStrEntityId(null);
        prospectionRepository.save(pre);

        mockMvc.perform(get("/api/actions-recentes").with(connecte(moi)))
                .andExpect(jsonPath(present(creation.getId()) + ".cibleType").value(org.hamcrest.Matchers.contains("PROSPECTION")));
    }

    @Test
    void interaction_rattacheeAuxDeux_viseLaProspection_commeLeFiltre() throws Exception {
        ComProspection p = newProspection("PSN-AR-DEUX", trancheA);
        ComFolder f = newDossier("DOS-AR-DEUX", trancheA);
        interaction(p, f, moi, T0, "Les deux");

        mockMvc.perform(get("/api/actions-recentes").with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cibleType").value("PROSPECTION"))
                .andExpect(jsonPath("$[0].cibleId").value(p.getId().toString()))
                .andExpect(jsonPath("$[0].prospectId").value(p.getCfgProspect().getId().toString()));
    }

    @Test
    void memeDate_ordreSqlEtJavaIdentiques_surLesTroisSources() throws Exception {
        ComProspection p = newProspection("PSN-AR-ORDRE", trancheA);
        ComFolder f = newDossier("DOS-AR-ORDRE", trancheA);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ids.add(switch (i % 3) {
                case 0 -> historiqueProspection(p, moi, T0, "Désarchivée").getId();
                case 1 -> historiqueDossier(f, moi, T0).getId();
                default -> interaction(p, null, moi, T0, "Note " + i).getId();
            });
        }
        List<String> attendus = ids.stream()
                .map(id -> id.toString().toUpperCase(java.util.Locale.ROOT))
                .sorted(java.util.Comparator.reverseOrder())
                .map(String::toLowerCase)
                .toList();

        List<String> servis = new ArrayList<>();
        String avant = null;
        String avantId = null;
        for (int page = 0; page < 6; page++) {
            var requete = get("/api/actions-recentes").param("limite", "2").with(connecte(moi));
            if (avant != null) {
                requete = requete.param("avant", avant).param("avantId", avantId);
            }
            String json = mockMvc.perform(requete).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> page_ = JsonPath.read(json, "$[*].id");
            if (page_.isEmpty()) {
                break;
            }
            servis.addAll(page_);
            List<String> dates = JsonPath.read(json, "$[*].date");
            avant = dates.get(dates.size() - 1);
            avantId = page_.get(page_.size() - 1);
        }

        assertThat(servis).containsExactlyElementsOf(attendus);
    }

    @Test
    void avantIdSansAvant_400() throws Exception {
        mockMvc.perform(get("/api/actions-recentes").param("avantId", UUID.randomUUID().toString())
                        .with(connecte(moi)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void interaction_rattacheeAuDossierSeul_viseLeDossier_etLibelleTronque() throws Exception {
        ComFolder f = newDossier("DOS-AR-INT", trancheA);
        ComActionMessageType type = new ComActionMessageType();
        type.setId(UUID.randomUUID());
        type.setCode("AR-TEL");
        type.setDescription("Téléphone Sortant");
        type = actionMessageTypeRepository.save(type);
        ComActionMessage m = interaction(null, f, moi, T0, "x".repeat(300));
        m.setComActionMessageType(type);
        actionMessageRepository.save(m);

        String json = mockMvc.perform(get("/api/actions-recentes").with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cibleType").value("DOSSIER"))
                .andExpect(jsonPath("$[0].cibleId").value(f.getId().toString()))
                .andExpect(jsonPath("$[0].prospectId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String libelle = JsonPath.read(json, "$[0].libelle");
        assertThat(libelle).startsWith("Téléphone Sortant — xxx").hasSize(120).endsWith("…");
    }
}
