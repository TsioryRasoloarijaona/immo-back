package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
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
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.montantAgregat;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.TOTAUX;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Lot A1 bis — FEATURE-achat-bc-agregats.md « Cas limites » n° 10 et réponse du manager Q1 (règle 6, visibilité) : un BC
 * hors périmètre n'apparaît jamais, ni dans la liste ni dans {@code totaux.agregats} ; les pièces liées comptent toutes,
 * même d'une société ou d'un projet non habilités.
 * <pre>
 *   S (habilitée)    : BC-V (facture liée de S2, facture liée du projet PX), BC-PX (projet PX non habilité) + facture
 *   S2 (non habilitée) : BC-X + facture liée
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatAgregatsPerimetreTest {

    private static final String ROLE = "Commercial";
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final LocalDate MARS_10 = LocalDate.of(2026, 3, 10);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ABP");
        UUID s = fixture.societe("S-" + jeton);
        UUID s2 = fixture.societe("S2-" + jeton);
        UUID horsProjet = fixture.projet("PX-" + jeton, s);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, s);
        fixture.sansPersonnel(compte);

        UUID visible = piece("-BC-V", BON_COMMANDE, s, null, "1000", "1200");
        liee(visible, "-BC-V-F2", s2, null, "700", "840");
        liee(visible, "-BC-V-FP", s, horsProjet, "100", "120");

        UUID bcProjet = piece("-BC-PX", BON_COMMANDE, s, horsProjet, "5000", "6000");
        liee(bcProjet, "-BC-PX-F", s, horsProjet, "5000", "6000");

        UUID bcAutre = piece("-BC-X", BON_COMMANDE, s2, null, "9000", "10800");
        liee(bcAutre, "-BC-X-F", s2, null, "9000", "10800");
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    private UUID piece(String suffixe, String nature, UUID societe, UUID projet, String ht, String ttc) {
        UUID id = fixture.piece(Piece.facture(code(suffixe), MARS_10, fournisseur, nom, societe, projet,
                new BigDecimal(ttc)).nature(nature));
        fixture.montants(id, ht, ttc, ttc);
        return id;
    }

    private void liee(UUID bc, String suffixe, UUID societe, UUID projet, String ht, String ttc) {
        fixture.poser(piece(suffixe, FACTURE, societe, projet, ht, ttc), BUY_ORDER_ID, bc);
    }

    @Test
    void cas10_bcHorsPerimetre_absentDeLaListe_bcVisibleAvecSesAgregats() throws Exception {
        String json = corps(mockMvc, liste(qui(), AppelsImpayes.NATURES, "ORDER"));

        assertAll(
                () -> assertThat(codes(json)).contains(code("-BC-V"))
                        .doesNotContain(code("-BC-X"), code("-BC-PX")),
                () -> assertThat(montantAgregat(json, code("-BC-V"), "factureHT")).isEqualByComparingTo("800"));
    }

    @Test
    void cas10_factureLieeDUneAutreSocieteEtDUnProjetNonHabilite_compteDansLeBc() throws Exception {
        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(codes(json)).doesNotContain(code("-BC-V-F2"), code("-BC-V-FP")),
                () -> assertThat(montantAgregat(json, code("-BC-V"), "factureTTC")).isEqualByComparingTo("960"),
                () -> assertThat(montantAgregat(json, code("-BC-V"), "resteAPayerTotal")).isEqualByComparingTo("1200"));
    }

    @Test
    void cas10_totaux_bcHorsPerimetreExclus() throws Exception {
        String totaux = corps(mockMvc, appel(TOTAUX, qui(), AppelsImpayes.NATURES, "ORDER"));

        assertAll(
                () -> assertThat(nombre(totaux, "$.nombre")).isEqualByComparingTo("1"),
                () -> assertThat(nombre(totaux, "$.agregats.factureHT")).isEqualByComparingTo("800"),
                () -> assertThat(nombre(totaux, "$.agregats.resteALivrerHT")).isEqualByComparingTo("1000"));
    }
}
