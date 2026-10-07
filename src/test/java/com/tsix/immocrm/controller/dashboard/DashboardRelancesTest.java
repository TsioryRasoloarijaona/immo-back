package com.tsix.immocrm.controller.dashboard;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dashboard/relances} et {@code GET /api/dashboard/nouveaux-pre-prospects} —
 * voir {@code instructions/FEATURE-dashboard-relances.md}.
 *
 * <p>Les assertions portent sur <b>l'identifiant fabriqué par le test</b>, jamais sur un total :
 * la base de test partage ses tables avec les fixtures des autres classes, et un
 * {@code totalElements} attendu serait faux dès qu'une autre classe pose une alerte dans la même
 * fenêtre. D'où le filtre JsonPath par {@code interactionId} et la taille de page large.
 *
 * <p>Ce qui est couvert en priorité, ce sont les <b>exclusions</b> : elles portent l'essentiel du
 * volume (86 % des alertes de la base réelle sont désarmées) et ce sont elles qui casseraient en
 * silence — une relance en trop ne lève aucune erreur, elle pollue simplement la journée d'un
 * commercial.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DashboardRelancesTest {

    private static final String RELANCES = "/api/dashboard/relances";
    private static final String LEADS = "/api/dashboard/nouveaux-pre-prospects";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    @Autowired
    private ComDocumentRepository documentRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    // ---------------------------------------------------------------- fixtures

    private CfgTier newTier(String prefixe) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode(prefixe + "-" + UUID.randomUUID());
        t.setFirstName("Relance");
        t.setLastName("TEST");
        t.setMobile("0600000000");
        return tierRepository.save(t);
    }

    private ComProspection newProspection(CfgTier prospect, CfgTier commercial) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("DR-PSN-" + UUID.randomUUID());
        p.setCfgProspect(prospect);
        p.setCfgCommercial(commercial);
        p.setDocDate(LocalDateTime.now());
        p.setUserCreateDateTime(LocalDateTime.now());
        return prospectionRepository.save(p);
    }

    /** Une pré-prospection : les deux FK nulles, c'est le discriminant du légataire. */
    private ComProspection newPreProspection(LocalDateTime creation) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("DR-PRE-" + UUID.randomUUID());
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setTempDescription("Lead TEST");
        p.setTempTel("0611223344");
        p.setTempEMail("lead@test.local");
        p.setDocDate(creation);
        p.setUserCreateDateTime(creation);
        return prospectionRepository.save(p);
    }

    private ComActionMessage newRelance(ComProspection p, LocalDateTime echeance, CfgTier employe) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(p);
        m.setCfgEmployee(employe);
        m.setDocDate(LocalDateTime.now().minusDays(1));
        m.setAlertValidityDate(echeance);
        m.setAlertMessage("rappeler le client");
        m.setDescription("libelle genere");
        return actionMessageRepository.saveAndFlush(m);
    }

    /** {@code employeeId} est la clé de {@code portee=MOI} ; l'{@code admUserId} reste sans habilitation, donc non restreint. */
    private RequestPostProcessor connecte(UUID employeeId) {
        AuthenticatedUser principal =
                new AuthenticatedUser(UUID.randomUUID(), "dashboard-test", employeeId, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static String filtre(UUID interactionId) {
        return "$.content[?(@.interactionId=='" + interactionId + "')]";
    }

    // ---------------------------------------------------------------- chemin nominal

    @Test
    void aujourdhui_sertLaRelanceDuJour() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);
        ComActionMessage relance = newRelance(p, LocalDate.now().atStartOfDay(), commercial);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(relance.getId())).isNotEmpty())
                .andExpect(jsonPath(filtre(relance.getId()) + ".joursDeRetard").value(hasItem(0)))
                .andExpect(jsonPath(filtre(relance.getId()) + ".nonAffectee").value(hasItem(false)))
                .andExpect(jsonPath(filtre(relance.getId()) + ".preProspection").value(hasItem(false)));
    }

    /**
     * L'heure de {@code AlertValidityDate} ne porte aucune information en base légataire — les
     * 3 765 relances armées sont à minuit. Une relance posée à 23:59 par cette API doit malgré
     * tout compter pour son jour, d'où une borne haute exclusive au lendemain 00:00 et non une
     * égalité d'instants.
     */
    @Test
    void alerteEnFinDeJournee_compteCommeAujourdhui() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);
        ComActionMessage relance = newRelance(p, LocalDate.now().atTime(23, 59, 59), commercial);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(relance.getId())).isNotEmpty());
    }

    // ---------------------------------------------------------------- les exclusions

    /**
     * Le filtre qui porte le plus de volume : 33 941 des 39 335 alertes réelles sont désarmées,
     * posées par {@code desarmerRelancesDeLaProspection} à la transformation en vente. L'oublier
     * ressortirait des prospections déjà conclues.
     */
    @Test
    void relanceDesarmee_nApparaitPas() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);
        ComActionMessage relance = newRelance(p, LocalDate.now().atStartOfDay(), commercial);
        relance.setWithoutRevival(true);
        actionMessageRepository.saveAndFlush(relance);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(relance.getId())).isEmpty());
    }

    /** Une alerte rattachée à une échéance est un rappel de paiement, pas du travail commercial. */
    @Test
    void relanceRattacheeAUneEcheance_nApparaitPas() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);

        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        documentRepository.save(echeance);

        ComActionMessage relance = newRelance(p, LocalDate.now().atStartOfDay(), commercial);
        relance.setComDocument(echeance);
        actionMessageRepository.saveAndFlush(relance);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(relance.getId())).isEmpty());
    }

    // ---------------------------------------------------------------- RETARD : les prospections du mois
    //
    // Décision du 28/09/2026 : le tableau de bord doit afficher le même nombre que la liste des
    // prospections, qui travaille sur les prospections créées dans le mois. Mesuré en production
    // pour une commerciale : 81 relances avec l'ancienne fenêtre de 30 jours, 45 dans sa liste.

    private ComProspection newProspectionCreeeLe(LocalDateTime creation, CfgTier commercial) {
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);
        p.setDocDate(creation);
        return prospectionRepository.saveAndFlush(p);
    }

    private void attendreEnRetard(ComActionMessage relance, boolean servie) throws Exception {
        mockMvc.perform(get(RELANCES).param("echeance", "RETARD").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(servie
                        ? jsonPath(filtre(relance.getId())).isNotEmpty()
                        : jsonPath(filtre(relance.getId())).isEmpty());
    }

    @Test
    void retard_sertLaRelanceEchueDUneProspectionDuMois() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);
        ComActionMessage relance = newRelance(p, LocalDate.now().minusDays(1).atStartOfDay(), commercial);

        mockMvc.perform(get(RELANCES).param("echeance", "RETARD").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(relance.getId())).isNotEmpty())
                .andExpect(jsonPath(filtre(relance.getId()) + ".joursDeRetard").value(hasItem(1)));
    }

    /** Les deux bords du mois : le 1er à 00:00 est dedans, la veille à 23:59 dehors. */
    @Test
    void retard_borneAuMoisCivilDeLaProspection() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        LocalDate premier = LocalDate.now().withDayOfMonth(1);
        ComProspection duPremier = newProspectionCreeeLe(premier.atStartOfDay(), commercial);
        ComProspection deLaVeille = newProspectionCreeeLe(premier.minusDays(1).atTime(23, 59), commercial);
        LocalDateTime hier = LocalDate.now().minusDays(1).atStartOfDay();

        attendreEnRetard(newRelance(duPremier, hier, commercial), true);
        attendreEnRetard(newRelance(deLaVeille, hier, commercial), false);
    }

    /**
     * La borne porte sur la prospection, plus sur l'alerte : une date d'alerte saisie loin dans le
     * passé (4 cas réels chez une commerciale) reste un retard de la prospection du mois.
     */
    @Test
    void retard_sansBorneSurLAncienneteDeLAlerte() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);
        ComActionMessage ancienne = newRelance(p, LocalDate.now().minusDays(45).atStartOfDay(), commercial);

        mockMvc.perform(get(RELANCES).param("echeance", "RETARD").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(ancienne.getId()) + ".joursDeRetard").value(hasItem(45)));
    }

    /** Une pré-prospection n'est dans aucune liste de prospections : elle ne compte pas en retard. */
    @Test
    void retard_ecarteLesPreProspections() throws Exception {
        ComProspection lead = newPreProspection(LocalDateTime.now());
        attendreEnRetard(newRelance(lead, LocalDate.now().minusDays(1).atStartOfDay(), null), false);
    }

    /** Le légataire laisse poser une alerte sur une prospection archivée et ne l'éteint jamais. */
    @Test
    void retard_ecarteUneProspectionFermee() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection fermee = newProspection(newTier("DR-PRO"), commercial);
        fermee.setIsClosed(true);
        prospectionRepository.saveAndFlush(fermee);

        attendreEnRetard(newRelance(fermee, LocalDate.now().minusDays(1).atStartOfDay(), commercial), false);
    }

    /** {@code AUJOURDHUI} garde sa population : la règle du mois ne vaut que pour le retard. */
    @Test
    void aujourdhui_resteOuvertAuxProspectionsAnciennesEtAuxPreProspections() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection ancienne = newProspectionCreeeLe(LocalDateTime.now().minusMonths(3), commercial);
        ComActionMessage surAncienne = newRelance(ancienne, LocalDate.now().atStartOfDay(), commercial);
        ComActionMessage surLead = newRelance(newPreProspection(LocalDateTime.now().minusMonths(2)),
                LocalDate.now().atStartOfDay(), null);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(surAncienne.getId())).isNotEmpty())
                .andExpect(jsonPath(filtre(surLead.getId())).isNotEmpty());
    }

    /** Une relance du jour n'est pas un retard, et réciproquement : les deux tranches sont disjointes. */
    @Test
    void aujourdhuiEtRetard_sontDisjoints() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);
        ComActionMessage duJour = newRelance(p, LocalDate.now().atStartOfDay(), commercial);

        mockMvc.perform(get(RELANCES).param("echeance", "RETARD").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(duJour.getId())).isEmpty());
    }

    // ---------------------------------------------------------------- le commercial

    /**
     * Les deux sources concordent à 98,8 % en base réelle, mais 31 lignes les opposent. C'est
     * l'affectation de la prospection qui gagne : elle est l'attribution de droit, tandis que
     * l'auteur de l'interaction n'est que celui qui a décroché ce jour-là.
     */
    @Test
    void commercialDeLaProspection_primeSurAuteurDeLInteraction() throws Exception {
        CfgTier titulaire = newTier("DR-TIT");
        CfgTier auteur = newTier("DR-AUT");
        ComProspection p = newProspection(newTier("DR-PRO"), titulaire);
        ComActionMessage relance = newRelance(p, LocalDate.now().atStartOfDay(), auteur);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(relance.getId()) + ".commercialId")
                        .value(hasItem(titulaire.getId().toString())));
    }

    /** {@code portee=MOI} retient bien les miennes — la prospection m'est affectée. */
    @Test
    void porteeMoi_sertLesRelancesDontJeSuisLeCommercial() throws Exception {
        CfgTier moi = newTier("DR-MOI");
        ComProspection p = newProspection(newTier("DR-PRO"), moi);
        ComActionMessage mienne = newRelance(p, LocalDate.now().atStartOfDay(), moi);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "MOI")
                        .param("size", "200")
                        .with(connecte(moi.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(mienne.getId())).isNotEmpty());
    }

    /** …et écarte celles d'un autre commercial. */
    @Test
    void porteeMoi_ecarteLesRelancesDunAutre() throws Exception {
        CfgTier moi = newTier("DR-MOI");
        CfgTier autre = newTier("DR-AUTRE");
        ComProspection p = newProspection(newTier("DR-PRO"), autre);
        ComActionMessage sienne = newRelance(p, LocalDate.now().atStartOfDay(), autre);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "MOI")
                        .param("size", "200")
                        .with(connecte(moi.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(sienne.getId())).isEmpty());
    }

    /**
     * Le point le moins intuitif du contrat. 108 relances réelles ne désignent personne, et les
     * 21 posées sur des pré-prospections en font toutes partie — une pré-prospection est
     * <em>définie</em> par l'absence de commercial. Les filtrer sous {@code MOI} les rendrait
     * invisibles à tout le monde, or c'est le canal des leads injectés automatiquement.
     */
    @Test
    void relanceSansCommercial_estServieMemeSousPorteeMoi() throws Exception {
        ComProspection pre = newPreProspection(LocalDateTime.now());
        ComActionMessage orpheline = newRelance(pre, LocalDate.now().atStartOfDay(), null);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "MOI")
                        .param("size", "200")
                        .with(connecte(newTier("DR-MOI").getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(orpheline.getId())).isNotEmpty())
                .andExpect(jsonPath(filtre(orpheline.getId()) + ".nonAffectee").value(hasItem(true)))
                .andExpect(jsonPath(filtre(orpheline.getId()) + ".preProspection").value(hasItem(true)));
    }

    // ---------------------------------------------------------------- le nom du prospect
    //
    // FEATURE-nom-pre-prospection-relances : une pré-prospection n'a pas de fiche tiers, son nom
    // est dans TempDescription. Sans repli, le bloc « À rappeler » affichait « Lead sans nom ».

    @Test
    void preProspectionSansFiche_prospectNomEstLeTempDescription() throws Exception {
        ComProspection lead = newPreProspection(LocalDateTime.now());
        lead.setTempDescription("  Karim Lead-Nom  ");
        prospectionRepository.saveAndFlush(lead);
        ComActionMessage relance = newRelance(lead, LocalDate.now().atStartOfDay(), null);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(relance.getId()) + ".prospectNom")
                        .value(hasItem("Karim Lead-Nom")));
    }

    @Test
    void prospectionAvecFiche_gardeLeNomDeLaFicheMalgreTempDescription() throws Exception {
        CfgTier commercial = newTier("DR-COM");
        ComProspection p = newProspection(newTier("DR-PRO"), commercial);
        p.setTempDescription("Nom saisi au premier contact");
        prospectionRepository.saveAndFlush(p);
        ComActionMessage relance = newRelance(p, LocalDate.now().atStartOfDay(), commercial);

        mockMvc.perform(get(RELANCES).param("echeance", "AUJOURDHUI").param("portee", "EQUIPE")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre(relance.getId()) + ".prospectNom")
                        .value(hasItem("Relance TEST")));
    }

    // ---------------------------------------------------------------- nouveaux pré-prospects

    @Test
    void nouveauxPreProspects_sertLesLeadsDuJour() throws Exception {
        ComProspection lead = newPreProspection(LocalDateTime.now());

        mockMvc.perform(get(LEADS).param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.prospectionId=='" + lead.getId() + "')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.prospectionId=='" + lead.getId() + "')].telephone")
                        .value(hasItem("0611223344")));
    }

    /** Une prospection qualifiée n'est pas un lead : le discriminant est l'absence des deux FK. */
    @Test
    void nouveauxPreProspects_ecarteUneProspectionQualifiee() throws Exception {
        ComProspection qualifiee = newProspection(newTier("DR-PRO"), newTier("DR-COM"));

        mockMvc.perform(get(LEADS).param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.prospectionId=='" + qualifiee.getId() + "')]").isEmpty());
    }

    /** Le jour est un filtre strict : un lead d'hier ne remonte pas dans la tuile d'aujourd'hui. */
    @Test
    void nouveauxPreProspects_ecarteLeadDeLaVeille() throws Exception {
        ComProspection hier = newPreProspection(LocalDateTime.now().minusDays(1));

        mockMvc.perform(get(LEADS).param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.prospectionId=='" + hier.getId() + "')]").isEmpty());
    }

    /** …mais reste atteignable en nommant sa date, ce à quoi sert le paramètre. */
    @Test
    void nouveauxPreProspects_jourExplicite_rattrapeLaVeille() throws Exception {
        ComProspection hier = newPreProspection(LocalDateTime.now().minusDays(1));

        mockMvc.perform(get(LEADS).param("size", "200")
                        .param("jour", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.prospectionId=='" + hier.getId() + "')]").isNotEmpty());
    }
}
