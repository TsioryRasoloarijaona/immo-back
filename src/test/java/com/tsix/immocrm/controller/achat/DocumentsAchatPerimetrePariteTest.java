package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.total;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.TOTAUX;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot C8 — FEATURE-achat-liste-parite.md E1 ({@code societeIds} ∩ sociétés habilitées), E5 (référentiels),
 * Vérification n° 3.
 * <pre>
 *   S1 (habilitée) : P1 projet A habilité, P2 sans projet, P3 projet B non habilité    S2 (habilitée) : P4 sans projet
 *   S3 (non habilitée) : P5 sans projet
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatPerimetrePariteTest {

    private static final String ROLE = "Commercial";
    private static final String SOCIETE_IDS = "societeIds";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID s1;
    private UUID s2;
    private UUID s3;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("APP");
        s1 = fixture.societe("S1-" + jeton);
        s2 = fixture.societe("S2-" + jeton);
        s3 = fixture.societe("S3-" + jeton);
        UUID projetA = fixture.projet("PA-" + jeton, s1);
        UUID projetB = fixture.projet("PB-" + jeton, s1);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        fixture.piece(facture("-1", fournisseur, nom, s1, projetA));
        fixture.piece(facture("-2", fournisseur, nom, s1, null));
        fixture.piece(facture("-3", fournisseur, nom, s1, projetB));
        fixture.piece(facture("-4", fournisseur, nom, s2, null));
        fixture.piece(facture("-5", fournisseur, nom, s3, null));
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, s1);
        fixture.habiliterSociete(compte, s2);
        fixture.habiliter(compte, projetA);
    }

    private Piece facture(String suffixe, UUID fournisseur, String nom, UUID societe, UUID projet) {
        return Piece.facture(jeton + suffixe, LocalDate.of(2026, 3, 10), fournisseur, nom, societe, projet,
                new BigDecimal("100"));
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    // ------------------------------------------------------------------ Vérification n° 3 : périmètre

    @Test
    void v3_temoin_sansFiltre_societesHabiliteesEtProjetsHabilitesOuSansProjet() throws Exception {
        String json = corps(mockMvc, liste(qui()));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-1"), code("-2"), code("-4"));
    }

    @Test
    void v3_societeNonHabilitee_zeroLigne_jamaisAucunFiltre() throws Exception {
        String json = corps(mockMvc, liste(qui(), SOCIETE_IDS, s3.toString()));

        assertThat(total(json)).isZero();
    }

    @Test
    void v3_melangeHabiliteeEtNonHabilitee_seulementLHabilitee() throws Exception {
        String json = corps(mockMvc, liste(qui(), SOCIETE_IDS, s2.toString(), SOCIETE_IDS, s3.toString()));

        assertThat(codes(json)).containsExactly(code("-4"));
    }

    @Test
    void v3_filtreSociete_pieceSansProjetVisible_projetNonHabiliteJamais() throws Exception {
        String json = corps(mockMvc, liste(qui(), SOCIETE_IDS, s1.toString()));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-1"), code("-2"));
    }

    @Test
    void v3_compteSansSociete_200Vide_memeAvecSocieteIds() throws Exception {
        AdmUser sansSociete = fixture.compte("ACH-NS-" + jeton, ROLE);

        String json = corps(mockMvc, liste(connecte(sansSociete, ROLE), SOCIETE_IDS, s1.toString()));

        assertThat(total(json)).isZero();
    }

    @Test
    void v3_totaux_societeNonHabilitee_zero() throws Exception {
        String totaux = corps(mockMvc, appel(TOTAUX, qui(), SOCIETE_IDS, s3.toString()));

        assertThat(nombre(totaux, "$.nombre")).isEqualByComparingTo("0");
    }

    @Test
    void v3_export_societeIds_memeIntersection() throws Exception {
        List<String> textes = AppelsAchat.textesExport(mockMvc, get(AppelsAchat.EXPORT).param("format", "xlsx")
                .param(SOCIETE_IDS, s2.toString()).param(SOCIETE_IDS, s3.toString()).with(qui()));

        assertThat(textes).contains(code("-4")).doesNotContain(code("-1"), code("-2"), code("-5"));
    }

    // ------------------------------------------------------------------ E5 : référentiels

    @Test
    void e5_referentiels_societesHabiliteesSeulement_avecCodeEtDescription() throws Exception {
        fixture.poserSur("CfgCompany", s1, "Description", "Société un " + jeton);

        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(qui()));

        List<String> ids = JsonPath.read(json, "$.societes[*].id");
        String chemin = "$.societes[?(@.id == '" + s1 + "')]";
        assertAll(
                () -> assertThat(ids).contains(s1.toString(), s2.toString()).doesNotContain(s3.toString()),
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".code")).containsExactly("S1-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".description"))
                        .containsExactly("Société un " + jeton));
    }

    @Test
    void e5_referentiels_compteSansSociete_societesVides_200() throws Exception {
        AdmUser sansSociete = fixture.compte("ACH-NS-" + jeton, ROLE);

        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(connecte(sansSociete, ROLE)));

        assertThat((List<Object>) JsonPath.read(json, "$.societes")).isEmpty();
    }

    @Test
    void e5_referentiels_modulesBuyPrjStl_avecLibelles() throws Exception {
        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(qui()));

        List<String> modules = JsonPath.read(json, "$.modules[*].code");
        assertAll(
                () -> assertThat(modules).containsExactlyInAnyOrder("BUY", "PRJ", "STL"),
                () -> assertThat((List<Object>) JsonPath.read(json, "$.modules[?(@.code == 'BUY')].libelle"))
                        .containsExactly("Achat"),
                () -> assertThat((List<Object>) JsonPath.read(json, "$.modules[?(@.code == 'PRJ')].libelle"))
                        .containsExactly("Projet"));
    }

    @Test
    void e5_referentiels_typesLot_idCodeLibelle() throws Exception {
        UUID type = fixture.typeLot("TRV-" + jeton, "TRAVAUX " + jeton);
        UUID piece = fixture.piece(Piece.facture(code("-T"), LocalDate.of(2026, 3, 10),
                fixture.fournisseur("FOT-" + jeton, "Fournisseur T " + jeton), "Fournisseur T " + jeton, s1, null,
                new BigDecimal("100")));
        fixture.poser(piece, "PrjMarketTypeId", type);

        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(qui()));

        String chemin = "$.typesLot[?(@.id == '" + type + "')]";
        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".code")).containsExactly("TRV-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".libelle"))
                        .containsExactly("TRAVAUX " + jeton));
    }
}
