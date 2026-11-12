package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.fixtures.FixtureAchat.AVANCE_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.DOCUMENT_A_REGLER;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureAchat.GARANTIE_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot A1 bis — FEATURE-achat-bc-agregats.md E2 (25 clés de tri, non-BC en fin), E4 (export) et « Cas limites » n° 12.
 * Deux BC dont chaque agrégat est strictement plus petit sur A que sur B ; les pièces liées (non-BC) sont dans la même
 * liste, agrégats nuls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatAgregatsTrisTest {

    private static final String ROLE = "Commercial";
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String REFERENCE_TYPE = "ReferenceType";
    private static final LocalDate MARS_10 = LocalDate.of(2026, 3, 10);
    private static final LocalDate MARS_20 = LocalDate.of(2026, 3, 20);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID fournisseur;
    private String nom;
    private UUID typeReglement;
    private AdmUser compte;
    private UUID bcA;

    /** Montants d'un BC et de ses pièces liées. */
    private record Jeu(String bcHt, String bcTtc, String ratio, String fHt, String fTtc, String fAPayer,
                       String fAvance, String fGarantie, String fRegle, String blHt, String blTtc, String avAPayer,
                       String avRegle, String restitutionHt) {
    }

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ABS");
        societe = fixture.societe("SOC-" + jeton);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        typeReglement = fixture.typeReglement("VIR-" + jeton);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);

        bcA = bonDeCommande("-A", new Jeu("100", "120", "0.1", "10", "12", "12", "1", "1", "1", "20", "24", "6", "2",
                "0.5"));
        bonDeCommande("-B", new Jeu("1000", "1200", "0.3", "500", "600", "600", "5", "9", "10", "200", "240", "600",
                "300", "1"));
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    private UUID piece(String suffixe, String nature, String ht, String ttc, String aPayer) {
        UUID id = fixture.piece(Piece.facture(code(suffixe), MARS_10, fournisseur, nom, societe, null,
                new BigDecimal(aPayer)).nature(nature));
        fixture.montants(id, ht, ttc, aPayer);
        return id;
    }

    private UUID liee(UUID bc, String suffixe, String nature, String ht, String ttc, String aPayer) {
        UUID id = piece(suffixe, nature, ht, ttc, aPayer);
        fixture.poser(id, BUY_ORDER_ID, bc);
        return id;
    }

    private void regler(UUID pieceId, String suffixe, String montant) {
        fixture.reglement(pieceId, "RG" + suffixe + jeton, new BigDecimal(montant), FixtureAchat.EN_COURS, MARS_20,
                typeReglement);
    }

    private UUID bonDeCommande(String s, Jeu j) {
        UUID bc = piece(s, BON_COMMANDE, j.bcHt(), j.bcTtc(), j.bcTtc());
        fixture.poser(bc, "OrderAdvanceRatio", new BigDecimal(j.ratio()));
        UUID devis = piece(s + "-DV", FixtureAchat.DEVIS, j.bcHt(), j.bcTtc(), j.bcTtc());
        fixture.poser(devis, "DocRef", "D" + s + "-" + jeton);
        fixture.poser(bc, "BuyQuoteId", devis);
        UUID f = liee(bc, s + "-F", FACTURE, j.fHt(), j.fTtc(), j.fAPayer());
        fixture.poser(f, "AdvanceAmount", new BigDecimal(j.fAvance()));
        fixture.poser(f, "WarrantyAmount", new BigDecimal(j.fGarantie()));
        regler(f, s + "F", j.fRegle());
        liee(bc, s + "-BL", BON_LIVRAISON, j.blHt(), j.blTtc(), j.blTtc());
        UUID avance = liee(bc, s + "-AV", DOCUMENT_A_REGLER, "0", j.avAPayer(), j.avAPayer());
        fixture.poser(avance, REFERENCE_TYPE, AVANCE_COMMANDE);
        regler(avance, s + "AV", j.avRegle());
        UUID restitution = liee(bc, s + "-RG", DOCUMENT_A_REGLER, j.restitutionHt(), j.restitutionHt(),
                j.restitutionHt());
        fixture.poser(restitution, REFERENCE_TYPE, GARANTIE_COMMANDE);
        return bc;
    }

    @ParameterizedTest(name = "tri {0}")
    @ValueSource(strings = {"factureHT", "factureTTC", "factureNetAPayer", "livreHT", "livreTTC", "livreNetAPayer",
            "resteALivrerHT", "resteALivrerTTC", "resteALivrerNetAPayer", "avance", "avanceTauxPrevu",
            "avanceTauxEffectif", "avanceAmortie", "avanceRegle", "avanceReste", "avanceResteAAmortir",
            "garantieRetenue", "garantieRestituee", "garantieReste", "regle", "resteAPayerSurFacture",
            "resteAPayerTotal", "bonsLivraisonCodes", "facturesCodes", "devisCode"})
    void cas12_nouvelleCleDeTri_ascendant_bcDansLOrdre_nonBcEnFin(String cle) throws Exception {
        String json = corps(mockMvc, liste(qui(), "sort", cle + ",asc"));

        List<String> ordre = codes(json);
        assertThat(ordre).hasSizeGreaterThan(2).startsWith(code("-A"), code("-B"));
    }

    @Test
    void e4_export_nouvellesColonnesEtCodes() throws Exception {
        liee(bcA, "-A-BL2", BON_LIVRAISON, "1", "1", "1");
        List<String> textes = AppelsAchat.textesExport(mockMvc,
                get(AppelsAchat.EXPORT).param("format", "xlsx").with(qui()));

        assertAll(
                () -> assertThat(textes).contains("Reste à livrer TTC", "Reste à payer total"),
                () -> assertThat(textes).anyMatch(t -> t.equalsIgnoreCase("Montant facturé HT")),
                () -> assertThat(textes).contains(code("-A-BL") + ", " + code("-A-BL2")));
    }
}
