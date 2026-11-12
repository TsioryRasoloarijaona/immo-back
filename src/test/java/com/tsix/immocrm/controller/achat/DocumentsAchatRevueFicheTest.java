package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
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

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.textesExport;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revue A2a (règle 6) : références d'en-tête vers une pièce non visible (autre déclencheur, hors périmètre) à null
 * (réponse du manager Q6), et restriction BC sur les totaux et l'export de Documents Achats (réponse Q5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatRevueFicheTest {

    private static final String ROLE = "Commercial";
    private static final String TRIGGER = "CfgTierTriggerId";
    private static final String BON_COMMANDE_ID = "$.bonCommandeId";
    private static final String BON_COMMANDE_CODE = "$.bonCommandeCode";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private AdmUser restreint;
    private AdmUser autre;
    private UUID bcAutre;
    private UUID blDuBcAutre;
    private UUID factureDuBcHorsProjet;
    private UUID avoirDeFactureHorsProjet;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ARF");
        UUID societe = fixture.societe("SOC-" + jeton);
        UUID projet = fixture.projet("P-" + jeton, societe);
        UUID projetNonHabilite = fixture.projet("P2-" + jeton, societe);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = fixture.fournisseur("FOU-" + jeton, nom);

        restreint = fixture.compte("ACH-A-" + jeton, ROLE);
        autre = fixture.compte("ACH-B-" + jeton, ROLE);
        for (AdmUser c : List.of(restreint, autre)) {
            fixture.habiliterSociete(c, societe);
            fixture.habiliter(c, projet);
        }
        UUID personnelA = restreint.getCfgEmployee().getId();
        UUID personnelB = autre.getCfgEmployee().getId();

        UUID bcA = piece("-BC-A", FixtureAchat.BON_COMMANDE, fournisseur, nom, societe, projet);
        fixture.poser(bcA, TRIGGER, personnelA);
        bcAutre = piece("-BC-B", FixtureAchat.BON_COMMANDE, fournisseur, nom, societe, projet);
        fixture.poser(bcAutre, TRIGGER, personnelB);
        blDuBcAutre = piece("-BL-B", FixtureAchat.BON_LIVRAISON, fournisseur, nom, societe, projet);
        fixture.poser(blDuBcAutre, "BuyOrderId", bcAutre);

        UUID bcHorsProjet = piece("-BC-HP", FixtureAchat.BON_COMMANDE, fournisseur, nom, societe, projetNonHabilite);
        fixture.poser(bcHorsProjet, TRIGGER, personnelA);
        factureDuBcHorsProjet = piece("-FAC", FixtureAchat.FACTURE, fournisseur, nom, societe, projet);
        fixture.poser(factureDuBcHorsProjet, "BuyOrderId", bcHorsProjet);

        UUID factureHorsProjet = piece("-FAC-HP", FixtureAchat.FACTURE, fournisseur, nom, societe, projetNonHabilite);
        avoirDeFactureHorsProjet = piece("-AV", "REFUND", fournisseur, nom, societe, projet);
        fixture.poser(avoirDeFactureHorsProjet, "InvoiceSourceId", factureHorsProjet);
    }

    private UUID piece(String suffixe, String nature, UUID fournisseur, String nom, UUID societe, UUID projet) {
        return fixture.piece(Piece.facture(jeton + suffixe, LocalDate.of(2026, 3, 10), fournisseur, nom, societe,
                projet, new BigDecimal("100")).nature(nature));
    }

    private RequestPostProcessor qui(AdmUser c) {
        return connecte(c, ROLE);
    }

    private String fiche(AdmUser c, UUID id) throws Exception {
        return corps(mockMvc, get(AppelsAchat.DOCUMENT, id).with(qui(c)));
    }

    /** Q6 : BL visible dont le BC est celui d'un autre déclencheur ⇒ id et code du BC à null, le BL reste 200. */
    @Test
    void q6_bonLivraisonVisible_bcDUnAutreDeclencheur_referenceNulle() throws Exception {
        String pourA = fiche(restreint, blDuBcAutre);
        String pourB = fiche(autre, blDuBcAutre);

        assertAll(
                () -> assertThat((Object) JsonPath.read(pourA, BON_COMMANDE_ID)).isNull(),
                () -> assertThat((Object) JsonPath.read(pourA, BON_COMMANDE_CODE)).isNull(),
                () -> assertThat((Object) JsonPath.read(pourB, BON_COMMANDE_ID)).isEqualTo(bcAutre.toString()),
                () -> assertThat((Object) JsonPath.read(pourB, BON_COMMANDE_CODE)).isEqualTo(jeton + "-BC-B"));
    }

    /** Q6 : facture visible rattachée à un BC d'un projet non habilité ⇒ référence nulle, même pour son déclencheur. */
    @Test
    void q6_factureVisible_bcHorsProjet_referenceNulle() throws Exception {
        String json = fiche(restreint, factureDuBcHorsProjet);

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, BON_COMMANDE_ID)).isNull(),
                () -> assertThat((Object) JsonPath.read(json, BON_COMMANDE_CODE)).isNull());
    }

    /** Q6 : avoir visible dont la facture source est hors projet ⇒ factureSource* à null. */
    @Test
    void q6_avoirVisible_factureSourceHorsProjet_referenceNulle() throws Exception {
        String json = fiche(restreint, avoirDeFactureHorsProjet);

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.factureSourceId")).isNull(),
                () -> assertThat((Object) JsonPath.read(json, "$.factureSourceCode")).isNull());
    }

    /** Q5 : totaux et export de Documents Achats (toutes natures) suivent la restriction BC de la liste. */
    @Test
    void q5_totauxEtExport_documentsAchats_bcDUnAutreExclu() throws Exception {
        String liste = corps(mockMvc, AppelsAchat.liste(qui(restreint)));
        String totaux = corps(mockMvc, get(AppelsAchat.DOCUMENTS + "/totaux").with(qui(restreint)));
        List<String> export = textesExport(mockMvc, get(AppelsAchat.EXPORT).with(qui(restreint)));

        assertAll(
                () -> assertThat(AppelsAchat.codes(liste)).doesNotContain(jeton + "-BC-B").contains(jeton + "-BC-A"),
                () -> assertThat(((Number) JsonPath.read(totaux, "$.nombre")).longValue())
                        .isEqualTo(AppelsAchat.total(liste)),
                () -> assertThat(export).doesNotContain(jeton + "-BC-B").contains(jeton + "-BC-A"));
    }
}
