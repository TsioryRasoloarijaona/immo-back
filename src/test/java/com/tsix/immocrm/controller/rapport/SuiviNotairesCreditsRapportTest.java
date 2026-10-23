package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnesAvecTotal;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.CHEQUE_NOTAIRE;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.NOTAIRES_CREDITS;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.PARAM_CREDIT;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.PARAM_STATUT_NOTAIRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * N° 28 ÉTAT SUIVI NOTAIRES CRÉDITS (FEATURE-tdb-vente-rapports-dossiers.md §4, §9 « N° 28 », décisions 1–3 du manager).
 * Année 2022, tranche T ; statut seuil {@code DefaultComFolderStatusLawyer} = ENVOI (Sorting 4) :
 * <pre>
 *   d1 : N1, envoi notaire 10/03, MINUTE, prix 1 000 000, crédit 600 000 ;
 *        CHEQUE NOTAIRE 400 000 (doc 20/03 → éch. 30/03 = 10 j), CHÈQUE 100 000, CHEQUE NOTAIRE CANCELED 50 000 (100 j)
 *   d2 : N2, envoi 25/03, ENVOI, prix 800 000, crédit 500 000 ; CHEQUE NOTAIRE 500 000 (01/04 → 21/04 = 20 j)
 *   d3 : N1, envoi 05/06, MINUTE, prix 500 000, crédit 300 000, aucun chèque notaire
 *   exclus, un par mois : 2021 (hors année), janv. sans notaire, févr. sans échéance crédit, avril statut RESA (Sorting 1),
 *   mai ANNULÉ, juillet tranche d'une entité INACTIVE
 * </pre>
 * Le délai de déblocage d'un règlement = jours de {@code StlSettlement.DocDate} à {@code EcheanceDate}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviNotairesCreditsRapportTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String ANNEE = "annee";
    private static final String NOTAIRE_ID = "notaireId";
    private static final String MOIS = "mois";
    private static final String NB = "nbDossiers";
    private static final String CA = "ca";
    private static final String CREDIT = "credit";
    private static final String DEBLOQUE = "creditDebloque";
    private static final String DELAI = "delaiDeblocage";
    private static final String ATTESTATIONS = "attestationsNonDebloquees";
    private static final String ECHEANCES_ATTESTATIONS = "echeancesAttestationsNonDebloquees";
    private static final String MARS = "Mars";
    private static final String AVERTISSEMENTS = "$.avertissements";
    private static final BigDecimal PARASITE = new BigDecimal("777");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsDossiers rd;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private String prefixe;
    private RequestPostProcessor libre;
    private CfgTranche t;
    private CfgTranche ti;
    private StkItemCategory categorie;
    private StkItemType type;
    private StrStatus minute;
    private ComSettlementCategory credit;
    private StlSettlementType chequeNotaire;
    private CfgTier n1;
    private CfgTier n2;
    private ComFolder d1;
    private ComFolder d2;
    private int numero;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("SNC");
        fixture.tables();
        fixture.rapportVente(NOTAIRES_CREDITS, "**ÉTAT SUIVI NOTAIRES CRÉDITS**", "VENTE", 2100);
        libre = connecte(fixture.compte("snc." + prefixe, fixture.role("Libre " + prefixe, false)));

        t = ev.tranche("T-" + prefixe, ev.projet("P-" + prefixe, true), true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        StrStatus envoi = rd.statut("ENVOI-" + prefixe, 4);
        rd.parametre(PARAM_STATUT_NOTAIRE, envoi.getId());
        minute = rd.statut("MIN-" + prefixe, 7);
        credit = rd.categorieCredit();
        chequeNotaire = rd.typeReglement(CHEQUE_NOTAIRE, false);
        StlSettlementType cheque = rd.typeReglement("CHQ-" + prefixe, false);
        n1 = rd.notaire("N1-" + prefixe);
        n2 = rd.notaire("N2-" + prefixe);

        d1 = dossier(t, minute, n1, date(2022, 3, 10), "1000000");
        ComDocument c1 = rd.echeance(d1, credit, new BigDecimal("600000"));
        rd.regler(c1, new BigDecimal("400000"), true, chequeNotaire, date(2022, 3, 20), date(2022, 3, 30));
        rd.regler(c1, new BigDecimal("100000"), true, cheque, date(2022, 3, 20), date(2022, 3, 30));
        StlSettlement annule = rd.regler(c1, new BigDecimal("50000"), true, chequeNotaire, date(2022, 1, 1),
                date(2022, 4, 11));
        annule.setStatus("CANCELED");

        d2 = dossier(t, envoi, n2, date(2022, 3, 25), "800000");
        rd.regler(rd.echeance(d2, credit, new BigDecimal("500000")), new BigDecimal("500000"), true, chequeNotaire,
                date(2022, 4, 1), date(2022, 4, 21));

        rd.echeance(dossier(t, minute, n1, date(2022, 6, 5), "500000"), credit, new BigDecimal("300000"));

        exclu(dossier(t, minute, n1, date(2021, 3, 10), "999000"));
        exclu(dossier(t, minute, null, date(2022, 1, 10), "999000"));
        ComFolder sansCredit = dossier(t, minute, n1, date(2022, 2, 10), "999000");
        rd.regler(rd.echeance(sansCredit, ev.categorieEcheance(Nature.AVANCE, true), PARASITE), PARASITE, true,
                chequeNotaire, date(2022, 2, 11), date(2022, 2, 12));
        exclu(dossier(t, rd.statut("RESA-" + prefixe, 1), n1, date(2022, 4, 10), "999000"));
        exclu(dossier(t, rd.statutAnnule("ANN-" + prefixe), n1, date(2022, 5, 10), "999000"));
        exclu(dossier(ti, minute, n1, date(2022, 7, 10), "999000"));
        fixture.relire();
    }

    private static LocalDateTime date(int annee, int mois, int jour) {
        return LocalDateTime.of(annee, mois, jour, 0, 0);
    }

    private ComFolder dossier(CfgTranche tranche, StrStatus statut, CfgTier notaire, LocalDateTime envoiNotaire,
                              String prix) {
        numero++;
        ComFolder d = ev.vendre(ev.article(prefixe + "-A" + numero, tranche, categorie, type),
                "DOS-" + prefixe + "-" + numero, statut, ev.client("C" + numero + "-" + prefixe), null);
        d.setCfgLawyer(notaire);
        d.setAgreementDate(envoiNotaire);
        return ev.prix(d, new BigDecimal(prix));
    }

    /** Dossier qui serait compté s'il n'était pas exclu : échéance crédit et chèque notaire. */
    private void exclu(ComFolder dossier) {
        rd.regler(rd.echeance(dossier, credit, PARASITE), PARASITE, true, chequeNotaire,
                dossier.getAgreementDate(), dossier.getAgreementDate().plusDays(3));
    }

    private MockHttpServletRequestBuilder annee2022() {
        return get(resultat(VENTE, NOTAIRES_CREDITS)).param(ANNEE, "2022").param(TRANCHE_ID, t.getId().toString())
                .with(libre);
    }

    private Map<String, Object> mois(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), MOIS);
    }

    private static Map<String, Object> ligne(Map<String, Object> section, String mois) {
        List<Map<String, Object>> trouvees = lignesOu(section, MOIS, mois);
        assertThat(trouvees).as("ligne %s", mois).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ forme

    @Test
    void n28_douzeLignes_deJanvierADecembre() throws Exception {
        List<Map<String, Object>> lignes = lignes(mois(annee2022()));

        assertThat(lignes.stream().map(l -> l.get(MOIS)).toList()).containsExactly("Janvier", "Février", MARS,
                "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre");
    }

    @Test
    void n28_anneeSansDossier_douzeLignesAZero() throws Exception {
        Map<String, Object> section = mois(get(resultat(VENTE, NOTAIRES_CREDITS)).param(ANNEE, "1990")
                .param(TRANCHE_ID, t.getId().toString()).with(libre));

        assertThat(lignes(section)).hasSize(12);
        assertThat(somme(lignes(section), NB)).isEqualByComparingTo("0");
    }

    @Test
    void n28_anneeParDefaut_anneeCourante_avertissementAucuneDonnee() throws Exception {
        int courante = LocalDate.now(clock).getYear();

        String corps = appels.corps(get(resultat(VENTE, NOTAIRES_CREDITS)).param(TRANCHE_ID, t.getId().toString())
                .with(libre));

        List<String> avertissements = JsonPath.read(corps, AVERTISSEMENTS);
        assertThat(avertissements).contains("Aucune donnée pour " + courante);
    }

    // ------------------------------------------------------------------ nominal chiffré

    @Test
    void n28_mars_deuxNotaires_nbCaCreditDebloqueDelai() throws Exception {
        Map<String, Object> mars = ligne(mois(annee2022()), MARS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(mars.get(NB))).isEqualByComparingTo("2");
        soft.assertThat(nombre(mars.get(CA))).isEqualByComparingTo("1800000");
        soft.assertThat(nombre(mars.get(CREDIT))).isEqualByComparingTo("1100000");
        soft.assertThat(nombre(mars.get(DEBLOQUE))).isEqualByComparingTo("900000");
        soft.assertThat(nombre(mars.get(DELAI))).isEqualByComparingTo("15");
        soft.assertAll();
    }

    @Test
    void n28_juin_sansChequeNotaire_debloqueZero_delaiNull() throws Exception {
        Map<String, Object> juin = ligne(mois(annee2022()), "Juin");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(juin.get(NB))).isEqualByComparingTo("1");
        soft.assertThat(nombre(juin.get(CREDIT))).isEqualByComparingTo("300000");
        soft.assertThat(nombre(juin.get(DEBLOQUE))).isEqualByComparingTo("0");
        soft.assertThat(juin.get(DELAI)).isNull();
        soft.assertAll();
    }

    /** Totaux serveur : SOMME sur les montants et effectifs, MOYENNE des mois renseignés sur le délai (15, pas 15/12). */
    @Test
    void n28_totaux_sommesEtMoyenneDesMoisRenseignes() throws Exception {
        Map<String, Object> section = mois(annee2022());

        assertThat(colonnesAvecTotal(section, "SOMME")).contains(NB, CA, CREDIT, DEBLOQUE, ATTESTATIONS,
                ECHEANCES_ATTESTATIONS);
        assertThat(colonnesAvecTotal(section, "MOYENNE")).containsExactly(DELAI);
        Map<String, Object> totaux = totaux(section);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(totaux.get(NB))).isEqualByComparingTo("3");
        soft.assertThat(nombre(totaux.get(CA))).isEqualByComparingTo("2300000");
        soft.assertThat(nombre(totaux.get(CREDIT))).isEqualByComparingTo("1400000");
        soft.assertThat(nombre(totaux.get(DEBLOQUE))).isEqualByComparingTo("900000");
        soft.assertThat(nombre(totaux.get(DELAI))).isEqualByComparingTo("15");
        soft.assertAll();
    }

    /** RV-R4b : {@code credit} = colonne {@code credit} de la liste des dossiers pour les mêmes dossiers. */
    @Test
    void rv_r4b_credit_egalColonneCreditDeLaListe() throws Exception {
        Map<String, Object> mars = ligne(mois(annee2022()), MARS);

        BigDecimal deLaListe = BigDecimal.ZERO;
        for (ComFolder d : List.of(d1, d2)) {
            String corps = appels.corps(get("/api/dossiers").param("search", d.getCode())
                    .param("colonnes", "code,credit").with(libre));
            List<Object> valeurs = JsonPath.read(corps, "$.content[?(@.code == '" + d.getCode() + "')].credit");
            assertThat(valeurs).hasSize(1);
            deLaListe = deLaListe.add(nombre(valeurs.get(0)));
        }

        assertThat(nombre(mars.get(CREDIT))).isEqualByComparingTo(deLaListe);
    }

    // ------------------------------------------------------------------ cohorte : exclusions (un mois chacune)

    @Test
    void n28_dossierHorsAnnee_exclu() throws Exception {
        Map<String, Object> section = mois(get(resultat(VENTE, NOTAIRES_CREDITS)).param(ANNEE, "2021")
                .param(TRANCHE_ID, t.getId().toString()).with(libre));

        assertThat(nombre(ligne(section, MARS).get(NB))).isEqualByComparingTo("1");
        assertThat(nombre(ligne(mois(annee2022()), MARS).get(CA))).isEqualByComparingTo("1800000");
    }

    @Test
    void n28_dossierSansNotaire_exclu() throws Exception {
        assertThat(nombre(ligne(mois(annee2022()), "Janvier").get(NB))).isEqualByComparingTo("0");
    }

    @Test
    void n28_dossierSansEcheanceCredit_exclu() throws Exception {
        assertThat(nombre(ligne(mois(annee2022()), "Février").get(NB))).isEqualByComparingTo("0");
    }

    @Test
    void n28_statutInferieurAEnvoiNotaire_exclu() throws Exception {
        assertThat(nombre(ligne(mois(annee2022()), "Avril").get(NB))).isEqualByComparingTo("0");
    }

    @Test
    void n28_dossierAnnule_exclu() throws Exception {
        assertThat(nombre(ligne(mois(annee2022()), "Mai").get(NB))).isEqualByComparingTo("0");
    }

    /** Sans {@code trancheId} : toutes les tranches visibles, donc jamais celle d'une entité inactive. */
    @Test
    void rv_r4a_trancheDUneEntiteInactive_exclue() throws Exception {
        Map<String, Object> section = mois(get(resultat(VENTE, NOTAIRES_CREDITS)).param(ANNEE, "2022").with(libre));

        assertThat(nombre(ligne(section, "Juillet").get(NB))).isEqualByComparingTo("0");
    }

    @Test
    void rv_r4a_trancheIdDUneEntiteInactive_aucunDossier() throws Exception {
        Map<String, Object> section = mois(get(resultat(VENTE, NOTAIRES_CREDITS)).param(ANNEE, "2022")
                .param(TRANCHE_ID, ti.getId().toString()).with(libre));

        assertThat(somme(lignes(section), NB)).isEqualByComparingTo("0");
    }

    @Test
    void rv_r4a_projetHorsPerimetre_aucunDossier() throws Exception {
        AdmUser compte = fixture.compte("snc.hp." + prefixe, fixture.role("Hors " + prefixe, false));
        ev.affecter(compte, ev.projet("PX-" + prefixe, true));
        fixture.relire();

        Map<String, Object> section = mois(get(resultat(VENTE, NOTAIRES_CREDITS)).param(ANNEE, "2022")
                .param(TRANCHE_ID, t.getId().toString()).with(connecte(compte)));

        assertThat(somme(lignes(section), NB)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ règlements

    /** Filtre N1 : d1 seul en mars ; le chèque notaire CANCELED (50 000, 100 j) ne compte ni au montant ni au délai. */
    @Test
    void n28_reglementCanceled_exclu_duMontantEtDuDelai() throws Exception {
        Map<String, Object> mars = ligne(mois(annee2022().param(NOTAIRE_ID, n1.getId().toString())), MARS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(mars.get(DEBLOQUE))).isEqualByComparingTo("400000");
        soft.assertThat(nombre(mars.get(DELAI))).isEqualByComparingTo("10");
        soft.assertAll();
    }

    @Test
    void n28_filtreNotaire_seulsSesDossiers() throws Exception {
        Map<String, Object> section = mois(annee2022().param(NOTAIRE_ID, n1.getId().toString()));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligne(section, MARS).get(NB))).isEqualByComparingTo("1");
        soft.assertThat(nombre(ligne(section, MARS).get(CA))).isEqualByComparingTo("1000000");
        soft.assertThat(nombre(ligne(section, "Juin").get(NB))).isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux(section).get(NB))).isEqualByComparingTo("2");
        soft.assertAll();
    }

    @Test
    void n28_notaireIdHorsListe_refuse400_jamaisTous() throws Exception {
        mockMvc.perform(annee2022().param(NOTAIRE_ID, UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void n28_optionsNotaire_notairesAyantUnDossier() throws Exception {
        List<String> options = JsonPath.read(appels.corps(get(descripteur(VENTE, NOTAIRES_CREDITS)).with(libre)),
                "$.parametres[?(@.nom == 'notaireId')].options[*].valeur");

        assertThat(options).contains(n1.getId().toString(), n2.getId().toString());
    }

    // ------------------------------------------------------------------ attestations (décision 1), paramètre absent

    @Test
    void n28_attestations_zeroEtAvertissement_sansException() throws Exception {
        String corps = appels.corps(annee2022());

        List<Map<String, Object>> lignes = JsonPath.read(corps, "$.sections[?(@.id == 'mois')].lignes[*]");
        List<String> avertissements = JsonPath.read(corps, AVERTISSEMENTS);
        assertThat(lignes).isNotEmpty().allSatisfy(l -> {
            assertThat(nombre(l.get(ATTESTATIONS))).isEqualByComparingTo("0");
            assertThat(nombre(l.get(ECHEANCES_ATTESTATIONS))).isEqualByComparingTo("0");
        });
        assertThat(avertissements).anySatisfy(a -> assertThat(a).containsIgnoringCase("attestation"));
    }

    @Test
    void n28_parametreCreditAbsent_sectionVide_avertissement() throws Exception {
        rd.retirerParametre(PARAM_CREDIT);

        String corps = appels.corps(annee2022());

        List<List<Object>> lignes = JsonPath.read(corps, "$.sections[?(@.id == 'mois')].lignes");
        List<String> avertissements = JsonPath.read(corps, AVERTISSEMENTS);
        assertThat(lignes).singleElement().satisfies(l -> assertThat(l).isEmpty());
        assertThat(avertissements).anySatisfy(a -> assertThat(a).contains(PARAM_CREDIT));
    }
}
