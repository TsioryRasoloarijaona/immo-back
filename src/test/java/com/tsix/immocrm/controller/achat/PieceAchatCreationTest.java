package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NatureSaisie;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureAchat.SequenceNature;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2a E-C4 (FEATURE-achat-fiche-creation.md, Vérification n° 2) : {@code POST /api/achat/documents} par nature — séquence de
 * la nature (non consommée sur un refus), gestion, statut de départ OMRAN (EN COURS ; devis NULL ; BL à cycle sans 500),
 * historique, échéance, constantes, Q-6 (facture à lot : taux du lot) ; {@code GET …/modele} pré-rempli.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatCreationTest {

    private static final String STATUT = "BuyDocumentStatusId";
    private static final String ECHEANCE = "MaturityDate";
    private static final String DELAI = "PaymentDelay";
    private static final String HISTORIQUE = "SELECT * FROM BuyDocumentStatusHistory WHERE BuyDocumentId = :id";
    private static final List<String> RATIOS_LOT = List.of("AdvanceRatio", "WarrantyRatio", "ProportionRatio",
            "InsuranceDECRatio", "InsuranceTRCRatio");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ACR");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "ACR", ROLE_JWT, NOUVEAU_DEVIS, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
    }

    private MvcResult creerPar(AdmUser compte, CorpsJson corps) throws Exception {
        return appeler(mockMvc, creer(corps), connecte(compte, ROLE_JWT));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        MvcResult r = creerPar(redacteur, corps);
        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        return idCree(r);
    }

    private Map<String, Object> enBase(UUID piece) {
        return fixture.pieceEnBase(piece);
    }

    private static LocalDate jour(Map<String, Object> ligne, String colonne) {
        return FixtureTerrains.horodatage(ligne.get(colonne)).toLocalDate();
    }

    // ------------------------------------------------------------------ E-C4.1 séquence, E-C4.2 gestion

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec41_creation_201_codeDeLaSequenceDeLaNature_seuleCetteSequenceAvance(NatureSaisie n) throws Exception {
        MvcResult r = creerPar(redacteur, corpsValide(c, s, n.code));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        UUID piece = idCree(r);
        Map<String, Object> d = enBase(piece);
        assertAll(
                () -> assertThat(r.getResponse().getHeader("Location")).isNotNull().containsIgnoringCase(piece.toString()),
                () -> assertThat((String) JsonPath.read(corps(r), "$.code")).isEqualTo(n.sequence().codeSuivant()),
                () -> assertThat(d).containsEntry("Code", n.sequence().codeSuivant()).containsEntry("DocType", n.code),
                () -> assertThat(guidDe(d, "StrEntityId")).isEqualTo(id(s.gestion(n.gestion))),
                () -> assertThat((String) JsonPath.read(corps(r), "$.entiteCode")).isEqualTo(n.gestion));
        for (SequenceNature autre : FixtureAchat.SEQUENCES_SAISIE.values()) {
            int attendu = autre.equals(n.sequence()) ? autre.valeur() + 1 : autre.valeur();
            assertThat(fixture.valeurSequence(autre.code())).as(autre.code()).isEqualTo(attendu);
        }
    }

    /** E-C4.1 : un refus (400) ne consomme pas de numéro. */
    @Test
    void ec41_refus400_sequenceNonConsommee_rienEcrit() throws Exception {
        CorpsJson ligneSansDescription = ligne("ART-1", "1", "100", s.categorie()).avec("description", "");

        MvcResult r = creerPar(redacteur, corpsValide(c, s, FixtureAchat.BON_COMMANDE)
                .avec("lignes", List.of(ligneSansDescription)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        fixture.oublier();
        assertAll(
                () -> assertThat(fixture.valeurSequence(NatureSaisie.BC.sequence().code())).isEqualTo(600),
                () -> assertThat(fixture.nombre("BuyDocument", "CfgTierId", c.fournisseur())).isZero());
    }

    /** E-C4.2 : {@code AdmModuleId} = {@code moduleId} du corps (ici le module Projet). */
    @Test
    void ec42_moduleDuCorps_ecrit() throws Exception {
        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.FACTURE).avec("moduleId", s.moduleProjet()));

        assertThat(guidDe(enBase(piece), "AdmModuleId")).isEqualTo(id(s.moduleProjet()));
    }

    // ------------------------------------------------------------------ E-C4.3 statut de départ

    /** E-C4.3 : EN COURS (départ « Nouveau ») pour BC, BL (gestion à cycle EN COURS ↔ VALIDÉ DG : plus de 500) et facture. */
    @ParameterizedTest
    @EnumSource(value = NatureSaisie.class, names = "DEVIS", mode = EnumSource.Mode.EXCLUDE)
    void ec43_statutDeDepart_enCours(NatureSaisie n) throws Exception {
        UUID piece = creerOk(corpsValide(c, s, n.code));

        assertThat(guidDe(enBase(piece), STATUT)).isEqualTo(id(s.enCours()));
    }

    /** E-C4.3 : gestion sans transition (devis) → statut NULL, sans erreur. */
    @Test
    void ec43_devis_sansTransition_statutNull() throws Exception {
        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.DEVIS));

        assertThat(enBase(piece)).containsEntry(STATUT, null);
    }

    /** E-C4.3 : transitions dont aucun départ n'est « Nouveau » → 500, séquence intacte, rien d'écrit. */
    @Test
    void ec43_transitionsSansDepartNouveau_500_sequenceIntacte_rienEcrit() throws Exception {
        UUID gestion = fixture.gestionVide(FixtureAchat.GESTION_BC, s.gestion(FixtureAchat.GESTION_BC));
        fixture.transitionDe(gestion, 10, s.valideDg(), s.valideDt());

        MvcResult r = creerPar(redacteur, corpsValide(c, s, FixtureAchat.BON_COMMANDE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(500);
        fixture.oublier();
        assertAll(
                () -> assertThat(fixture.valeurSequence(NatureSaisie.BC.sequence().code())).isEqualTo(600),
                () -> assertThat(fixture.nombre("BuyDocument", "CfgTierId", c.fournisseur())).isZero());
    }

    /** E-C4.3 : la 1re transition par {@code Sorting} au départ « Nouveau » (une transition antérieure à départ non Nouveau est sautée). */
    @Test
    void ec43_premiereTransitionAuDepartNouveau_parSorting() throws Exception {
        UUID gestion = fixture.gestionVide(FixtureAchat.GESTION_BC, s.gestion(FixtureAchat.GESTION_BC));
        UUID autreNouveau = fixture.statutSaisie("BROUILLON-" + c.jeton(), 5, FixtureAchat.ETAT_NOUVEAU, false, false);
        fixture.transitionDe(gestion, 5, s.valideDg(), s.valideDt());
        fixture.transitionDe(gestion, 20, autreNouveau, s.valideDg());
        fixture.transitionDe(gestion, 10, s.enCours(), s.valideDg());

        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.BON_COMMANDE));

        assertThat(guidDe(enBase(piece), STATUT)).isEqualTo(id(s.enCours()));
    }

    // ------------------------------------------------------------------ E-C4.4 historique

    /** E-C4.4 : une ligne d'historique au statut de départ, sans transition, Notes ''. */
    @ParameterizedTest
    @EnumSource(value = NatureSaisie.class, names = {"BC", "FACTURE"})
    void ec44_historique_uneLigne_statutDeDepart_sansTransition(NatureSaisie n) throws Exception {
        UUID piece = creerOk(corpsValide(c, s, n.code));

        assertThat(fixture.lire(HISTORIQUE, piece)).singleElement().satisfies(h -> assertAll(
                () -> assertThat(guidDe(h, STATUT)).isEqualTo(id(s.enCours())),
                () -> assertThat(h).containsEntry("buyDocumentWorkFlowId", null).containsEntry("Notes", "")));
    }

    /** E-C4.4 / Q-4 (comme OMRAN : 176 devis, 0 ligne d'historique en prod) : devis à statut NULL → aucune ligne. */
    @Test
    void ec44_devis_statutNull_aucuneLigneHistorique() throws Exception {
        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.DEVIS));

        assertThat(fixture.lire(HISTORIQUE, piece)).isEmpty();
    }

    // ------------------------------------------------------------------ E-C4.5 échéance

    @Test
    void ec45_echeance_delaiDuLot_prioritaire() throws Exception {
        UUID lot = fixture.lot("LOT-" + c.jeton(), c.projet());
        fixture.poserSur("PrjMarket", lot, DELAI, 30);
        fixture.poserSur("CfgTier", c.fournisseur(), DELAI, 10);

        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("lotId", lot));

        assertThat(jour(enBase(piece), ECHEANCE)).isEqualTo(DATE.plusDays(30));
    }

    @Test
    void ec45_echeance_sansDelaiDeLot_delaiDuFournisseur() throws Exception {
        fixture.poserSur("CfgTier", c.fournisseur(), DELAI, 10);

        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.BON_COMMANDE));

        assertThat(jour(enBase(piece), ECHEANCE)).isEqualTo(DATE.plusDays(10));
    }

    @Test
    void ec45_echeance_sansAucunDelai_dateDuDocument() throws Exception {
        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.BON_COMMANDE));

        assertThat(jour(enBase(piece), ECHEANCE)).isEqualTo(DATE);
    }

    @Test
    void ec45_echeanceSaisie_conservee() throws Exception {
        fixture.poserSur("CfgTier", c.fournisseur(), DELAI, 10);
        LocalDate saisie = LocalDate.of(2026, 7, 14);

        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("dateEcheance", saisie));

        assertThat(jour(enBase(piece), ECHEANCE)).isEqualTo(saisie);
    }

    // ------------------------------------------------------------------ E-C4.6 constantes, champs du corps

    @Test
    void ec46_constantes_etChampsDuCorps() throws Exception {
        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.BON_COMMANDE));

        Map<String, Object> d = enBase(piece);
        assertAll(
                () -> assertThat(d).containsEntry("ReferenceType", "").containsEntry("CanBePayed", true)
                        .containsEntry("IsClosed", false).containsEntry("IsAccounted", false)
                        .containsEntry("IsSelected", false).containsEntry("IsPropotionCharged", false)
                        .containsEntry("BuyFolderId", null).containsEntry("StatusValidityDate", null)
                        .containsEntry("BuySelectionId", null).containsEntry("BuyOrderId", null)
                        .containsEntry("BuyQuoteId", null).containsEntry("SupplierDescription", c.fournisseurNom())
                        .containsEntry("Description", "Bon de commande N° 21/00601 du 04/05/2026"),
                () -> assertThat(guidDe(d, "StlCurrencyId")).isEqualTo(id(s.devise())),
                () -> assertThat(guidDe(d, "CfgCompanyId")).isEqualTo(id(c.societe())),
                () -> assertThat(guidDe(d, "CfgTierId")).isEqualTo(id(c.fournisseur())),
                () -> assertThat(guidDe(d, "PrjProjectId")).isEqualTo(id(c.projet())),
                () -> assertThat(guidDe(d, "PrjMarketTypeId")).isEqualTo(id(s.typeMarche())),
                () -> assertThat(guidDe(d, "AdmModuleId")).isEqualTo(id(s.moduleAchat())),
                () -> assertThat(jour(d, "DocDate")).isEqualTo(DATE),
                () -> assertThat(jour(d, "PrevisionDeliveryDate")).as("null → dateDocument").isEqualTo(DATE),
                () -> assertThat(RATIOS_LOT).isNotEmpty().allSatisfy(col -> assertThat(decimalDe(d, col)).as(col).isZero()),
                () -> assertThat(List.of("WarrantyAmount", "RetainedAmount", "AdvanceAmount", "StockAmountToPay"))
                        .isNotEmpty().allSatisfy(col -> assertThat(decimalDe(d, col)).as(col).isZero()));
    }

    /** E-C4.6 / Q-10 : libellé de nature mesuré en prod (« Devis », « Bon de livraison », « Facture »). */
    @ParameterizedTest
    @EnumSource(value = NatureSaisie.class, names = "BC", mode = EnumSource.Mode.EXCLUDE)
    void ec46_description_libelleNature_code_date(NatureSaisie n) throws Exception {
        String libelle = switch (n) {
            case DEVIS -> "Devis";
            case BL -> "Bon de livraison";
            default -> "Facture";
        };

        UUID piece = creerOk(corpsValide(c, s, n.code));

        assertThat(enBase(piece)).containsEntry("Description",
                libelle + " N° " + n.sequence().codeSuivant() + " du 04/05/2026");
    }

    /** Spec (DTO) : textes absents → '' ; date de livraison souhaitée saisie → écrite. */
    @Test
    void dto_textesAbsents_videsEnBase_dateLivraisonSaisie() throws Exception {
        LocalDate livraison = LocalDate.of(2026, 6, 1);
        CorpsJson corps = corpsValide(c, s, FixtureAchat.BON_COMMANDE).sans("personneAContacter").sans("referenceInterne")
                .sans("referenceBureauOrdre").sans("notes").sans("notes2").avec("dateLivraisonSouhaitee", livraison);

        UUID piece = creerOk(corps);

        Map<String, Object> d = enBase(piece);
        assertAll(
                () -> assertThat(d).containsEntry("PersonToContact", "").containsEntry("DocRef", "")
                        .containsEntry("OfficeOrderRef", "").containsEntry("Notes", "").containsEntry("Notes2", ""),
                () -> assertThat(jour(d, "PrevisionDeliveryDate")).isEqualTo(livraison));
    }

    /** Spec (DTO) : textes saisis écrits dans leurs colonnes. */
    @Test
    void dto_textesSaisis_ecrits() throws Exception {
        CorpsJson corps = corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("personneAContacter", "M. Alami")
                .avec("referenceInterne", "RI-7").avec("referenceBureauOrdre", "BO-9").avec("notes", "N1")
                .avec("notes2", "N2");

        UUID piece = creerOk(corps);

        assertThat(enBase(piece)).containsEntry("PersonToContact", "M. Alami").containsEntry("DocRef", "RI-7")
                .containsEntry("OfficeOrderRef", "BO-9").containsEntry("Notes", "N1").containsEntry("Notes2", "N2");
    }

    // ------------------------------------------------------------------ E-C4.7 lot, Q-6

    @Test
    void ec47_lotHorsDuProjet_400_rienEcrit() throws Exception {
        UUID autreProjet = fixture.projet("P2-" + c.jeton(), c.societe());
        fixture.habiliter(redacteur, autreProjet);
        UUID lotAilleurs = fixture.lot("LOT2-" + c.jeton(), autreProjet);

        MvcResult r = creerPar(redacteur, corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("lotId", lotAilleurs));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        fixture.oublier();
        assertThat(fixture.nombre("BuyDocument", "CfgTierId", c.fournisseur())).isZero();
    }

    private UUID lotAvecTaux() {
        UUID lot = fixture.lot("LOT-" + c.jeton(), c.projet());
        fixture.poserSur("PrjMarket", lot, "AdvanceRatio", new BigDecimal("0.1"));
        fixture.poserSur("PrjMarket", lot, "WarrantyRatio", new BigDecimal("0.07"));
        fixture.poserSur("PrjMarket", lot, "ProportionRatio", new BigDecimal("0.02"));
        fixture.poserSur("PrjMarket", lot, "InsuranceDECRatio", new BigDecimal("0.01"));
        fixture.poserSur("PrjMarket", lot, "InsuranceTRCRatio", new BigDecimal("0.005"));
        return lot;
    }

    /** Q-6 (réponse du manager : comme OMRAN, {@code GetMarketInfoForInvoice}) : facture libre à lot → taux du lot recopiés. */
    @Test
    void q6_factureALot_copieLesTauxDuLot() throws Exception {
        UUID lot = lotAvecTaux();

        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.FACTURE).avec("lotId", lot));

        Map<String, Object> d = enBase(piece);
        assertAll(
                () -> assertThat(guidDe(d, "PrjMarketId")).isEqualTo(id(lot)),
                () -> assertThat(decimalDe(d, "AdvanceRatio")).isEqualByComparingTo("0.1"),
                () -> assertThat(decimalDe(d, "WarrantyRatio")).isEqualByComparingTo("0.07"),
                () -> assertThat(decimalDe(d, "ProportionRatio")).isEqualByComparingTo("0.02"),
                () -> assertThat(decimalDe(d, "InsuranceDECRatio")).isEqualByComparingTo("0.01"),
                () -> assertThat(decimalDe(d, "InsuranceTRCRatio")).isEqualByComparingTo("0.005"));
    }

    /**
     * Q-6 (réponse 6 du manager), chiffré à la main d'après OMRAN :
     * {@code AdvanceRest} = Σ AmountTTC des avances MARKET_ADVANCE du lot (5 000) − Σ PrjWorkingOut.AdvanceAmount du lot (1 200)
     * = 3 800 ({@code Yes.Buy/Utils.cs:GetAdvanceRestByMarket(market, docId)}) ; les taux ne calculent aucun montant de retenue
     * (champs en lecture seule de {@code Yes.Stl.Front.Debit/Invoice.cs}) → retenue, amortissement, prorata = 0 ;
     * net = TTC + régul + timbre − retenues ({@code Yes.Stl.Debit/Invoice.cs:TotalRetainedAmountWithoutRegul}) = 1 200 + 30 + 20.
     */
    @Test
    void q6_factureALot_resteAvance_retenuesNulles_netAPayer() throws Exception {
        UUID lot = lotAvecTaux();
        UUID avance = fixture.piece(FixtureAchat.Piece.facture(c.jeton() + "-AVL", DATE, c.fournisseur(), c.fournisseurNom(),
                c.societe(), c.projet(), new BigDecimal("5000")).nature(FixtureAchat.DOCUMENT_A_REGLER).lot(lot));
        fixture.poser(avance, "ReferenceType", "MARKET_ADVANCE");
        UUID factureDecompte = fixture.piece(c, "-FDL", FixtureAchat.FACTURE);
        UUID decompte = fixture.decompte(factureDecompte);
        fixture.poserSur("PrjWorkingOut", decompte, "PrjMarketId", lot);
        fixture.poserSur("PrjWorkingOut", decompte, "AdvanceAmount", new BigDecimal("1200"));

        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.FACTURE).avec("lotId", lot)
                .avec("montantRegul", new BigDecimal("30")).avec("timbreFiscal", new BigDecimal("20")));

        Map<String, Object> d = enBase(piece);
        assertAll(
                () -> assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("1200"),
                () -> assertThat(decimalDe(d, "AdvanceRest")).isEqualByComparingTo("3800"),
                () -> assertThat(decimalDe(d, "AdvanceAmount")).isEqualByComparingTo("0"),
                () -> assertThat(decimalDe(d, "WarrantyAmount")).isEqualByComparingTo("0"),
                () -> assertThat(decimalDe(d, "ProportionAmount")).isEqualByComparingTo("0"),
                () -> assertThat(decimalDe(d, "AmountToPay")).isEqualByComparingTo("1250"));
    }

    /** Q-6 / E-C4.6 : la recopie est propre à la facture ; un BC à lot garde des taux nuls. */
    @Test
    void q6_bonDeCommandeALot_tauxNuls() throws Exception {
        UUID lot = lotAvecTaux();

        UUID piece = creerOk(corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("lotId", lot));

        Map<String, Object> d = enBase(piece);
        assertThat(RATIOS_LOT).isNotEmpty().allSatisfy(col -> assertThat(decimalDe(d, col)).as(col).isZero());
    }

    // ------------------------------------------------------------------ GET …/modele

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void modele_preRempli_gestion_module_devise_typeMarche_tvaParDefaut(NatureSaisie n) throws Exception {
        MvcResult r = appeler(mockMvc, modele(n.code), connecte(redacteur, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        String json = corps(r);
        assertAll(
                () -> assertThat((String) JsonPath.read(json, "$.nature")).isEqualTo(n.code),
                () -> assertThat((String) JsonPath.read(json, "$.gestion")).isEqualTo(n.gestion),
                () -> assertThat(((String) JsonPath.read(json, "$.moduleId")).toUpperCase()).isEqualTo(id(s.moduleAchat())),
                () -> assertThat(((String) JsonPath.read(json, "$.deviseId")).toUpperCase()).isEqualTo(id(s.devise())),
                () -> assertThat(((String) JsonPath.read(json, "$.typeMarcheId")).toUpperCase())
                        .isEqualTo(id(s.typeMarche())),
                () -> assertThat(new BigDecimal(JsonPath.read(json, "$.tvaParDefaut").toString()))
                        .isEqualByComparingTo(FixtureAchat.TVA_PAR_DEFAUT),
                () -> assertThat((List<?>) JsonPath.read(json, "$.sources")).isNotNull().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = NatureSaisie.class, names = "DEVIS", mode = EnumSource.Mode.EXCLUDE)
    void modele_statutDeDepart_enCours(NatureSaisie n) throws Exception {
        String json = corps(appeler(mockMvc, modele(n.code), connecte(redacteur, ROLE_JWT)));

        assertAll(
                () -> assertThat(((String) JsonPath.read(json, "$.statutDepartId")).toUpperCase()).isEqualTo(id(s.enCours())),
                () -> assertThat((String) JsonPath.read(json, "$.statutDepartLibelle")).isNotBlank());
    }

    @Test
    void modele_devis_statutDeDepartNull() throws Exception {
        String json = corps(appeler(mockMvc, modele(FixtureAchat.DEVIS), connecte(redacteur, ROLE_JWT)));

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.statutDepartId")).isNull(),
                () -> assertThat((Object) JsonPath.read(json, "$.statutDepartLibelle")).isNull());
    }

    /** Spec (DTO) : {@code declencheurId} par défaut = personnel du compte. */
    @Test
    void modele_declencheurParDefaut_personnelDuCompte() throws Exception {
        AdmUser avecPersonnel = fixture.compteHabilite(c, "ACR-P", ROLE_JWT, NOUVEAU_BC);

        String json = corps(appeler(mockMvc, modele(FixtureAchat.BON_COMMANDE), connecte(avecPersonnel, ROLE_JWT)));

        assertThat(((String) JsonPath.read(json, "$.declencheurId")).toUpperCase())
                .isEqualTo(id(avecPersonnel.getCfgEmployee().getId()));
    }

    /**
     * Réponse 8 du manager : {@code declencheurId} absent du corps au POST → défaut OMRAN = personnel du compte connecté
     * ({@code Yes.Buy/DocumentBase.cs} constructeur l.2159 : {@code cfgTierTriggerId = ConnectedUser.CfgEmployeeId}).
     */
    @Test
    void ec46_declencheurAbsent_personnelDuCompte() throws Exception {
        AdmUser avecPersonnel = fixture.compteHabilite(c, "ACR-P", ROLE_JWT, NOUVEAU_FACTURE);

        MvcResult r = creerPar(avecPersonnel, corpsValide(c, s, FixtureAchat.FACTURE).sans("declencheurId"));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        assertThat(guidDe(enBase(idCree(r)), "CfgTierTriggerId")).isEqualTo(id(avecPersonnel.getCfgEmployee().getId()));
    }
}
