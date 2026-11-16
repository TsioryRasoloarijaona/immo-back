package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.ConnexionDeTest;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.bordereau;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ligne;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.inchange;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.somme;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.texte;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.LIGNE_PERIMEE;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Recalcul de {@code PrjMarketPaymentPrevision} après chaque famille d'écriture du §6 (FEATURE-lot-previsions.md R8,
 * réponse 4 : la transition aussi). Chaque lot porte une ligne périmée d'un ancien calcul : recalculé = la ligne a disparu
 * et les lignes du calcul sont là. Lot de fixture : prévisions 01/01/2026 + 90 j, estimation 1000 / 1200, sans retenue →
 * trois « Décompte prévisionnel » de 400 (Σ 1200).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsRecalculTest {

    private static final String DESCRIPTION = "Description";
    private static final String DECOMPTE_N = "Décompte prévisionnel N° ";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final BigDecimal CENTIME = new BigDecimal("0.01");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private FixtureBordereauxEcriture bordereaux;
    @Autowired private FixtureProjetLots projetLots;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureProjetConfig config;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureConfigurationAchat configAchat;

    private String jeton;
    private UUID projet;
    private UUID statut;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        projet = parite.projet("PVC-" + jeton, parite.entiteJuridique("PVD-" + jeton));
        statut = parite.statut(ENTREPRENEURS, "PVO-" + jeton);
    }

    private RequestPostProcessor admin() {
        if (admin == null) {
            admin = connecte(parite.administrateur("lot.pvc." + jeton, projet), ADMINISTRATEUR);
        }
        return admin;
    }

    /** Lot du projet de test, au statut donné, paramétré et porteur d'une ligne périmée. */
    private UUID lot(UUID statutDuLot) {
        rang++;
        return parametrer(parite.lot(projet, "PVC" + rang + "-" + jeton, ENTREPRENEURS, statutDuLot));
    }

    private UUID parametrer(UUID lot) {
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0", "0", "0");
        prev.multiFournisseur(lot, false);
        prev.lignePerimee(lot);
        return lot;
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private static void reussi(Reponse r) {
        assertThat(r.statut()).as("écriture déclencheuse : " + r.corps()).isBetween(200, 204);
    }

    /** Ligne périmée supprimée, lignes du calcul écrites (trois décomptes prévisionnels). */
    private void recalcule(SoftAssertions s, UUID lot, String quoi) {
        List<Map<String, Object>> lignes = prev.lignesPaiement(lot);
        s.assertThat(lignes).as(quoi + " : ligne périmée").extracting(l -> texte(l, DESCRIPTION)).doesNotContain(LIGNE_PERIMEE);
        s.assertThat(lignes).as(quoi + " : décomptes prévisionnels").extracting(l -> texte(l, DESCRIPTION))
                .containsExactly(DECOMPTE_N + 1, DECOMPTE_N + 2, DECOMPTE_N + 3);
    }

    private void recalcule(UUID lot, String quoi) {
        SoftAssertions.assertSoftly(s -> recalcule(s, lot, quoi));
    }

    // ------------------------------------------------------------------ prévisions (PUT)

    @Test
    void r8_putPrevisions_lignesReecrites_pkeyNeuves() throws Exception {
        UUID lot = lot(statut);
        String perimee = texte(prev.lignesPaiement(lot).getFirst(), "id");

        reussi(new PrevisionsLotSupport(mockMvc).enregistrer(admin(), lot, inchange()));

        recalcule(lot, "PUT prévisions");
        assertThat(prev.lignesPaiement(lot)).extracting(l -> texte(l, "id")).doesNotContain(perimee);
    }

    // ------------------------------------------------------------------ bordereau estimatif

    /** Bordereau estimatif 2 × 100 HT, TVA 20 % : estimation confidentielle 240 TTC → Σ des décomptes = 240. */
    @Test
    void r8_bordereauEstimatif_recalculSurLeNouveauBordereau() throws Exception {
        bordereaux.tableVentilation();
        UUID ouvert = parite.statut(ENTREPRENEURS, "PVB-" + jeton);
        bordereaux.ouvrirEstimatif(ouvert);
        UUID categorie = bordereaux.categorie("PVK-" + jeton);
        UUID lot = lot(ouvert);

        reussi(new BordereauxEcritureSupport(mockMvc).ecrire(admin(), lot, ESTIMATIF,
                bordereau(List.of(), List.of(ligne(UUID.randomUUID(), null, 1, "1", "2", "100", categorie)))));

        List<Map<String, Object>> lignes = prev.lignesPaiement(lot);
        SoftAssertions.assertSoftly(s -> {
            recalcule(s, lot, "bordereau estimatif");
            s.assertThat(somme(lignes, "Amount")).isCloseTo(new BigDecimal("240"), within(CENTIME));
        });
    }

    // ------------------------------------------------------------------ lot : modification, transition, suppression

    @Test
    void r8_modificationDuLot_recalcule() throws Exception {
        UUID lot = lot(statut);

        reussi(new LotFicheSupport(mockMvc).modifier(admin(), lot, corps().avec(LotFicheSupport.DESCRIPTION, "Lot modifié " + jeton)));

        recalcule(lot, "PATCH lot");
    }

    /** Réponse du manager 4 : la transition de statut déclenche le recalcul. */
    @Test
    void r8_transitionDeStatut_recalcule() throws Exception {
        UUID depart = parite.statut(ENTREPRENEURS, "PVA-" + jeton);
        UUID arrivee = parite.statut(ENTREPRENEURS, "PVZ-" + jeton);
        UUID transition = parite.transition(ENTREPRENEURS, depart, arrivee, "PV " + jeton);
        UUID lot = lot(depart);
        AdmUser compte = fiche.administrateurRestreint("lot.pvt." + jeton, false, projet);
        parite.intervenant(ENTREPRENEURS, parite.roleDe(compte), depart, transition);

        reussi(new LotsPariteSupport(mockMvc).executer(connecte(compte, ADMINISTRATEUR), lot, transition));

        recalcule(lot, "transition");
    }

    /** Lot prorata du projet = un autre lot : il est recalculé après l'écriture du premier (R8, l.1387-1392). */
    @Test
    void r8_lotProrataDuProjet_recalculeApresEcritureDUnAutreLot() throws Exception {
        UUID lot = lot(statut);
        UUID prorata = lot(statut);
        prev.lotProrata(projet, prorata);

        reussi(new LotFicheSupport(mockMvc).modifier(admin(), lot, corps().avec(LotFicheSupport.DESCRIPTION, "Lot écrit " + jeton)));

        SoftAssertions.assertSoftly(s -> {
            recalcule(s, lot, "lot écrit");
            recalcule(s, prorata, "lot prorata");
        });
    }

    /** Suppression (§6, AfterDelete) : lignes du lot supprimées, lot prorata du projet recalculé. */
    @Test
    void r8_suppressionDuLot_lignesSupprimees_lotProrataRecalcule() throws Exception {
        UUID lot = lot(statut);
        UUID prorata = lot(statut);
        prev.lotProrata(projet, prorata);

        reussi(new LotsPariteSupport(mockMvc).supprimer(admin(), lot));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(prev.lignesPaiement(lot)).as("lignes du lot supprimé").isEmpty();
            recalcule(s, prorata, "lot prorata");
        });
    }

    // ------------------------------------------------------------------ décomptes

    @Test
    void r8_creationDUnDecompte_recalcule() throws Exception {
        UUID projetDecomptes = projetLots.projet("PVN-" + jeton);
        UUID lot = parametrer(projetLots.lot(projetDecomptes, "PVM-" + jeton, projetLots.fournisseur("PVF-" + jeton)));
        RequestPostProcessor qui = decomptes.compte(projetDecomptes, jeton, DROIT_MODIFIER);

        reussi(envoyer(post(AppelsProjet.LOTS + "/" + lot + "/decomptes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateDocument\":\"2026-04-01\"}"), qui));

        List<Map<String, Object>> lignes = prev.lignesPaiement(lot);
        assertThat(lignes).isNotEmpty().extracting(l -> texte(l, DESCRIPTION)).doesNotContain(LIGNE_PERIMEE);
    }

    @Test
    void r8_suppressionDUnDecompte_recalcule() throws Exception {
        UUID projetDecomptes = projetLots.projet("PVN-" + jeton);
        UUID lot = parametrer(projetLots.lot(projetDecomptes, "PVM-" + jeton, projetLots.fournisseur("PVF-" + jeton)));
        UUID decompte = projetLots.decompte(lot, "PVW-" + jeton, 1,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
        AdmUser compte = vente.compte("prj.pv.a." + jeton, ADMINISTRATEUR);
        projetLots.habiliter(compte, projetDecomptes);

        reussi(envoyer(delete(AppelsProjet.DECOMPTES + "/" + decompte), connecte(compte, ADMINISTRATEUR)));

        recalcule(lot, "suppression de décompte");
    }

    // ------------------------------------------------------------------ projet : lot prorata changé

    /** {@code ProjetConfigService.modifier} : ancien et nouveau lot prorata recalculés (Project.cs AfterUpdate). */
    @Test
    void r8_projet_lotProrataChange_ancienEtNouveauRecalcules() throws Exception {
        config.parametrage();
        UUID societe = patrimoine.entite("PVS-" + jeton, true).getId();
        UUID projetConfig = config.projet("PVJ-" + jeton, societe);
        UUID ancien = parametrer(config.lotDuProjet("PVL1-" + jeton, projetConfig));
        UUID nouveau = parametrer(config.lotDuProjet("PVL2-" + jeton, projetConfig));
        config.liens(projetConfig, null, null, ancien);

        reussi(envoyer(put(FixtureProjetConfig.PROJET, projetConfig).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec(FixtureProjetConfig.CODE, "PVJ-" + jeton).avec(FixtureProjetConfig.DESCRIPTION, "Projet PVJ")
                        .avec(FixtureProjetConfig.SOCIETE_ID, societe)
                        .avec(FixtureProjetConfig.LOT_GESTIONNAIRE_PRORATA_ID, nouveau).json()),
                ConnexionDeTest.connecte(patrimoine.administrateur())));

        SoftAssertions.assertSoftly(s -> {
            recalcule(s, ancien, "ancien lot prorata");
            recalcule(s, nouveau, "nouveau lot prorata");
        });
    }

    // ------------------------------------------------------------------ pièce d'achat du lot

    /** Suppression d'un BC rattaché au lot ({@code DocType} ORDER) : le lot est recalculé. */
    @Test
    void r8_suppressionDUnBonDeCommandeDuLot_recalcule() throws Exception {
        Cadre c = achat.cadre("PVA");
        achat.tablesComptables();
        AdmUser compte = achat.compteHabilite(c, "PV-ADM", ADMINISTRATEUR);
        achat.sansPersonnel(compte);
        UUID lot = parametrer(achat.lot("PVB-" + c.jeton(), c.projet()));
        UUID bc = achat.piece(c, "-BC", FixtureAchat.BON_COMMANDE);
        achat.poser(bc, "PrjMarketId", lot);
        achat.poser(bc, "BuyDocumentStatusId", configAchat.statut(Statut.simple("PV-" + c.jeton(), 1)));

        reussi(envoyer(delete("/api/achat/documents/" + bc), connecte(compte, "Commercial")));

        recalcule(lot, "suppression de BC");
    }
}
