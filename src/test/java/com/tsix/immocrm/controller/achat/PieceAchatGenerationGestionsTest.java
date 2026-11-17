package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.NON_CONFIGUREE;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.avecSources;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.modele;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BC;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BL;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.decimalDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.guidDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.idCree;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.DEVIS;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2b G3 et Q-6 (FEATURE-achat-generation.md §4, §8 « gestion d'arrivée », « Q-6 chiffré ») : la gestion de la pièce
 * générée est celle de la paire mesurée (pas {@code nature.gestion()}), statut de départ lu sur cette gestion ; gestion
 * source sans paire → 409 ; taux du lot recopiés sur une facture générée seulement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatGenerationGestionsTest {

    private static final String STATUT = "BuyDocumentStatusId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AGG");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AGG", ROLE_JWT, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
        generation.parametres(true, false, false);
        generation.tableBuyOrderInvoice();
    }

    private UUID gestion(String code) {
        UUID saisie = s.gestion(code);
        return saisie != null ? saisie : generation.gestion(code);
    }

    /** Source validée (statut NULL pour un devis) de gestion donnée, une ligne ART-1 10 × 100. */
    private UUID source(String nature, String gestion) {
        UUID statut = DEVIS.equals(nature) ? null : s.valideDg();
        UUID id = generation.source(c, "-S" + (rang++), nature, gestion(gestion), statut);
        generation.ligne(id, LigneSource.simple(1, "ART-1", "10", s.categorie()));
        return id;
    }

    private MvcResult par(MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(redacteur, ROLE_JWT));
    }

    // ------------------------------------------------------------------ G3 paires

    /** Les 8 paires mesurées (§4 G3) : gestion et nature de la source → nature et gestion de la cible. */
    enum Paire {
        DEVIS_BC(FixtureAchat.GESTION_DEVIS, DEVIS, BON_COMMANDE, FixtureAchat.GESTION_BC),
        DEVIS_BC_MULTI(FixtureGenerationAchat.GESTION_DEVIS_MULTI, DEVIS, BON_COMMANDE, FixtureGenerationAchat.GESTION_BC_MULTI),
        BC_BL(FixtureAchat.GESTION_BC, BON_COMMANDE, BON_LIVRAISON, FixtureAchat.GESTION_BL),
        BC_FACTURE(FixtureAchat.GESTION_BC, BON_COMMANDE, FACTURE, FixtureAchat.GESTION_FACTURE),
        BL_FACTURE(FixtureAchat.GESTION_BL, BON_LIVRAISON, FACTURE, FixtureAchat.GESTION_FACTURE),
        BC_BL_MULTI(FixtureGenerationAchat.GESTION_BC_MULTI, BON_COMMANDE, BON_LIVRAISON,
                FixtureGenerationAchat.GESTION_BL_MULTI),
        BC_FACTURE_MULTI(FixtureGenerationAchat.GESTION_BC_MULTI, BON_COMMANDE, FACTURE,
                FixtureGenerationAchat.GESTION_FACTURE_MULTI),
        BL_FACTURE_MULTI(FixtureGenerationAchat.GESTION_BL_MULTI, BON_LIVRAISON, FACTURE,
                FixtureGenerationAchat.GESTION_FACTURE_MULTI);

        final String gestionSource;
        final String natureSource;
        final String natureCible;
        final String gestionCible;

        Paire(String gestionSource, String natureSource, String natureCible, String gestionCible) {
            this.gestionSource = gestionSource;
            this.natureSource = natureSource;
            this.natureCible = natureCible;
            this.gestionCible = gestionCible;
        }
    }

    /** G3 : le modèle renvoie la gestion de la paire. */
    @ParameterizedTest
    @EnumSource(Paire.class)
    void g3_modele_gestionDeLaPaire(Paire p) throws Exception {
        gestion(p.gestionCible);
        UUID source = source(p.natureSource, p.gestionSource);

        MvcResult r = par(modele(p.natureCible, source));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat((String) JsonPath.read(corps(r), "$.gestion")).isEqualTo(p.gestionCible);
    }

    /** G3 : la pièce créée porte la gestion de la paire (StrEntityId). */
    @ParameterizedTest
    @EnumSource(Paire.class)
    void g3_post_gestionDeLaPaire(Paire p) throws Exception {
        UUID cible = gestion(p.gestionCible);
        UUID source = source(p.natureSource, p.gestionSource);

        MvcResult r = par(creer(avecSources(corpsValide(c, s, p.natureCible), source)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        assertThat(guidDe(fixture.pieceEnBase(idCree(r)), "StrEntityId")).isEqualTo(id(cible));
    }

    /** G3 : statut de départ lu sur la gestion d'arrivée (multi-fournisseur), pas sur {@code nature.gestion()}. */
    @Test
    void g3_multiFournisseur_statutDeDepart_deLaGestionDArrivee() throws Exception {
        UUID blMulti = gestion(FixtureGenerationAchat.GESTION_BL_MULTI);
        UUID departMulti = fixture.statutSaisie("DEPART-MULTI-" + c.jeton(), 5, FixtureAchat.ETAT_NOUVEAU, false, false);
        fixture.transitionDe(blMulti, 10, departMulti, s.valideDg());
        UUID bcMulti = source(BON_COMMANDE, FixtureGenerationAchat.GESTION_BC_MULTI);

        String modele = corps(par(modele(BON_LIVRAISON, bcMulti)));
        MvcResult r = par(creer(avecSources(corpsValide(c, s, BON_LIVRAISON), bcMulti)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        assertAll(
                () -> assertThat(((String) JsonPath.read(modele, "$.statutDepartId")).toUpperCase()).isEqualTo(id(departMulti)),
                () -> assertThat(guidDe(fixture.pieceEnBase(idCree(r)), STATUT)).isEqualTo(id(departMulti)));
    }

    /** G3 (Q-8) : gestion source sans paire (devis de lot PrjMarketQuote) → 409 nommant la gestion, au modèle. */
    @Test
    void g3_modele_gestionSansPaire_409_message() throws Exception {
        UUID devisLot = source(DEVIS, FixtureGenerationAchat.GESTION_DEVIS_LOT);

        MvcResult r = par(modele(BON_COMMANDE, devisLot));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(NON_CONFIGUREE + " " + FixtureGenerationAchat.GESTION_DEVIS_LOT);
    }

    /** G3 (Q-8) : même refus au POST, rien écrit. */
    @Test
    void g3_post_gestionSansPaire_409_rienEcrit() throws Exception {
        UUID devisLot = source(DEVIS, FixtureGenerationAchat.GESTION_DEVIS_LOT);

        MvcResult r = par(creer(avecSources(corpsValide(c, s, BON_COMMANDE), devisLot)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(NON_CONFIGUREE + " " + FixtureGenerationAchat.GESTION_DEVIS_LOT);
        fixture.oublier();
        FixtureAchat.SequenceNature seq = FixtureAchat.SEQUENCES_SAISIE.get(BON_COMMANDE);
        assertThat(fixture.valeurSequence(seq.code())).isEqualTo(seq.valeur());
    }

    /** Réponse 10 : source sans {@code StrEntityId} → 409 « non configurée », au modèle et au POST, rien écrit. */
    @Test
    void r10_sourceSansGestion_409_nonConfiguree() throws Exception {
        UUID bc = source(BON_COMMANDE, FixtureAchat.GESTION_BC);
        generation.sansGestion(bc);

        MvcResult m = par(modele(BON_LIVRAISON, bc));
        MvcResult r = par(creer(avecSources(corpsValide(c, s, BON_LIVRAISON), bc)));

        assertThat(statut(m)).as(corps(m)).isEqualTo(409);
        assertThat(detail(m)).startsWith(NON_CONFIGUREE);
        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).startsWith(NON_CONFIGUREE);
        fixture.oublier();
        FixtureAchat.SequenceNature seq = FixtureAchat.SEQUENCES_SAISIE.get(BON_LIVRAISON);
        assertThat(fixture.valeurSequence(seq.code())).isEqualTo(seq.valeur());
    }

    // ------------------------------------------------------------------ Q-6 (décision Q-A)

    private static final Map<String, String> TAUX_LOT = Map.of("AdvanceRatio", "0.1", "WarrantyRatio", "0.07",
            "ProportionRatio", "0.02", "InsuranceDECRatio", "0.01", "InsuranceTRCRatio", "0.005");

    private UUID lotAvecTaux() {
        UUID lot = fixture.lot("LOT-" + c.jeton(), c.projet());
        TAUX_LOT.forEach((colonne, taux) -> fixture.poserSur("PrjMarket", lot, colonne, new BigDecimal(taux)));
        return lot;
    }

    /** Q-6 chiffré : facture générée depuis un BL d'un lot à taux ≠ 0 → les 5 taux du lot recopiés. */
    @Test
    void q6_factureGeneree_depuisBlDUnLot_copieLesTauxDuLot() throws Exception {
        UUID lot = lotAvecTaux();
        UUID bl = source(BON_LIVRAISON, FixtureAchat.GESTION_BL);
        fixture.poser(bl, "PrjMarketId", lot);

        MvcResult r = par(creer(avecSources(corpsValide(c, s, FACTURE).avec("lotId", lot), bl)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        Map<String, Object> d = fixture.pieceEnBase(idCree(r));
        assertThat(TAUX_LOT).isNotEmpty().allSatisfy((colonne, taux) ->
                assertThat(decimalDe(d, colonne)).as(colonne).isEqualByComparingTo(taux));
    }
}
