package com.tsix.immocrm.controller.patrimoine;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglet Commission vendeur ({@code ComSalesCommission}) — FEATURE-patrimoine-tranche-regles.md §2, §7
 * (TrancheCommissionsTest), §9.3 (PUT liste réconcilié, convention P1b). Le 403 est dans PatrimoineSecuriteTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheCommissionsTest {

    private static final String COMMISSIONS = "/api/tranches/{id}/commissions";
    private static final String DATE_DEBUT = "dateDebut";
    private static final String DATE_FIN = "dateFin";
    private static final String DEBUT_2027 = "2027-01-01T00:00:00";
    private static final String DE_LA_TRANCHE = "SELECT COUNT(*) FROM ComSalesCommission WHERE CfgTrancheId = :id";
    private static final LocalDateTime JANVIER = LocalDateTime.of(2027, 1, 1, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheRegles regles;

    private AdmUser admin;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        String jeton = "P1CK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private ResultActions envoyer(String corps) throws Exception {
        return mockMvc.perform(put(COMMISSIONS, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private static CorpsJson periode(String debut, String fin) {
        return corps().avec(DATE_DEBUT, debut).avec(DATE_FIN, fin);
    }

    private List<Tuple> lignesEnBase() {
        return regles.lignes("SELECT * FROM ComSalesCommission WHERE CfgTrancheId = :id", tranche.getId());
    }

    // ------------------------------------------------------------------ « La Date début doit être < à la Date fin! »

    @Test
    void r2_dateDebutApresDateFin_400_champIndexe() throws Exception {
        envoyer(lignes(periode(DEBUT_2027, "2027-03-31T00:00:00"),
                periode("2027-09-01T00:00:00", "2027-08-01T00:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("lignes[1].dateFin")));
    }

    /** SalesCommission.Verif l.880 : seul {@code StartDate > EndDate} est refusé. */
    @Test
    void r2_dateDebutEgaleDateFin_200() throws Exception {
        envoyer(lignes(periode("2027-05-01T00:00:00", "2027-05-01T00:00:00"))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ PUT liste réconcilié, GET

    @Test
    void r2_get_lignesDeLaTranche() throws Exception {
        UUID ligne = regles.ligneCommission(tranche.getId(), JANVIER, null);

        mockMvc.perform(get(COMMISSIONS, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[*].id", hasItem(ligne.toString())));
    }

    @Test
    void r2_put_idConnuMaj_sansIdCree_absentSupprime() throws Exception {
        UUID modifiee = regles.ligneCommission(tranche.getId(), JANVIER, LocalDateTime.of(2027, 3, 31, 0, 0));
        UUID retiree = regles.ligneCommission(tranche.getId(), LocalDateTime.of(2028, 1, 1, 0, 0), null);

        envoyer(lignes(periode(DEBUT_2027, "2027-02-28T00:00:00").avec("id", modifiee),
                periode("2027-06-01T00:00:00", "2027-12-31T00:00:00")))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(regles.compter("SELECT COUNT(*) FROM ComSalesCommission WHERE PKey = :id "
                        + "AND EndDate = '2027-02-28'", modifiee)).isEqualTo(1),
                () -> assertThat(regles.compter("SELECT COUNT(*) FROM ComSalesCommission WHERE PKey = :id", retiree))
                        .isZero(),
                () -> assertThat(regles.compter(DE_LA_TRANCHE + " AND StartDate = '2027-06-01'", tranche.getId()))
                        .isEqualTo(1),
                () -> assertThat(regles.compter(DE_LA_TRANCHE, tranche.getId())).isEqualTo(2));
    }

    /** §2 : {@code baseSurMontant} vaut {@code true} par défaut sur les six blocs (SalesCommission.cs l.21-51). */
    @Test
    void r2_blocsOmis_baseSurMontantVraiSurLesSixColonnes() throws Exception {
        envoyer(lignes(periode(DEBUT_2027, null))).andExpect(status().isOk());

        assertThat(lignesEnBase()).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get("SaleCommissionBasedOnAmount")).isEqualTo(true),
                () -> assertThat(l.get("SaleCommissionExternBasedOnAmount")).isEqualTo(true),
                () -> assertThat(l.get("DeliveryCommissionBasedOnAmount")).isEqualTo(true),
                () -> assertThat(l.get("DeliveryCommissionExternBasedOnAmount")).isEqualTo(true),
                () -> assertThat(l.get("DeliveryCommission2BasedOnAmount")).isEqualTo(true),
                () -> assertThat(l.get("DeliveryCommissionExtern2BasedOnAmount")).isEqualTo(true)));
    }

    /** Nom de fil du bloc supposé (voir le rapport) : {@code concretisation} = SaleCommission…. */
    @Test
    void r2_blocConcretisation_ecritLesColonnesSaleCommission() throws Exception {
        envoyer("{\"lignes\":[{\"dateDebut\":\"2027-01-01T00:00:00\",\"dateFin\":null,"
                + "\"concretisation\":{\"baseSurMontant\":false,\"pourcentage\":0.015,\"montant\":0}}]}")
                .andExpect(status().isOk());

        assertThat(lignesEnBase()).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get("SaleCommissionBasedOnAmount")).isEqualTo(false),
                () -> assertThat(FixturePatrimoine.decimal(l.get("SaleCommissionPercent"))).isEqualByComparingTo("0.015"),
                () -> assertThat(FixturePatrimoine.decimal(l.get("SaleCommissionAmount"))).isEqualByComparingTo(BigDecimal.ZERO)));
    }
}
