package com.tsix.immocrm.controller.entite;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Groupes d'actionnaires {@code /api/groupes-actionnaires} — FEATURE-entites-juridiques.md §2 : Code O U,
 * Description O, lignes {@code {actionnaireId, pourcentage}} en fraction, Σ = 100 % ({@code CfgGroupShareholder.cs:118-125}),
 * doublon refusé, DELETE 204/409 (GroupesActionnairesTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupesActionnairesTest {

    private static final String GROUPES = "/api/groupes-actionnaires";
    private static final String GROUPE = "/api/groupes-actionnaires/{id}";
    private static final String SOMME = "La somme des pourcentages doit être égale à 100 % !";
    private static final String DOUBLON = "L'actionnaire existe déjà dans la liste !!!";
    private static final String COMPTE = "SELECT COUNT(*) FROM CfgGroupShareholder WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private String jeton;
    private UUID a;
    private UUID b;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        a = entites.actionnaire("ACT-GA-" + jeton);
        b = entites.actionnaire("ACT-GB-" + jeton);
    }

    /** {@code paires} = actionnaire, pourcentage, actionnaire, pourcentage… */
    private static String lignes(Object... paires) {
        StringJoiner json = new StringJoiner(",", "[", "]");
        for (int i = 0; i + 1 < paires.length; i += 2) {
            json.add("{\"actionnaireId\":\"" + paires[i] + "\",\"pourcentage\":" + paires[i + 1] + "}");
        }
        return json.toString();
    }

    private static String corps(String code, String lignes) {
        return "{\"code\":\"" + code + "\",\"description\":\"Groupe " + code + "\",\"lignes\":" + lignes + "}";
    }

    private ResultActions creer(String corps) throws Exception {
        return mockMvc.perform(post(GROUPES).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ResultActions modifier(UUID id, String corps) throws Exception {
        return mockMvc.perform(patch(GROUPE, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(corps));
    }

    private static List<String> enLignes(List<Tuple> lignes) {
        return lignes.stream()
                .map(t -> guid(t.get("CfgShareholderId")) + "=" + decimal(t.get("SharingNumber")).stripTrailingZeros()
                        .toPlainString())
                .toList();
    }

    // ------------------------------------------------------------------ création

    @Test
    void r2_creation_201_groupeEtLignesEcrits() throws Exception {
        String json = creer(corps("GRP-CRE-" + jeton, lignes(a, "0.6", b, "0.4")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        assertThat(entites.compter(COMPTE, id)).isEqualTo(1);
        assertThat(enLignes(entites.lignesGroupe(id))).containsExactlyInAnyOrder(guid(a) + "=0.6", guid(b) + "=0.4");
    }

    @Test
    void r2_creation_sommeDifferenteDe100_400() throws Exception {
        creer(corps("GRP-SOM-" + jeton, lignes(a, "0.6", b, "0.3")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(SOMME)));
    }

    /** OMRAN contrôle la somme sans condition : aucune ligne = 0 % ≠ 100 %. */
    @Test
    void r2_creation_sansLigne_400() throws Exception {
        creer(corps("GRP-VIDE-" + jeton, "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(SOMME)));
    }

    @Test
    void r2_creation_doublon_400() throws Exception {
        creer(corps("GRP-DBL-" + jeton, lignes(a, "0.5", a, "0.5")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(DOUBLON)));
    }

    @Test
    void r2_creation_400_sansCode() throws Exception {
        creer("{\"description\":\"Groupe\",\"lignes\":" + lignes(a, "1") + "}").andExpect(status().isBadRequest());
    }

    @Test
    void r2_creation_400_sansDescription() throws Exception {
        creer("{\"code\":\"GRP-SD-" + jeton + "\",\"lignes\":" + lignes(a, "1") + "}")
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ lecture

    @Test
    void r2_fiche_200_avecSesLignes_et_liste() throws Exception {
        UUID id = entites.groupe("GRP-FI-" + jeton);
        entites.ligneGroupe(id, a, new BigDecimal("0.25"));
        entites.ligneGroupe(id, b, new BigDecimal("0.75"));

        mockMvc.perform(get(GROUPE, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("GRP-FI-" + jeton))
                .andExpect(jsonPath("$..[?(@.actionnaireId=='" + a + "')].pourcentage").value(0.25))
                .andExpect(jsonPath("$..[?(@.actionnaireId=='" + b + "')].pourcentage").value(0.75));
        mockMvc.perform(get(GROUPES).with(connecte(admin)).param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void r2_fiche_404_idInconnu() throws Exception {
        mockMvc.perform(get(GROUPE, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ modification

    @Test
    void r2_patch_lignesValides_remplacentLesAnciennes() throws Exception {
        UUID id = entites.groupe("GRP-MOD-" + jeton);
        entites.ligneGroupe(id, a, BigDecimal.ONE);

        modifier(id, "{\"lignes\":" + lignes(a, "0.3", b, "0.7") + "}").andExpect(status().isOk());

        assertThat(enLignes(entites.lignesGroupe(id))).containsExactlyInAnyOrder(guid(a) + "=0.3", guid(b) + "=0.7");
    }

    @Test
    void r2_patch_sommeDifferenteDe100_400_lignesInchangees() throws Exception {
        UUID id = entites.groupe("GRP-MSOM-" + jeton);
        entites.ligneGroupe(id, a, BigDecimal.ONE);

        modifier(id, "{\"lignes\":" + lignes(a, "0.3", b, "0.3") + "}")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(SOMME)));

        assertThat(enLignes(entites.lignesGroupe(id))).containsExactly(guid(a) + "=1");
    }

    @Test
    void r2_patch_doublon_400() throws Exception {
        UUID id = entites.groupe("GRP-MDBL-" + jeton);
        entites.ligneGroupe(id, a, BigDecimal.ONE);

        modifier(id, "{\"lignes\":" + lignes(b, "0.5", b, "0.5") + "}")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(DOUBLON)));
    }

    // ------------------------------------------------------------------ suppression

    @Test
    void r2_suppression_204_groupeEtLignesSupprimes() throws Exception {
        UUID id = entites.groupe("GRP-DEL-" + jeton);
        entites.ligneGroupe(id, a, BigDecimal.ONE);

        mockMvc.perform(delete(GROUPE, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(entites.compter(COMPTE, id)).isZero();
        assertThat(entites.lignesGroupe(id)).isEmpty();
    }

    /**
     * §2 « piège » : {@code CfgCompany → CfgGroupShareholder} est CASCADE en production — supprimer un groupe choisi
     * par une entité supprimerait l'entité. 409, et l'entité comme le groupe restent.
     */
    @Test
    void r2_suppression_409_groupeChoisiParUneEntite_entiteEtGroupeConserves() throws Exception {
        UUID id = entites.groupe("GRP-409-" + jeton);
        CfgCompany entite = fixture.entite("EJ-GRP409-" + jeton, true);
        entites.groupeDeLEntite(entite.getId(), id);

        mockMvc.perform(delete(GROUPE, id).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(entites.compter(COMPTE, id)).isEqualTo(1);
        assertThat(entites.compter("SELECT COUNT(*) FROM CfgCompany WHERE PKey = :id", entite.getId())).isEqualTo(1);
    }

    @Test
    void r2_suppression_404_idInconnu() throws Exception {
        mockMvc.perform(delete(GROUPE, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }
}
