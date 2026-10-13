package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un désistement ou une annulation libère le lot ; la réactivation le reprend s'il est libre. FEATURE-liberer-lot.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class LiberationLotTest {

    private static final String STATUT = "/api/dossiers/{id}/statut";
    private static final String RESERVATION = StatutDossier.RESERVATION.getCode();
    private static final String ANNULATION = StatutDossier.ANNULATION.getCode();
    private static final String DESISTEMENT = StatutDossier.DESISTEMENT.getCode();

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StrStatusRepository strStatusRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;
    private CfgProject projet;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-LIB-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-LIB-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-LIB-TEST");
        stkVocationRepository.save(vocation);

        projet = newProjet("PRJ-LIB-A");
        tranche = newTranche("TR-LIB-A", projet);
        statut(RESERVATION);
        statut(ANNULATION);
        statut(DESISTEMENT);
    }

    // ------------------------------------------------------------------ fixtures

    private CfgProject newProjet(String code) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-" + code);
        societe.setDescription("Société " + code);
        societe.setIsActive(true);
        companyRepository.save(societe);

        CfgProject p = new CfgProject();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setDescription("Projet " + code);
        p.setCfgCompany(societe);
        return projectRepository.save(p);
    }

    private CfgTranche newTranche(String code, CfgProject parent) {
        CfgTranche t = new CfgTranche();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        t.setCfgProject(parent);
        return trancheRepository.save(t);
    }

    private StrStatus statut(String code) {
        return Optional.ofNullable(strStatusRepository.findByStrConfigEntityIdAndCode(
                        StatutDossier.CONFIG_ENTITY_ID, code))
                .orElseGet(() -> {
                    StrStatus s = new StrStatus();
                    s.setId(UUID.randomUUID());
                    s.setCode(code);
                    s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
                    return strStatusRepository.save(s);
                });
    }

    private StkItem newItem(String code, CfgTranche itemTranche) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkHierarchy(itemTranche);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        return stkItemRepository.save(item);
    }

    private StkItem newSousArticle(String code, StkItem parent, Boolean alwaysAffected) {
        StkItem sous = newItem(code, tranche);
        sous.setParent(parent);
        sous.setIsUnderItem(true);
        sous.setIsUnderItemOrigin(true);
        sous.setAlwaysAffected(alwaysAffected);
        return stkItemRepository.save(sous);
    }

    /** Dossier au statut donné, lot principal = {@code principal} ; chaque article de {@code lies} reçoit ComFolderId. */
    private ComFolder newDossier(String code, String statutCode, StkItem principal, StkItem... lies) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        folder.setStkItem(principal);
        folder.setStrStatus(statut(statutCode));
        folderRepository.save(folder);
        for (StkItem item : lies) {
            item.setComFolderId(folder.getId());
            stkItemRepository.save(item);
        }
        return folder;
    }

    private ResultActions changerStatut(ComFolder folder, String body) throws Exception {
        return mockMvc.perform(patch(STATUT, folder.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String corps(String statutCode, String note) {
        return note == null
                ? "{\"statut\":\"%s\"}".formatted(statutCode)
                : "{\"statut\":\"%s\",\"note\":\"%s\"}".formatted(statutCode, note);
    }

    private StkItem relu(StkItem item) {
        entityManager.flush();
        entityManager.clear();
        return stkItemRepository.findById(item.getId()).orElseThrow();
    }

    private ComFolder relu(ComFolder folder) {
        entityManager.flush();
        entityManager.clear();
        return folderRepository.findById(folder.getId()).orElseThrow();
    }

    // ------------------------------------------------------------------ R1, R2, R3

    @Test
    void annulationAvecNote_libereLotEtAnnexe_gardeStkItemId_poseDateEtCause() throws Exception {
        StkItem principal = newItem("LIB-PRINC-1", tranche);
        StkItem annexe = newItem("LIB-TER-1", tranche);
        ComFolder folder = newDossier("DOS-LIB-1", RESERVATION, principal, principal, annexe);

        changerStatut(folder, "{\"statut\":\"ANNULATION\",\"date\":\"2026-10-01T00:00:00\",\"note\":\"Client parti\"}")
                .andExpect(status().isOk());

        assertThat(relu(principal).getComFolderId()).isNull();
        assertThat(relu(annexe).getComFolderId()).isNull();
        ComFolder apres = relu(folder);
        assertThat(apres.getStkItem().getId()).isEqualTo(principal.getId());
        assertThat(apres.getCancelDate()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(apres.getCancelDescription()).isEqualTo("Client parti");
    }

    @Test
    void desistement_libereLeLot_etPoseCancelDate() throws Exception {
        StkItem principal = newItem("LIB-PRINC-2", tranche);
        ComFolder folder = newDossier("DOS-LIB-2", RESERVATION, principal, principal);

        changerStatut(folder, corps(DESISTEMENT, "Financement refusé")).andExpect(status().isOk());

        assertThat(relu(principal).getComFolderId()).isNull();
        ComFolder apres = relu(folder);
        assertThat(apres.getCancelDate()).isNotNull();
        assertThat(apres.getCancelDescription()).isEqualTo("Financement refusé");
        assertThat(apres.getStrStatus().getCode()).isEqualTo(DESISTEMENT);
    }

    @Test
    void sousArticles_toujoursAffecteGardeSonParent_lesAutresLePerdent() throws Exception {
        StkItem principal = newItem("LIB-PRINC-3", tranche);
        StkItem toujours = newSousArticle("LIB-SOUS-AA-3", principal, true);
        StkItem jamais = newSousArticle("LIB-SOUS-NA-3", principal, false);
        StkItem indefini = newSousArticle("LIB-SOUS-NULL-3", principal, null);
        ComFolder folder = newDossier("DOS-LIB-3", RESERVATION, principal, principal, toujours, jamais, indefini);

        changerStatut(folder, corps(ANNULATION, "Erreur de saisie")).andExpect(status().isOk());

        StkItem toujoursApres = relu(toujours);
        assertThat(toujoursApres.getComFolderId()).isNull();
        assertThat(toujoursApres.getParent().getId()).isEqualTo(principal.getId());
        StkItem jamaisApres = relu(jamais);
        assertThat(jamaisApres.getComFolderId()).isNull();
        assertThat(jamaisApres.getParent()).isNull();
        assertThat(relu(indefini).getParent()).isNull();
    }

    @Test
    void sortieSansNote_400_etRienNEstEcrit() throws Exception {
        StkItem principal = newItem("LIB-PRINC-4", tranche);
        ComFolder folder = newDossier("DOS-LIB-4", RESERVATION, principal, principal);

        changerStatut(folder, corps(ANNULATION, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("La cause est obligatoire")));
        changerStatut(folder, corps(DESISTEMENT, "   ")).andExpect(status().isBadRequest());

        assertThat(relu(principal).getComFolderId()).isEqualTo(folder.getId());
        assertThat(relu(folder).getStrStatus().getCode()).isEqualTo(RESERVATION);
    }

    // ------------------------------------------------------------------ R4

    @Test
    void reactivation_lotLibre_lotEtSousArticlesToujoursAffectesReoccupes() throws Exception {
        StkItem principal = newItem("LIB-PRINC-5", tranche);
        StkItem toujours = newSousArticle("LIB-SOUS-AA-5", principal, true);
        StkItem jamais = newSousArticle("LIB-SOUS-NA-5", principal, false);
        ComFolder folder = newDossier("DOS-LIB-5", ANNULATION, principal);

        changerStatut(folder, corps(RESERVATION, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles[*].id", hasItem(principal.getId().toString())));

        assertThat(relu(principal).getComFolderId()).isEqualTo(folder.getId());
        assertThat(relu(toujours).getComFolderId()).isEqualTo(folder.getId());
        assertThat(relu(jamais).getComFolderId()).isNull();
    }

    @Test
    void reactivation_lotReprisParUnAutreDossier_409_statutInchange() throws Exception {
        StkItem principal = newItem("LIB-PRINC-6", tranche);
        ComFolder annule = newDossier("DOS-LIB-6A", ANNULATION, principal);
        ComFolder repreneur = newDossier("DOS-LIB-6B", RESERVATION, principal, principal);

        changerStatut(annule, corps(RESERVATION, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("LIB-PRINC-6 a été revendu entre-temps")));

        assertThat(relu(annule).getStrStatus().getCode()).isEqualTo(ANNULATION);
        assertThat(relu(principal).getComFolderId()).isEqualTo(repreneur.getId());
    }

    // ------------------------------------------------------------------ lecture : article et détail (R8)

    @Test
    void lotLibere_apparaitDisponible() throws Exception {
        StkItem principal = newItem("LIB-PRINC-7", tranche);
        ComFolder folder = newDossier("DOS-LIB-7", RESERVATION, principal, principal);

        changerStatut(folder, corps(ANNULATION, "Annulé")).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/articles/{id}", principal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("DISPONIBLE"));
    }

    @Test
    void dossierAnnule_detailMontreToujoursSonLotPrincipal() throws Exception {
        StkItem principal = newItem("LIB-PRINC-8", tranche);
        StkItem annexe = newItem("LIB-TER-8", tranche);
        ComFolder folder = newDossier("DOS-LIB-8", RESERVATION, principal, principal, annexe);

        changerStatut(folder, corps(ANNULATION, "Annulé"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles.length()").value(1))
                .andExpect(jsonPath("$.articles[0].id").value(principal.getId().toString()));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/dossiers/{id}", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles.length()").value(1))
                .andExpect(jsonPath("$.articles[0].id").value(principal.getId().toString()));
    }

    // ------------------------------------------------------------------ R5

    @Test
    void annexeHorsPerimetre_403_etAucunArticleLibere() throws Exception {
        CfgTranche trancheAutre = newTranche("TR-LIB-B", newProjet("PRJ-LIB-B"));
        StkItem principal = newItem("LIB-PRINC-9", tranche);
        StkItem annexe = newItem("LIB-TER-9", trancheAutre);
        ComFolder folder = newDossier("DOS-LIB-9", RESERVATION, principal, principal, annexe);
        AdmUser commercial = new AdmUser();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("COM-LIB-TEST");
        commercial.setDisabled(false);
        userRepository.save(commercial);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(commercial);
        habilitation.setCfgProject(projet);
        habilitationRepository.save(habilitation);

        mockMvc.perform(patch(STATUT, folder.getId()).with(connecte(commercial))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(ANNULATION, "Annulé")))
                .andExpect(status().isForbidden());

        assertThat(relu(principal).getComFolderId()).isEqualTo(folder.getId());
        assertThat(relu(annexe).getComFolderId()).isEqualTo(folder.getId());
    }

    private static RequestPostProcessor connecte(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }
}
