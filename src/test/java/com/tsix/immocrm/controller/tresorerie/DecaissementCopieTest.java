package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import jakarta.persistence.EntityManager;
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
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-b1-reglements.md §3, C-a, C-b (réponse Q4 du manager : statut et pointage gardés, aucune ligne), C-c :
 * {@code GET /decaissements/{id}/copie} rend un {@code SaisieDecaissementDto} prérempli, sans rien enregistrer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementCopieTest {

    private static final String REGLEMENT = "StlSettlement";
    private static final String TIERS = "TierId";
    private static final String NOTES = "Notes du règlement copié";
    private static final LocalDate POINTAGE = LocalDate.of(2026, 7, 15);
    private static final Configuration TOLERANTE =
            Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;
    @Autowired private FixtureAchat achat;
    @Autowired private EntityManager em;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private RequestPostProcessor qui;
    private UUID payeur;
    private UUID id;

    @BeforeEach
    void setUp() throws Exception {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DCP");
        qui = connecte(fixture.compte(s, "DCPA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU),
                FixtureTresorerie.ROLE);
        UUID pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
        payeur = achat.fournisseur("PAY-" + s.c().jeton(), "Payeur " + s.c().jeton());
        Map<String, Object> corps = corps(s, s.code("-R1"), "800", List.of(ligne(pa, "300")));
        corps.put("statut", AppelsSaisieDecaissement.POINTED);
        corps.put("dateDecaissement", POINTAGE);
        corps.put("notes", NOTES);
        corps.put("payeurId", payeur);
        id = appels.creerOk(qui, corps);
        achat.poserSur(REGLEMENT, id, "ReceiptCode", "REC-" + s.c().jeton());
        achat.poserSur(REGLEMENT, id, "PropositionSheetCode", "PP-" + s.c().jeton());
        achat.poserSur(REGLEMENT, id, "RSSheetCode", "CRAS-" + s.c().jeton());
        em.clear();
    }

    private Reponse copie(RequestPostProcessor compte, UUID reglement) throws Exception {
        return appels.lire(compte, AppelsSaisieDecaissement.DECAISSEMENTS + "/" + reglement + "/copie");
    }

    private static Object champ(Reponse r, String chemin) {
        return JsonPath.using(TOLERANTE).parse(r.corps()).read(chemin);
    }

    private static BigDecimal montant(Reponse r, String chemin) {
        return new BigDecimal(String.valueOf(champ(r, chemin)));
    }

    // ------------------------------------------------------------------ C-a / C-b champs vidés

    @Test
    void cA_copie_200_referenceEtCodesVides_aucuneLigne() throws Exception {
        Reponse r = copie(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat((String) champ(r, "$.reference")).as("référence (Code) vidée").isNullOrEmpty();
        soft.assertThat((String) champ(r, "$.codeRecu")).as("ReceiptCode vidé").isNullOrEmpty();
        soft.assertThat((String) champ(r, "$.codeFeuillePaiement")).as("code P.P vidé").isNullOrEmpty();
        soft.assertThat((String) champ(r, "$.codeFeuilleRs")).as("code C.R.A.S vidé").isNullOrEmpty();
        soft.assertThat((List<?>) champ(r, "$.lignes")).as("lignes non copiées (Q4)").isEmpty();
        soft.assertAll();
    }

    // ------------------------------------------------------------------ C-b champs gardés (Q4 : comme OMRAN)

    @Test
    void cB_copie_gardeStatutPointageEtChampsScalaires() throws Exception {
        Reponse r = copie(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(champ(r, "$.statut")).as("statut gardé (Q4)").isEqualTo(AppelsSaisieDecaissement.POINTED);
        soft.assertThat(champ(r, "$.dateDecaissement")).as("pointage gardé (Q4)").isEqualTo(POINTAGE.toString());
        soft.assertThat(champ(r, "$.module")).isEqualTo(AppelsTresorerie.PRJ);
        soft.assertThat(champ(r, "$.societeId")).isEqualTo(s.c().societe().toString());
        soft.assertThat(champ(r, "$.typeId")).isEqualTo(s.c().type().toString());
        soft.assertThat(champ(r, "$.compteId")).isEqualTo(s.compte().toString());
        soft.assertThat(montant(r, "$.montant")).isEqualByComparingTo("800");
        soft.assertThat(champ(r, "$.fournisseurId")).isEqualTo(s.c().fournisseur().toString());
        soft.assertThat(champ(r, "$.payeurId")).isEqualTo(payeur.toString());
        soft.assertThat(champ(r, "$.notes")).isEqualTo(NOTES);
        soft.assertThat(champ(r, "$.dateDocument")).isEqualTo(AppelsSaisieDecaissement.DATE_PIECE.toString());
        soft.assertThat(champ(r, "$.dateEcheance")).isEqualTo(AppelsSaisieDecaissement.ECHEANCE.toString());
        soft.assertAll();
    }

    @Test
    void cA_copie_nEnregistreRien() throws Exception {
        int avant = fixture.nombre(REGLEMENT, TIERS, s.c().fournisseur());

        Reponse r = copie(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.nombre(REGLEMENT, TIERS, s.c().fournisseur())).as("aucun StlSettlement créé").isEqualTo(avant);
    }

    // ------------------------------------------------------------------ C-c droit toolNew, virement interne

    @Test
    void cC_sansToolNew_403() throws Exception {
        RequestPostProcessor lecteur = connecte(fixture.compte(s, "DCPL", FixtureDecaissements.DROIT_LIRE),
                FixtureTresorerie.ROLE);

        Reponse r = copie(lecteur, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        Reponse temoin = copie(qui, id);
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
    }

    @Test
    void cC_virementInterne_copiePermise_200() throws Exception {
        fixture.lierVirementInterne(id);

        Reponse r = copie(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ §3 404 hors périmètre

    @Test
    void reglementDUneSocieteNonHabilitee_404() throws Exception {
        UUID autre = tresorerie.reglement(Reglement.debit(s.code("-RS"), s.c(), "300").societe(s.autreSociete()));

        Reponse r = copie(qui, autre);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        Reponse temoin = copie(qui, id);
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
    }

    @Test
    void reglementInconnu_404() throws Exception {
        Reponse r = copie(qui, UUID.randomUUID());

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        Reponse temoin = copie(qui, id);
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
    }
}
