package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.CATEGORIE_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.GENRE;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.PROJET_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.TYPE_ID;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_EXPORTER;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_NOUVEAU_MULTI;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Droits OMRAN ({@code AdmRight.Code}, lus en base par les rôles du compte) — FEATURE-lots-liste-parite.md E4, E5, E6, R6 :
 * pour chaque action, 403 sans le code, 2xx avec le code seul (témoin), 2xx Administrateur. Chaque compte non administrateur
 * porte un rôle propre exempté de la clause de statut et habilité sur le projet : seul le droit varie.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsDroitsTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String ADMINISTRATEUR = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;
    @Autowired private FixtureProjetConfig config;

    private LotsPariteSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statut;
    private UUID categorie;
    private UUID type;
    private int numero;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        fixture.statutsDepart(jeton);
        fixture.sequenceMarche(60_000 + Math.floorMod(jeton.hashCode(), 20_000));
        projet = fixture.projet("DP-" + jeton, fixture.entiteJuridique("DE-" + jeton));
        statut = fixture.statut(ENTREPRENEURS, "DS-" + jeton);
        categorie = config.categorie("DCAT-" + jeton, false);
        type = config.typeLot("DTYP-" + jeton);
    }

    /** Compte non administrateur portant exactement ces droits. */
    private RequestPostProcessor avec(String... droits) {
        numero++;
        return connecte(fixture.avecDroits("lots.dr." + numero + "." + jeton, List.of(droits), projet), COMMERCIAL);
    }

    private RequestPostProcessor administrateur() {
        numero++;
        return connecte(fixture.administrateur("lots.dr.adm." + numero + "." + jeton, projet), ADMINISTRATEUR);
    }

    private CorpsJson corpsLot(String genre) {
        return corps().avec(GENRE, genre).avec(DESCRIPTION, "Lot droits " + jeton).avec(PROJET_ID, projet)
                .avec(CATEGORIE_ID, categorie).avec(TYPE_ID, type);
    }

    private int creer(RequestPostProcessor qui, String genre) throws Exception {
        Creation c = appels.creer(qui, corpsLot(genre));
        return c.statut();
    }

    private UUID lotLibre(String code) {
        return fixture.lot(projet, code + "-" + jeton, ENTREPRENEURS, statut);
    }

    // ------------------------------------------------------------------ E4 export

    @Test
    void e4_export_sansDroit_403() throws Exception {
        assertThat(appels.exporter(avec(DROIT_NOUVEAU, DROIT_SUPPRIMER)).getStatus()).isEqualTo(403);
    }

    @Test
    void e4_export_avecLeSeulDroitExport_200() throws Exception {
        assertThat(appels.exporter(avec(DROIT_EXPORTER)).getStatus()).isEqualTo(200);
    }

    @Test
    void e4_export_administrateur_200() throws Exception {
        assertThat(appels.exporter(administrateur()).getStatus()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ E5 création, Entrepreneurs : toolNew

    @Test
    void e5_entrepreneurs_sansDroit_403_aucunLot() throws Exception {
        assertThat(creer(avec(DROIT_EXPORTER), ENTREPRENEURS)).isEqualTo(403);
        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjMarket WHERE Description = ?", "Lot droits " + jeton)).isZero();
    }

    @Test
    void e5_entrepreneurs_avecLeSeulToolNew_201() throws Exception {
        assertThat(creer(avec(DROIT_NOUVEAU), ENTREPRENEURS)).isEqualTo(201);
    }

    @Test
    void e5_entrepreneurs_administrateur_201() throws Exception {
        assertThat(creer(administrateur(), ENTREPRENEURS)).isEqualTo(201);
    }

    // ------------------------------------------------------------------ E5 création, autres genres : toolNewMultiSupplier
    // OMRAN MarketMonitoringList.FilterToolsByRights : toolAddMarketMultiSupplier/Administrative/Studies/Dealers
    // exigent tous toolNewMultiSupplier (aucun AdmRight par genre en base).

    @ParameterizedTest
    @ValueSource(strings = {"FOURNISSEURS", "ADMINISTRATION", "MAITRES_OEUVRE", "CONCESSIONNAIRES"})
    void e5_autresGenres_sansDroit_403(String genre) throws Exception {
        assertThat(creer(avec(DROIT_EXPORTER), genre)).isEqualTo(403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOURNISSEURS", "ADMINISTRATION", "MAITRES_OEUVRE", "CONCESSIONNAIRES"})
    void e5_autresGenres_avecLeSeulToolNewMultiSupplier_201(String genre) throws Exception {
        assertThat(creer(avec(DROIT_NOUVEAU_MULTI), genre)).isEqualTo(201);
    }

    @Test
    void e5_fournisseurs_administrateur_201() throws Exception {
        assertThat(creer(administrateur(), FOURNISSEURS)).isEqualTo(201);
    }

    // ------------------------------------------------------------------ R6 : le droit de l'autre genre ne suffit pas

    @Test
    void r6_toolNewSeul_201EnEntrepreneurs_403EnFournisseurs() throws Exception {
        RequestPostProcessor qui = avec(DROIT_NOUVEAU);

        assertThat(creer(qui, ENTREPRENEURS)).isEqualTo(201);
        assertThat(creer(qui, FOURNISSEURS)).isEqualTo(403);
    }

    @Test
    void r6_toolNewMultiSupplierSeul_403EnEntrepreneurs() throws Exception {
        assertThat(creer(avec(DROIT_NOUVEAU_MULTI), ENTREPRENEURS)).isEqualTo(403);
    }

    // ------------------------------------------------------------------ 2ᵉ passe point 7 : 403 avant 400

    @Test
    void p7_sansDroit_champAbsent_403_temoinToolNew_400() throws Exception {
        CorpsJson invalide = corpsLot(ENTREPRENEURS).sans(DESCRIPTION);

        assertThat(appels.creer(avec(DROIT_EXPORTER), invalide).statut()).isEqualTo(403);
        assertThat(appels.creer(avec(DROIT_NOUVEAU), invalide).statut()).isEqualTo(400);
    }

    @Test
    void p7_sansDroit_genreInconnu_403_temoinAdministrateur_400() throws Exception {
        CorpsJson invalide = corpsLot("INCONNU");

        assertThat(appels.creer(avec(DROIT_EXPORTER), invalide).statut()).isEqualTo(403);
        assertThat(appels.creer(administrateur(), invalide).statut()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ E6 suppression : toolDelete

    @Test
    void e6_sansDroit_403_lotConserve() throws Exception {
        UUID lot = lotLibre("DL1");

        assertThat(appels.supprimer(avec(DROIT_NOUVEAU, DROIT_EXPORTER), lot).statut()).isEqualTo(403);
        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjMarket WHERE PKey = ?", lot.toString())).isEqualTo(1);
    }

    @Test
    void e6_avecLeSeulToolDelete_204() throws Exception {
        assertThat(appels.supprimer(avec(DROIT_SUPPRIMER), lotLibre("DL2")).statut()).isEqualTo(204);
    }

    @Test
    void e6_administrateur_204() throws Exception {
        assertThat(appels.supprimer(administrateur(), lotLibre("DL3")).statut()).isEqualTo(204);
    }
}
