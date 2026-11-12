package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Revue du lot D1 : ordre 404 → 403 → 409 → 400, périmètre des écritures, colonnes OMRAN posées à la création. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesRevueD1Test {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String TVA = "0.2";
    private static final String DROIT_PAR_MONTANT =
            "Yes.Prj.Front.Market.MarketMonitoringList.CanEditWorkingOutRetainByAmount";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private JdbcTemplate jdbc;

    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor admin;
    private RequestPostProcessor libre;

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PRV-" + jeton);
        lot = fixture.lot(projet, "LRV-" + jeton, fixture.fournisseur("FRV-" + jeton));
        AdmUser compte = vente.compte("prj.rv.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
        libre = decomptes.compte(projet, jeton, DROIT_MODIFIER, DROIT_SUPPRIMER);
    }

    private UUID decompte(UUID lotId, String code, int numero) {
        return fixture.decompte(lotId, code + jeton, numero,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
    }

    private int executer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r.getStatus();
    }

    private int creer(UUID lotId, String corps, RequestPostProcessor qui) throws Exception {
        return executer(post(LOTS + "/" + lotId + "/decomptes").contentType(MediaType.APPLICATION_JSON).content(corps), qui);
    }

    private int enregistrer(UUID decompte, String corps, RequestPostProcessor qui) throws Exception {
        return executer(put(DECOMPTES + "/" + decompte).contentType(MediaType.APPLICATION_JSON).content(corps), qui);
    }

    private Map<String, Object> seulDecompteDuLot() {
        decomptes.flush();
        return jdbc.queryForMap("""
                SELECT Code, Description, SupplierDescription, CAST(StlCurrencyId AS varchar(36)) AS devise
                FROM PrjWorkingOut WHERE PrjMarketId = ?""", lot.toString());
    }

    /** Projet non habilité pour les comptes de ce test, avec un lot et un décompte dernier non facturé. */
    private record Ailleurs(UUID lot, UUID decompte) {
    }

    private Ailleurs ailleurs() {
        UUID autre = fixture.projet("PRX-" + jeton);
        UUID autreLot = fixture.lot(autre, "LRX-" + jeton, fixture.fournisseur("FRX-" + jeton));
        return new Ailleurs(autreLot, decompte(autreLot, "RX1-", 1));
    }

    // ------------------------------------------------------------------ colonnes OMRAN à la création

    /** W:Description l.266-276 : « Décompte N° code du jj/mm/aaaa » (749 / 749 renseignés dans yesRemsProd). */
    @Test
    void creation_poseLaDescriptionOmran() throws Exception {
        assertThat(creer(lot, "{\"dateDocument\":\"2026-04-01\"}", libre)).isEqualTo(201);

        Map<String, Object> n = seulDecompteDuLot();
        assertThat(n).containsEntry("Description", "Décompte N° " + n.get("Code") + " du 01/04/2026");
    }

    /** W:SupplierDescription l.1821-1835 : raison sociale du tiers (749 / 749 renseignés dans yesRemsProd). */
    @Test
    void creation_poseLeNomDuFournisseurOmran() throws Exception {
        assertThat(creer(lot, "{\"dateDocument\":\"2026-04-01\"}", libre)).isEqualTo(201);

        assertThat(seulDecompteDuLot()).extractingByKey("SupplierDescription").isNotNull();
    }

    /** W:StlCurrencyId l.44, l.334-335 : [IsMandatory], devise par défaut (749 / 749 renseignés dans yesRemsProd). */
    @Test
    void creation_poseLaDeviseObligatoireOmran() throws Exception {
        assertThat(creer(lot, "{\"dateDocument\":\"2026-04-01\"}", libre)).isEqualTo(201);

        assertThat(seulDecompteDuLot()).extractingByKey("devise").isNotNull();
    }

    // ------------------------------------------------------------------ ordre des refus

    @Test
    void creation_corpsSansDate_lotInconnu_404avant400() throws Exception {
        assertThat(creer(UUID.randomUUID(), "{}", libre)).isEqualTo(404);
    }

    @Test
    void creation_corpsSansDate_sansDroit_403avant400() throws Exception {
        assertThat(creer(lot, "{}", decomptes.compte(projet, jeton, DROIT_SUPPRIMER))).isEqualTo(403);
    }

    /** §3 : « basé sur le montant » sans A_0152_22 → 403, même si la date est aussi invalide (403 avant 400). */
    @Test
    void enregistrer_parMontantSansDroit_dateInvalide_403avant400() throws Exception {
        decompte(lot, "RV1-", 1);
        UUID d2 = decompte(lot, "RV2-", 2);

        int statut = enregistrer(d2, "{\"dateDocument\":\"2026-01-01\",\"retenueGarantieSurMontant\":true,"
                + "\"retenueGarantie\":50}", decomptes.compte(projet, jeton, DROIT_MODIFIER));

        assertThat(statut).isEqualTo(403);
    }

    @Test
    void enregistrer_parMontantSansDroit_403_rienEcrit() throws Exception {
        UUID d = decompte(lot, "RV3-", 1);

        int statut = enregistrer(d, "{\"retenueGarantieSurMontant\":true,\"retenueGarantie\":50}",
                decomptes.compte(projet, jeton, DROIT_MODIFIER));

        assertThat(statut).isEqualTo(403);
        assertThat(decomptes.lire(d, "WarrantyBasedOnAmount")).doesNotContainEntry("WarrantyBasedOnAmount", Boolean.TRUE);
    }

    @Test
    void enregistrer_parMontantAvecDroit_passe() throws Exception {
        UUID d = decompte(lot, "RV4-", 1);

        int statut = enregistrer(d, "{\"retenueGarantieSurMontant\":true,\"retenueGarantie\":50}",
                decomptes.compte(projet, jeton, DROIT_MODIFIER, DROIT_PAR_MONTANT));

        assertThat(statut).isEqualTo(200);
        assertThat(new BigDecimal(String.valueOf(decomptes.lire(d, "WarrantyAmount").get("WarrantyAmount"))))
                .isEqualByComparingTo("50");
    }

    // ------------------------------------------------------------------ saisie d'une quantité

    /** WL l.141-185 : (saisie + antérieure) × coef − antérieure ; AmountTTC recalculé sur la ligne modifiée. */
    @Test
    void enregistrer_quantiteSaisie_recalculeLaLigneEtLeTTC() throws Exception {
        UUID a = decomptes.ligneMarche(lot, 1, "10", "100", TVA);
        UUID d1 = decompte(lot, "RQ1-", 1);
        decomptes.ligne(d1, a, 1, "4", "0", "100", TVA);
        UUID d2 = decompte(lot, "RQ2-", 2);
        UUID l2 = decomptes.ligne(d2, a, 1, "1", "4", "100", TVA);

        int statut = enregistrer(d2, "{\"lignes\":[{\"id\":\"" + l2 + "\",\"quantiteSaisie\":2}]}", admin);

        assertThat(statut).isEqualTo(200);
        assertThat(new BigDecimal(String.valueOf(decomptes.lignes(d2).getFirst().get("Quantity")))).isEqualByComparingTo("2");
        assertThat(new BigDecimal(String.valueOf(decomptes.lire(d2, TTC).get(TTC)))).isEqualByComparingTo("240");
    }

    // ------------------------------------------------------------------ périmètre des écritures

    @Test
    void enregistrer_horsPerimetre_404_rienEcrit() throws Exception {
        Ailleurs x = ailleurs();

        int statut = enregistrer(x.decompte(), "{\"notes\":\"pirate\"}", decomptes.compte(projet, jeton, DROIT_MODIFIER));

        assertThat(statut).isEqualTo(404);
        assertThat(decomptes.lire(x.decompte(), "Notes")).doesNotContainEntry("Notes", "pirate");
    }

    @Test
    void creation_lotHorsPerimetre_404() throws Exception {
        Ailleurs x = ailleurs();

        assertThat(creer(x.lot(), "{\"dateDocument\":\"2026-04-01\"}", decomptes.compte(projet, jeton, DROIT_MODIFIER)))
                .isEqualTo(404);
    }

    @Test
    void suppression_horsPerimetre_404() throws Exception {
        Ailleurs x = ailleurs();

        assertThat(executer(delete(DECOMPTES + "/" + x.decompte()), decomptes.compte(projet, jeton, DROIT_SUPPRIMER)))
                .isEqualTo(404);
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM PrjWorkingOut WHERE PKey = ?", Integer.class,
                x.decompte().toString());
        assertThat(n).isEqualTo(1);
    }
}
