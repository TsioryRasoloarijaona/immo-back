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
 * 2e revue A3 — « Décision du manager après 1re revue A3 » (FEATURE-agences-vente.md) : une acceptation ne baisse
 * jamais une remise (taux accordé strictement supérieur au taux actuel relu) ; taux arrondis à 4 décimales HALF_UP
 * avant tout contrôle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesDemandesRemiseRevue2Test {

    private static final String DROIT_NIVEAU_2 = "Yes.Com.Front.Sales.Monitoring.toolModifyAmounts1";
    private static final String DROIT_SANS_PLAFOND = "Yes.Com.Front.Sales.Monitoring.toolOverDiscount";
    private static final String EN_ATTENTE = "EN_ATTENTE";

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
    private RequestPostProcessor compteA;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        statuts.reservation();
        siege = connecte(patrimoine.administrateur());
        tranche = plans.tranche("RVT-" + jeton, true);
        UUID agenceA = agences.agence("RVA-" + jeton);
        vendeurA = agences.vendeur("RVVA-" + jeton);
        agences.lier(agenceA, vendeurA);
        AdmUser compte = agences.compteAgence("RVCA-" + jeton, vendeurA);
        plans.habiliter(compte, tranche.getCfgProject());
        identifiants(compte);
        compteA = FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);
    }

    private void identifiants(AdmUser compte) {
        em.flush();
        jdbc.update("""
                INSERT INTO AppUserCredential (PKey, AdmUserId, PasswordHash, MustChangePassword, ProfileCompleted, TokenVersion,
                    UserCreateDateTime) VALUES (?, ?, ?, 0, 1, 0, ?)""",
                UUID.randomUUID().toString(), compte.getId().toString(), "$2a$10$abcdefghijklmnopqrstuv",
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 10, 0)));
    }

    private RequestPostProcessor patron() {
        AdmUser compte = dossiersVente.compte("RVDG-" + jeton, "Direction Générale");
        plans.habiliter(compte, tranche.getCfgProject());
        em.flush();
        for (String droit : List.of(DROIT_NIVEAU_2, DROIT_SANS_PLAFOND)) {
            jdbc.update("INSERT INTO AdmRight (PKey, AdmRoleId, Code) SELECT NEWID(), AdmRoleId, ? FROM AdmUserRole "
                    + "WHERE AdmUserId = ?", droit, compte.getId().toString());
        }
        identifiants(compte);
        return FixtureDossierVente.connecte(compte, "Direction Générale");
    }

    /** Dossier en Réservation, 1 000 000 HT, plafonds 3,70 % / 7,41 %. */
    private UUID dossierA(String suffixe) {
        ComFolder folder = dossiersVente.dossier("RVD" + suffixe + "-" + jeton, tranche, StatutDossier.RESERVATION);
        folder.setPrice(new BigDecimal("1000000"));
        StkItem article = folder.getStkItem();
        article.setDiscountMax(new BigDecimal("0.037037"));
        article.setDiscountMax2(new BigDecimal("0.074074"));
        em.flush();
        UUID id = folder.getId();
        agences.commercialDuDossier(id, vendeurA);
        return id;
    }

    private void remiseSiege(UUID dossier, String taux) throws Exception {
        mockMvc.perform(patch("/api/dossiers/{id}", dossier).with(siege).contentType(MediaType.APPLICATION_JSON)
                .content("{\"remisePourcentage\":" + taux + "}")).andExpect(status().isOk());
    }

    private ResultActions demander(UUID dossier, String taux) throws Exception {
        return mockMvc.perform(post("/api/dossiers/{id}/demandes-remise", dossier).with(compteA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"taux\":" + taux + ",\"motif\":\"Client hésitant\"}"));
    }

    private String demandeA(UUID dossier, String taux) throws Exception {
        String corps = demander(dossier, taux).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(corps, "$.id");
    }

    private BigDecimal remiseRelue(UUID dossier) {
        em.flush();
        em.clear();
        return em.find(ComFolder.class, dossier).getDiscountAmount();
    }

    // Demande à 6 %, puis le siège porte la remise à 7 % : accepter les 6 % baisserait la remise → 400, rien ne bouge.
    @Test
    void acceptation_tauxInferieurALaRemiseActuelle_400_remiseInchangeeDemandeEnAttente() throws Exception {
        UUID dossier = dossierA("B1");
        String demande = demandeA(dossier, "6");
        remiseSiege(dossier, "7");

        mockMvc.perform(post("/api/demandes-remise/{id}/acceptation", demande).with(patron())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());

        assertThat(remiseRelue(dossier)).isEqualByComparingTo("70000");
        mockMvc.perform(get("/api/dossiers/{id}/demandes-remise", dossier).with(siege)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + demande + "')].statut", hasItem(EN_ATTENTE)));
    }

    // Taux accordé égal au taux actuel : « strictement supérieur » → 400.
    @Test
    void acceptation_tauxEgalALaRemiseActuelle_400() throws Exception {
        UUID dossier = dossierA("B2");
        String demande = demandeA(dossier, "6");
        remiseSiege(dossier, "3");

        mockMvc.perform(post("/api/demandes-remise/{id}/acceptation", demande).with(patron())
                .contentType(MediaType.APPLICATION_JSON).content("{\"taux\":3}")).andExpect(status().isBadRequest());

        assertThat(remiseRelue(dossier)).isEqualByComparingTo("30000");
    }

    // 5,00004 arrondi à 4 décimales = 5,0000 = taux actuel → 400 (arrondi avant contrôle).
    @Test
    void creation_tauxArrondiEgalAuTauxActuel_400() throws Exception {
        UUID dossier = dossierA("B3");
        remiseSiege(dossier, "5");

        demander(dossier, "5.00004").andExpect(status().isBadRequest());
    }
}
