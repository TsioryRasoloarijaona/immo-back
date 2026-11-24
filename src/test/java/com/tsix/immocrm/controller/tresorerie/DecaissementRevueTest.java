package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.detail;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md, revue : pièce d'une autre société ou d'un autre module refusée, règlement annulé
 * non réactivable au PUT, payeur conservé quand il est omis au PUT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementRevueTest {

    private static final String STATUT = "statut";
    private static final String CANCELED = "CANCELED";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;
    @Autowired private FixtureAchat achat;
    @Autowired private EntityManager em;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private AdmUser utilisateur;
    private RequestPostProcessor qui;
    private UUID pa;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DRV");
        utilisateur = fixture.compte(s, "DRVA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU);
        qui = connecte(utilisateur, FixtureTresorerie.ROLE);
        pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
    }

    private Map<String, Object> origine() {
        return corps(s, s.code("-R"), "500", List.of(ligne(pa, "300")));
    }

    private Object enBase(UUID id, String colonne) {
        return fixture.reglementEnBase(id).get(0).get(colonne);
    }

    private UUID annule() throws Exception {
        UUID id = appels.creerOk(qui, origine());
        achat.poserSur("StlSettlement", id, "Status", CANCELED);
        em.clear();
        return id;
    }

    @Test
    void post_pieceDUneAutreSocieteVisible_400() throws Exception {
        UUID projet = achat.projet(s.c().jeton() + "-PS", s.autreSociete());
        achat.habiliterSociete(utilisateur, s.autreSociete());
        achat.habiliter(utilisateur, projet);
        UUID piece = tresorerie.piece(s.code("-FS2"), s.autreSociete(), projet, s.c().fournisseur(),
                s.c().fournisseurNom(), s.c().modulePrj(), "1000");
        achat.poser(piece, "StlCurrencyId", s.devise());
        achat.poser(piece, "BuyDocumentStatusId", achat.statut("ST" + s.code("-FS2"), true, false, 1));

        Reponse r = appels.creer(qui, corps(s, s.code("-R"), "500", List.of(ligne(piece, "300"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).contains("société du règlement");
    }

    @Test
    void post_pieceDUnAutreModule_400() throws Exception {
        UUID piece = fixture.piece(s, "-FB", s.c().projet(), s.c().fournisseur(), s.c().moduleBuy(), "1000");

        Reponse r = appels.creer(qui, corps(s, s.code("-R"), "500", List.of(ligne(piece, "300"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).contains("module du règlement");
    }

    @Test
    void put_reglementAnnule_statutOmis_resteAnnule() throws Exception {
        UUID id = annule();
        Map<String, Object> corps = origine();
        corps.remove(STATUT);

        Reponse r = appels.modifier(qui, id, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(enBase(id, STATUT)).isEqualTo(CANCELED);
    }

    @Test
    void put_reglementAnnule_canceledRenvoye_200() throws Exception {
        UUID id = annule();
        Map<String, Object> corps = origine();
        corps.put(STATUT, CANCELED);

        Reponse r = appels.modifier(qui, id, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(enBase(id, STATUT)).isEqualTo(CANCELED);
    }

    @Test
    void put_reglementAnnule_reactivation_400() throws Exception {
        UUID id = annule();

        Reponse r = appels.modifier(qui, id, origine());

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(enBase(id, STATUT)).isEqualTo(CANCELED);
    }

    @Test
    void post_canceled_400() throws Exception {
        Map<String, Object> corps = origine();
        corps.put(STATUT, CANCELED);

        Reponse r = appels.creer(qui, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void put_payeurOmis_payeurExistantConserve() throws Exception {
        UUID payeur = achat.fournisseur("PAY-" + s.c().jeton(), "Payeur " + s.c().jeton());
        Map<String, Object> creation = origine();
        creation.put("payeurId", payeur);
        UUID id = appels.creerOk(qui, creation);

        Reponse r = appels.modifier(qui, id, origine());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(enBase(id, "payeur").toString()).isEqualToIgnoringCase(payeur.toString());
    }
}
