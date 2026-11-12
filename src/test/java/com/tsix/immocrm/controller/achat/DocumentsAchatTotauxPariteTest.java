package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.total;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.TOTAUX;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot C8 — FEATURE-achat-liste-parite.md E4, Vérification n° 4 : pour un jeu de filtres, Σ des lignes de toutes les
 * pages de la liste (pages de 2) = {@code /totaux} (nombre, HT, TVA, TTC, à payer, réglé, reste), même périmètre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatTotauxPariteTest {

    private static final String ROLE = "Commercial";
    private static final List<String> MONTANTS = List.of("montantHT", "montantTVA", "montantTTC", "montantAPayer",
            "montantRegle", "resteARegler");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID s1;
    private UUID s2;
    private AdmUser compte;
    private UUID type;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ATP");
        s1 = fixture.societe("S1-" + jeton);
        s2 = fixture.societe("S2-" + jeton);
        UUID projet = fixture.projet("P-" + jeton, s1);
        UUID horsPerimetre = fixture.projet("PX-" + jeton, s1);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        type = fixture.typeLot("TRV-" + jeton, "TRAVAUX " + jeton);
        UUID typeReglement = fixture.typeReglement("VIR-" + jeton);
        UUID achat = fixture.module("BUY");
        UUID chantier = fixture.module("PRJ");

        UUID p1 = fixture.piece(piece("-1", fournisseur, nom, s1, projet, "1200"));
        fixture.poser(p1, "AdmModuleId", achat);
        fixture.poser(p1, "PrjMarketTypeId", type);
        fixture.reglement(p1, "RG1-" + jeton, new BigDecimal("400"), FixtureAchat.EN_COURS, LocalDate.of(2026, 3, 20),
                typeReglement);
        UUID p2 = fixture.piece(piece("-2", fournisseur, nom, s1, null, "600").nature(FixtureAchat.DOCUMENT_A_REGLER));
        fixture.poser(p2, "AdmModuleId", achat);
        fixture.poser(p2, "IsClosed", true);
        UUID p3 = fixture.piece(piece("-3", fournisseur, nom, s2, null, "-240").nature(FixtureAchat.DOCUMENT_A_REGLER));
        fixture.poser(p3, "AdmModuleId", chantier);
        fixture.poser(p3, "IsAccounted", true);
        UUID p4 = fixture.piece(piece("-4", fournisseur, nom, s2, null, "360"));
        fixture.reglement(p4, "RG4-" + jeton, new BigDecimal("360"), FixtureAchat.EN_COURS, LocalDate.of(2026, 3, 21),
                typeReglement);
        fixture.piece(piece("-5", fournisseur, nom, s1, null, "840").nature(FixtureAchat.AVOIR));
        fixture.piece(piece("-X", fournisseur, nom, s1, horsPerimetre, "9999"));

        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, s1);
        fixture.habiliterSociete(compte, s2);
        fixture.habiliter(compte, projet);
    }

    private Piece piece(String suffixe, UUID fournisseur, String nom, UUID societe, UUID projet, String aPayer) {
        return Piece.facture(jeton + suffixe, LocalDate.of(2026, 3, 10), fournisseur, nom, societe, projet,
                new BigDecimal(aPayer));
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    /** Jeu de filtres nommé → paires nom / valeur. */
    private String[] jeu(String nomJeu) {
        return switch (nomJeu) {
            case "societe" -> new String[]{"societeIds", s2.toString()};
            case "module" -> new String[]{"modules", "BUY"};
            case "type" -> new String[]{"typeLotIds", type.toString()};
            case "archive" -> new String[]{"archive", "false"};
            case "comptabilise" -> new String[]{"comptabilise", "true"};
            case "montants" -> new String[]{"montantDe", "-240", "montantA", "600"};
            case "echeance" -> new String[]{"champDate", "ECHEANCE", "du", "2026-04-09", "au", "2026-04-09"};
            default -> new String[0];
        };
    }

    private static MockHttpServletRequestBuilder page(RequestPostProcessor qui, int numero, String... params) {
        MockHttpServletRequestBuilder requete = get(AppelsAchat.DOCUMENTS).param("size", "2")
                .param("page", String.valueOf(numero)).with(qui);
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete.param(params[i], params[i + 1]);
        }
        return requete;
    }

    /** Σ par montant de toutes les pages, plus le nombre de lignes vues sous la clé « lignes ». */
    private Map<String, BigDecimal> sommeDesPages(RequestPostProcessor qui, String... params) throws Exception {
        Map<String, BigDecimal> sommes = new HashMap<>();
        MONTANTS.forEach(m -> sommes.put(m, BigDecimal.ZERO));
        sommes.put("lignes", BigDecimal.ZERO);
        String premiere = corps(mockMvc, page(qui, 0, params));
        int pages = ((Number) JsonPath.read(premiere, "$.totalPages")).intValue();
        for (int n = 0; n < pages; n++) {
            String json = n == 0 ? premiere : corps(mockMvc, page(qui, n, params));
            List<Object> codes = JsonPath.read(json, "$.content[*].code");
            sommes.merge("lignes", BigDecimal.valueOf(codes.size()), BigDecimal::add);
            for (String m : MONTANTS) {
                List<Object> valeurs = JsonPath.read(json, "$.content[*]." + m);
                for (Object v : valeurs) {
                    sommes.merge(m, new BigDecimal(String.valueOf(v)), BigDecimal::add);
                }
            }
        }
        sommes.put("totalElements", BigDecimal.valueOf(total(premiere)));
        return sommes;
    }

    private void verifierInvariant(RequestPostProcessor qui, String... params) throws Exception {
        Map<String, BigDecimal> lignes = sommeDesPages(qui, params);
        String totaux = corps(mockMvc, appel(TOTAUX, qui, params));

        assertAll(
                () -> assertThat(lignes.get("lignes")).isEqualByComparingTo(lignes.get("totalElements")),
                () -> assertThat(nombre(totaux, "$.nombre")).isEqualByComparingTo(lignes.get("totalElements")),
                () -> assertThat(nombre(totaux, "$.montantHT")).isEqualByComparingTo(lignes.get("montantHT")),
                () -> assertThat(nombre(totaux, "$.montantTVA")).isEqualByComparingTo(lignes.get("montantTVA")),
                () -> assertThat(nombre(totaux, "$.montantTTC")).isEqualByComparingTo(lignes.get("montantTTC")),
                () -> assertThat(nombre(totaux, "$.montantAPayer")).isEqualByComparingTo(lignes.get("montantAPayer")),
                () -> assertThat(nombre(totaux, "$.montantRegle")).isEqualByComparingTo(lignes.get("montantRegle")),
                () -> assertThat(nombre(totaux, "$.resteARegler")).isEqualByComparingTo(lignes.get("resteARegler")));
    }

    @ParameterizedTest(name = "jeu {0}")
    @ValueSource(strings = {"aucun", "societe", "module", "type", "archive", "comptabilise", "montants", "echeance"})
    void v4_totauxEgauxSommeDesLignesDeToutesLesPages(String nomJeu) throws Exception {
        verifierInvariant(qui(), jeu(nomJeu));
    }

    @Test
    void v4_totaux_valeursAttenduesSansFiltre_horsPerimetreExclu() throws Exception {
        String totaux = corps(mockMvc, appel(TOTAUX, qui()));

        assertAll(
                () -> assertThat(nombre(totaux, "$.nombre")).isEqualByComparingTo("5"),
                () -> assertThat(nombre(totaux, "$.montantHT")).isEqualByComparingTo("2300"),
                () -> assertThat(nombre(totaux, "$.montantTVA")).isEqualByComparingTo("460"),
                () -> assertThat(nombre(totaux, "$.montantAPayer")).isEqualByComparingTo("2760"));
    }

    @Test
    void v4_memesFiltresDeVisibilite_compteEtroit() throws Exception {
        AdmUser etroit = fixture.compte("ACH-E-" + jeton, ROLE);
        fixture.habiliterSociete(etroit, s2);

        verifierInvariant(connecte(etroit, ROLE));
        String totaux = corps(mockMvc, appel(TOTAUX, connecte(etroit, ROLE)));
        assertThat(nombre(totaux, "$.nombre")).isEqualByComparingTo("2");
    }
}
