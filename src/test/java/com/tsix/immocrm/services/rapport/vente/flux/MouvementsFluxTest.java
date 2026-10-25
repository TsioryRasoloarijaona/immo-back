package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
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

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.fixtures.FixtureFluxVente.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.POINTED;
import static com.tsix.immocrm.services.rapport.vente.flux.MouvementsParReflexion.mesure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Extension du calcul partagé (FEATURE-tdb-vente-rapports-rythme.md §3) : {@code FluxVenteMensuel.mouvements}, les mesures
 * {@code RESERVATIONS}, {@code ENCAISSEMENTS_VIVANTS}, {@code BASE_CONCRETISATION_QUANTITE} ; invariants I-U1…I-U5, M5, M7.
 * {@code mouvements} est appelé par réflexion ({@link MouvementsParReflexion}) ; seuils posés par {@link FixtureFluxVente}
 * (RES 1, CONC 2, MIN 7, CLEFS 8, ANN 30, DES 31).
 */
@SpringBootTest
@Transactional
class MouvementsFluxTest {

    private static final String RESERVATIONS = "RESERVATIONS";
    private static final String ENCAISSEMENTS = "ENCAISSEMENTS";
    private static final String ENCAISSEMENTS_VIVANTS = "ENCAISSEMENTS_VIVANTS";
    private static final String CONCRETISATIONS_NETTES = "CONCRETISATIONS_NETTES";
    private static final String FRAIS_DESISTEMENT_DUS = "FRAIS_DESISTEMENT_DUS";
    private static final String BASE_CONCRETISATION_QUANTITE = "BASE_CONCRETISATION_QUANTITE";
    private static final LocalDate AVRIL = LocalDate.of(2024, 4, 10);

    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FluxVenteMensuel service;

    private MouvementsParReflexion reflexion;
    private Jeu jeu;
    private Statuts st;
    private CfgTranche t1;
    private CfgTranche t2;

