package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Demandes de remise d'une agence (A3) — FEATURE-agences-vente.md §5 « Demander une remise », §8 « Remise »,
 * décisions du manager A3, réponses du manager A3 et au test-writer A3. Article : plafond 1 = 3,70 %, plafond 2 = 7,41 %.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesDemandesRemiseTest {

    private static final String DEMANDES_DOSSIER = "/api/dossiers/{id}/demandes-remise";
    private static final String FILE = "/api/demandes-remise";
    private static final String ACCEPTATION = "/api/demandes-remise/{id}/acceptation";
    private static final String REFUS = "/api/demandes-remise/{id}/refus";
    private static final String ANNULATION = "/api/demandes-remise/{id}/annulation";
    private static final String EN_ATTENTE = "EN_ATTENTE";
    private static final String STATUT = "$.statut";
    private static final String DROIT_NIVEAU_2 = "Yes.Com.Front.Sales.Monitoring.toolModifyAmounts1";
    private static final String DROIT_SANS_PLAFOND = "Yes.Com.Front.Sales.Monitoring.toolOverDiscount";
    private static final String DIRECTION_FONCIERE = "Direction fonciere";
    private static final String DIRECTION_GENERALE = "Direction Générale";
    private static final String VIDE = "{}";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureStatutsDossier statuts;
    @Autowired private FixtureDossierVente dossiersVente;

    private String jeton;
    private RequestPostProcessor siege;
    private CfgTranche tranche;
    private UUID vendeurA;
    private UUID vendeurB;
    private UUID vendeurSiege;
    private RequestPostProcessor compteA;
    private RequestPostProcessor compteB;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        statuts.reservation();
        siege = connecte(patrimoine.administrateur());
        tranche = plans.tranche("DRT-" + jeton, true);
        UUID agenceA = agences.agence("DRA-" + jeton);
        UUID agenceB = agences.agence("DRB-" + jeton);
        vendeurA = agences.vendeur("DRVA-" + jeton);
        vendeurB = agences.vendeur("DRVB-" + jeton);
        vendeurSiege = agences.vendeur("DRVS-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);
        compteA = compteAgence("DRCA", vendeurA);
        compteB = compteAgence("DRCB", vendeurB);
    }

    // ------------------------------------------------------------------ fixtures

    private RequestPostProcessor compteAgence(String prefixe, UUID vendeur, String... autresRoles) {
        AdmUser compte = agences.compteAgence(prefixe + "-" + jeton, vendeur, autresRoles);
        plans.habiliter(compte, tranche.getCfgProject());
        identifiants(compte);
        String[] roles = new String[autresRoles.length + 1];
        roles[0] = FixtureAgences.ROLE_AGENCE;
        System.arraycopy(autresRoles, 0, roles, 1, autresRoles.length);
        return FixtureDossierVente.connecte(compte, roles);
    }

    /** Compte du siège : rôles en base et dans le jeton, droits {@code AdmRight} posés sur chacun de ses rôles. */
    private RequestPostProcessor compteSiege(String prefixe, List<String> droits, String... roles) {
        return compteSiegeJeton(prefixe, droits, roles, roles);
    }

    private RequestPostProcessor compteSiegeJeton(String prefixe, List<String> droits, String[] rolesEnBase,
                                                 String[] rolesDuJeton) {
        AdmUser compte = dossiersVente.compte(prefixe + "-" + jeton, rolesEnBase);
        plans.habiliter(compte, tranche.getCfgProject());
        em.flush();
        for (String droit : droits) {
            jdbc.update("INSERT INTO AdmRight (PKey, AdmRoleId, Code) SELECT NEWID(), AdmRoleId, ? FROM AdmUserRole "
                    + "WHERE AdmUserId = ?", droit, compte.getId().toString());
        }
        identifiants(compte);
        return FixtureDossierVente.connecte(compte, rolesDuJeton);
    }

    /** {@code GET /api/mon-compte} répond 404 sans ligne {@code AppUserCredential}. */
    private void identifiants(AdmUser compte) {
        em.flush();
        jdbc.update("""
                INSERT INTO AppUserCredential (PKey, AdmUserId, PasswordHash, MustChangePassword, ProfileCompleted, TokenVersion,
                    UserCreateDateTime) VALUES (?, ?, ?, 0, 1, 0, ?)""",
                UUID.randomUUID().toString(), compte.getId().toString(), "$2a$10$abcdefghijklmnopqrstuv",
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 10, 0)));
    }

    private RequestPostProcessor directionFonciere() {
        return compteSiege("DRDF", List.of(DROIT_NIVEAU_2), DIRECTION_FONCIERE);
    }

    private RequestPostProcessor patron() {
        return compteSiege("DRDG", List.of(DROIT_NIVEAU_2, DROIT_SANS_PLAFOND), DIRECTION_GENERALE);
    }

    /** Dossier en Réservation : 100 m² × 10 000 HT = 1 000 000 HT, plafonds 3,70 % / 7,41 %. */
    private UUID dossier(String suffixe, UUID commercial) {
        ComFolder folder = dossiersVente.dossier("DRD" + suffixe + "-" + jeton, tranche, StatutDossier.RESERVATION);
        folder.setPrice(new BigDecimal("1000000"));
        StkItem article = folder.getStkItem();
        article.setDiscountMax(new BigDecimal("0.037037"));
        article.setDiscountMax2(new BigDecimal("0.074074"));
        em.flush();
        UUID id = folder.getId();
        agences.commercialDuDossier(id, commercial);
        return id;
    }

    private UUID dossierA(String suffixe) {
        return dossier(suffixe, vendeurA);
    }

    private void annulerLeDossier(UUID dossier) {
        ComFolder folder = em.find(ComFolder.class, dossier);
        folder.setStrStatus(dossiersVente.statut(StatutDossier.ANNULATION));
        em.flush();
    }

    private ResultActions demander(UUID dossier, RequestPostProcessor qui, String taux) throws Exception {
        return mockMvc.perform(post(DEMANDES_DOSSIER, dossier).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content("{\"taux\":" + taux + ",\"motif\":\"Client hésitant\"}"));
    }

    /** Demande créée par le compte A ; rend son id. */
    private String demandeA(UUID dossier, String taux) throws Exception {
        String corps = demander(dossier, compteA, taux).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.id");
    }

    private ResultActions corps(String route, String demande, RequestPostProcessor qui, String json) throws Exception {
        return mockMvc.perform(post(route, demande).with(qui).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions accepter(String demande, RequestPostProcessor qui, String json) throws Exception {
        return corps(ACCEPTATION, demande, qui, json);
    }

    private ResultActions refuser(String demande, RequestPostProcessor qui, String json) throws Exception {
        return corps(REFUS, demande, qui, json);
    }

    private ResultActions annuler(String demande, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post(ANNULATION, demande).with(qui));
    }

    private ResultActions historique(UUID dossier, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(DEMANDES_DOSSIER, dossier).with(qui));
    }

    private ResultActions file(RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(FILE).param("statut", EN_ATTENTE).with(qui));
    }

    private void statutDeLaDemande(UUID dossier, String demande, String attendu) throws Exception {
        historique(dossier, siege).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + demande + "')].statut", hasItem(attendu)));
    }

    private BigDecimal remiseRelue(UUID dossier) {
        em.flush();
        em.clear();
        return em.find(ComFolder.class, dossier).getDiscountAmount();
    }

    // ------------------------------------------------------------------ création (§5, décisions 1-3, Q1)

    @Test
    void creation_dossierDeMonAgence_201_enAttente() throws Exception {
        UUID dossier = dossierA("C1");

        demander(dossier, compteA, "5").andExpect(status().isCreated())
                .andExpect(jsonPath(STATUT).value(EN_ATTENTE))
                .andExpect(jsonPath("$.dossierId").value(dossier.toString()))
                .andExpect(jsonPath("$.tauxDemande").value(5.0))
                .andExpect(jsonPath("$.motif").value("Client hésitant"));
    }

    @Test
    void creation_dossierDeLAgenceB_404_temoin201() throws Exception {
        demander(dossier("C2", vendeurB), compteA, "5").andExpect(status().isNotFound());

        demander(dossierA("C2T"), compteA, "5").andExpect(status().isCreated());
    }

    @Test
    void creation_dossierDuSiege_404_temoin201() throws Exception {
        demander(dossier("C3", vendeurSiege), compteA, "5").andExpect(status().isNotFound());

        demander(dossierA("C3T"), compteA, "5").andExpect(status().isCreated());
    }

    @Test
    void creation_deuxiemeDemandeEnAttente_409() throws Exception {
        UUID dossier = dossierA("C4");
        demandeA(dossier, "5");

        demander(dossier, compteA, "6").andExpect(status().isConflict());
    }

    // Décision 2 : 0 < taux ≤ 100.
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "101"})
    void creation_tauxHorsBornes_400(String taux) throws Exception {
        demander(dossierA("C5"), compteA, taux).andExpect(status().isBadRequest());
    }

    // Q1 : taux total, strictement supérieur au taux actuel ; l'acceptation remplace la remise (pas de cumul).
    @Test
    void creation_tauxPasSuperieurAuTauxActuel_400_puisAcceptationRemplaceLaRemise() throws Exception {
        UUID dossier = dossierA("C6");
        mockMvc.perform(patch("/api/dossiers/{id}", dossier).with(siege).contentType(MediaType.APPLICATION_JSON)
                .content("{\"remisePourcentage\":5}")).andExpect(status().isOk());

        demander(dossier, compteA, "5").andExpect(status().isBadRequest());
        String demande = demandeA(dossier, "6");
        accepter(demande, patron(), VIDE).andExpect(status().isOk());

        assertThat(remiseRelue(dossier)).isEqualByComparingTo("60000");
    }

    // Décision 3 / Q4 : dossier sorti → 409.
    @Test
    void creation_dossierAnnule_409() throws Exception {
        UUID dossier = dossierA("C7");
        annulerLeDossier(dossier);

        demander(dossier, compteA, "5").andExpect(status().isConflict());
    }

    // Réponse au test-writer : DiscountMax = 0 n'empêche pas la demande.
    @Test
    void creation_articleSansPremierPlafond_201() throws Exception {
        UUID dossier = dossierA("C8");
        ComFolder folder = em.find(ComFolder.class, dossier);
        folder.getStkItem().setDiscountMax(BigDecimal.ZERO);
        em.flush();

        demander(dossier, compteA, "5").andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ annulation (décision 1, réponses au test-writer)

    @Test
    void annulation_parLAgenceDuDossier_200_annulee() throws Exception {
        String demande = demandeA(dossierA("A1"), "5");

        annuler(demande, compteA).andExpect(status().isOk()).andExpect(jsonPath(STATUT).value("ANNULEE"));
    }

    @Test
    void annulation_parLAgenceB_404_temoin200() throws Exception {
        String demande = demandeA(dossierA("A2"), "5");

        annuler(demande, compteB).andExpect(status().isNotFound());
        annuler(demande, compteA).andExpect(status().isOk());
    }

    @Test
    void annulation_parLeSiege_403_temoin200() throws Exception {
        String demande = demandeA(dossierA("A3"), "5");

        annuler(demande, siege).andExpect(status().isForbidden());
        annuler(demande, compteA).andExpect(status().isOk());
    }

    @Test
    void annulation_demandeDejaAnnulee_409() throws Exception {
        String demande = demandeA(dossierA("A4"), "5");
        annuler(demande, compteA).andExpect(status().isOk());

        annuler(demande, compteA).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ historique du dossier (décision 4)

    @Test
    void historique_agenceDuDossierEtSiege_200_contiennentLaDemande() throws Exception {
        UUID dossier = dossierA("H1");
        String demande = demandeA(dossier, "5");

        historique(dossier, compteA).andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(demande)));
        historique(dossier, siege).andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(demande)));
    }

    @Test
    void historique_agenceB_404_temoin200() throws Exception {
        UUID dossier = dossierA("H2");
        demandeA(dossier, "5");

        historique(dossier, compteB).andExpect(status().isNotFound());
        historique(dossier, compteA).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ file du siège (§5, R-S3)

    @Test
    void file_compteAgence_403_temoinNiveau2_200() throws Exception {
        file(compteA).andExpect(status().isForbidden());

        file(directionFonciere()).andExpect(status().isOk());
    }

    @Test
    void file_siegeSansDroitDeRemise_403_temoinNiveau2_200() throws Exception {
        file(compteSiege("DRST", List.of(), "Standardiste")).andExpect(status().isForbidden());

        file(directionFonciere()).andExpect(status().isOk());
    }

    @Test
    void file_contientLaDemandeEnAttente() throws Exception {
        String demande = demandeA(dossierA("F1"), "5");

        file(directionFonciere()).andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(demande)));
    }

    // ------------------------------------------------------------------ acceptation (R-R1, R-R2, R-R3, Q5)

    @Test
    void rR1_niveau2AuDelaDuPlafond2_400_demandeResteEnAttente() throws Exception {
        UUID dossier = dossierA("R1");
        String demande = demandeA(dossier, "10");

        accepter(demande, directionFonciere(), VIDE).andExpect(status().isBadRequest());

        statutDeLaDemande(dossier, demande, EN_ATTENTE);
    }

    @Test
    void rR1_toolOverDiscount_auDelaDuPlafond2_200() throws Exception {
        String demande = demandeA(dossierA("R2"), "10");

        accepter(demande, patron(), "{\"taux\":10}").andExpect(status().isOk())
                .andExpect(jsonPath(STATUT).value("ACCEPTEE"));
    }

    // Réponse au test-writer : valideur = niveau 2 OU toolOverDiscount (un rôle niveau 1 avec l'override valide).
    @Test
    void rR1_overrideSansNiveau2_auDelaDuPlafond2_200() throws Exception {
        String demande = demandeA(dossierA("R3"), "10");

        accepter(demande, compteSiege("DROV", List.of(DROIT_SANS_PLAFOND), "Commercial"), VIDE)
                .andExpect(status().isOk());
    }

    // Décision 2 : le siège n'accorde jamais plus que demandé.
    @Test
    void acceptation_tauxAccordeSuperieurAuDemande_400() throws Exception {
        String demande = demandeA(dossierA("R4"), "5");

        accepter(demande, directionFonciere(), "{\"taux\":6}").andExpect(status().isBadRequest());
    }

    // R-R2 + Q5 : Direction fonciere n'a pas le groupe PRIX ; la remise est écrite sur ComFolder.DiscountAmount.
    @Test
    void rR2_tauxInferieurAccorde_200_remiseAppliqueeAuDossier() throws Exception {
        UUID dossier = dossierA("R5");
        String demande = demandeA(dossier, "5");

        accepter(demande, directionFonciere(), "{\"taux\":3}").andExpect(status().isOk())
                .andExpect(jsonPath(STATUT).value("ACCEPTEE"))
                .andExpect(jsonPath("$.tauxAccorde").value(3.0));

        assertThat(remiseRelue(dossier)).isEqualByComparingTo("30000");
    }

    @Test
    void rR2_corpsVide_tauxDemandeApplique() throws Exception {
        UUID dossier = dossierA("R6");
        String demande = demandeA(dossier, "5");

        accepter(demande, directionFonciere(), VIDE).andExpect(status().isOk())
                .andExpect(jsonPath("$.tauxAccorde").value(5.0));

        assertThat(remiseRelue(dossier)).isEqualByComparingTo("50000");
    }

    @Test
    void rR3_agenceSurSaPropreDemande_403_temoin200() throws Exception {
        String demande = demandeA(dossierA("R7"), "5");

        accepter(demande, compteA, VIDE).andExpect(status().isForbidden());
        accepter(demande, directionFonciere(), VIDE).andExpect(status().isOk());
    }

    @Test
    void rR3_agenceAdministrateur_403_temoin200() throws Exception {
        String demande = demandeA(dossierA("R8"), "5");

        accepter(demande, compteAgence("DRADM", vendeurA, "Administrateur"), VIDE).andExpect(status().isForbidden());
        accepter(demande, directionFonciere(), VIDE).andExpect(status().isOk());
    }

    // Décision 3 : dossier sorti après la demande → 409, la demande reste en attente.
    @Test
    void acceptation_dossierAnnuleApresLaDemande_409_demandeResteEnAttente() throws Exception {
        UUID dossier = dossierA("R9");
        String demande = demandeA(dossier, "5");
        annulerLeDossier(dossier);

        accepter(demande, directionFonciere(), VIDE).andExpect(status().isConflict());

        statutDeLaDemande(dossier, demande, EN_ATTENTE);
    }

    @Test
    void acceptation_demandeDejaTraitee_409() throws Exception {
        String demande = demandeA(dossierA("R10"), "5");
        refuser(demande, directionFonciere(), "{\"motif\":\"Non\"}").andExpect(status().isOk());

        accepter(demande, directionFonciere(), VIDE).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ refus (décision 1)

    @Test
    void refus_sansMotif_400() throws Exception {
        String demande = demandeA(dossierA("RF1"), "5");

        refuser(demande, directionFonciere(), VIDE).andExpect(status().isBadRequest());
    }

    @Test
    void refus_avecMotif_200_refusee() throws Exception {
        String demande = demandeA(dossierA("RF2"), "5");

        refuser(demande, directionFonciere(), "{\"motif\":\"Prix déjà bas\"}").andExpect(status().isOk())
                .andExpect(jsonPath(STATUT).value("REFUSEE"))
                .andExpect(jsonPath("$.motifRefus").value("Prix déjà bas"));
    }

    @Test
    void refus_demandeDejaAcceptee_409() throws Exception {
        String demande = demandeA(dossierA("RF3"), "5");
        accepter(demande, directionFonciere(), VIDE).andExpect(status().isOk());

        refuser(demande, directionFonciere(), "{\"motif\":\"Non\"}").andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ profil : validationRemise (droits en base)

    @Test
    void profil_validationRemise_vraiPourNiveau2EtPatron() throws Exception {
        mockMvc.perform(get("/api/mon-compte").with(directionFonciere())).andExpect(status().isOk())
                .andExpect(jsonPath("$.validationRemise").value(true));
        mockMvc.perform(get("/api/mon-compte").with(patron())).andExpect(status().isOk())
                .andExpect(jsonPath("$.validationRemise").value(true));
    }

    @Test
    void profil_validationRemise_fauxPourAgenceEtStandardiste() throws Exception {
        mockMvc.perform(get("/api/mon-compte").with(compteA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.validationRemise").value(false));
        mockMvc.perform(get("/api/mon-compte").with(compteSiege("DRSP", List.of(), "Standardiste")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationRemise").value(false));
    }

    // ------------------------------------------------------------------ PATCH siège : forcerRemise lu en base (Q2)

    // Rôle Administrateur dans le jeton (groupe PRIX) mais pas en base : toolOverDiscount absent de AdmRight.
    @Test
    void q2_patchForcerRemiseSansToolOverDiscount_400() throws Exception {
        UUID dossier = dossier("Q1", vendeurSiege);
        RequestPostProcessor sansDroit = compteSiegeJeton("DRQ1", List.of(DROIT_NIVEAU_2),
                new String[]{"Commercial Chef"}, new String[]{"Administrateur"});

        mockMvc.perform(patch("/api/dossiers/{id}", dossier).with(sansDroit).contentType(MediaType.APPLICATION_JSON)
                .content("{\"remisePourcentage\":10,\"forcerRemise\":true}")).andExpect(status().isBadRequest());
    }

    @Test
    void q2_patchForcerRemiseAvecToolOverDiscount_200() throws Exception {
        UUID dossier = dossier("Q2", vendeurSiege);
        RequestPostProcessor avecDroit = compteSiegeJeton("DRQ2", List.of(DROIT_NIVEAU_2, DROIT_SANS_PLAFOND),
                new String[]{"Commercial Chef"}, new String[]{"Administrateur"});

        mockMvc.perform(patch("/api/dossiers/{id}", dossier).with(avecDroit).contentType(MediaType.APPLICATION_JSON)
                .content("{\"remisePourcentage\":10,\"forcerRemise\":true}")).andExpect(status().isOk());

        assertThat(remiseRelue(dossier)).isEqualByComparingTo("100000");
    }
}
