package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecompteExtras;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.champs;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.corps;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.json;
import static com.tsix.immocrm.fixtures.FixtureDecompteExtras.nombre;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Seconde revue LC6 (règle 6) : R7 verrou 409 par liste et droits, R8 suppression, R9 non-régression des taux. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesExtrasSecondeRevueTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String EXTRAS = "extras";
    private static final String RETENUES = "retenues";
    private static final String FIGEE = "Figée";
    private static final String RATIO_RG = "WarrantyRatio";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private FixtureDecompteExtras extras;

    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor admin;
    private RequestPostProcessor modifier;

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PXS-" + jeton);
        lot = nouveauLot("LXS-");
        AdmUser compte = vente.compte("prj.xs.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
        modifier = decomptes.compte(projet, jeton, DROIT_MODIFIER);
    }

    private UUID nouveauLot(String code) {
        return fixture.lot(projet, code + jeton, fixture.fournisseur("F" + code + jeton));
    }

    private UUID decompte(UUID lotId, String code, int numero) {
        return fixture.decompte(lotId, code + jeton, numero,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
    }

    private int executer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r.getStatus();
    }

    private int enregistrer(UUID decompte, String corps, RequestPostProcessor qui) throws Exception {
        return executer(put(DECOMPTES + "/" + decompte).contentType(MediaType.APPLICATION_JSON).content(corps), qui);
    }

    private static Map<String, Object> extraLibre() {
        return champs("designation", "Nouvelle", "quantite", BigDecimal.ONE, "prixUnitaireHT", new BigDecimal("100"),
                "tauxTVA", new BigDecimal("20"));
    }

    private static Map<String, Object> retenueLibre() {
        return champs("libelle", "Nouvelle", "montant", new BigDecimal("100"));
    }

    private static String corpsDeuxListes() {
        return json(champs(EXTRAS, List.of(extraLibre()), RETENUES, List.of(retenueLibre())));
    }

    /** Corps qui ajoute une ligne à la liste {@code liste}. */
    private static String ajout(String liste) {
        return corps(liste, List.of(EXTRAS.equals(liste) ? extraLibre() : retenueLibre()));
    }

    private int lignes(String liste, UUID decompte) {
        return (EXTRAS.equals(liste) ? extras.extras(decompte) : extras.retenues(decompte)).size();
    }

    private void figer(String liste, UUID decompte) {
        if (EXTRAS.equals(liste)) {
            extras.extra(decompte, 1, FIGEE);
        } else {
            extras.retenue(decompte, 1, FIGEE, "10");
        }
    }

    // ------------------------------------------------------------------ R7 verrou (409 par liste)

    /** R7 : décompte verrouillé (non dernier du lot, ou facturé) → 409, la liste stockée reste ; témoin 200 sur un décompte libre. */
    @ParameterizedTest
    @CsvSource({"extras,nonDernier", "extras,facture", "retenues,nonDernier", "retenues,facture"})
    void r7_decompteVerrouille_409_rienEcrit(String liste, String verrou) throws Exception {
        UUID verrouille = decompte(lot, "XV1-", 1);
        figer(liste, verrouille);
        if ("facture".equals(verrou)) {
            fixture.facture(verrouille, "FXV" + jeton, new BigDecimal("1000"));
        } else {
            decompte(lot, "XV2-", 2);
        }
        UUID libreDecompte = decompte(nouveauLot("LXT-"), "XT1-", 1);

        int statut = enregistrer(verrouille, ajout(liste), modifier);

        assertThat(statut).isEqualTo(409);
        assertThat(lignes(liste, verrouille)).isEqualTo(1);
        assertThat(enregistrer(libreDecompte, ajout(liste), modifier)).as("témoin décompte non verrouillé").isEqualTo(200);
        assertThat(lignes(liste, libreDecompte)).as("témoin écrit").isEqualTo(1);
    }

    /** R7 : DROIT_MODIFIER (A_0152_13) suffit pour les extras, aucun droit supplémentaire. */
    @Test
    void r7_droitModifierSeul_extras_200_ecrites() throws Exception {
        UUID d = decompte(lot, "XM1-", 1);

        int statut = enregistrer(d, ajout(EXTRAS), decomptes.compte(projet, jeton, DROIT_MODIFIER));

        assertThat(statut).isEqualTo(200);
        assertThat(extras.extras(d)).hasSize(1);
    }

    /** R7 : retenues forfaitaires hors A_0152_22 (CanEditWorkingOutRetainByAmount) : DROIT_MODIFIER seul suffit. */
    @Test
    void r7_retenuesForfaitaires_sansDroitParMontant_200_ecrites() throws Exception {
        UUID d = decompte(lot, "XM2-", 1);

        int statut = enregistrer(d, ajout(RETENUES), decomptes.compte(projet, jeton, DROIT_MODIFIER));

        assertThat(statut).isEqualTo(200);
        assertThat(extras.retenues(d)).hasSize(1);
    }

    @Test
    void r7_sansDroitModifier_403_rienEcrit() throws Exception {
        UUID d = decompte(lot, "XM3-", 1);

        int statut = enregistrer(d, corpsDeuxListes(), decomptes.compte(projet, jeton, DROIT_SUPPRIMER));

        assertThat(statut).isEqualTo(403);
        assertThat(extras.extras(d)).isEmpty();
        assertThat(extras.retenues(d)).isEmpty();
    }

    // ------------------------------------------------------------------ R8 suppression du décompte

    /** R8 : extras et retenues écrites par Enregistrer, puis supprimées avec le décompte (retenues NO_ACTION). */
    @Test
    void r8_suppression_supprimeExtrasEtRetenuesEcrites() throws Exception {
        UUID d = decompte(lot, "XD1-", 1);
        assertThat(enregistrer(d, corpsDeuxListes(), modifier)).isEqualTo(200);
        assertThat(extras.extras(d)).as("extras écrites").hasSize(1);
        assertThat(extras.retenues(d)).as("retenues écrites").hasSize(1);

        int statut = executer(delete(DECOMPTES + "/" + d), admin);

        assertThat(statut).isIn(200, 204);
        assertThat(extras.extras(d)).isEmpty();
        assertThat(extras.retenues(d)).isEmpty();
    }

    // ------------------------------------------------------------------ R9 non-régression des taux (déjà fait)

    /** R9 : taux de RG saisi (5 %) conservé par un second Enregistrer qui ne le porte pas. */
    @Test
    void r9_tauxModifie_conserveApresEnregistrer() throws Exception {
        UUID d = decompte(lot, "XN1-", 1);
        assertThat(enregistrer(d, "{\"tauxRetenueGarantie\":5}", modifier)).isEqualTo(200);

        int statut = enregistrer(d, "{\"notes\":\"x\"}", modifier);

        assertThat(statut).isEqualTo(200);
        assertThat(nombre(decomptes.lire(d, RATIO_RG), RATIO_RG)).isEqualByComparingTo("0.05");
    }

    /** R9 : Recalculer remet le taux du lot (WarrantyRatio 0,1). */
    @Test
    void r9_recalculer_remetLeTauxDuLot() throws Exception {
        UUID d = decompte(lot, "XN2-", 1);
        assertThat(enregistrer(d, "{\"tauxRetenueGarantie\":5}", modifier)).isEqualTo(200);

        int statut = executer(post(DECOMPTES + "/" + d + DecomptesPariteEcrituresTest.RECALCUL), admin);

        assertThat(statut).isIn(200, 204);
        assertThat(nombre(decomptes.lire(d, RATIO_RG), RATIO_RG)).isEqualByComparingTo("0.1");
    }
}
