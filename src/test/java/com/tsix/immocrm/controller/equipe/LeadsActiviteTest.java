package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.services.equipe.ActiviteLead;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code POST /api/equipe/leads/activite} et colonne « Sans action > 7 j » alignée (FEATURE-leads-a-relancer.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeadsActiviteTest {

    private static final String CHEF = RolesEquipe.CHEF;
    private static final String COMMERCIAL = RolesEquipe.COMMERCIAL;
    private static final String ADMIN = "Administrateur";
    private static final String URL = "/api/equipe/leads/activite";
    private static final String PREMIER = "$[0].";
    private static final DateTimeFormatter A_LA_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private ComActionMessageRepository messageRepository;
    @Autowired private ComProspectionKindRepository kindRepository;
    @Autowired private EntityManager em;
    @Autowired private Clock clock;

    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private AdmUser chef;
    private AdmUser commercialA;
    private AdmUser commercialB;
    private AdmUser admin;
    private LocalDateTime maintenant;

    @BeforeEach
    void setUp() {
        var projetA = outils.projet("EQ-ACT-A");
        var projetB = outils.projet("EQ-ACT-B");
        trancheA = outils.tranche("EQ-ACT-TA", projetA);
        trancheB = outils.tranche("EQ-ACT-TB", projetB);
        chef = outils.compte("eq.act.chef", CHEF);
        outils.affecter(chef, projetA);
        commercialA = outils.compte("eq.act.coma", COMMERCIAL);
        outils.affecter(commercialA, projetA);
        commercialB = outils.compte("eq.act.comb", COMMERCIAL);
        outils.affecter(commercialB, projetB);
        admin = outils.compte("eq.act.admin", ADMIN);
        maintenant = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void comptage_actionsReellesSeulement_derniereEstLaPlusRecenteDesReelles() throws Exception {
        ComProspection lead = lead("EQ-ACT-L1", trancheA, commercialA, 30);
        action(lead, 10, false);
        LocalDateTime derniereReelle = action(lead, 3, false);
        action(lead, 1, true);
        em.flush();

        demander(chef, CHEF, lead.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath(PREMIER + "nombreActions").value(2))
                .andExpect(jsonPath(PREMIER + "derniereActionDate", startsWith(derniereReelle.format(A_LA_MINUTE))))
                .andExpect(jsonPath(PREMIER + "joursSansAction").value(3))
                .andExpect(jsonPath(PREMIER + "endormi").value(false));
    }

    @Test
    void isSystemNull_compteCommeAction() throws Exception {
        ComProspection lead = lead("EQ-ACT-L2", trancheA, commercialA, 30);
        action(lead, 9, null);
        em.flush();

        demander(chef, CHEF, lead.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "nombreActions").value(1))
                .andExpect(jsonPath(PREMIER + "joursSansAction").value(9))
                .andExpect(jsonPath(PREMIER + "endormi").value(true));
    }

    @Test
    void messagesSystemeSeulement_zeroAction_joursDepuisLaDateDuLead() throws Exception {
        ComProspection lead = lead("EQ-ACT-L3", trancheA, commercialA, 12);
        action(lead, 1, true);
        em.flush();

        demander(chef, CHEF, lead.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "nombreActions").value(0))
                .andExpect(jsonPath(PREMIER + "derniereActionDate").doesNotExist())
                .andExpect(jsonPath(PREMIER + "joursSansAction").value(12))
                .andExpect(jsonPath(PREMIER + "endormi").value(true));
    }

    @Test
    void borne_septJoursPileNonEndormi_uneMinuteDePlusEndormi() {
        LocalDateTime fixe = LocalDateTime.of(2026, 10, 13, 10, 0);
        LocalDateTime septJours = fixe.minusDays(7);
        LocalDateTime septJoursEtUneMinute = septJours.minusMinutes(1);

        assertAll(
                () -> assertThat(ActiviteLead.endormi(septJours, fixe)).isFalse(),
                () -> assertThat(ActiviteLead.endormi(septJoursEtUneMinute, fixe)).isTrue(),
                () -> assertThat(ActiviteLead.joursSansAction(septJoursEtUneMinute, fixe)).isEqualTo(7),
                () -> assertThat(ActiviteLead.joursSansAction(fixe.plusDays(1), fixe)).isZero(),
                () -> assertThat(ActiviteLead.reference(null, septJours)).isEqualTo(septJours));
    }

    @Test
    void visibilite_chefNeRecoitQueSesProjetsEtLesSansProjetDeSonEquipe() throws Exception {
        ComProspection sonProjet = lead("EQ-ACT-V1", trancheA, commercialA, 2);
        ComProspection autreProjet = lead("EQ-ACT-V2", trancheB, commercialB, 2);
        ComProspection sansProjetMembre = lead("EQ-ACT-V3", null, commercialA, 2);
        ComProspection sansProjetNonMembre = lead("EQ-ACT-V4", null, commercialB, 2);
        em.flush();
        UUID[] ids = {sonProjet.getId(), autreProjet.getId(), sansProjetMembre.getId(), sansProjetNonMembre.getId(),
                UUID.randomUUID()};

        demander(chef, CHEF, ids)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        sonProjet.getId().toString(), sansProjetMembre.getId().toString())));
        demander(admin, ADMIN, ids)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(sonProjet.getId().toString(),
                        autreProjet.getId().toString(), sansProjetMembre.getId().toString(),
                        sansProjetNonMembre.getId().toString())));
    }

    @Test
    void chefSansProjet_listeVideSansErreur() throws Exception {
        AdmUser chefSansProjet = outils.compte("eq.act.chef0", CHEF);
        ComProspection lead = lead("EQ-ACT-V5", trancheA, commercialA, 2);
        em.flush();

        demander(chefSansProjet, CHEF, lead.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void roleCommercial_403() throws Exception {
        demander(commercialA, COMMERCIAL, UUID.randomUUID()).andExpect(status().isForbidden());
    }

    @Test
    void listeVide_400() throws Exception {
        demander(chef, CHEF).andExpect(status().isBadRequest());
    }

    @Test
    void plusDe200Ids_400() throws Exception {
        UUID[] ids = IntStream.range(0, 201).mapToObj(i -> UUID.randomUUID()).toArray(UUID[]::new);

        demander(chef, CHEF, ids).andExpect(status().isBadRequest());
    }

    @Test
    void colonneSansAction_ignoreLesMessagesSysteme() throws Exception {
        ComProspection lead = lead("EQ-ACT-C1", trancheA, commercialA, 10);
        action(lead, 1, true);
        em.flush();

        mockMvc.perform(get("/api/equipe/commerciaux").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'eq.act.coma')].portefeuilleOuvert").value(1))
                .andExpect(jsonPath("$[?(@.code == 'eq.act.coma')].sansActionDepuis7Jours").value(1));
    }

    /** FEATURE-tri-a-jour.md §Règle 1 : 0 action <b>et</b> étape de départ (ou nulle), même règle que la Boîte de tri. */
    @Test
    void jamaisTraite_zeroActionEtEtapeDeDepart() throws Exception {
        ComProspection sansEtape = lead("EQ-ACT-J1", trancheA, commercialA, 2);
        ComProspection aTraiter = avecEtape(lead("EQ-ACT-J2", trancheA, commercialA, 2), "1 A TRAITER");
        ComProspection etapeChangee = avecEtape(lead("EQ-ACT-J3", trancheA, commercialA, 2), "2 EN COURS DE TRAITEMENT");
        ComProspection avecAction = lead("EQ-ACT-J4", trancheA, commercialA, 2);
        action(avecAction, 1, false);
        em.flush();

        demander(chef, CHEF, sansEtape.getId(), aTraiter.getId(), etapeChangee.getId(), avecAction.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath(jamaisTraite(sansEtape)).value(true))
                .andExpect(jsonPath(jamaisTraite(aTraiter)).value(true))
                .andExpect(jsonPath(jamaisTraite(etapeChangee)).value(false))
                .andExpect(jsonPath("$[?(@.id == '%s')].nombreActions".formatted(etapeChangee.getId())).value(0))
                .andExpect(jsonPath(jamaisTraite(avecAction)).value(false));
    }

    private static String jamaisTraite(ComProspection lead) {
        return "$[?(@.id == '%s')].jamaisTraite".formatted(lead.getId());
    }

    private ComProspection avecEtape(ComProspection lead, String code) {
        ComProspectionKind etape = new ComProspectionKind();
        etape.setId(UUID.randomUUID());
        etape.setCode(code);
        etape.setDescription(code);
        lead.setComProspectionKind(kindRepository.save(etape));
        return lead;
    }

    private ResultActions demander(AdmUser user, String role, UUID... ids) throws Exception {
        String corps = Arrays.stream(ids).map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "{\"ids\":[", "]}"));
        RequestPostProcessor connexion = EquipeOutils.connecte(user, role);
        return mockMvc.perform(post(URL).with(connexion).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ComProspection lead(String code, CfgTranche tranche, AdmUser commercial, int ilYaJours) {
        CfgTier employe = commercial.getCfgEmployee();
        return outils.prospection(code, tranche, employe, maintenant.minusDays(ilYaJours));
    }

    private LocalDateTime action(ComProspection lead, int ilYaJours, Boolean systeme) {
        LocalDateTime date = maintenant.minusDays(ilYaJours);
        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(lead);
        message.setDocDate(date);
        message.setIsSystem(systeme);
        messageRepository.save(message);
        return date;
    }
}
