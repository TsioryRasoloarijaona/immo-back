package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementsParReflexion.Mouvement;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementsParReflexion.Resultat;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.fixtures.FixtureFluxVente.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static com.tsix.immocrm.services.rapport.vente.flux.MouvementsParReflexion.mesure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ajouts du lot Pa au calcul partagé (FEATURE-tdb-vente-previsions.md §3.1, §3.3, §9.1 R-P1…R-P5, carte Pa) : mesures
 * {@code ENCAISSEMENTS_FRAIS}, {@code ECHEANCES_DUES}, {@code ECHEANCES_DUES_FRAIS}, {@code BASE_*} / {@code ANCIEN_PREVU_*},
 * méthode de période {@code encaissements(tranches, du, au)}. Le détail (§3.2) se bâtit sur {@code mouvements} (décision du
 * manager 25/10) : I-P1 est vérifié sur {@code mouvements}. Mesures et méthode appelées par leur nom (réflexion).
 */
@SpringBootTest
@Transactional
class FluxVentePrevisionsTest {

    private static final String ENCAISSEMENTS_FRAIS = "ENCAISSEMENTS_FRAIS";
    private static final String ECHEANCES_DUES = "ECHEANCES_DUES";
    private static final String ECHEANCES_DUES_FRAIS = "ECHEANCES_DUES_FRAIS";
    private static final String CONCRETISATIONS_NETTES = "CONCRETISATIONS_NETTES";
    private static final String ENCAISSEMENTS = "ENCAISSEMENTS";
    /** Mesures non prévues de l'écran (§4.2). */
    private static final List<String> MESURES_ECRAN = List.of(CONCRETISATIONS_NETTES, ENCAISSEMENTS, ENCAISSEMENTS_FRAIS,
            ECHEANCES_DUES, ECHEANCES_DUES_FRAIS);
    /** Les 8 mesures PREVU du §9.1 R-P5 (dont {@code BASE_CONCRETISATION_QUANTITE}, ajoutée par R2a). */
    private static final List<String> PREVUES_PA = List.of("BASE_CONCRETISATION_QUANTITE", "BASE_CONCRETISATION_MONTANT",
            "BASE_CA_QUANTITE", "ANCIEN_PREVU_CONCRETISATION_QUANTITE", "ANCIEN_PREVU_CONCRETISATION_MONTANT",
            "ANCIEN_PREVU_CA_QUANTITE", "ANCIEN_PREVU_CA_TTC", "ANCIEN_PREVU_ENCAISSEMENT");
    private static final int AN = 2024;

    @Autowired private FixtureFluxVente fixture;
    @Autowired private FluxVenteMensuel service;

    private MouvementsParReflexion reflexion;
    private Jeu jeu;
    private Statuts st;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche t3;

    @BeforeEach
    void setUp() {
        reflexion = new MouvementsParReflexion(service);
        jeu = fixture.jeu("PRV");
        st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        t2 = jeu.tranche("T2", projet);
        t3 = jeu.tranche("T3", projet);
    }

    private static LocalDate j(int mois, int jour) {
        return LocalDate.of(AN, mois, jour);
    }

    private static BigDecimal m(String valeur) {
        return new BigDecimal(valeur);
    }

    private static Set<MesureFlux> mesures(List<String> noms) {
        return noms.stream().map(MouvementsParReflexion::mesure).collect(Collectors.toSet());
    }

    private FluxMensuel calculer(List<CfgTranche> tranches, List<String> noms) {
        fixture.relire();
        return service.calculer(new DemandeFlux(ids(tranches), AN, AN, mesures(noms)));
    }

    private static List<UUID> ids(List<CfgTranche> tranches) {
        return tranches.stream().map(CfgTranche::getId).toList();
    }

    private ComFolder concretise(CfgTranche t, LocalDate avance) {
        return jeu.dater(jeu.dossier(t, st.concretisation()), avance, null, null, null);
    }

    private ComDocument echeance(ComFolder d, Genre genre, String montant, LocalDate date) {
        return jeu.echeance(d, jeu.categorie(genre, true), m(montant), date);
    }

    private void payer(ComDocument e, String montant, String statut, LocalDate date) {
        jeu.regler(e, m(montant), true, statut, null, date);
    }

    /** {@code FluxVenteMensuel.encaissements(List<UUID>, LocalDate, LocalDate)} par son nom (§3.3). */
    @SuppressWarnings("unchecked")
    private Map<UUID, BigDecimal> encaissements(List<CfgTranche> tranches, LocalDate du, LocalDate au) {
        fixture.relire();
        Method methode;
        try {
            methode = FluxVenteMensuel.class.getMethod("encaissements", List.class, LocalDate.class, LocalDate.class);
        } catch (NoSuchMethodException absente) {
            throw new IllegalStateException("contrat absent : FluxVenteMensuel.encaissements(List, LocalDate, LocalDate)",
                    absente);
        }
        try {
            return (Map<UUID, BigDecimal>) methode.invoke(service, ids(tranches), du, au);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("encaissements", e);
        }
    }

