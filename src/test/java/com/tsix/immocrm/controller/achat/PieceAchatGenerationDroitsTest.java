package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.LIGNE_BORDEREAU;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.avecSources;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.modele;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BC;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BL;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BL_LOT;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_DEVIS;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.ligne;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.DEVIS;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2b ordre des refus et droits (FEATURE-achat-generation.md §3 « 404 → 403 → 400 → 409 », G2 droit, §8
 * « PieceAchatGenerationDroitsTest ») : 404 source inconnue / hors {@code VISIBLE} / BC d'un autre déclencheur, chacun avec
 * un témoin 2xx du même compte sur la même route (une 404 de Spring ne prouve rien) ; 403 sans droit de créer la cible ;
 * clé d'API refusée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatGenerationDroitsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private Cadre c;
    private Saisie s;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AGD");
        s = fixture.configurationSaisie(c.jeton());
        generation.parametres(false, false, false);
        generation.tableBuyOrderInvoice();
    }

    /** Compte sans personnel (pas de restriction « mes BC »), habilité société + projet du cadre. */
    private AdmUser compte(String... droits) {
        AdmUser compte = fixture.compteHabilite(c, "AGD" + (rang++), ROLE_JWT, droits);
        fixture.sansPersonnel(compte);
        return compte;
    }

    private MvcResult par(AdmUser compte, MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(compte, ROLE_JWT));
    }

    /** BC validé du cadre, ligne ART-1 10 × 100. */
    private UUID bcValide() {
        UUID bc = generation.source(c, "-BC" + (rang++), BON_COMMANDE, s.gestion(FixtureAchat.GESTION_BC), s.valideDg());
        generation.ligne(bc, LigneSource.simple(1, "ART-1", "10", s.categorie()));
        return bc;
    }

    private CorpsJson corpsBl(UUID... sources) {
        return avecSources(corpsValide(c, s, BON_LIVRAISON), sources);
    }

    private static void assertStatut(MvcResult r, int attendu) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(attendu);
    }

    private void assertRienCree(String nature) {
        fixture.oublier();
        FixtureAchat.SequenceNature seq = FixtureAchat.SEQUENCES_SAISIE.get(nature);
        assertAll(
                () -> assertThat(fixture.valeurSequence(seq.code())).isEqualTo(seq.valeur()),
                () -> assertThat(generation.nombreParCode(c.fournisseur(), seq.codeSuivant())).isZero());
    }

    // ------------------------------------------------------------------ 404 source inconnue

    @Test
    void r404_modele_sourceInconnue_404_temoinSourceConnue200() throws Exception {
        AdmUser compte = compte(NOUVEAU_BL);
        UUID connue = bcValide();

        MvcResult inconnue = par(compte, modele(BON_LIVRAISON, UUID.randomUUID()));
        MvcResult temoin = par(compte, modele(BON_LIVRAISON, connue));

        assertStatut(temoin, 200);
        assertStatut(inconnue, 404);
    }

    @Test
    void r404_post_sourceInconnue_404_rienEcrit_temoinSourceConnue201() throws Exception {
        AdmUser compte = compte(NOUVEAU_BL);

        MvcResult inconnue = par(compte, creer(corpsBl(UUID.randomUUID())));
        assertStatut(inconnue, 404);
        assertRienCree(BON_LIVRAISON);

        assertStatut(par(compte, creer(corpsBl(bcValide()))), 201);
    }

    /** §3 : une source inconnue parmi plusieurs suffit. */
    @Test
    void r404_post_uneSourceInconnueParmiPlusieurs_404() throws Exception {
        AdmUser compte = compte(NOUVEAU_FACTURE);
        UUID bl = generation.source(c, "-BL", BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), s.valideDg());

        assertStatut(par(compte, creer(avecSources(corpsValide(c, s, FACTURE), bl, UUID.randomUUID()))), 404);
        assertRienCree(FACTURE);
    }

    // ------------------------------------------------------------------ 404 hors VISIBLE

    /** BC validé d'un projet de la société où le compte n'est pas habilité. */
    private UUID bcHorsProjetHabilite() {
        UUID projet = fixture.projet("PNH-" + c.jeton(), c.societe());
        UUID bc = fixture.piece(FixtureAchat.Piece.facture(c.jeton() + "-BCNH", AppelsSaisieAchat.DATE, c.fournisseur(),
                c.fournisseurNom(), c.societe(), projet, java.math.BigDecimal.TEN).nature(BON_COMMANDE).statut(s.valideDg()));
        fixture.poser(bc, "StrEntityId", s.gestion(FixtureAchat.GESTION_BC));
        generation.ligne(bc, LigneSource.simple(1, "ART-1", "10", s.categorie()));
        return bc;
    }

    @Test
    void r404_modele_sourceHorsVisible_404_temoin200() throws Exception {
        AdmUser compte = compte(NOUVEAU_BL);

        MvcResult invisible = par(compte, modele(BON_LIVRAISON, bcHorsProjetHabilite()));
        MvcResult temoin = par(compte, modele(BON_LIVRAISON, bcValide()));

        assertStatut(temoin, 200);
        assertStatut(invisible, 404);
    }

    @Test
    void r404_post_sourceHorsVisible_404_rienEcrit() throws Exception {
        AdmUser compte = compte(NOUVEAU_BL);

        assertStatut(par(compte, creer(corpsBl(bcHorsProjetHabilite()))), 404);
        assertRienCree(BON_LIVRAISON);
        assertStatut(par(compte, creer(corpsBl(bcValide()))), 201);
    }

    // ------------------------------------------------------------------ 404 BC d'un autre déclencheur

    /** Compte restreint « mes BC » : il a un personnel et pas le droit {@code OrderList.WithNoRestriction}. */
    private AdmUser compteRestreint() {
        return fixture.compteHabilite(c, "AGD-R" + (rang++), ROLE_JWT, NOUVEAU_BL);
    }

    private UUID bcDeclenchePar(UUID personnel) {
        UUID bc = bcValide();
        fixture.poser(bc, "CfgTierTriggerId", personnel);
        return bc;
    }

    @Test
    void r404_modele_bcDUnAutreDeclencheur_404_temoinSonBc200() throws Exception {
        AdmUser restreint = compteRestreint();
        AdmUser autre = compteRestreint();
        UUID sonBc = bcDeclenchePar(restreint.getCfgEmployee().getId());
        UUID bcAutre = bcDeclenchePar(autre.getCfgEmployee().getId());

        MvcResult refuse = par(restreint, modele(BON_LIVRAISON, bcAutre));
        MvcResult temoin = par(restreint, modele(BON_LIVRAISON, sonBc));

        assertStatut(temoin, 200);
        assertStatut(refuse, 404);
    }

    @Test
    void r404_post_bcDUnAutreDeclencheur_404_rienEcrit() throws Exception {
        AdmUser restreint = compteRestreint();
        UUID bcAutre = bcDeclenchePar(compteRestreint().getCfgEmployee().getId());

        assertStatut(par(restreint, creer(corpsBl(bcAutre))), 404);
        assertRienCree(BON_LIVRAISON);
    }

    /** Témoin de la restriction : avec {@code OrderList.WithNoRestriction}, le BC d'un autre déclencheur est générable. */
    @Test
    void r404_bcDUnAutreDeclencheur_droitSansRestriction_200() throws Exception {
        AdmUser libre = compteRestreint();
        fixture.droitOmran(libre, FixtureAchat.DROIT_BC_SANS_RESTRICTION);
        UUID bcAutre = bcDeclenchePar(compteRestreint().getCfgEmployee().getId());

        assertStatut(par(libre, modele(BON_LIVRAISON, bcAutre)), 200);
    }

    // ------------------------------------------------------------------ ordre 404 → 403 → 400 → 409

    /** 404 avant 403 : source inconnue pour un compte sans droit → 404 ; témoin : source connue → 403. */
    @Test
    void ordre_404avant403_sourceInconnue_sansDroit() throws Exception {
        AdmUser sansDroit = compte();

        MvcResult inconnue = par(sansDroit, creer(corpsBl(UUID.randomUUID())));
        MvcResult temoin = par(sansDroit, creer(corpsBl(bcValide())));

        assertStatut(temoin, 403);
        assertStatut(inconnue, 404);
    }

    /** 403 avant 400 : {@code sources} sur QUOTE (400 G1) pour un compte sans le droit de créer un devis → 403. */
    @Test
    void ordre_403avant400_sourcesSurDevis_sansDroitDevis() throws Exception {
        UUID bc = bcValide();

        MvcResult sansDroit = par(compte(NOUVEAU_BC), modele(DEVIS, bc));
        MvcResult avecDroit = par(compte(NOUVEAU_DEVIS), modele(DEVIS, bc));

        assertStatut(avecDroit, 400);
        assertStatut(sansDroit, 403);
    }

    /** 400 avant 409 : BL non validé (409 G2) avec un {@code ligneBordereauId} étranger aux sources (400) → 400. */
    @Test
    void ordre_400avant409_bordereauEtranger_blNonValide() throws Exception {
        AdmUser compte = compte(NOUVEAU_FACTURE);
        UUID lot = fixture.lot("LOT-" + c.jeton(), c.projet());
        UUID etranger = generation.ligneBordereau(lot, "E");
        UUID bl = generation.source(c, "-BLNV", BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), s.enCours());
        generation.ligne(bl, LigneSource.simple(1, "ART-1", "1", s.categorie()));
        CorpsJson ligneEtrangere = ligne("ART-1", "1", "100", s.categorie()).avec(LIGNE_BORDEREAU, etranger);

        MvcResult avec400 = par(compte, creer(avecSources(corpsValide(c, s, FACTURE).avec("lignes", List.of(ligneEtrangere)),
                bl)));
        MvcResult temoin = par(compte, creer(avecSources(corpsValide(c, s, FACTURE), bl)));

        assertStatut(temoin, 409);
        assertStatut(avec400, 400);
    }

    // ------------------------------------------------------------------ 403 droit de créer la cible (G2)

    @Test
    void droit_modele_sansDroitDeCreerLaCible_403_droitDeLaSourceInsuffisant() throws Exception {
        UUID bc = bcValide();

        MvcResult droitSource = par(compte(NOUVEAU_BC), modele(BON_LIVRAISON, bc));
        MvcResult droitCible = par(compte(NOUVEAU_BL), modele(BON_LIVRAISON, bc));

        assertStatut(droitCible, 200);
        assertStatut(droitSource, 403);
    }

    @Test
    void droit_post_sansDroitDeCreerLaCible_403_rienEcrit() throws Exception {
        UUID bc = bcValide();

        assertStatut(par(compte(NOUVEAU_BC), creer(corpsBl(bc))), 403);
        assertRienCree(BON_LIVRAISON);
        assertThat(fixture.nombre("BuyDocument", "BuyOrderId", bc)).isZero();
    }

    /** G2 « comme 2a » : code Projet › Lot de la cible avec un lot dans le corps → 201 ; sans lot → 403. */
    @Test
    void droit_post_codeProjetLot_avecLot201_sansLot403() throws Exception {
        AdmUser compte = compte(NOUVEAU_BL_LOT);
        UUID lot = fixture.lot("LOT-" + c.jeton(), c.projet());

        MvcResult sansLot = par(compte, creer(corpsBl(bcValide())));
        MvcResult avecLot = par(compte, creer(corpsBl(bcValide()).avec("lotId", lot)));

        assertStatut(sansLot, 403);
        assertStatut(avecLot, 201);
    }

    // ------------------------------------------------------------------ clé d'API

    private String cleToutesPortees(AdmUser compte) {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-" + c.jeton());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    private MvcResult parCle(String cle, MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete.header(ApiKeyAuthenticationFilter.EN_TETE, cle)).andReturn();
    }

    @Test
    void cleApi_modeleAvecSources_refusee_temoinCompte200() throws Exception {
        AdmUser compte = compte(NOUVEAU_BL);
        String cle = cleToutesPortees(compte);
        UUID bc = bcValide();

        MvcResult temoin = par(compte, modele(BON_LIVRAISON, bc));
        MvcResult parCle = parCle(cle, modele(BON_LIVRAISON, bc));

        assertStatut(temoin, 200);
        assertThat(statut(parCle)).as(corps(parCle)).isIn(401, 403);
    }

    @Test
    void cleApi_postAvecSources_refusee_rienEcrit() throws Exception {
        AdmUser compte = compte(NOUVEAU_BL);
        String cle = cleToutesPortees(compte);
        UUID bc = bcValide();

        MvcResult parCle = parCle(cle, creer(corpsBl(bc)));

        assertThat(statut(parCle)).as(corps(parCle)).isIn(401, 403);
        assertRienCree(BON_LIVRAISON);
    }
}
