package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheRegles;
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

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglet Finalisations ({@code CfgTrancheFolderEntity}) — FEATURE-patrimoine-tranche-regles.md §1, §7
 * (TrancheFinalisationsTest), §9.3 (PUT liste réconcilié ; doublon d'entité → 400, pas ignoré comme en P1b).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheFinalisationsTest {

    private static final String FINALISATIONS = "/api/tranches/{id}/finalisations";
    private static final String ENTITE = "entiteDossierId";
    private static final String STATUT_1 = "statutFinalisation1Id";
    private static final String STATUT_2 = "statutFinalisation2Id";
    private static final String DE_LA_TRANCHE = "SELECT COUNT(*) FROM CfgTrancheFolderEntity WHERE CfgTrancheId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheRegles regles;

    private AdmUser admin;
    private CfgTranche tranche;
    private UUID entite;
    private UUID formalise;
    private UUID remiseDesClefs;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        String jeton = "P1CF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
        entite = regles.entiteDossierVente();
        formalise = regles.statutDossier(StatutDossier.FORMALISE);
        remiseDesClefs = regles.statutDossier(StatutDossier.REMISE_DES_CLEFS);
    }

    private ResultActions envoyer(String corps) throws Exception {
        return mockMvc.perform(put(FINALISATIONS, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private CorpsJson ligne(UUID statut1, UUID statut2) {
        return corps().avec(ENTITE, entite).avec(STATUT_1, statut1).avec(STATUT_2, statut2);
    }

    @Test
    void r1_get_lignesDeLaTranche() throws Exception {
        regles.ligneFinalisation(tranche.getId(), entite, formalise, remiseDesClefs);

        mockMvc.perform(get(FINALISATIONS, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[*]." + ENTITE, hasItem(entite.toString())))
                .andExpect(jsonPath("$.lignes[*]." + STATUT_1, hasItem(formalise.toString())))
                .andExpect(jsonPath("$.lignes[*]." + STATUT_2, hasItem(remiseDesClefs.toString())));
    }

    @Test
    void r1_put_ecritLesColonnesOmran() throws Exception {
        envoyer(lignes(ligne(formalise, remiseDesClefs))).andExpect(status().isOk());

        assertThat(lignesEnBase()).singleElement().satisfies(l -> assertAll(
                () -> assertThat(FixturePatrimoine.guid(l.get("StrFolderEntityId"))).isEqualTo(FixturePatrimoine.guid(entite)),
                () -> assertThat(FixturePatrimoine.guid(l.get("StrStatusDeliveryId1")))
                        .isEqualTo(FixturePatrimoine.guid(formalise)),
                () -> assertThat(FixturePatrimoine.guid(l.get("StrStatusDeliveryId2")))
                        .isEqualTo(FixturePatrimoine.guid(remiseDesClefs))));
    }

    @Test
    void r1_put_idConnuMaj_absentSupprime() throws Exception {
        UUID modifiee = regles.ligneFinalisation(tranche.getId(), entite, formalise, formalise);
        UUID retiree = regles.ligneFinalisation(tranche.getId(), entite, remiseDesClefs, remiseDesClefs);

        envoyer(lignes(ligne(formalise, remiseDesClefs).avec("id", modifiee))).andExpect(status().isOk());

        assertThat(lignesEnBase()).singleElement().satisfies(l -> assertAll(
                () -> assertThat(FixturePatrimoine.guid(l.get("PKey"))).isEqualTo(FixturePatrimoine.guid(modifiee))
                        .isNotEqualTo(FixturePatrimoine.guid(retiree)),
                () -> assertThat(FixturePatrimoine.guid(l.get("StrStatusDeliveryId2")))
                        .isEqualTo(FixturePatrimoine.guid(remiseDesClefs))));
    }

    private List<Tuple> lignesEnBase() {
        return regles.lignes("SELECT * FROM CfgTrancheFolderEntity WHERE CfgTrancheId = :id", tranche.getId());
    }

    @Test
    void r1_put_listeVide_supprimeLesLignes() throws Exception {
        regles.ligneFinalisation(tranche.getId(), entite, formalise, remiseDesClefs);

        envoyer(lignes()).andExpect(status().isOk());

        assertThat(regles.compter(DE_LA_TRANCHE, tranche.getId())).isZero();
    }

    /** §1 « Unicité par entité dans la tranche (400 sinon) » ; §9.3 option « doublon de clé → 400 ». */
    @Test
    void r1_memeEntiteDeuxFois_400() throws Exception {
        envoyer(lignes(ligne(formalise, remiseDesClefs), ligne(remiseDesClefs, formalise)))
                .andExpect(status().isBadRequest());
    }
}
