package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionInteractionsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private ComActionMessageCategoryRepository actionMessageCategoryRepository;

    @Autowired
    private ComActionMessageSubCategoryRepository actionMessageSubCategoryRepository;

    @Autowired
    private FixtureInteraction fixtureInteraction;

    @Autowired
    private AdmUserRepository admUserRepository;

    private String note() {
        return fixtureInteraction.typeNote().getId().toString();
    }

    private String telephone() {
        return fixtureInteraction.typeTelephone().getId().toString();
    }

    private ComProspection newProspection(String code) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        return prospectionRepository.save(p);
    }

    private ComActionMessageSubCategory newResultat(String code, boolean revivalMandatory) {
        ComActionMessageCategory category = new ComActionMessageCategory();
        category.setId(UUID.randomUUID());
        category.setCode("Prospection-TEST-" + UUID.randomUUID());
        actionMessageCategoryRepository.save(category);

        ComActionMessageSubCategory resultat = new ComActionMessageSubCategory();
        resultat.setId(UUID.randomUUID());
        resultat.setCode(code);
        resultat.setDescription(code);
        resultat.setComActionMessageCategory(category);
        resultat.setIsRevivalMandatory(revivalMandatory);
        return actionMessageSubCategoryRepository.save(resultat);
    }

    @Test
    void findInteractions_returnsExistingJournalEntry() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-1");

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setDescription("Appel initial");
        message.setIsIn(true);
        actionMessageRepository.save(message);

        mockMvc.perform(get("/api/prospections/{id}/interactions", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + message.getId() + "')]").exists());
    }

    /**
     * L'auteur affiché ne peut pas dépendre du seul employé lié : {@code CfgEmployeeId} n'est
     * renseigné que sur 77 % des 48 278 interactions réelles — et jamais par les comptes créés
     * par cette plateforme, qui n'ont pas d'employé — quand {@code UserName} l'est à 99,996 %.
     * Sans le repli, le client affichait « Système » à la place de la personne connectée,
     * alors que le logiciel de bureau, lui, retrace l'auteur.
     */
    @Test
    void findInteractions_sansEmploye_lAuteurVientDeUserName() throws Exception {
        ComProspection prospection = newProspection("PSN-AUTEUR-1");

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setUserName("Khadija ELKORRI");
        message.setUserCreate("Khadija ELKORRI");
        actionMessageRepository.save(message);

        mockMvc.perform(get("/api/prospections/{id}/interactions", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeNom").value("Khadija ELKORRI"))
                .andExpect(jsonPath("$.content[0].employeId").doesNotExist());
    }

    private CfgTier newEmploye(String prenom, String nom) {
        CfgTier employe = new CfgTier();
        employe.setId(UUID.randomUUID());
        employe.setCode("EMP-" + UUID.randomUUID());
        employe.setFirstName(prenom);
        employe.setLastName(nom);
        return tierRepository.save(employe);
    }

    private AdmUser newCompte(String description, CfgTier employe) {
        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        compte.setCode("tst.auteur." + UUID.randomUUID());
        compte.setDescription(description);
        compte.setDisabled(false);
        compte.setCfgEmployee(employe);
        return admUserRepository.save(compte);
    }

    private ComActionMessage newAction(ComProspection prospection, AdmUser compte, CfgTier commercial,
                                       String userName, String userCreate) {
        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setAdmUser(compte);
        message.setCfgEmployee(commercial);
        message.setUserName(userName);
        message.setUserCreate(userCreate);
        return actionMessageRepository.save(message);
    }

    /** OMRAN met dans CfgEmployeeId le commercial de la fiche : l'auteur est l'employé du compte AdmUser. */
    @Test
    void findInteractions_auteurEstLEmployeDuCompte_pasLeCommercialConcerne() throws Exception {
        CfgTier auteur = newEmploye("Othman", "DIOURI");
        CfgTier commercial = newEmploye("Sabah", "HANINI");
        ComProspection prospection = newProspection("PSN-AUTEUR-2");
        newAction(prospection, newCompte("o.diouri", auteur), commercial, "Othman DIOURI", "Othman DIOURI");

        mockMvc.perform(get("/api/prospections/{id}/interactions", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeNom").value("Othman DIOURI"))
                .andExpect(jsonPath("$.content[0].employeId").value(commercial.getId().toString()));
    }

    @Test
    void findInteractions_compteSansEmploye_libelleDuCompte() throws Exception {
        ComProspection prospection = newProspection("PSN-AUTEUR-3");
        newAction(prospection, newCompte("Compte Accueil", null), newEmploye("Sabah", "HANINI"),
                "accueil", "accueil");

        mockMvc.perform(get("/api/prospections/{id}/interactions", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeNom").value("Compte Accueil"));
    }

    @Test
    void findInteractions_sansCompte_userNamePlutotQueLeCommercial() throws Exception {
        ComProspection prospection = newProspection("PSN-AUTEUR-4");
        newAction(prospection, null, newEmploye("Hamza", "ATCHANY"), "compte.generique", "autre.compte");

        mockMvc.perform(get("/api/prospections/{id}/interactions", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeNom").value("compte.generique"));
    }

    @Test
    void findInteractions_sansCompteNiUserName_userCreate() throws Exception {
        ComProspection prospection = newProspection("PSN-AUTEUR-5");
        newAction(prospection, null, newEmploye("Hamza", "ATCHANY"), null, "createur.legacy");

        mockMvc.perform(get("/api/prospections/{id}/interactions", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeNom").value("createur.legacy"));
    }

    @Test
    void findInteractions_unknownProspection_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}/interactions", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void addInteraction_happyPath_returns201() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-2");
        String body = """
                {"description":"Relance telephonique","entrante":false,"actionTypeId":"%s"}""".formatted(note());

        mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                // Le texte fourni ressort dans `note`, plus dans `description` : celui-ci est
                // désormais un libellé généré. Voir instructions/FIX-interaction-note.md.
                .andExpect(jsonPath("$.note").value("Relance telephonique"))
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.not("Relance telephonique")))
                .andExpect(jsonPath("$.entrante").value(false))
                .andExpect(jsonPath("$.id").exists());
    }

    /**
     * Les quatre drapeaux du légataire, posés à la création.
     *
     * <p><strong>Ce n'est pas de la conformité décorative.</strong>
     * {@code ComSaleProspectionView} calcule {@code LastCommunicationDate} et
     * {@code LastCommunicationResult} avec {@code AND IsSystem = 0}, et en SQL un {@code NULL} ne
     * satisfait pas {@code = 0} : tant que nous laissions la colonne nulle, nos interactions
     * étaient écartées du « dernier échange » — dans l'écran du logiciel de bureau comme dans notre
     * propre liste, qui lit cette colonne depuis la vue.
     *
     * <p>Mesuré avant correctif sur {@code yesRems} : 14 lignes à {@code IsSystem NULL}, toutes
     * écrites par cette API, sur 8 prospections. Rattrapage :
     * {@code db/migration/007_ComActionMessage_drapeaux_api.sql}.
     */
    @Test
    void addInteraction_poseLesDrapeauxDuLegataire() throws Exception {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("TIER-DRAPEAUX");
        prospect.setLastName("BENNANI");
        prospect = tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DRAPEAUX");
        prospection.setCfgProspect(prospect);
        prospection = prospectionRepository.save(prospection);

        String body = """
                {"description":"Appel abouti","entrante":true,"actionTypeId":"%s"}""".formatted(note());
        String reponse = mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(com.jayway.jsonpath.JsonPath.read(reponse, "$.id"));
        ComActionMessage ecrite = actionMessageRepository.findById(id).orElseThrow();

        assertThat(ecrite.getIsSystem())
                .as("IsSystem doit valoir 0 : c'est ce que la vue légataire exige pour compter "
                    + "l'interaction dans le dernier échange")
                .isFalse();
        assertThat(ecrite.getIsNotRead()).isFalse();
        assertThat(ecrite.getIsUnpaid()).isFalse();
        assertThat(ecrite.getWithoutRevival())
                .as("0 à la création — le passage à 1 est l'affaire de la transformation en vente")
                .isFalse();
        assertThat(ecrite.getCfgTier()).isNotNull();
        assertThat(ecrite.getCfgTier().getId())
                .as("le tiers de l'interaction est le prospect de la prospection, comme chez le "
                    + "légataire (96,5 % de ses interactions humaines le portent)")
                .isEqualTo(prospect.getId());
    }

    /** Sans prospect lié, le tiers reste nul plutôt qu'inventé — le cas des pré-prospections. */
    @Test
    void addInteraction_sansProspect_laisseLeTiersNul() throws Exception {
        ComProspection prospection = newProspection("PSN-DRAPEAUX-2");

        String reponse = mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Lead brut rappele","actionTypeId":"%s"}""".formatted(note())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(com.jayway.jsonpath.JsonPath.read(reponse, "$.id"));
        ComActionMessage ecrite = actionMessageRepository.findById(id).orElseThrow();
        assertThat(ecrite.getCfgTier()).isNull();
        assertThat(ecrite.getIsSystem()).isFalse();
    }

    @Test
    void addInteraction_unknownProspection_returns404() throws Exception {
        String body = """
                {"description":"x"}""";

        mockMvc.perform(post("/api/prospections/{id}/interactions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void addInteraction_noteSansMessage_returns400() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-3");

        mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionTypeId\":\"" + note() + "\",\"description\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("message")));
    }

    @Test
    void addInteraction_withResultatRequiringRdv_andDateRdv_returns201WithDateRdv() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-4");
        ComActionMessageSubCategory resultat = newResultat("Rendez vous pris", true);
        String body = """
                {"description":"RDV pris avec le prospect","resultatId":"%s","dateRdv":"2026-09-01T10:00:00",
                 "actionTypeId":"%s","dateAlerte":"2026-09-01T09:00:00"}"""
                .formatted(resultat.getId(), telephone());

        mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultatId").value(resultat.getId().toString()))
                .andExpect(jsonPath("$.dateRdv").value("2026-09-01T10:00:00"));
    }

    @Test
    void addInteraction_withResultatRequiringRdv_butNoDateRdv_returns400() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-5");
        ComActionMessageSubCategory resultat = newResultat("RDV reporté", true);
        String body = """
                {"description":"Sans date de RDV","resultatId":"%s","actionTypeId":"%s",
                 "dateAlerte":"2026-09-01T09:00:00"}""".formatted(resultat.getId(), telephone());

        mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addInteraction_withResultatVisite_andDateRdv_returns201WithDateRdv() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-6");
        ComActionMessageSubCategory resultat = newResultat("Le prospect veut faire une visite du projet", true);
        String body = """
                {"description":"Le prospect veut visiter le showroom","resultatId":"%s","dateRdv":"2026-09-15T10:00:00",
                 "actionTypeId":"%s","dateAlerte":"2026-09-15T09:00:00"}"""
                .formatted(resultat.getId(), telephone());

        mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultatId").value(resultat.getId().toString()))
                .andExpect(jsonPath("$.dateRdv").value("2026-09-15T10:00:00"));
    }

    @Test
    void addInteraction_withResultatVisite_butNoDateRdv_returns400() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-7");
        ComActionMessageSubCategory resultat = newResultat("Le prospect veut faire une visite du projet", true);
        String body = """
                {"description":"Sans date de visite","resultatId":"%s","actionTypeId":"%s",
                 "dateAlerte":"2026-09-15T09:00:00"}""".formatted(resultat.getId(), telephone());

        mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
