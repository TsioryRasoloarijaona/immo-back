package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.detail;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-b1-reglements.md §3, G-a, G-c, G-d : {@code GET /decaissements/proposition?pieceId=} rend un
 * {@code SaisieDecaissementDto} prérempli depuis une pièce, sans rien enregistrer. Chaque refus a son témoin 200.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementPropositionTest {

    static final String MSG_NON_VALIDEE = "Vous ne pouvez pas effectuer un paiement sur une pièce qui n'est pas validée !";

    private static final String PROPOSITION = AppelsSaisieDecaissement.DECAISSEMENTS + "/proposition";
    private static final String PIECE_ID = "pieceId";
    private static final String REGLEMENT = "StlSettlement";
    private static final String TIERS = "TierId";
    private static final Configuration TOLERANTE =
            Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;
    @Autowired @Qualifier("horlogeMetier") private Clock horloge;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private RequestPostProcessor qui;
    private UUID pa;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DPR");
        qui = connecte(fixture.compte(s, "DPRA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU),
                FixtureTresorerie.ROLE);
        pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
    }

    private Reponse proposition(RequestPostProcessor compte, UUID piece) throws Exception {
        return appels.lire(compte, PROPOSITION, PIECE_ID, piece.toString());
    }

    private static Object champ(Reponse r, String chemin) {
        return JsonPath.using(TOLERANTE).parse(r.corps()).read(chemin);
    }

    private static BigDecimal montant(Reponse r, String chemin) {
        return new BigDecimal(String.valueOf(champ(r, chemin)));
    }

    // ------------------------------------------------------------------ G-d préremplissage

    @Test
    void gD_proposition_200_preremplieDepuisLaPiece_resteAPayer() throws Exception {
        UUID existant = tresorerie.reglement(Reglement.debit(s.code("-RX"), s.c(), "400"));
        tresorerie.ligne(existant, pa, "400");
        LocalDate aujourdhui = LocalDate.now(horloge);

        Reponse r = proposition(qui, pa);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(champ(r, "$.module")).as("module de la pièce").isEqualTo(AppelsTresorerie.PRJ);
        soft.assertThat(champ(r, "$.societeId")).as("société de la pièce").isEqualTo(s.c().societe().toString());
        soft.assertThat(champ(r, "$.fournisseurId")).as("fournisseur de la pièce").isEqualTo(s.c().fournisseur().toString());
        soft.assertThat(champ(r, "$.payeurId")).as("payeur = fournisseur").isEqualTo(s.c().fournisseur().toString());
        soft.assertThat(montant(r, "$.montant")).as("montant = reste 1000 − 400").isEqualByComparingTo("600");
        soft.assertThat((List<?>) champ(r, "$.lignes")).as("une seule ligne").hasSize(1);
        soft.assertThat(champ(r, "$.lignes[0].pieceId")).isEqualTo(pa.toString());
        soft.assertThat(montant(r, "$.lignes[0].montant")).as("ligne = reste").isEqualByComparingTo("600");
        soft.assertThat(champ(r, "$.dateDocument")).as("date pièce = aujourd'hui").isEqualTo(aujourdhui.toString());
        soft.assertThat(champ(r, "$.dateEcheance")).as("échéance = date pièce").isEqualTo(aujourdhui.toString());
        soft.assertThat(champ(r, "$.statut")).as("EN COURS").isEqualTo(AppelsSaisieDecaissement.IN_WORK);
        soft.assertThat(champ(r, "$.typeId")).as("type vide").isNull();
        soft.assertThat(champ(r, "$.compteId")).as("compte vide").isNull();
        soft.assertThat((String) champ(r, "$.reference")).as("référence vide").isNullOrEmpty();
        soft.assertThat(champ(r, "$.dateDecaissement")).as("pointage vide").isNull();
        soft.assertAll();
    }

    @Test
    void gD_proposition_moduleBuy_estCeluiDeLaPiece() throws Exception {
        UUID pBuy = fixture.piece(s, "-FBUY", s.c().projet(), s.c().fournisseur(), s.c().moduleBuy(), "300");

        Reponse r = proposition(qui, pBuy);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(champ(r, "$.module")).isEqualTo(AppelsTresorerie.BUY);
    }

    @Test
    void gD_proposition_nEnregistreRien() throws Exception {
        int avant = fixture.nombre(REGLEMENT, TIERS, s.c().fournisseur());

        Reponse r = proposition(qui, pa);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.nombre(REGLEMENT, TIERS, s.c().fournisseur())).as("aucun StlSettlement créé").isEqualTo(avant);
    }

    // ------------------------------------------------------------------ G-c pièce soldée : pas de refus

    @Test
    void gC_pieceSoldee_200_ligneAZero() throws Exception {
        UUID soldee = fixture.piece(s, "-FS", s.c().projet(), "500");
        UUID existant = tresorerie.reglement(Reglement.debit(s.code("-RS"), s.c(), "500"));
        tresorerie.ligne(existant, soldee, "500");

        Reponse r = proposition(qui, soldee);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(montant(r, "$.lignes[0].montant")).as("ligne = reste ≤ 0").isLessThanOrEqualTo(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ G-a pièce non validée

    @Test
    void gA_pieceNonValidee_400_messageOmran() throws Exception {
        UUID nonValidee = fixture.pieceNonValidee(s, "-FNV", s.c().projet(), "1000");

        Reponse r = proposition(qui, nonValidee);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).isEqualTo(MSG_NON_VALIDEE);
        Reponse temoin = proposition(qui, pa);
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ §3 droits et périmètre

    @Test
    void droit_sansToolNew_403() throws Exception {
        RequestPostProcessor lecteur = connecte(fixture.compte(s, "DPRL", FixtureDecaissements.DROIT_LIRE),
                FixtureTresorerie.ROLE);

        Reponse r = proposition(lecteur, pa);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        Reponse temoin = proposition(qui, pa);
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
    }

    @Test
    void pieceInconnue_404() throws Exception {
        Reponse r = proposition(qui, UUID.randomUUID());

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        Reponse temoin = proposition(qui, pa);
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
    }

    @Test
    void pieceHorsPerimetreAchat_404() throws Exception {
        UUID invisible = fixture.piece(s, "-FINV", s.c().projetNonHabilite(), "1000");

        Reponse r = proposition(qui, invisible);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        Reponse temoin = proposition(qui, pa);
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
    }
}
