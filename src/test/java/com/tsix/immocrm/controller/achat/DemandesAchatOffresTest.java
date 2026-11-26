package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
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
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_CONFIDENTIEL;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.DROIT_AJOUT_OFFRE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.DROIT_SELECTION_OFFRE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.DROIT_SUPPRESSION_OFFRE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.sauf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6b R16 (offres, sélection, désélection), R17 (création d'offre), R8 (phases 003 / 004, lot multi), D6 / D7 sur les routes
 * {@code /offres} et {@code /comparaison} (FEATURE-n6-demandes-achat.md ; Q-b1, Q-b6, Q-b7, Q-b8 du manager).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatOffresTest {

    private static final String OFFRES = "/offres";
    private static final String SELECTION = "/selection";
    private static final String COMPARAISON = "/comparaison";
    private static final String FOURNISSEUR_ID = "fournisseurId";
    private static final LocalDate DEPOT = LocalDate.of(2026, 3, 20);
    private static final String OFFRES_DU_TIERS = "SELECT * FROM BuyDocument WHERE BuySelectionId = ? AND CfgTierId = ?";
    private static final String OFFRE = "SELECT * FROM BuyDocument WHERE PKey = ?";
    private static final String GESTION = """
            SELECT e.Code FROM BuyDocument d JOIN StrEntity e ON e.PKey = d.StrEntityId WHERE d.BuySelectionId = ? AND d.CfgTierId = ?""";
    private static final String LIGNES_OFFRE = "SELECT * FROM BuyDocumentLine WHERE BuyDocumentId = ? ORDER BY Sorting ASC";
    private static final String GROUPES_DIA = "SELECT COUNT(*) FROM BuySelectionGroup WHERE BuySelectionId = ?";
    private static final String LIGNES_DIA = "SELECT COUNT(*) FROM BuySelectionLine WHERE BuySelectionId = ?";
    private static final String LIGNES_LOT = "SELECT COUNT(*) FROM PrjMarketLine WHERE PrjMarketId = ?";
    private static final String GROUPES_LOT = "SELECT COUNT(*) FROM PrjMarketLineGrouping WHERE PrjMarketId = ?";
    private static final String SELECTIONNEE = "IsSelected";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureLotsParite parite;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("OF");
        qLibre = qui(b.libre(c, "n6b.of.libre." + c.jeton()));
    }

    private UUID dia(String suffixe, UUID statut) {
        return demandes.demande(c, c.code(suffixe), statut);
    }

    private UUID tiers(String suffixe) {
        return achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
    }

    private static CorpsJson offrePour(UUID tiers) {
        return corps().avec(FOURNISSEUR_ID, tiers);
    }

    private RequestPostProcessor compteSans(String droit, String suffixe) {
        return qui(demandes.libre(c, "n6b.of." + suffixe + "." + c.jeton(), sauf(droit)));
    }

    private static String maj(UUID id) {
        return id.toString().toUpperCase();
    }

    private static String maj(Object valeur) {
        return valeur == null ? null : valeur.toString().toUpperCase();
    }

    private static LocalDate jour(Object valeur) {
        return ((Timestamp) valeur).toLocalDateTime().toLocalDate();
    }

    private static String selection(UUID offre) {
        return OFFRES + "/" + offre + SELECTION;
    }

    /** Bordereau de la DIA à 2 niveaux : G1 ▸ (A1, G2 ▸ A2). */
    private record Bordereau(UUID categorie, UUID g1, UUID g2, UUID a1, UUID a2) {
    }

    private Bordereau bordereau(UUID dia, String suffixe) {
        UUID categorie = achat.categorie("CAT" + suffixe + "-" + c.jeton());
        UUID g1 = b.groupe(dia, "G1", null, 1);
        UUID g2 = b.groupe(dia, "G2", g1, 2);
        UUID a1 = b.ligne(dia, g1, 1, "A1", "10", categorie);
        UUID a2 = b.ligne(dia, g2, 2, "A2", "20", categorie);
        return new Bordereau(categorie, g1, g2, a1, a2);
    }

    // ------------------------------------------------------------------ R16 : création refusée

    @Test
    void r16_offre_fournisseurEcarte_409_nonEcarte_cree() throws Exception {
        UUID dia = dia("ECA", c.prep());
        UUID ecarte = tiers("ECAA");
        UUID retenu = tiers("ECAB");
        b.ecarter(b.preselection(dia, ecarte, DEPOT));
        b.preselection(dia, retenu, DEPOT);

        Reponse refus = appels.poster(qLibre, dia, OFFRES, offrePour(ecarte));
        int offresEcarte = b.lignesApresRefus(OFFRES_DU_TIERS, dia.toString(), ecarte.toString()).size();
        Reponse temoin = appels.poster(qLibre, dia, OFFRES, offrePour(retenu));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(offresEcarte).isZero();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
            s.assertThat(b.lignes(OFFRES_DU_TIERS, dia.toString(), retenu.toString())).hasSize(1);
        });
    }

    @Test
    void r16_offre_dateDeDepotVide_409() throws Exception {
        UUID dia = dia("DEP", c.prep());
        UUID sansDepot = tiers("DEPA");
        UUID avecDepot = tiers("DEPB");
        b.preselection(dia, sansDepot, null);
        b.preselection(dia, avecDepot, DEPOT);

        Reponse refus = appels.poster(qLibre, dia, OFFRES, offrePour(sansDepot));
        int offres = b.lignesApresRefus(OFFRES_DU_TIERS, dia.toString(), sansDepot.toString()).size();
        Reponse temoin = appels.poster(qLibre, dia, OFFRES, offrePour(avecDepot));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(offres).isZero();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    // ------------------------------------------------------------------ R17 : contenu de l'offre

    @Test
    void r17_offre_enTeteCopieDeLaDia_dateEtEcheanceAuDepot_statutNull_gestionLotNonMulti() throws Exception {
        UUID dia = dia("R17", c.prep());
        UUID lot = b.lotDe(c, dia, "LR17-" + c.jeton(), false);
        UUID typeLot = achat.typeLot("TL17-" + c.jeton(), "Type " + c.jeton());
        UUID devise = achat.devise("DV17-" + c.jeton());
        demandes.poser(DEMANDE, dia, "PrjMarketTypeId", typeLot);
        demandes.poser(DEMANDE, dia, "StlCurrencyId", devise);
        UUID t = tiers("R17");
        b.preselection(dia, t, DEPOT);

        Reponse r = appels.poster(qLibre, dia, OFFRES, offrePour(t));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        List<Map<String, Object>> offres = b.lignes(OFFRES_DU_TIERS, dia.toString(), t.toString());
        assertThat(offres).hasSize(1);
        Map<String, Object> o = offres.getFirst();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(o.get("DocType")).isEqualTo(FixtureAchat.DEVIS);
            s.assertThat(maj(o.get("CfgCompanyId"))).isEqualTo(maj(c.societe()));
            s.assertThat(maj(o.get("PrjProjectId"))).isEqualTo(maj(c.projet()));
            s.assertThat(maj(o.get("PrjMarketId"))).isEqualTo(maj(lot));
            s.assertThat(maj(o.get("AdmModuleId"))).isEqualTo(maj(c.moduleAchat()));
            s.assertThat(maj(o.get("PrjMarketTypeId"))).isEqualTo(maj(typeLot));
            s.assertThat(maj(o.get("StlCurrencyId"))).isEqualTo(maj(devise));
            s.assertThat(jour(o.get("DocDate"))).isEqualTo(DEPOT);
            s.assertThat(jour(o.get("MaturityDate"))).isEqualTo(DEPOT);
            s.assertThat(o.get("BuyDocumentStatusId")).as("Q-b8 : statut NULL").isNull();
            s.assertThat(b.lignes(GESTION, dia.toString(), t.toString())).singleElement()
                    .satisfies(l -> assertThat(l).containsEntry("Code", "PrjMarketQuote"));
        });
    }

    @Test
    void r17_gestion_sansLot_BuyQuote() throws Exception {
        UUID dia = dia("GSL", c.prep());
        UUID t = tiers("GSL");
        b.preselection(dia, t, DEPOT);

        Reponse r = appels.poster(qLibre, dia, OFFRES, offrePour(t));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(b.lignes(GESTION, dia.toString(), t.toString())).singleElement()
                .satisfies(l -> assertThat(l).containsEntry("Code", "BuyQuote"));
    }

    @Test
    void r17_gestion_lotMulti_PrjMarketMultiSupplierQuote() throws Exception {
        UUID dia = dia("GLM", c.prep());
        b.lotDe(c, dia, "LGLM-" + c.jeton(), true);
        UUID t = tiers("GLM");
        b.preselection(dia, t, DEPOT);

        Reponse r = appels.poster(qLibre, dia, OFFRES, offrePour(t));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(b.lignes(GESTION, dia.toString(), t.toString())).singleElement()
                .satisfies(l -> assertThat(l).containsEntry("Code", "PrjMarketMultiSupplierQuote"));
    }

    @Test
    void r17_lignes_uneParLigneDuBordereau() throws Exception {
        UUID dia = dia("LIG", c.prep());
        Bordereau bd = bordereau(dia, "LIG");
        UUID t = tiers("LIG");
        b.preselection(dia, t, DEPOT);

        Reponse r = appels.poster(qLibre, dia, OFFRES, offrePour(t));

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        Object offre = b.lignes(OFFRES_DU_TIERS, dia.toString(), t.toString()).getFirst().get("PKey");
        List<Map<String, Object>> lignes = b.lignes(LIGNES_OFFRE, offre.toString());
        assertThat(lignes).extracting(l -> l.get("Code")).containsExactly("A1", "A2");
        Map<String, Object> a1 = lignes.getFirst();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(a1).containsEntry("Sorting", 1).containsEntry("Description", "Ligne A1").containsEntry("Unity", "M2");
            s.assertThat((BigDecimal) a1.get("Quantity")).isEqualByComparingTo("2");
            s.assertThat((BigDecimal) a1.get("VatRatio")).isEqualByComparingTo("0.2");
            s.assertThat((BigDecimal) a1.get("FODECRatio")).isEqualByComparingTo("0");
            s.assertThat(maj(a1.get("StlCategoryId"))).isEqualTo(maj(bd.categorie()));
        });
    }

    // ------------------------------------------------------------------ R16 : suppression

    @Test
    void r16_suppression_offreSelectionnee_409_nonSelectionnee_supprimee() throws Exception {
        UUID dia = dia("SUP", c.prep());
        UUID retenue = b.offre(dia, tiers("SUPA"), c.code("OSUPA"), true);
        UUID autre = b.offre(dia, tiers("SUPB"), c.code("OSUPB"), false);

        Reponse refus = appels.retirer(qLibre, dia, OFFRES + "/" + retenue);
        int restante = b.lignesApresRefus(OFFRE, retenue.toString()).size();
        Reponse temoin = appels.retirer(qLibre, dia, OFFRES + "/" + autre);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(restante).isEqualTo(1);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
            s.assertThat(b.lignes(OFFRE, autre.toString())).isEmpty();
        });
    }

    // ------------------------------------------------------------------ R16 : sélection

    @Test
    void r16_selection_uneSeuleSelectionnee_fournisseurPose_delaiDuFournisseurSiZero() throws Exception {
        UUID dia = dia("SEL", c.prep());
        b.lotDe(c, dia, "LSEL-" + c.jeton(), false);
        demandes.poser(DEMANDE, dia, "PaymentDelay", 0);
        UUID ta = tiers("SELA");
        UUID tb = tiers("SELB");
        demandes.poser("CfgTier", tb, "PaymentDelay", 45);
        UUID oa = b.offre(dia, ta, c.code("OSELA"), true);
        achat.montants(oa, "100", "100", "100"); // LC7 R6 : ob n'est pas la plus disante
        UUID ob = b.offre(dia, tb, c.code("OSELB"), false);

        Reponse r = appels.poster(qLibre, dia, selection(ob), corps());

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        Map<String, Object> enBase = demandes.demandeEnBase(dia);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(b.lignes(OFFRE, ob.toString()).getFirst()).containsEntry(SELECTIONNEE, true);
            s.assertThat(b.lignes(OFFRE, oa.toString()).getFirst()).containsEntry(SELECTIONNEE, false);
            s.assertThat(maj(enBase.get("CfgTierId"))).isEqualTo(maj(tb));
            s.assertThat(enBase).containsEntry("PaymentDelay", 45);
        });
    }

    @Test
    void r16_selection_delaiPositifDeLaDia_conserve() throws Exception {
        UUID dia = dia("SDP", c.prep());
        b.lotDe(c, dia, "LSDP-" + c.jeton(), false);
        demandes.poser(DEMANDE, dia, "PaymentDelay", 30);
        UUID t = tiers("SDP");
        demandes.poser("CfgTier", t, "PaymentDelay", 45);
        UUID o = b.offre(dia, t, c.code("OSDP"), false);

        Reponse r = appels.poster(qLibre, dia, selection(o), corps());

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(demandes.demandeEnBase(dia)).containsEntry("PaymentDelay", 30);
    }

    @Test
    void r16_selection_fournisseurEcarte_409() throws Exception {
        UUID dia = dia("SEC", c.prep());
        b.lotDe(c, dia, "LSEC-" + c.jeton(), false);
        UUID ecarte = tiers("SECA");
        UUID retenu = tiers("SECB");
        b.ecarter(b.preselection(dia, ecarte, DEPOT));
        b.preselection(dia, retenu, DEPOT);
        UUID oEcartee = b.offre(dia, ecarte, c.code("OSECA"), false);
        achat.poser(oEcartee, "IsRejected", true);
        achat.montants(oEcartee, "100", "100", "100"); // LC7 R6 : le témoin n'est pas la plus disante
        UUID oRetenue = b.offre(dia, retenu, c.code("OSECB"), false);

        Reponse refus = appels.poster(qLibre, dia, selection(oEcartee), corps());
        Object apresRefus = b.lignesApresRefus(OFFRE, oEcartee.toString()).getFirst().get(SELECTIONNEE);
        Reponse temoin = appels.poster(qLibre, dia, selection(oRetenue), corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(apresRefus).isEqualTo(false);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void r16_selection_lotNonMultiAvecDecompte_409() throws Exception {
        UUID dia = dia("SDC", c.prep());
        UUID lot = b.lotDe(c, dia, "LSDC-" + c.jeton(), false);
        parite.decompteDuLot(lot, "DC-SDC-" + c.jeton());
        UUID o = b.offre(dia, tiers("SDC"), c.code("OSDC"), false);
        UUID diaTemoin = dia("SDT", c.prep());
        b.lotDe(c, diaTemoin, "LSDT-" + c.jeton(), false);
        UUID oTemoin = b.offre(diaTemoin, tiers("SDT"), c.code("OSDT"), false);

        Reponse refus = appels.poster(qLibre, dia, selection(o), corps());
        Object apresRefus = b.lignesApresRefus(OFFRE, o.toString()).getFirst().get(SELECTIONNEE);
        Reponse temoin = appels.poster(qLibre, diaTemoin, selection(oTemoin), corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(apresRefus).isEqualTo(false);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void r1_selection_lotMultiSansBonDeCommande_200() throws Exception {
        // LC7 R1 : un lot multi-fournisseur sans pièce non devis rattachée ne bloque plus la sélection
        UUID dia = dia("SLM", c.prep());
        b.lotDe(c, dia, "LSLM-" + c.jeton(), true);
        UUID o = b.offre(dia, tiers("SLM"), c.code("OSLM"), false);

        Reponse r = appels.poster(qLibre, dia, selection(o), corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(b.lignes(OFFRE, o.toString()).getFirst()).containsEntry(SELECTIONNEE, true);
        });
    }

    @Test
    void r8_selection_horsPhase004_409() throws Exception {
        demandes.phases(c.depouil(), false, false, true, false);
        UUID dia = dia("SHP", c.depouil());
        b.lotDe(c, dia, "LSHP-" + c.jeton(), false);
        UUID o = b.offre(dia, tiers("SHP"), c.code("OSHP"), false);
        UUID diaTemoin = dia("SHT", c.prep());
        b.lotDe(c, diaTemoin, "LSHT-" + c.jeton(), false);
        UUID oTemoin = b.offre(diaTemoin, tiers("SHT"), c.code("OSHT"), false);

        Reponse refus = appels.poster(qLibre, dia, selection(o), corps());
        Reponse temoin = appels.poster(qLibre, diaTemoin, selection(oTemoin), corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    // ------------------------------------------------------------------ R16 : désélection

    @Test
    void r16_deselection_toutesA0_bordereauDeLaDiaVide_fournisseurNull_bordereauDuLotIntact() throws Exception {
        UUID dia = dia("DES", c.prep());
        UUID lot = b.lotDe(c, dia, "LDES-" + c.jeton(), false);
        b.groupeDefinitif(lot, "GL-" + c.jeton());
        b.ligneDefinitive(lot, "LL-" + c.jeton());
        bordereau(dia, "DES");
        UUID t = tiers("DES");
        demandes.poser(DEMANDE, dia, "CfgTierId", t);
        UUID retenue = b.offre(dia, t, c.code("ODESA"), true);
        UUID autre = b.offre(dia, tiers("DESB"), c.code("ODESB"), false);

        Reponse r = appels.retirer(qLibre, dia, OFFRES + SELECTION);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(b.lignes(OFFRE, retenue.toString()).getFirst()).containsEntry(SELECTIONNEE, false);
            s.assertThat(b.lignes(OFFRE, autre.toString()).getFirst()).containsEntry(SELECTIONNEE, false);
            s.assertThat(b.compter(GROUPES_DIA, dia.toString())).isZero();
            s.assertThat(b.compter(LIGNES_DIA, dia.toString())).isZero();
            s.assertThat(demandes.demandeEnBase(dia).get("CfgTierId")).isNull();
            s.assertThat(b.compter(LIGNES_LOT, lot.toString())).as("Q-b6 : le lot garde son bordereau").isEqualTo(1);
            s.assertThat(b.compter(GROUPES_LOT, lot.toString())).isEqualTo(1);
        });
    }

    @Test
    void r16_deselection_lotNonMultiAvecDecompte_409_rienNEstVide() throws Exception {
        UUID dia = dia("DDC", c.prep());
        UUID lot = b.lotDe(c, dia, "LDDC-" + c.jeton(), false);
        parite.decompteDuLot(lot, "DC-DDC-" + c.jeton());
        bordereau(dia, "DDC");
        UUID retenue = b.offre(dia, tiers("DDC"), c.code("ODDC"), true);
        UUID diaTemoin = dia("DDT", c.prep());
        b.lotDe(c, diaTemoin, "LDDT-" + c.jeton(), false);
        b.offre(diaTemoin, tiers("DDT"), c.code("ODDT"), true);

        Reponse refus = appels.retirer(qLibre, dia, OFFRES + SELECTION);
        Object apresRefus = b.lignesApresRefus(OFFRE, retenue.toString()).getFirst().get(SELECTIONNEE);
        int lignesDia = b.compterApresRefus(LIGNES_DIA, dia.toString());
        Reponse temoin = appels.retirer(qLibre, diaTemoin, OFFRES + SELECTION);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(apresRefus).isEqualTo(true);
            s.assertThat(lignesDia).isEqualTo(2);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    // ------------------------------------------------------------------ R8 : phases offres / comparaison

    @Test
    void r8_offre_horsPhase003_409() throws Exception {
        UUID horsPhase = dia("OHP", c.depouil());
        UUID enPhase = dia("OEP", c.prep());
        UUID t = tiers("OHP");
        b.preselection(horsPhase, t, DEPOT);
        b.preselection(enPhase, t, DEPOT);

        Reponse refus = appels.poster(qLibre, horsPhase, OFFRES, offrePour(t));
        Reponse temoin = appels.poster(qLibre, enPhase, OFFRES, offrePour(t));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(b.lignesApresRefus(OFFRES_DU_TIERS, horsPhase.toString(), t.toString())).isEmpty();
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void r8_comparaison_horsPhases003Et004_409_enPhase003_200() throws Exception {
        UUID dia = dia("CMP", c.depouil());
        b.offre(dia, tiers("CMP"), c.code("OCMP"), false);

        Reponse refus = appels.lire(qLibre, dia, COMPARAISON);
        demandes.phases(c.depouil(), false, false, true, false);
        Reponse enPhase003 = appels.lire(qLibre, dia, COMPARAISON);
        demandes.phases(c.depouil(), false, false, false, true);
        Reponse enPhase004 = appels.lire(qLibre, dia, COMPARAISON);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(enPhase003.statut()).as(enPhase003.corps()).isEqualTo(200);
            s.assertThat(enPhase004.statut()).as(enPhase004.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ D7 : droits, D6 : confidentialité

    @Test
    void d7_sansDroitAjoutOffre_403() throws Exception {
        UUID dia = dia("D7A", c.prep());
        UUID t = tiers("D7A");
        b.preselection(dia, t, DEPOT);

        Reponse refus = appels.poster(compteSans(DROIT_AJOUT_OFFRE, "sansajout"), dia, OFFRES, offrePour(t));
        Reponse temoin = appels.poster(qLibre, dia, OFFRES, offrePour(t));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void d7_sansDroitSuppressionOffre_403() throws Exception {
        UUID dia = dia("D7S", c.prep());
        UUID o = b.offre(dia, tiers("D7S"), c.code("OD7S"), false);

        Reponse refus = appels.retirer(compteSans(DROIT_SUPPRESSION_OFFRE, "sanssup"), dia, OFFRES + "/" + o);
        int apresRefus = b.lignesApresRefus(OFFRE, o.toString()).size();
        Reponse temoin = appels.retirer(qLibre, dia, OFFRES + "/" + o);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(apresRefus).isEqualTo(1);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void d7_sansDroitSelectionOffre_403() throws Exception {
        UUID dia = dia("D7L", c.prep());
        b.lotDe(c, dia, "LD7L-" + c.jeton(), false);
        UUID o = b.offre(dia, tiers("D7L"), c.code("OD7L"), false);

        Reponse refus = appels.poster(compteSans(DROIT_SELECTION_OFFRE, "sanssel"), dia, selection(o), corps());
        Reponse refusDeselection = appels.retirer(compteSans(DROIT_SELECTION_OFFRE, "sansdesel"), dia, OFFRES + SELECTION);
        Reponse temoin = appels.poster(qLibre, dia, selection(o), corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(refusDeselection.statut()).as(refusDeselection.corps()).isEqualTo(403);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void d6_procedureConfidentielle_sansLeDroit_offres_403() throws Exception {
        UUID confidentielle = demandes.procedure("PCONF-" + c.jeton(), c.entite());
        demandes.confidentielle(confidentielle);
        UUID diaConf = demandes.demande(c, c.code("CONF"), c.prep(), confidentielle, LocalDate.of(2026, 3, 10));
        RequestPostProcessor sansConf = compteSans(DROIT_CONFIDENTIEL, "sansconf");

        Reponse refus = appels.lire(sansConf, diaConf, OFFRES);
        Reponse temoinOrdinaire = appels.lire(sansConf, dia("ORD", c.prep()), OFFRES);
        Reponse temoinAvecDroit = appels.lire(qLibre, diaConf, OFFRES);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoinOrdinaire.statut()).as(temoinOrdinaire.corps()).isEqualTo(200);
            s.assertThat(temoinAvecDroit.statut()).as(temoinAvecDroit.corps()).isEqualTo(200);
        });
    }
}
