package com.tsix.immocrm.controller.dashboard;

import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tableau de bord, Ma journée, calendrier d'un compte agence — FEATURE-agences-vente.md §4 V4, V5, V8, R-V8.
 * Projet P affecté à l'agence A : prospections pA (vA, ouverte), pA2 (vA, close), pB (vB, agence B), pS (vS, siège)
 * et une pré-prospection assignée à vA ; chaque prospection porte une relance du jour, une en retard et un rendez-vous.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesTableauDeBordTest {

    private static final UUID ENTITE_PROSPECTION = UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601");
    private static final String DASHBOARD = "/api/dashboard";
    private static final String RELANCES = "/api/dashboard/relances";
    private static final String JOURNEE = "/api/journee";
    private static final String CALENDRIER = "/api/calendrier/evenements";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private String jeton;
    private RequestPostProcessor siege;
    private CfgTranche trancheP;
    private UUID vendeurA;
    private RequestPostProcessor compteA;

    private ComProspection preProspectionA;
    private ComActionMessage duJourA;
    private ComActionMessage retardA;
    private ComActionMessage duJourB;
    private ComActionMessage retardB;
    private ComActionMessage duJourS;
    private ComActionMessage retardS;
    private ComActionMessage rdvA;
    private ComActionMessage rdvB;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        siege = connecte(patrimoine.administrateur());
        trancheP = plans.tranche("AGP-" + jeton, true);

        UUID agenceA = agences.agence("AGA-" + jeton);
        UUID agenceB = agences.agence("AGB-" + jeton);
        vendeurA = agences.vendeur("VA-" + jeton);
        UUID vendeurB = agences.vendeur("VB-" + jeton);
        UUID vendeurSiege = agences.vendeur("VS-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        ComProspection pA = prospection("PA", vendeurA, false);
        prospection("PA2", vendeurA, true);
        ComProspection pB = prospection("PB", vendeurB, false);
        ComProspection pS = prospection("PS", vendeurSiege, false);
        preProspectionA = preProspection(vendeurA);

        LocalDateTime aujourdhui = LocalDate.now().atStartOfDay();
        LocalDateTime enRetard = aujourdhui.minusDays(3);
        duJourA = relance(pA, aujourdhui, vendeurA);
        retardA = relance(pA, enRetard, vendeurA);
        duJourB = relance(pB, aujourdhui, vendeurB);
        retardB = relance(pB, enRetard, vendeurB);
        duJourS = relance(pS, aujourdhui, vendeurSiege);
        retardS = relance(pS, enRetard, vendeurSiege);
        rdvA = rendezVous(pA, vendeurA);
        rdvB = rendezVous(pB, vendeurB);
        em.flush();

        compteA = compteAgence("CA", vendeurA);
    }

    // ---------------------------------------------------------------- fixtures

    private CfgTier tiers(String prefixe) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode(prefixe + "-" + jeton);
        t.setFirstName("Agence");
        t.setLastName("TEST");
        t.setMobile("0600000000");
        em.persist(t);
        return t;
    }

    private ComProspection prospection(String prefixe, UUID commercial, boolean close) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(prefixe + "-" + jeton);
        p.setDocDate(LocalDateTime.now());
        p.setUserCreateDateTime(LocalDateTime.now());
        p.setIsClosed(close);
        p.setStrEntityId(ENTITE_PROSPECTION);
        p.setCfgProspect(tiers("PRO-" + prefixe));
        p.setCfgCommercial(em.getReference(CfgTier.class, commercial));
        p.setStkHierarchy(trancheP);
        em.persist(p);
        return p;
    }

    /** V5 : pré-prospection assignée à un vendeur de l'agence, sur le projet affecté — invisible quand même. */
    private ComProspection preProspection(UUID commercial) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PRE-" + jeton);
        p.setDocDate(LocalDateTime.now());
        p.setUserCreateDateTime(LocalDateTime.now());
        p.setIsClosed(false);
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setTempDescription("Lead agence TEST");
        p.setTempTel("0611223344");
        p.setCfgCommercial(em.getReference(CfgTier.class, commercial));
        p.setStkHierarchy(trancheP);
        em.persist(p);
        return p;
    }

    private ComActionMessage message(ComProspection p, UUID employe) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(p);
        m.setCfgEmployee(em.getReference(CfgTier.class, employe));
        m.setDocDate(LocalDateTime.now().minusDays(5));
        m.setDescription("interaction " + jeton);
        m.setWithoutRevival(false);
        m.setIsUnpaid(false);
        m.setIsSystem(false);
        return m;
    }

    private ComActionMessage relance(ComProspection p, LocalDateTime echeance, UUID employe) {
        ComActionMessage m = message(p, employe);
        m.setAlertValidityDate(echeance);
        m.setAlertMessage("rappeler le client");
        em.persist(m);
        return m;
    }

    /** Rendez-vous du jour à 10 h, rattaché comme dans {@code CalendrierControllerTest}. */
    private ComActionMessage rendezVous(ComProspection p, UUID employe) {
        ComActionMessageCategory categorie = new ComActionMessageCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("Prospection-AG-" + UUID.randomUUID());
        em.persist(categorie);

        ComActionMessageSubCategory resultat = new ComActionMessageSubCategory();
        resultat.setId(UUID.randomUUID());
        resultat.setCode("Le prospect veut faire une visite du projet");
        resultat.setDescription("Le prospect veut faire une visite du projet");
        resultat.setComActionMessageCategory(categorie);
        em.persist(resultat);

        AdmEvent evenement = new AdmEvent();
        evenement.setId(UUID.randomUUID());
        evenement.setSubject("Visite " + p.getCode());
        evenement.setStartHour(LocalDate.now().atTime(10, 0));
        evenement.setComActionMessageSubCategory(resultat);
        em.persist(evenement);

        ComActionMessage m = message(p, employe);
        m.setIsIn(true);
        m.setComActionMessageSubCategory(resultat);
        m.setAdmEvent(evenement);
        em.persist(m);
        return m;
    }

    private RequestPostProcessor compteAgence(String prefixe, UUID vendeur, String... autresRoles) {
        AdmUser compte = agences.compteAgence(prefixe + "-" + jeton, vendeur, autresRoles);
        plans.habiliter(compte, trancheP.getCfgProject());
        String[] roles = new String[autresRoles.length + 1];
        roles[0] = FixtureAgences.ROLE_AGENCE;
        System.arraycopy(autresRoles, 0, roles, 1, autresRoles.length);
        return FixtureDossierVente.connecte(compte, roles);
    }

    private static String parInteraction(String tableau, ComActionMessage m) {
        return tableau + "[?(@.interactionId=='" + m.getId() + "')]";
    }

    private static String calendrierDebut() {
        return LocalDate.now() + "T00:00:00";
    }

    private static String calendrierFin() {
        return LocalDate.now() + "T23:59:59";
    }

    // ---------------------------------------------------------------- tableau de bord (V8, R-V8)

    /** R-V8 : compteurs agence recalculés sur V4 — pA et pA2 seulement (ni pB, ni pS, ni la pré-prospection V5). */
    @Test
    void rv8_tableauDeBord_compteurs_prospectionsDeMonAgenceSeulement() throws Exception {
        mockMvc.perform(get(DASHBOARD).with(compteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectionsTotal").value(2))
                .andExpect(jsonPath("$.prospectionsOuvertes").value(1));
    }

    /** R-V8 témoin : le siège garde ses requêtes et compte les quatre prospections. */
    @Test
    void rv8_temoin_tableauDeBord_siege_compteToutesLesProspections() throws Exception {
        mockMvc.perform(get(DASHBOARD).with(siege))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectionsTotal").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.prospectionsOuvertes").value(greaterThanOrEqualTo(3)));
    }

    /** V4/V8 : les relances en retard (liste du tableau de bord, par interaction) ne contiennent ni pB ni pS. */
    @Test
    void v8_tableauDeBord_relancesEnRetard_sansAutreAgenceNiSiege() throws Exception {
        mockMvc.perform(get(RELANCES).with(compteA)
                        .param("portee", "MOI").param("echeance", "RETARD").param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(parInteraction("$.content", retardA)).isNotEmpty())
                .andExpect(jsonPath(parInteraction("$.content", retardB)).isEmpty())
                .andExpect(jsonPath(parInteraction("$.content", retardS)).isEmpty());
    }

    /** V4/V8 : liste des relances du jour (portée MOI) d'une agence — la sienne, aucune de pB ou pS. */
    @Test
    void v8_relancesDuJour_moi_sansAutreAgenceNiSiege() throws Exception {
        mockMvc.perform(get(RELANCES).with(compteA)
                        .param("portee", "MOI").param("echeance", "AUJOURDHUI").param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(parInteraction("$.content", duJourA)).isNotEmpty())
                .andExpect(jsonPath(parInteraction("$.content", duJourB)).isEmpty())
                .andExpect(jsonPath(parInteraction("$.content", duJourS)).isEmpty());
    }

    // ---------------------------------------------------------------- Ma journée (V5, V8)

    /** V5 (R-V8) : la pré-prospection assignée à vA n'apparaît pas dans les nouveaux leads de l'agence. */
    @Test
    void v5_journee_nouveauxLeads_sansPreProspection_memeAssignee() throws Exception {
        mockMvc.perform(get(JOURNEE).with(compteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.lignes[?(@.prospectionId=='" + preProspectionA.getId() + "')]")
                        .isEmpty())
                .andExpect(jsonPath("$.nouveauxLeads.total").value(0));
    }

    /** V8 + R-V3 : un compte Agence + Administrateur ne voit dans sa journée aucune relance du siège ni de B. */
    @Test
    void v8_journee_agenceAdministrateur_sansRelancesDuSiegeNiAutreAgence() throws Exception {
        RequestPostProcessor agenceAdmin = compteAgence("CAD", vendeurA, "Administrateur");

        mockMvc.perform(get(JOURNEE).with(agenceAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath(parInteraction("$.relancesDuJour.lignes", duJourA)).isNotEmpty())
                .andExpect(jsonPath(parInteraction("$.relancesDuJour.lignes", duJourS)).isEmpty())
                .andExpect(jsonPath(parInteraction("$.relancesDuJour.lignes", duJourB)).isEmpty())
                .andExpect(jsonPath(parInteraction("$.enRetard.lignes", retardA)).isNotEmpty())
                .andExpect(jsonPath(parInteraction("$.enRetard.lignes", retardS)).isEmpty())
                .andExpect(jsonPath(parInteraction("$.enRetard.lignes", retardB)).isEmpty());
    }

    // ---------------------------------------------------------------- calendrier (V8)

    /** V8 : le calendrier de l'agence montre le rendez-vous de pA, pas celui de pB. */
    @Test
    void v8_calendrier_agence_rendezVousDeMonAgenceSeulement() throws Exception {
        mockMvc.perform(get(CALENDRIER).with(compteA)
                        .param("debut", calendrierDebut()).param("fin", calendrierFin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(parInteraction("$", rdvA)).isNotEmpty())
                .andExpect(jsonPath(parInteraction("$", rdvB)).isEmpty());
    }

    /** V8 témoin : le siège voit le rendez-vous de pB. */
    @Test
    void v8_temoin_calendrier_siege_voitRendezVousAutreAgence() throws Exception {
        mockMvc.perform(get(CALENDRIER).with(siege)
                        .param("debut", calendrierDebut()).param("fin", calendrierFin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(parInteraction("$", rdvB)).isNotEmpty());
    }
}
