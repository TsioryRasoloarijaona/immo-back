package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureEtatsAchat;
import com.tsix.immocrm.fixtures.LectureExport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.RETENUES;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.RETENUES_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.TVA;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.TVA_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.annee;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.classeur;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureEtatsAchat.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Exports xlsx des états — FEATURE-n7-etats-achat.md D5, R9 (« export = mêmes lignes + ligne Total »), Vérification
 * « ligne Total = totaux du JSON ».
 * <p>Fixture (2093, une société) : factures F1 à lignes (HT 1 800 + 500, TVA 360 + 70), F2 sans ligne (1 000 / 200),
 * F3 annulée (500 / 100) → TVA 630, HT 3 300, 4 lignes ; pièces à régler : amortissement 500, avance B601 1 000 ajustement 50,
 * avance C004 annulée 700 → avance 1 000, ajustement 50, amortissement 500, 3 lignes ; retenue 250, restitution B602
 * 300 pénalité 20, retenue annulée 999 → retenue 250, pénalité 20, remboursé 300, 3 lignes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtatsAchatExportTest {

    private static final String TOTAL = "Total";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureEtatsAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("N7X");
        societe = achat.societe("SOC-" + jeton);
        projet = achat.projet("PRJ-" + jeton, societe);
        nom = "Fournisseur EXP " + jeton;
        fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        UUID annule = fixture.statutAnnule("ANN-" + jeton);
        compte = fixture.compte("N7X-" + jeton, societe, projet, FixtureEtatsAchat.TOUS_LES_DROITS);

        UUID f1 = piece("-F1", FixtureAchat.FACTURE, null, "2760");
        fixture.ligne(f1, 1, "2", "1000", "0.10", "0.20");
        fixture.ligne(f1, 2, "1", "500", "0", "0.14");
        piece("-F2", FixtureAchat.FACTURE, null, "1200");
        piece("-F3", FixtureAchat.FACTURE, annule, "600");

        fixture.avance(piece("-A1", FixtureAchat.DOCUMENT_A_REGLER, null, "500"), "500", "0");
        UUID a2 = piece("-A2", FixtureAchat.DOCUMENT_A_REGLER, null, "1000");
        fixture.nature(a2, FixtureEtatsAchat.AVANCE_MARCHE);
        fixture.regularisation(a2, "50");
        fixture.nature(piece("-A3", FixtureAchat.DOCUMENT_A_REGLER, annule, "700"), FixtureEtatsAchat.AVANCE_COMMANDE);

        fixture.retenue(piece("-R1", FixtureAchat.DOCUMENT_A_REGLER, null, "250"), "250");
        UUID r2 = piece("-R2", FixtureAchat.DOCUMENT_A_REGLER, null, "300");
        fixture.nature(r2, FixtureEtatsAchat.RESTITUTION_MARCHE);
        fixture.regularisation(r2, "20");
        fixture.retenue(piece("-R3", FixtureAchat.DOCUMENT_A_REGLER, annule, "999"), "999");
    }

    private UUID piece(String suffixe, String nature, UUID statut, String aPayer) {
        return fixture.piece(jeton + suffixe, nature, date(8, 8), fournisseur, nom, societe, projet, statut, aPayer);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, FixtureEtatsAchat.role());
    }

    private List<List<Object>> feuille(String route) throws Exception {
        return LectureExport.lignesXlsx(classeur(mockMvc, annee(route, qui())));
    }

    private static List<Object> derniere(List<List<Object>> lignes) {
        assertThat(lignes).hasSizeGreaterThan(1);
        return lignes.get(lignes.size() - 1);
    }

    private static int nombreDeLignes(String json) {
        List<Object> lignes = JsonPath.read(json, "$.lignes");
        return lignes.size();
    }

    private static double total(String json, String champ) {
        return nombre(json, "$.totaux." + champ).doubleValue();
    }

    @Test
    void r9_exportTva_ligneTotal_egaleTotauxDuJson() throws Exception {
        String json = corps(mockMvc, annee(TVA, qui()));
        List<List<Object>> lignes = feuille(TVA_EXPORT);

        assertAll(
                () -> assertThat(total(json, "montantTva")).isEqualTo(630.0),
                () -> assertThat(derniere(lignes)).contains(TOTAL, total(json, "montantTva"), total(json, "montantHT")),
                () -> assertThat(lignes).hasSize(nombreDeLignes(json) + 2));
    }

    @Test
    void r9_exportAvances_ligneTotal_egaleTotauxDuJson() throws Exception {
        String json = corps(mockMvc, annee(AVANCES, qui()));
        List<List<Object>> lignes = feuille(AVANCES_EXPORT);

        assertAll(
                () -> assertThat(total(json, "avance")).isEqualTo(1000.0),
                () -> assertThat(derniere(lignes)).contains(TOTAL, total(json, "avance"), total(json, "ajustement"),
                        total(json, "amortissement")),
                () -> assertThat(lignes).hasSize(nombreDeLignes(json) + 2));
    }

    @Test
    void r9_exportRetenues_ligneTotal_egaleTotauxDuJson() throws Exception {
        String json = corps(mockMvc, annee(RETENUES, qui()));
        List<List<Object>> lignes = feuille(RETENUES_EXPORT);

        assertAll(
                () -> assertThat(total(json, "retenue")).isEqualTo(250.0),
                () -> assertThat(derniere(lignes)).contains(TOTAL, total(json, "retenue"), total(json, "penalite"),
                        total(json, "rembourse")),
                () -> assertThat(lignes).hasSize(nombreDeLignes(json) + 2));
    }

    @Test
    void d4_exportTva_contientLaFactureAnnulee() throws Exception {
        List<String> textes = AppelsAchat.textesExport(mockMvc, annee(TVA_EXPORT, qui()));

        assertThat(textes).contains(jeton + "-F3", jeton + "-F2");
    }
}
