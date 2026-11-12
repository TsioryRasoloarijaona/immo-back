package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.DOCUMENT_TO_PAY;
import static com.tsix.immocrm.fixtures.FixtureLotSousFenetres.MARKET_ADVANCE;

/**
 * Règle 1 de FEATURE-lot-sous-fenetres.md §3 : chaque sous-fenêtre appelle d'abord la visibilité du lot. Lot inconnu, sans
 * projet, projet hors périmètre ou statut invisible → 404 « Lot introuvable », avant toute lecture ; le témoin visible répond 200
 * (une route absente répond aussi 404). Le lot hors périmètre porte une avance : le 404 passe avant les données.
 * <pre>
 *   projet A (société SA) : lot LA (statut S1 ← rôle RU), lot LC (statut S2, aucun intervenant du compte)
 *   projet B : lot LB          lot LS sans projet          compte « u » habilité sur A et SA, rôle restreint RU
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsSousFenetresVisibiliteTest {

    private static final String LOT_INTROUVABLE = "Lot introuvable";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureVisibiliteOmran visibilite;
    @Autowired private FixtureLotSousFenetres sf;

    private AppelsProjet appels;
    private UUID lotVisible;
    private UUID lotHorsPerimetre;
    private UUID lotSansProjet;
    private UUID lotStatutCache;
    private RequestPostProcessor u;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        String jeton = FixtureProjetLots.jeton();
        sf.tables();
        UUID fournisseur = fixture.fournisseur("SVF-" + jeton);
        UUID projetA = fixture.projet("SVA-" + jeton);
        UUID projetB = fixture.projet("SVB-" + jeton);
        String roleU = "SVRU-" + jeton;
        AdmUser compteU = visibilite.compte("sf.vis.u." + jeton, roleU, true);
        UUID entite = visibilite.entite("SVLOT-" + jeton);
        UUID s1 = visibilite.statut(entite, "SVS1-" + jeton);
        UUID s2 = visibilite.statut(entite, "SVS2-" + jeton);
        visibilite.intervenantRole(entite, s1, visibilite.idRole(roleU));
        visibilite.intervenantRole(entite, s2, visibilite.role("SVRX-" + jeton));
        lotVisible = fixture.lot(projetA, "SVLA-" + jeton, fournisseur);
        lotStatutCache = fixture.lot(projetA, "SVLC-" + jeton, fournisseur);
        lotHorsPerimetre = fixture.lot(projetB, "SVLB-" + jeton, fournisseur);
        lotSansProjet = fixture.lot(null, "SVLS-" + jeton, fournisseur);
        visibilite.statutLot(lotVisible, entite, s1);
        visibilite.statutLot(lotStatutCache, entite, s2);
        visibilite.statutLot(lotHorsPerimetre, entite, s1);
        visibilite.statutLot(lotSansProjet, entite, s1);
        fixture.habiliter(compteU, projetA);
        sf.piece(lotHorsPerimetre, projetB, null, "SVAV-" + jeton, DOCUMENT_TO_PAY, MARKET_ADVANCE, LocalDate.of(2026, 3, 5),
                "100");
        u = connecte(compteU, roleU);
    }

    private Reponse lire(UUID lot, String route) throws Exception {
        return appels.lire(u, LOTS + "/" + lot + "/" + route);
    }

    @ParameterizedTest(name = "r1 {0} : inconnu, hors périmètre, sans projet, statut invisible → 404 ; témoin 200")
    @ValueSource(strings = {"avances", "avoirs", "avenants", "remboursements", "bordereau-estimatif", "previsions"})
    void r1_sousFenetre_lotInvisible_404LotIntrouvable_temoin200(String route) throws Exception {
        Reponse temoin = lire(lotVisible, route);
        Reponse inconnu = lire(UUID.randomUUID(), route);
        Reponse horsPerimetre = lire(lotHorsPerimetre, route);
        Reponse sansProjet = lire(lotSansProjet, route);
        Reponse statutCache = lire(lotStatutCache, route);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin visible : " + temoin.corps()).isEqualTo(200);
            refus(s, "lot inconnu", inconnu);
            refus(s, "projet hors périmètre", horsPerimetre);
            refus(s, "lot sans projet", sansProjet);
            refus(s, "statut invisible", statutCache);
        });
    }

    private static void refus(SoftAssertions s, String cas, Reponse r) {
        s.assertThat(r.statut()).as(cas).isEqualTo(404);
        s.assertThat(r.corps()).as(cas).contains(LOT_INTROUVABLE);
    }
}
