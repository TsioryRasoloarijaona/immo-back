package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Compte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.FixtureTerrains.TypeFrais;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.document;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.STATUT_EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTerrains.VALIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R9 — instantané des deux écrivains de {@code BuyDocument} (FEATURE-achat-regle-commune.md §2 colonne « Arpent », §3 R1,
 * R5, R8, R9, §7 n° 2, décision du manager n° 1) : pièce, lignes, ventilations et historique relus colonne par colonne.
 * Seul changement assumé (D1) : {@code CfgTierTriggerId} = personnel du compte pour la commission aussi ; compte sans
 * personnel → NULL. Horloge au 10/02/2027 11:30 : code « 27/03014 » depuis la séquence à 3013.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcriturePieceAchatInstantaneTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2027, 2, 10, 11, 30);
    private static final LocalDateTime JOUR = MAINTENANT.toLocalDate().atStartOfDay();
    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final BigDecimal MONTANT_FRAIS = new BigDecimal("8000.123456");
    private static final String PIECE_DU_TIERS = "SELECT * FROM BuyDocument WHERE CfgTierId = :id";
    private static final String PIECE_DU_FRAIS = "SELECT * FROM BuyDocument WHERE StkExpenseId = :id";
    private static final String LIGNES = "SELECT * FROM BuyDocumentLine WHERE BuyDocumentId = :id";
    private static final String CATEGORIES = "SELECT * FROM BuyVentilationCategory WHERE BuyDocumentId = :id";
    private static final String PROJETS = "SELECT * FROM BuyVentilationExpense WHERE BuyDocumentId = :id";
    private static final String HISTORIQUE = "SELECT * FROM BuyDocumentStatusHistory WHERE BuyDocumentId = :id";
    private static final String MARCHE_PAR_DEFAUT =
            "SELECT TOP 1 CodeValue AS v FROM AdmParamsGuid WHERE Code = N'DefaultMarketType' AND :id IS NOT NULL";
    private static final String USER_CREATE = "UserCreate";
    private static final String USER_UPDATE = "UserUpdate";
    private static final String CREE_LE = "UserCreateDateTime";
    private static final String MODIFIE_LE = "UserUpdateDateTime";
    private static final String DESCRIPTION = "Description";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return Clock.fixed(MAINTENANT.atZone(FuseauMetier.ZONE).toInstant(), FuseauMetier.ZONE);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions commissions;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixturePatrimoine patrimoine;

    private Contexte c;
    private Compte compte;
    private UUID commercial;
    private UUID projet;
    private Dossier d1;
    private Dossier d2;
    private UUID employe;
    private UUID societe;

    @BeforeEach
    void setUp() {
        c = commissions.contexte("A0I");
        commissions.configurationDocument();
        compte = commissions.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU);
        commercial = commissions.personne("COM-" + c.jeton(), false);
        projet = commissions.chantier(c.tranche(), "CH-" + c.jeton());
        d1 = commissions.dossier(c, "D1", commercial, new BigDecimal("775000"), AVANCE);
        d2 = commissions.dossier(c, "D2", commercial, new BigDecimal("775000"), AVANCE);
        // lus avant toute relecture SQL : lignes() vide le contexte de persistance (associations LAZY détachées)
        employe = compte.compte().getCfgEmployee().getId();
        societe = c.projet().getCfgCompany().getId();
    }

    // ------------------------------------------------------------------ commission

    private Tuple documenterCommission() throws Exception {
        List<UUID> lignes = List.of(
                commissions.ligne(d1, CONCRETISATION, commercial, new BigDecimal("300.5"), AVANCE, null),
                commissions.ligne(d2, CONCRETISATION, commercial, new BigDecimal("100"), AVANCE, null));
        mockMvc.perform(document(lignes).with(compte.qui())).andExpect(status().isCreated());
        List<Tuple> pieces = commissions.lignes(PIECE_DU_TIERS, commercial);
        assertThat(pieces).hasSize(1);
        return pieces.get(0);
    }

    @Test
    void r9_commission_piece_colonneParColonne() throws Exception {
        Tuple p = documenterCommission();

        String auteur = compte.compte().getDescription();
        new ColonnesEcrites()
                .avec("Code", "27/03014")
                .avec(DESCRIPTION, "Commission N° 27/03014")
                .avec("DocDate", JOUR).avec("PrevisionDeliveryDate", JOUR).avec("MaturityDate", JOUR)
                .avec("DocType", "INVOICE").avec("ReferenceType", "SALE_COMMISSION").avec("StkExpenseId", null)
                .avec("CfgTierId", commercial).avec("SupplierDescription", "Employe COM-" + c.jeton())
                .avec("CfgCompanyId", societe)
                .avec("PrjMarketTypeId", marcheParDefaut())
                .avec("AdmModuleId", commissions.moduleCom())
                .avec("StrEntityId", commissions.entiteDocument())
                .avec("StlCurrencyId", terrains.deviseParDefaut())
                .avec("BuyDocumentStatusId", STATUT_EN_COURS)
                .avec("Notes", "")
                .avec("AmountHT", new BigDecimal("400.5")).avec("AmountTTC", new BigDecimal("400.5"))
                .avec("AmountToPay", new BigDecimal("400.5"))
                .montantsAZero()
                .avec("IsSelected", false).avec("IsClosed", false).avec("CanBePayed", true)
                .avec("IsPropotionCharged", false).avec("IsAccounted", false)
                .avec("LinesCount", 2)
                .avec("DocRef", "").avec("PersonToContact", "").avec("Notes2", "").avec("OfficeOrderRef", "")
                .avec(USER_CREATE, auteur).avec(USER_UPDATE, auteur).avec(CREE_LE, MAINTENANT).avec(MODIFIE_LE, MAINTENANT)
                .nulles(ColonnesEcrites.NULLES)
                .verifier(p, "BuyDocument");
    }

    /** Décision n° 1 (D1) : le déclencheur de la commission = personnel du compte connecté, comme les frais terrain. */
    @Test
    void d1_commission_declencheur_personnelDuCompte() throws Exception {
        Tuple p = documenterCommission();

        assertThat(guid(p.get("CfgTierTriggerId"))).isEqualTo(guid(employe));
    }

    /** Décision n° 1 : compte sans personnel lié → NULL. */
    @Test
    void d1_commission_compteSansPersonnel_declencheurNull() throws Exception {
        // en base et dans le jeton : la source (AdmUser ou principal) n'est pas imposée par le spec
        compte.compte().setCfgEmployee(null);
        terrains.employeDuCompte(compte.compte().getId(), null);

        Tuple p = documenterCommission();

        assertThat(p.get("CfgTierTriggerId")).isNull();
    }

    @Test
    void r9_commission_lignes_colonneParColonne() throws Exception {
        UUID piece = UUID.fromString(guid(documenterCommission().get("PKey")));

        List<Tuple> lignes = commissions.lignes(LIGNES, piece);
        String auteur = compte.compte().getDescription();
        assertThat(lignes).hasSize(2);
        assertThat(lignes.stream().map(l -> ((Number) l.get("Sorting")).intValue()).toList())
                .containsExactlyInAnyOrder(1, 2);
        for (Tuple l : lignes) {
            boolean premier = ("Concrétisation du dossier N° " + d1.code()).equals(l.get(DESCRIPTION));
            BigDecimal montant = new BigDecimal(premier ? "300.5" : "100");
            new ColonnesEcrites()
                    .avec("Code", null)
                    .avec(DESCRIPTION, "Concrétisation du dossier N° " + (premier ? d1.code() : d2.code()))
                    .avec("Unity", "U").avec("Quantity", BigDecimal.ONE)
                    .avec("UnitPriceHT", montant).avec("UnitPriceTTC", montant)
                    .avec("VatRatio", BigDecimal.ZERO).avec("DiscountRatio", BigDecimal.ZERO)
                    .avec("FODECRatio", BigDecimal.ZERO).avec("Coefficient", BigDecimal.ONE)
                    .avec("QuantityOld", BigDecimal.ZERO).avec("QuantityOrigin", BigDecimal.ZERO)
                    .avec("Notes", "Montant").avec("StlCategoryId", null)
                    .avec("PrjMarketLineId", null).avec("BuyItemId", null).avec("BuyConsultationLineId", null)
                    .avec(USER_CREATE, auteur).avec(USER_UPDATE, auteur).avec(CREE_LE, MAINTENANT).avec(MODIFIE_LE, MAINTENANT)
                    .verifier(l, "BuyDocumentLine");
        }
    }

    /** D3 : aucune ventilation par catégorie ; une ventilation par projet à 100 %. Historique R8. */
    @Test
    void r9_commission_ventilationsEtHistorique() throws Exception {
        Tuple p = documenterCommission();
        UUID piece = UUID.fromString(guid(p.get("PKey")));

        List<Tuple> categories = commissions.lignes(CATEGORIES, piece);
        List<Tuple> projets = commissions.lignes(PROJETS, piece);
        List<Tuple> historique = commissions.lignes(HISTORIQUE, piece);
        assertAll(
                () -> assertThat(categories).isEmpty(),
                () -> {
                    assertThat(projets).hasSize(1);
                    new ColonnesEcrites()
                            .avec("PrjProjectId", projet).avec("VentilationRate", BigDecimal.ONE)
                            .verifier(projets.get(0), "BuyVentilationExpense");
                },
                () -> assertThat(historique).singleElement().satisfies(h -> new ColonnesEcrites()
                        .avec("BuyDocumentStatusId", STATUT_EN_COURS)
                        .avec("AdmUserId", compte.compte().getId())
                        .avec("Notes", "")
                        .avec("DocDate", MAINTENANT)
                        .avec("buyDocumentWorkFlowId", null)
                        .verifier(h, "BuyDocumentStatusHistory")));
    }

    // ------------------------------------------------------------------ frais terrain

    private record Frais(AdmUser admin, FixtureTerrains.Contexte contexte, UUID terrain, TypeFrais type,
                         UUID fournisseur, UUID frais, UUID employe) {
    }

    private Frais frais() {
        AdmUser admin = patrimoine.administrateur();
        FixtureTerrains.Contexte t = terrains.contexte("A0F");
        UUID terrain = terrains.terrain(t, "TER-" + t.jeton());
        TypeFrais type = terrains.typeFrais(t.jeton(), true, true);
        UUID fournisseur = terrains.fournisseur("FRN-" + t.jeton(), null);
        UUID frais = terrains.frais(terrain, type.id(), fournisseur, MONTANT_FRAIS, false);
        UUID personnel = terrains.fournisseur("EMP-" + t.jeton(), null);
        terrains.employeDuCompte(admin.getId(), personnel);
        return new Frais(admin, t, terrain, type, fournisseur, frais, personnel);
    }

    private Tuple validerFrais(Frais f) throws Exception {
        mockMvc.perform(post(VALIDER, f.terrain(), f.frais()).with(connecte(f.admin()))).andExpect(status().isOk());
        return terrains.ligne(PIECE_DU_FRAIS, f.frais());
    }

    @Test
    void r9_fraisTerrain_piece_colonneParColonne() throws Exception {
        Frais f = frais();

        Tuple p = validerFrais(f);

        new ColonnesEcrites()
                .avec("Code", "27/03014")
                .avec(DESCRIPTION, "Facture N° 27/03014 du 10/02/2027")
                .avec("DocDate", JOUR).avec("PrevisionDeliveryDate", JOUR).avec("MaturityDate", JOUR)
                .avec("DocType", "INVOICE").avec("ReferenceType", "LAND_EXPENSE").avec("StkExpenseId", f.frais())
                .avec("CfgTierId", f.fournisseur()).avec("SupplierDescription", "Fournisseur FRN-" + f.contexte().jeton())
                .avec("CfgCompanyId", f.contexte().entite().getId())
                .avec("PrjMarketTypeId", f.type().typeMarche())
                .avec("AdmModuleId", terrains.moduleStl())
                .avec("StrEntityId", terrains.entiteFactureTerrain())
                .avec("StlCurrencyId", terrains.deviseParDefaut())
                .avec("BuyDocumentStatusId", STATUT_EN_COURS)
                .avec("CfgTierTriggerId", f.employe())
                .avec("Notes", "Frais : " + f.type().description() + " sur terrain : Terrain TER-" + f.contexte().jeton())
                .avec("AmountHT", MONTANT_FRAIS).avec("AmountTTC", MONTANT_FRAIS).avec("AmountToPay", MONTANT_FRAIS)
                .montantsAZero()
                .avec("IsSelected", false).avec("IsClosed", false).avec("CanBePayed", true)
                .avec("IsPropotionCharged", false).avec("IsAccounted", false)
                .avec("LinesCount", 1)
                .avec("DocRef", "").avec("PersonToContact", "").avec("Notes2", "").avec("OfficeOrderRef", "")
                .avec(USER_CREATE, AUTEUR).avec(USER_UPDATE, AUTEUR).avec(CREE_LE, MAINTENANT).avec(MODIFIE_LE, MAINTENANT)
                .nulles(ColonnesEcrites.NULLES)
                .verifier(p, "BuyDocument");
    }

    @Test
    void r9_fraisTerrain_ligneVentilationsHistorique_colonneParColonne() throws Exception {
        Frais f = frais();
        UUID piece = UUID.fromString(guid(validerFrais(f).get("PKey")));

        List<Tuple> lignes = terrains.lignes(LIGNES, piece);
        List<Tuple> categories = terrains.lignes(CATEGORIES, piece);
        List<Tuple> projets = terrains.lignes(PROJETS, piece);
        List<Tuple> historique = terrains.lignes(HISTORIQUE, piece);
        assertAll(
                () -> assertThat(lignes).singleElement().satisfies(l -> new ColonnesEcrites()
                        .avec("Code", f.type().code())
                        .avec(DESCRIPTION, "Frais : " + f.type().description())
                        .avec("Unity", "U").avec("Quantity", BigDecimal.ONE).avec("Sorting", 1)
                        .avec("UnitPriceHT", MONTANT_FRAIS).avec("UnitPriceTTC", MONTANT_FRAIS)
                        .avec("VatRatio", BigDecimal.ZERO).avec("DiscountRatio", BigDecimal.ZERO)
                        .avec("FODECRatio", BigDecimal.ZERO).avec("Coefficient", BigDecimal.ONE)
                        .avec("QuantityOld", BigDecimal.ZERO).avec("QuantityOrigin", BigDecimal.ZERO)
                        .avec("Notes", "").avec("StlCategoryId", f.type().categorieComptable())
                        .avec("PrjMarketLineId", null).avec("BuyItemId", null).avec("BuyConsultationLineId", null)
                        .avec(USER_CREATE, AUTEUR).avec(USER_UPDATE, AUTEUR).avec(CREE_LE, MAINTENANT)
                        .avec(MODIFIE_LE, MAINTENANT)
                        .verifier(l, "BuyDocumentLine")),
                () -> assertThat(categories).singleElement().satisfies(v -> new ColonnesEcrites()
                        .avec("StlCategoryId", f.type().categorieComptable()).avec("VentilationRate", BigDecimal.ONE)
                        .verifier(v, "BuyVentilationCategory")),
                () -> assertThat(projets).isEmpty(),
                () -> assertThat(historique).singleElement().satisfies(h -> new ColonnesEcrites()
                        .avec("BuyDocumentStatusId", STATUT_EN_COURS)
                        .avec("AdmUserId", f.admin().getId())
                        .avec("Notes", "")
                        .avec("DocDate", MAINTENANT)
                        .avec("buyDocumentWorkFlowId", null)
                        .verifier(h, "BuyDocumentStatusHistory")));
    }

    // ------------------------------------------------------------------ R1 séquence partagée

    /** R1 : commission puis frais terrain, une ligne « Facture fournisseur », SeqValue + 1 par pièce. */
    @Test
    void r1_commissionPuisFraisTerrain_seqValuePlusUnParPiece() throws Exception {
        Frais f = frais();

        Tuple commission = documenterCommission();
        int apresCommission = terrains.valeurSequence();
        Tuple facture = validerFrais(f);

        assertAll(
                () -> assertThat(commission.get("Code")).isEqualTo("27/03014"),
                () -> assertThat(apresCommission).isEqualTo(3014),
                () -> assertThat(facture.get("Code")).isEqualTo("27/03015"),
                () -> assertThat(terrains.valeurSequence()).isEqualTo(3015));
    }

    private UUID marcheParDefaut() {
        List<Tuple> l = terrains.lignes(MARCHE_PAR_DEFAUT, commercial);
        Object v = l.isEmpty() ? null : l.get(0).get("v");
        return v == null ? null : UUID.fromString(v.toString());
    }
}
