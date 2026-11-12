package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lot C8 « liste des pièces » — FEATURE-achat-liste-parite.md E1 (nouveaux filtres, recherche étendue, tris),
 * Vérification n° 1, 2 et 7. Un compte habilité à deux sociétés et au projet ; chaque test crée ses pièces.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatFiltresPariteTest {

    private static final String ROLE = "Commercial";
    private static final String SOCIETE_IDS = "societeIds";
    private static final String MODULES = "modules";
    private static final String TYPE_LOT_IDS = "typeLotIds";
    private static final String ARCHIVE = "archive";
    private static final String COMPTABILISE = "comptabilise";
    private static final String CHAMP_DATE = "champDate";
    private static final String SEARCH = "search";
    private static final String IS_CLOSED = "IsClosed";
    private static final String IS_ACCOUNTED = "IsAccounted";
    private static final String DESCRIPTION = "Description";
    private static final LocalDate MARS_10 = LocalDate.of(2026, 3, 10);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID societe2;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AFP");
        societe = fixture.societe("SOC-" + jeton);
        societe2 = fixture.societe("SOC2-" + jeton);
        projet = fixture.projet("P-" + jeton, societe);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliterSociete(compte, societe2);
        fixture.habiliter(compte, projet);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private UUID piece(String suffixe, String aPayer) {
        return piece(suffixe, MARS_10, aPayer);
    }

    private UUID piece(String suffixe, LocalDate date, String aPayer) {
        return fixture.piece(Piece.facture(jeton + suffixe, date, fournisseur, nom, societe, projet,
                new BigDecimal(aPayer)));
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    // ------------------------------------------------------------------ Vérification n° 1 : chaque filtre seul restreint

    @Test
    void v1_societeIds_neGardeQueLaSocieteDemandee() throws Exception {
        piece("-S1", "100");
        fixture.piece(Piece.facture(code("-S2"), MARS_10, fournisseur, nom, societe2, null, new BigDecimal("100")));

        String json = corps(mockMvc, liste(qui(), SOCIETE_IDS, societe2.toString()));

        assertThat(codes(json)).containsExactly(code("-S2"));
    }

    @Test
    void v1_modules_codeAdmModuleDeLaPiece_repetable() throws Exception {
        fixture.poser(piece("-BUY", "100"), "AdmModuleId", fixture.module("BUY"));
        fixture.poser(piece("-PRJ", "100"), "AdmModuleId", fixture.module("PRJ"));
        fixture.poser(piece("-STL", "100"), "AdmModuleId", fixture.module("STL"));
        piece("-SANS", "100");

        String achat = corps(mockMvc, liste(qui(), MODULES, "BUY"));
        String achatEtTresorerie = corps(mockMvc, liste(qui(), MODULES, "BUY", MODULES, "STL"));

        assertAll(
                () -> assertThat(codes(achat)).containsExactly(code("-BUY")),
                () -> assertThat(codes(achatEtTresorerie)).containsExactlyInAnyOrder(code("-BUY"), code("-STL")));
    }

    @Test
    void v1_modules_seCombineParEtAvecModuleProjet() throws Exception {
        fixture.poser(piece("-BUY", "100"), "AdmModuleId", fixture.module("BUY"));
        fixture.poser(piece("-PRJ", "100"), "AdmModuleId", fixture.module("PRJ"));

        String achatEtProjet = corps(mockMvc, liste(qui(), MODULES, "BUY", "moduleProjet", "true"));
        String projetEtProjet = corps(mockMvc, liste(qui(), MODULES, "PRJ", "moduleProjet", "true"));

        assertAll(
                () -> assertThat(total(achatEtProjet)).isZero(),
                () -> assertThat(codes(projetEtProjet)).containsExactly(code("-PRJ")));
    }

    @Test
    void v1_typeLotIds_typeDeLaPiece() throws Exception {
        UUID travaux = fixture.typeLot("TRV-" + jeton, "TRAVAUX " + jeton);
        UUID etude = fixture.typeLot("ETU-" + jeton, "ETUDE " + jeton);
        fixture.poser(piece("-TRV", "100"), "PrjMarketTypeId", travaux);
        fixture.poser(piece("-ETU", "100"), "PrjMarketTypeId", etude);
        piece("-SANS", "100");

        String json = corps(mockMvc, liste(qui(), TYPE_LOT_IDS, etude.toString()));

        assertThat(codes(json)).containsExactly(code("-ETU"));
    }

    @Test
    void v1_archive_vraiFauxAbsent_isClosedNullCommeNonArchive() throws Exception {
        fixture.poser(piece("-A1", "100"), IS_CLOSED, true);
        fixture.poser(piece("-A0", "100"), IS_CLOSED, false);
        piece("-AN", "100");

        String archivees = corps(mockMvc, liste(qui(), ARCHIVE, "true"));
        String nonArchivees = corps(mockMvc, liste(qui(), ARCHIVE, "false"));
        String toutes = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(codes(archivees)).containsExactly(code("-A1")),
                () -> assertThat(codes(nonArchivees)).containsExactlyInAnyOrder(code("-A0"), code("-AN")),
                () -> assertThat(codes(toutes)).containsExactlyInAnyOrder(code("-A1"), code("-A0"), code("-AN")));
    }

    /** 2ᵉ passe : {@code false} = {@code IsAccounted = 0 OR NULL}, comme {@code archive}. */
    @Test
    void v1_comptabilise_vraiFauxAbsent_isAccountedNullCommeNonComptabilise() throws Exception {
        fixture.poser(piece("-C1", "100"), IS_ACCOUNTED, true);
        fixture.poser(piece("-C0", "100"), IS_ACCOUNTED, false);
        piece("-CN", "100");

        String oui = corps(mockMvc, liste(qui(), COMPTABILISE, "true"));
        String non = corps(mockMvc, liste(qui(), COMPTABILISE, "false"));
        String tous = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(codes(oui)).containsExactly(code("-C1")),
                () -> assertThat(codes(non)).containsExactlyInAnyOrder(code("-C0"), code("-CN")),
                () -> assertThat(codes(tous)).containsExactlyInAnyOrder(code("-C1"), code("-C0"), code("-CN")));
    }

    @Test
    void v1_montantDeA_bornesInclusesSurAPayer() throws Exception {
        piece("-099", "99.99");
        piece("-100", "100");
        piece("-200", "200");
        piece("-201", "200.01");

        String json = corps(mockMvc, liste(qui(), "montantDe", "100", "montantA", "200"));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-100"), code("-200"));
    }

    @Test
    void v1_montantDeSuperieurAMontantA_pageVidePas400() throws Exception {
        piece("-150", "150");

        String json = corps(mockMvc, liste(qui(), "montantDe", "200", "montantA", "100"));

        assertThat(total(json)).isZero();
    }

    @Test
    void v1_champDateEcheance_duAuPortentSurMaturityDate() throws Exception {
        piece("-E1", LocalDate.of(2026, 3, 1), "100");
        piece("-E2", LocalDate.of(2026, 3, 15), "100");

        String surEcheance = corps(mockMvc, liste(qui(), CHAMP_DATE, "ECHEANCE", "du", "2026-04-01", "au", "2026-04-30"));
        String surDatePiece = corps(mockMvc, liste(qui(), CHAMP_DATE, "DATE_PIECE", "du", "2026-04-01",
                "au", "2026-04-30"));

        assertAll(
                () -> assertThat(codes(surEcheance)).containsExactly(code("-E2")),
                () -> assertThat(total(surDatePiece)).isZero());
    }

    @Test
    void v1_champDateEcheance_bornesIncluses() throws Exception {
        piece("-B1", LocalDate.of(2026, 3, 1), "100");
        piece("-B2", LocalDate.of(2026, 3, 2), "100");
        piece("-B3", LocalDate.of(2026, 3, 3), "100");

        String json = corps(mockMvc, liste(qui(), CHAMP_DATE, "ECHEANCE", "du", "2026-03-31", "au", "2026-04-01"));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-B1"), code("-B2"));
    }

    @Test
    void v1_dateRenseignee_colonneChoisieNonNulle_bornesIgnorees() throws Exception {
        piece("-AVEC", MARS_10, "100");
        fixture.effacer(piece("-SANS", MARS_10, "100"), "MaturityDate");

        String json = corps(mockMvc, liste(qui(), CHAMP_DATE, "ECHEANCE", "dateRenseignee", "true",
                "du", "2030-01-01", "au", "2030-01-31"));

        assertThat(codes(json)).containsExactly(code("-AVEC"));
    }

    // ------------------------------------------------------------------ Vérification n° 2 : rien coché = tout

    @Test
    void v2_listesVides_memeTotalQueSansParametre() throws Exception {
        piece("-R1", "100");
        fixture.piece(Piece.facture(code("-R2"), MARS_10, fournisseur, nom, societe2, null, new BigDecimal("100")));

        String sans = corps(mockMvc, liste(qui()));
        String vides = corps(mockMvc, liste(qui(), SOCIETE_IDS, "", MODULES, "", TYPE_LOT_IDS, ""));

        assertAll(
                () -> assertThat(total(sans)).isEqualTo(2),
                () -> assertThat(total(vides)).isEqualTo(total(sans)));
    }

    // ------------------------------------------------------------------ E1 : recherche multi-critère étendue

    @Test
    void e1_recherche_referenceBureauOrdre_etNotes_sansCasse() throws Exception {
        fixture.poser(piece("-BOR", "100"), "OfficeOrderRef", "BORZQX" + jeton);
        fixture.poser(piece("-NOT", "100"), "Notes", "Note NOTZQX" + jeton);
        piece("-RIEN", "100");

        String bureau = corps(mockMvc, liste(qui(), SEARCH, ("borzqx" + jeton).toLowerCase()));
        String notes = corps(mockMvc, liste(qui(), SEARCH, "NOTZQX" + jeton));

        assertAll(
                () -> assertThat(codes(bureau)).containsExactly(code("-BOR")),
                () -> assertThat(codes(notes)).containsExactly(code("-NOT")));
    }

    @Test
    void e1_recherche_projetCodeEtDescription() throws Exception {
        UUID autre = fixture.projet("PCQ" + jeton, societe);
        fixture.habiliter(compte, autre);
        fixture.poserSur("PrjProject", autre, DESCRIPTION, "Chantier PDQ" + jeton);
        fixture.piece(Piece.facture(code("-PRJ"), MARS_10, fournisseur, nom, societe, autre, new BigDecimal("100")));
        piece("-RIEN", "100");

        String parCode = corps(mockMvc, liste(qui(), SEARCH, "PCQ" + jeton));
        String parDescription = corps(mockMvc, liste(qui(), SEARCH, "PDQ" + jeton));

        assertAll(
                () -> assertThat(codes(parCode)).containsExactly(code("-PRJ")),
                () -> assertThat(codes(parDescription)).containsExactly(code("-PRJ")));
    }

    @Test
    void e1_recherche_lotCodeEtDescription() throws Exception {
        UUID lot = fixture.lot("LCQ" + jeton, projet);
        fixture.poserSur("PrjMarket", lot, DESCRIPTION, "Gros oeuvre LDQ" + jeton);
        fixture.piece(Piece.facture(code("-LOT"), MARS_10, fournisseur, nom, societe, projet, new BigDecimal("100"))
                .lot(lot));
        piece("-RIEN", "100");

        String parCode = corps(mockMvc, liste(qui(), SEARCH, "LCQ" + jeton));
        String parDescription = corps(mockMvc, liste(qui(), SEARCH, "LDQ" + jeton));

        assertAll(
                () -> assertThat(codes(parCode)).containsExactly(code("-LOT")),
                () -> assertThat(codes(parDescription)).containsExactly(code("-LOT")));
    }

    @Test
    void e1_recherche_dossierDeGroupement() throws Exception {
        UUID dossier = fixture.dossierGroupement("DG-" + jeton, "Groupement GDQ" + jeton);
        fixture.poser(piece("-DG", "100"), "BuyFolderId", dossier);
        piece("-RIEN", "100");

        String json = corps(mockMvc, liste(qui(), SEARCH, "GDQ" + jeton));

        assertThat(codes(json)).containsExactly(code("-DG"));
    }

    // ------------------------------------------------------------------ Vérification n° 7 : tris et 400

    @ParameterizedTest(name = "tri {0}")
    @ValueSource(strings = {"typeLotLibelle", "moduleLibelle", "societeNom", "archive", "comptabilise",
            "referenceBureauOrdre", "personneAContacter", "dossierGroupement", "aCloturer", "deviseCode", "categories",
            "dateAlerte"})
    void v7_triNouvellesCles_accepte(String cle) throws Exception {
        piece("-T", "100");

        String json = corps(mockMvc, liste(qui(), "sort", cle + ",asc"));

        assertThat(codes(json)).containsExactly(code("-T"));
    }

    @Test
    void v7_triSocieteNom_ordonneParLibelleDeSociete() throws Exception {
        fixture.poserSur("CfgCompany", societe, DESCRIPTION, "B Société " + jeton);
        fixture.poserSur("CfgCompany", societe2, DESCRIPTION, "A Société " + jeton);
        piece("-SB", "100");
        fixture.piece(Piece.facture(code("-SA"), MARS_10, fournisseur, nom, societe2, null, new BigDecimal("100")));

        String asc = corps(mockMvc, liste(qui(), "sort", "societeNom,asc"));
        String desc = corps(mockMvc, liste(qui(), "sort", "societeNom,desc"));

        assertAll(
                () -> assertThat(codes(asc)).containsExactly(code("-SA"), code("-SB")),
                () -> assertThat(codes(desc)).containsExactly(code("-SB"), code("-SA")));
    }

    @Test
    void v7_triCleInconnue_400() throws Exception {
        mockMvc.perform(liste(qui(), "sort", "inconnue,asc")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}={1} → 400")
    @CsvSource({"modules, XXX", "champDate, XXX", "societeIds, pas-un-uuid", "typeLotIds, pas-un-uuid"})
    void v7_valeurInconnue_400(String parametre, String valeur) throws Exception {
        piece("-T", "100");

        mockMvc.perform(get(AppelsAchat.DOCUMENTS).param(parametre, valeur).with(qui()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void v7_temoin_modulesEtChampDateConnus_200() throws Exception {
        piece("-T", "100");

        mockMvc.perform(get(AppelsAchat.DOCUMENTS).param(MODULES, "BUY").param(CHAMP_DATE, "ECHEANCE").with(qui()))
                .andExpect(status().isOk());
    }
}
