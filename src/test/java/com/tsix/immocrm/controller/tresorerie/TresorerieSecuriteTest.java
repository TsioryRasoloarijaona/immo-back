package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Cadre;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.AFFECTATIONS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.CREDIT;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DEBIT;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DECAISSEMENTS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.MODULE;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.PRJ;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.RECETTES;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.SENS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R7 / D7 (FEATURE-n3-recettes-depenses.md, « Réponses du manager » Q1) : une clé {@code Yes.Stl.Front.*} par écran, 403
 * sans elle ; la clé d'un autre écran n'ouvre pas celui-ci. R4 : {@code module} absent ou hors PRJ/BUY → 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieSecuriteTest {

    private static final String FACTURES = RECETTES + "factures";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerie fixture;

    private AppelsTresorerie appels;
    private Cadre c;
    private UUID ra;

    @BeforeEach
    void setUp() {
        appels = new AppelsTresorerie(mockMvc);
        c = fixture.cadre("TRS");
        UUID fa = fixture.piece(c.code("-FA"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "1000");
        ra = fixture.reglement(Reglement.debit(c.code("-RA"), c, "300"));
        fixture.ligne(ra, fa, "300");
    }

    private RequestPostProcessor avec(String code, String... droits) {
        return connecte(fixture.compte(c, code, droits), FixtureTresorerie.ROLE);
    }

    private static String[] parametres(String sens) {
        return sens == null || sens.isBlank() ? new String[]{MODULE, PRJ} : new String[]{MODULE, PRJ, SENS, sens};
    }

    // ------------------------------------------------------------------ R7 : 403 sans le droit

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "/api/tresorerie/decaissements, ''",
            "/api/tresorerie/affectations, DEBIT",
            "/api/tresorerie/affectations, CREDIT",
            "/api/tresorerie/recettes/proforma, ''",
            "/api/tresorerie/recettes/factures, ''",
            "/api/tresorerie/recettes/avoirs, ''",
            "/api/tresorerie/recettes/encaissements, ''"
    })
    void r7_sansAucunDroit_403(String url, String sens) throws Exception {
        Reponse r = appels.lire(avec("TRS0"), url, parametres(sens));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void r7_ficheDecaissementVisible_sansDroit_403() throws Exception {
        Reponse r = appels.lire(avec("TRS0F"), DECAISSEMENTS + "/" + ra);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @ParameterizedTest(name = "{0} {1} ouvert par {2}")
    @CsvSource({
            "/api/tresorerie/decaissements, '', Yes.Stl.Front.Debit.SettlementList",
            "/api/tresorerie/affectations, DEBIT, Yes.Stl.Front.Debit.SettlementAffectationList",
            "/api/tresorerie/affectations, CREDIT, Yes.Stl.Front.Credit.SettlementAffectationList",
            "/api/tresorerie/recettes/proforma, '', Yes.Stl.Front.Credit.InvoiceProformaList",
            "/api/tresorerie/recettes/factures, '', Yes.Stl.Front.Credit.InvoiceList",
            "/api/tresorerie/recettes/avoirs, '', Yes.Stl.Front.Credit.RefundList",
            "/api/tresorerie/recettes/encaissements, '', Yes.Stl.Front.Credit.SettlementList"
    })
    void r7_cleTresorerieDeLEcran_ouvreLEcran(String url, String sens, String droit) throws Exception {
        Reponse r = appels.lire(avec("TRS1", droit), url, parametres(sens));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    @Test
    void r7_droitDecaissementsSeul_affectationsRefusees() throws Exception {
        RequestPostProcessor qui = avec("TRS2", FixtureTresorerie.DROIT_DECAISSEMENTS);

        Reponse temoin = appels.lire(qui, DECAISSEMENTS, MODULE, PRJ);
        Reponse refus = appels.lire(qui, AFFECTATIONS, MODULE, PRJ, SENS, DEBIT);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin : %s", temoin.corps()).isEqualTo(200);
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        });
    }

    @Test
    void r7_affectationsDebitSeul_affectationsCreditRefusees() throws Exception {
        RequestPostProcessor qui = avec("TRS3", FixtureTresorerie.DROIT_AFFECTATIONS);

        Reponse temoin = appels.lire(qui, AFFECTATIONS, MODULE, PRJ, SENS, DEBIT);
        Reponse refus = appels.lire(qui, AFFECTATIONS, MODULE, PRJ, SENS, CREDIT);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin : %s", temoin.corps()).isEqualTo(200);
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        });
    }

    @Test
    void r7_facturesDeRecetteSeul_avoirsRefuses() throws Exception {
        RequestPostProcessor qui = avec("TRS4", FixtureTresorerie.DROIT_FACTURES);

        Reponse temoin = appels.lire(qui, FACTURES, MODULE, PRJ);
        Reponse refus = appels.lire(qui, RECETTES + "avoirs", MODULE, PRJ);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin : %s", temoin.corps()).isEqualTo(200);
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        });
    }

    // ------------------------------------------------------------------ R4 : module obligatoire, PRJ | BUY

    @ParameterizedTest(name = "{0} {1} module={2}")
    @CsvSource({
            "/api/tresorerie/decaissements, '', ''",
            "/api/tresorerie/decaissements, '', STL",
            "/api/tresorerie/affectations, DEBIT, ''",
            "/api/tresorerie/affectations, DEBIT, COM",
            "/api/tresorerie/recettes/factures, '', ''",
            "/api/tresorerie/recettes/factures, '', XYZ"
    })
    void r4_moduleAbsentOuHorsPrjBuy_400(String url, String sens, String module) throws Exception {
        RequestPostProcessor qui = avec("TRS5", FixtureTresorerie.DROIT_DECAISSEMENTS,
                FixtureTresorerie.DROIT_AFFECTATIONS, FixtureTresorerie.DROIT_FACTURES);
        String[] params = module.isBlank() ? new String[0] : new String[]{MODULE, module};
        if (!sens.isBlank()) {
            params = module.isBlank() ? new String[]{SENS, sens} : new String[]{MODULE, module, SENS, sens};
        }

        Reponse r = appels.lire(qui, url, params);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }
}
