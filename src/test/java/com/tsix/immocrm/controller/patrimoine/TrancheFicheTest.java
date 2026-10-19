package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import static com.tsix.immocrm.fixtures.ChampsAViderJson.avecChampsAVider;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTrancheFiche.libelle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tranche complète — FEATURE-patrimoine-tranche-fiche.md §1, §4.1, §4.6, §6 (TrancheFicheTest), recalé par §8.5-8.6. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheFicheTest {

    private static final String TRANCHES = "/api/tranches";
    private static final String TRANCHE = "/api/tranches/{id}";
    private static final String DETAIL = "$.detail";
    private static final String CHAMPS_A_VIDER = "champsAVider";
    private static final String STANDING_ID = "standingId";
    private static final List<String> VISIBILITES = List.of("visibleProjet", "visibleRom", "visibleAchat",
            "visibleSav", "visiblePrm");
    private static final List<String> COLONNES_VISIBILITE = List.of("IsForProject", "IsForRom", "IsForBuy",
            "IsForSav", "IsForPrm");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private CfgTrancheRepository trancheRepository;

    private AdmUser admin;
    private CfgCompany entite;
    private CfgProject projet;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1AT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = fixture.entite("ENT-" + jeton, true);
        projet = fixture.projet("PRJ-" + jeton, entite);
    }

    private CorpsJson corpsMinimal(String suffixe) {
        return corps().avec("projetId", projet.getId()).avec("code", "TR-" + jeton + "-" + suffixe)
                .avec("description", "Tranche " + suffixe);
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TRANCHES).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(UUID id, String json) throws Exception {
        return mockMvc.perform(patch(TRANCHE, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private CfgTranche tranche(String suffixe) {
        return fixture.tranche("TR-" + jeton + "-" + suffixe, projet);
    }

    // ------------------------------------------------------------------ §4.1 nouveaux champs : écriture

    @Test
    void r4_1_nouveauxChamps_ecritsDansLeursColonnes() throws Exception {
        UUID standing = fiche.standing("STD-" + jeton);
        UUID niveau = fiche.niveauTerrain("LVL-" + jeton);
        UUID rubrique = fiche.rubrique("RUB-" + jeton, 1);
        UUID compte = fiche.planComptable("ACT-" + jeton);
        UUID facturation = fiche.projetFacturation("PRJF-" + jeton, entite.getId());
        CfgTier sav = fiche.employe("EMP-SAV-" + jeton);
        CfgTier autrui = fiche.fournisseur("FOU-" + jeton);

        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("CHAMPS")
                .avec(STANDING_ID, standing).avec("niveauTerrainId", niveau).avec("rubriqueId", rubrique)
                .avec("planComptableId", compte).avec("projetFacturationId", facturation)
                .avec("responsableSavId", sav.getId()).avec("tiersAutruiId", autrui.getId())
                .avec("adresseSav", "12 rue SAV")
                .avec("cadeauParrain", true).avec("cadeauAncienClient", true)
                .avec("cadeauOrigineProspection", true).avec("cadeauxCumules", true)));

        assertAll(
                () -> assertThat(guid(t.get("CfgTrancheStandingId"))).isEqualTo(guid(standing)),
                () -> assertThat(guid(t.get("LndLevelId"))).isEqualTo(guid(niveau)),
                () -> assertThat(guid(t.get("StkTrancheHeadingId"))).isEqualTo(guid(rubrique)),
                () -> assertThat(guid(t.get("ActAccountId"))).isEqualTo(guid(compte)),
                () -> assertThat(guid(t.get("PrjProjectId"))).isEqualTo(guid(facturation)),
                () -> assertThat(guid(t.get("CfgClaimsManagerId"))).isEqualTo(guid(sav.getId())),
                () -> assertThat(guid(t.get("CfgTierOtherId"))).isEqualTo(guid(autrui.getId())),
                () -> assertThat(t.get("SavAddress")).isEqualTo("12 rue SAV"),
                () -> assertThat(t.get("IsWithGiftForProposer")).isEqualTo(true),
                () -> assertThat(t.get("IsWithGiftForOldCustomer")).isEqualTo(true),
                () -> assertThat(t.get("IsWithGiftForProspectionOrigin")).isEqualTo(true),
                () -> assertThat(t.get("AccumulatedGifts")).isEqualTo(true));
    }

    // ------------------------------------------------------------------ §4.1 nouveaux champs : relecture

    @Test
    void r4_1_nouveauxChamps_relusParLaFiche() throws Exception {
        UUID standing = fiche.standing("STD-R-" + jeton);
        UUID niveau = fiche.niveauTerrain("LVL-R-" + jeton);
        UUID rubrique = fiche.rubrique("RUB-R-" + jeton, 1);
        UUID compte = fiche.planComptable("ACT-R-" + jeton);
        UUID facturation = fiche.projetFacturation("PRJF-R-" + jeton, entite.getId());
        CfgTier sav = fiche.employe("EMP-SAV-R-" + jeton);
        CfgTier autrui = fiche.fournisseur("FOU-R-" + jeton);
        UUID id = creerOk(corpsMinimal("RELU")
                .avec(STANDING_ID, standing).avec("niveauTerrainId", niveau).avec("rubriqueId", rubrique)
                .avec("planComptableId", compte).avec("projetFacturationId", facturation)
                .avec("responsableSavId", sav.getId()).avec("tiersAutruiId", autrui.getId())
                .avec("adresseSav", "12 rue SAV").avec("cadeauParrain", true).avec("cadeauxCumules", true)
                .avec("visibleSav", true));

        mockMvc.perform(get(TRANCHE, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standingId").value(standing.toString()))
                .andExpect(jsonPath("$.standingLibelle").value(libelle("STD-R-" + jeton)))
                .andExpect(jsonPath("$.niveauTerrainId").value(niveau.toString()))
                .andExpect(jsonPath("$.niveauTerrainLibelle").value(libelle("LVL-R-" + jeton)))
                .andExpect(jsonPath("$.rubriqueId").value(rubrique.toString()))
                .andExpect(jsonPath("$.rubriqueLibelle").value(libelle("RUB-R-" + jeton)))
                .andExpect(jsonPath("$.planComptableId").value(compte.toString()))
                .andExpect(jsonPath("$.projetFacturationId").value(facturation.toString()))
                .andExpect(jsonPath("$.responsableSavId").value(sav.getId().toString()))
                .andExpect(jsonPath("$.tiersAutruiId").value(autrui.getId().toString()))
                .andExpect(jsonPath("$.adresseSav").value("12 rue SAV"))
                .andExpect(jsonPath("$.cadeauParrain").value(true))
                .andExpect(jsonPath("$.cadeauAncienClient").value(false))
                .andExpect(jsonPath("$.cadeauOrigineProspection").value(false))
                .andExpect(jsonPath("$.cadeauxCumules").value(true))
                .andExpect(jsonPath("$.visibleSav").value(true))
                .andExpect(jsonPath("$.visibleProjet").value(false));
    }

    /** §1.2 : référence inconnue → 400 nommant le champ dans {@code detail} (§8.4). */
    @ParameterizedTest
    @ValueSource(strings = {STANDING_ID, "niveauTerrainId", "rubriqueId", "responsableSavId", "tiersAutruiId",
            "projetFacturationId", "planComptableId"})
    void r1_2_referenceInconnue_400_detailNommeLeChamp(String champ) throws Exception {
        creer(corpsMinimal("INC-" + champ.length()).avec(champ, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(champ)));
    }

    // ------------------------------------------------------------------ §4.1 cases IsFor* (§8.5)

    @Test
    void r4_1_visibilites_falseParDefaut_dansLaReponse() throws Exception {
        ResultActions reponse = creer(corpsMinimal("VIS-DEF")).andExpect(status().isCreated());

        for (String champ : VISIBILITES) {
            reponse.andExpect(jsonPath("$." + champ).value(false));
        }
    }

    @Test
    void r4_1_visibilites_modifiablesAVrai() throws Exception {
        CfgTranche tranche = tranche("VIS-VRAI");
        CorpsJson corps = corps();
        VISIBILITES.forEach(champ -> corps.avec(champ, true));

        modifier(tranche.getId(), corps.json()).andExpect(status().isOk());

        Tuple t = fixture.ligneTranche(tranche.getId());
        assertThat(COLONNES_VISIBILITE).isNotEmpty()
                .allSatisfy(colonne -> assertThat(t.get(colonne)).as(colonne).isEqualTo(true));
    }

    @Test
    void r4_1_visibilites_modifiablesAFaux() throws Exception {
        CfgTranche tranche = tranche("VIS-FAUX");
        tranche.setIsForProject(true);
        tranche.setIsForRom(true);
        tranche.setIsForBuy(true);
        tranche.setIsForSav(true);
        tranche.setIsForPrm(true);
        trancheRepository.save(tranche);
        CorpsJson corps = corps();
        VISIBILITES.forEach(champ -> corps.avec(champ, false));

        modifier(tranche.getId(), corps.json()).andExpect(status().isOk());

        Tuple t = fixture.ligneTranche(tranche.getId());
        assertThat(COLONNES_VISIBILITE).isNotEmpty()
                .allSatisfy(colonne -> assertThat(t.get(colonne)).as(colonne).isEqualTo(false));
    }

    @Test
    void r4_1_visibiliteSav_seule_lesAutresInchangees() throws Exception {
        CfgTranche tranche = tranche("VIS-SAV");

        modifier(tranche.getId(), corps().avec("visibleSav", true).json()).andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleSav").value(true));

        Tuple t = fixture.ligneTranche(tranche.getId());
        assertAll(
                () -> assertThat(t.get("IsForSav")).isEqualTo(true),
                () -> assertThat(t.get("IsForProject")).isNotEqualTo(true));
    }

    // ------------------------------------------------------------------ §1.1 champsAVider (§8.3)

    @Test
    void r1_1_champsAVider_dateRemiseANull() throws Exception {
        CfgTranche tranche = tranche("VID-DATE");
        tranche.setDeliveryDate(LocalDateTime.of(2027, 3, 1, 0, 0));
        tranche.setOccupancyPermitsDate(LocalDateTime.of(2027, 4, 1, 0, 0));
        trancheRepository.save(tranche);

        modifier(tranche.getId(), avecChampsAVider(corps(), "dateOctroiTitresFonciers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateOctroiTitresFonciers").doesNotExist());

        Tuple t = fixture.ligneTranche(tranche.getId());
        assertAll(
                () -> assertThat(t.get("DeliveryDate")).isNull(),
                () -> assertThat(t.get("OccupancyPermitsDate")).as("champ non listé").isNotNull());
    }

    @Test
    void r1_1_champsAVider_cleEtrangereRemiseANull() throws Exception {
        CfgTranche tranche = tranche("VID-FK");
        fiche.standingDeLaTranche(tranche.getId(), fiche.standing("STD-V-" + jeton));

        modifier(tranche.getId(), avecChampsAVider(corps(), STANDING_ID)).andExpect(status().isOk());

        assertThat(fixture.ligneTranche(tranche.getId()).get("CfgTrancheStandingId")).isNull();
    }

    @Test
    void r1_1_champsAVider_paysRemisANull() throws Exception {
        CfgTranche tranche = tranche("VID-PAYS");
        tranche.setAdmCountry(fixture.pays("PAYS-" + jeton));
        trancheRepository.save(tranche);

        modifier(tranche.getId(), avecChampsAVider(corps(), "paysId")).andExpect(status().isOk());

        assertThat(fixture.ligneTranche(tranche.getId()).get("AdmCountryId")).isNull();
    }

    /** §6 : APF-HIDDEN porte 0 (jamais NULL) dans WarrantyDelay et SyndicAmount — vider remet 0. */
    @Test
    void r1_1_champsAVider_numeriqueRemisAZero_jamaisNull() throws Exception {
        CfgTranche tranche = tranche("VID-NUM");
        tranche.setWarrantyDelay(12);
        tranche.setSyndicAmount(new BigDecimal("500"));
        trancheRepository.save(tranche);

        modifier(tranche.getId(), avecChampsAVider(corps(), "delaiGarantie", "montantSyndic"))
                .andExpect(status().isOk());

        Tuple t = fixture.ligneTranche(tranche.getId());
        assertAll(
                () -> assertThat(t.get("WarrantyDelay")).isNotNull().isEqualTo(0),
                () -> assertThat(decimal(t.get("SyndicAmount"))).isNotNull().isEqualByComparingTo("0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "description", "projetId"})
    void r1_1_champsAVider_champObligatoire_400(String champ) throws Exception {
        CfgTranche tranche = tranche("VID-OBL-" + champ.length());

        modifier(tranche.getId(), avecChampsAVider(corps(), champ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(CHAMPS_A_VIDER)));
    }

    @Test
    void r1_1_champsAVider_nomInconnu_400() throws Exception {
        CfgTranche tranche = tranche("VID-INC");

        modifier(tranche.getId(), avecChampsAVider(corps(), "pasUnChamp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(CHAMPS_A_VIDER)));
    }

    // ------------------------------------------------------------------ §6 NULL jamais écrit (APF-HIDDEN)

    /** Ligne modèle APF-HIDDEN : SavAddress '' et IsFor* 0, jamais NULL, même quand le corps envoie null. */
    @Test
    void r6_creation_avecNullsExplicites_jamaisNullLaOuLeModeleEstRenseigne() throws Exception {
        CorpsJson corps = corpsMinimal("NULLS").avec("adresseSav", null);
        VISIBILITES.forEach(champ -> corps.avec(champ, null));

        Tuple t = fixture.ligneTranche(creerOk(corps));

        assertAll(
                () -> assertThat(t.get("SavAddress")).isEqualTo(""),
                () -> assertThat(COLONNES_VISIBILITE).isNotEmpty()
                        .allSatisfy(colonne -> assertThat(t.get(colonne)).as(colonne).isEqualTo(false)));
    }

    // ------------------------------------------------------------------ §4.6 crédit > 100 %

    @Test
    void r4_6_patch_creditSuperieurA100_400() throws Exception {
        CfgTranche tranche = tranche("CREDIT");

        modifier(tranche.getId(), corps().avec("creditPercent", new BigDecimal("1.01")).json())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("100%")));
    }
}
