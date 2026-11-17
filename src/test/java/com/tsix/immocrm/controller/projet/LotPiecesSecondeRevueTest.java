package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixturePiecesLot;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PiecesLotSupport.AVANCES;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.DROIT_AVANCE;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.idCree;
import static com.tsix.immocrm.controller.projet.PiecesLotSupport.saisie;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.DATE_PIECE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.MARKET_ADVANCE;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.WORKINGOUT_ADVANCE;
import static org.assertj.core.api.Assertions.assertThat;

/** Seconde revue du lot L3b-2b (FEATURE-lot-pieces-a-payer.md) : R8 bis à la modification, société habilitée sur les écritures. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPiecesSecondeRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private FixtureLotsParite parite;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private PiecesLotSupport appels;
    private Cadre c;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        c = pf.cadre("PS");
        appels = new PiecesLotSupport(mockMvc);
        admin = connecte(pf.administrateur(c, "ps.admin"), "Administrateur");
    }

    private static BigDecimal reste(java.util.Map<String, Object> decompte) {
        return new BigDecimal(decompte.get("reste").toString());
    }

    // R8 bis : modification d'une W.AVA rattachée → reste du décompte recalculé : W.AVA du lot (100 + 600) − autres décomptes (40).
    @Test
    void r8bis_modification_resteDuDecompteRecalcule() throws Exception {
        UUID lot = pf.lot(c, "PS1", c.statutLibre());
        pf.decompte(c, lot, 1, true, null, "40");
        UUID ouvert = pf.decompte(c, lot, 2, false, null, null);
        pf.piece(c, lot, WORKINGOUT_ADVANCE, "PS2", "100", c.enCours());
        Reponse cree = appels.creer(admin, lot, saisie(WORKINGOUT_ADVANCE, DATE_PIECE, null, "250"));
        assertThat(cree.statut()).as(cree.corps()).isEqualTo(201);

        Reponse r = appels.modifier(admin, lot, idCree(cree), saisie(WORKINGOUT_ADVANCE, DATE_PIECE, null, "600"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(reste(pf.decompteEnBase(ouvert))).isEqualByComparingTo("660");
        });
    }

    // Société habilitée : une AVA du lot visible mais d'une société non habilitée du compte → omise de la liste, 404 en lecture,
    // modification et suppression, pièce intacte.
    @Test
    void pieceDUneSocieteNonHabilitee_404_lectureEtEcritures() throws Exception {
        RequestPostProcessor compte = connecte(pf.compte(c, "ps.cpt", List.of(DROIT_AVANCE)), "Commercial");
        UUID lot = pf.lot(c, "PS3", c.statutLibre());
        UUID ava = pf.piece(c, lot, MARKET_ADVANCE, "PS4", "100", c.enCours());
        UUID autreSociete = parite.entiteJuridique("PSE-" + c.jeton());
        em.flush();
        jdbc.update("UPDATE BuyDocument SET CfgCompanyId = ? WHERE PKey = ?", autreSociete.toString(), ava.toString());
        em.clear();

        Reponse liste = appels.lecture(compte, lot, AVANCES);
        Reponse lire = appels.lire(compte, lot, ava);
        Reponse modifier = appels.modifier(compte, lot, ava, saisie(MARKET_ADVANCE, DATE_PIECE, null, "600"));
        Reponse supprimer = appels.supprimer(compte, lot, ava);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
            s.assertThat(liste.corps()).doesNotContain(ava.toString());
            s.assertThat(lire.statut()).as(lire.corps()).isEqualTo(404);
            s.assertThat(modifier.statut()).as(modifier.corps()).isEqualTo(404);
            s.assertThat(supprimer.statut()).as(supprimer.corps()).isEqualTo(404);
            s.assertThat(pf.existe(ava)).isEqualTo(1);
        });
    }
}
