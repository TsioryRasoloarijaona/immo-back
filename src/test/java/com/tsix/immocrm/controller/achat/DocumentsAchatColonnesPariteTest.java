package com.tsix.immocrm.controller.achat;

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

import static com.tsix.immocrm.controller.achat.AppelsAchat.champ;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot C8 — FEATURE-achat-liste-parite.md E2 (colonnes), E3 (« À clôturer », Vérification n° 5),
 * Vérification n° 6 (avance à déduire), E6 (export).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatColonnesPariteTest {

    private static final String ROLE = "Commercial";
    private static final String A_CLOTURER = "aCloturer";
    private static final String ORDER = "ORDER";
    private static final LocalDate MARS_10 = LocalDate.of(2026, 3, 10);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ACP");
        societe = fixture.societe("SOC-" + jeton);
        fixture.poserSur("CfgCompany", societe, "Description", "Société " + jeton);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.sansPersonnel(compte); // Q2 : avec un personnel, seuls ses BC seraient visibles
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    private UUID piece(String suffixe, String nature, String aPayer) {
        return fixture.piece(Piece.facture(code(suffixe), MARS_10, fournisseur, nom, societe, null,
                new BigDecimal(aPayer)).nature(nature));
    }

    // ------------------------------------------------------------------ E2 : colonnes de la ligne

    @Test
    void e2_colonnesRenseignees_valeursDesTablesSources() throws Exception {
        UUID type = fixture.typeLot("TRV-" + jeton, "TRAVAUX " + jeton);
        UUID devise = fixture.devise("MAD" + jeton.substring(3, 6));
        UUID dossier = fixture.dossierGroupement("DG-" + jeton, "Groupement " + jeton);
        UUID categorie = fixture.categorie("CAT-" + jeton);
        UUID statut = fixture.statutDEtat("VPP-" + jeton, FixtureAchat.ETAT_INTERMEDIAIRE, "Green");
        UUID p = piece("-C", FixtureAchat.FACTURE, "1200");
        fixture.poser(p, "AdmModuleId", fixture.module("BUY"));
        fixture.poser(p, "PrjMarketTypeId", type);
        fixture.poser(p, "IsClosed", true);
        fixture.poser(p, "IsAccounted", true);
        fixture.poser(p, "OfficeOrderRef", "BO-" + jeton);
        fixture.poser(p, "PersonToContact", "M. Contact " + jeton);
        fixture.poser(p, "BuyFolderId", dossier);
        fixture.poser(p, "StlCurrencyId", devise);
        fixture.poser(p, "BuyDocumentStatusId", statut);
        fixture.ventilation(p, categorie, BigDecimal.ONE);
        String c = code("-C");

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, c, "societeNom")).isEqualTo("Société " + jeton),
                () -> assertThat(champ(json, c, "moduleCode")).isEqualTo("BUY"),
                () -> assertThat(champ(json, c, "moduleLibelle")).isEqualTo("Achat"),
                () -> assertThat(champ(json, c, "typeLotId")).isEqualTo(type.toString()),
                () -> assertThat(champ(json, c, "typeLotLibelle")).isEqualTo("TRAVAUX " + jeton),
                () -> assertThat(champ(json, c, "archive")).isEqualTo(true),
                () -> assertThat(champ(json, c, "comptabilise")).isEqualTo(true),
                () -> assertThat(champ(json, c, "referenceBureauOrdre")).isEqualTo("BO-" + jeton),
                () -> assertThat(champ(json, c, "personneAContacter")).isEqualTo("M. Contact " + jeton),
                () -> assertThat(champ(json, c, "dossierGroupement")).isEqualTo("Groupement " + jeton),
                () -> assertThat(champ(json, c, "categories")).isEqualTo("Categorie CAT-" + jeton),
                () -> assertThat(champ(json, c, "deviseCode")).isEqualTo("MAD" + jeton.substring(3, 6)),
                () -> assertThat(champ(json, c, "dateAlerte")).isNull(),
                () -> assertThat(champ(json, c, A_CLOTURER)).isEqualTo(false),
                () -> assertThat(champ(json, c, "statutCouleur")).isEqualTo("Green"));
    }

    @Test
    void e2_colonnesVides_isClosedNullNonArchive_dateAlerteToujoursNulle() throws Exception {
        piece("-V", FixtureAchat.FACTURE, "100");
        String c = code("-V");

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, c, "archive")).isEqualTo(false),
                () -> assertThat(champ(json, c, "moduleCode")).isNull(),
                () -> assertThat(champ(json, c, "typeLotLibelle")).isNull(),
                () -> assertThat(champ(json, c, "dossierGroupement")).isNull(),
                () -> assertThat(champ(json, c, "deviseCode")).isNull(),
                () -> assertThat(champ(json, c, "dateAlerte")).isNull());
    }

    @Test
    void e2_categories_toutesLesCategoriesDeVentilation() throws Exception {
        UUID p = piece("-K", FixtureAchat.FACTURE, "100");
        fixture.ventilation(p, fixture.categorie("K1-" + jeton), new BigDecimal("0.4"));
        fixture.ventilation(p, fixture.categorie("K2-" + jeton), new BigDecimal("0.6"));

        String json = corps(mockMvc, liste(qui()));

        assertThat(String.valueOf(champ(json, code("-K"), "categories")))
                .contains("Categorie K1-" + jeton, "Categorie K2-" + jeton);
    }

    // ------------------------------------------------------------------ E3 / Vérification n° 5 : « À clôturer »

    /** BC de HT 1 000 (à payer 1 200) au type d'état {@code etat}, factures rattachées de HT = à payer / 1,2. */
    private void bonDeCommande(String suffixe, UUID etat, String... aPayerFactures) {
        UUID bc = piece(suffixe, ORDER, "1200");
        fixture.poser(bc, "BuyDocumentStatusId", fixture.statutDEtat("ST" + suffixe + jeton, etat, null));
        for (int i = 0; i < aPayerFactures.length; i++) {
            UUID f = piece(suffixe + "-F" + i, FixtureAchat.FACTURE, aPayerFactures[i]);
            fixture.poser(f, "BuyOrderId", bc);
        }
    }

    @Test
    void v5_htBcEgalSommeHtFactures_statutIntermediaire_alerte() throws Exception {
        bonDeCommande("-EG", FixtureAchat.ETAT_INTERMEDIAIRE, "600", "600");

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-EG"), A_CLOTURER)).isEqualTo(true);
    }

    @Test
    void v5_htBcInferieurSommeHtFactures_alerte() throws Exception {
        bonDeCommande("-SU", FixtureAchat.ETAT_INTERMEDIAIRE, "1440");

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-SU"), A_CLOTURER)).isEqualTo(true);
    }

    @Test
    void v5_htBcSuperieurSommeHtFactures_pasDAlerte() throws Exception {
        bonDeCommande("-IN", FixtureAchat.ETAT_INTERMEDIAIRE, "600");

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-IN"), A_CLOTURER)).isEqualTo(false);
    }

    @Test
    void v5_termineAvecSucces_pasDAlerte() throws Exception {
        bonDeCommande("-OK", FixtureAchat.ETAT_TERMINE_SUCCES, "1200");

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-OK"), A_CLOTURER)).isEqualTo(false);
    }

    @Test
    void v5_termineAvecEchec_pasDAlerte() throws Exception {
        bonDeCommande("-KO", FixtureAchat.ETAT_TERMINE_ECHEC, "1200");

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-KO"), A_CLOTURER)).isEqualTo(false);
    }

    @Test
    void v5_annulation_alerte() throws Exception {
        bonDeCommande("-AN", FixtureAchat.ETAT_ANNULATION, "1200");

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-AN"), A_CLOTURER)).isEqualTo(true);
    }

    /** 2ᵉ passe : {@code NOT IN} sur un type d'état NULL n'est pas vrai. */
    @Test
    void v5_bcSansStatut_pasDAlerte() throws Exception {
        UUID bc = piece("-NS", ORDER, "1200");
        fixture.poser(piece("-NS-F", FixtureAchat.FACTURE, "1200"), "BuyOrderId", bc);

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-NS"), A_CLOTURER)).isEqualTo(false);
    }

    /** 2ᵉ passe : HT 0 ≤ Σ HT 0 (aucune facture). */
    @Test
    void v5_bcHtZeroSansFacture_alerte() throws Exception {
        UUID bc = piece("-Z0", ORDER, "0");
        fixture.poser(bc, "BuyDocumentStatusId",
                fixture.statutDEtat("STZ0" + jeton, FixtureAchat.ETAT_INTERMEDIAIRE, null));

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-Z0"), A_CLOTURER)).isEqualTo(true);
    }

    @Test
    void v5_seulesLesFacturesComptent_unBlRattacheNeFermePas() throws Exception {
        UUID bc = piece("-BL", ORDER, "1200");
        fixture.poser(bc, "BuyDocumentStatusId",
                fixture.statutDEtat("STBL" + jeton, FixtureAchat.ETAT_INTERMEDIAIRE, null));
        fixture.poser(piece("-BL-L", "DELIVRY", "1200"), "BuyOrderId", bc);

        String json = corps(mockMvc, liste(qui()));

        assertThat(champ(json, code("-BL"), A_CLOTURER)).isEqualTo(false);
    }

    @Test
    void v5_autreNature_toujoursFaux_memeAvecLesConditionsDuBc() throws Exception {
        UUID devis = piece("-DV", "QUOTE", "1200");
        fixture.poser(devis, "BuyDocumentStatusId",
                fixture.statutDEtat("STDV" + jeton, FixtureAchat.ETAT_INTERMEDIAIRE, null));
        fixture.poser(piece("-DV-F", FixtureAchat.FACTURE, "1200"), "BuyOrderId", devis);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, code("-DV"), A_CLOTURER)).isEqualTo(false),
                () -> assertThat(champ(json, code("-DV-F"), A_CLOTURER)).isEqualTo(false));
    }

    // ------------------------------------------------------------------ Vérification n° 6 : avance à déduire

    @Test
    void v6_aDeduire_vraiSousMoins005_fauxAMoins005Exactement() throws Exception {
        piece("-M6", FixtureAchat.DOCUMENT_A_REGLER, "-0.06");
        piece("-M5", FixtureAchat.DOCUMENT_A_REGLER, "-0.05");

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, code("-M6"), "aDeduire")).isEqualTo(true),
                () -> assertThat(champ(json, code("-M5"), "aDeduire")).isEqualTo(false));
    }

    // ------------------------------------------------------------------ E6 : export

    @Test
    void e6_export_nouvellesColonnesEtValeurs() throws Exception {
        UUID type = fixture.typeLot("TRV-" + jeton, "TRAVAUX " + jeton);
        UUID devise = fixture.devise("DEV" + jeton.substring(3, 6));
        UUID p = piece("-X", FixtureAchat.FACTURE, "100");
        fixture.poser(p, "AdmModuleId", fixture.module("BUY"));
        fixture.poser(p, "PrjMarketTypeId", type);
        fixture.poser(p, "OfficeOrderRef", "BO-" + jeton);
        fixture.poser(p, "PersonToContact", "Contact " + jeton);
        fixture.poser(p, "StlCurrencyId", devise);
        fixture.ventilation(p, fixture.categorie("CX-" + jeton), BigDecimal.ONE);

        List<String> textes = AppelsAchat.textesExport(mockMvc,
                get(AppelsAchat.EXPORT).param("format", "xlsx").with(qui()));

        assertAll(
                () -> assertThat(textes).contains("Entité", "Module", "Type", "Archivé", "Comptabilisé",
                        "Réf. bureau d'ordre", "Personne à contacter", "Dossier de groupement", "Catégorie", "Devise",
                        "À clôturer"),
                () -> assertThat(textes).contains("Société " + jeton, "Achat", "TRAVAUX " + jeton, "BO-" + jeton,
                        "Contact " + jeton, "DEV" + jeton.substring(3, 6), "Categorie CX-" + jeton));
    }
}