    @BeforeEach
    void setUp() {
        reflexion = new MouvementsParReflexion(service);
        jeu = fixture.jeu("MVT");
        st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        t2 = jeu.tranche("T2", projet);
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private static BigDecimal m(String valeur) {
        return new BigDecimal(valeur);
    }

    private static DemandeFlux demande(List<CfgTranche> tranches, Set<MesureFlux> mesures) {
        return new DemandeFlux(tranches.stream().map(CfgTranche::getId).toList(), null, null, mesures);
    }

    private Resultat mouvements(List<CfgTranche> tranches, String... mesures) {
        fixture.relire();
        Set<MesureFlux> ensemble = Arrays.stream(mesures).map(MouvementsParReflexion::mesure)
                .collect(Collectors.toSet());
        return reflexion.mouvements(demande(tranches, ensemble));
    }

    private Resultat surT1(String... mesures) {
        return mouvements(List.of(t1), mesures);
    }

    private ComFolder saisir(ComFolder dossier, LocalDate saisie) {
        dossier.setDocDate(saisie == null ? null : saisie.atStartOfDay());
        return ev.enregistrer(dossier);
    }

    private ComFolder affecter(ComFolder dossier, CfgTier commercial) {
        dossier.setCfgCommercial(commercial);
        return ev.enregistrer(dossier);
    }

    private StlSettlementLine payer(ComFolder dossier, String montant, String statut, LocalDate date) {
        BigDecimal v = m(montant);
        ComDocument e = jeu.echeance(dossier, jeu.categorie(Genre.AUTOFINANCEMENT, true), v, date);
        return jeu.regler(e, v, true, statut, null, date);
    }

    // ------------------------------------------------------------------ contrat Java (§3.1, décision manager 2)

    @Test
    void contrat_mouvementFlux_recordPublic_composantesDuSpecPlusLigneId() {
        Class<?> classe = FluxParReflexion.classe("MouvementFlux");

        assertThat(classe.isRecord()).as("record").isTrue();
        assertThat(Modifier.isPublic(classe.getModifiers())).as("public").isTrue();
        Map<String, Class<?>> composantes = new LinkedHashMap<>();
        for (RecordComponent c : classe.getRecordComponents()) {
            composantes.put(c.getName(), c.getType());
        }
        assertThat(composantes).containsOnlyKeys(MouvementsParReflexion.COMPOSANTES)
                .containsEntry("mesure", MesureFlux.class).containsEntry("trancheId", UUID.class)
                .containsEntry("date", LocalDate.class).containsEntry("valeur", BigDecimal.class)
                .containsEntry("dossierId", UUID.class).containsEntry("commercialId", UUID.class)
                .containsEntry("categorieArticleId", UUID.class).containsEntry("sousArticle", boolean.class)
                .containsEntry("ligneId", UUID.class);
    }

    @Test
    void contrat_mouvementsFlux_recordPublic_lignesEtAnomalies() {
        Class<?> classe = FluxParReflexion.classe("MouvementsFlux");

        assertThat(classe.isRecord()).isTrue();
        assertThat(Arrays.stream(classe.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("lignes", "anomalies");
        assertThat(classe.getRecordComponents()[1].getType()).isEqualTo(AnomaliesFlux.class);
    }

    // §3.2 : trois mesures ajoutées, chacune dans sa famille
    @Test
    void contrat_mesuresAjoutees_reservationsDossier_vivantsReglement_baseConcretisationPrevu() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(mesure(RESERVATIONS).famille()).isEqualTo(MesureFlux.Famille.DOSSIER);
        soft.assertThat(mesure(ENCAISSEMENTS_VIVANTS).famille()).isEqualTo(MesureFlux.Famille.REGLEMENT);
        soft.assertThat(mesure(BASE_CONCRETISATION_QUANTITE).famille()).isEqualTo(MesureFlux.Famille.PREVU);
        soft.assertAll();
    }

    // ------------------------------------------------------------------ I-U1 un seul calcul

    /** Un cas de chaque sorte sur deux tranches, 2022–2024 ; frais de dossier et d'annulation paramétrés. */
    private void jeuComplet() {
        ComSettlementCategory annulation = jeu.categorie(Genre.FRAIS, true);
        ComSettlementCategory fraisDossier = jeu.categorie(Genre.FRAIS, true);
        fixture.parametre(FixtureFluxVente.FRAIS_ANNULATION, annulation.getId());
        fixture.parametre(FixtureFluxVente.FRAIS_DOSSIER, fraisDossier.getId());
        CfgTier commercial = ev.client("COM-" + jeu.prefixe());
        int rang = 0;
        for (CfgTranche t : List.of(t1, t2)) {
            rang++;
            saisir(jeu.dossier(t, st.reservation()), j(2023, rang, 10));
            ComFolder conc = jeu.dater(saisir(jeu.dossier(t, st.concretisation()), j(2023, 2, rang)),
                    j(2023, 2, 15), null, null, null);
            affecter(jeu.prix(conc, m("1200000"), m("1000000")), commercial);
            ComFolder min = jeu.dater(jeu.dossier(t, st.minute()), j(2023, 3, 3), j(2023, 4, 4 + rang), null, null);
            jeu.dater(jeu.dossier(t, st.remiseClefs()), j(2022, 5, 5), j(2022, 6, 6), j(2024, 7, 7), null);
            ComFolder ann = jeu.dater(saisir(jeu.dossier(t, st.annulation()), j(2023, 1, 20)), j(2023, 2, 20),
                    j(2023, 3, 20), null, j(2024, 2, 2));
            ComFolder des = jeu.dater(saisir(jeu.dossier(t, st.desistement()), j(2022, 12, 1)), j(2023, 5, 5), null,
                    null, j(2024, 3, 3));
            ComFolder sous = jeu.dater(jeu.dossierSousArticle(t, st.concretisation()), j(2024, 1, 5), null, null, null);
            ComFolder sansStatut = saisir(jeu.dossier(t, null), j(2023, 6, 6));

            jeu.encaisser(conc, "1000", j(2023, 4, 10));
            jeu.encaisser(min, "2500.50", j(2024, 1, 10));
            jeu.encaisser(ann, "300", j(2023, 6, 1));
            jeu.encaisser(des, "70", j(2023, 6, 2));
            jeu.encaisser(sous, "11000", j(2024, 2, 2));
            jeu.encaisser(sansStatut, "444", j(2023, 7, 7));
            payer(conc, "999", CANCELED, j(2023, 4, 11));
            jeu.regler(jeu.echeance(conc, jeu.categorie(Genre.AUTOFINANCEMENT, true), m("200"), j(2023, 8, 8)),
                    m("200"), false, IN_WORK, null, j(2023, 8, 8));
            jeu.regler(jeu.echeance(conc, jeu.categorie(Genre.AUTOFINANCEMENT, true), m("20000"), null),
                    m("20000"), true, IN_WORK, null, null);
            jeu.regler(jeu.echeance(conc, jeu.categorie(Genre.AVANCE, true), m("5000"), j(2023, 2, 15)), m("5000"),
                    true, POINTED, j(2023, 2, 16), j(2023, 2, 15));
            jeu.regler(jeu.echeance(conc, fraisDossier, m("100"), j(2023, 2, 15)), m("100"), true, IN_WORK, null,
                    j(2023, 2, 15));
            jeu.echeance(des, annulation, m("1500"), j(2024, 3, 10));
            jeu.echeance(des, jeu.categorie(Genre.RESILIATION, false), m("8000"), j(2024, 3, 3));
        }
    }

    private static String cle(String mesure, UUID tranche, int annee, int mois) {
        return mesure + "|" + tranche + "|" + annee + "|" + mois;
    }

    @Test
    void iU1_sommeDesMouvementsParTrancheAnneeMois_egalCalculer_celluleACellule_toutesMesuresNonPrevues() {
        jeuComplet();
        Set<MesureFlux> mesures = Arrays.stream(MesureFlux.values())
                .filter(x -> x.famille() != MesureFlux.Famille.PREVU).collect(Collectors.toSet());
        fixture.relire();

        Resultat r = reflexion.mouvements(demande(List.of(t1, t2), mesures));
        FluxMensuel flux = service.calculer(demande(List.of(t1, t2), mesures));

        Map<String, BigDecimal> sommes = new HashMap<>();
        for (Mouvement l : r.lignes()) {
            sommes.merge(cle(l.mesure(), l.trancheId(), l.date().getYear(), l.date().getMonthValue()), l.valeur(),
                    BigDecimal::add);
        }
        assertThat(r.lignes()).extracting(Mouvement::mesure).contains(RESERVATIONS, ENCAISSEMENTS_VIVANTS,
                ENCAISSEMENTS, CONCRETISATIONS_NETTES, FRAIS_DESISTEMENT_DUS);
        SoftAssertions soft = new SoftAssertions();
        for (MesureFlux mesure : mesures) {
            for (CleFlux c : flux.cles(mesure)) {
                soft.assertThat(sommes.getOrDefault(cle(mesure.name(), c.trancheId(), c.annee(), c.mois()),
                        BigDecimal.ZERO)).as("%s %s", mesure, c)
                        .isEqualByComparingTo(flux.valeur(mesure, c.trancheId(), c.annee(), c.mois()));
            }
        }
        for (Mouvement l : r.lignes()) {
            soft.assertThat(sommes.get(cle(l.mesure(), l.trancheId(), l.date().getYear(), l.date().getMonthValue())))
                    .as("%s %s %s", l.mesure(), l.trancheId(), l.date())
                    .isEqualByComparingTo(flux.valeur(mesure(l.mesure()), l.trancheId(), l.date().getYear(),
                            l.date().getMonthValue()));
        }
        soft.assertAll();
    }

    @Test
    void iU1_memesAnomalies_ligneSansDateHorsDesLignes_dossierSansStatutHorsDesLignes() {
        jeuComplet();
        Set<MesureFlux> mesures = Set.of(MesureFlux.ENCAISSEMENTS, MesureFlux.CONCRETISATIONS, mesure(RESERVATIONS));
        fixture.relire();

        Resultat r = reflexion.mouvements(demande(List.of(t1, t2), mesures));
        FluxMensuel flux = service.calculer(demande(List.of(t1, t2), mesures));

        assertThat(r.anomalies()).isEqualTo(flux.anomalies());
        assertThat(r.anomalies().dossiersSansStatut()).isEqualTo(2L);
        assertThat(r.anomalies().reglementsSansDate().montant()).isEqualByComparingTo("40000");
        assertThat(r.lignes()).isNotEmpty().allSatisfy(l -> assertThat(l.date()).isNotNull())
                .noneMatch(l -> l.valeur().compareTo(m("20000")) == 0)
                .noneMatch(l -> l.valeur().compareTo(m("444")) == 0);
    }

    // ------------------------------------------------------------------ I-U2 vivants

    @Test
    void iU2_encaissementsMoinsVivants_egalReglementsDesDossiersAnnulesEtDesistes() {
        ComFolder vivant = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 1, 5), null, null, null);
        ComFolder annule = jeu.dater(jeu.dossier(t1, st.annulation()), j(2024, 1, 6), null, null, j(2024, 6, 1));
        ComFolder desiste = jeu.dater(jeu.dossier(t1, st.desistement()), j(2024, 1, 7), null, null, j(2024, 6, 2));
        jeu.encaisser(vivant, "1000", AVRIL);
        jeu.encaisser(annule, "300", AVRIL);
        jeu.encaisser(annule, "200", j(2024, 5, 10));
        jeu.encaisser(desiste, "70", AVRIL);
        fixture.relire();

        FluxMensuel flux = service.calculer(demande(List.of(t1),
                Set.of(MesureFlux.ENCAISSEMENTS, mesure(ENCAISSEMENTS_VIVANTS))));

        assertThat(flux.total(MesureFlux.ENCAISSEMENTS)).isEqualByComparingTo("1570");
        assertThat(flux.total(mesure(ENCAISSEMENTS_VIVANTS))).isEqualByComparingTo("1000");
        assertThat(flux.total(MesureFlux.ENCAISSEMENTS).subtract(flux.total(mesure(ENCAISSEMENTS_VIVANTS))))
                .isEqualByComparingTo("570");
    }

