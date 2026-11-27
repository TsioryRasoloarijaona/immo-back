package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.dossier.workflow.IntervenantCourant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Statuts visibles d'un compte agence = ses rôles ∪ rôle Commercial, pour la visibilité seulement
 * (FEATURE-agences-correctifs-passe.md R-S1 à R-S3). S1 lié au rôle Commercial, S2 à un rôle étranger à l'agence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesStatutsVisiblesTest {

    private static final String DOSSIERS = "/api/dossiers";
    private static final String SEARCH = "search";
    private static final String COMMERCIAL = "Commercial";
    private static final String RESERVATION = "RESERVATION";
    private static final String CONCRETISATION = "CONCRETISATION";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureVisibiliteOmran visibilite;
    @Autowired private IntervenantCourant intervenantCourant;

    private String jeton;
    private CfgTranche tranche;
    private UUID roleCommercial;
    private UUID dossierS1;
    private AdmUser compteAgence;
    private RequestPostProcessor agence;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = plans.tranche("ASV-" + jeton, true);
        UUID agenceA = agences.agence("AGA-" + jeton);
        UUID agenceB = agences.agence("AGB-" + jeton);
        UUID vendeurA = agences.vendeur("VA-" + jeton);
        UUID vendeurB = agences.vendeur("VB-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        roleCommercial = visibilite.role(COMMERCIAL);
        UUID roleEtranger = visibilite.role("AUTRE-" + jeton);
        dossierS1 = dossier("L1", StatutDossier.RESERVATION, vendeurA, roleCommercial);
        dossier("L2", StatutDossier.CONCRETISATION, vendeurA, roleEtranger);
        dossier("L3", StatutDossier.RESERVATION, vendeurB, roleCommercial);

        compteAgence = agences.compteAgence("CA-" + jeton, vendeurA);
        plans.habiliter(compteAgence, tranche.getCfgProject());
        agence = FixtureDossierVente.connecte(compteAgence, FixtureAgences.ROLE_AGENCE);
    }

    @AfterEach
    void viderContexte() {
        SecurityContextHolder.clearContext();
    }

    /** Dossier vif du vendeur ; son statut (neuf, propre au dossier) est lié au rôle donné. */
    private UUID dossier(String prefixe, StatutDossier statut, UUID vendeur, UUID role) {
        StkItem article = plans.article(tranche, prefixe + "-" + jeton, false, null);
        ComFolder cree = plans.dossier(article, statut, "Magenta", statut.name());
        UUID id = cree.getId();
        UUID statutId = cree.getStrStatus().getId();
        visibilite.intervenantRole(StatutDossier.CONFIG_ENTITY_ID, statutId, role);
        plans.comFolderId(article.getId(), id);
        agences.commercialDuDossier(id, vendeur);
        return id;
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> codesStatuts(RequestPostProcessor qui) throws Exception {
        return JsonPath.read(corps(get(DOSSIERS + "/statuts").with(qui)), "$[*].code");
    }

    @Test
    void liste_agence_seulLeDossierS1DeSonVendeur() throws Exception {
        List<String> ids = JsonPath.read(corps(get(DOSSIERS).param(SEARCH, jeton).with(agence)), "$.content[*].id");

        assertThat(ids).singleElement()
                .satisfies(id -> assertThat(id).isEqualToIgnoringCase(dossierS1.toString()));
        mockMvc.perform(get(DOSSIERS).param(SEARCH, jeton).with(agence))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void statuts_agence_contientS1PasS2() throws Exception {
        assertThat(codesStatuts(agence)).contains(RESERVATION).doesNotContain(CONCRETISATION);
    }

    @Test
    void groupesEtTotaux_agence_memeTotalQueLaListe() throws Exception {
        List<Number> nombres = JsonPath.read(corps(get(DOSSIERS + "/groupes").param("groupBy", "statut")
                .param(SEARCH, jeton).with(agence)), "$.content[*].nombre");

        assertThat(nombres.stream().mapToLong(Number::longValue).sum()).isEqualTo(1);
        mockMvc.perform(get(DOSSIERS + "/totaux").param(SEARCH, jeton).with(agence))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDossiers").value(1));
    }

    @Test
    void temoin_commercialDuSiege_voitS1PasS2() throws Exception {
        AdmUser commercial = vente.compte("COM-" + jeton, COMMERCIAL);
        plans.habiliter(commercial, tranche.getCfgProject());

        assertThat(codesStatuts(FixtureDossierVente.connecte(commercial, COMMERCIAL)))
                .contains(RESERVATION).doesNotContain(CONCRETISATION);
    }

    @Test
    void transitions_restentSurLesVraisRoles_commercialPourLaVisibiliteSeulement() {
        connecterAgence();

        assertThat(intervenantCourant.courant().roleIds()).doesNotContain(roleCommercial);
        assertThat(intervenantCourant.pourVisibilite().roleIds()).contains(roleCommercial);
    }

    @Test
    void statutsDeProjets_agence_voitLeStatutLieACommercial() {
        UUID entite = visibilite.entite("PRJ-" + jeton);
        UUID statutProjet = visibilite.statut(entite, "SP-" + jeton);
        visibilite.intervenantRole(entite, statutProjet, roleCommercial);
        connecterAgence();

        assertThat(intervenantCourant.statutsDeLotsEtProjets()).hasValueSatisfying(
                statuts -> assertThat(statuts).contains(statutProjet));
    }

    private void connecterAgence() {
        AuthenticatedUser principal = new AuthenticatedUser(compteAgence.getId(), compteAgence.getCode(),
                null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + FixtureAgences.ROLE_AGENCE))));
    }
}