    // ------------------------------------------------------------------ contrat (§3.1, carte Pa : 10 valeurs)

    @Test
    void contrat_mesuresAjoutees_dansLeurFamille() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(mesure(ENCAISSEMENTS_FRAIS).famille()).as(ENCAISSEMENTS_FRAIS)
                .isEqualTo(MesureFlux.Famille.REGLEMENT);
        soft.assertThat(mesure(ECHEANCES_DUES).famille()).as(ECHEANCES_DUES).isEqualTo(MesureFlux.Famille.ECHEANCE);
        soft.assertThat(mesure(ECHEANCES_DUES_FRAIS).famille()).as(ECHEANCES_DUES_FRAIS)
                .isEqualTo(MesureFlux.Famille.ECHEANCE);
        for (String nom : PREVUES_PA) {
            soft.assertThat(mesure(nom).famille()).as(nom).isEqualTo(MesureFlux.Famille.PREVU);
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-P1 frais encaissés

    @Test
    void rP1_encaissementsFrais_fraisDeDossierPlusSyndic_horsCanceledEtNonComptabilise_absentsDesEncaissements() {
        ComSettlementCategory fraisDossier = jeu.categorie(Genre.FRAIS, true);
        ComSettlementCategory syndic = jeu.categorie(Genre.FRAIS, true);
        fixture.parametre(FixtureFluxVente.FRAIS_DOSSIER, fraisDossier.getId());
        fixture.parametre(FixtureFluxVente.FRAIS_SYNDIC, syndic.getId());
        ComFolder d = concretise(t1, j(2, 1));
        payer(jeu.echeance(d, fraisDossier, m("100"), j(3, 5)), "100", IN_WORK, j(3, 5));
        payer(jeu.echeance(d, syndic, m("40"), j(3, 6)), "40", IN_WORK, j(3, 6));
        payer(echeance(d, Genre.AUTOFINANCEMENT, "1000", j(3, 7)), "1000", IN_WORK, j(3, 7));
        payer(jeu.echeance(d, fraisDossier, m("900"), j(3, 8)), "900", CANCELED, j(3, 8));
        jeu.regler(jeu.echeance(d, syndic, m("800"), j(3, 9)), m("800"), true, IN_WORK, null, j(3, 9),
                jeu.typeReglement(false));

        FluxMensuel flux = calculer(List.of(t1), List.of(ENCAISSEMENTS_FRAIS, ENCAISSEMENTS));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(flux.valeur(mesure(ENCAISSEMENTS_FRAIS), t1.getId(), AN, 3)).as("frais de dossier + syndic")
                .isEqualByComparingTo("140");
        soft.assertThat(flux.valeur(MesureFlux.ENCAISSEMENTS, t1.getId(), AN, 3)).as("aucun frais")
                .isEqualByComparingTo("1000");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-P2 échéances dues

    /** Premier dossier CONCRETISATION : avance 100, crédit 200, autofinancement 400, frais seuls 70, autre 8 000. */
    private void echeancesDuPremierDossier() {
        ComFolder d = concretise(t1, j(1, 10));
        echeance(d, Genre.AVANCE, "100", j(3, 1));
        echeance(d, Genre.CREDIT, "200", j(3, 15));
        echeance(d, Genre.AUTOFINANCEMENT, "400", j(4, 2));
        echeance(d, Genre.FRAIS, "70", j(3, 20));
        echeance(d, Genre.RESILIATION, "8000", j(3, 21));
    }

    @Test
    void rP2_echeancesDues_avanceCreditAutofinancement_auMoisDeDocDate_fraisSeulsAPart_autreCategorieAbsente() {
        echeancesDuPremierDossier();
        ComFolder reservation = jeu.dossier(t1, st.reservation());
        echeance(reservation, Genre.AVANCE, "5000", j(3, 3));
        ComFolder annule = jeu.dater(jeu.dossier(t1, st.annulation()), j(1, 11), null, null, j(5, 1));
        echeance(annule, Genre.AVANCE, "6000", j(3, 4));
        echeance(annule, Genre.FRAIS, "600", j(3, 4));

        FluxMensuel flux = calculer(List.of(t1), List.of(ECHEANCES_DUES, ECHEANCES_DUES_FRAIS));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(flux.valeur(mesure(ECHEANCES_DUES), t1.getId(), AN, 3)).as("mars : avance + crédit")
                .isEqualByComparingTo("300");
        soft.assertThat(flux.valeur(mesure(ECHEANCES_DUES), t1.getId(), AN, 4)).as("avril : autofinancement")
                .isEqualByComparingTo("400");
        soft.assertThat(flux.valeur(mesure(ECHEANCES_DUES_FRAIS), t1.getId(), AN, 3)).as("frais seuls")
                .isEqualByComparingTo("70");
        soft.assertThat(flux.total(mesure(ECHEANCES_DUES))).as("ni réservation, ni annulé, ni catégorie autre")
                .isEqualByComparingTo("700");
        soft.assertThat(flux.total(mesure(ECHEANCES_DUES_FRAIS))).isEqualByComparingTo("70");
        soft.assertAll();
    }

    @Test
    void rP2_echeancesDues_dossierDesisteEtStatutMinute_desisteAbsent_minuteCompte() {
        ComFolder desiste = jeu.dater(jeu.dossier(t1, st.desistement()), j(1, 5), null, null, j(6, 1));
        echeance(desiste, Genre.CREDIT, "3000", j(3, 3));
        ComFolder minute = jeu.dater(jeu.dossier(t1, st.minute()), j(1, 6), j(2, 6), null, null);
        echeance(minute, Genre.CREDIT, "250", j(3, 3));

        FluxMensuel flux = calculer(List.of(t1), List.of(ECHEANCES_DUES));

        assertThat(flux.total(mesure(ECHEANCES_DUES))).isEqualByComparingTo("250");
    }

    // §3.1 : « aucun filtre d'article (comme l'écran) »
    @Test
    void rP2_echeancesDues_dossierSurSousArticle_compte() {
        ComFolder sous = jeu.dater(jeu.dossierSousArticle(t1, st.concretisation()), j(1, 5), null, null, null);
        echeance(sous, Genre.AVANCE, "123", j(3, 3));

        FluxMensuel flux = calculer(List.of(t1), List.of(ECHEANCES_DUES));

        assertThat(flux.valeur(mesure(ECHEANCES_DUES), t1.getId(), AN, 3)).isEqualByComparingTo("123");
    }

    @Test
    void rP2_seuilDeConcretisationAbsent_echeancesDuesA0_parametreSignale() {
        echeancesDuPremierDossier();
        fixture.retirerParametre(FixtureFluxVente.SEUIL_CONCRETISATION);

        FluxMensuel flux = calculer(List.of(t1), List.of(ECHEANCES_DUES, ECHEANCES_DUES_FRAIS));

        assertThat(flux.total(mesure(ECHEANCES_DUES))).isEqualByComparingTo("0");
        assertThat(flux.total(mesure(ECHEANCES_DUES_FRAIS))).isEqualByComparingTo("0");
        assertThat(flux.anomalies().parametresAbsents()).contains(FixtureFluxVente.SEUIL_CONCRETISATION);
    }

    // ------------------------------------------------------------------ R-P3 détail = cellule (I-P1)

    /** Sur chaque tranche : 2 concrétisations (mars, avril), encaissements, frais encaissés, échéances dues et de frais. */
    private void jeuDeTroisTranches() {
        int rang = 0;
        for (CfgTranche t : List.of(t1, t2, t3)) {
            rang++;
            ComFolder mars = concretise(t, j(3, rang));
            concretise(t, j(4, rang));
            ComDocument auto = echeance(mars, Genre.AUTOFINANCEMENT, "2000", j(3, 10 + rang));
            payer(auto, "1000", IN_WORK, j(3, 10 + rang));
            payer(auto, "500", IN_WORK, j(4, 10 + rang));
            ComDocument frais = echeance(mars, Genre.FRAIS, "70", j(4, 1));
            payer(frais, "50", IN_WORK, j(4, 2));
            echeance(mars, Genre.AVANCE, String.valueOf(100 * rang), j(4, 20));
        }
    }

    private static String cle(String mesure, UUID tranche, int annee, int mois) {
        return mesure + "|" + tranche + "|" + annee + "|" + mois;
    }

    @Test
    void rP3_sommeDesLignesSourcesParCellule_egalCalculer_mesuresDeLEcran_deuxMois() {
        jeuDeTroisTranches();
        List<CfgTranche> demandees = List.of(t1, t2, t3);
        fixture.relire();

        Resultat r = reflexion.mouvements(new DemandeFlux(ids(demandees), AN, AN, mesures(MESURES_ECRAN)));
        FluxMensuel flux = service.calculer(new DemandeFlux(ids(demandees), AN, AN, mesures(MESURES_ECRAN)));

        Map<String, BigDecimal> sommes = new HashMap<>();
        for (Mouvement l : r.lignes()) {
            sommes.merge(cle(l.mesure(), l.trancheId(), l.date().getYear(), l.date().getMonthValue()), l.valeur(),
                    BigDecimal::add);
        }
        assertThat(r.lignes()).extracting(Mouvement::mesure).containsAll(MESURES_ECRAN);
        SoftAssertions soft = new SoftAssertions();
        for (String nom : MESURES_ECRAN) {
            for (CfgTranche t : demandees) {
                for (int mois : List.of(3, 4)) {
                    soft.assertThat(sommes.getOrDefault(cle(nom, t.getId(), AN, mois), BigDecimal.ZERO))
                            .as("%s %s %d", nom, t.getCode(), mois)
                            .isEqualByComparingTo(flux.valeur(mesure(nom), t.getId(), AN, mois));
                }
            }
        }
        soft.assertAll();
    }

    @Test
    void rP3_lignesSources_aucuneDUneTrancheNonDemandee() {
        jeuDeTroisTranches();
        fixture.relire();

        Resultat r = reflexion.mouvements(new DemandeFlux(ids(List.of(t1, t2)), AN, AN, mesures(MESURES_ECRAN)));

        assertThat(r.lignes()).isNotEmpty().extracting(Mouvement::trancheId).containsOnly(t1.getId(), t2.getId());
    }

    @Test
    void rP3_lignesSourcesAvecUneMesurePrevue_illegalArgument() {
        DemandeFlux demande = new DemandeFlux(ids(List.of(t1)), AN, AN,
                Set.of(mesure(ECHEANCES_DUES), mesure("ANCIEN_PREVU_ENCAISSEMENT")));

        assertThatThrownBy(() -> reflexion.mouvements(demande)).isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------ R-P4 période (I-P2)

    /** t1 : 31/03 (10), 01/04 (20), 07/04 (40), 08/04 (80), ligne sans date (160) ; t2 : 15/04 (1 000). */
    private void lignesDePeriode() {
        ComFolder d = concretise(t1, j(1, 5));
        jeu.encaisser(d, "10", j(3, 31));
        jeu.encaisser(d, "20", j(4, 1));
        jeu.encaisser(d, "40", j(4, 7));
        jeu.encaisser(d, "80", j(4, 8));
        jeu.regler(echeance(d, Genre.AUTOFINANCEMENT, "160", null), m("160"), true, IN_WORK, null, null);
        jeu.encaisser(concretise(t2, j(1, 6)), "1000", j(4, 15));
    }

    @Test
    void rP4_encaissementsDu01au07Avril_lesDeuxLignesDAvrilBornesIncluses() {
        lignesDePeriode();

        Map<UUID, BigDecimal> periode = encaissements(List.of(t1), j(4, 1), j(4, 7));

        assertThat(periode).containsOnlyKeys(t1.getId())
                .hasEntrySatisfying(t1.getId(), v -> assertThat(v).isEqualByComparingTo("60"));
    }

    @Test
    void rP4_moisCivil_sommeDesTranches_egalMoisDesEncaissementsDuFlux_ligneSansDateAbsente() {
        lignesDePeriode();

        Map<UUID, BigDecimal> avril = encaissements(List.of(t1, t2), j(4, 1), j(4, 30));
        FluxMensuel flux = calculer(List.of(t1, t2), List.of(ENCAISSEMENTS));

        BigDecimal somme = avril.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somme).isEqualByComparingTo(flux.mois(MesureFlux.ENCAISSEMENTS, AN, 4))
                .isEqualByComparingTo("1140");
    }

    @Test
    void rP4_auAvantDu_illegalArgument() {
        List<CfgTranche> tranches = List.of(t1);
        LocalDate du = j(4, 8);
        LocalDate au = j(4, 7);

        assertThatThrownBy(() -> encaissements(tranches, du, au)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rP4_tranchesVides_mapVide() {
        lignesDePeriode();

        assertThat(encaissements(List.of(), j(4, 1), j(4, 30))).isEmpty();
    }

    // ------------------------------------------------------------------ R-P5 prévu

    @Test
    void rP5_huitMesuresPrevues_valent0_prevuNonSaisi() {
        concretise(t1, j(3, 3));
        List<String> noms = new ArrayList<>(PREVUES_PA);
        noms.add(CONCRETISATIONS_NETTES);

        FluxMensuel flux = calculer(List.of(t1), noms);

        SoftAssertions soft = new SoftAssertions();
        for (String nom : PREVUES_PA) {
            soft.assertThat(flux.total(mesure(nom))).as(nom).isEqualByComparingTo("0");
        }
        soft.assertThat(flux.total(MesureFlux.CONCRETISATIONS_NETTES)).isEqualByComparingTo("1");
        soft.assertThat(flux.anomalies().prevuNonSaisi()).isTrue();
        soft.assertAll();
    }
}
