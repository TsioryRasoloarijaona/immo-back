package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres.Ligne;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import org.assertj.core.api.SoftAssertions;
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

import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.DOCUMENT_TO_PAY;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.INVOICE;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.MARKET_ADVANCE;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.MARKET_PROPORTION;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.MARKET_WARRANTY;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.REFUND;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.WORKINGOUT_ADVANCE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sous-fenêtres du lot en lecture (FEATURE-lot-sous-fenetres.md §3, §4) : Avances, Avoirs, Avenants, Remboursements, Bordereau
 * estimatif, Bordereau définitif étendu, Prévisions. Aucune de ces fenêtres n'a de ligne en prod (sauf avances décompte,
 * bordereaux, prévisions) : chaque route a sa fixture, valeurs calculées à la main.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsSousFenetresTest {

    private static final String AVANCES = "avances";
    private static final String AVOIRS = "avoirs";
    private static final String AVENANTS = "avenants";
    private static final String REMBOURSEMENTS = "remboursements";
    private static final String ESTIMATIF = "bordereau-estimatif";
    private static final String DEFINITIF = "bordereau";
    private static final String PREVISIONS = "previsions";

    private static final String SUR_MARCHE = "$.surMarche";
    private static final String SUR_DECOMPTE = "$.surDecompte";
    private static final String IDS = "[*].id";
    private static final String PREMIER = "[0].";
    private static final String NET = "netAPayer";
    private static final String REGLE = "regle";
    private static final String RESTE = "reste";
    private static final String LIGNES = "$.lignes";
    private static final String GROUPES = "$.groupes";
    private static final String TOTAUX = "$.totaux.";
    private static final String MOIS = "$.mois";
    private static final String POINTE = "POINTED";
    private static final String ANNULE = "CANCELED";
    private static final String STATUT_LIBELLE = "Validé DG ";
    private static final String COULEUR = "Orange";
    private static final String JOUR_MARS = "2026-03-05";
    private static final String VERROUILLE = "$.verrouille";
    private static final String LOT_ID = "$.lotId";

    private static final LocalDate MARS = LocalDate.of(2026, 3, 5);
    private static final LocalDate AVRIL = LocalDate.of(2026, 4, 10);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureLotSousFenetres sf;
    @Autowired private FixtureVisibiliteOmran visibilite;

    private AppelsProjet appels;
    private String jeton;
    private UUID societe;
    private UUID autreSociete;
    private UUID projet;
    private UUID statutLot;
    private UUID statutPiece;
    private UUID devise;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        fiche.entitesPiecesLot();
        sf.tables();
        societe = parite.entiteJuridique("SFE-" + jeton);
        autreSociete = parite.entiteJuridique("SFX-" + jeton);
        projet = parite.projet("SFP-" + jeton, societe);
        statutLot = parite.statut(ENTREPRENEURS, "SFS-" + jeton);
        statutPiece = sf.statutPiece("SFB-" + jeton, STATUT_LIBELLE + jeton, COULEUR);
        devise = sf.devise("SFD" + jeton);
        AdmUser compte = parite.administrateur("lot.sf." + jeton, projet);
        visibilite.habiliterSociete(compte, societe);
        qui = connecte(compte, "Administrateur");
    }

    private UUID lot(String code) {
        return parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statutLot);
    }

    private Reponse ok(UUID lot, String route) throws Exception {
        Reponse r = appels.lire(qui, LOTS + "/" + lot + "/" + route);
        assertThat(r.statut()).as(route + " : " + r.corps()).isEqualTo(200);
        return r;
    }

    private UUID piece(UUID lot, String code, String nature, String reference, LocalDate date, String aPayer) {
        return sf.piece(lot, projet, societe, code + "-" + jeton, nature, reference, date, aPayer);
    }

    private void regler(UUID piece, String code, String montant, String statut) {
        lots.reglement(piece, code + "-" + jeton, new BigDecimal(montant), statut);
    }

    // ------------------------------------------------------------------ §4.1 Avances : deux grilles, valeurs exactes

    @Test
    void avances_deuxGrilles_valeursExactes_regleEtResteDeSoldeDocumentAchat() throws Exception {
        UUID lot = lot("AV");
        UUID am = piece(lot, "AM", DOCUMENT_TO_PAY, MARKET_ADVANCE, MARS, "1000");
        sf.entete(am, statutPiece, devise, "Avance de démarrage");
        regler(am, "RAM1", "300", POINTE);
        regler(am, "RAM2", "200", ANNULE);
        UUID ad = piece(lot, "AD", DOCUMENT_TO_PAY, WORKINGOUT_ADVANCE, AVRIL, "500");
        regler(ad, "RAD", "500", POINTE);

        Reponse r = ok(lot, AVANCES);

        String m = SUR_MARCHE + PREMIER;
        String d = SUR_DECOMPTE + PREMIER;
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(SUR_MARCHE + IDS)).containsExactly(am.toString());
            s.assertThat(r.ids(SUR_DECOMPTE + IDS)).containsExactly(ad.toString());
            s.assertThat(r.valeur(m + "code")).isEqualTo("AM-" + jeton);
            s.assertThat(r.valeur(m + "statutId")).isEqualTo(statutPiece.toString());
            s.assertThat(r.valeur(m + "statutDescription")).isEqualTo(STATUT_LIBELLE + jeton);
            s.assertThat(r.valeur(m + "statutCouleur")).isEqualTo(COULEUR);
            s.assertThat(r.valeur(m + "date")).isEqualTo(JOUR_MARS);
            s.assertThat(r.nombre(m + NET)).isEqualByComparingTo("1000");
            s.assertThat(r.nombre(m + REGLE)).as("règlement annulé exclu (Q3)").isEqualByComparingTo("300");
            s.assertThat(r.nombre(m + RESTE)).as("1 000 − 300").isEqualByComparingTo("700");
            s.assertThat(r.valeur(m + "deviseCode")).isEqualTo("SFD" + jeton);
            s.assertThat(r.valeur(m + "notes")).isEqualTo("Avance de démarrage");
            s.assertThat(r.valeur(d + "code")).isEqualTo("AD-" + jeton);
            s.assertThat(r.valeur(d + "date")).isEqualTo("2026-04-10");
            s.assertThat(r.nombre(d + NET)).isEqualByComparingTo("500");
            s.assertThat(r.nombre(d + REGLE)).isEqualByComparingTo("500");
            s.assertThat(r.nombre(d + RESTE)).isEqualByComparingTo("0");
        });
    }

    @Test
    void avances_recuperationNegative_resteNegatif_regleZeroSansLigne() throws Exception {
        UUID lot = lot("AN");
        piece(lot, "AN", DOCUMENT_TO_PAY, WORKINGOUT_ADVANCE, MARS, "-200");

        Reponse r = ok(lot, AVANCES);

        String d = SUR_DECOMPTE + PREMIER;
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(d + NET)).isEqualByComparingTo("-200");
            s.assertThat(r.nombre(d + REGLE)).as("aucune ligne de règlement").isEqualByComparingTo("0");
            s.assertThat(r.nombre(d + RESTE)).as("avance à déduire").isEqualByComparingTo("-200");
        });
    }

    // §3 règle 5 : avances par date décroissante.
    @Test
    void avances_triParDateDecroissante() throws Exception {
        UUID lot = lot("AT");
        UUID mars = piece(lot, "AT1", DOCUMENT_TO_PAY, MARKET_ADVANCE, MARS, "100");
        UUID avril = piece(lot, "AT2", DOCUMENT_TO_PAY, MARKET_ADVANCE, AVRIL, "100");

        Reponse r = ok(lot, AVANCES);

        assertThat(r.ids(SUR_MARCHE + IDS)).containsExactly(avril.toString(), mars.toString());
    }

    // §4.1 : DocType DOCUMENT_TO_PAY, ReferenceType et PrjMarketId du lot ; une facture ou l'avance d'un autre lot n'y sont pas.
    @Test
    void avances_seulementDocumentsAPayerDuLot_parReference() throws Exception {
        UUID lot = lot("AF");
        UUID autreLot = lot("AG");
        UUID attendue = piece(lot, "AF1", DOCUMENT_TO_PAY, MARKET_ADVANCE, MARS, "100");
        piece(autreLot, "AF2", DOCUMENT_TO_PAY, MARKET_ADVANCE, MARS, "100");
        piece(lot, "AF3", INVOICE, null, MARS, "100");
        piece(lot, "AF4", DOCUMENT_TO_PAY, MARKET_WARRANTY, MARS, "100");

        Reponse r = ok(lot, AVANCES);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(SUR_MARCHE + IDS)).containsExactly(attendue.toString());
            s.assertThat(r.ids(SUR_DECOMPTE + IDS)).isEmpty();
        });
    }

    // §3 règle 2 / Q2 : pièce d'une société non habilitée omise, jamais 404.
    @Test
    void q2_avances_societeNonHabilitee_omise_reponse200() throws Exception {
        UUID lot = lot("AS");
        UUID visible = piece(lot, "AS1", DOCUMENT_TO_PAY, MARKET_ADVANCE, MARS, "100");
        sf.piece(lot, projet, autreSociete, "AS2-" + jeton, DOCUMENT_TO_PAY, MARKET_ADVANCE, MARS, "100");
        sf.piece(lot, projet, autreSociete, "AS3-" + jeton, DOCUMENT_TO_PAY, WORKINGOUT_ADVANCE, MARS, "100");

        Reponse r = ok(lot, AVANCES);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(SUR_MARCHE + IDS)).containsExactly(visible.toString());
            s.assertThat(r.ids(SUR_DECOMPTE + IDS)).isEmpty();
        });
    }

    // §3 règle 2 : seules les pièces CanBePayed = 1.
    @Test
    void r2_avances_pieceNonPayable_omise() throws Exception {
        UUID lot = lot("AP");
        UUID payable = piece(lot, "AP1", DOCUMENT_TO_PAY, WORKINGOUT_ADVANCE, MARS, "100");
        sf.nonPayable(piece(lot, "AP2", DOCUMENT_TO_PAY, WORKINGOUT_ADVANCE, MARS, "100"));

        Reponse r = ok(lot, AVANCES);

        assertThat(r.ids(SUR_DECOMPTE + IDS)).containsExactly(payable.toString());
    }

    // ------------------------------------------------------------------ §4.2 Avoirs

    @Test
    void avoirs_valeursExactes_seulementLesAvoirsDuLot() throws Exception {
        UUID lot = lot("AO");
        UUID avoir = piece(lot, "AO1", REFUND, "", MARS, "120");
        sf.montants(avoir, "100", "20", "120");
        sf.entete(avoir, statutPiece, devise, null);
        sf.avoir(avoir, "M. Contact", "BO-" + jeton);
        piece(lot("AQ"), "AO2", REFUND, "", MARS, "50");
        piece(lot, "AO3", INVOICE, "", MARS, "50");

        Reponse r = ok(lot, AVOIRS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$" + IDS)).containsExactly(avoir.toString());
            s.assertThat(r.valeur("$[0].date")).isEqualTo(JOUR_MARS);
            s.assertThat(r.valeur("$[0].code")).isEqualTo("AO1-" + jeton);
            s.assertThat(r.valeur("$[0].statutDescription")).isEqualTo(STATUT_LIBELLE + jeton);
            s.assertThat(r.valeur("$[0].statutCouleur")).isEqualTo(COULEUR);
            s.assertThat(r.valeur("$[0].personneAContacter")).isEqualTo("M. Contact");
            s.assertThat(r.nombre("$[0].montantHT")).isEqualByComparingTo("100");
            s.assertThat(r.nombre("$[0].montantTVA")).isEqualByComparingTo("20");
            s.assertThat(r.nombre("$[0].montantTTC")).isEqualByComparingTo("120");
            s.assertThat(r.valeur("$[0].referenceBureauOrdre")).isEqualTo("BO-" + jeton);
        });
    }

    // §3 règle 2 / Q2 : avoir d'une société non habilitée ou non payable omis.
    @Test
    void q2_avoirs_societeNonHabiliteeEtNonPayable_omis() throws Exception {
        UUID lot = lot("AR");
        UUID visible = piece(lot, "AR1", REFUND, "", MARS, "10");
        sf.piece(lot, projet, autreSociete, "AR2-" + jeton, REFUND, "", MARS, "10");
        sf.nonPayable(piece(lot, "AR3", REFUND, "", MARS, "10"));

        Reponse r = ok(lot, AVOIRS);

        assertThat(r.ids("$" + IDS)).containsExactly(visible.toString());
    }

    @Test
    void avoirs_lotSansAvoir_listeVide() throws Exception {
        Reponse r = ok(lot("AV0"), AVOIRS);

        assertThat(r.ids("$" + IDS)).isEmpty();
    }

    // ------------------------------------------------------------------ §4.3 Avenants

    @Test
    void avenants_valeursExactes_factureCode_triDateDecroissante() throws Exception {
        UUID lot = lot("AK");
        UUID av1 = sf.avenant(lot, 1, "AK1-" + jeton, MARS, "500", "100", "600", "580");
        UUID av2 = sf.avenant(lot, 2, "AK2-" + jeton, AVRIL, "50", "10", "60", "60");
        UUID facture = piece(lot, "AKF", INVOICE, "", MARS, "300");
        sf.rattacherAvenant(facture, av1);
        sf.avenant(lot("AL"), 1, "AK3-" + jeton, MARS, "1", "0", "1", "1");

        Reponse r = ok(lot, AVENANTS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$" + IDS)).containsExactly(av2.toString(), av1.toString());
            s.assertThat(r.nombre("$[1].ordre")).isEqualByComparingTo("1");
            s.assertThat(r.valeur("$[1].date")).isEqualTo(JOUR_MARS);
            s.assertThat(r.valeur("$[1].code")).isEqualTo("AK1-" + jeton);
            s.assertThat(r.valeur("$[1].description")).isEqualTo("Avenant AK1-" + jeton);
            s.assertThat(r.nombre("$[1].montantHT")).isEqualByComparingTo("500");
            s.assertThat(r.nombre("$[1].montantTVA")).isEqualByComparingTo("100");
            s.assertThat(r.nombre("$[1].montantTTC")).isEqualByComparingTo("600");
            s.assertThat(r.nombre("$[1].netAPayer")).isEqualByComparingTo("580");
            s.assertThat(r.valeur("$[1].factureCode")).as("PrjClauseView.BuyDocumentCode").isEqualTo("AKF-" + jeton);
        });
    }

    // ------------------------------------------------------------------ §4.4 Remboursements : 4 listes

    @Test
    void remboursements_echeancesTrieesParOrdre_ratioEnPourcent() throws Exception {
        UUID lot = lot("RE");
        UUID e2 = sf.echeanceRg(lot, 2, "Solde", 730, LocalDate.of(2028, 6, 30), "0.5");
        UUID e1 = sf.echeanceRg(lot, 1, "Réception provisoire", 365, LocalDate.of(2027, 6, 30), "0.5");
        sf.echeanceRg(lot("RF"), 1, "Autre lot", 1, MARS, "1");

        Reponse r = ok(lot, REMBOURSEMENTS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$.echeances" + IDS)).containsExactly(e1.toString(), e2.toString());
            s.assertThat(r.nombre("$.echeances[0].ordre")).isEqualByComparingTo("1");
            s.assertThat(r.valeur("$.echeances[0].description")).isEqualTo("Réception provisoire");
            s.assertThat(r.nombre("$.echeances[0].delaiJours")).isEqualByComparingTo("365");
            s.assertThat(r.valeur("$.echeances[0].echeance")).isEqualTo("2027-06-30");
            s.assertThat(r.nombre("$.echeances[0].pourcentage")).as("Ratio × 100").isEqualByComparingTo("50");
        });
    }

    @Test
    void remboursements_receptions_typeGenreEtDefinitive() throws Exception {
        UUID lot = lot("RR");
        UUID type = sf.typeReception("RT-" + jeton);
        UUID genre = sf.genreReception("RG-" + jeton, true);
        UUID rec = sf.reception(lot, 1, "RC-" + jeton, MARS, true, "Chantier nord", type, genre);

        Reponse r = ok(lot, REMBOURSEMENTS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$.receptions" + IDS)).containsExactly(rec.toString());
            s.assertThat(r.nombre("$.receptions[0].ordre")).isEqualByComparingTo("1");
            s.assertThat(r.valeur("$.receptions[0].code")).isEqualTo("RC-" + jeton);
            s.assertThat(r.valeur("$.receptions[0].dateReception")).isEqualTo(JOUR_MARS);
            s.assertThat(r.valeur("$.receptions[0].demandeeParFournisseur")).isEqualTo(true);
            s.assertThat(r.valeur("$.receptions[0].lieu")).isEqualTo("Chantier nord");
            s.assertThat(r.valeur("$.receptions[0].typeDescription")).isEqualTo("Type RT-" + jeton);
            s.assertThat(r.valeur("$.receptions[0].genreDescription")).isEqualTo("Genre RG-" + jeton);
            s.assertThat(r.valeur("$.receptions[0].definitive")).isEqualTo(true);
        });
    }

    @Test
    void remboursements_rgEtProrata_pieces_soldeDocumentAchat_societeNonHabiliteeOmise() throws Exception {
        UUID lot = lot("RP");
        UUID rg = piece(lot, "RPG", DOCUMENT_TO_PAY, MARKET_WARRANTY, MARS, "400");
        regler(rg, "RRG", "100", POINTE);
        UUID prorata = piece(lot, "RPP", DOCUMENT_TO_PAY, MARKET_PROPORTION, MARS, "250");
        sf.piece(lot, projet, autreSociete, "RPX-" + jeton, DOCUMENT_TO_PAY, MARKET_WARRANTY, MARS, "10");
        piece(lot, "RPA", DOCUMENT_TO_PAY, MARKET_ADVANCE, MARS, "10");

        Reponse r = ok(lot, REMBOURSEMENTS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$.remboursementsRg" + IDS)).containsExactly(rg.toString());
            s.assertThat(r.ids("$.remboursementsProrata" + IDS)).containsExactly(prorata.toString());
            s.assertThat(r.nombre("$.remboursementsRg[0]." + NET)).isEqualByComparingTo("400");
            s.assertThat(r.nombre("$.remboursementsRg[0]." + REGLE)).isEqualByComparingTo("100");
            s.assertThat(r.nombre("$.remboursementsRg[0]." + RESTE)).isEqualByComparingTo("300");
            s.assertThat(r.nombre("$.remboursementsProrata[0]." + NET)).isEqualByComparingTo("250");
        });
    }

    @Test
    void remboursements_lotVide_quatreListesVides() throws Exception {
        Reponse r = ok(lot("RV"), REMBOURSEMENTS);

        SoftAssertions.assertSoftly(s -> {
            for (String liste : List.of("echeances", "receptions", "remboursementsRg", "remboursementsProrata")) {
                s.assertThat(r.ids("$." + liste + IDS)).as(liste).isEmpty();
            }
        });
    }

    // ------------------------------------------------------------------ §4.5 Bordereau estimatif

    @Test
    void bordereauEstimatif_arbreDesGroupes_lignesHorsStock_ponderationEtTotaux() throws Exception {
        UUID lot = lot("BE");
        UUID g1 = sf.groupeEstimatif(lot, null, 1, "G1-" + jeton, "Notes G1");
        UUID g2 = sf.groupeEstimatif(lot, g1, 2, "G2-" + jeton, null);
        UUID a = sf.ligneEstimative(lot, new Ligne(1, "A", "2", "100", "120", false, g2, "0.3"));
        UUID b = sf.ligneEstimative(lot, new Ligne(2, "B", "3", "50", "60", false, g1, null));
        sf.ligneEstimative(lot, new Ligne(3, "S", "10", "1000", "1200", true, g1, null));
        sf.ligneDefinitive(lot, new Ligne(4, "D", "1", "999", "1198.8", false, null, null));

        Reponse r = ok(lot, ESTIMATIF);

        String g1Chemin = "$.groupes[?(@.id == '" + g1 + "')].";
        String g2Chemin = "$.groupes[?(@.id == '" + g2 + "')].";
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(LOT_ID)).isEqualTo(lot.toString());
            s.assertThat(r.valeur(VERROUILLE)).as("cadenas OMRAN").isEqualTo(true);
            s.assertThat(r.ids(GROUPES + IDS)).containsExactlyInAnyOrder(g1.toString(), g2.toString());
            s.assertThat(r.ids(g1Chemin + "parentId")).as("racine").containsExactly((String) null);
            s.assertThat(r.ids(g2Chemin + "parentId")).containsExactly(g1.toString());
            s.assertThat(r.ids(g1Chemin + "code")).containsExactly("G1-" + jeton);
            s.assertThat(r.ids(g1Chemin + "description")).containsExactly("Groupe G1-" + jeton);
            s.assertThat(r.ids(g1Chemin + "notes")).containsExactly("Notes G1");
            s.assertThat(String.valueOf(r.valeur(g1Chemin + "ordre"))).isEqualTo("[1]");
            s.assertThat(String.valueOf(r.valeur(g1Chemin + "stock"))).isEqualTo("[false]");
            s.assertThat(r.ids(LIGNES + IDS)).as("stock exclu, tri par ordre").containsExactly(a.toString(), b.toString());
            s.assertThat(r.valeur("$.lignes[0].groupeId")).isEqualTo(g2.toString());
            s.assertThat(r.valeur("$.lignes[1].groupeId")).isEqualTo(g1.toString());
            s.assertThat(r.nombre("$.lignes[0].ponderation")).as("WeightingRatio tel que stocké").isEqualByComparingTo("0.3");
            s.assertThat(r.valeur("$.lignes[1].ponderation")).isNull();
            s.assertThat(r.nombre("$.lignes[0].totalHT")).isEqualByComparingTo("200");
            s.assertThat(r.nombre("$.lignes[0].totalTTC")).isEqualByComparingTo("240");
            s.assertThat(r.nombre("$.lignes[0].tauxTVA")).isEqualByComparingTo("20");
            s.assertThat(r.nombre(TOTAUX + "totalHT")).as("200 + 150").isEqualByComparingTo("350");
            s.assertThat(r.nombre(TOTAUX + "totalTVA")).isEqualByComparingTo("70");
            s.assertThat(r.nombre(TOTAUX + "totalTTC")).isEqualByComparingTo("420");
            s.assertThat(r.nombre(TOTAUX + "nbLignes")).isEqualByComparingTo("2");
        });
    }

    // §8 : invariant Σ lignes du bordereau estimatif = totaux.
    @Test
    void bordereauEstimatif_invariant_sommeDesLignes_egaleTotaux() throws Exception {
        UUID lot = lot("BI");
        sf.ligneEstimative(lot, new Ligne(1, "I1", "1.5", "40", "48", false, null, null));
        sf.ligneEstimative(lot, new Ligne(2, "I2", "2.25", "80", "96", false, null, null));
        sf.ligneEstimative(lot, new Ligne(3, "I3", "4", "12.5", "15", false, null, null));

        Reponse r = ok(lot, ESTIMATIF);
        List<Object> ht = valeurs(r, "$.lignes[*].totalHT");
        List<Object> ttc = valeurs(r, "$.lignes[*].totalTTC");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ht).hasSize(3);
            s.assertThat(somme(ht)).isEqualByComparingTo(r.nombre(TOTAUX + "totalHT")).isEqualByComparingTo("290");
            s.assertThat(somme(ttc)).isEqualByComparingTo(r.nombre(TOTAUX + "totalTTC")).isEqualByComparingTo("348");
        });
    }

    @Test
    void bordereauEstimatif_lotSansLigne_videEtVerrouille() throws Exception {
        UUID lot = lot("BV");

        Reponse r = ok(lot, ESTIMATIF);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(LIGNES + IDS)).isEmpty();
            s.assertThat(r.ids(GROUPES + IDS)).isEmpty();
            s.assertThat(r.valeur(VERROUILLE)).isEqualTo(true);
            s.assertThat(r.nombre(TOTAUX + "totalTTC")).isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ §4.5 Bordereau définitif étendu : groupes, groupeId, pondération

    @Test
    void bordereauDefinitif_etendu_groupesGroupeIdEtPonderation_totauxInchanges() throws Exception {
        UUID lot = lot("BD");
        UUID g1 = sf.groupeDefinitif(lot, null, 1, "D1-" + jeton, null);
        UUID g2 = sf.groupeDefinitif(lot, g1, 2, "D2-" + jeton, null);
        UUID a = sf.ligneDefinitive(lot, new Ligne(1, "A", "2", "100", "120", false, g2, "0.25"));
        sf.ligneDefinitive(lot, new Ligne(2, "B", "1", "50", "60", false, null, null));
        sf.ligneEstimative(lot, new Ligne(3, "E", "1", "999", "1198.8", false, null, null));

        Reponse r = ok(lot, DEFINITIF);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(GROUPES + IDS)).containsExactlyInAnyOrder(g1.toString(), g2.toString());
            s.assertThat(r.ids("$.groupes[?(@.id == '" + g2 + "')].parentId")).containsExactly(g1.toString());
            s.assertThat(r.ids(LIGNES + IDS)).hasSize(2).first().isEqualTo(a.toString());
            s.assertThat(r.valeur("$.lignes[0].groupeId")).isEqualTo(g2.toString());
            s.assertThat(r.nombre("$.lignes[0].ponderation")).isEqualByComparingTo("0.25");
            s.assertThat(r.valeur("$.lignes[1].groupeId")).isNull();
            s.assertThat(r.nombre(TOTAUX + "totalHT")).as("200 + 50, ligne estimative exclue").isEqualByComparingTo("250");
        });
    }

    // ------------------------------------------------------------------ §4.6 Prévisions

    @Test
    void previsions_moisConsecutifsDepuisPremierPointMoinsUnMois_previsionEtReel() throws Exception {
        UUID lot = lot("PV");
        sf.prevision(lot, LocalDate.of(2026, 3, 31), "2000", "0.25");
        sf.prevision(lot, LocalDate.of(2026, 5, 31), "3000", "0.6");
        sf.echeance(piece(lot, "PF1", INVOICE, "", MARS, "1000"), LocalDate.of(2026, 4, 15));
        sf.echeance(piece(lot, "PF2", INVOICE, "", MARS, "500"), LocalDate.of(2026, 4, 20));
        sf.echeance(piece(lot, "PA", REFUND, "", MARS, "300"), LocalDate.of(2026, 4, 25));

        Reponse r = ok(lot, PREVISIONS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(LOT_ID)).isEqualTo(lot.toString());
            s.assertThat(r.ids("$.mois[*].mois")).as("février (premier point − 1 mois) à mai, sans trou")
                    .containsExactly("2026-02-01", "2026-03-01", "2026-04-01", "2026-05-01");
            s.assertThat(r.nombre("$.mois[1].prevision")).isEqualByComparingTo("2000");
            s.assertThat(r.nombre("$.mois[1].avancementPrevision")).isEqualByComparingTo("0.25");
            s.assertThat(r.nombre("$.mois[3].prevision")).isEqualByComparingTo("3000");
            s.assertThat(r.nombre("$.mois[3].avancementPrevision")).isEqualByComparingTo("0.6");
            s.assertThat(r.nombre("$.mois[2].reel")).as("1 000 + 500, avoir exclu, mois de l'échéance")
                    .isEqualByComparingTo("1500");
        });
    }

    @Test
    void previsions_lotSansAucunPoint_moisVide() throws Exception {
        Reponse r = ok(lot("P0"), PREVISIONS);

        assertThat(r.ids(MOIS + "[*].mois")).isEmpty();
    }

    // ------------------------------------------------------------------ utilitaires

    private static List<Object> valeurs(Reponse r, String chemin) {
        return JsonPath.read(r.corps(), chemin);
    }

    private static BigDecimal somme(List<Object> valeurs) {
        return valeurs.stream().map(v -> new BigDecimal(String.valueOf(v))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
