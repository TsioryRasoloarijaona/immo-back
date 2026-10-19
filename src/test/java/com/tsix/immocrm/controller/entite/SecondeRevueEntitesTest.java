package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_VENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seconde revue (visibilité, autorisation, suppressions) de FEATURE-entites-juridiques.md §2, §7.6, §7.11 : chemins
 * non couverts par les tests d'acceptation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecondeRevueEntitesTest {

    private static final String ENTITE = "/api/entites-juridiques/{id}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "SR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = fixture.entite("EJ-" + jeton, true);
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    /** §7.6 : créer un article dans une tranche d'une entité inactive est refusé (400 sur la tranche), rien n'est écrit. */
    @Test
    void creationArticle_trancheDUneEntiteInactive_400_rienEcrit() throws Exception {
        fixture.referentielArticle();
        CfgTranche tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, entite));
        StkItemCategory categorie = fixture.categorie("CAT-" + jeton, fixture.natureBat(), false);
        StkItemType type = fixture.type("TYP-" + jeton, fixture.natureBat(), false);
        entites.actif(entite.getId(), false);

        mockMvc.perform(post("/api/articles").with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("trancheId", tranche.getId()).avec("code", "ART-" + jeton)
                                .avec("description", "Article R4").avec("vocationId", VOCATION_VENTE)
                                .avec("categorieId", categorie.getId()).avec("typeId", type.getId())
                                .avec("immeubleId", fixture.immeuble("IMM-" + jeton).getId())
                                .avec("etageId", fixture.etage("ETG-" + jeton).getId())
                                .avec("surfaceProvisoire", new BigDecimal("10"))
                                .avec("surfaceDefinitive", new BigDecimal("10")).json()))
                .andExpect(status().isBadRequest());

        assertThat(entites.compter("SELECT COUNT(*) FROM StkItem WHERE StkHierarchyId = :id", tranche.getId())).isZero();
    }

    /** R4 sur une route de tranche hors TrancheController : réajustement des superficies. */
    @Test
    void reajustementSuperficies_trancheDUneEntiteInactive_404() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, entite));
        String url = "/api/tranches/" + tranche.getId() + "/reajustement-superficies";
        mockMvc.perform(get(url).with(connecte(admin))).andExpect(status().isOk());

        entites.actif(entite.getId(), false);

        mockMvc.perform(get(url).with(connecte(admin))).andExpect(status().isNotFound());
    }

    /** §7.11.4 : actionnaire cité par une seule ligne de groupe (CASCADE en base) → 409, ligne et actionnaire gardés. */
    @Test
    void suppressionActionnaire_presentDansUnGroupeSeulement_409_ligneConservee() throws Exception {
        UUID actionnaire = entites.actionnaire("ACT-" + jeton);
        UUID groupe = entites.groupe("GRP-" + jeton);
        entites.ligneGroupe(groupe, actionnaire, BigDecimal.ONE);

        mockMvc.perform(delete("/api/actionnaires/{id}", actionnaire).with(connecte(admin)))
                .andExpect(status().isConflict());

        assertThat(entites.lignesGroupe(groupe)).hasSize(1);
        assertThat(entites.compter("SELECT COUNT(*) FROM CfgShareholder WHERE PKey = :id", actionnaire)).isEqualTo(1);
    }

    /** Supprimer une entité n'efface ni le groupe qu'elle a choisi, ni la répartition d'une autre entité. */
    @Test
    void suppressionEntite_neToucheNiAuGroupePartageNiALaRepartitionDUneAutreEntite() throws Exception {
        CfgCompany autre = fixture.entite("EJ2-" + jeton, true);
        UUID actionnaire = entites.actionnaire("ACT-" + jeton);
        UUID groupe = entites.groupe("GRP-" + jeton);
        entites.ligneGroupe(groupe, actionnaire, BigDecimal.ONE);
        entites.groupeDeLEntite(entite.getId(), groupe);
        entites.groupeDeLEntite(autre.getId(), groupe);
        entites.repartition(entite.getId(), actionnaire, 10);
        entites.repartition(autre.getId(), actionnaire, 20);

        mockMvc.perform(delete(ENTITE, entite.getId()).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(entites.repartitions(autre.getId())).hasSize(1);
        assertThat(entites.lignesGroupe(groupe)).hasSize(1);
        assertThat(entites.compter("SELECT COUNT(*) FROM CfgCompany WHERE PKey = :id", autre.getId())).isEqualTo(1);
        assertThat(entites.compter("SELECT COUNT(*) FROM CfgGroupShareholder WHERE PKey = :id", groupe)).isEqualTo(1);
    }

    /** PUT répartition lu en JsonNode : un corps valide d'un Commercial est refusé (403) sans rien écrire. */
    @Test
    void putRepartition_commercialCorpsValide_403_repartitionInchangee() throws Exception {
        UUID actionnaire = entites.actionnaire("ACT-" + jeton);
        entites.parts(entite.getId(), 10, 1);
        entites.repartition(entite.getId(), actionnaire, 10);

        mockMvc.perform(put(ENTITE + "/repartition", entite.getId()).with(commercial(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isForbidden());

        assertThat(entites.repartitions(entite.getId())).hasSize(1);
    }

    /** Le porteur ENTITE ne doit pas ouvrir le journal d'un actionnaire ou d'un groupe par son id : 404. */
    @Test
    void audit_idDActionnaire_404() throws Exception {
        UUID actionnaire = entites.actionnaire("ACT-" + jeton);

        mockMvc.perform(get("/api/audit").param("objetId", actionnaire.toString()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
