package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import com.tsix.immocrm.fixtures.FixtureAchat.ReglementPointe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.pagesPdf;
import static com.tsix.immocrm.fixtures.LectureExport.textePdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Impression « Paiement approuvé » d'une facture d'achat — FEATURE-lc8b-paiement-approuve.md R1-R9. Chaque refus porte
 * un témoin 200 (une route absente répond déjà 404). Texte du PDF comparé sans blancs ({@code compact}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaiementApprouvePdfTest {

    private static final String ROUTE = "/api/achat/documents/{id}/paiement-approuve.pdf";
    private static final String DROIT = "Yes.Stl.Front.Debit.InvoiceList.toolPrint";
    private static final String ROLE = "Commercial";
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String STL_SETTLEMENT = "StlSettlement";
    private static final String DESCRIPTION = "Description";
    private static final LocalDate DATE_PIECE = LocalDate.of(2026, 5, 4);
    private static final String MESSAGE_NATURE = "L'impression Paiement approuvé est réservée aux factures d'achat";

    /** Libellés des 10 retenues du pied long, dans l'ordre OMRAN (R6), sans l'apostrophe (droite ou typographique). */
    private static final List<String> RETENUES = List.of("avance sur marché", "assurance décennale",
            "assurance tous risques chantier", "Retenue de garantie", "Retenue de compte prorata",
            "avance sur décompte", "Remboursement approvisionnement", "Retenues forfaitaires",
            "Remboursement compte prorata", "Fluctuation");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private JdbcTemplate jdbc;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("PAP");
        societe = fixture.societe("SOC-" + jeton);
        projet = fixture.projet("P-" + jeton, societe);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        compte = compteHabilite("PA", DROIT);
    }

    private AdmUser compteHabilite(String code, String... droits) {
        AdmUser c = fixture.compte(code + "-" + jeton, ROLE);
        fixture.habiliterSociete(c, societe);
        fixture.habiliter(c, projet);
        for (String droit : droits) {
            fixture.droitOmran(c, droit);
        }
        return c;
    }

    private String code(String suffixe) {
        return "26/" + jeton + suffixe;
    }

    private UUID piece(String suffixe, String nature, String aPayer) {
        return fixture.piece(Piece.facture(code(suffixe), DATE_PIECE, fournisseur, nom, societe, projet,
                new BigDecimal(aPayer)).nature(nature));
    }

    private MockHttpServletResponse imprimer(UUID id, AdmUser qui) throws Exception {
        return executer(mockMvc, get(ROUTE, id).with(connecte(qui, ROLE))).getResponse();
    }

    private String texte(UUID id) throws Exception {
        MockHttpServletResponse r = imprimer(id, compte);
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);
        return textePdf(r.getContentAsByteArray());
    }

    /** Facture de lot : 2 lignes (20 % et 14 %), les 10 retenues ≠ 0 dont garantie 840, net 12 345,67. */
    private UUID factureDeLot() {
        UUID lot = fixture.lot("LOT-" + jeton, projet);
        UUID id = fixture.piece(Piece.facture(code("-L"), DATE_PIECE, fournisseur, nom, societe, projet,
                new BigDecimal("12345.67")).lot(lot));
        fixture.montants(id, "5350", "6399", "12345.67");
        fixture.poser(id, "AmountRegul", new BigDecimal("15"));
        fixture.poser(id, "FiscalStamp", new BigDecimal("20"));
        fixture.poser(id, "AdvanceAmount", new BigDecimal("111"));
        fixture.poser(id, "InsuranceDECAmount", new BigDecimal("122"));
        fixture.poser(id, "InsuranceTRCAmount", new BigDecimal("133"));
        fixture.poser(id, "WarrantyRatio", new BigDecimal("0.07"));
        fixture.poser(id, "WarrantyAmount", new BigDecimal("840"));
        fixture.poser(id, "ProportionAmount", new BigDecimal("144"));
        fixture.poser(id, "WorkingOutAdvanceAmount", new BigDecimal("155"));
        fixture.poser(id, "StockAmountToPay", new BigDecimal("166"));
        fixture.poser(id, "RetainedAmount", new BigDecimal("177"));
        fixture.poser(id, "ProrataRefundAmount", new BigDecimal("188"));
        fixture.poser(id, "FluctuationsToPay", new BigDecimal("199"));
        fixture.ligne(id, 2, "Enduit " + jeton, "M2", BigDecimal.ONE, new BigDecimal("350"), new BigDecimal("0.14"));
        fixture.ligne(id, 1, "Beton " + jeton, "M3", new BigDecimal("4"), new BigDecimal("1250"), new BigDecimal("0.2"));
        return id;
    }

    /** Règlement propre (pointé 12/05) affecté 2 500 ; avance du BC (échéance 20/04, non pointée) affectée 1 700. */
    private void reglements(UUID facture) {
        UUID type = fixture.typeReglement("VIR-" + jeton);
        UUID propre = fixture.reglement(new ReglementPointe("RG1-" + jeton, new BigDecimal("3000"), new BigDecimal("3500"),
                FixtureAchat.EN_COURS, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12), null), type);
        fixture.poserSur(STL_SETTLEMENT, propre, DESCRIPTION, "Virement propre " + jeton);
        fixture.ligneReglement(propre, facture, new BigDecimal("2500"));

        UUID bc = piece("-BC", FixtureAchat.BON_COMMANDE, "20000");
        fixture.poser(facture, BUY_ORDER_ID, bc);
        UUID avance = piece("-AV", FixtureAchat.DOCUMENT_A_REGLER, "1800");
        fixture.poser(avance, BUY_ORDER_ID, bc);
        fixture.poser(avance, "ReferenceType", FixtureAchat.AVANCE_COMMANDE);
        UUID reglementAvance = fixture.reglement(new ReglementPointe("RG2-" + jeton, new BigDecimal("1800"),
                new BigDecimal("1800"), FixtureAchat.EN_COURS, LocalDate.of(2026, 4, 1), null,
                LocalDate.of(2026, 4, 20)), type);
        fixture.poserSur(STL_SETTLEMENT, reglementAvance, DESCRIPTION, "Avance chantier " + jeton);
        fixture.ligneReglement(reglementAvance, avance, new BigDecimal("1700"));
    }

    // ------------------------------------------------------------------ R1 visibilité, R2 droit, R3 nature, ordre

    // R1 : pièce inconnue → 404 sans octet PDF ; témoin 200 sur une facture visible.
    @Test
    void r1_pieceInconnue_404_sansPdf() throws Exception {
        UUID visible = piece("-V", FixtureAchat.FACTURE, "1200");

        MockHttpServletResponse temoin = imprimer(visible, compte);
        MockHttpServletResponse inconnue = imprimer(UUID.randomUUID(), compte);

        assertAll(
                () -> assertThat(temoin.getStatus()).as("témoin").isEqualTo(200),
                () -> assertThat(inconnue.getStatus()).isEqualTo(404),
                () -> assertThat(estPdf(inconnue.getContentAsByteArray())).isFalse());
    }

    // R1 : pièce d'une société non habilitée → 404 sans octet PDF ; témoin 200.
    @Test
    void r1_horsPerimetreSociete_404_sansPdf() throws Exception {
        UUID visible = piece("-V", FixtureAchat.FACTURE, "1200");
        UUID autreSociete = fixture.societe("SOCX-" + jeton);
        UUID cachee = fixture.piece(Piece.facture(code("-X"), DATE_PIECE, fournisseur, nom, autreSociete,
                fixture.projet("PX-" + jeton, autreSociete), new BigDecimal("1200")));

        MockHttpServletResponse temoin = imprimer(visible, compte);
        MockHttpServletResponse horsPerimetre = imprimer(cachee, compte);

        assertAll(
                () -> assertThat(temoin.getStatus()).as("témoin").isEqualTo(200),
                () -> assertThat(horsPerimetre.getStatus()).isEqualTo(404),
                () -> assertThat(estPdf(horsPerimetre.getContentAsByteArray())).isFalse());
    }

    // R1 : pièce de la société habilitée mais d'un projet non habilité → 404 (même règle que la fiche) ; témoin 200.
    @Test
    void r1_horsPerimetreProjet_404_sansPdf() throws Exception {
        UUID visible = piece("-V", FixtureAchat.FACTURE, "1200");
        UUID cachee = fixture.piece(Piece.facture(code("-Y"), DATE_PIECE, fournisseur, nom, societe,
                fixture.projet("PY-" + jeton, societe), new BigDecimal("1200")));

        MockHttpServletResponse temoin = imprimer(visible, compte);
        MockHttpServletResponse horsPerimetre = imprimer(cachee, compte);

        assertAll(
                () -> assertThat(temoin.getStatus()).as("témoin").isEqualTo(200),
                () -> assertThat(horsPerimetre.getStatus()).isEqualTo(404),
                () -> assertThat(estPdf(horsPerimetre.getContentAsByteArray())).isFalse());
    }

    // R2 : compte habilité sans le droit toolPrint → 403 ; même facture, compte avec le droit → 200.
    @Test
    void r2_sansDroitToolPrint_403_avecDroit_200() throws Exception {
        UUID facture = piece("-D", FixtureAchat.FACTURE, "1200");
        AdmUser sansDroit = compteHabilite("SD");

        MockHttpServletResponse refuse = imprimer(facture, sansDroit);
        MockHttpServletResponse temoin = imprimer(facture, compte);

        assertAll(
                () -> assertThat(refuse.getStatus()).isEqualTo(403),
                () -> assertThat(estPdf(refuse.getContentAsByteArray())).isFalse(),
                () -> assertThat(temoin.getStatus()).as("témoin").isEqualTo(200));
    }

    // R3 : pièce visible qui n'est pas une facture (BC) → 409 avec le message du spec ; témoin 200 sur une facture.
    @Test
    void r3_pieceNonFacture_409_message() throws Exception {
        UUID bonCommande = piece("-BC", FixtureAchat.BON_LIVRAISON, "1200");
        UUID facture = piece("-F", FixtureAchat.FACTURE, "1200");

        MockHttpServletResponse refuse = imprimer(bonCommande, compte);
        MockHttpServletResponse temoin = imprimer(facture, compte);

        assertAll(
                () -> assertThat(refuse.getStatus()).isEqualTo(409),
                () -> assertThat(refuse.getContentAsString(StandardCharsets.UTF_8)).contains(MESSAGE_NATURE),
                () -> assertThat(temoin.getStatus()).as("témoin").isEqualTo(200));
    }

    // R3 : un document à régler (avance) n'est pas une facture non plus → 409.
    @Test
    void r3_documentARegler_409() throws Exception {
        UUID aRegler = piece("-AR", FixtureAchat.DOCUMENT_A_REGLER, "1200");

        MockHttpServletResponse refuse = imprimer(aRegler, compte);

        assertThat(refuse.getStatus()).isEqualTo(409);
    }

    // R2 ordre : hors périmètre ET sans droit → 404 (pas 403) ; témoin : le même compte reçoit 403 sur une facture visible.
    @Test
    void r2_ordre_horsPerimetreSansDroit_404() throws Exception {
        UUID visible = piece("-V", FixtureAchat.FACTURE, "1200");
        UUID autreSociete = fixture.societe("SOCZ-" + jeton);
        UUID cachee = fixture.piece(Piece.facture(code("-Z"), DATE_PIECE, fournisseur, nom, autreSociete, null,
                new BigDecimal("1200")));
        AdmUser sansDroit = compteHabilite("SO");

        MockHttpServletResponse temoin = imprimer(visible, sansDroit);
        MockHttpServletResponse horsPerimetre = imprimer(cachee, sansDroit);

        assertAll(
                () -> assertThat(temoin.getStatus()).as("témoin").isEqualTo(403),
                () -> assertThat(horsPerimetre.getStatus()).isEqualTo(404));
    }

    // R2 ordre : non-facture ET sans droit → 403 (pas 409).
    @Test
    void r2_ordre_nonFactureSansDroit_403() throws Exception {
        UUID bonCommande = piece("-BC", FixtureAchat.BON_LIVRAISON, "1200");
        AdmUser sansDroit = compteHabilite("SN");

        MockHttpServletResponse refuse = imprimer(bonCommande, sansDroit);

        assertThat(refuse.getStatus()).isEqualTo(403);
    }

    // ------------------------------------------------------------------ contrat HTTP

    // Contrat : 200 application/pdf, Content-Disposition inline, nom « paiement-approuve-<code, / → ->.pdf ».
    @Test
    void contrat_200_pdf_inline_nomDeFichierSansBarre() throws Exception {
        UUID facture = piece("-N", FixtureAchat.FACTURE, "1200");

        MockHttpServletResponse r = imprimer(facture, compte);

        assertThat(r.getStatus()).isEqualTo(200);
        assertAll(
                () -> assertThat(r.getContentType()).startsWith("application/pdf"),
                () -> assertThat(estPdf(r.getContentAsByteArray())).isTrue(),
                () -> assertThat(r.getHeader("Content-Disposition")).startsWith("inline")
                        .contains("filename=\"paiement-approuve-26-" + jeton + "-N.pdf\""));
    }

    // ------------------------------------------------------------------ R4-R9 texte du PDF, facture de lot

    // R4 : titre, libellés d'en-tête, valeurs de la pièce, dates jj/mm/aaaa, champ vide « — ».
    @Test
    void r4_enTete_titreLibellesEtValeurs() throws Exception {
        String texte = compact(texte(factureDeLot()));

        assertThat(texte).contains(compact("Paiement Approuvé"))
                .contains(compact(code("-L")))
                .contains(compact("Description " + code("-L")))
                .contains(compact(nom))
                .contains(compact("REF-" + code("-L")))
                .contains(compact("P-" + jeton))
                .contains(compact("LOT-" + jeton))
                .contains("04/05/2026", "03/06/2026", "—")
                .contains(compact("Échéance"), compact("Module"), compact("Entité juridique"), compact("Fournisseur"),
                        compact("Réf. interne"), compact("Personne à contacter"), compact("Projet"),
                        compact("Dossier de groupement"), compact("Lot"))
                .doesNotContain("2026-05-04");
    }

    // R5 : colonnes du tableau des lignes, une ligne PDF par ligne de pièce, dans l'ordre (Sorting).
    @Test
    void r5_lignes_colonnesEtOrdre() throws Exception {
        String texte = compact(texte(factureDeLot()));

        assertThat(texte).contains(compact("PU HT"), compact("TVA (%)"), compact("Total HT"), compact("Rem. (%)"),
                        compact("Total TTC"), compact("Unité"), compact("Quantité"))
                .contains(compact("Beton " + jeton), compact("Enduit " + jeton))
                .doesNotContain(compact("Aucune ligne"));
        assertThat(texte.indexOf(compact("Beton " + jeton))).isLessThan(texte.indexOf(compact("Enduit " + jeton)));
    }

    // R6 pied long (pièce avec lot) : totaux, régul., timbre, les 10 retenues dans l'ordre OMRAN, puis Net à payer.
    @Test
    void r6_piedLong_dixRetenuesDansLOrdre_puisNetAPayer() throws Exception {
        String texte = compact(texte(factureDeLot()));

        assertThat(texte).contains(compact("Total TVA"), compact("Montant Régul."), compact("Timbre fiscal"),
                compact("Net à payer"));
        List<Integer> positions = RETENUES.stream().map(l -> texte.indexOf(compact(l))).toList();
        assertThat(positions).as("positions de %s", RETENUES).doesNotContain(-1).isSorted();
        assertThat(texte.lastIndexOf(compact("Net à payer"))).isGreaterThan(positions.get(positions.size() - 1));
    }

    // R6 / D3 : facture de lot dont seule la garantie est posée → les 10 retenues s'impriment quand même, à 0,00 DH.
    @Test
    void r6_d3_piedLong_retenuesNulles_imprimeesAZero() throws Exception {
        UUID lot = fixture.lot("LOTZ-" + jeton, projet);
        UUID id = fixture.piece(Piece.facture(code("-LZ"), DATE_PIECE, fournisseur, nom, societe, projet,
                new BigDecimal("1200")).lot(lot));
        fixture.poser(id, "WarrantyAmount", new BigDecimal("60"));

        String texte = compact(texte(id));

        assertThat(RETENUES.stream().filter(l -> !texte.contains(compact(l))).toList()).as("retenues absentes").isEmpty();
        assertThat(texte).contains("0,00DH");
    }

    // R6 / R9 : montants lus de la pièce, formatés « x xxx,xx DH » (garantie 840, net 12 345,67).
    @Test
    void r6_r9_montantsLus_formatDH() throws Exception {
        String brut = texte(factureDeLot());

        assertThat(brut).contains("12 345,67 DH", "840,00 DH");
        assertThat(compact(brut)).contains("7%").doesNotContain("12345.67", "12345,670");
    }

    // R7 / R9 : récap TVA = une ligne par taux, Base / TVA (%) / Montant (5 000 à 20 % → 1 000 ; 350 à 14 % → 49).
    @Test
    void r7_recapTva_parTaux() throws Exception {
        String texte = compact(texte(factureDeLot()));

        assertThat(texte).containsPattern("5000,00(DH)?20%1000,00(DH)?")
                .containsPattern("350,00(DH)?14%49,00(DH)?");
    }

    // R8 : règlement propre + règlement de l'avance du BC suffixé « (avance B.C.) » ; Date = pointage sinon échéance,
    // Montant = AmountBase, Affectation = ligne ; pied « Reste à régler » (12 345,67 − 2 500).
    @Test
    void r8_reglements_propreEtAvanceDuBc_resteARegler() throws Exception {
        UUID facture = factureDeLot();
        reglements(facture);

        String texte = compact(texte(facture));

        assertThat(texte).contains(compact("Virement propre " + jeton), "12/05/2026", "3500,00", "2500,00")
                .contains(compact("Avance chantier " + jeton + " (avance B.C.)"), "20/04/2026", "1800,00", "1700,00")
                .contains(compact("Affectation"), compact("Reste à régler"), "9845,67")
                .doesNotContain("10/05/2026", "01/04/2026")
                .doesNotContain(compact("Virement propre " + jeton + " (avance B.C.)"));
    }

    // R9 : le mot « Signature » sur la dernière page.
    @Test
    void r9_signature_dernierePage() throws Exception {
        MockHttpServletResponse r = imprimer(factureDeLot(), compte);
        assertThat(r.getStatus()).isEqualTo(200);

        List<String> pages = pagesPdf(r.getContentAsByteArray());

        assertThat(pages).isNotEmpty();
        assertThat(pages.get(pages.size() - 1)).contains("Signature");
    }

    // ------------------------------------------------------------------ facture libre (sans lot, sans ligne)

    // R5 / R6 : facture sans lot ni ligne → « Aucune ligne », pied court (Amortissement d'avance, Net à payer), pas les
    // retenues du pied long.
    @Test
    void r5_r6_factureLibre_aucuneLigne_piedCourt() throws Exception {
        UUID libre = piece("-LB", FixtureAchat.FACTURE, "7392");

        String brut = texte(libre);
        String texte = compact(brut);

        assertThat(texte).contains(compact("Aucune ligne"), compact("Total HT"), compact("Total TVA"),
                        compact("Total TTC"), compact("Montant Régul."), compact("Timbre fiscal"),
                        compact("Amortissement d"), compact("Net à payer"), "Signature")
                .doesNotContain(compact("Retenue de garantie"), compact("assurance décennale"), compact("Fluctuation"),
                        compact("Retenues forfaitaires"));
        assertThat(brut).contains("7 392,00 DH");
    }

    // ------------------------------------------------------------------ non-régression manuelle (yesRemsProd)

    // Jeu d'acceptation 26/02866 (lot 25/00050) : net 78 696,83 DH, garantie 8 744,09. DB_NAME=yesRemsProd, retirer @Disabled.
    @Disabled("Manuel : facture réelle 26/02866 de yesRemsProd, absente de yesRemsTest et de la CI")
    @Test
    void nonRegression_26_02866_netEtGarantie() throws Exception {
        Map<String, Object> reelle = jdbc.queryForMap("""
                SELECT CAST(PKey AS varchar(36)) AS id, CAST(CfgCompanyId AS varchar(36)) AS societe,
                       CAST(PrjProjectId AS varchar(36)) AS projet
                FROM BuyDocument WHERE Code = '26/02866' AND DocType = 'INVOICE'""");
        AdmUser lecteur = fixture.compte("PA-PROD-" + jeton, ROLE);
        fixture.habiliterSociete(lecteur, UUID.fromString(String.valueOf(reelle.get("societe"))));
        fixture.habiliter(lecteur, UUID.fromString(String.valueOf(reelle.get("projet"))));
        fixture.droitOmran(lecteur, DROIT);

        MockHttpServletResponse r = imprimer(UUID.fromString(String.valueOf(reelle.get("id"))), lecteur);

        assertThat(r.getStatus()).isEqualTo(200);
        assertThat(textePdf(r.getContentAsByteArray())).contains("78 696,83 DH", "8 744,09 DH", "Retenue de garantie",
                "26/02866", "Signature");
    }
}
