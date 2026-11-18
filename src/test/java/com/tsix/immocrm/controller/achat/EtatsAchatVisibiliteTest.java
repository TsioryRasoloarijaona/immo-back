package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureEtatsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.AVANCES;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.AVANCES_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.FACTURE_ID;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.PIECE_ID;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.RETENUES;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.RETENUES_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.TVA;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.TVA_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.annee;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.lignesDe;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureEtatsAchat.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Périmètre des 3 états et de leurs exports — FEATURE-n7-etats-achat.md D3, R10 (règle 6 : visibilité, chemins de refus).
 * <p>Compte habilité à la société S1 et au projet PA seulement. Chaque pièce est une facture (TVA = 1/6 du TTC), avec avance
 * et retenue : VA (S1, PA) TTC 120 → TVA 20, avance 10, retenue 5 ; V0 (S1, sans projet) TTC 240 → TVA 40, avance 20,
 * retenue 7 ; HB (S1, PB non habilité) TTC 1 200 → TVA 200, avance 100, retenue 50 ; HS (S2 non habilitée, P2 habilité)
 * TTC 12 000 → TVA 2 000, avance 1 000, retenue 500.
 * Visibles : VA + V0 → TVA 60, HT 300, amortissement 30, retenue 12.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtatsAchatVisibiliteTest {

    private static final String ZERO = "0";
    private static final String PROJET_IDS = "projetIds";
    private static final String SOCIETE_ID = "societeId";
    private static final String TOTAL_TVA = "$.totaux.montantTva";
    private static final String TOTAL_AMORTISSEMENT = "$.totaux.amortissement";
    private static final String TOTAL_RETENUE = "$.totaux.retenue";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureEtatsAchat fixture;

    private String jeton;
    private UUID s1;
    private UUID s2;
    private UUID projetA;
    private UUID projetB;
    private UUID visible;
    private UUID sansProjet;
    private UUID horsProjet;
    private UUID horsSociete;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("N7V");
        s1 = achat.societe("S1-" + jeton);
        s2 = achat.societe("S2-" + jeton);
        projetA = achat.projet("PA-" + jeton, s1);
        projetB = achat.projet("PB-" + jeton, s1);
        UUID projet2 = achat.projet("P2-" + jeton, s2);
        String nom = "Fournisseur VIS " + jeton;
        UUID fournisseur = achat.fournisseur("FOU-" + jeton, nom);

        visible = piece("-VA", fournisseur, nom, s1, projetA, "120", "10", "5");
        sansProjet = piece("-V0", fournisseur, nom, s1, null, "240", "20", "7");
        horsProjet = piece("-HB", fournisseur, nom, s1, projetB, "1200", "100", "50");
        horsSociete = piece("-HS", fournisseur, nom, s2, projet2, "12000", "1000", "500");

        compte = fixture.compte("N7V-" + jeton, s1, projetA, FixtureEtatsAchat.TOUS_LES_DROITS);
        achat.habiliter(compte, projet2);
    }

    private UUID piece(String suffixe, UUID fournisseur, String nom, UUID societe, UUID projet, String ttc,
                       String avance, String retenue) {
        UUID id = fixture.piece(jeton + suffixe, FixtureAchat.FACTURE, date(5, 5), fournisseur, nom, societe, projet, null, ttc);
        fixture.avance(id, avance, "0");
        fixture.retenue(id, retenue);
        return id;
    }

    private RequestPostProcessor qui() {
        return connecte(compte, FixtureEtatsAchat.role());
    }

    private String json(String route, String... params) throws Exception {
        return corps(mockMvc, annee(route, qui(), params));
    }

    private static String champId(String route) {
        return TVA.equals(route) ? FACTURE_ID : PIECE_ID;
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void r10_pieceProjetHabilite_etPieceSansProjet_visibles(String route) throws Exception {
        String json = json(route);

        assertAll(
                () -> assertThat(lignesDe(json, champId(route), visible)).isNotEmpty(),
                () -> assertThat(lignesDe(json, champId(route), sansProjet)).isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void r10_pieceProjetNonHabilite_absente(String route) throws Exception {
        assertThat(lignesDe(json(route), champId(route), horsProjet)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void r10_pieceSocieteNonHabilitee_absenteMemeSiProjetHabilite(String route) throws Exception {
        assertThat(lignesDe(json(route), champId(route), horsSociete)).isEmpty();
    }

    @Test
    void r10_totauxTva_neSommentQueLeVisible() throws Exception {
        String json = json(TVA);

        assertAll(
                () -> assertThat(nombre(json, TOTAL_TVA)).isEqualByComparingTo("60"),
                () -> assertThat(nombre(json, "$.totaux.montantHT")).isEqualByComparingTo("300"),
                () -> assertThat(nombre(json, "$.totaux.factures")).isEqualByComparingTo("2"));
    }

    @Test
    void r10_totauxAvancesEtRetenues_neSommentQueLeVisible() throws Exception {
        String avances = json(AVANCES);
        String retenues = json(RETENUES);

        assertAll(
                () -> assertThat(nombre(avances, TOTAL_AMORTISSEMENT)).isEqualByComparingTo("30"),
                () -> assertThat(nombre(retenues, TOTAL_RETENUE)).isEqualByComparingTo("12"));
    }

    @Test
    void r10_projetIdsHorsPerimetre_ignore_rienNiMontant() throws Exception {
        String tva = json(TVA, PROJET_IDS, projetB.toString());
        String avances = json(AVANCES, PROJET_IDS, projetB.toString());
        String retenues = json(RETENUES, PROJET_IDS, projetB.toString());

        assertAll(
                () -> assertThat(lignesDe(tva, FACTURE_ID, horsProjet)).isEmpty(),
                () -> assertThat(nombre(tva, TOTAL_TVA)).isEqualByComparingTo(ZERO),
                () -> assertThat(lignesDe(avances, PIECE_ID, horsProjet)).isEmpty(),
                () -> assertThat(nombre(avances, TOTAL_AMORTISSEMENT)).isEqualByComparingTo(ZERO),
                () -> assertThat(lignesDe(retenues, PIECE_ID, horsProjet)).isEmpty(),
                () -> assertThat(nombre(retenues, TOTAL_RETENUE)).isEqualByComparingTo(ZERO));
    }

    @Test
    void r10_projetIdsAEtB_neGardeQueA() throws Exception {
        String json = json(TVA, PROJET_IDS, projetA.toString(), PROJET_IDS, projetB.toString());

        assertAll(
                () -> assertThat(lignesDe(json, FACTURE_ID, visible)).hasSize(1),
                () -> assertThat(lignesDe(json, FACTURE_ID, horsProjet)).isEmpty(),
                () -> assertThat(nombre(json, TOTAL_TVA)).isEqualByComparingTo("20"));
    }

    @Test
    void r10_societeIdNonHabilitee_rienNiMontant() throws Exception {
        String tva = json(TVA, SOCIETE_ID, s2.toString());
        String retenues = json(RETENUES, SOCIETE_ID, s2.toString());

        assertAll(
                () -> assertThat(lignesDe(tva, FACTURE_ID, horsSociete)).isEmpty(),
                () -> assertThat(nombre(tva, TOTAL_TVA)).isEqualByComparingTo(ZERO),
                () -> assertThat(lignesDe(retenues, PIECE_ID, horsSociete)).isEmpty(),
                () -> assertThat(nombre(retenues, TOTAL_RETENUE)).isEqualByComparingTo(ZERO));
    }

    @Test
    void r10_administrateurSansHabilitation_rienNiMontant_pasDExemption() throws Exception {
        AdmUser admin = achat.compte("N7V-ADM-" + jeton, "Administrateur");

        String json = corps(mockMvc, annee(TVA, connecte(admin, "Administrateur")));

        assertAll(
                () -> assertThat(lignesDe(json, FACTURE_ID, visible)).isEmpty(),
                () -> assertThat(lignesDe(json, FACTURE_ID, sansProjet)).isEmpty(),
                () -> assertThat(nombre(json, TOTAL_TVA)).isEqualByComparingTo(ZERO));
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA_EXPORT, AVANCES_EXPORT, RETENUES_EXPORT})
    void r10_export_sansLesPiecesHorsPerimetre(String route) throws Exception {
        List<String> textes = AppelsAchat.textesExport(mockMvc, annee(route, qui()));

        assertThat(textes).contains(jeton + "-VA")
                .doesNotContain(jeton + "-HB", jeton + "-HS");
    }
}
