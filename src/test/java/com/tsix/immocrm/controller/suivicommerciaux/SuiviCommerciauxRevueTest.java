package com.tsix.immocrm.controller.suivicommerciaux;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.JOUR;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.a;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.cle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue adversariale du lot (FEATURE-suivi-commerciaux.md R8, R11, §9.8, §3) : chemins non couverts par les tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviCommerciauxRevueTest {

    private static final String URL_MENSUEL = "/api/suivi-commerciaux/mensuel";
    private static final String COMMERCIAL = "Commercial";
    private static final String CHEF = "Commercial Chef";
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
    private AdmUserHabilitationRepository habilitations;

    private static LocalDateTime enMars(int jour) {
        return LocalDate.of(ANNEE, MARS, jour).atTime(10, 0);
    }

    private void habiliterSurTranche(AdmUser compte, CfgTranche tranche) {
        AdmUserHabilitation h = new AdmUserHabilitation();
        h.setId(UUID.randomUUID());
        h.setAdmUser(compte);
        h.setCfgProject(tranche.getCfgProject());
        h.setCfgTranche(tranche);
        habilitations.save(h);
    }

    private double nbProspectionsDuMois(AdmUser appelant, String role, AdmUser commercial) throws Exception {
        String corps = mockMvc.perform(get(URL_MENSUEL).param("annee", String.valueOf(ANNEE))
                        .param("mois", String.valueOf(MARS)).param("commercialIds", cle(commercial))
                        .with(connecte(appelant, role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> valeurs = JsonPath.read(corps,
                "$.mois.lignes[?(@.commercialCle=='" + cle(commercial) + "')].nbProspections");
        assertThat(valeurs).hasSize(1);
        return valeurs.getFirst().doubleValue();
    }

    // ------------------------------------------------------------------ §9.8 : sans trancheIds, tranches habilitées

    /** §9.8 : « trancheIds absent = tranches habilitées de l'utilisateur, Administrateur sans habilitation = toutes ». */
    @Test
    void mensuel_sansTranche_administrateurHabiliteSurUneTranche_neCompteQueCelleCi() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SCR");
        CfgTranche habilitee = fixture.tranche("TRH-" + p, true);
        CfgTranche autre = fixture.tranche("TRA-" + p, true);
        AdmUser admin = fixture.compte("scr.adm." + p, ADMINISTRATEUR);
        AdmUser commercial = fixture.compte("scr.com." + p, COMMERCIAL);
        habiliterSurTranche(admin, habilitee);
        fixture.prospection(habilitee, fixture.personne(p + "-P1"), commercial, enMars(5));
        fixture.prospection(autre, fixture.personne(p + "-P2"), commercial, enMars(6));
        fixture.relire();

        assertThat(nbProspectionsDuMois(admin, ADMINISTRATEUR, commercial)).isEqualTo(1.0);
    }

    /** §9.8 / OMRAN l.158 : le filtre porte sur les tranches habilitées, pas sur les projets qui les contiennent. */
    @Test
    void mensuel_sansTranche_commercialHabiliteSurUneTrancheDuProjet_neCompteQueCelleCi() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SCR");
        CfgProject projet = etatVentes.projet("PRJ-" + p, true);
        CfgTranche habilitee = etatVentes.tranche("TRH-" + p, projet, true);
        CfgTranche voisine = etatVentes.tranche("TRV-" + p, projet, true);
        AdmUser commercial = fixture.compte("scr.com." + p, COMMERCIAL);
        habiliterSurTranche(commercial, habilitee);
        fixture.prospection(habilitee, fixture.personne(p + "-P1"), commercial, enMars(5));
        fixture.prospection(voisine, fixture.personne(p + "-P2"), commercial, enMars(6));
        fixture.relire();

        assertThat(nbProspectionsDuMois(commercial, COMMERCIAL, commercial)).isEqualTo(1.0);
    }

    // ------------------------------------------------------------------ R8 sur le mensuel

    @Test
    void mensuel_commercialDemandantUnAutre_rend403() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SCR");
        AdmUser moi = fixture.compte("scr.moi." + p, COMMERCIAL);
        AdmUser autre = fixture.compte("scr.aut." + p, COMMERCIAL);
        fixture.relire();

        mockMvc.perform(get(URL_MENSUEL).param("annee", String.valueOf(ANNEE)).param("mois", String.valueOf(MARS))
                        .param("commercialIds", cle(autre)).with(connecte(moi, COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    /** Règle 6 : un chef ne voit ni les ventes ni les versements d'un membre sur un projet hors de son périmètre. */
    @Test
    void mensuel_chef_ventesEtVersementsDUnMembreHorsDeSesProjets_exclus() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SCR");
        CfgTranche commune = fixture.tranche("TRC-" + p, true);
        CfgTranche horsChef = fixture.tranche("TRM-" + p, true);
        AdmUser chef = fixture.compte("scr.chef." + p, CHEF);
        AdmUser membre = fixture.compte("scr.mbr." + p, COMMERCIAL);
        fixture.affecter(chef, commune);
        fixture.affecter(membre, commune);
        fixture.affecter(membre, horsChef);
        ComFolder vente = fixture.vente(p + "-V", membre, horsChef, null, enMars(10), enMars(1),
                new BigDecimal("900000"));
        fixture.versement(vente, "100000", true, true, null, enMars(15));
        fixture.relire();

        String corps = mockMvc.perform(get(URL_MENSUEL).param("annee", String.valueOf(ANNEE))
                        .param("mois", String.valueOf(MARS)).param("commercialIds", cle(membre))
                        .with(connecte(chef, CHEF)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Number ventes = JsonPath.read(corps, "$.mois.lignes[0].nbVentes");
        Number versements = JsonPath.read(corps, "$.mois.lignes[0].versements");
        assertThat(List.of(ventes.doubleValue(), versements.doubleValue())).containsExactly(0.0, 0.0);
    }

    /** R8 : intersection des projets — l'appel d'un membre sur un projet du membre seul n'est pas compté au chef. */
    @Test
    void synthese_chef_appelDUnMembreHorsDeSesProjets_nonCompte() throws Exception {
        String p = FixtureSuiviCommerciaux.prefixe("SCR");
        CfgTranche commune = fixture.tranche("TRC-" + p, true);
        CfgTranche horsChef = fixture.tranche("TRM-" + p, true);
        AdmUser chef = fixture.compte("scr.chef." + p, CHEF);
        AdmUser membre = fixture.compte("scr.mbr." + p, COMMERCIAL);
        fixture.affecter(chef, commune);
        fixture.affecter(membre, commune);
        fixture.affecter(membre, horsChef);
        fixture.interaction(membre, fixture.prospection(horsChef, p + "-PM"), fixture.appelSortant(),
                fixture.resultat(FixtureSuiviCommerciaux.RDV_PRIS), a(JOUR, 10, 0));
        fixture.relire();

        mockMvc.perform(get("/api/suivi-commerciaux/synthese").param("du", JOUR.toString()).param("au", JOUR.toString())
                        .param("commercialIds", cle(membre)).with(connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].appelsPasses").value(0));
    }

    // ------------------------------------------------------------------ §3 : objectif entier

    @Test
    void objectif_valeurDecimale_rend400() throws Exception {
        AdmUser admin = fixture.compte("scr.adm." + FixtureSuiviCommerciaux.prefixe("SCR"), ADMINISTRATEUR);

        mockMvc.perform(put("/api/suivi-commerciaux/objectif").with(connecte(admin, ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"objectifPersonnesJointesJour\":45.7}"))
                .andExpect(status().isBadRequest());
    }
}
