package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTrancheFiche.libelle;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Référentiels de lecture de la fiche tranche — FEATURE-patrimoine-tranche-fiche.md §1.2, §2, §7.3. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielPatrimoineControllerTest {

    private static final String BASE = "/api/referentiels-patrimoine/";
    private static final String PROJETS_FACTURATION = BASE + "projets-facturation";
    private static final String ENTITE_ID = "entiteId";
    private static final String PAR_ID = "$[?(@.id=='%s')]";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private FixtureArticleFiche ficheArticle;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1AR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private ResultActions lire(String ressource) throws Exception {
        return mockMvc.perform(get(BASE + ressource).with(connecte(admin)));
    }

    private static String ligne(UUID id) {
        return PAR_ID.formatted(id);
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    // ------------------------------------------------------------------ tables simples

    @Test
    void r2_standings_exposeIdCodeDescription() throws Exception {
        UUID id = fiche.standing("STD-" + jeton);

        lire("standings")
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(id) + ".code").value("STD-" + jeton))
                .andExpect(jsonPath(ligne(id) + ".description").value(libelle("STD-" + jeton)));
    }

    @Test
    void r2_niveauxTerrain_exposeIdCodeDescription() throws Exception {
        UUID id = fiche.niveauTerrain("LVL-" + jeton);

        lire("niveaux-terrain")
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(id) + ".code").value("LVL-" + jeton))
                .andExpect(jsonPath(ligne(id) + ".description").value(libelle("LVL-" + jeton)));
    }

    @Test
    void r2_rubriquesTranche_exposentLeSorting() throws Exception {
        UUID id = fiche.rubrique("RUB-" + jeton, 7);

        lire("rubriques-tranche")
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(id) + ".code").value("RUB-" + jeton))
                .andExpect(jsonPath(ligne(id) + ".sorting").value(7));
    }

    @Test
    void r2_plansComptables_exposeLesComptes() throws Exception {
        UUID id = fiche.planComptable("ACT-" + jeton);

        lire("plans-comptables")
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(id) + ".code").value("ACT-" + jeton));
    }

    // ------------------------------------------------------------------ tiers filtrés

    /** CfgTierView.GetEmployeeList : CfgTierTypeId = EMP. */
    @Test
    void r2_personnels_employesSeuls() throws Exception {
        CfgTier employe = fiche.employe("EMP-" + jeton);
        CfgTier fournisseur = fiche.fournisseur("FOU-" + jeton);

        lire("personnels")
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(employe.getId())).isNotEmpty())
                .andExpect(jsonPath(ligne(employe.getId()) + ".code").value("EMP-" + jeton))
                .andExpect(jsonPath(ligne(fournisseur.getId())).isEmpty());
    }

    /** Tranche.cs, sélecteur « Tiers autrui » : IsSupplier seul. */
    @Test
    void r2_tiersAutrui_fournisseursSeuls() throws Exception {
        CfgTier employe = fiche.employe("EMP-TA-" + jeton);
        CfgTier fournisseur = fiche.fournisseur("FOU-TA-" + jeton);

        lire("tiers-autrui")
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(fournisseur.getId())).isNotEmpty())
                .andExpect(jsonPath(ligne(employe.getId())).isEmpty());
    }

    // ------------------------------------------------------------------ projets de facturation (§7.3)

    @Test
    void r7_3_projetsFacturation_parEntite() throws Exception {
        CfgCompany entite = fixture.entite("ENT-F-" + jeton, true);
        CfgCompany autre = fixture.entite("ENT-F2-" + jeton, true);
        UUID duProjet = fiche.projetFacturation("PRJF-" + jeton, entite.getId());
        UUID dAilleurs = fiche.projetFacturation("PRJF2-" + jeton, autre.getId());

        mockMvc.perform(get(PROJETS_FACTURATION).param(ENTITE_ID, entite.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(duProjet)).isNotEmpty())
                .andExpect(jsonPath(ligne(dAilleurs)).isEmpty());
    }

    @Test
    void r7_3_projetsFacturation_entiteInactive_listeVide() throws Exception {
        CfgCompany inactive = fixture.entite("ENT-FI-" + jeton, false);
        fiche.projetFacturation("PRJFI-" + jeton, inactive.getId());

        mockMvc.perform(get(PROJETS_FACTURATION).param(ENTITE_ID, inactive.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void r7_3_projetsFacturation_sansEntite_entitesActivesSeules() throws Exception {
        UUID actif = fiche.projetFacturation("PRJFA-" + jeton, fixture.entite("ENT-FA-" + jeton, true).getId());
        UUID inactif = fiche.projetFacturation("PRJFB-" + jeton, fixture.entite("ENT-FB-" + jeton, false).getId());

        mockMvc.perform(get(PROJETS_FACTURATION).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(actif)).isNotEmpty())
                .andExpect(jsonPath(ligne(inactif)).isEmpty());
    }

    @Test
    void r7_3_projetsFacturation_nonAdmin_403() throws Exception {
        mockMvc.perform(get(PROJETS_FACTURATION).with(commercial(admin)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ §1.2 clé d'API

    /** Préfixe absent de PorteeCleApi (à ne pas y ajouter) : une clé, même à portée totale, est refusée. */
    @Test
    void r1_2_cleApi_horsPortee_403() throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-p1a-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(admin);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(get(BASE + "standings").header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    // ------------------------------------------------------------------ FEATURE-patrimoine-article-fiche.md §1, §2, §9.9

    @ParameterizedTest(name = "{1}")
    @CsvSource({"StkOrientation, orientations", "StkCostCategory, categories-charge", "StkFacade, facades",
            "StkVue, vues"})
    void fiche_r9_9_referentielDeLArticle_exposeIdCodeDescription(String table, String ressource) throws Exception {
        String code = "ART-REF-" + jeton;
        UUID id = ficheArticle.reference(table, code);

        lire(ressource)
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(id) + ".code").value(code))
                .andExpect(jsonPath(ligne(id) + ".description").value(FixtureArticleFiche.libelle(code)));
    }

    /** §2 : référentiel des contenances avec leurs options (option grisée si vide, quantité si IsOptionNWithQuantity). */
    @Test
    void fiche_r2_contenances_exposentLesOptions() throws Exception {
        UUID chambre = ficheArticle.contenance("CH-" + jeton, "SDB", true);
        UUID salon = ficheArticle.contenance("SAL-" + jeton, "", false);

        lire("contenances")
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(chambre) + ".code").value("CH-" + jeton))
                .andExpect(jsonPath(ligne(chambre) + ".option1").value("SDB"))
                .andExpect(jsonPath(ligne(chambre) + ".option1AvecQuantite").value(true))
                .andExpect(jsonPath(ligne(salon) + ".option1AvecQuantite").value(false));
    }
}
