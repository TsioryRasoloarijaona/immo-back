package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.sauf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6b R18 : création d'une DIA depuis un lot ({@code POST /api/projet/lots/{id}/demandes-achat}, {@code New(MarketMonitoring)} l.1962),
 * corps {@code {procedureId, description}} (dialogue « procédure, description » du Brief front) ; FEATURE-n6-demandes-achat.md, Q-b5, Q-b7.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatDepuisLotTest {

    private static final String AUTRES_NON_ANNULEES =
            "Pour pouvoir ajouter un nouvel appel d'offre il faut d'abords annuler tous les autres !";
    private static final String CREEES = "SELECT * FROM BuySelection WHERE PrjMarketId = ? AND Description = ?";
    private static final String GROUPES = "SELECT * FROM BuySelectionGroup WHERE BuySelectionId = ? ORDER BY Sorting ASC";
    private static final String LIGNES = "SELECT * FROM BuySelectionLine WHERE BuySelectionId = ? ORDER BY Sorting ASC";
    private static final String MARCHE = "PrjMarket";
    private static final String PKEY = "PKey";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("DL");
        qLibre = qui(b.libre(c, "n6b.dl.libre." + c.jeton()));
    }

    /** Lot du projet du cadre dont le statut porte « Avec DIA » ({@code BoolField005}). */
    private UUID lot(String suffixe, boolean multi) {
        UUID lot = demandes.lot(c, "L" + suffixe + "-" + c.jeton(), multi);
        b.lotAvecDia(lot, true);
        return lot;
    }

    /** DIA existante rattachée au lot, au statut donné. */
    private void diaDuLot(UUID lot, String suffixe, UUID statut) {
        UUID dia = demandes.demande(c, c.code(suffixe), statut);
        demandes.poser(DEMANDE, dia, "PrjMarketId", lot);
    }

    private String description(String suffixe) {
        return "Depuis lot " + suffixe + " " + c.jeton();
    }

    private CorpsJson saisie(String description) {
        return corps().avec("procedureId", c.procedure()).avec("description", description);
    }

    private List<Map<String, Object>> creees(UUID lot, String description) {
        return b.lignes(CREEES, lot.toString(), description);
    }

    private static String maj(Object valeur) {
        return valeur == null ? null : valeur.toString().toUpperCase();
    }

    // ------------------------------------------------------------------ refus

    @Test
    void r18_lotNonMulti_avecUneDiaNonAnnulee_409_message() throws Exception {
        UUID occupe = lot("OCC", false);
        diaDuLot(occupe, "OCC", c.depouil());
        UUID libere = lot("LIB", false);
        diaDuLot(libere, "LIB", c.annul());

        Reponse refus = appels.creerDepuisLot(qLibre, occupe, saisie(description("OCC")));
        int creeesRefus = b.lignesApresRefus(CREEES, occupe.toString(), description("OCC")).size();
        Reponse temoin = appels.creerDepuisLot(qLibre, libere, saisie(description("LIB")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).contains(AUTRES_NON_ANNULEES);
            s.assertThat(creeesRefus).isZero();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
            s.assertThat(creees(libere, description("LIB"))).hasSize(1);
        });
    }

    @Test
    void r18_lotMulti_avecUneDiaEnCours_cree() throws Exception {
        UUID multi = lot("MUL", true);
        diaDuLot(multi, "MUL", c.depouil());

        Reponse r = appels.creerDepuisLot(qLibre, multi, saisie(description("MUL")));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(creees(multi, description("MUL"))).hasSize(1);
    }

    @Test
    void r18_sansDroitNouveau_403() throws Exception {
        UUID lot = lot("D7", false);
        RequestPostProcessor sansNouveau = qui(demandes.libre(c, "n6b.dl.sansnew." + c.jeton(), sauf(DROIT_NOUVEAU)));

        Reponse refus = appels.creerDepuisLot(sansNouveau, lot, saisie(description("D7R")));
        int creeesRefus = b.lignesApresRefus(CREEES, lot.toString(), description("D7R")).size();
        Reponse temoin = appels.creerDepuisLot(qLibre, lot, saisie(description("D7T")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(creeesRefus).isZero();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void r18_statutDuLotSansAvecDia_refuse_rienNEstCree() throws Exception {
        UUID ferme = lot("FER", false);
        b.lotAvecDia(ferme, false);
        UUID ouvert = lot("OUV", false);

        Reponse refus = appels.creerDepuisLot(qLibre, ferme, saisie(description("FER")));
        int creeesRefus = b.lignesApresRefus(CREEES, ferme.toString(), description("FER")).size();
        Reponse temoin = appels.creerDepuisLot(qLibre, ouvert, saisie(description("OUV")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isIn(403, 409);
            s.assertThat(creeesRefus).isZero();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    // ------------------------------------------------------------------ contenu

    @Test
    void r18_enTete_lot_modulePRJ_statutDeDepart_delaiPaiementEtDatesDuLot() throws Exception {
        UUID lot = lot("ENT", false);
        UUID reglement = achat.typeReglement("REG-" + c.jeton());
        achat.poserSur(MARCHE, lot, "PaymentDelay", 20);
        achat.poserSur(MARCHE, lot, "StlSettlementTypeId", reglement);
        achat.poserSur(MARCHE, lot, "TenderDate", Timestamp.valueOf(LocalDate.of(2026, 4, 15).atStartOfDay()));

        Reponse r = appels.creerDepuisLot(qLibre, lot, saisie(description("ENT")));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        List<Map<String, Object>> creees = creees(lot, description("ENT"));
        assertThat(creees).hasSize(1);
        Map<String, Object> d = creees.getFirst();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(maj(d.get("AdmModuleId"))).isEqualTo(maj(achat.module("PRJ")));
            s.assertThat(maj(d.get("CfgCompanyId"))).isEqualTo(maj(c.societe()));
            s.assertThat(maj(d.get("PrjProjectId"))).isEqualTo(maj(c.projet()));
            s.assertThat(maj(d.get("BuySelectionKindId"))).as("estimé 0 : procédure du corps").isEqualTo(maj(c.procedure()));
            s.assertThat(maj(d.get("StrEntityId"))).isEqualTo(maj(c.entite()));
            s.assertThat(maj(d.get("StrStatusId"))).isEqualTo(maj(c.prep()));
            s.assertThat(maj(d.get("StlSettlementTypeId"))).isEqualTo(maj(reglement));
            s.assertThat(d).containsEntry("PaymentDelay", 20).containsEntry("IsTheLast", true);
            s.assertThat(((Timestamp) d.get("TenderDate")).toLocalDateTime().toLocalDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        });
    }

    @Test
    void r18_copieLeBordereauEstimatif_parentsRemappes_lignesDansLesNouveauxGroupes() throws Exception {
        UUID lot = lot("EST", false);
        UUID categorie = achat.categorie("CATEST-" + c.jeton());
        UUID g1 = b.groupeEstimatif(lot, "E-G1", null, 1);
        UUID g2 = b.groupeEstimatif(lot, "E-G2", g1, 2);
        b.ligneEstimative(lot, g1, 1, "E1", "10", categorie);
        b.ligneEstimative(lot, g2, 2, "E2", "20", categorie);

        Reponse r = appels.creerDepuisLot(qLibre, lot, saisie(description("EST")));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        Object dia = creees(lot, description("EST")).getFirst().get(PKEY);
        List<Map<String, Object>> groupes = b.lignes(GROUPES, dia.toString());
        List<Map<String, Object>> lignes = b.lignes(LIGNES, dia.toString());
        assertThat(groupes).extracting(g -> g.get("Code")).containsExactly("E-G1", "E-G2");
        assertThat(lignes).extracting(l -> l.get("Code")).containsExactly("E1", "E2");
        Object nouveauG1 = groupes.get(0).get(PKEY);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(groupes.get(0).get("ParentId")).isNull();
            s.assertThat(maj(groupes.get(1).get("ParentId"))).isEqualTo(maj(nouveauG1));
            s.assertThat(maj(lignes.get(0).get("BuySelectionGroupId"))).isEqualTo(maj(nouveauG1));
            s.assertThat(maj(lignes.get(1).get("BuySelectionGroupId"))).isEqualTo(maj(groupes.get(1).get(PKEY)));
            s.assertThat(maj(lignes.get(1).get("StlCategoryId"))).isEqualTo(maj(categorie));
            s.assertThat((BigDecimal) lignes.get(1).get("Quantity")).isEqualByComparingTo("3");
        });
    }

    @Test
    void r18_estimeNonNul_procedureChoisieParMontant() throws Exception {
        UUID petite = demandes.procedureVerifiee("P10K-" + c.jeton(), c.entite(), new BigDecimal("10000"));
        UUID grande = demandes.procedureVerifiee("P100K-" + c.jeton(), c.entite(), new BigDecimal("100000"));
        UUID lot = lot("MNT", false);
        achat.poserSur(MARCHE, lot, "EstimatedPriceTTCManual", new BigDecimal("50000"));

        Reponse r = appels.creerDepuisLot(qLibre, lot, corps().avec("procedureId", petite).avec("description", description("MNT")));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(creees(lot, description("MNT"))).singleElement().satisfies(d -> {
            assertThat(maj(d.get("BuySelectionKindId"))).isEqualTo(maj(grande));
            assertThat((BigDecimal) d.get("PriceEstimatedTTC")).isEqualByComparingTo("50000");
        });
    }
}
