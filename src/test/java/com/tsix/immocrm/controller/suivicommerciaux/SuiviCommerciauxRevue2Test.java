package com.tsix.immocrm.controller.suivicommerciaux;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.cle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seconde revue (règle 6, FEATURE-suivi-commerciaux.md R8, R11, §9.8, §9.9) : entité inactive, sorties, 403 des routes de liste. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviCommerciauxRevue2Test {

    private static final String URL = "/api/suivi-commerciaux";
    private static final String COMMERCIAL = "Commercial";
    private static final String COMMERCIAL_CLE = "commercialCle";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final int ANNEE = 2025;
    private static final int MARS = 3;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviCommerciaux fixture;
    @Autowired
    private FixtureEtatVentes etatVentes;
    @Autowired
    private ComFolderRepository dossiers;

    private static LocalDateTime enMars(int jour) {
        return LocalDate.of(ANNEE, MARS, jour).atTime(10, 0);
    }

    private Map<String, Object> ligneDuMois(AdmUser appelant, String role, AdmUser commercial) throws Exception {
        String corps = mockMvc.perform(get(URL + "/mensuel").param("annee", String.valueOf(ANNEE))
                        .param("mois", String.valueOf(MARS)).param("commercialIds", cle(commercial))
                        .with(connecte(appelant, role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> lignes = JsonPath.read(corps,
                "$.mois.lignes[?(@.commercialCle=='" + cle(commercial) + "')]");
        assertThat(lignes).hasSize(1);
        return lignes.getFirst();
    }

    private static double nombre(Map<String, Object> ligne, String champ) {
        assertThat(ligne.get(champ)).as(champ).isInstanceOf(Number.class);
        return ((Number) ligne.get(champ)).doubleValue();
    }

    /** R8 : prospection, visite, vente et versement d'une entité inactive ne comptent pour personne, Administrateur compris. */
    @Test
    void mensuel_entiteInactive_rienNEstCompte() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SR2");
        CfgTranche inactive = fixture.tranche("TRI-" + p, false);
        AdmUser admin = fixture.compte("sr2.adm." + p, ADMINISTRATEUR);
        AdmUser commercial = fixture.compte("sr2.com." + p, COMMERCIAL);
        ComProspection prospection = fixture.prospection(inactive, fixture.personne(p + "-P"), commercial, enMars(5));
        fixture.interaction(commercial, prospection, fixture.visite(), null, enMars(6));
        ComFolder vente = fixture.vente(p + "-V", commercial, inactive, prospection, enMars(10), enMars(1),
                new BigDecimal("900000"));
        fixture.versement(vente, "100000", true, true, null, enMars(15));
        fixture.relire();

        Map<String, Object> ligne = ligneDuMois(admin, ADMINISTRATEUR, commercial);
        assertThat(List.of(nombre(ligne, "nbProspections"), nombre(ligne, "nbVisitesProspection"),
                nombre(ligne, "nbVentes"), nombre(ligne, "prixArticles"), nombre(ligne, "versements")))
                .containsExactly(0.0, 0.0, 0.0, 0.0, 0.0);
    }

    /** §9.8 : un dossier annulé (BoolField002) ou désisté (BoolField003) n'est ni une vente ni un prix d'article. */
    @Test
    void mensuel_dossiersAnnuleEtDesiste_exclusDesVentes() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SR2");
        CfgTranche tranche = fixture.tranche("TR-" + p, true);
        AdmUser admin = fixture.compte("sr2.adm." + p, ADMINISTRATEUR);
        AdmUser commercial = fixture.compte("sr2.com." + p, COMMERCIAL);
        fixture.vente(p + "-OK", commercial, tranche, null, enMars(10), enMars(1), new BigDecimal("500000"));
        ComFolder annule = fixture.vente(p + "-AN", commercial, tranche, null, enMars(11), enMars(1),
                new BigDecimal("700000"));
        annule.setStrStatus(etatVentes.statutSortie("ANN-" + p, true));
        dossiers.save(annule);
        ComFolder desiste = fixture.vente(p + "-DS", commercial, tranche, null, enMars(12), enMars(1),
                new BigDecimal("800000"));
        desiste.setStrStatus(etatVentes.statutSortie("DES-" + p, false));
        dossiers.save(desiste);
        fixture.relire();

        Map<String, Object> ligne = ligneDuMois(admin, ADMINISTRATEUR, commercial);
        assertThat(List.of(nombre(ligne, "nbVentes"), nombre(ligne, "prixArticles")))
                .containsExactly(1.0, 500_000.0);
    }

    /** §9.9 : 403 hors portée aussi sur /groupes, /totaux-colonnes et /export. */
    @Test
    void routesDeListe_commercialDemandantUnAutre_rendent403() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SR2");
        AdmUser moi = fixture.compte("sr2.moi." + p, COMMERCIAL);
        AdmUser autre = fixture.compte("sr2.aut." + p, COMMERCIAL);
        fixture.relire();

        mockMvc.perform(get(URL + "/groupes").param("groupBy", "commercialNom")
                        .param(COMMERCIAL_CLE, cle(autre)).with(connecte(moi, COMMERCIAL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/totaux-colonnes").param(COMMERCIAL_CLE, cle(autre))
                        .with(connecte(moi, COMMERCIAL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/export").param(COMMERCIAL_CLE, cle(autre)).with(connecte(moi, COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    /** R8 : sans filtre, les groupes d'un commercial ne contiennent que lui. */
    @Test
    void groupes_sansFiltre_commercialNeVoitQueLui() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SR2");
        CfgTranche tranche = fixture.tranche("TR-" + p, true);
        AdmUser moi = fixture.compte("sr2.moi." + p, COMMERCIAL);
        AdmUser autre = fixture.compte("sr2.aut." + p, COMMERCIAL);
        fixture.interaction(moi, fixture.prospection(tranche, p + "-PM"), fixture.appelSortant(),
                fixture.resultat(FixtureSuiviCommerciaux.RDV_PRIS), FixtureSuiviCommerciaux.a(FixtureSuiviCommerciaux.JOUR, 10, 0));
        fixture.interaction(autre, fixture.prospection(tranche, p + "-PA"), fixture.appelSortant(),
                fixture.resultat(FixtureSuiviCommerciaux.RDV_PRIS), FixtureSuiviCommerciaux.a(FixtureSuiviCommerciaux.JOUR, 11, 0));
        fixture.relire();

        String corps = mockMvc.perform(get(URL + "/groupes").param("groupBy", "commercialNom")
                        .param("jour", FixtureSuiviCommerciaux.JOUR.toString()).with(connecte(moi, COMMERCIAL)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(corps).contains(moi.getDescription()).doesNotContain(autre.getDescription());
    }
}
