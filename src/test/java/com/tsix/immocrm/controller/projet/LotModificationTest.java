package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.projet.LotFicheSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.LotFicheSupport.NOTES;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.nombre;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E6 (PATCH /api/projet/lots/{id}), R8, R11, R12 et ordre 404 → 403 → 409 → 400 — FEATURE-lot-fiche-parite.md. Appelant : un
 * compte qui voit le lot sans aucun droit OMRAN (réponse du manager 6 : qui voit le lot enregistre, sauf l'estimation manuelle).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotModificationTest {

    /** R8 : la chaîne des dates, dans l'ordre OMRAN ({@code MarketMonitoring.cs:Verif}), noms du fil (E1). */
    static final List<String> CHAINE = List.of("dateLancementAppelOffres", "dateRetraitDossiers", "dateEcheanceRemiseOffres",
            "dateReportAppelOffres", "dateOuverturePlis", "dateRapportDepouillement", "dateAdjudication", "dateSignatureMarche",
            "dateOrdreService", "dateLancementTravaux", "dateAchevementTravaux", "dateRecollement");
    static final List<String> COLONNES = List.of("LaunchOfTenderDate", "WithdrawalTenderDocumentsDate", "TenderMaturityDate",
            "TendersResortDate", "BidOpeningDate", "TendersReportDate", "TenderDate", "ContractSignatureDate", "ServiceOrderDate",
            "WorkLaunchDate", "WorkCompletionDate", "ReattachmentDate");

    private static final String MONTANT_FORFAITAIRE_HT = "montantForfaitaireHT";
    private static final String TOT = "2026-03-11";
    private static final String TARD = "2026-03-10";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureProjetConfig config;

    private LotFicheSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statut;
    private UUID lot;
    private AdmUser compte;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        projet = parite.projet("MOP-" + jeton, parite.entiteJuridique("MOE-" + jeton));
        statut = parite.statut(ENTREPRENEURS, "MOS-" + jeton);
        lot = parite.lot(projet, "MOL-" + jeton, ENTREPRENEURS, statut);
        compte = parite.avecDroits("lots.mod." + jeton, List.of(), projet);
        qui = connecte(compte, "Commercial");
    }

    private Reponse modifier(CorpsJson corps) throws Exception {
        return appels.modifier(qui, lot, corps);
    }

    private Reponse modifierOk(CorpsJson corps) throws Exception {
        Reponse r = modifier(corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static LocalDate jourDe(Object valeur) {
        return valeur == null ? null : ((Timestamp) valeur).toLocalDateTime().toLocalDate();
    }

    private static BigDecimal decimal(Object valeur) {
        return nombre(valeur);
    }

    // ------------------------------------------------------------------ 200, partiel

    @Test
    void e6_description_200_renvoieLaFiche_ecritAuteurEtDate() throws Exception {
        Reponse r = modifierOk(corps().avec(DESCRIPTION, "Libellé modifié " + jeton));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.id")).isEqualTo(lot.toString());
            s.assertThat(r.valeur("$.description")).isEqualTo("Libellé modifié " + jeton);
            s.assertThat(enBase).containsEntry("Description", "Libellé modifié " + jeton);
            s.assertThat(enBase.get("UserUpdate")).as("UserUpdate").isNotNull();
            s.assertThat(enBase.get("UserUpdateDateTime")).as("UserUpdateDateTime").isNotNull();
        });
    }

    @Test
    void e6_champAbsent_inchange() throws Exception {
        fiche.avanceEtSignature(lot, new BigDecimal("0.15"), LocalDate.of(2026, 4, 1));
        parite.notes(lot, "Notes " + jeton);

        modifierOk(corps().avec(DESCRIPTION, "Autre " + jeton));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(decimal(enBase.get("AdvanceRatio"))).isEqualByComparingTo("0.15");
            s.assertThat(jourDe(enBase.get("ContractSignatureDate"))).isEqualTo(LocalDate.of(2026, 4, 1));
            s.assertThat(enBase).containsEntry("Notes", "Notes " + jeton);
            s.assertThat(decimal(enBase.get("WarrantyRatio"))).isEqualByComparingTo("0.1");
        });
    }

    @Test
    void e6_nullExplicite_videLaColonne() throws Exception {
        fiche.avanceEtSignature(lot, new BigDecimal("0.15"), LocalDate.of(2026, 4, 1));
        parite.notes(lot, "Notes " + jeton);

        modifierOk(corps().avec(NOTES, null).avec("dateSignatureMarche", null));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(enBase.get("Notes")).as("Notes").isNull();
            s.assertThat(enBase.get("ContractSignatureDate")).as("ContractSignatureDate").isNull();
        });
    }

    // ------------------------------------------------------------------ R12 : aller-retour des taux

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "tauxAvancePct, tauxAvance, AdvanceRatio",
            "tauxRetenueGarantiePct, tauxRetenueGarantie, WarrantyRatio",
            "tauxCompteProrataPct, tauxCompteProrata, ProportionRatio",
            "tauxAssuranceDECPct, tauxAssuranceDEC, InsuranceDECRatio",
            "tauxAssuranceTRCPct, tauxAssuranceTRC, InsuranceTRCRatio",
            "tvaDefautPct, tvaDefaut, DefaultVat",
            "variationMassePct, variationMassePct, MassChangeRatio",
            "tauxRemboursementApproPct, tauxRemboursementApproPct, StockPileRatio"})
    void r12_taux10_baseRecoit01_ficheRedonne10(String champ, String champFiche, String colonne) throws Exception {
        modifierOk(corps().avec(champ, new BigDecimal("10")));

        Map<String, Object> enBase = fiche.lot(lot);
        Reponse relue = appels.fiche(qui, lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(decimal(enBase.get(colonne))).as(colonne).isEqualByComparingTo("0.1");
            s.assertThat(relue.statut()).isEqualTo(200);
            s.assertThat(nombre(relue.valeur("$." + champFiche))).as(champFiche).isEqualByComparingTo("10");
        });
    }

    // ------------------------------------------------------------------ en-tête, montants, options, résiliation, autres

    @Test
    void e6_entete_fournisseurDeviseCategorieType_ecrits() throws Exception {
        UUID fournisseur = lots.fournisseur("MOF-" + jeton);
        UUID devise = fiche.devise("MOD-" + jeton);
        UUID categorie = config.categorie("MOC-" + jeton, false);
        UUID type = config.typeLot("MOT-" + jeton);

        modifierOk(corps().avec("fournisseurId", fournisseur).avec("deviseId", devise).avec("categorieId", categorie)
                .avec("typeId", type));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(guid(enBase.get("CfgSupplierId"))).isEqualTo(guid(fournisseur));
            s.assertThat(guid(enBase.get("StlCurrencyId"))).isEqualTo(guid(devise));
            s.assertThat(guid(enBase.get("StlCategoryId"))).isEqualTo(guid(categorie));
            s.assertThat(guid(enBase.get("PrjMarketTypeId"))).isEqualTo(guid(type));
        });
    }

    @Test
    void e6_montantsAssurance_delaiPaiement_garantie_ecrits() throws Exception {
        modifierOk(corps().avec("montantAssuranceDEC", new BigDecimal("1500")).avec("montantAssuranceTRC", new BigDecimal("700"))
                .avec("delaiPaiementJours", 45).avec("dureeGarantieMois", 12));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(decimal(enBase.get("InsuranceDECAmount"))).isEqualByComparingTo("1500");
            s.assertThat(decimal(enBase.get("InsuranceTRCAmount"))).isEqualByComparingTo("700");
            s.assertThat(enBase).containsEntry("PaymentDelay", 45);
            s.assertThat(decimal(enBase.get("WarrantyInMonths"))).isEqualByComparingTo("12");
        });
    }

    @Test
    void e6_options_resiliation_autres_ecrits() throws Exception {
        UUID type = fiche.typePaiement("MOY-" + jeton);

        modifierOk(corps().avec("totalPoints", 7).avec("typePaiementId", type).avec("remiseSurTravauxSupp", true)
                .avec("retenueSurTravauxSupp", true).avec("saisieSurQuantites", true).avec("avancementParLigne", true)
                .avec("dateResiliation", "2026-09-30").avec("causeResiliation", "Cause " + jeton)
                .avec("descriptionDetaillee", "Détail " + jeton));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(enBase).containsEntry("TotalPoints", 7);
            s.assertThat(guid(enBase.get("StlSettlementTypeId"))).isEqualTo(guid(type));
            s.assertThat(enBase).containsEntry("ApplyDiscountOnAdditionalWork", Boolean.TRUE);
            s.assertThat(enBase).containsEntry("ApplyWithholdingTaxOnAdditionalWork", Boolean.TRUE);
            s.assertThat(enBase).containsEntry("ExecutedQuantityBasedInsert", Boolean.TRUE);
            s.assertThat(enBase).containsEntry("CanEditWorkingOutLineByAchivement", Boolean.TRUE);
            s.assertThat(jourDe(enBase.get("CancellationDate"))).isEqualTo(LocalDate.of(2026, 9, 30));
            s.assertThat(enBase).containsEntry("CancellationCause", "Cause " + jeton);
            s.assertThat(enBase).containsEntry("DescriptionLong", "Détail " + jeton);
        });
    }

    // ------------------------------------------------------------------ description (400)

    @Test
    void e6_descriptionVide_400_inchange() throws Exception {
        Reponse r = modifier(corps().avec(DESCRIPTION, "   "));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(fiche.lotApresRefus(lot)).containsEntry("Description", "Lot MOL-" + jeton);
    }

    @Test
    void e6_description256_400_inchange() throws Exception {
        Reponse r = modifier(corps().avec(DESCRIPTION, "x".repeat(256)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(fiche.lotApresRefus(lot)).containsEntry("Description", "Lot MOL-" + jeton);
    }

    // ------------------------------------------------------------------ non modifiables

    static Stream<Arguments> nonModifiables() {
        return Stream.of(
                Arguments.of("code", "99/99999", "Code"),
                Arguments.of("multiFournisseur", true, "IsMultiSupplier"),
                Arguments.of("dateDebutPrevisionnelle", "2027-01-01", "StartDate"),
                Arguments.of("delaiPrevisionnelJours", 999, "Delay"),
                Arguments.of("dateFinPrevisionnelle", "2027-12-31", "EndDate"),
                Arguments.of("genre", FixtureLotsParite.FOURNISSEURS, "StrEntityId"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonModifiables")
    void e6_champNonModifiable_ignoreOuRefuse_baseInchangee(String champ, Object valeur, String colonne) throws Exception {
        fiche.previsionnel(lot, LocalDate.of(2026, 1, 1), 90, LocalDate.of(2026, 4, 1));
        Object avant = fiche.lot(lot).get(colonne);

        Reponse r = modifier(corps().avec(champ, valeur));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isIn(200, 400);
            s.assertThat(fiche.lotApresRefus(lot).get(colonne)).as(colonne).isEqualTo(avant);
        });
    }

    @Test
    void e6_q4_projetNonModifiable_baseInchangee() throws Exception {
        UUID autre = parite.projet("MOQ-" + jeton, null);
        lots.habiliter(compte, autre);

        Reponse r = modifier(corps().avec("projetId", autre));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isIn(200, 400);
            s.assertThat(guid(fiche.lotApresRefus(lot).get("PrjProjectId"))).isEqualTo(guid(projet));
        });
    }

    @Test
    void e6_statutNonModifiable_passeParE7_baseInchangee() throws Exception {
        UUID autre = parite.statut(ENTREPRENEURS, "MOS2-" + jeton);

        Reponse r = modifier(corps().avec("statutId", autre));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isIn(200, 400);
            s.assertThat(guid(fiche.lotApresRefus(lot).get("StrStatusId"))).isEqualTo(guid(statut));
        });
    }

    // ------------------------------------------------------------------ R8 : chaîne des dates

    @Test
    void r8_chaqueCouple_dateApresAnterieure_400_baseInchangee() throws Exception {
        SoftAssertions s = new SoftAssertions();
        int couples = 0;
        for (int i = 0; i < CHAINE.size(); i++) {
            for (int j = i + 1; j < CHAINE.size(); j++) {
                couples++;
                Reponse r = modifier(corps().avec(CHAINE.get(i), TOT).avec(CHAINE.get(j), TARD));
                Map<String, Object> enBase = fiche.lotApresRefus(lot);
                String couple = CHAINE.get(i) + " > " + CHAINE.get(j);
                s.assertThat(r.statut()).as(couple + " : " + r.corps()).isEqualTo(400);
                s.assertThat(enBase.get(COLONNES.get(i))).as(couple + " : " + COLONNES.get(i)).isNull();
                s.assertThat(enBase.get(COLONNES.get(j))).as(couple + " : " + COLONNES.get(j)).isNull();
            }
        }
        s.assertThat(couples).isEqualTo(66);
        s.assertAll();
    }

    @Test
    void r8_datesEgales_acceptees() throws Exception {
        modifierOk(corps().avec("dateLancementAppelOffres", TARD).avec("dateRecollement", TARD));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jourDe(enBase.get("LaunchOfTenderDate"))).isEqualTo(LocalDate.parse(TARD));
            s.assertThat(jourDe(enBase.get("ReattachmentDate"))).isEqualTo(LocalDate.parse(TARD));
        });
    }

    @Test
    void r8_chaineCroissante_douzeDates_200_toutesEcrites() throws Exception {
        CorpsJson corps = corps();
        for (int i = 0; i < CHAINE.size(); i++) {
            corps.avec(CHAINE.get(i), LocalDate.of(2026, 1, 1).plusDays(i).toString());
        }

        modifierOk(corps);

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions s = new SoftAssertions();
        for (int i = 0; i < COLONNES.size(); i++) {
            s.assertThat(jourDe(enBase.get(COLONNES.get(i)))).as(COLONNES.get(i)).isEqualTo(LocalDate.of(2026, 1, 1).plusDays(i));
        }
        s.assertAll();
    }

    @Test
    void r8_dateDejaEnBase_entreDansLaChaine_400() throws Exception {
        fiche.signature(lot, LocalDate.parse(TARD));

        Reponse r = modifier(corps().avec("dateLancementAppelOffres", TOT));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(fiche.lotApresRefus(lot).get("LaunchOfTenderDate")).isNull();
    }

    @Test
    void r8_dateEnBaseVideeParNull_nEntrePlusDansLaChaine_200() throws Exception {
        fiche.signature(lot, LocalDate.parse(TARD));

        modifierOk(corps().avec("dateLancementAppelOffres", TOT).avec("dateSignatureMarche", null));

        assertThat(jourDe(fiche.lot(lot).get("LaunchOfTenderDate"))).isEqualTo(LocalDate.parse(TOT));
    }

    // ------------------------------------------------------------------ R11 : statut en lecture seule

    @Test
    void r11_statutIsReadOnly_409_inchange() throws Exception {
        fiche.lectureSeule(statut);

        Reponse r = modifier(corps().avec(DESCRIPTION, "Interdit " + jeton));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(fiche.lotApresRefus(lot)).containsEntry("Description", "Lot MOL-" + jeton);
    }

    // ------------------------------------------------------------------ ordre 404 → 403 → 409 → 400

    @Test
    void ordre_lotInvisible_404_avantDroitStatutEtControles() throws Exception {
        UUID autre = parite.projet("MOX-" + jeton, null);
        UUID statutX = parite.statut(ENTREPRENEURS, "MOXS-" + jeton);
        fiche.lectureSeule(statutX);
        UUID invisible = parite.lot(autre, "MOX-" + jeton, ENTREPRENEURS, statutX);

        Reponse r = appels.modifier(qui, invisible, corps().avec(MONTANT_FORFAITAIRE_HT, new BigDecimal("1000"))
                .avec("dateLancementAppelOffres", TOT).avec("dateRecollement", TARD));

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void ordre_lotInconnu_404() throws Exception {
        assertThat(appels.modifier(qui, UUID.randomUUID(), corps().avec(DESCRIPTION, "x")).statut()).isEqualTo(404);
    }

    @Test
    void ordre_droitEstimation_403_avantStatutLectureSeule() throws Exception {
        fiche.lectureSeule(statut);

        Reponse r = modifier(corps().avec(MONTANT_FORFAITAIRE_HT, new BigDecimal("1000")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void ordre_droitEstimation_403_avantControlesDeDates() throws Exception {
        Reponse r = modifier(corps().avec(MONTANT_FORFAITAIRE_HT, new BigDecimal("1000"))
                .avec("dateLancementAppelOffres", TOT).avec("dateRecollement", TARD));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void ordre_statutLectureSeule_409_avantControlesDeDates() throws Exception {
        fiche.lectureSeule(statut);

        Reponse r = modifier(corps().avec("dateLancementAppelOffres", TOT).avec("dateRecollement", TARD));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
    }

    @Test
    void ordre_statutLectureSeule_409_avantDescriptionInvalide() throws Exception {
        fiche.lectureSeule(statut);

        Reponse r = modifier(corps().avec(DESCRIPTION, ""));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
    }
}
