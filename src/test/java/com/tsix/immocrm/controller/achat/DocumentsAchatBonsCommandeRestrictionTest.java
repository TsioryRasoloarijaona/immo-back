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
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NATURES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.TOTAUX;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot C8 — FEATURE-achat-liste-parite.md, réponse du manager Q1 (règle 6, visibilité) : liste des BC
 * ({@code natures=ORDER} seul) restreinte à {@code CfgTierTriggerId} = personnel du compte ({@code AdmUser.CfgEmployeeId},
 * {@code OrderView.cs:803}), sauf droit OMRAN {@code …OrderList.WithNoRestriction} ; compte sans personnel = aucune restriction
 * (2ᵉ passe) ; seulement si {@code natures} vaut exactement [ORDER].
 * <pre>
 *   société S (habilitée) : BC-MOI (déclencheur = mon personnel), BC-AUTRE (autre personnel), BC-SANS (aucun),
 *                           FAC-AUTRE (facture, autre personnel)
 * </pre>
 * Rôle non Administrateur : {@code DroitsOmran} donne tous les droits à l'Administrateur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatBonsCommandeRestrictionTest {

    private static final String ROLE = "Commercial";
    private static final String ORDER = "ORDER";
    private static final String TRIGGER = "CfgTierTriggerId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ABC");
        UUID societe = fixture.societe("SOC-" + jeton);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        UUID autrePersonnel = fixture.fournisseur("EMP2-" + jeton, "Autre personnel " + jeton);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        UUID monPersonnel = compte.getCfgEmployee().getId();

        fixture.poser(piece("-BC-MOI", ORDER, fournisseur, nom, societe), TRIGGER, monPersonnel);
        fixture.poser(piece("-BC-AUTRE", ORDER, fournisseur, nom, societe), TRIGGER, autrePersonnel);
        piece("-BC-SANS", ORDER, fournisseur, nom, societe);
        fixture.poser(piece("-FAC-AUTRE", FixtureAchat.FACTURE, fournisseur, nom, societe), TRIGGER, autrePersonnel);
    }

    private UUID piece(String suffixe, String nature, UUID fournisseur, String nom, UUID societe) {
        return fixture.piece(Piece.facture(jeton + suffixe, LocalDate.of(2026, 3, 10), fournisseur, nom, societe, null,
                new BigDecimal("100")).nature(nature));
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    @Test
    void q1_sansDroit_seulementLesBcDontJeSuisLeDeclencheur() throws Exception {
        String json = corps(mockMvc, liste(qui(), NATURES, ORDER));

        assertThat(codes(json)).containsExactly(code("-BC-MOI"));
    }

    @Test
    void q1_avecDroitWithNoRestriction_tousLesBcDuPerimetre() throws Exception {
        fixture.droitOmran(compte, FixtureAchat.DROIT_BC_SANS_RESTRICTION);

        String json = corps(mockMvc, liste(qui(), NATURES, ORDER));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-BC-MOI"), code("-BC-AUTRE"), code("-BC-SANS"));
    }

    /** 2ᵉ passe : sans personnel, aucune restriction, comme OMRAN. */
    @Test
    void q1_sansPersonnelNiDroit_aucuneRestriction_tousLesBcDuPerimetre() throws Exception {
        fixture.sansPersonnel(compte);

        String json = corps(mockMvc, liste(qui(), NATURES, ORDER));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-BC-MOI"), code("-BC-AUTRE"), code("-BC-SANS"));
    }

    /** 2ᵉ passe : restriction seulement pour {@code natures} = [ORDER] exactement. */
    @Test
    void q1_naturesAbsentes_pasDeRestriction() throws Exception {
        String json = corps(mockMvc, liste(qui()));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-BC-MOI"), code("-BC-AUTRE"), code("-BC-SANS"),
                code("-FAC-AUTRE"));
    }

    @Test
    void q1_naturesOrderEtInvoice_pasDeRestriction() throws Exception {
        String json = corps(mockMvc, liste(qui(), NATURES, ORDER, NATURES, FixtureAchat.FACTURE));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-BC-MOI"), code("-BC-AUTRE"), code("-BC-SANS"),
                code("-FAC-AUTRE"));
    }

    @Test
    void q1_sansPersonnelAvecDroit_tousLesBcDuPerimetre() throws Exception {
        fixture.sansPersonnel(compte);
        fixture.droitOmran(compte, FixtureAchat.DROIT_BC_SANS_RESTRICTION);

        String json = corps(mockMvc, liste(qui(), NATURES, ORDER));

        assertThat(codes(json)).containsExactlyInAnyOrder(code("-BC-MOI"), code("-BC-AUTRE"), code("-BC-SANS"));
    }

    @Test
    void q1_unAutreDroitNeLevePasLaRestriction() throws Exception {
        fixture.droitOmran(compte, "Yes.Buy.Front.Order.OrderList.toolNew");

        String json = corps(mockMvc, liste(qui(), NATURES, ORDER));

        assertThat(codes(json)).containsExactly(code("-BC-MOI"));
    }

    @Test
    void q1_autresNatures_nonTouchees() throws Exception {
        String factures = corps(mockMvc, liste(qui(), NATURES, FixtureAchat.FACTURE));

        assertThat(codes(factures)).containsExactly(code("-FAC-AUTRE"));
    }

    @Test
    void q1_autresNatures_nonTouchees_memeSansPersonnel() throws Exception {
        fixture.sansPersonnel(compte);

        String factures = corps(mockMvc, liste(qui(), NATURES, FixtureAchat.FACTURE));

        assertThat(codes(factures)).containsExactly(code("-FAC-AUTRE"));
    }

    @Test
    void q1_totaux_memeRegle() throws Exception {
        String sansDroit = corps(mockMvc, appel(TOTAUX, qui(), NATURES, ORDER));
        fixture.droitOmran(compte, FixtureAchat.DROIT_BC_SANS_RESTRICTION);
        String avecDroit = corps(mockMvc, appel(TOTAUX, qui(), NATURES, ORDER));

        assertAll(
                () -> assertThat(nombre(sansDroit, "$.nombre")).isEqualByComparingTo("1"),
                () -> assertThat(nombre(sansDroit, "$.montantAPayer")).isEqualByComparingTo("100"),
                () -> assertThat(nombre(avecDroit, "$.nombre")).isEqualByComparingTo("3"));
    }

    @Test
    void q1_totaux_sansPersonnelNiDroit_tousLesBc() throws Exception {
        fixture.sansPersonnel(compte);

        String totaux = corps(mockMvc, appel(TOTAUX, qui(), NATURES, ORDER));

        assertThat(nombre(totaux, "$.nombre")).isEqualByComparingTo("3");
    }

    @Test
    void q1_export_memeRegle() throws Exception {
        List<String> textes = AppelsAchat.textesExport(mockMvc, get(AppelsAchat.EXPORT).param("format", "xlsx")
                .param(NATURES, ORDER).with(qui()));

        assertThat(textes).contains(code("-BC-MOI")).doesNotContain(code("-BC-AUTRE"), code("-BC-SANS"));
    }
}
