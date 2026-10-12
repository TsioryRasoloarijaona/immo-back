package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.cfg.CfgTrancheStatus;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.fixtures.FixtureFichePatrimoine;
import com.tsix.immocrm.fixtures.FixtureOngletsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheCoveredFloorRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheLevelRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheSettlementCategoryRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStatusRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStkItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-tranche-onglets.md §3 et §7. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChargerDepuisTrancheTest {

    private static final String URL = "/api/tranches/{id}/charger-depuis/{sourceId}";
    private static final LocalDateTime DATE_CIBLE = LocalDateTime.of(2026, 3, 1, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureFichePatrimoine fiche;
    @Autowired private FixtureOngletsTranche onglets;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTrancheLevelRepository etageRepository;
    @Autowired private CfgTrancheCoveredFloorRepository plancherRepository;
    @Autowired private CfgTrancheSettlementCategoryRepository fraisRepository;
    @Autowired private CfgTrancheStatusRepository seuilRepository;
    @Autowired private CfgTrancheStkItemRepository terrainRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgTranche source;
    private CfgTranche cible;
    private LndLevel niveau;
    private StkItemCategory appartement;
    private StkItemCategory villa;
    private ComSettlementCategory notaire;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-TRA-CHG");
        CfgProject projet = fixture.projet("PRJ-TRA-CHG", fixture.entite("ENT-TRA-CHG", true));
        source = fixture.tranche("TRA-CHG-SRC", projet);
        cible = fixture.tranche("TRA-CHG-CIB", projet);
        niveau = fiche.niveau("TST-R+4");
        appartement = fixture.categorie("TST-APP-CHG", fixture.natureBatiment(), false);
        villa = fixture.categorie("TST-VIL-CHG", fixture.natureBatiment(), false);
        notaire = onglets.categorieReglement("TST-NOT-CHG", true, true);

        source.setAdvancePercent(new BigDecimal("0.3"));
        source.setSyndicAmount(new BigDecimal("1500"));
        source.setCity("Casablanca");
        source.setIsWithGiftForProposer(true);
        source.setAgreementFirstDelay(45);
        source.setWithdrawalDelay(10);
        source.setCreditPercent(new BigDecimal("0.8"));
        source.setWorkStartDate(LocalDateTime.of(2025, 1, 1, 0, 0));
        source.setCfgTrancheStanding(fiche.standing("TST-STD-CHG"));
        cible.setWithdrawalDelay(48);
        cible.setCreditPercent(BigDecimal.ZERO);
        cible.setWorkStartDate(DATE_CIBLE);
        cible.setCoveredFloorArea(BigDecimal.ZERO);
        trancheRepository.save(source);
        trancheRepository.save(cible);
    }

    private ResultActions charger(UUID cibleId, UUID sourceId, String role) throws Exception {
        return mockMvc.perform(post(URL, cibleId, sourceId).with(connecte(admin, role)));
    }

    private CfgTrancheCoveredFloor plancher(CfgTranche tranche, StkItemCategory categorie, String surface) {
        CfgTrancheCoveredFloor plancher = new CfgTrancheCoveredFloor();
        plancher.setId(UUID.randomUUID());
        plancher.setCfgTranche(tranche);
        plancher.setStkItemCategory(categorie);
        plancher.setArea(new BigDecimal(surface));
        return plancherRepository.save(plancher);
    }

    private CfgTranche relue() {
        em.flush();
        em.clear();
        return trancheRepository.findById(cible.getId()).orElseThrow();
    }

    @Test
    void parametresCopies_etJamaisDatesNiDelaiRetractationNiCredit() throws Exception {
        charger(cible.getId(), source.getId(), ROLE_ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TRA-CHG-CIB"));
        CfgTranche t = relue();

        assertAll("copiés",
                () -> assertThat(t.getAdvancePercent()).isEqualByComparingTo("0.3"),
                () -> assertThat(t.getConcretisationPercent()).isEqualByComparingTo("0.3"),
                () -> assertThat(t.getSyndicAmount()).isEqualByComparingTo("1500"),
                () -> assertThat(t.getCity()).isEqualTo("Casablanca"),
                () -> assertThat(t.getIsWithGiftForProposer()).isTrue(),
                () -> assertThat(t.getAgreementFirstDelay()).isEqualTo(45),
                () -> assertThat(t.getCfgTrancheStanding().getCode()).isEqualTo("TST-STD-CHG"));
        assertAll("jamais copiés",
                () -> assertThat(t.getCode()).isEqualTo("TRA-CHG-CIB"),
                () -> assertThat(t.getWithdrawalDelay()).isEqualTo(48),
                () -> assertThat(t.getCreditPercent()).isEqualByComparingTo("0"),
                () -> assertThat(t.getWorkStartDate()).isEqualTo(DATE_CIBLE));
    }

    @Test
    void listesFusionneesParCle_plancherRecalcule() throws Exception {
        plancher(source, appartement, "100");
        plancher(source, villa, "40");
        CfgTrancheCoveredFloor existant = plancher(cible, appartement, "7");
        CfgTrancheLevel etage = new CfgTrancheLevel();
        etage.setId(UUID.randomUUID());
        etage.setCfgTranche(source);
        etage.setLndLevel(niveau);
        etageRepository.save(etage);
        CfgTrancheSettlementCategory frais = new CfgTrancheSettlementCategory();
        frais.setId(UUID.randomUUID());
        frais.setCfgTranche(source);
        frais.setComSettlementCategory(notaire);
        frais.setSorting(3);
        frais.setAmountHT(new BigDecimal("1000"));
        frais.setVatRatio(new BigDecimal("0.2"));
        fraisRepository.save(frais);

        charger(cible.getId(), source.getId(), ROLE_ADMIN).andExpect(status().isOk());
        CfgTranche t = relue();

        assertThat(t.getCoveredFloorArea()).isEqualByComparingTo("140");
        assertThat(plancherRepository.findByCfgTrancheId(cible.getId()))
                .extracting(p -> p.getStkItemCategory().getId(), p -> p.getArea().stripTrailingZeros().toPlainString())
                .containsExactlyInAnyOrder(tuple(appartement.getId(), "100"), tuple(villa.getId(), "40"));
        assertThat(plancherRepository.findById(existant.getId())).isPresent();
        assertThat(etageRepository.findByCfgTrancheId(cible.getId()))
                .singleElement().satisfies(e -> assertThat(e.getLndLevel().getId()).isEqualTo(niveau.getId()));
        assertThat(fraisRepository.findByCfgTrancheIdOrderBySortingAsc(cible.getId()))
                .singleElement().satisfies(f -> assertAll(
                        () -> assertThat(f.getSorting()).isEqualTo(3),
                        () -> assertThat(f.getAmountTTC()).isEqualByComparingTo("1200")));
        assertThat(plancherRepository.findByCfgTrancheId(source.getId())).hasSize(2);
    }

    @Test
    void terrainsEtSeuilsJamaisCopies() throws Exception {
        CfgTrancheStkItem lien = new CfgTrancheStkItem();
        lien.setId(UUID.randomUUID());
        lien.setCfgTranche(source);
        lien.setStkItem(onglets.terrain("TST-TER-CHG"));
        terrainRepository.save(lien);
        CfgTrancheStatus seuil = new CfgTrancheStatus();
        seuil.setId(UUID.randomUUID());
        seuil.setCfgTranche(source);
        seuilRepository.save(seuil);

        charger(cible.getId(), source.getId(), ROLE_ADMIN).andExpect(status().isOk());
        em.flush();
        em.clear();

        assertAll(
                () -> assertThat(terrainRepository.findByCfgTrancheIdOrderBySortingAsc(cible.getId())).isEmpty(),
                () -> assertThat(seuilRepository.findByCfgTrancheId(cible.getId())).isEmpty());
    }

    @Test
    void memeTranche_400() throws Exception {
        charger(cible.getId(), cible.getId(), ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value("sourceId"));
    }

    @Test
    void sourceOuCibleInconnue_404() throws Exception {
        charger(cible.getId(), UUID.randomUUID(), ROLE_ADMIN).andExpect(status().isNotFound());
        charger(UUID.randomUUID(), source.getId(), ROLE_ADMIN).andExpect(status().isNotFound());
    }

    @Test
    void sourceDUneEntiteInactive_404() throws Exception {
        CfgTranche masquee = fixture.tranche("TRA-CHG-INACT",
                fixture.projet("PRJ-CHG-INACT", fixture.entite("ENT-CHG-INACT", false)));
        charger(cible.getId(), masquee.getId(), ROLE_ADMIN).andExpect(status().isNotFound());
    }

    @Test
    void nonAdministrateur_403() throws Exception {
        charger(cible.getId(), source.getId(), "Commercial").andExpect(status().isForbidden());
    }
}
