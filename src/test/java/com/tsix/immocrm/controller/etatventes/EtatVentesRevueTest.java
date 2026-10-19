package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.repositories.stk.StkItemRepository;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue de FEATURE-etat-des-ventes.md : R4 sur groupes / totaux, §7.7, §7.8, §7.9. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesRevueTest {

    private static final String URL = "/api/etat-des-ventes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COLONNES = "colonnes";
    private static final String TOTAL_ELEMENTS = "$.totalElements";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;
    @Autowired
    private FixtureDossierVente fixtureDossier;
    @Autowired
    private StkItemRepository articles;

    private String prefixe;
    private CfgTranche tranche;
    private StkItem reserve;
    private StkItem annule;
    private CfgTier clientAnnule;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVR");
        tranche = fixture.tranche("TR-" + prefixe, fixture.projet("PRJ-" + prefixe, true), true);
        StkItemCategory appartement = fixture.categorie("APP-" + prefixe);
        StkItemType type = fixture.type("T3-" + prefixe);
        fixture.article(prefixe + "-A1", tranche, appartement, type);
        reserve = fixture.article(prefixe + "-A2", tranche, appartement, type);
        fixture.vendre(reserve, "DOS-" + prefixe + "-A2", fixture.statut(StatutDossier.RESERVATION),
                fixture.client("ZRES" + prefixe), LocalDateTime.of(2025, 3, 15, 10, 0));
        annule = fixture.article(prefixe + "-A3", tranche, appartement, type);
        clientAnnule = fixture.client("ZANN" + prefixe);
        fixture.vendre(annule, "DOS-" + prefixe + "-A3", fixture.statutSortie("ANN-" + prefixe, true), clientAnnule,
                LocalDateTime.of(2025, 4, 15, 10, 0));
        fixture.relire();
    }

    private String article(StkItem a) {
        return "$.content[?(@.articleId=='" + a.getId() + "')]";
    }

    // ------------------------------------------------------------------ §7.7 montants

    @Test
    void montants_vendu_regleEtReste_libre_null() throws Exception {
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .param(COLONNES, "articleId,prixVenteTTC,montantRegle,resteARegler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(article(reserve) + ".prixVenteTTC").value(Matchers.contains(1200000.0)))
                .andExpect(jsonPath(article(reserve) + ".montantRegle").value(Matchers.contains(
                        Matchers.anyOf(Matchers.<Object>equalTo(0), Matchers.<Object>equalTo(0.0)))))
                .andExpect(jsonPath(article(reserve) + ".resteARegler").value(Matchers.contains(1200000.0)));
    }

    @Test
    void totaux_montants_surLeFiltre() throws Exception {
        mockMvc.perform(get(URL + "/totaux-colonnes").param(TRANCHE_ID, tranche.getId().toString())
                        .param("agregats", "prixVenteTTC", "montantRegle", "resteARegler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(3))
                .andExpect(jsonPath("$.totaux.montantRegle").value(0))
                .andExpect(jsonPath("$.totaux.resteARegler").value(1200000.0));
    }

    /** Dossier retenant deux articles racines (3 cas en base) : le Σ du reste doit égaler la somme de la colonne. */
    @Test
    void totaux_reste_egaleSommeDesLignes_dossierADeuxArticles() throws Exception {
        StkItem second = fixture.article(prefixe + "-A4", tranche, fixture.categorie("PAR-" + prefixe),
                fixture.type("PK-" + prefixe));
        second.setComFolderId(articles.findById(reserve.getId()).orElseThrow().getComFolderId());
        articles.save(second);
        fixture.relire();

        String corps = mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .param(COLONNES, "articleId,resteARegler"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> restes = JsonPath.read(corps, "$.content[*].resteARegler");
        double sommeLignes = restes.stream().filter(Objects::nonNull).mapToDouble(Number::doubleValue).sum();

        mockMvc.perform(get(URL + "/totaux-colonnes").param(TRANCHE_ID, tranche.getId().toString())
                        .param("agregats", "resteARegler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux.resteARegler").value(sommeLignes));
    }

    // ------------------------------------------------------------------ §7.8 dossier annulé : aucune donnée

    @Test
    void annule_aucuneDonneeDuDossierAffichee() throws Exception {
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()).param(COLONNES,
                        "articleId,statutCode,dossierId,dossierCode,clientNom,dateReservation,montantRegle,"
                                + "resteARegler,prixVenteTTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(article(annule) + ".statutCode").value(Matchers.contains("LIBRE")))
                .andExpect(jsonPath(article(annule) + ".dossierId").value(Matchers.contains(Matchers.nullValue())))
                .andExpect(jsonPath(article(annule) + ".clientNom").value(Matchers.contains(Matchers.nullValue())))
                .andExpect(jsonPath(article(annule) + ".dateReservation")
                        .value(Matchers.contains(Matchers.nullValue())))
                .andExpect(jsonPath(article(annule) + ".montantRegle").value(Matchers.contains(Matchers.nullValue())))
                .andExpect(jsonPath(article(annule) + ".prixVenteTTC")
                        .value(Matchers.contains(FixtureEtatVentes.PRIX_CATALOGUE.doubleValue())));
    }

    @Test
    void annule_niCherchableParSonClient_niFiltreParSaDate() throws Exception {
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .param("search", clientAnnule.getLastName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(0));
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .param("du", "2025-04-01").param("au", "2025-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(0));
    }

    // ------------------------------------------------------------------ §7.9 400

    @Test
    void statutInconnu_rend400_surListeGroupesTotaux() throws Exception {
        for (String url : new String[] {URL, URL + "/groupes", URL + "/totaux-colonnes"}) {
            mockMvc.perform(get(url).param("statut", "PAS_UN_STATUT").param("groupBy", "statutCode"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void natureInconnue_rend400() throws Exception {
        mockMvc.perform(get(URL).param("nature", "PAS_UNE_NATURE"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ R4 sur toutes les lectures

    @Test
    void r4_commercialHorsPerimetre_groupesEtTotauxVides() throws Exception {
        AdmUser commercial = fixtureDossier.compte("edvr.com." + prefixe);
        fixture.affecter(commercial, fixture.projet("PRJB-" + prefixe, true));
        fixture.relire();

        mockMvc.perform(get(URL + "/groupes").param(TRANCHE_ID, tranche.getId().toString())
                        .param("groupBy", "statutCode").with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get(URL + "/totaux-colonnes").param(TRANCHE_ID, tranche.getId().toString())
                        .param("agregats", "surface", "montantRegle", "resteARegler")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(0))
                .andExpect(jsonPath("$.totaux.resteARegler").value(0));
    }
}
