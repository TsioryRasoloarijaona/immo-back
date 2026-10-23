package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.rapport.vente.flux.FluxParReflexion.Cellule;
import com.tsix.immocrm.services.rapport.vente.flux.FluxParReflexion.Flux;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.SoftAssertions;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureFluxVente.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.POINTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le calcul partagé {@code FluxVenteMensuel} (FEATURE-tdb-vente-rapports-flux.md §3, règles R-F2…R-F13, R-V6, I1, I5–I9).
 * Appelé par réflexion sur le contrat Java du spec (§3.1) : la classe compile avant le service. Chaque test pose ses
 * propres dossiers dans des tranches neuves ; seuils posés par {@link FixtureFluxVente} (RES 1, CONC 2, MIN 7, CLEFS 8,
 * ANN 30, DES 31).
 */
@SpringBootTest
@Transactional
class FluxVenteMensuelTest {

    private static final String CONCRETISATIONS = "CONCRETISATIONS";
    private static final String CONCRETISATIONS_NETTES = "CONCRETISATIONS_NETTES";
    private static final String CONCRETISATIONS_MONTANT = "CONCRETISATIONS_MONTANT";
    private static final String AVANCES_ENCAISSEES = "AVANCES_ENCAISSEES";
    private static final String CA_QUANTITE = "CA_QUANTITE";
    private static final String CA_TTC = "CA_TTC";
    private static final String CA_HT = "CA_HT";
    private static final String LIVRAISONS = "LIVRAISONS";
    private static final String ENCAISSEMENTS = "ENCAISSEMENTS";
    private static final String DESISTEMENTS = "DESISTEMENTS";
    private static final String DESISTEMENTS_MONTANT = "DESISTEMENTS_MONTANT";
    private static final String ANNULATIONS = "ANNULATIONS";
    private static final String ANNULATIONS_MONTANT = "ANNULATIONS_MONTANT";
    private static final String FRAIS_DOSSIER = "ENCAISSEMENTS_FRAIS_DOSSIER";
    private static final String FRAIS_TRANSFERT = "ENCAISSEMENTS_FRAIS_TRANSFERT";
    private static final String FRAIS_DESISTEMENT_DUS = "FRAIS_DESISTEMENT_DUS";
    private static final String APRES_ACTE = "ENCAISSEMENTS_APRES_ACTE";
    private static final String PREVU_CA_TTC = "PREVU_CA_TTC";
    private static final String PREVU_ENCAISSEMENT = "PREVU_ENCAISSEMENT";
    private static final String BASE_CA = "BASE_CA";

    /** Toutes les mesures « réel » du §3.2. */
    private static final String[] REEL = {CONCRETISATIONS, CONCRETISATIONS_NETTES, CONCRETISATIONS_MONTANT,
            AVANCES_ENCAISSEES, CA_QUANTITE, CA_TTC, CA_HT, LIVRAISONS, ENCAISSEMENTS, DESISTEMENTS,
            DESISTEMENTS_MONTANT, ANNULATIONS, ANNULATIONS_MONTANT, FRAIS_DOSSIER, FRAIS_TRANSFERT,
            FRAIS_DESISTEMENT_DUS, APRES_ACTE};

    private static final LocalDate AVRIL = LocalDate.of(2024, 4, 10);
    private static final BigDecimal MILLE = new BigDecimal("1000");

    @Autowired private ApplicationContext contexte;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private MontantsParNatureDossier montantsParNature;
    @Autowired private ComFolderRepository dossiers;
    @Autowired private EntityManagerFactory emf;

    private FluxParReflexion flux;
    private Jeu jeu;
    private Statuts st;
    private CfgProject projet;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche t3;

    @BeforeEach
    void setUp() {
        flux = new FluxParReflexion(contexte);
        jeu = fixture.jeu("FLX");
        st = jeu.statuts();
        projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        t2 = jeu.tranche("T2", projet);
        t3 = jeu.tranche("T3", projet);
    }

    private Flux calculer(List<CfgTranche> tranches, Integer debut, Integer fin, String... mesures) {
        fixture.relire();
        return flux.calculer(tranches.stream().map(CfgTranche::getId).toList(), debut, fin, mesures);
    }

    private Flux surT1(Integer debut, Integer fin, String... mesures) {
        return calculer(List.of(t1), debut, fin, mesures);
    }

