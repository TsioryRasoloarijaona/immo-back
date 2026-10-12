package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgCompanyShareholderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue du lot P5 (FEATURE-entites-juridiques.md §2) : défauts constatés, tests en échec tant qu'ils ne sont pas corrigés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RevueEntitesJuridiquesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private CfgCompanyShareholderRepository repartitionRepository;

    private AdmUser admin;
    private CfgShareholder a;
    private CfgShareholder b;
    private CfgShareholder c;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-P5-REVUE");
        entites.donnerRole(admin, ROLE_ADMIN);
        a = entites.actionnaire("TST-P5-RVA");
        b = entites.actionnaire("TST-P5-RVB");
        c = entites.actionnaire("TST-P5-RVC");
    }

    /**
     * CfgGroupShareholderLine.SharingNumber est decimal(26,6) : 0,3333333 + 0,3333333 + 0,3333334 = 1 en Java passe le
     * contrôle Σ = 100 %, mais la base arrondit chaque ligne à 0,333333 et enregistre un groupe à 99,9999 % que
     * CfgGroupShareholder.Verif d'OMRAN refuse. Attendu : 400 sur « lignes » (contrôle sur les valeurs à l'échelle 6).
     */
    @Test
    void groupe_pourcentagesAuDelaDe6Decimales_sommeEnregistreeDifferenteDe100_400() throws Exception {
        List<Map<String, Object>> lignes = List.of(
                Map.of("actionnaireId", a.getId(), "pourcentage", "0.3333333"),
                Map.of("actionnaireId", b.getId(), "pourcentage", "0.3333333"),
                Map.of("actionnaireId", c.getId(), "pourcentage", "0.3333334"));

        mockMvc.perform(post("/api/groupes-actionnaires").with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "TST-P5-RVGRP", "description", "Groupe tiers", "lignes", lignes))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value("lignes"));
    }

    /**
     * OMRAN SetRepartitions calcule sur le nombre de parts saisi sur la fiche (en mémoire), avant Verif. Ici le calcul
     * part du nombre ENREGISTRÉ, et Verif interdit d'enregistrer nombreParts seul : une entité sans répartition (0 part)
     * ne peut jamais recevoir 1000 parts réparties depuis un groupe sans saisir d'abord une répartition à la main.
     * Forme de correctif supposée ici : le nombre de parts de la fiche transmis au chargement (?nombreParts=).
     */
    @Test
    void depuisGroupe_entiteSansPart_calculSurLeNombreDePartsSaisi() throws Exception {
        CfgCompany entite = fixture.entite("TST-P5-RVENT", true);
        CfgGroupShareholder groupe = entites.groupe("TST-P5-RVG6040");
        entites.ligneGroupe(groupe, a, "0.6");
        entites.ligneGroupe(groupe, b, "0.4");

        mockMvc.perform(post("/api/entites-juridiques/{id}/repartition/depuis-groupe/{groupeId}",
                                entite.getId(), groupe.getId())
                        .param("nombreParts", "1000")
                        .with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreParts").value(1000));

        assertThat(repartitionRepository.deLEntite(entite.getId()))
                .extracting(l -> l.getCfgShareholder().getCode(), CfgCompanyShareholder::getSharingNumber)
                .containsExactly(tuple("TST-P5-RVA", 600), tuple("TST-P5-RVB", 400));
    }
}
