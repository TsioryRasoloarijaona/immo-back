package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.AGREGATS;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.MONTANTS;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.RATIOS_ET_CODES;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.agregatsDeLaPage;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NATURES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.TOTAUX;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
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
 * Lot A1 bis — FEATURE-achat-bc-agregats.md E3 et « Cas limites » n° 9 : {@code totaux.agregats} = Σ champ à champ des
 * {@code agregats} des lignes de toutes les pages (pages de 2), mêmes filtres, même visibilité, même restriction
 * « mes BC » ; nul sans BC ; ratios et codes nuls.
 * <pre>
 *   société S1 : BC1 (déclencheur = mon personnel ; facture, BL, avance, garantie), BC2 (sans pièce), BC3 (dépassement),
 *                facture seule
 *   société S2 : BC4 + facture liée
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatAgregatsTotauxTest {

    private static final String ROLE = "Commercial";
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String REFERENCE_TYPE = "ReferenceType";
    private static final String ORDER = "ORDER";
    private static final LocalDate MARS_10 = LocalDate.of(2026, 3, 10);
    private static final LocalDate MARS_20 = LocalDate.of(2026, 3, 20);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID s1;
    private UUID s2;
    private UUID fournisseur;
    private String nom;
    private UUID typeReglement;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ABT");
        s1 = fixture.societe("S1-" + jeton);
        s2 = fixture.societe("S2-" + jeton);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        UUID autrePersonnel = fixture.fournisseur("EMP2-" + jeton, "Autre personnel " + jeton);
        typeReglement = fixture.typeReglement("VIR-" + jeton);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, s1);
        fixture.habiliterSociete(compte, s2);

        UUID bc1 = piece("-BC1", BON_COMMANDE, s1, "1000", "1200", "1200");
        fixture.poser(bc1, "CfgTierTriggerId", compte.getCfgEmployee().getId());
        fixture.poser(bc1, "OrderAdvanceRatio", new BigDecimal("0.2"));
        UUID f1 = liee(bc1, "-BC1-F", FACTURE, s1, "400", "480", "470");
        fixture.poser(f1, "AdvanceAmount", new BigDecimal("50"));
        fixture.poser(f1, "WarrantyAmount", new BigDecimal("30"));
        regler(f1, "F1", "100");
        liee(bc1, "-BC1-BL", BON_LIVRAISON, s1, "300", "360", "360");
        UUID avance = liee(bc1, "-BC1-AV", DOCUMENT_A_REGLER, s1, "200", "240", "240");
        fixture.poser(avance, REFERENCE_TYPE, AVANCE_COMMANDE);
        regler(avance, "AV", "120");
        UUID restitution = liee(bc1, "-BC1-RG", DOCUMENT_A_REGLER, s1, "10", "12", "12");
        fixture.poser(restitution, REFERENCE_TYPE, GARANTIE_COMMANDE);

        fixture.poser(piece("-BC2", BON_COMMANDE, s1, "500", "600", "600"), "CfgTierTriggerId", autrePersonnel);
        UUID bc3 = piece("-BC3", BON_COMMANDE, s1, "2000", "2400", "2400");
        regler(liee(bc3, "-BC3-F", FACTURE, s1, "2500", "3000", "3000"), "F3", "3000");
        piece("-SEULE", FACTURE, s1, "100", "120", "120");

        UUID bc4 = piece("-BC4", BON_COMMANDE, s2, "100", "120", "120");
        liee(bc4, "-BC4-F", FACTURE, s2, "40", "48", "48");
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private UUID piece(String suffixe, String nature, UUID societe, String ht, String ttc, String aPayer) {
        UUID id = fixture.piece(Piece.facture(jeton + suffixe, MARS_10, fournisseur, nom, societe, null,
                new BigDecimal(aPayer)).nature(nature));
        fixture.montants(id, ht, ttc, aPayer);
        return id;
    }

    private UUID liee(UUID bc, String suffixe, String nature, UUID societe, String ht, String ttc, String aPayer) {
        UUID id = piece(suffixe, nature, societe, ht, ttc, aPayer);
        fixture.poser(id, BUY_ORDER_ID, bc);
        return id;
    }

    private void regler(UUID pieceId, String suffixe, String montant) {
        fixture.reglement(pieceId, "RG" + suffixe + jeton, new BigDecimal(montant), FixtureAchat.EN_COURS, MARS_20,
                typeReglement);
    }

    private static MockHttpServletRequestBuilder page(RequestPostProcessor qui, int numero, String... params) {
        MockHttpServletRequestBuilder requete = get(AppelsAchat.DOCUMENTS).param("size", "2")
                .param("page", String.valueOf(numero)).with(qui);
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete.param(params[i], params[i + 1]);
        }
        return requete;
    }

    /** Σ par champ monétaire des {@code agregats} de toutes les pages, et le nombre de lignes BC vues. */
    private Map<String, BigDecimal> sommeDesPages(RequestPostProcessor qui, String... params) throws Exception {
        Map<String, BigDecimal> sommes = new HashMap<>();
        MONTANTS.forEach(m -> sommes.put(m, BigDecimal.ZERO));
        String premiere = corps(mockMvc, page(qui, 0, params));
        int pages = ((Number) JsonPath.read(premiere, "$.totalPages")).intValue();
        int bc = 0;
        for (int n = 0; n < pages; n++) {
            String json = n == 0 ? premiere : corps(mockMvc, page(qui, n, params));
            for (Map<String, Object> agregats : agregatsDeLaPage(json)) {
                bc++;
                for (String m : MONTANTS) {
                    sommes.merge(m, new BigDecimal(String.valueOf(agregats.get(m))), BigDecimal::add);
                }
            }
        }
        sommes.put("bc", BigDecimal.valueOf(bc));
        return sommes;
    }

    private void verifierInvariant(RequestPostProcessor qui, String... params) throws Exception {
        Map<String, BigDecimal> lignes = sommeDesPages(qui, params);
        String totaux = corps(mockMvc, appel(TOTAUX, qui, params));

        assertThat(lignes.get("bc")).as("le jeu doit contenir au moins un BC").isPositive();
        List<Executable> verifs = new ArrayList<>();
        for (String m : MONTANTS) {
            verifs.add(() -> assertThat(nombre(totaux, "$.agregats." + m)).as(m).isEqualByComparingTo(lignes.get(m)));
        }
        assertAll(verifs);
    }

    private static Map<String, Object> agregatsDesTotaux(String totaux) {
        Map<String, Object> racine = JsonPath.read(totaux, "$");
        assertThat(racine).containsKey(AGREGATS);
        Map<String, Object> agregats = JsonPath.read(totaux, "$." + AGREGATS);
        assertThat(agregats).isNotNull();
        return agregats;
    }

    // ------------------------------------------------------------------ invariant Σ lignes = totaux

    @ParameterizedTest(name = "jeu {0}")
    @ValueSource(strings = {"mixte", "bc", "societe"})
    void cas9_totauxAgregatsEgauxSommeDesLignesDeToutesLesPages(String nomJeu) throws Exception {
        String[] params = switch (nomJeu) {
            case "bc" -> new String[]{NATURES, ORDER};
            case "societe" -> new String[]{"societeIds", s2.toString()};
            default -> new String[0];
        };
        fixture.sansPersonnel(compte);

        verifierInvariant(qui(), params);
    }

    @Test
    void cas9_restrictionMesBc_totauxCommeLaListe() throws Exception {
        verifierInvariant(qui(), NATURES, ORDER);

        String totaux = corps(mockMvc, appel(TOTAUX, qui(), NATURES, ORDER));

        assertAll(
                () -> assertThat(nombre(totaux, "$.agregats.factureHT")).isEqualByComparingTo("400"),
                () -> assertThat(nombre(totaux, "$.agregats.resteAPayerTotal")).isEqualByComparingTo("980"),
                () -> assertThat(nombre(totaux, "$.agregats.resteALivrerHT")).isEqualByComparingTo("700"));
    }

    @Test
    void cas9_valeursAttendues_sansFiltre() throws Exception {
        String totaux = corps(mockMvc, appel(TOTAUX, qui()));

        assertAll(
                () -> assertThat(nombre(totaux, "$.agregats.factureHT")).isEqualByComparingTo("2940"),
                () -> assertThat(nombre(totaux, "$.agregats.factureNetAPayer")).isEqualByComparingTo("3518"),
                () -> assertThat(nombre(totaux, "$.agregats.livreHT")).isEqualByComparingTo("300"),
                () -> assertThat(nombre(totaux, "$.agregats.resteALivrerHT")).isEqualByComparingTo("3300"),
                () -> assertThat(nombre(totaux, "$.agregats.avance")).isEqualByComparingTo("240"),
                () -> assertThat(nombre(totaux, "$.agregats.avanceRegle")).isEqualByComparingTo("120"),
                () -> assertThat(nombre(totaux, "$.agregats.avanceResteAAmortir")).isEqualByComparingTo("190"),
                () -> assertThat(nombre(totaux, "$.agregats.garantieReste")).isEqualByComparingTo("20"),
                () -> assertThat(nombre(totaux, "$.agregats.regle")).isEqualByComparingTo("3220"),
                () -> assertThat(nombre(totaux, "$.agregats.resteAPayerTotal")).isEqualByComparingTo("1100"));
    }

    @Test
    void e3_ratiosEtCodesNulsDansLesTotaux() throws Exception {
        String totaux = corps(mockMvc, appel(TOTAUX, qui()));

        Map<String, Object> agregats = agregatsDesTotaux(totaux);

        List<Executable> verifs = new ArrayList<>();
        for (String champ : RATIOS_ET_CODES) {
            verifs.add(() -> assertThat(agregats).as(champ).containsEntry(champ, null));
        }
        assertAll(verifs);
    }

    @Test
    void cas9_jeuSansBc_agregatsNul() throws Exception {
        String totaux = corps(mockMvc, appel(TOTAUX, qui(), NATURES, FACTURE));

        Map<String, Object> racine = JsonPath.read(totaux, "$");
        assertThat(racine).containsEntry(AGREGATS, null);
    }
}
