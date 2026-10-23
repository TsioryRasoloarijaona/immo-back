package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.REMISE_PIECES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revue R4, n° 32 (FEATURE-tdb-vente-rapports-dossiers.md §6 « Population ») : le spec exige
 * {@code PerimetreCommercialService.filtreEncaissements()} en plus des tranches visibles, donc la société directe du
 * règlement ({@code StlSettlement.CfgCompanyId}) doit être active, comme dans la liste des encaissements.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RemisePiecesSocieteReglementRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsDossiers rd;

    @Test
    void n32_reglementDUneSocieteInactive_exclu_commeLaListeDesEncaissements() throws Exception {
        String prefixe = FixtureEtatVentes.prefixe("RPS");
        fixture.tables();
        fixture.rapportVente(REMISE_PIECES, "REMISE DE PIÈCES BANCAIRES", "VENTE", 2100);
        RequestPostProcessor libre = connecte(fixture.compte("rps." + prefixe,
                fixture.role("Libre " + prefixe, false)));

        CfgTranche t = ev.tranche("T-" + prefixe, ev.projet("P-" + prefixe, true), true);
        ComFolder d = ev.vendre(ev.article(prefixe + "-A1", t, ev.categorie("CAT-" + prefixe),
                        ev.type("TYP-" + prefixe)), "DOS-" + prefixe + "-1", ev.statutEnCours("ENC-" + prefixe),
                ev.client("C1-" + prefixe), null);
        ComDocument e = ev.echeance(d, Nature.AVANCE, true, new BigDecimal("900000"));

        LocalDateTime remiseLe = LocalDateTime.of(2026, 3, 10, 0, 0);
        StlSettlement s = rd.reglement(new BigDecimal("100000"), true, null, remiseLe, remiseLe);
        s.setIsDcoumentDeliveredToCenter(true);
        s.setDeliveredToCenterDate(remiseLe);
        s.setAdmModuleId(rd.moduleVente());
        s.setItemNumber(prefixe + "-INACTIF");
        s.setStatus("POINTED");
        s.setCfgCompany(ev.projet("PI-" + prefixe, false).getCfgCompany());
        rd.ligne(s, e, new BigDecimal("100000"));
        fixture.relire();

        List<Object> articles = lignes(section(new AppelsRapports(mockMvc).sections(
                get(resultat(VENTE, REMISE_PIECES)).param("du", "2026-03-01").param("au", "2026-03-31")
                        .with(libre)), "remises")).stream().map(l -> l.get("article")).toList();

        assertThat(articles).doesNotContain(prefixe + "-INACTIF");
    }
}
