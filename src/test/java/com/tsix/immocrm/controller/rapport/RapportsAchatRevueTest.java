package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ACHAT;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.lignesAvec;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.lignesMentionnant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.DEPENSES_PAR_MOIS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.ECHEANCIER_CREDITS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.RETENUES_LOTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revue N7b (FEATURE-n7-etats-achat.md R12, R13, R15, réponse 8) : chemins de refus non couverts par les tests verrouillés
 * et correspondance des colonnes R13 avec {@code PrjMarketMonitoringView}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsAchatRevueTest {

    private static final String PREVU = "prevu";
    private static final String DECAISSEMENTS = "decaissements";
    private static final String CODE = "code";
    private static final String PROJETS = "projets";
    private static final String ANNEE_TEST = String.valueOf(FixtureRapportsAchat.ANNEE);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;

    private AppelsRapports appels;
    private String jeton;
    private UUID s1;
    private UUID s2;
    private UUID pa;
    private UUID pb;
    private UUID p2;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        jeton = FixtureAchat.jeton("N7R");
        fixture.rapports();
        s1 = achat.societe("S1-" + jeton);
        s2 = achat.societe("S2-" + jeton);
        pa = achat.projet("PA-" + jeton, s1);
        pb = achat.projet("PB-" + jeton, s1);
        p2 = achat.projet("P2-" + jeton, s2);
        compte = fixture.compte("N7R-" + jeton, false, s1, pa, p2);
    }

    private MockHttpServletRequestBuilder requete(String code, String... parametres) {
        MockHttpServletRequestBuilder r = get(resultat(ACHAT, code)).with(connecte(compte, FixtureRapportsAchat.role()));
        for (int i = 0; i + 1 < parametres.length; i += 2) {
            r.param(parametres[i], parametres[i + 1]);
        }
        return r;
    }

    private List<Map<String, Object>> sections(String code, String... parametres) throws Exception {
        return appels.sections(requete(code, parametres));
    }

    private static LocalDate jour(int mois, int jour) {
        return LocalDate.of(FixtureRapportsAchat.ANNEE, mois, jour);
    }

    private List<Map<String, Object>> mois() throws Exception {
        List<Map<String, Object>> lignes = lignesAvec(sections(DEPENSES_PAR_MOIS, ANNEE, ANNEE_TEST), PREVU);
        assertThat(lignes).hasSize(12);
        return lignes;
    }

    // ------------------------------------------------------------------ R12 : D3 sur chaque source (réponse 8)

    @Test
    void r12_prevu_previsionDUnProjetNonHabilite_exclue() throws Exception {
        fixture.previsionProjet(FixtureRapportsAchat.ANNEE, 1, "3", pa);
        fixture.previsionProjet(FixtureRapportsAchat.ANNEE, 1, "4000", pb);

        assertThat(somme(mois(), PREVU)).isEqualByComparingTo("3");
    }

    @Test
    void r12_prevu_echeancesDePretHorsPerimetre_exclues() throws Exception {
        fixture.echeance(fixture.pret("PV-" + jeton, s1, pa), jour(1, 15), "5");
        fixture.echeance(fixture.pret("PB-" + jeton, s1, pb), jour(1, 15), "600");
        fixture.echeance(fixture.pret("PS-" + jeton, s2, p2), jour(1, 15), "70000");

        assertThat(somme(mois(), PREVU)).as("seul le prêt S1/PA").isEqualByComparingTo("5");
    }

    @Test
    void r12_decaissements_societeNonHabilitee_exclus() throws Exception {
        fixture.reglement(jeton + "-RV", "BUY", false, "9", "IN_WORK", null, jour(6, 1), s1);
        fixture.reglement(jeton + "-RS", "BUY", false, "8000", "IN_WORK", null, jour(6, 1), s2);

        assertThat(somme(mois(), DECAISSEMENTS)).isEqualByComparingTo("9");
    }

    // ------------------------------------------------------------------ R15 : société du prêt (Q5)

    @Test
    void r15_pretDUneSocieteNonHabilitee_absent_memeAvecProjetHabilite() throws Exception {
        fixture.echeance(fixture.pret("LV-" + jeton, s1, pa), jour(2, 10), "11");
        fixture.echeance(fixture.pret("LS-" + jeton, s2, p2), jour(2, 10), "22");

        List<Map<String, Object>> sections = sections(ECHEANCIER_CREDITS, ANNEE, ANNEE_TEST);

        assertThat(lignesMentionnant(sections, "LV-" + jeton)).as("témoin").isNotEmpty();
        assertThat(lignesMentionnant(sections, "LS-" + jeton)).isEmpty();
    }

    // ------------------------------------------------------------------ R13

    private String lotAvecFacture(UUID projet, String prefixe, String retenue, String remboursementProrata) {
        parite.genresDeLot();
        fiche.entitesPiecesLot();
        UUID statut = parite.statut(ENTREPRENEURS, "N7R-" + prefixe + "-" + jeton);
        String code = prefixe + "-" + jeton;
        UUID lot = parite.lot(projet, code, ENTREPRENEURS, statut);
        UUID facture = fiche.factureLot(lot, code + "-F", "800", "960");
        achat.poser(facture, "WarrantyAmount", new BigDecimal(retenue));
        if (remboursementProrata != null) {
            achat.poser(facture, "ProrataRefundAmount", new BigDecimal(remboursementProrata));
        }
        return code;
    }

    /**
     * OMRAN : {@code InvoiceCostProrataRefundAmount} = {@code TT.InvoiceTotalProrataRefundAmount} =
     * Σ {@code BuyDocument.ProrataRefundAmount} des factures du lot (vue {@code PrjMarketMonitoringView}).
     */
    @Test
    void r13_prorataRembourse_estLeRemboursementProrataDesFacturesDuLot() throws Exception {
        String code = lotAvecFacture(pa, "LR", "10", "42");

        Map<String, Object> ligne = lignesAvec(sections(RETENUES_LOTS), CODE).stream()
                .filter(l -> code.equals(l.get(CODE))).findFirst().orElseThrow();

        assertThat(Objects.requireNonNullElse(nombre(ligne.get("prorataRembourse")), BigDecimal.ZERO))
                .isEqualByComparingTo("42");
    }

    /** Réponse 8 : « un identifiant hors périmètre est ignoré (200, ses lots absents) ». */
    @Disabled("En attente d'arbitrage manager : CHOIX aux options visibles répond 400 (socle) ; voir RAPPORT-n7-etats-achat.md")
    @Test
    void r13_projetHorsPerimetre_ignore_200() throws Exception {
        String visible = lotAvecFacture(pa, "LV", "10", null);
        String cache = lotAvecFacture(pb, "LH", "10", null);

        int statut = appels.statut(requete(RETENUES_LOTS, PROJETS, pa.toString(), PROJETS, pb.toString()));

        assertThat(statut).as("pb hors périmètre : ignoré, pas 400").isEqualTo(200);
        List<Object> codes = lignesAvec(sections(RETENUES_LOTS, PROJETS, pa.toString(), PROJETS, pb.toString()), CODE)
                .stream().map(l -> l.get(CODE)).toList();
        assertThat(codes).contains(visible).doesNotContain(cache);
    }
}