    @Test
    void iU2_reglementCanceled_dansAucuneDesDeuxMesures() {
        ComFolder vivant = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 1, 5), null, null, null);
        StlSettlementLine compte = payer(vivant, "1000", IN_WORK, AVRIL);
        StlSettlementLine annule = payer(vivant, "5000", CANCELED, AVRIL);

        Resultat r = surT1(ENCAISSEMENTS, ENCAISSEMENTS_VIVANTS);

        assertThat(r.lignes()).extracting(Mouvement::ligneId).isNotEmpty().doesNotContain(annule.getId())
                .containsOnly(compte.getId());
        assertThat(r.de(ENCAISSEMENTS)).hasSize(1);
        assertThat(r.de(ENCAISSEMENTS_VIVANTS)).hasSize(1);
    }

    // ------------------------------------------------------------------ I-U3 réservations

    @Test
    void iU3_reservations_reservationConcretisationAnnulationDesistement_aLeurDocDate_sansStatutEnAnomalie() {
        ComFolder res = saisir(jeu.dossier(t1, st.reservation()), j(2024, 1, 10));
        ComFolder conc = jeu.dater(saisir(jeu.dossier(t1, st.concretisation()), j(2024, 2, 11)), j(2024, 3, 1), null,
                null, null);
        ComFolder ann = jeu.dater(saisir(jeu.dossier(t1, st.annulation()), j(2024, 3, 12)), null, null, null,
                j(2024, 9, 9));
        ComFolder des = jeu.dater(saisir(jeu.dossier(t1, st.desistement()), j(2024, 4, 13)), null, null, null,
                j(2024, 9, 10));
        saisir(jeu.dossier(t1, null), j(2024, 5, 14));

        Resultat r = surT1(RESERVATIONS);

        assertThat(r.de(RESERVATIONS)).extracting(Mouvement::dossierId, Mouvement::date)
                .containsExactlyInAnyOrder(tuple(res.getId(), j(2024, 1, 10)), tuple(conc.getId(), j(2024, 2, 11)),
                        tuple(ann.getId(), j(2024, 3, 12)), tuple(des.getId(), j(2024, 4, 13)));
        assertThat(r.de(RESERVATIONS)).isNotEmpty().allSatisfy(l -> assertThat(l.valeur()).isEqualByComparingTo("1"));
        assertThat(r.anomalies().dossiersSansStatut()).isEqualTo(1L);
    }

    @Test
    void iU3_reservations_dossierSansDocDate_absent_calculerAuMoisDeSaisie() {
        jeu.dossier(t1, st.reservation());
        jeu.dater(saisir(jeu.dossier(t1, st.annulation()), j(2024, 3, 12)), null, null, null, j(2024, 9, 9));
        fixture.relire();

        Resultat r = surT1(RESERVATIONS);
        FluxMensuel flux = service.calculer(demande(List.of(t1), Set.of(mesure(RESERVATIONS))));

        assertThat(r.de(RESERVATIONS)).hasSize(1);
        assertThat(flux.valeur(mesure(RESERVATIONS), t1.getId(), 2024, 3)).isEqualByComparingTo("1");
        assertThat(flux.total(mesure(RESERVATIONS))).as("pas la date d'annulation").isEqualByComparingTo("1");
    }

    // ------------------------------------------------------------------ I-U4 attributs de la ligne

    @Test
    void iU4_dossierSurSousArticle_porteSousArticleSonCommercialSaCategorie_ligneIdNul() {
        CfgTier commercial = ev.client("COM-" + jeu.prefixe());
        ComFolder sous = jeu.dater(affecter(jeu.dossierSousArticle(t1, st.concretisation()), commercial),
                j(2024, 1, 5), null, null, null);
        UUID categorie = sous.getStkItem().getStkItemCategory().getId();

        Resultat r = surT1(CONCRETISATIONS_NETTES);

        assertThat(r.lignes()).singleElement().satisfies(l -> {
            assertThat(l.mesure()).isEqualTo(CONCRETISATIONS_NETTES);
            assertThat(l.trancheId()).isEqualTo(t1.getId());
            assertThat(l.date()).isEqualTo(j(2024, 1, 5));
            assertThat(l.valeur()).isEqualByComparingTo("1");
            assertThat(l.dossierId()).isEqualTo(sous.getId());
            assertThat(l.commercialId()).isEqualTo(commercial.getId());
            assertThat(l.categorieArticleId()).isEqualTo(categorie);
            assertThat(l.sousArticle()).isTrue();
            assertThat(l.ligneId()).isNull();
        });
    }

    @Test
    void iU4_reglement_porteLeDossierDeSonEcheance_saLigne_dateDuMouvementAuJour() {
        CfgTier commercial = ev.client("COM-" + jeu.prefixe());
        ComFolder d = affecter(jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 1, 5), null, null, null),
                commercial);
        UUID categorie = d.getStkItem().getStkItemCategory().getId();
        StlSettlementLine parEcheance = jeu.encaisser(d, "1234.56", AVRIL);
        StlSettlementLine parReglement = jeu.regler(jeu.echeance(d, jeu.categorie(Genre.AUTOFINANCEMENT, true),
                m("200"), AVRIL), m("200"), true, POINTED, j(2024, 5, 3), AVRIL);

        Resultat r = surT1(ENCAISSEMENTS);

        assertThat(r.lignes()).hasSize(2).allSatisfy(l -> {
            assertThat(l.dossierId()).isEqualTo(d.getId());
            assertThat(l.commercialId()).isEqualTo(commercial.getId());
            assertThat(l.categorieArticleId()).isEqualTo(categorie);
            assertThat(l.sousArticle()).isFalse();
        });
        assertThat(r.lignes()).extracting(Mouvement::ligneId, Mouvement::date)
                .containsExactlyInAnyOrder(tuple(parEcheance.getId(), AVRIL), tuple(parReglement.getId(), j(2024, 5, 3)));
    }

    @Test
    void iU4_fraisDeDesistementDus_ligneIdEgalEcheance_dossierDeLEcheance() {
        ComSettlementCategory annulation = jeu.categorie(Genre.FRAIS, true);
        fixture.parametre(FixtureFluxVente.FRAIS_ANNULATION, annulation.getId());
        ComFolder d = jeu.dater(jeu.dossier(t1, st.desistement()), j(2023, 6, 1), null, null, j(2024, 2, 1));
        ComDocument e = jeu.echeance(d, annulation, m("1500"), j(2024, 2, 15));

        Resultat r = surT1(FRAIS_DESISTEMENT_DUS);

        assertThat(r.lignes()).singleElement().satisfies(l -> {
            assertThat(l.ligneId()).isEqualTo(e.getId());
            assertThat(l.dossierId()).isEqualTo(d.getId());
            assertThat(l.date()).isEqualTo(j(2024, 2, 15));
            assertThat(l.valeur()).isEqualByComparingTo("1500");
        });
    }

    // ------------------------------------------------------------------ I-U5 prévu refusé

    @Test
    void iU5_mouvementsAvecMesurePrevue_illegalArgument() {
        DemandeFlux demande = demande(List.of(t1), Set.of(mesure(BASE_CONCRETISATION_QUANTITE),
                MesureFlux.ENCAISSEMENTS));

        assertThatThrownBy(() -> reflexion.mouvements(demande)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void iU5_calculer_baseConcretisationQuantite_zero_prevuNonSaisi() {
        jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 1, 5), null, null, null);
        fixture.relire();

        FluxMensuel flux = service.calculer(demande(List.of(t1),
                Set.of(mesure(BASE_CONCRETISATION_QUANTITE), MesureFlux.CONCRETISATIONS)));

        assertThat(flux.total(mesure(BASE_CONCRETISATION_QUANTITE))).isEqualByComparingTo("0");
        assertThat(flux.total(MesureFlux.CONCRETISATIONS)).isEqualByComparingTo("1");
        assertThat(flux.anomalies().prevuNonSaisi()).isTrue();
    }

    // ------------------------------------------------------------------ M5 précision

    @Test
    void m5_mouvementsGardentLesCentimes_190virgule75Plus0virgule25_egal191() {
        ComFolder d = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 1, 5), null, null, null);
        jeu.encaisser(d, "190.75", j(2024, 1, 10));
        jeu.encaisser(d, "0.25", j(2024, 1, 11));

        Resultat r = surT1(ENCAISSEMENTS);
        FluxMensuel flux = service.calculer(demande(List.of(t1), Set.of(MesureFlux.ENCAISSEMENTS)));

        assertThat(r.lignes()).extracting(Mouvement::valeur).usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(m("190.75"), m("0.25"));
        assertThat(flux.valeur(MesureFlux.ENCAISSEMENTS, t1.getId(), 2024, 1)).isEqualByComparingTo("191.00");
    }

    // ------------------------------------------------------------------ M7 aucune règle de règlement recopiée

    private static final Path SOURCES = Path.of("src", "main", "java", "com", "tsix", "immocrm", "services", "rapport",
            "vente");

    @Test
    void m7_aucuneClasseR2a_neLitIsCreditSettlementDateIsSaleExpense() throws IOException {
        List<Path> fichiers = List.of(SOURCES.resolve("RythmeVentesRapport.java"),
                SOURCES.resolve("ConcretisationsParCommercialRapport.java"),
                SOURCES.resolve("ConcretisationEncaissementRapport.java"),
                SOURCES.resolve("flux").resolve("MouvementFlux.java"),
                SOURCES.resolve("flux").resolve("MouvementsFlux.java"));

        SoftAssertions soft = new SoftAssertions();
        for (Path f : fichiers) {
            soft.assertThat(Files.exists(f)).as("%s existe", f).isTrue();
            if (Files.exists(f)) {
                soft.assertThat(Files.readString(f, StandardCharsets.UTF_8)).as(f.toString())
                        .doesNotContain("IsCredit", "SettlementDate", "IsSaleExpense");
            }
        }
        soft.assertAll();
    }
}
