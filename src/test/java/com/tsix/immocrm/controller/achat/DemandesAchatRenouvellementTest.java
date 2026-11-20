package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6b R19 (renouvellement d'une DIA annulée, {@code New(BuySelection)} l.2064) et R7 sur {@code POST /renouvellement}
 * (FEATURE-n6-demandes-achat.md ; Q-b5, Q-b7 : statut de type Annulation posé par la fixture, groupes à 2 niveaux).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatRenouvellementTest {

    private static final String RENOUVELLEMENT = "/renouvellement";
    private static final String HORS_ANNULATION = "Pour pouvoir générer un nouvel appel d'offre il faut d'abords annuler l'actuel !";
    private static final String DU_GROUPE = "SELECT * FROM BuySelection WHERE GroupId = ? AND PKey <> ?";
    private static final String GROUPES = "SELECT * FROM BuySelectionGroup WHERE BuySelectionId = ? ORDER BY Sorting ASC";
    private static final String LIGNES = "SELECT * FROM BuySelectionLine WHERE BuySelectionId = ? ORDER BY Sorting ASC";
    private static final String MODALITES = "SELECT * FROM BuySelectionPaymentTerm WHERE BuySelectionId = ?";
    private static final String FOURNISSEURS = "SELECT * FROM BuySelectionSupplier WHERE BuySelectionId = ?";
    private static final String PKEY = "PKey";
    private static final LocalDate DEPOT = LocalDate.of(2026, 3, 20);

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
        c = b.cadre("RN");
        qLibre = qui(b.libre(c, "n6b.rn.libre." + c.jeton()));
    }

    private UUID dia(String suffixe, UUID statut) {
        return demandes.demande(c, c.code(suffixe), statut);
    }

    private static String maj(Object valeur) {
        return valeur == null ? null : valeur.toString().toUpperCase();
    }

    private UUID tiers(String suffixe) {
        return achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
    }

    @Test
    void r19_statutHorsAnnulation_409_message_aucuneCopie() throws Exception {
        UUID enCours = dia("R19A", c.depouil());
        UUID annulee = dia("R19B", c.annul());

        Reponse refus = appels.poster(qLibre, enCours, RENOUVELLEMENT, corps());
        int copiesRefus = b.lignesApresRefus(DU_GROUPE, enCours.toString(), enCours.toString()).size();
        Reponse temoin = appels.poster(qLibre, annulee, RENOUVELLEMENT, corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.corps()).contains(HORS_ANNULATION);
            s.assertThat(copiesRefus).isZero();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void r19_horsAnnulation_messageDansLeDetail() throws Exception {
        UUID enCours = dia("R19M", c.prep());

        Reponse refus = appels.poster(qLibre, enCours, RENOUVELLEMENT, corps());

        assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
        assertThat(refus.valeur(DETAIL)).isEqualTo(HORS_ANNULATION);
    }

    @Test
    void r19_nouvelleDia_memeGroupe_derniere_statutDeDepart_ancienneNonDerniere() throws Exception {
        UUID origine = dia("R19C", c.annul());
        demandes.poser(DEMANDE, origine, "Description", "Appel " + c.jeton());

        Reponse r = appels.poster(qLibre, origine, RENOUVELLEMENT, corps());

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        List<Map<String, Object>> copies = b.lignes(DU_GROUPE, origine.toString(), origine.toString());
        assertThat(copies).hasSize(1);
        Map<String, Object> copie = copies.getFirst();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(copie).containsEntry("IsTheLast", true).containsEntry("Description", "Appel " + c.jeton());
            s.assertThat(maj(copie.get("StrStatusId"))).isEqualTo(maj(c.prep()));
            s.assertThat(maj(copie.get("StrEntityId"))).isEqualTo(maj(c.entite()));
            s.assertThat(maj(copie.get("BuySelectionKindId"))).isEqualTo(maj(c.procedure()));
            s.assertThat(maj(copie.get("CfgCompanyId"))).isEqualTo(maj(c.societe()));
            s.assertThat(maj(copie.get("PrjProjectId"))).isEqualTo(maj(c.projet()));
            s.assertThat(demandes.demandeEnBase(origine)).containsEntry("IsTheLast", false);
        });
    }

    @Test
    void r19_copieLeBordereau_parentsDesGroupesRemappes_lignesDansLesNouveauxGroupes() throws Exception {
        UUID origine = dia("R19D", c.annul());
        UUID categorie = achat.categorie("CATR19-" + c.jeton());
        UUID g1 = b.groupe(origine, "G1", null, 1);
        UUID g2 = b.groupe(origine, "G2", g1, 2);
        b.ligne(origine, g1, 1, "A1", "10", categorie);
        b.ligne(origine, g2, 2, "A2", "20", categorie);

        Reponse r = appels.poster(qLibre, origine, RENOUVELLEMENT, corps());

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        Object copie = b.lignes(DU_GROUPE, origine.toString(), origine.toString()).getFirst().get(PKEY);
        List<Map<String, Object>> groupes = b.lignes(GROUPES, copie.toString());
        List<Map<String, Object>> lignes = b.lignes(LIGNES, copie.toString());
        assertThat(groupes).extracting(g -> g.get("Code")).containsExactly("G1", "G2");
        assertThat(lignes).extracting(l -> l.get("Code")).containsExactly("A1", "A2");
        Object nouveauG1 = groupes.get(0).get(PKEY);
        Object nouveauG2 = groupes.get(1).get(PKEY);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(maj(nouveauG1)).isNotEqualTo(maj(g1));
            s.assertThat(groupes.get(0).get("ParentId")).isNull();
            s.assertThat(maj(groupes.get(1).get("ParentId"))).isEqualTo(maj(nouveauG1));
            s.assertThat(maj(lignes.get(0).get("BuySelectionGroupId"))).isEqualTo(maj(nouveauG1));
            s.assertThat(maj(lignes.get(1).get("BuySelectionGroupId"))).isEqualTo(maj(nouveauG2));
            s.assertThat(maj(lignes.get(1).get("StlCategoryId"))).isEqualTo(maj(categorie));
            s.assertThat((BigDecimal) lignes.get(1).get("Quantity")).isEqualByComparingTo("2");
        });
    }

    @Test
    void r19_copieLesModalites() throws Exception {
        UUID origine = dia("R19E", c.annul());
        b.modalite(origine, 1, "AVANCE", "0.3");

        Reponse r = appels.poster(qLibre, origine, RENOUVELLEMENT, corps());

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        Object copie = b.lignes(DU_GROUPE, origine.toString(), origine.toString()).getFirst().get(PKEY);
        assertThat(b.lignes(MODALITES, copie.toString())).singleElement().satisfies(m -> {
            assertThat(m).containsEntry("Code", "AVANCE").containsEntry("Sorting", 1);
            assertThat((BigDecimal) m.get("Ratio")).isEqualByComparingTo("0.3");
        });
    }

    @Test
    void r19_copieLesFournisseursNonEcartesAyantDepose_datesEtReferenceVidees() throws Exception {
        UUID origine = dia("R19F", c.annul());
        UUID depose = tiers("R19FA");
        UUID ecarte = tiers("R19FB");
        UUID sansDepot = tiers("R19FC");
        b.preselection(origine, depose, DEPOT);
        b.ecarter(b.preselection(origine, ecarte, DEPOT));
        b.preselection(origine, sansDepot, null);

        Reponse r = appels.poster(qLibre, origine, RENOUVELLEMENT, corps());

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        Object copie = b.lignes(DU_GROUPE, origine.toString(), origine.toString()).getFirst().get(PKEY);
        assertThat(b.lignes(FOURNISSEURS, copie.toString())).singleElement().satisfies(f -> {
            assertThat(maj(f.get("CfgTierId"))).isEqualTo(maj(depose));
            assertThat((String) f.get("OfficeOrderRef")).isNullOrEmpty();
            assertThat(f.get("TenderDepositDate")).isNull();
            assertThat(f.get("TenderAcquisitionDate")).isNull();
            assertThat(f.get("SelectionLetterSendDate")).isNull();
        });
    }

    @Test
    void r7_demandeNonDerniere_renouvellement_409() throws Exception {
        UUID ancienne = dia("R19G", c.annul());
        demandes.poser(DEMANDE, ancienne, "IsTheLast", false);
        UUID derniere = dia("R19H", c.annul());

        Reponse refus = appels.poster(qLibre, ancienne, RENOUVELLEMENT, corps());
        int copies = b.lignesApresRefus(DU_GROUPE, ancienne.toString(), ancienne.toString()).size();
        Reponse temoin = appels.poster(qLibre, derniere, RENOUVELLEMENT, corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(copies).isZero();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }
}
