package com.tsix.immocrm.controller.suiviaffectations;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureSuiviAffectations;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.tsix.immocrm.fixtures.FixtureSuiviAffectations.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureSuiviAffectations.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Montants signés du suivi des affectations (argent, règle 6) — FEATURE-suivi-affectations.md R2, R3, D2, D4, R7.
 * Jeu d'une tranche propre au test :
 * <pre>
 *   crédit      IsCredit = 1     ligne 1000                     → +1000
 *   débit       IsCredit = 0     ligne  300                     →  −300
 *   sans sens   IsCredit NULL    ligne  250                     →  +250
 *   annulée     CANCELED crédit  ligne  700                     →  +700, hors Σ
 *   pièce 5000 à deux lignes     lignes 1200 (dossier 1) et 800 (dossier 2)
 *   Σ hors annulées = 1000 − 300 + 250 + 1200 + 800 = 2950 ; 6 lignes
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SuiviAffectationsMontantsTest {

    private static final String URL = "/api/suivi-affectations";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COLONNES = "colonnes";
    private static final String AGREGATS = "agregats";
    private static final String MONTANT_AFFECTE = "montantAffecte";
    private static final String TOTAUX_MONTANT = "$.totaux.montantAffecte";
    private static final int NOMBRE_LIGNES = 6;
    private static final double SOMME_HORS_ANNULEES = 2950.0;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviAffectations fixture;

    private CfgTranche tranche;
    private StlSettlementLine credit;
    private StlSettlementLine debit;
    private StlSettlementLine sansSens;
    private StlSettlementLine annulee;
    private StlSettlementLine partieDossier1;
    private StlSettlementLine partieDossier2;

    @BeforeEach
    void setUp() {
        FixtureEtatVentes ev = fixture.etatVentes();
        String prefixe = FixtureEtatVentes.prefixe("SAM");
        tranche = ev.tranche("TR-" + prefixe, ev.projet("PRJ-" + prefixe, true), true);
        StkItemCategory categorieArticle = ev.categorie("APP-" + prefixe);
        StkItemType typeArticle = ev.type("T3-" + prefixe);
        ComSettlementCategory avance = fixture.categorie("AV-" + prefixe);
        ComFolder d1 = ev.vendre(ev.article(prefixe + "-A1", tranche, categorieArticle, typeArticle),
                "DOS-" + prefixe + "-1", ev.statut(StatutDossier.RESERVATION), null, null);
        ComFolder d2 = ev.vendre(ev.article(prefixe + "-A2", tranche, categorieArticle, typeArticle),
                "DOS-" + prefixe + "-2", ev.statut(StatutDossier.RESERVATION), null, null);
        ComDocument e1 = fixture.echeance(d1, avance, new BigDecimal("10000"));
        ComDocument e2 = fixture.echeance(d2, avance, new BigDecimal("10000"));
        LocalDateTime jour = LocalDateTime.of(2025, 6, 1, 0, 0);

        credit = fixture.ligne(fixture.piece("CR " + prefixe, true, IN_WORK, jour, null, new BigDecimal("1000")),
                e1, new BigDecimal("1000"));
        debit = fixture.ligne(fixture.piece("DB " + prefixe, false, IN_WORK, jour, null, new BigDecimal("300")),
                e1, new BigDecimal("300"));
        sansSens = fixture.ligne(fixture.piece("NU " + prefixe, null, null, jour, null, new BigDecimal("250")),
                e1, new BigDecimal("250"));
        annulee = fixture.ligne(fixture.piece("AN " + prefixe, true, CANCELED, jour, null, new BigDecimal("700")),
                e1, new BigDecimal("700"));
        StlSettlement pieceDouble = fixture.piece("DL " + prefixe, true, IN_WORK, jour, null, new BigDecimal("5000"));
        partieDossier1 = fixture.ligne(pieceDouble, e1, new BigDecimal("1200"));
        partieDossier2 = fixture.ligne(pieceDouble, e2, new BigDecimal("800"));
        fixture.relire();
    }

    private MockHttpServletRequestBuilder liste() {
        return get(URL).param(TRANCHE_ID, tranche.getId().toString()).param("size", "50")
                .param(COLONNES, "id,montantAffecte,sens,montantPiece,etatPiece");
    }

    private static String ligne(StlSettlementLine l) {
        return "$.content[?(@.id=='" + l.getId() + "')]";
    }

    private static String corpsDe(MockMvc mockMvc, MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    // ------------------------------------------------------------------ R2 / D2 — signe

    @Test
    void r2_credit_montantPositif() throws Exception {
        mockMvc.perform(liste())
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(credit) + ".montantAffecte").value(1000.0))
                .andExpect(jsonPath(ligne(credit) + ".sens").value("CREDIT"));
    }

    @Test
    void r2_debit_montantNegatif() throws Exception {
        mockMvc.perform(liste())
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(debit) + ".montantAffecte").value(-300.0))
                .andExpect(jsonPath(ligne(debit) + ".sens").value("DEBIT"));
    }

    /** D2 (§11, décision de l'utilisateur) : IsCredit NULL suit la règle du CRM, positif, sens CREDIT. */
    @Test
    void d2_isCreditNull_montantPositif() throws Exception {
        mockMvc.perform(liste())
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(sansSens) + ".montantAffecte").value(250.0))
                .andExpect(jsonPath(ligne(sansSens) + ".sens").value("CREDIT"));
    }

    // ------------------------------------------------------------------ D4 — pièce annulée

    @Test
    void d4_pieceAnnulee_listeeEtComptee_maisHorsDeLaSomme() throws Exception {
        mockMvc.perform(liste())
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(annulee) + ".montantAffecte").value(700.0))
                .andExpect(jsonPath("$.totalElements").value(NOMBRE_LIGNES));

        mockMvc.perform(get(URL + "/totaux-colonnes").param(TRANCHE_ID, tranche.getId().toString())
                        .param(AGREGATS, MONTANT_AFFECTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(NOMBRE_LIGNES))
                .andExpect(jsonPath(TOTAUX_MONTANT).value(SOMME_HORS_ANNULEES));
    }

    @Test
    void d4_pieceAnnulee_compteeDansSonGroupe_horsDeSaSomme() throws Exception {
        mockMvc.perform(get(URL + "/groupes").param(TRANCHE_ID, tranche.getId().toString())
                        .param("groupBy", "etatPiece").param(AGREGATS, MONTANT_AFFECTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='CANCELED')].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle=='CANCELED')].agregats.montantAffecte")
                        .value(Matchers.contains(Matchers.anyOf(Matchers.nullValue(), Matchers.equalTo(0),
                                Matchers.equalTo(0.0)))));
    }

    // ------------------------------------------------------------------ R1 / R2 — pièce à plusieurs lignes

    @Test
    void r2_pieceADeuxLignes_deuxLignes_montantsDeLigneEtNonDePiece() throws Exception {
        mockMvc.perform(liste())
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(partieDossier1) + ".montantAffecte").value(1200.0))
                .andExpect(jsonPath(ligne(partieDossier2) + ".montantAffecte").value(800.0))
                .andExpect(jsonPath(ligne(partieDossier1) + ".montantPiece").value(5000.0))
                .andExpect(jsonPath(ligne(partieDossier2) + ".montantPiece").value(5000.0));
    }

    // ------------------------------------------------------------------ R7 — une seule base

    @Test
    void r7_totaux_egalentLaSommeDesLignesNonAnnuleesDeToutesLesPages() throws Exception {
        BigDecimal somme = BigDecimal.ZERO;
        for (int page = 0; page < NOMBRE_LIGNES; page++) {
            String corps = corpsDe(mockMvc, get(URL).param(TRANCHE_ID, tranche.getId().toString())
                    .param("size", "1").param("page", String.valueOf(page))
                    .param("sort", "id,asc").param(COLONNES, "id,montantAffecte,etatPiece"));
            String etat = JsonPath.read(corps, "$.content[0].etatPiece");
            Number montant = JsonPath.read(corps, "$.content[0].montantAffecte");
            if (!CANCELED.equals(etat)) {
                somme = somme.add(new BigDecimal(montant.toString()));
            }
        }

        mockMvc.perform(get(URL + "/totaux-colonnes").param(TRANCHE_ID, tranche.getId().toString())
                        .param(AGREGATS, MONTANT_AFFECTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAUX_MONTANT).value(somme.doubleValue()))
                .andExpect(jsonPath(TOTAUX_MONTANT).value(SOMME_HORS_ANNULEES));
    }

    @Test
    void r7_groupes_sommeDesGroupesEgaleLesTotauxDeLaListe() throws Exception {
        String groupes = corpsDe(mockMvc, get(URL + "/groupes").param(TRANCHE_ID, tranche.getId().toString())
                .param("groupBy", "sens").param(AGREGATS, MONTANT_AFFECTE));

        List<Number> sommes = JsonPath.read(groupes, "$.content[*].agregats.montantAffecte");
        List<Integer> effectifs = JsonPath.read(groupes, "$.content[*].nombre");
        Number total = JsonPath.read(groupes, TOTAUX_MONTANT);

        assertThat(sommes).isNotEmpty();
        assertThat(sommes.stream().mapToDouble(Number::doubleValue).sum())
                .isEqualTo(total.doubleValue())
                .isEqualTo(SOMME_HORS_ANNULEES);
        assertThat(effectifs.stream().mapToLong(Integer::longValue).sum()).isEqualTo(NOMBRE_LIGNES);
    }
}
