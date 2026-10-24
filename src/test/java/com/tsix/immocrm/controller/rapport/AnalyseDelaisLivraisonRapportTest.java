package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.DELAIS_LIVRAISON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 29 ANALYSE DÉLAIS DE LIVRAISONS (FEATURE-tdb-vente-rapports-dossiers.md §5, §9 « N° 29 », décision 2 du manager :
 * valeurs absentes ou négatives ignorées, moyenne à 1 décimale, {@code null} plutôt que 0). Tranche T, 2022 :
 * <pre>
 *   comptant (aucune échéance DefaultCreditDocumentType)
 *     c1 : N1, titre 01/01, envoi 01/02, minute 01/03, livraison 11/03, formalisé 21/03
 *          → titre/envoi 31, minute/remise 10, titre/remise 69, envoi/minute 28, minute/formalisé 20
 *     c2 : N1, envoi 01/02, minute 02/03, livraison 13/03 → envoi/minute 29, minute/remise 11 (formalisé absent)
 *     c3 : sans notaire, minute 01/05, livraison 01/04 → minute/remise négatif, ignoré
 *     c4 : SANS STATUT, sans notaire, sans date
 *   crédit (une échéance de la catégorie crédit)
 *     k1 : N2, envoi 10/01, crédit en cours 20/01, débloqué 04/02, minute 14/02, formalisé 24/02, livraison 01/03
 *     k2 : sans notaire, minute 01/01, livraison 21/01 → minute/remise 20
 *   exclus : comptant N1 ANNULÉ, comptant N1 sur une tranche d'entité INACTIVE (délais énormes)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyseDelaisLivraisonRapportTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String COMPTANT = "comptant";
    private static final String COMPTANT_NOTAIRES = "comptant-notaires";
    private static final String CREDIT = "credit";
    private static final String CREDIT_NOTAIRES = "credit-notaires";
    private static final String NB = "nbDossiers";
    private static final String NOTAIRE = "notaire";
    private static final String TITRE_ENVOI = "titreEnvoiNotaire";
    private static final String MINUTE_REMISE = "minuteRemiseCles";
    private static final String TITRE_REMISE = "titreRemiseCles";
    private static final String ENVOI_MINUTE = "envoiMinute";
    private static final String MINUTE_FORMALISE = "minuteFormalise";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsDossiers rd;
    @Autowired private StkItemRepository articles;

    private AppelsRapports appels;
    private String prefixe;
    private RequestPostProcessor libre;
    private CfgTranche t;
    private CfgTranche ti;
    private StkItemCategory categorie;
    private StkItemType type;
    private StrStatus enCours;
    private ComSettlementCategory credit;
    private int numero;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("ADL");
        fixture.tables();
        fixture.rapportVente(DELAIS_LIVRAISON, "**ANALYSE DELAIS DE LIVRAISONS**", "VENTE", 2120);
        libre = connecte(fixture.compte("adl." + prefixe, fixture.role("Libre " + prefixe, false)));

        t = ev.tranche("T-" + prefixe, ev.projet("P-" + prefixe, true), true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        enCours = ev.statutEnCours("ENC-" + prefixe);
        credit = rd.categorieCredit();
        CfgTier n1 = rd.notaire("N1-" + prefixe);
        CfgTier n2 = rd.notaire("N2-" + prefixe);

        ComFolder c1 = dossier(t, enCours, n1);
        titre(c1, date(1, 1));
        c1.setAgreementDate(date(2, 1));
        c1.setAgreementSavingDate(date(3, 1));
        c1.setDeliveryDateReal(date(3, 11));
        c1.setOwnershipCertificateDate(date(3, 21));
        ComFolder c2 = dossier(t, enCours, n1);
        c2.setAgreementDate(date(2, 1));
        c2.setAgreementSavingDate(date(3, 2));
        c2.setDeliveryDateReal(date(3, 13));
        ComFolder c3 = dossier(t, enCours, null);
        c3.setAgreementSavingDate(date(5, 1));
        c3.setDeliveryDateReal(date(4, 1));
        dossier(t, null, null);

        ComFolder k1 = dossier(t, enCours, n2);
        rd.echeance(k1, credit, new BigDecimal("400000"));
        k1.setAgreementDate(date(1, 10));
        k1.setCreditApprovalDate(date(1, 20));
        k1.setCreditPossessionDate(date(2, 4));
        k1.setAgreementSavingDate(date(2, 14));
        k1.setOwnershipCertificateDate(date(2, 24));
        k1.setDeliveryDateReal(date(3, 1));
        ComFolder k2 = dossier(t, enCours, null);
        rd.echeance(k2, credit, new BigDecimal("300000"));
        k2.setAgreementSavingDate(date(1, 1));
        k2.setDeliveryDateReal(date(1, 21));

        exclu(dossier(t, rd.statutAnnule("ANN-" + prefixe), n1));
        exclu(dossier(ti, enCours, n1));
        fixture.relire();
    }

    private static LocalDateTime date(int mois, int jour) {
        return LocalDateTime.of(2022, mois, jour, 0, 0);
    }

    private ComFolder dossier(CfgTranche tranche, StrStatus statut, CfgTier notaire) {
        numero++;
        ComFolder d = ev.vendre(ev.article(prefixe + "-A" + numero, tranche, categorie, type),
                "DOS-" + prefixe + "-" + numero, statut, ev.client("C" + numero + "-" + prefixe), null);
        d.setCfgLawyer(notaire);
        return ev.enregistrer(d);
    }

    private void titre(ComFolder dossier, LocalDateTime date) {
        StkItem article = dossier.getStkItem();
        article.setTitleDate(date);
        articles.save(article);
    }

    /** Délais qui fausseraient toutes les moyennes s'ils étaient comptés. */
    private void exclu(ComFolder dossier) {
        titre(dossier, LocalDateTime.of(2000, 1, 1, 0, 0));
        dossier.setAgreementDate(date(1, 1));
        dossier.setAgreementSavingDate(date(12, 1));
        dossier.setDeliveryDateReal(date(12, 31));
        dossier.setOwnershipCertificateDate(date(12, 31));
    }

    private MockHttpServletRequestBuilder sur(CfgTranche tranche) {
        return get(resultat(VENTE, DELAIS_LIVRAISON)).param(TRANCHE_ID, tranche.getId().toString()).with(libre);
    }

    private Map<String, Object> unique(String section) throws Exception {
        List<Map<String, Object>> lignes = lignes(section(appels.sections(sur(t)), section));
        assertThat(lignes).as(section).hasSize(1);
        return lignes.get(0);
    }

    private static Map<String, Object> notaire(List<Map<String, Object>> lignes, String nom) {
        List<Map<String, Object>> trouvees = lignes.stream()
                .filter(l -> String.valueOf(l.get(NOTAIRE)).contains(nom)).toList();
        assertThat(trouvees).as("ligne du notaire %s", nom).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ comptant

    @Test
    void n29_comptant_nbDossiers_etDelaisTitreFoncier() throws Exception {
        Map<String, Object> comptant = unique(COMPTANT);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(comptant.get(NB))).isEqualByComparingTo("4");
        soft.assertThat(nombre(comptant.get(TITRE_ENVOI))).isEqualByComparingTo("31");
        soft.assertThat(nombre(comptant.get(TITRE_REMISE))).isEqualByComparingTo("69");
        soft.assertAll();
    }

    /** c3 (livraison avant la minute) ignoré : (10 + 11) / 2, ni 7 (négatif à 0) ni 3,3 (négatif compté). */
    @Test
    void n29_delaiNegatif_ignore_moyenneAUneDecimale() throws Exception {
        assertThat(nombre(unique(COMPTANT).get(MINUTE_REMISE))).isEqualByComparingTo("10.5");
    }

    @Test
    void n29_dossierSansStatut_inclus() throws Exception {
        assertThat(nombre(unique(COMPTANT).get(NB))).isEqualByComparingTo("4");
    }

    // ------------------------------------------------------------------ comptant-notaires

    @Test
    void n29_comptantNotaires_comptantSeul_uneLigneParNotaire() throws Exception {
        List<Map<String, Object>> lignes = lignes(section(appels.sections(sur(t)), COMPTANT_NOTAIRES));

        assertThat(lignes).hasSize(1);
        assertThat(String.valueOf(lignes.get(0).get(NOTAIRE))).contains("N1-" + prefixe)
                .doesNotContain("N2-" + prefixe);
    }

    @Test
    void n29_dossierSansNotaire_absentDesNotaires_presentDansComptant() throws Exception {
        List<Map<String, Object>> sections = appels.sections(sur(t));

        assertThat(somme(lignes(section(sections, COMPTANT_NOTAIRES)), NB)).isEqualByComparingTo("2");
        assertThat(nombre(lignes(section(sections, COMPTANT)).get(0).get(NB))).isEqualByComparingTo("4");
    }

    @Test
    void n29_comptantNotaires_envoiMinute_moyenneAUneDecimale() throws Exception {
        Map<String, Object> n1 = notaire(lignes(section(appels.sections(sur(t)), COMPTANT_NOTAIRES)), "N1-" + prefixe);

        assertThat(nombre(n1.get(ENVOI_MINUTE))).isEqualByComparingTo("28.5");
    }

    /** c2 n'a pas de date « formalisé » : ignoré (20), pas compté à 0 (10). */
    @Test
    void n29_dateManquante_ignoree_pasZero() throws Exception {
        Map<String, Object> n1 = notaire(lignes(section(appels.sections(sur(t)), COMPTANT_NOTAIRES)), "N1-" + prefixe);

        assertThat(nombre(n1.get(MINUTE_FORMALISE))).isEqualByComparingTo("20");
    }

    @Test
    void n29_dossierAnnule_exclu() throws Exception {
        Map<String, Object> n1 = notaire(lignes(section(appels.sections(sur(t)), COMPTANT_NOTAIRES)), "N1-" + prefixe);

        assertThat(nombre(n1.get(NB))).isEqualByComparingTo("2");
    }

    // ------------------------------------------------------------------ crédit

    @Test
    void n29_credit_nbDossiers_minuteRemiseCles_titreNull() throws Exception {
        Map<String, Object> ligneCredit = unique(CREDIT);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneCredit.get(NB))).isEqualByComparingTo("2");
        soft.assertThat(nombre(ligneCredit.get(MINUTE_REMISE))).isEqualByComparingTo("17.5");
        soft.assertThat(ligneCredit.get(TITRE_ENVOI)).isNull();
        soft.assertAll();
    }

    @Test
    void n29_creditNotaires_sixDelais() throws Exception {
        List<Map<String, Object>> lignes = lignes(section(appels.sections(sur(t)), CREDIT_NOTAIRES));
        Map<String, Object> n2 = notaire(lignes, "N2-" + prefixe);

        assertThat(lignes).hasSize(1);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(n2.get(NB))).isEqualByComparingTo("1");
        soft.assertThat(nombre(n2.get("envoiCreditEnCours"))).isEqualByComparingTo("10");
        soft.assertThat(nombre(n2.get("creditEnCoursDebloque"))).isEqualByComparingTo("15");
        soft.assertThat(nombre(n2.get("debloqueMinute"))).isEqualByComparingTo("10");
        soft.assertThat(nombre(n2.get("creditEnCoursMinute"))).isEqualByComparingTo("25");
        soft.assertThat(nombre(n2.get(MINUTE_FORMALISE))).isEqualByComparingTo("10");
        soft.assertThat(n2.get(TITRE_REMISE)).isNull();
        soft.assertAll();
    }

    // ------------------------------------------------------------------ titre foncier vide, refus

    @Test
    void n29_titreFoncierVide_valeurNull_etAvertissement() throws Exception {
        CfgTranche sansTitre = ev.tranche("TS-" + prefixe, ev.projet("PS-" + prefixe, true), true);
        ComFolder d = dossier(sansTitre, enCours, null);
        d.setAgreementDate(date(2, 1));
        d.setDeliveryDateReal(date(3, 1));
        fixture.relire();

        String corps = appels.corps(sur(sansTitre));

        List<Object> titres = JsonPath.read(corps, "$.sections[?(@.id == 'comptant')].lignes[*].titreEnvoiNotaire");
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");
        assertThat(titres).singleElement().isNull();
        assertThat(avertissements).anySatisfy(a -> assertThat(a).containsIgnoringCase("titre foncier"));
    }

    @Test
    void rv_r4a_trancheDUneEntiteInactive_aucunDossier() throws Exception {
        List<Map<String, Object>> sections = appels.sections(sur(ti));

        BigDecimal total = BigDecimal.ZERO;
        for (String id : List.of(COMPTANT, COMPTANT_NOTAIRES, CREDIT, CREDIT_NOTAIRES)) {
            total = total.add(somme(lignes(section(sections, id)), NB));
        }
        assertThat(total).isEqualByComparingTo("0");
    }
}
