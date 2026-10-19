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
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Répartition des actions d'une entité — FEATURE-entites-juridiques.md §2 (« Répartition »), règles OMRAN
 * {@code CfgCompany.cs:Verif} l.1249-1260 et {@code SetRepartitions} l.1314-1352 (RepartitionTest).
 *
 * <p>Corps du PUT : la liste elle-même, {@code [{actionnaireId, nombreParts}]}. {@code SharingNumber} (nombre de
 * parts) et {@code TotalSharing} (nombre de répartitions) sont posés en SQL : le spec ne nomme pas leurs champs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepartitionTest {

    private static final String REPARTITION = "/api/entites-juridiques/{id}/repartition";
    private static final String DEPUIS_GROUPE = "/api/entites-juridiques/{id}/repartition/depuis-groupe/{groupeId}";
    private static final String DOUBLON = "L'actionnaire existe déjà dans la liste !!!";
    private static final String TOTAL_PARTS =
            "Le nombre de parts (actions) doit être égal au total des nombre de parts / actions dans la répartition !";
    private static final String NOMBRE_REPARTITIONS = "Le nombre de répartitions doit être égal au nombre d'actionnaires !";
    private static final String PARTS = "SharingNumber";
    private static final String ACTIONNAIRE = "CfgShareholderId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private UUID a;
    private UUID b;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = fixture.entite("EJ-REP-" + jeton, true);
        a = entites.actionnaire("ACT-A-" + jeton);
        b = entites.actionnaire("ACT-B-" + jeton);
    }

    /** {@code paires} = actionnaire, parts, actionnaire, parts… */
    private static String liste(Object... paires) {
        StringJoiner json = new StringJoiner(",", "[", "]");
        for (int i = 0; i + 1 < paires.length; i += 2) {
            json.add("{\"actionnaireId\":\"" + paires[i] + "\",\"nombreParts\":" + paires[i + 1] + "}");
        }
        return json.toString();
    }

    private ResultActions remplacer(UUID entiteId, String corps) throws Exception {
        return mockMvc.perform(put(REPARTITION, entiteId).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ResultActions depuisGroupe(UUID groupeId) throws Exception {
        return mockMvc.perform(post(DEPUIS_GROUPE, entite.getId(), groupeId).with(connecte(admin)));
    }

    private static List<String> enLignes(List<Tuple> lignes) {
        return lignes.stream().map(t -> guid(t.get(ACTIONNAIRE)) + "=" + t.get(PARTS)).toList();
    }

    private static List<Integer> parts(String json, UUID actionnaire) {
        return JsonPath.read(json, "$..[?(@.actionnaireId=='" + actionnaire + "')].nombreParts");
    }

    // ------------------------------------------------------------------ PUT

    @Test
    void r2_put_repartitionValide_ecriteDansCfgCompanyShareholder() throws Exception {
        entites.parts(entite.getId(), 100, 2);

        remplacer(entite.getId(), liste(a, 60, b, 40)).andExpect(status().isOk());

        assertThat(enLignes(entites.repartitions(entite.getId())))
                .containsExactlyInAnyOrder(guid(a) + "=60", guid(b) + "=40");
    }

    @Test
    void r2_put_remplaceLaRepartitionPrecedente() throws Exception {
        entites.parts(entite.getId(), 100, 2);
        entites.repartition(entite.getId(), a, 100);

        remplacer(entite.getId(), liste(a, 70, b, 30)).andExpect(status().isOk());

        assertThat(enLignes(entites.repartitions(entite.getId())))
                .containsExactlyInAnyOrder(guid(a) + "=70", guid(b) + "=30");
    }

    @Test
    void r2_put_doublon_400_rienEcrit() throws Exception {
        entites.parts(entite.getId(), 100, 2);

        remplacer(entite.getId(), liste(a, 60, a, 40))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(DOUBLON)));

        assertThat(entites.repartitions(entite.getId())).isEmpty();
    }

    @Test
    void r2_verif_put_totalDesPartsDifferentDuNombreDeParts_400() throws Exception {
        entites.parts(entite.getId(), 100, 2);

        remplacer(entite.getId(), liste(a, 60, b, 30))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(TOTAL_PARTS)));

        assertThat(entites.repartitions(entite.getId())).isEmpty();
    }

    /** {@code Verif} l.1252 : première règle, {@code TotalSharing} = nombre de lignes. */
    @Test
    void r2_verif_put_nombreDeRepartitionsDifferentDuNombreDActionnaires_400() throws Exception {
        entites.parts(entite.getId(), 100, 3);

        remplacer(entite.getId(), liste(a, 60, b, 40))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(NOMBRE_REPARTITIONS)));
    }

    /** §2 : Verif seulement quand la répartition est non vide. */
    @Test
    void r2_verif_put_listeVide_200_repartitionEffacee() throws Exception {
        entites.parts(entite.getId(), 100, 2);
        entites.repartition(entite.getId(), a, 100);

        remplacer(entite.getId(), "[]").andExpect(status().isOk());

        assertThat(entites.repartitions(entite.getId())).isEmpty();
    }

    @Test
    void r2_put_404_entiteInconnue() throws Exception {
        remplacer(UUID.randomUUID(), liste(a, 10)).andExpect(status().isNotFound());
    }

    /** §2 : Verif « à l'enregistrement de la fiche et du PUT » — la fiche change le nombre de parts. */
    @Test
    void r2_verif_patchFiche_nombreDePartsDevenuFaux_400() throws Exception {
        entites.parts(entite.getId(), 100, 2);
        entites.repartition(entite.getId(), a, 60);
        entites.repartition(entite.getId(), b, 40);
        entites.parts(entite.getId(), 120, 2);

        mockMvc.perform(patch("/api/entites-juridiques/{id}", entite.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Autre libelle\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(TOTAL_PARTS)));
    }

    // ------------------------------------------------------------------ depuis un groupe (SetRepartitions)

    @Test
    void r2_depuisGroupe_partsAuProrata_laDerniereLigneRecoitLeReste() throws Exception {
        entites.parts(entite.getId(), 1000, 2);
        UUID groupe = entites.groupe("GRP-REP-" + jeton);
        entites.ligneGroupe(groupe, a, new BigDecimal("0.25"));
        entites.ligneGroupe(groupe, b, new BigDecimal("0.75"));

        String json = depuisGroupe(groupe).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(List.of(parts(json, a), parts(json, b))).containsExactly(List.of(250), List.of(750));
    }

    /**
     * {@code (int)(pourcentage × 100) × SharingNumber / 100} tronque : 33,5 % de 100 → 33, pas 34 ; la dernière ligne
     * prend le reste (34). Un arrondi au plus proche donnerait 34 + 34 + 32.
     */
    @Test
    void r2_depuisGroupe_troncatureOmran_puisResteSurLaDerniere() throws Exception {
        entites.parts(entite.getId(), 100, 3);
        UUID c = entites.actionnaire("ACT-C-" + jeton);
        UUID groupe = entites.groupe("GRP-TRC-" + jeton);
        entites.ligneGroupe(groupe, a, new BigDecimal("0.335"));
        entites.ligneGroupe(groupe, b, new BigDecimal("0.335"));
        entites.ligneGroupe(groupe, c, new BigDecimal("0.33"));

        String json = depuisGroupe(groupe).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Integer> toutes = JsonPath.read(json, "$..nombreParts");
        assertThat(toutes).containsExactlyInAnyOrder(33, 33, 34);
    }

    @Test
    void r2_depuisGroupe_uneSeuleLigne_toutesLesParts() throws Exception {
        entites.parts(entite.getId(), 500, 1);
        UUID groupe = entites.groupe("GRP-UN-" + jeton);
        entites.ligneGroupe(groupe, a, BigDecimal.ONE);

        String json = depuisGroupe(groupe).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(parts(json, a)).containsExactly(500);
    }

    @Test
    void r2_depuisGroupe_zeroPart_toutesLesLignesAZero() throws Exception {
        entites.parts(entite.getId(), 0, 2);
        UUID groupe = entites.groupe("GRP-ZERO-" + jeton);
        entites.ligneGroupe(groupe, a, new BigDecimal("0.4"));
        entites.ligneGroupe(groupe, b, new BigDecimal("0.6"));

        String json = depuisGroupe(groupe).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Integer> toutes = JsonPath.read(json, "$..nombreParts");
        assertThat(toutes).containsExactly(0, 0);
    }

    @Test
    void r2_depuisGroupe_404_entiteInconnue() throws Exception {
        UUID groupe = entites.groupe("GRP-404-" + jeton);

        mockMvc.perform(post(DEPUIS_GROUPE, UUID.randomUUID(), groupe).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
