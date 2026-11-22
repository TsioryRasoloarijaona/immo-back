package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.DATE_PIECE;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ECHEANCE;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.detail;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md : suppression d'un décaissement ({@code DELETE /decaissements/{id}}), refus R-m
 * (pointé), R-n (affectations), R-o (référencé), virement interne ; historique supprimé avec le règlement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementSuppressionTest {

    private static final String REGLEMENT = "StlSettlement";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DSU");
        qui = connecte(fixture.compte(s, "DSUA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU,
                FixtureDecaissements.DROIT_SUPPRIMER, FixtureDecaissements.DROIT_SANS_AFFECTATION), FixtureTresorerie.ROLE);
    }

    private UUID reglementSansLigne(String suffixe) {
        return tresorerie.reglement(Reglement.debit(s.code(suffixe), s.c(), "500"));
    }

    @Test
    void delete_204_reglementCreeSansLigne_supprimeReglementEtHistorique() throws Exception {
        UUID id = appels.creerOk(qui, corps(s, s.code("-R1"), "500", List.of()));

        Reponse r = appels.supprimer(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(fixture.nombre(REGLEMENT, "PKey", id)).as("StlSettlement").isZero();
            soft.assertThat(fixture.nombre("StlSettlementHistory", "StlSettlementId", id)).as("StlSettlementHistory")
                    .isZero();
        });
    }

    @Test
    void delete_204_temoin_reglementSansLigneNonPointe() throws Exception {
        UUID id = reglementSansLigne("-RT");

        Reponse r = appels.supprimer(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(fixture.nombre(REGLEMENT, "PKey", id)).isZero();
    }

    @Test
    void rm_reglementPointe_409() throws Exception {
        UUID id = tresorerie.reglement(Reglement.debit(s.code("-RP"), s.c(), "500").statut(FixtureTresorerie.POINTE)
                .dates(DATE_PIECE, ECHEANCE, LocalDate.of(2026, 5, 4)));

        Reponse r = appels.supprimer(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("Vous ne pouvez pas supprimer un règlement pointé en comptabilité !");
        assertThat(fixture.nombre(REGLEMENT, "PKey", id)).as("règlement conservé").isEqualTo(1);
    }

    @Test
    void rn_reglementAvecAffectation_409() throws Exception {
        UUID piece = fixture.piece(s, "-FA", s.c().projet(), "1000");
        UUID id = reglementSansLigne("-RN");
        tresorerie.ligne(id, piece, "500");

        Reponse r = appels.supprimer(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("Vous ne pouvez pas supprimer un règlement avec des affectations !");
        assertThat(fixture.nombre("StlSettlementLine", "StlSettlementId", id)).as("ligne conservée").isEqualTo(1);
    }

    @Test
    void ro_reglementReferenceParEcritureComptable_409() throws Exception {
        UUID id = reglementSansLigne("-RO");
        fixture.ecritureComptable(id);

        Reponse r = appels.supprimer(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).startsWith("Ce règlement est référencé par");
    }

    @Test
    void suppression_virementInterne_409() throws Exception {
        UUID id = reglementSansLigne("-RV");
        fixture.lierVirementInterne(id);

        Reponse r = appels.supprimer(qui, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(fixture.nombre(REGLEMENT, "PKey", id)).as("règlement conservé").isEqualTo(1);
    }
}
