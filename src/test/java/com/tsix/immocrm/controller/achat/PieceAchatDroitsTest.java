package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
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

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-1, E-B1 (table des droits de suppression / modification) et E-B6 (bloc {@code actions}), Vérification n° 1 :
 * {@code DELETE /api/achat/documents/{id}} → 403 sans le code, 204 avec ; code Projet › Lot accepté si la pièce a un lot ;
 * Administrateur reconnu par {@code AdmUserRole} ; {@code actions.peutSupprimer / peutAffecter} suivent les mêmes droits.
 * Comptes sans personnel : la restriction « mes BC » ne s'applique pas ici (testée dans {@code PieceAchatVisibiliteEcrituresTest}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatDroitsTest {

    /** Nature, code de suppression Achat, code Projet › Lot (null : aucun pour la facture, Q-D). */
    enum Nature {
        DEVIS(FixtureAchat.DEVIS, SUPPR_DEVIS, SUPPR_DEVIS_LOT),
        BC(FixtureAchat.BON_COMMANDE, SUPPR_BC, SUPPR_BC_LOT),
        BL(FixtureAchat.BON_LIVRAISON, SUPPR_BL, SUPPR_BL_LOT),
        FACTURE(FixtureAchat.FACTURE, SUPPR_FACTURE, null),
        AVOIR(FixtureAchat.AVOIR, SUPPR_AVOIR, SUPPR_AVOIR_LOT);

        final String code;
        final String droitAchat;
        final String droitLot;

        Nature(String code, String droitAchat, String droitLot) {
            this.code = code;
            this.droitAchat = droitAchat;
            this.droitLot = droitLot;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private UUID lot;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ADR");
        lot = fixture.lot("LOT-" + c.jeton(), c.projet());
        fixture.tablesComptables();
    }

    private AdmUser compte(String... droits) {
        AdmUser compte = fixture.compteHabilite(c, "ADR" + (rang++), ROLE_JWT, droits);
        fixture.sansPersonnel(compte);
        return compte;
    }

    private AdmUser administrateur() {
        AdmUser compte = fixture.compteHabilite(c, "ADR-ADM" + (rang++), ADMINISTRATEUR);
        fixture.sansPersonnel(compte);
        return compte;
    }

    private UUID piece(Nature n, boolean avecLot) {
        UUID id = fixture.piece(c, "-" + n.name() + (rang++), n.code);
        if (avecLot) {
            fixture.poser(id, "PrjMarketId", lot);
        }
        return id;
    }

    private MvcResult supprimerPar(AdmUser compte, UUID piece) throws Exception {
        return appeler(mockMvc, supprimer(piece), connecte(compte, ROLE_JWT));
    }

    private String ficheDe(AdmUser compte, UUID piece) throws Exception {
        MvcResult r = appeler(mockMvc, fiche(piece), connecte(compte, ROLE_JWT));
        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        return corps(r);
    }

    private void assertRefus403EtPieceIntacte(MvcResult r, UUID piece) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
        fixture.oublier();
        assertThat(fixture.nombre("BuyDocument", "PKey", piece)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ suppression : code Achat

    @ParameterizedTest
    @EnumSource(Nature.class)
    void eb1_sansAucunDroit_suppression403_pieceIntacte(Nature n) throws Exception {
        UUID piece = piece(n, false);

        assertRefus403EtPieceIntacte(supprimerPar(compte(), piece), piece);
    }

    @ParameterizedTest
    @EnumSource(Nature.class)
    void eb1_avecLeCodeAchat_pieceSansLot_204(Nature n) throws Exception {
        UUID piece = piece(n, false);

        MvcResult r = supprimerPar(compte(n.droitAchat), piece);

        assertThat(statut(r)).as(corps(r)).isEqualTo(204);
        assertThat(fixture.nombre("BuyDocument", "PKey", piece)).isZero();
    }

    /** « l'un ou l'autre de deux codes » : le code Achat reste accepté sur une pièce à lot. */
    @ParameterizedTest
    @EnumSource(Nature.class)
    void eb1_avecLeCodeAchat_pieceAvecLot_204(Nature n) throws Exception {
        UUID piece = piece(n, true);

        assertThat(statut(supprimerPar(compte(n.droitAchat), piece))).isEqualTo(204);
    }

    // ------------------------------------------------------------------ suppression : code Projet › Lot

    @ParameterizedTest
    @EnumSource(value = Nature.class, names = "FACTURE", mode = EnumSource.Mode.EXCLUDE)
    void eb1_codeProjetLot_pieceAvecLot_204(Nature n) throws Exception {
        UUID piece = piece(n, true);

        MvcResult r = supprimerPar(compte(n.droitLot), piece);

        assertThat(statut(r)).as(corps(r)).isEqualTo(204);
    }

    @ParameterizedTest
    @EnumSource(value = Nature.class, names = "FACTURE", mode = EnumSource.Mode.EXCLUDE)
    void eb1_codeProjetLot_pieceSansLot_403(Nature n) throws Exception {
        UUID piece = piece(n, false);

        assertRefus403EtPieceIntacte(supprimerPar(compte(n.droitLot), piece), piece);
    }

    /** Q-D : {@code Yes.Prj.Front.Market.InvoiceList.toolDelete} n'ouvre pas la suppression d'une facture, même à lot. */
    @Test
    void qD_factureAvecLot_codeProjetLotFacture_ignore_403() throws Exception {
        UUID piece = piece(Nature.FACTURE, true);

        assertRefus403EtPieceIntacte(supprimerPar(compte(SUPPR_FACTURE_LOT_IGNORE), piece), piece);
    }

    /** Un code voisin (création) ne vaut pas le code de suppression : seul le code exact compte. */
    @Test
    void eb1_droitDeCreationSeul_neSuffitPasPourSupprimer_403() throws Exception {
        UUID piece = piece(Nature.BC, false);

        assertRefus403EtPieceIntacte(supprimerPar(compte("Yes.Buy.Front.Order.OrderList.toolNew"), piece), piece);
    }

    // ------------------------------------------------------------------ Administrateur

    @ParameterizedTest
    @EnumSource(Nature.class)
    void eb1_administrateurParAdmUserRole_sansAucunAdmRight_204(Nature n) throws Exception {
        UUID piece = piece(n, false);

        MvcResult r = supprimerPar(administrateur(), piece);

        assertThat(statut(r)).as(corps(r)).isEqualTo(204);
    }

    // ------------------------------------------------------------------ E-B6 actions

    @ParameterizedTest
    @EnumSource(Nature.class)
    void eb6_peutSupprimer_vraiAvecLeCode_fauxSans(Nature n) throws Exception {
        UUID piece = piece(n, false);
        AdmUser avec = compte(n.droitAchat);
        AdmUser sans = compte();

        String ficheAvec = ficheDe(avec, piece);
        String ficheSans = ficheDe(sans, piece);

        assertAll(
                () -> assertThat((Boolean) JsonPath.read(ficheAvec, "$.actions.peutSupprimer")).isTrue(),
                () -> assertThat((Boolean) JsonPath.read(ficheSans, "$.actions.peutSupprimer")).isFalse());
    }

    @Test
    void eb6_peutSupprimer_codeProjetLot_vraiSurPieceALot_fauxSansLot() throws Exception {
        AdmUser compte = compte(SUPPR_BC_LOT);
        UUID avecLot = piece(Nature.BC, true);
        UUID sansLot = piece(Nature.BC, false);

        String ficheAvecLot = ficheDe(compte, avecLot);
        String ficheSansLot = ficheDe(compte, sansLot);

        assertAll(
                () -> assertThat((Boolean) JsonPath.read(ficheAvecLot, "$.actions.peutSupprimer")).isTrue(),
                () -> assertThat((Boolean) JsonPath.read(ficheSansLot, "$.actions.peutSupprimer")).isFalse());
    }

    /** E-B6 : pièce « gérée ailleurs » (avance, facture de décompte) → peutSupprimer faux, même pour l'Administrateur. */
    @Test
    void eb6_pieceGereeAilleurs_peutSupprimerFaux_memeAdministrateur() throws Exception {
        AdmUser admin = administrateur();
        UUID avance = fixture.piece(c, "-AVANCE", FixtureAchat.DOCUMENT_A_REGLER);
        UUID factureDecompte = piece(Nature.FACTURE, false);
        fixture.decompte(factureDecompte);
        UUID fraisTerrain = piece(Nature.FACTURE, false);
        fixture.poser(fraisTerrain, "ReferenceType", "LAND_EXPENSE");

        String ficheAvance = ficheDe(admin, avance);
        String ficheDecompte = ficheDe(admin, factureDecompte);
        String ficheFrais = ficheDe(admin, fraisTerrain);

        assertAll(
                () -> assertThat((Boolean) JsonPath.read(ficheAvance, "$.actions.peutSupprimer")).isFalse(),
                () -> assertThat((Boolean) JsonPath.read(ficheDecompte, "$.actions.peutSupprimer")).isFalse(),
                () -> assertThat((Boolean) JsonPath.read(ficheFrais, "$.actions.peutSupprimer")).isFalse(),
                () -> assertThat((Boolean) JsonPath.read(ficheDecompte, "$.actions.peutAffecter")).isFalse(),
                () -> assertThat((Boolean) JsonPath.read(ficheFrais, "$.actions.peutAffecter")).isFalse());
    }

    /** E-B6 / Q-H : peutAffecter = facture + {@code InvoiceList.toolNew} + non gérée ailleurs ; jamais pour une autre nature. */
    @Test
    void eb6_peutAffecter_factureAvecDroitDeModification_seulement() throws Exception {
        UUID facture = piece(Nature.FACTURE, true);
        UUID bc = piece(Nature.BC, false);
        AdmUser avec = compte(MODIF_FACTURE);
        AdmUser sans = compte();
        AdmUser codeLotIgnore = compte(MODIF_FACTURE_LOT_IGNORE);
        AdmUser admin = administrateur();

        String factureAvec = ficheDe(avec, facture);
        String factureSans = ficheDe(sans, facture);
        String factureCodeLot = ficheDe(codeLotIgnore, facture);
        String bcAdmin = ficheDe(admin, bc);

        assertAll(
                () -> assertThat((Boolean) JsonPath.read(factureAvec, "$.actions.peutAffecter")).isTrue(),
                () -> assertThat((Boolean) JsonPath.read(factureSans, "$.actions.peutAffecter")).isFalse(),
                () -> assertThat((Boolean) JsonPath.read(factureCodeLot, "$.actions.peutAffecter")).as("Q-D").isFalse(),
                () -> assertThat((Boolean) JsonPath.read(bcAdmin, "$.actions.peutAffecter")).isFalse());
    }

    /** E-B6 : sans transition dans la gestion de la pièce, {@code actions.transitions} est une liste vide (jamais absente). */
    @Test
    void eb6_pieceSansWorkflow_transitionsVide() throws Exception {
        UUID devis = piece(Nature.DEVIS, false);

        String json = ficheDe(administrateur(), devis);

        assertThat((List<?>) JsonPath.read(json, "$.actions.transitions")).isNotNull().isEmpty();
    }
}