    private static BigDecimal m(String valeur) {
        return new BigDecimal(valeur);
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    /** Dossier concrétisé (statut CONCRETISATION, article racine) dans T1. */
    private ComFolder dossierConcretise() {
        return jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 1, 5), null, null, null);
    }

    private ComDocument echeanceAutofinancement(ComFolder d, BigDecimal montant) {
        return jeu.echeance(d, jeu.categorie(Genre.AUTOFINANCEMENT, true), montant, AVRIL);
    }

    private BigDecimal encaissementsAvrilT1() {
        return surT1(2024, 2024, ENCAISSEMENTS).valeur(ENCAISSEMENTS, t1.getId(), 2024, 4);
    }

    // ------------------------------------------------------------------ R-F2 grille creuse

    @Test
    void r_f2_trancheSansMouvementEn2023_aucuneCellule2023_moisAZero() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2022, 3, 10), null, null, null);
        jeu.encaisser(d, "1000", j(2024, 6, 5));

        Flux f = surT1(2022, 2024, CONCRETISATIONS, ENCAISSEMENTS);

        assertThat(f.cles()).containsExactlyInAnyOrder(new Cellule(t1.getId(), 2022, 3),
                new Cellule(t1.getId(), 2024, 6));
        SoftAssertions soft = new SoftAssertions();
        for (int mois = 1; mois <= 12; mois++) {
            soft.assertThat(f.mois(CONCRETISATIONS, 2023, mois)).as("conc. 2023-%d", mois).isEqualByComparingTo("0");
            soft.assertThat(f.mois(ENCAISSEMENTS, 2023, mois)).as("enc. 2023-%d", mois).isEqualByComparingTo("0");
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-F3 dossier annulé / désisté

    @Test
    void r_f3_concretise112023_annule032024_uneConcretisationUneAnnulation_zeroNetteCaAvance() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.annulation()), j(2023, 11, 15), j(2023, 12, 1), null,
                j(2024, 3, 20));
        jeu.prix(d, m("1200000"), m("1000000"));
        ComDocument avance = jeu.echeance(d, jeu.categorie(Genre.AVANCE, true), m("100000"), j(2023, 11, 15));
        jeu.regler(avance, m("100000"), true, IN_WORK, null, j(2023, 11, 15));

        Flux f = surT1(2023, 2024, CONCRETISATIONS, ANNULATIONS, CONCRETISATIONS_NETTES, CA_QUANTITE, CA_TTC, CA_HT,
                AVANCES_ENCAISSEES, DESISTEMENTS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(f.valeur(CONCRETISATIONS, t1.getId(), 2023, 11)).isEqualByComparingTo("1");
        soft.assertThat(f.valeur(ANNULATIONS, t1.getId(), 2024, 3)).isEqualByComparingTo("1");
        soft.assertThat(f.total(DESISTEMENTS)).isEqualByComparingTo("0");
        soft.assertThat(f.total(CONCRETISATIONS_NETTES)).isEqualByComparingTo("0");
        soft.assertThat(f.total(CA_QUANTITE)).isEqualByComparingTo("0");
        soft.assertThat(f.total(CA_TTC)).isEqualByComparingTo("0");
        soft.assertThat(f.total(CA_HT)).isEqualByComparingTo("0");
        soft.assertThat(f.total(AVANCES_ENCAISSEES)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r_f3_concretise112023_desiste032024_uneConcretisationUnDesistement_zeroNetteCaAvance() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.desistement()), j(2023, 11, 15), j(2023, 12, 1), null,
                j(2024, 3, 20));
        ComDocument avance = jeu.echeance(d, jeu.categorie(Genre.AVANCE, true), m("100000"), j(2023, 11, 15));
        jeu.regler(avance, m("100000"), true, IN_WORK, null, j(2023, 11, 15));

        Flux f = surT1(2023, 2024, CONCRETISATIONS, DESISTEMENTS, ANNULATIONS, CONCRETISATIONS_NETTES, CA_QUANTITE,
                CA_TTC, AVANCES_ENCAISSEES);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(f.valeur(CONCRETISATIONS, t1.getId(), 2023, 11)).isEqualByComparingTo("1");
        soft.assertThat(f.valeur(DESISTEMENTS, t1.getId(), 2024, 3)).isEqualByComparingTo("1");
        soft.assertThat(f.total(ANNULATIONS)).isEqualByComparingTo("0");
        soft.assertThat(f.total(CONCRETISATIONS_NETTES)).isEqualByComparingTo("0");
        soft.assertThat(f.total(CA_QUANTITE)).isEqualByComparingTo("0");
        soft.assertThat(f.total(CA_TTC)).isEqualByComparingTo("0");
        soft.assertThat(f.total(AVANCES_ENCAISSEES)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-F4 seuils de statut

    /** Quatre dossiers RES (1), CONC (2), MIN (7), CLEFS (8) : concrétisés 05/2023, acte 06/2023, livrés 07/2023. */
    private void quatreSeuils() {
        for (StrStatus statut : List.of(st.reservation(), st.concretisation(), st.minute(), st.remiseClefs())) {
            jeu.dater(jeu.dossier(t1, statut), j(2023, 5, 10), j(2023, 6, 10), j(2023, 7, 10), null);
        }
    }

    @Test
    void r_f4_concretisations_sortingAuMoinsConcretisation() {
        quatreSeuils();

        Flux f = surT1(2023, 2023, CONCRETISATIONS, CONCRETISATIONS_NETTES);

        assertThat(f.valeur(CONCRETISATIONS, t1.getId(), 2023, 5)).isEqualByComparingTo("3");
        assertThat(f.valeur(CONCRETISATIONS_NETTES, t1.getId(), 2023, 5)).isEqualByComparingTo("3");
    }

    @Test
    void r_f4_chiffreAffaires_sortingAuMoinsFinalisation_prixTtc() {
        quatreSeuils();

        Flux f = surT1(2023, 2023, CA_QUANTITE, CA_TTC);

        assertThat(f.valeur(CA_QUANTITE, t1.getId(), 2023, 6)).isEqualByComparingTo("2");
        assertThat(f.valeur(CA_TTC, t1.getId(), 2023, 6)).isEqualByComparingTo("2400000");
    }

    @Test
    void r_f4_livraisons_sortingAuMoinsLivraison() {
        quatreSeuils();

        assertThat(surT1(2023, 2023, LIVRAISONS).valeur(LIVRAISONS, t1.getId(), 2023, 7)).isEqualByComparingTo("1");
    }

    @Test
    void r_f4_annuleEtDesiste_comptesEnConcretisationsEtLivraisons_pasEnNettesNiCa() {
        for (StrStatus statut : List.of(st.annulation(), st.desistement())) {
            jeu.dater(jeu.dossier(t1, statut), j(2023, 5, 10), j(2023, 6, 10), j(2023, 7, 10), j(2023, 8, 1));
        }

        Flux f = surT1(2023, 2023, CONCRETISATIONS, CONCRETISATIONS_NETTES, LIVRAISONS, CA_QUANTITE);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(f.valeur(CONCRETISATIONS, t1.getId(), 2023, 5)).isEqualByComparingTo("2");
        soft.assertThat(f.valeur(LIVRAISONS, t1.getId(), 2023, 7)).isEqualByComparingTo("2");
        soft.assertThat(f.total(CONCRETISATIONS_NETTES)).isEqualByComparingTo("0");
        soft.assertThat(f.total(CA_QUANTITE)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r_f4_parametreConcretisationAbsent_mesuresConcerneesAZero_nomDansAnomalies_sansException() {
        quatreSeuils();
        fixture.retirerParametre(FixtureFluxVente.SEUIL_CONCRETISATION);

        Flux f = surT1(2023, 2023, CONCRETISATIONS, CONCRETISATIONS_NETTES, CA_QUANTITE);

        assertThat(f.total(CONCRETISATIONS)).isEqualByComparingTo("0");
        assertThat(f.total(CONCRETISATIONS_NETTES)).isEqualByComparingTo("0");
        assertThat(f.total(CA_QUANTITE)).as("seuil de finalisation toujours présent").isEqualByComparingTo("2");
        assertThat(f.anomalies().parametresAbsents()).contains(FixtureFluxVente.SEUIL_CONCRETISATION);
    }

    // ------------------------------------------------------------------ R-F5 / I7 dossier sans statut

    @Test
    void r_f5_dossierSansStatut_absentDeToutesLesMesures_uneAnomalie() {
        ComFolder d = jeu.dater(jeu.dossier(t1, null), j(2024, 2, 1), j(2024, 3, 1), j(2024, 4, 1), j(2024, 5, 1));
        jeu.encaisser(d, "5000", j(2024, 2, 1));
        jeu.regler(jeu.echeance(d, jeu.categorie(Genre.AVANCE, true), m("700"), j(2024, 2, 1)), m("700"), true,
                IN_WORK, null, j(2024, 2, 1));

        Flux f = surT1(null, null, REEL);

        SoftAssertions soft = new SoftAssertions();
        for (String mesure : REEL) {
            soft.assertThat(f.total(mesure)).as(mesure).isEqualByComparingTo("0");
        }
        soft.assertThat(f.anomalies().dossiersSansStatut()).isEqualTo(1L);
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-F6 / §3.3 règle d'un règlement

    @Test
    void r_f6_reglementEnCoursAuCredit_compte() {
        jeu.regler(echeanceAutofinancement(dossierConcretise(), MILLE), MILLE, true, IN_WORK, null, AVRIL);

        assertThat(encaissementsAvrilT1()).isEqualByComparingTo("1000");
    }

    @Test
    void r_f6_reglementPointeAuCredit_compte() {
        jeu.regler(echeanceAutofinancement(dossierConcretise(), m("200")), m("200"), true, POINTED, null, AVRIL);

        assertThat(encaissementsAvrilT1()).isEqualByComparingTo("200");
    }

    @Test
    void r_f6_e1_reglementAnnule_exclu() {
        ComFolder d = dossierConcretise();
        jeu.regler(echeanceAutofinancement(d, MILLE), MILLE, true, IN_WORK, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("5000")), m("5000"), true, CANCELED, null, AVRIL);

        assertThat(encaissementsAvrilT1()).isEqualByComparingTo("1000");
    }

    @Test
    void r_f6_decaissement_isCreditFaux_soustrait() {
        ComFolder d = dossierConcretise();
        jeu.regler(echeanceAutofinancement(d, MILLE), MILLE, true, IN_WORK, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("300")), m("300"), false, IN_WORK, null, AVRIL);

        assertThat(encaissementsAvrilT1()).isEqualByComparingTo("700");
    }

    @Test
    void r_f6_e2_isCreditNull_sansStatut_compteEnEncaissement() {
        jeu.regler(echeanceAutofinancement(dossierConcretise(), m("40")), m("40"), null, null, null, AVRIL);

        assertThat(encaissementsAvrilT1()).isEqualByComparingTo("40");
    }

    @Test
    void r_f6_typeDePieceNonComptabilise_exclu() {
        ComFolder d = dossierConcretise();
        jeu.regler(echeanceAutofinancement(d, MILLE), MILLE, true, IN_WORK, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("7000")), m("7000"), true, IN_WORK, null, AVRIL,
                jeu.typeReglement(false));

        assertThat(encaissementsAvrilT1()).isEqualByComparingTo("1000");
    }

    @Test
    void r_f6_categorieDeFrais_exclue() {
        ComFolder d = dossierConcretise();
        jeu.regler(echeanceAutofinancement(d, MILLE), MILLE, true, IN_WORK, null, AVRIL);
        ComDocument frais = jeu.echeance(d, jeu.categorie(Genre.FRAIS, true), m("9000"), AVRIL);
        jeu.regler(frais, m("9000"), true, IN_WORK, null, AVRIL);

        assertThat(encaissementsAvrilT1()).isEqualByComparingTo("1000");
    }

    @Test
    void r_f6_dossierSurSousArticle_exclu() {
        jeu.regler(echeanceAutofinancement(dossierConcretise(), MILLE), MILLE, true, IN_WORK, null, AVRIL);
        ComFolder sous = jeu.dater(jeu.dossierSousArticle(t1, st.concretisation()), j(2024, 1, 5), null, null, null);
        jeu.regler(echeanceAutofinancement(sous, m("11000")), m("11000"), true, IN_WORK, null, AVRIL);

        assertThat(encaissementsAvrilT1()).isEqualByComparingTo("1000");
    }

    @Test
    void r_f6_dateDuMouvement_reglementSinonEcheance() {
        ComFolder d = dossierConcretise();
        jeu.regler(echeanceAutofinancement(d, MILLE), MILLE, true, IN_WORK, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("200")), m("200"), true, POINTED, j(2024, 5, 3), AVRIL);

        Flux f = surT1(2024, 2024, ENCAISSEMENTS);

        assertThat(f.valeur(ENCAISSEMENTS, t1.getId(), 2024, 4)).isEqualByComparingTo("1000");
        assertThat(f.valeur(ENCAISSEMENTS, t1.getId(), 2024, 5)).isEqualByComparingTo("200");
    }

    @Test
    void r_f6_e3_ligneSansAucuneDate_horsGrille_nombreEtMontantEnAnomalie() {
        ComFolder d = dossierConcretise();
        jeu.regler(echeanceAutofinancement(d, MILLE), MILLE, true, IN_WORK, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("20000")), m("20000"), true, IN_WORK, null, null);

        Flux f = surT1(null, null, ENCAISSEMENTS);

        assertThat(f.total(ENCAISSEMENTS)).isEqualByComparingTo("1000");
        assertThat(f.anomalies().reglementsSansDate()).usingElementComparator(Comparator.naturalOrder())
                .contains(BigDecimal.ONE, m("20000"));
    }

    /** I8 : toutes les règles d'un règlement à la fois, sur une même échéance de mois. */
    @Test
    void i8_jeuComplet_creditsNonAnnulesPlusNullMoinsDecaissements_horsFraisNonComptabiliseSousArticle() {
        ComFolder d = dossierConcretise();
        jeu.regler(echeanceAutofinancement(d, MILLE), MILLE, true, IN_WORK, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("200")), m("200"), true, POINTED, j(2024, 5, 3), AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("5000")), m("5000"), true, CANCELED, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("300")), m("300"), false, IN_WORK, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("40")), m("40"), null, null, null, AVRIL);
        jeu.regler(echeanceAutofinancement(d, m("7000")), m("7000"), true, IN_WORK, null, AVRIL,
                jeu.typeReglement(false));
        jeu.regler(jeu.echeance(d, jeu.categorie(Genre.FRAIS, true), m("9000"), AVRIL), m("9000"), true, IN_WORK,
                null, AVRIL);
        ComFolder sous = jeu.dater(jeu.dossierSousArticle(t1, st.concretisation()), j(2024, 1, 5), null, null, null);
        jeu.regler(echeanceAutofinancement(sous, m("11000")), m("11000"), true, IN_WORK, null, AVRIL);

        Flux f = surT1(2024, 2024, ENCAISSEMENTS);

        assertThat(f.valeur(ENCAISSEMENTS, t1.getId(), 2024, 4)).isEqualByComparingTo("740");
        assertThat(f.valeur(ENCAISSEMENTS, t1.getId(), 2024, 5)).isEqualByComparingTo("200");
        assertThat(f.annee(ENCAISSEMENTS, 2024)).isEqualByComparingTo("940");
    }

    // ------------------------------------------------------------------ R-F7 frais

    /** Frais d'avril 2024 : dossier 100 (+ 30 sur sous-article), transfert 250, syndic 999, frais sans paramètre 77. */
    private void fraisDAvril() {
        ComSettlementCategory dossierFrais = jeu.categorie(Genre.FRAIS, true);
        ComSettlementCategory transfert = jeu.categorie(Genre.FRAIS, true);
        ComSettlementCategory syndic = jeu.categorie(Genre.FRAIS, true);
        ComSettlementCategory autre = jeu.categorie(Genre.FRAIS, true);
        fixture.parametre(FixtureFluxVente.FRAIS_DOSSIER, dossierFrais.getId());
        fixture.parametre(FixtureFluxVente.FRAIS_AUTRES, null);
        fixture.parametre(FixtureFluxVente.FRAIS_TRANSFERT_CLIENT, transfert.getId());
        fixture.retirerParametre(FixtureFluxVente.FRAIS_TRANSFERT_PRODUIT);
        fixture.parametre(FixtureFluxVente.FRAIS_SYNDIC, syndic.getId());

        ComFolder d = dossierConcretise();
        ComFolder sous = jeu.dossierSousArticle(t1, st.concretisation());
        payer(d, dossierFrais, "100");
        payer(sous, dossierFrais, "30");
        payer(d, transfert, "250");
        payer(d, syndic, "999");
        payer(d, autre, "77");
    }

    private void payer(ComFolder d, ComSettlementCategory categorie, String montant) {
        jeu.regler(jeu.echeance(d, categorie, m(montant), AVRIL), m(montant), true, IN_WORK, null, AVRIL);
    }

    @Test
    void r_f7_fraisDeDossier_categorieDuParametre_sansFiltreDArticle_parametreNullIgnore() {
        fraisDAvril();

        Flux f = surT1(2024, 2024, FRAIS_DOSSIER);

        assertThat(f.valeur(FRAIS_DOSSIER, t1.getId(), 2024, 4)).isEqualByComparingTo("130");
    }

    @Test
    void r_f7_fraisDeTransfert_categorieDuParametre() {
        fraisDAvril();

        Flux f = surT1(2024, 2024, FRAIS_TRANSFERT);

        assertThat(f.valeur(FRAIS_TRANSFERT, t1.getId(), 2024, 4)).isEqualByComparingTo("250");
    }

    @Test
    void r_f7_syndicEtFraisSansParametre_dansAucuneMesure_niEncaissements() {
        fraisDAvril();

        Flux f = surT1(2024, 2024, FRAIS_DOSSIER, FRAIS_TRANSFERT, ENCAISSEMENTS);

        assertThat(f.total(FRAIS_DOSSIER).add(f.total(FRAIS_TRANSFERT))).isEqualByComparingTo("380");
        assertThat(f.total(ENCAISSEMENTS)).isEqualByComparingTo("0");
    }

    @Test
    void r_f7_fraisDeDesistementDus_echeanceDeCategorieAnnulation_dateDeLEcheance() {
        ComSettlementCategory annulation = jeu.categorie(Genre.FRAIS, true);
        fixture.parametre(FixtureFluxVente.FRAIS_ANNULATION, annulation.getId());
        ComFolder d = jeu.dater(jeu.dossier(t1, st.desistement()), j(2023, 6, 1), null, null, j(2024, 2, 1));
        jeu.echeance(d, annulation, m("1500"), j(2024, 2, 15));
        jeu.echeance(d, jeu.categorie(Genre.FRAIS, true), m("800"), j(2024, 2, 15));

        Flux f = surT1(2024, 2024, FRAIS_DESISTEMENT_DUS);

        assertThat(f.valeur(FRAIS_DESISTEMENT_DUS, t1.getId(), 2024, 2)).isEqualByComparingTo("1500");
        assertThat(f.total(FRAIS_DESISTEMENT_DUS)).isEqualByComparingTo("1500");
    }

    // ------------------------------------------------------------------ R-F8 / I1 partition

    @Test
    void r_f8_i1_partition_sommeDesTranches_egalAnnee_sommeDesMois_egalAnnee_sommeDesAnnees_egalTotal() {
        int rang = 0;
        for (CfgTranche t : List.of(t1, t2, t3)) {
            for (int annee = 2022; annee <= 2024; annee++) {
                rang++;
                ComFolder d = jeu.dater(jeu.dossier(t, st.minute()), j(annee, 1 + rang % 12, 3),
                        j(annee, 1 + (rang + 4) % 12, 3), null, null);
                jeu.encaisser(d, String.valueOf(1000 * rang), j(annee, 1 + (rang + 2) % 12, 9));
            }
        }

        Flux f = calculer(List.of(t1, t2, t3), 2022, 2024, CONCRETISATIONS, ENCAISSEMENTS, CA_TTC);

        SoftAssertions soft = new SoftAssertions();
        for (String mesure : List.of(CONCRETISATIONS, ENCAISSEMENTS, CA_TTC)) {
            BigDecimal annees = BigDecimal.ZERO;
            for (int annee = 2022; annee <= 2024; annee++) {
                BigDecimal tranches = BigDecimal.ZERO;
                for (CfgTranche t : List.of(t1, t2, t3)) {
                    tranches = tranches.add(f.tranche(mesure, t.getId(), annee));
                }
                BigDecimal mois = BigDecimal.ZERO;
                for (int k = 1; k <= 12; k++) {
                    mois = mois.add(f.mois(mesure, annee, k));
                }
                soft.assertThat(tranches).as("%s %d Σ tranches", mesure, annee).isEqualByComparingTo(f.annee(mesure, annee));
                soft.assertThat(mois).as("%s %d Σ mois", mesure, annee).isEqualByComparingTo(f.annee(mesure, annee));
                annees = annees.add(f.annee(mesure, annee));
            }
            soft.assertThat(annees).as("%s Σ années", mesure).isEqualByComparingTo(f.total(mesure));
        }
        soft.assertThat(f.total(ENCAISSEMENTS)).isEqualByComparingTo("45000");
        soft.assertThat(f.total(CONCRETISATIONS)).isEqualByComparingTo("9");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-F9 / I9 un seul calcul du signé

    @Test
    void r_f9_i9_encaissementsToutesAnnees_egalReglesHorsFraisDeMontantsParNature() {
        ComFolder d = dossierConcretise();
        ComSettlementCategory avance = jeu.categorie(Genre.AVANCE, true);
        ComSettlementCategory credit = jeu.categorie(Genre.CREDIT, true);
        ComDocument eAvance = jeu.echeance(d, avance, m("150"), j(2023, 3, 1));
        jeu.regler(eAvance, m("100"), true, IN_WORK, null, j(2023, 3, 1));
        jeu.regler(eAvance, m("50"), true, POINTED, j(2023, 4, 2), j(2023, 3, 1));
        jeu.regler(eAvance, m("20"), false, IN_WORK, null, j(2023, 5, 1));
        jeu.regler(eAvance, m("999"), true, CANCELED, null, j(2023, 5, 1));
        jeu.regler(echeanceAutofinancement(d, m("300")), m("300"), true, IN_WORK, null, AVRIL);
        ComDocument eCredit = jeu.echeance(d, credit, m("407"), j(2024, 7, 1));
        jeu.regler(eCredit, m("400"), true, IN_WORK, null, j(2024, 7, 1));
        jeu.regler(eCredit, m("7"), null, null, null, j(2024, 8, 1));

        Flux f = surT1(null, null, ENCAISSEMENTS);
        Map<ColonneDossier, BigDecimal> regles = montantsParNature
                .pour(List.of(dossiers.findById(d.getId()).orElseThrow())).get(d.getId());

        BigDecimal attendu = regles.get(ColonneDossier.AVANCE_REGLEE).add(regles.get(ColonneDossier.AUTOFINANCEMENT_REGLE))
                .add(regles.get(ColonneDossier.CREDIT_REGLE)).add(regles.get(ColonneDossier.MODIFICATIONS_REGLEES))
                .add(regles.get(ColonneDossier.PENALITES_REGLES));
        assertThat(f.total(ENCAISSEMENTS)).isEqualByComparingTo(attendu).isEqualByComparingTo("837");
    }

    @Test
    void r_f9_regleEncaissement_porteLeSigneEtLExclusionPartages() {
        List<String> fragments = FluxParReflexion.constantes("RegleEncaissement");

        assertThat(fragments).isNotEmpty()
                .anyMatch(s -> s.contains(StatutReglement.SQL_MONTANT_SIGNE)
                        || s.contains(StlSettlementLineRepository.MONTANT_SIGNE))
                .anyMatch(s -> s.contains(StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX)
                        || s.contains(StatutReglement.JPQL_COMPTE_DANS_LES_TOTAUX));
    }

    // ------------------------------------------------------------------ R-F10 montants dus

    @Test
    void r_f10_concretisationsMontant_egalAvanceDeMontantsParNature() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 3, 5), null, null, null);
        jeu.echeance(d, jeu.categorie(Genre.AVANCE, true), m("100000"), j(2024, 3, 5));
        jeu.echeance(d, jeu.categorie(Genre.AVANCE, true), m("50000"), j(2024, 4, 5));
        jeu.echeance(d, jeu.categorie(Genre.AVANCE, false), m("7000"), j(2024, 4, 5));
        jeu.echeance(d, jeu.categorie(Genre.AUTOFINANCEMENT, true), m("300"), j(2024, 4, 5));

        Flux f = surT1(2024, 2024, CONCRETISATIONS_MONTANT);
        BigDecimal avance = montantsParNature.pour(List.of(dossiers.findById(d.getId()).orElseThrow()))
                .get(d.getId()).get(ColonneDossier.AVANCE);

        assertThat(f.valeur(CONCRETISATIONS_MONTANT, t1.getId(), 2024, 3)).isEqualByComparingTo(avance)
                .isEqualByComparingTo("150000");
    }

    /** Échéances de résiliation au débit 8 000 + 2 000 ; résiliation au crédit 500 et avance au débit 300 ignorées. */
    private void echeancesDeSortie(ComFolder d) {
        jeu.echeance(d, jeu.categorie(Genre.RESILIATION, false), m("8000"), j(2024, 6, 12));
        jeu.echeance(d, jeu.categorie(Genre.RESILIATION, false), m("2000"), j(2024, 7, 12));
        jeu.echeance(d, jeu.categorie(Genre.RESILIATION, true), m("500"), j(2024, 6, 12));
        jeu.echeance(d, jeu.categorie(Genre.AVANCE, false), m("300"), j(2024, 6, 12));
    }

    @Test
    void r_f10_desistementsMontant_sommeDesResiliationsAuDebit_moisDeCancelDate() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.desistement()), j(2023, 2, 1), null, null, j(2024, 6, 12));
        echeancesDeSortie(d);

        Flux f = surT1(2024, 2024, DESISTEMENTS_MONTANT, ANNULATIONS_MONTANT);

        assertThat(f.valeur(DESISTEMENTS_MONTANT, t1.getId(), 2024, 6)).isEqualByComparingTo("10000");
        assertThat(f.total(ANNULATIONS_MONTANT)).isEqualByComparingTo("0");
    }

    @Test
    void r_f10_annulationsMontant_memeSomme_moisDeCancelDate() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.annulation()), j(2023, 2, 1), null, null, j(2024, 6, 12));
        echeancesDeSortie(d);

        Flux f = surT1(2024, 2024, DESISTEMENTS_MONTANT, ANNULATIONS_MONTANT);

        assertThat(f.valeur(ANNULATIONS_MONTANT, t1.getId(), 2024, 6)).isEqualByComparingTo("10000");
        assertThat(f.total(DESISTEMENTS_MONTANT)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ R-F11 précision

    @Test
    void r_f11_precision_centimesConserves_pasDArrondiALEntier() {
        ComFolder d = dossierConcretise();
        jeu.regler(echeanceAutofinancement(d, m("190.75")), m("190.75"), true, IN_WORK, null, j(2024, 1, 10));
        jeu.regler(echeanceAutofinancement(d, m("0.25")), m("0.25"), true, IN_WORK, null, j(2024, 1, 11));
        jeu.regler(echeanceAutofinancement(d, m("0.40")), m("0.40"), true, IN_WORK, null, j(2024, 2, 10));

        Flux f = surT1(2024, 2024, ENCAISSEMENTS);

        assertThat(f.valeur(ENCAISSEMENTS, t1.getId(), 2024, 1)).isEqualByComparingTo("191.00");
        assertThat(f.valeur(ENCAISSEMENTS, t1.getId(), 2024, 2)).isEqualByComparingTo("0.40");
    }

    // ------------------------------------------------------------------ R-F12 lots de 2 000 identifiants

    @Test
    void r_f12_2500Tranches_resultatIdentiqueAUnAppelSurLaSeuleTrancheReelle() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.minute()), j(2024, 3, 3), j(2024, 4, 4), null, null);
        jeu.encaisser(d, "1234.56", j(2024, 5, 5));
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 2_499; i++) {
            ids.add(i == 2_300 ? t1.getId() : UUID.randomUUID());
        }
        ids.add(t2.getId());

        fixture.relire();
        Flux lots = flux.calculer(ids, 2024, 2024, CONCRETISATIONS, ENCAISSEMENTS, CA_TTC);
        Flux seul = surT1(2024, 2024, CONCRETISATIONS, ENCAISSEMENTS, CA_TTC);

        assertThat(lots.cles()).containsExactlyInAnyOrderElementsOf(seul.cles());
        assertThat(lots.total(ENCAISSEMENTS)).isEqualByComparingTo(seul.total(ENCAISSEMENTS))
                .isEqualByComparingTo("1234.56");
        assertThat(lots.total(CONCRETISATIONS)).isEqualByComparingTo("1");
        assertThat(lots.total(CA_TTC)).isEqualByComparingTo("1200000");
    }

    // ------------------------------------------------------------------ R-F13 entrées invalides

    @Test
    void r_f13_anneeDebutApresAnneeFin_illegalArgument() {
        List<UUID> tranches = List.of(t1.getId());

        assertThatThrownBy(() -> flux.calculer(tranches, 2025, 2024, ENCAISSEMENTS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void r_f13_mesuresVides_illegalArgument() {
        List<UUID> tranches = List.of(t1.getId());

        assertThatThrownBy(() -> flux.calculer(tranches, 2024, 2024)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void r_f13_tranchesNull_illegalArgument() {
        assertThatThrownBy(() -> flux.calculer(null, 2024, 2024, ENCAISSEMENTS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void r_f13_tranchesVides_fluxVide_aucuneRequete() {
        jeu.encaisser(dossierConcretise(), "1000", AVRIL);
        fixture.relire();
        Statistics statistiques = emf.unwrap(SessionFactory.class).getStatistics();
        boolean actives = statistiques.isStatisticsEnabled();
        statistiques.setStatisticsEnabled(true);
        statistiques.clear();
        Flux f;
        long requetes;
        try {
            f = flux.calculer(List.of(), null, null, ENCAISSEMENTS, CONCRETISATIONS, CA_TTC);
            requetes = statistiques.getPrepareStatementCount();
        } finally {
            statistiques.setStatisticsEnabled(actives);
        }

        assertThat(requetes).isZero();
        assertThat(f.cles()).isEmpty();
        assertThat(f.total(ENCAISSEMENTS)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ R-V6 aucune lecture hors tranches demandées

    @Test
    void r_v6_deuxTranches_uneDemandee_aucuneCelluleDeLAutre() {
        jeu.encaisser(jeu.dater(jeu.dossier(t1, st.minute()), j(2024, 2, 2), j(2024, 3, 3), null, null), "1000", AVRIL);
        jeu.encaisser(jeu.dater(jeu.dossier(t2, st.minute()), j(2024, 2, 2), j(2024, 3, 3), null, null), "7777", AVRIL);

        Flux f = surT1(2024, 2024, CONCRETISATIONS, CA_TTC, ENCAISSEMENTS);

        assertThat(f.cles()).isNotEmpty().allMatch(c -> c.tranche().equals(t1.getId()));
        assertThat(f.total(ENCAISSEMENTS)).isEqualByComparingTo("1000");
        assertThat(f.total(CONCRETISATIONS)).isEqualByComparingTo("1");
    }

    // ------------------------------------------------------------------ I5 / I6

    @Test
    void i5_concretisationsMoinsNettes_egalNombreDAnnulesEtDesistesConcretises() {
        for (StrStatus statut : List.of(st.annulation(), st.annulation(), st.desistement(), st.concretisation(),
                st.minute())) {
            jeu.dater(jeu.dossier(t1, statut), j(2022, 9, 9), null, null, null);
        }

        Flux f = surT1(2022, 2022, CONCRETISATIONS, CONCRETISATIONS_NETTES);

        assertThat(f.annee(CONCRETISATIONS, 2022).subtract(f.annee(CONCRETISATIONS_NETTES, 2022)))
                .isEqualByComparingTo("3");
    }

    @Test
    void i6_acteSansAdvanceDate_compteEnChiffreAffaires_pasEnConcretisations_prixNulVautZero() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.minute()), null, j(2023, 9, 10), null, null);
        jeu.prix(d, m("900000"), m("750000"));
        jeu.prix(jeu.dater(jeu.dossier(t1, st.minute()), null, j(2023, 9, 20), null, null), null, null);

        Flux f = surT1(2023, 2023, CA_QUANTITE, CA_TTC, CA_HT, CONCRETISATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(f.valeur(CA_QUANTITE, t1.getId(), 2023, 9)).isEqualByComparingTo("2");
        soft.assertThat(f.valeur(CA_TTC, t1.getId(), 2023, 9)).isEqualByComparingTo("900000");
        soft.assertThat(f.valeur(CA_HT, t1.getId(), 2023, 9)).isEqualByComparingTo("750000");
        soft.assertThat(f.total(CONCRETISATIONS)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ AVANCES_ENCAISSEES (§3.2)

    @Test
    void avances_reglementDAvanceEnContexteDAccord_auMoisDuMouvement() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 1, 10), null, null, null);
        jeu.regler(jeu.echeance(d, jeu.categorie(Genre.AVANCE, true), m("5000"), j(2024, 2, 3)), m("5000"), true,
                IN_WORK, null, j(2024, 2, 3));

        Flux f = surT1(2024, 2024, AVANCES_ENCAISSEES);

        assertThat(f.valeur(AVANCES_ENCAISSEES, t1.getId(), 2024, 2)).isEqualByComparingTo("5000");
    }

    @Test
    void avances_categorieHorsContexteDAccord_exclue_maisCompteeEnEncaissements() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 1, 10), null, null, null);
        jeu.regler(jeu.echeance(d, jeu.categorie(Genre.AVANCE_HORS_ACCORD, true), m("3000"), AVRIL), m("3000"), true,
                IN_WORK, null, AVRIL);

        Flux f = surT1(2024, 2024, AVANCES_ENCAISSEES, ENCAISSEMENTS);

        assertThat(f.total(AVANCES_ENCAISSEES)).isEqualByComparingTo("0");
        assertThat(f.total(ENCAISSEMENTS)).isEqualByComparingTo("3000");
    }

    @Test
    void avances_dossierSansAdvanceDate_ouSousLeSeuil_ouSurSousArticle_exclus() {
        ComFolder sansDate = jeu.dossier(t1, st.concretisation());
        ComFolder reservation = jeu.dater(jeu.dossier(t1, st.reservation()), j(2024, 1, 10), null, null, null);
        ComFolder sous = jeu.dater(jeu.dossierSousArticle(t1, st.concretisation()), j(2024, 1, 10), null, null, null);
        for (ComFolder d : List.of(sansDate, reservation, sous)) {
            jeu.regler(jeu.echeance(d, jeu.categorie(Genre.AVANCE, true), m("4000"), AVRIL), m("4000"), true,
                    IN_WORK, null, AVRIL);
        }

        assertThat(surT1(2024, 2024, AVANCES_ENCAISSEES).total(AVANCES_ENCAISSEES)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ ENCAISSEMENTS_APRES_ACTE (§3.2)

    @Test
    void apresActe_mouvementAuMoinsALaDateDActe_dossierNiAnnuleNiDesiste() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.minute()), j(2024, 1, 2), j(2024, 5, 10), null, null);
        jeu.encaisser(d, "100", j(2024, 5, 1));
        jeu.encaisser(d, "200", j(2024, 5, 10));
        jeu.encaisser(d, "300", j(2024, 6, 1));
        ComFolder annule = jeu.dater(jeu.dossier(t1, st.annulation()), j(2024, 1, 2), j(2024, 5, 10), null,
                j(2024, 7, 1));
        jeu.encaisser(annule, "500", j(2024, 6, 1));

        Flux f = surT1(2024, 2024, APRES_ACTE);

        assertThat(f.valeur(APRES_ACTE, t1.getId(), 2024, 5)).isEqualByComparingTo("200");
        assertThat(f.valeur(APRES_ACTE, t1.getId(), 2024, 6)).isEqualByComparingTo("300");
        assertThat(f.total(APRES_ACTE)).isEqualByComparingTo("500");
    }

    // ------------------------------------------------------------------ §3.6 prévu non saisi

    @Test
    void prevu_nonSaisi_mesuresPrevuesAZero_drapeauLeve() {
        jeu.dater(jeu.dossier(t1, st.minute()), j(2024, 1, 2), j(2024, 2, 2), null, null);

        Flux f = surT1(2024, 2024, PREVU_CA_TTC, PREVU_ENCAISSEMENT, BASE_CA, CA_TTC);

        assertThat(f.total(PREVU_CA_TTC)).isEqualByComparingTo("0");
        assertThat(f.total(PREVU_ENCAISSEMENT)).isEqualByComparingTo("0");
        assertThat(f.total(BASE_CA)).isEqualByComparingTo("0");
        assertThat(f.total(CA_TTC)).isEqualByComparingTo("1200000");
        assertThat(f.anomalies().prevuNonSaisi()).isTrue();
    }
}
