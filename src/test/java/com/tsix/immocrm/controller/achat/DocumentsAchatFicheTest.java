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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.montant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Fiche d'une pièce : en-tête, lignes, ventilation, historique trié, règlements avec {@code annule} —
 * FEATURE-achat-lecture-factures.md §3, §7 test 6 (+ « Réponses du manager » n° 2, 4, 5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatFicheTest {

    private static final String ROLE = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID piece;
    private String code;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AFC");
        UUID societe = fixture.societe("SOC-" + jeton);
        UUID projet = fixture.projet("P-" + jeton, societe);
        String nom = "Fournisseur " + jeton;
        code = jeton + "-FIC";
        piece = fixture.piece(Piece.facture(code, LocalDate.of(2026, 5, 4), fixture.fournisseur("FOU-" + jeton, nom),
                nom, societe, projet, new BigDecimal("1200")));
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliter(compte, projet);
    }

    private String fiche() throws Exception {
        return corps(mockMvc, get(AppelsAchat.DOCUMENT, piece).with(connecte(compte, ROLE)));
    }

    @Test
    void r6_fiche_enTete_champsDeLaLigneEtDeLaFiche() throws Exception {
        fixture.poser(piece, "Notes", "Note de la pièce " + jeton);
        fixture.poser(piece, "AmountRegul", new BigDecimal("15"));
        fixture.poser(piece, "FiscalStamp", new BigDecimal("20"));

        String json = fiche();

        assertAll(
                () -> assertThat((Object) JsonPath.read(json, "$.id")).isEqualTo(piece.toString()),
                () -> assertThat((Object) JsonPath.read(json, "$.code")).isEqualTo(code),
                () -> assertThat((Object) JsonPath.read(json, "$.nature")).isEqualTo("INVOICE"),
                () -> assertThat((Object) JsonPath.read(json, "$.notes")).isEqualTo("Note de la pièce " + jeton),
                () -> assertThat(montant(json, "$.regularisation")).isEqualByComparingTo("15"),
                () -> assertThat(montant(json, "$.timbreFiscal")).isEqualByComparingTo("20"),
                () -> assertThat((Object) JsonPath.read(json, "$.creePar")).isEqualTo("TEST-ACHAT"));
    }

    @Test
    void r6_fiche_lignes_colonnesStockees_tvaEnPourcent() throws Exception {
        fixture.ligne(piece, 2, "Ciment CPJ45 " + jeton, "SAC", new BigDecimal("40"), new BigDecimal("75.5"),
                new BigDecimal("0.2"));
        fixture.ligne(piece, 1, "Transport " + jeton, "FORFAIT", new BigDecimal("1"), new BigDecimal("300"),
                new BigDecimal("0.14"));

        String json = fiche();

        String ciment = "$.lignes[?(@.designation == 'Ciment CPJ45 " + jeton + "')]";
        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.lignes[*].designation")).hasSize(2),
                () -> assertThat((List<Object>) JsonPath.read(json, ciment + ".ordre")).containsExactly(2),
                () -> assertThat((List<Object>) JsonPath.read(json, ciment + ".unite")).containsExactly("SAC"),
                () -> assertThat(un(json, ciment + ".quantite")).isEqualByComparingTo("40"),
                () -> assertThat(un(json, ciment + ".prixUnitaireHT")).isEqualByComparingTo("75.5"),
                () -> assertThat(un(json, ciment + ".tauxTVA")).isEqualByComparingTo("20"));
    }

    /** Réponse du manager n° 2 : la ventilation expose le seul taux. */
    @Test
    void r6_reponse2_fiche_ventilation_categorieEtTaux() throws Exception {
        UUID gros = fixture.categorie("GO-" + jeton);
        UUID second = fixture.categorie("SO-" + jeton);
        fixture.ventilation(piece, gros, new BigDecimal("0.75"));
        fixture.ventilation(piece, second, new BigDecimal("0.25"));

        String json = fiche();

        String go = "$.ventilation[?(@.categorieCode == 'GO-" + jeton + "')]";
        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.ventilation[*].categorieCode"))
                        .containsExactlyInAnyOrder("GO-" + jeton, "SO-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, go + ".categorieDescription"))
                        .containsExactly("Categorie GO-" + jeton),
                () -> assertThat(un(json, go + ".taux")).isEqualByComparingTo("0.75"));
    }

    /** Réponse du manager n° 4 : une ligne = date, statutCode, statutDescription, utilisateur, note ; tri date ASC. */
    @Test
    void r6_reponse4_fiche_historique_trieParDateCroissante() throws Exception {
        UUID enCours = fixture.statut("ENC-" + jeton, false, false, 1);
        UUID valide = fixture.statut("VAL-" + jeton, true, false, 2);
        UUID cloture = fixture.statut("CLO-" + jeton, true, true, 3);
        fixture.historique(piece, cloture, LocalDateTime.of(2026, 5, 20, 9, 0), compte, "Troisième");
        fixture.historique(piece, enCours, LocalDateTime.of(2026, 5, 4, 8, 0), compte, "Premier");
        fixture.historique(piece, valide, LocalDateTime.of(2026, 5, 10, 17, 30), compte, "Deuxième");

        String json = fiche();

        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.historique[*].statutCode"))
                        .containsExactly("ENC-" + jeton, "VAL-" + jeton, "CLO-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, "$.historique[*].statutDescription"))
                        .containsExactly("Statut ENC-" + jeton, "Statut VAL-" + jeton, "Statut CLO-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, "$.historique[*].note"))
                        .containsExactly("Premier", "Deuxième", "Troisième"),
                () -> assertThat((List<Object>) JsonPath.read(json, "$.historique[*].date")).hasSize(3)
                        .doesNotContainNull());
    }

    @Test
    void r6_r3_fiche_reglements_annulesListesAvecAnnuleVrai_exclusDuRegle() throws Exception {
        UUID type = fixture.typeReglement("VIR-" + jeton);
        fixture.reglement(piece, "RGOK-" + jeton, new BigDecimal("300"), FixtureAchat.EN_COURS,
                LocalDate.of(2026, 5, 15), type);
        fixture.reglement(piece, "RGKO-" + jeton, new BigDecimal("19200"), FixtureAchat.ANNULE,
                LocalDate.of(2026, 5, 16), type);

        String json = fiche();

        String ok = "$.reglements[?(@.code == 'RGOK-" + jeton + "')]";
        String ko = "$.reglements[?(@.code == 'RGKO-" + jeton + "')]";
        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, "$.reglements[*].code"))
                        .containsExactlyInAnyOrder("RGOK-" + jeton, "RGKO-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, ok + ".annule")).containsExactly(false),
                () -> assertThat((List<Object>) JsonPath.read(json, ko + ".annule")).containsExactly(true),
                () -> assertThat(un(json, ok + ".montant")).isEqualByComparingTo("300"),
                () -> assertThat(un(json, ko + ".montant")).isEqualByComparingTo("19200"),
                () -> assertThat((List<Object>) JsonPath.read(json, ok + ".dateDocument"))
                        .containsExactly("2026-05-15"),
                () -> assertThat((List<Object>) JsonPath.read(json, ok + ".typeCode")).containsExactly("VIR-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, ok + ".typeDescription"))
                        .containsExactly("Type VIR-" + jeton),
                () -> assertThat(montant(json, "$.montantRegle")).isEqualByComparingTo("300"));
    }

    /** Seule valeur d'un chemin filtré, en montant. */
    private static BigDecimal un(String json, String chemin) {
        List<Object> valeurs = JsonPath.read(json, chemin);
        assertThat(valeurs).as(chemin).hasSize(1);
        return new BigDecimal(String.valueOf(valeurs.get(0)));
    }
}
