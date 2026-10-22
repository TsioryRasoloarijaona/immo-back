package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Toutes les dates » de la visionneuse ({@code du=1900-01-01&au=2100-12-31}) sur le n° 13 : bornes hors de la plage
 * d'un {@code smalldatetime} (AdvanceDate), acceptées sans débordement, dossier du jour {@code au} compris.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepartitionClientsBornesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private StrStatusRepository statuts;
    @Autowired private CfgTierRepository tiers;
    @Autowired private CfgTierTypeRepository typesTiers;

    @Test
    void toutesLesDates_bornes1900Et2100_200_etDossierDuDernierJourCompte() throws Exception {
        String prefixe = FixtureEtatVentes.prefixe("RCB");
        fixture.tables();
        fixture.rapportVente(RepartitionClientsRapport.CODE, "REPARTITION DES CLIENTS", "", 1100);
        CfgTranche tranche = ev.tranche("TR-" + prefixe, ev.projet("P-" + prefixe, true), true);
        StrStatus vendu = ev.statutEnCours("VENDU-" + prefixe);
        vendu.setVenteEffective(true);
        vendu = statuts.save(vendu);
        CfgTierType typeClient = typesTiers.findByCode("CLI").orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode("CLI");
            t.setDescription("CLIENT");
            return typesTiers.save(t);
        });
        CfgTier client = ev.client("C-" + prefixe);
        client.setCfgTierType(typeClient);
        tiers.save(client);
        ComFolder dossier = ev.vendre(ev.article(prefixe + "-1", tranche, ev.categorie("CAT-" + prefixe),
                ev.type("TYP-" + prefixe)), "DOS-" + prefixe, vendu, client, null);
        dossier.setAdvanceDate(LocalDateTime.of(2020, 6, 15, 14, 30));
        ev.enregistrer(dossier);
        var libre = connecte(fixture.compte("rcb." + prefixe, fixture.role("Libre " + prefixe, false)));
        fixture.relire();

        mockMvc.perform(get("/api/rapports/vente/" + RepartitionClientsRapport.CODE + "/resultat")
                        .param("du", "1900-01-01").param("au", "2100-12-31")
                        .param("trancheId", tranche.getId().toString()).with(libre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[?(@.id == 'professions')].totaux.nbClients").value(1));
        mockMvc.perform(get("/api/rapports/vente/" + RepartitionClientsRapport.CODE + "/resultat")
                        .param("du", "2020-06-15").param("au", "2020-06-15")
                        .param("trancheId", tranche.getId().toString()).with(libre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[?(@.id == 'professions')].totaux.nbClients").value(1));
    }
}
