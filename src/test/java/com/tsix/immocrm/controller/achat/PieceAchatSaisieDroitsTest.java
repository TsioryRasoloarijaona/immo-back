package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NatureSaisie;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2a E-C1.1 à E-C1.3 et E-C6 (FEATURE-achat-fiche-creation.md, Vérification n° 3) : code {@code toolNew} de la nature pour
 * créer, ouvrir le modèle, modifier ; code Projet › Lot accepté seulement avec un lot (lot stocké ET lot du corps au
 * {@code PUT}) ; facture : code Projet ignoré ; Administrateur ; {@code naturesCreables} et {@code actions.peutModifier}.
 * Comptes sans personnel : la restriction « mes BC » ne joue pas ici.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatSaisieDroitsTest {

    private static final String LOT_CHAMP = "lotId";
    private static final String PEUT_MODIFIER = "$.actions.peutModifier";
    private static final String NATURES = "$.naturesCreables";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private Saisie s;
    private UUID lot;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ADS");
        s = fixture.configurationSaisie(c.jeton());
        lot = fixture.lot("LOT-" + c.jeton(), c.projet());
    }

    private AdmUser compte(String... droits) {
        AdmUser compte = fixture.compteHabilite(c, "ADS" + (rang++), ROLE_JWT, droits);
        fixture.sansPersonnel(compte);
        return compte;
    }

    private AdmUser administrateur() {
        AdmUser compte = fixture.compteHabilite(c, "ADS-ADM" + (rang++), ADMINISTRATEUR);
        fixture.sansPersonnel(compte);
        return compte;
    }

    private MvcResult par(AdmUser compte, MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(compte, ROLE_JWT));
    }

    /** Pièce existante de la nature, statut EN COURS (NULL pour un devis), lot éventuel. */
    private UUID piece(NatureSaisie n, boolean avecLot) {
        UUID id = fixture.piece(c, "-" + n.name() + (rang++), n.code);
        fixture.poser(id, "StrEntityId", s.gestion(n.gestion));
        if (n != NatureSaisie.DEVIS) {
            fixture.poser(id, "BuyDocumentStatusId", s.enCours());
        }
        if (avecLot) {
            fixture.poser(id, "PrjMarketId", lot);
        }
        return id;
    }

    private CorpsJson corpsDe(NatureSaisie n) {
        return corpsValide(c, s, n.code);
    }

    private void assert403RienCree(MvcResult r, NatureSaisie n) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
        fixture.oublier();
        assertAll(
                () -> assertThat(fixture.nombre("BuyDocument", "CfgTierId", c.fournisseur())).isZero(),
                () -> assertThat(fixture.valeurSequence(n.sequence().code())).isEqualTo(n.sequence().valeur()));
    }

    // ------------------------------------------------------------------ E-C1.1 création

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec11_creation_sansDroit_403_rienEcrit(NatureSaisie n) throws Exception {
        assert403RienCree(par(compte(), creer(corpsDe(n))), n);
    }

    /** E-C1.1 : le code de création d'une autre nature ne suffit pas. */
    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec11_creation_droitDUneAutreNature_403(NatureSaisie n) throws Exception {
        String autre = n == NatureSaisie.BC ? NOUVEAU_FACTURE : NOUVEAU_BC;

        assert403RienCree(par(compte(autre), creer(corpsDe(n))), n);
    }

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec11_creation_avecLeCodeDeLaNature_201(NatureSaisie n) throws Exception {
        MvcResult r = par(compte(n.droit), creer(corpsDe(n)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
    }

    @ParameterizedTest
    @EnumSource(value = NatureSaisie.class, names = "FACTURE", mode = EnumSource.Mode.EXCLUDE)
    void ec11_creation_codeProjetLot_avecLot_201(NatureSaisie n) throws Exception {
        MvcResult r = par(compte(n.droitLot), creer(corpsDe(n).avec(LOT_CHAMP, lot)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
    }

    @ParameterizedTest
    @EnumSource(value = NatureSaisie.class, names = "FACTURE", mode = EnumSource.Mode.EXCLUDE)
    void ec11_creation_codeProjetLot_sansLot_403(NatureSaisie n) throws Exception {
        assert403RienCree(par(compte(n.droitLot), creer(corpsDe(n))), n);
    }

    /** E-C1.1 : facture — {@code Yes.Prj.Front.Market.InvoiceList.toolNew} ignoré, même avec un lot. */
    @Test
    void ec11_facture_codeProjetLotIgnore_memeAvecLot_403() throws Exception {
        assert403RienCree(par(compte(NOUVEAU_FACTURE_LOT_IGNORE), creer(corpsDe(NatureSaisie.FACTURE).avec(LOT_CHAMP, lot))),
                NatureSaisie.FACTURE);
    }

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec11_creation_administrateurParAdmUserRole_201(NatureSaisie n) throws Exception {
        MvcResult r = par(administrateur(), creer(corpsDe(n)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
    }

    /** Contrat : 403 avant 400 — un corps invalide d'un compte sans droit reçoit 403. */
    @Test
    void contrat_creation_sansDroit_corpsInvalide_403() throws Exception {
        MvcResult r = par(compte(), creer(corpsDe(NatureSaisie.BC).avec("dateDocument", null)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
    }

    // ------------------------------------------------------------------ E-C1.3 modèle

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec13_modele_sansDroitDeCreer_403(NatureSaisie n) throws Exception {
        assertThat(statut(par(compte(), modele(n.code)))).isEqualTo(403);
    }

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec13_modele_avecLeCodeDeLaNature_200(NatureSaisie n) throws Exception {
        MvcResult r = par(compte(n.droit), modele(n.code));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
    }

    // ------------------------------------------------------------------ E-C1.2 modification

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec12_modification_sansDroit_403_pieceIntacte(NatureSaisie n) throws Exception {
        UUID piece = piece(n, false);

        MvcResult r = par(compte(), modifier(piece, corpsDe(n).avec("notes", "modifiée")));

        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
        fixture.oublier();
        assertThat(fixture.pieceEnBase(piece)).containsEntry("Notes", null);
    }

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec12_modification_avecLeCodeDeLaNature_200(NatureSaisie n) throws Exception {
        UUID piece = piece(n, false);

        MvcResult r = par(compte(n.droit), modifier(piece, corpsDe(n)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
    }

    /** E-C1.2 : code Projet › Lot, lot stocké ET lot du corps → 200. */
    @Test
    void ec12_codeProjetLot_lotStockeEtLotDuCorps_200() throws Exception {
        UUID piece = piece(NatureSaisie.BC, true);

        MvcResult r = par(compte(NOUVEAU_BC_LOT), modifier(piece, corpsDe(NatureSaisie.BC).avec(LOT_CHAMP, lot)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
    }

    /** E-C1.2 : pièce à lot dont le corps retire le lot → le corps doit aussi passer → 403. */
    @Test
    void ec12_codeProjetLot_lotStocke_corpsSansLot_403() throws Exception {
        UUID piece = piece(NatureSaisie.BC, true);

        MvcResult r = par(compte(NOUVEAU_BC_LOT), modifier(piece, corpsDe(NatureSaisie.BC)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
        fixture.oublier();
        assertThat(fixture.lienDe(piece, "PrjMarketId")).isEqualTo(AppelsEcrituresAchat.id(lot));
    }

    /** E-C1.2 : pièce sans lot à laquelle le corps ajoute un lot → la pièce stockée doit aussi passer → 403. */
    @Test
    void ec12_codeProjetLot_sansLotStocke_corpsAvecLot_403() throws Exception {
        UUID piece = piece(NatureSaisie.BC, false);

        MvcResult r = par(compte(NOUVEAU_BC_LOT), modifier(piece, corpsDe(NatureSaisie.BC).avec(LOT_CHAMP, lot)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
        fixture.oublier();
        assertThat(fixture.lienDe(piece, "PrjMarketId")).isNull();
    }

    @Test
    void ec12_facture_codeProjetLotIgnore_403() throws Exception {
        UUID piece = piece(NatureSaisie.FACTURE, true);

        MvcResult r = par(compte(NOUVEAU_FACTURE_LOT_IGNORE),
                modifier(piece, corpsDe(NatureSaisie.FACTURE).avec(LOT_CHAMP, lot)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
    }

    // ------------------------------------------------------------------ E-C6

    /** E-C6 : {@code naturesCreables} = natures dont le compte porte le code Achat {@code toolNew}. */
    @Test
    void ec6_naturesCreables_codesAchatDuCompte() throws Exception {
        String json = corps(par(compte(NOUVEAU_BC, NOUVEAU_BL), referentiels()));

        assertThat((List<String>) JsonPath.read(json, NATURES))
                .containsExactlyInAnyOrder(FixtureAchat.BON_COMMANDE, FixtureAchat.BON_LIVRAISON);
    }

    @Test
    void ec6_naturesCreables_sansDroit_vide() throws Exception {
        String json = corps(par(compte(), referentiels()));

        assertThat((List<String>) JsonPath.read(json, NATURES)).isNotNull().isEmpty();
    }

    /** E-C6 : « code Achat » seulement — un code Projet › Lot n'ouvre pas Ajouter. */
    @Test
    void ec6_naturesCreables_codeProjetLotSeul_vide() throws Exception {
        String json = corps(par(compte(NOUVEAU_BC_LOT, NOUVEAU_DEVIS_LOT), referentiels()));

        assertThat((List<String>) JsonPath.read(json, NATURES)).isNotNull().isEmpty();
    }

    @Test
    void ec6_naturesCreables_administrateur_lesQuatreNatures() throws Exception {
        String json = corps(par(administrateur(), referentiels()));

        assertThat((List<String>) JsonPath.read(json, NATURES)).containsExactlyInAnyOrder(FixtureAchat.DEVIS,
                FixtureAchat.BON_COMMANDE, FixtureAchat.BON_LIVRAISON, FixtureAchat.FACTURE);
    }

    /** Réponse 12 du manager : {@code naturesCreables} ne contient jamais REFUND, même avec {@code RefundList.toolNew}. */
    @Test
    void ec6_naturesCreables_jamaisRefund_memeAvecSonCode() throws Exception {
        String json = corps(par(compte(NOUVEL_AVOIR, NOUVEAU_FACTURE), referentiels()));

        assertThat((List<String>) JsonPath.read(json, NATURES)).containsExactly(FixtureAchat.FACTURE);
    }

    @ParameterizedTest
    @EnumSource(NatureSaisie.class)
    void ec6_peutModifier_vraiAvecLeCode_fauxSans(NatureSaisie n) throws Exception {
        UUID piece = piece(n, false);

        String avec = corps(par(compte(n.droit), fiche(piece)));
        String sans = corps(par(compte(), fiche(piece)));

        assertAll(
                () -> assertThat((Boolean) JsonPath.read(avec, PEUT_MODIFIER)).isTrue(),
                () -> assertThat((Boolean) JsonPath.read(sans, PEUT_MODIFIER)).isFalse());
    }
}
