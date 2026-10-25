package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureSuiviPrevisions;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import com.tsix.immocrm.services.rapport.vente.flux.AnomaliesFlux;
import com.tsix.immocrm.services.rapport.vente.flux.CleFlux;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.SourcePrevu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rapport n° 18 {@code ComTdbPrevisionState} (FEATURE-tdb-vente-previsions.md §5, §9.2 R-P8, §9.3 R-P11, §9.4 R-V4, R-V5),
 * par le hub R0. Tranche T, 2024 : concrétisations 02/2024 (une vivante, une annulée en 06/2024), une vente le 05/03/2024 avec
 * avance 100 (réglée), un acte le 10/06/2024 (prix 1 500 000). Prévu : {@link SourcePrevu} remplacé (défaut « non saisi »).
 * Avant le code, le code de rapport sans classe répond 404 : tous ces tests sont rouges.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsVenteRapportTest {

    private static final String CODE = "ComTdbPrevisionState";
    private static final String AN = "2024";
    private static final int ANNEE_TEST = 2024;
    private static final String REEL = "reel";
    private static final String POURCENTAGE_REALISE = "pourcentageRealise";
    private static final List<String> SECTIONS = List.of("concretisations-quantite", "concretisations-montant",
            "ca-quantite", "ca-montant");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureSuiviPrevisions sp;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FluxVenteMensuel flux;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;
    @Autowired private Clock clock;
    @MockitoBean private SourcePrevu sourcePrevu;

    private AppelsRapports appels;
    private UUID rapportId;
    private Jeu jeu;
    private CfgTranche t;
    private RequestPostProcessor libre;

    @BeforeEach
    void setUp() {
        when(sourcePrevu.prevu(any())).thenReturn(FluxMensuel.vide(new AnomaliesFlux(0, null, List.of(), true)));
        appels = new AppelsRapports(mockMvc);
        rapports.tables();
        rapportId = rapports.rapportVente(CODE, "SUIVI DES PREVISIONS DE VENTE", "", 1600);
        jeu = fixture.jeu("N18");
        t = jeu.tranche("T", jeu.projet("P", true));
        libre = compteAvec(rapports.role("Libre " + jeu.prefixe(), false));
    }

    private RequestPostProcessor compteAvec(AdmRole role) {
        RequestPostProcessor qui = connecte(rapports.compte("n18." + UUID.randomUUID().toString().substring(0, 6),
                role));
        rapports.relire();
        return qui;
    }

    private void jeuDe2024() {
        Statuts st = jeu.statuts();
        jeu.dater(jeu.dossier(t, st.concretisation()), LocalDate.of(2024, 2, 10), null, null, null);
        jeu.dater(jeu.dossier(t, st.annulation()), LocalDate.of(2024, 2, 12), null, null, LocalDate.of(2024, 6, 1));
        ComFolder mars = sp.vente(jeu, t, LocalDate.of(2024, 3, 5), null);
        sp.regler(jeu, sp.echeance(jeu, mars, Genre.AVANCE, "100", LocalDate.of(2024, 3, 5)), "100", IN_WORK,
                LocalDate.of(2024, 3, 6));
        ComFolder acte = jeu.dater(jeu.dossier(t, st.minute()), LocalDate.of(2024, 1, 3), LocalDate.of(2024, 6, 10),
                null, null);
        jeu.prix(acte, new BigDecimal("1500000"), new BigDecimal("1250000"));
        rapports.relire();
    }

    private List<Map<String, Object>> sections(String... parametres) throws Exception {
        return appels.sections(demande(CODE, libre, List.of(t), parametres));
    }

    private FluxMensuel fluxDe2024(MesureFlux... mesures) {
        return flux.calculer(new DemandeFlux(List.of(t.getId()), ANNEE_TEST, ANNEE_TEST, Set.of(mesures)));
    }

    // ------------------------------------------------------------------ R-P11 structure

    @Test
    void rP11_quatreSectionsDeDouzeLignes_douzeGraphiques() throws Exception {
        jeuDe2024();

        List<Map<String, Object>> s = sections(ANNEE, AN);

        assertThat(s).extracting(x -> x.get("id")).containsExactlyElementsOf(SECTIONS);
        SoftAssertions soft = new SoftAssertions();
        int graphiques = 0;
        for (Map<String, Object> x : s) {
            soft.assertThat(lignes(x)).as("%s", x.get("id")).hasSize(12);
            List<?> g = (List<?>) x.get("graphiques");
            soft.assertThat(g).as("%s : 3 graphiques", x.get("id")).hasSize(3);
            graphiques += g == null ? 0 : g.size();
        }
        soft.assertThat(graphiques).isEqualTo(12);
        soft.assertAll();
    }

    @Test
    void rP11_graphiquesParSection_prevuReel_cumuls_pourcentages() throws Exception {
        jeuDe2024();

        Map<String, Object> s = section(sections(ANNEE, AN), "ca-montant");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> g = (List<Map<String, Object>>) s.get("graphiques");
        assertThat(g).extracting(x -> x.get("type"), x -> x.get("colonnesValeurs")).containsExactly(
                tuple("COLONNES", List.of("prevu", REEL)),
                tuple("LIGNES", List.of("cumulPrevu", "cumulReel")),
                tuple("LIGNES", List.of(POURCENTAGE_REALISE, "pourcentageCumule",
                        "rentabilite")));
    }

    @Test
    void rP11_caMontantReel_egalCaTtcDuFluxParMois() throws Exception {
        jeuDe2024();
        FluxMensuel attendu = fluxDe2024(MesureFlux.CA_TTC);

        Map<String, Object> s = section(sections(ANNEE, AN), "ca-montant");

        assertThat(attendu.mois(MesureFlux.CA_TTC, ANNEE_TEST, 6)).as("précondition").isEqualByComparingTo("1500000");
        SoftAssertions soft = new SoftAssertions();
        for (int m = 1; m <= 12; m++) {
            soft.assertThat(nombre(lignes(s).get(m - 1).get(REEL))).as("mois %d", m)
                    .isEqualByComparingTo(attendu.mois(MesureFlux.CA_TTC, ANNEE_TEST, m));
        }
        soft.assertAll();
    }

    @Test
    void rP11_concretisationsQuantiteReel_egalConcretisationsAvecAnnules_libelleDontAnnulees() throws Exception {
        jeuDe2024();
        FluxMensuel attendu = fluxDe2024(MesureFlux.CONCRETISATIONS);

        Map<String, Object> s = section(sections(ANNEE, AN), "concretisations-quantite");

        assertThat(nombre(lignes(s).get(1).get(REEL))).as("février : vivante + annulée").isEqualByComparingTo("2")
                .isEqualByComparingTo(attendu.mois(MesureFlux.CONCRETISATIONS, ANNEE_TEST, 2));
        assertThat(colonnes(s)).filteredOn(c -> REEL.equals(c.get("nom"))).singleElement()
                .satisfies(c -> assertThat(String.valueOf(c.get("libelle"))).contains("dont annulées"));
    }

    @Test
    void rP11_concretisationsMontantReel_egalConcretisationsMontantDuFlux() throws Exception {
        jeuDe2024();
        FluxMensuel attendu = fluxDe2024(MesureFlux.CONCRETISATIONS_MONTANT);

        Map<String, Object> s = section(sections(ANNEE, AN), "concretisations-montant");

        SoftAssertions soft = new SoftAssertions();
        for (int m = 1; m <= 12; m++) {
            soft.assertThat(nombre(lignes(s).get(m - 1).get(REEL))).as("mois %d", m)
                    .isEqualByComparingTo(attendu.mois(MesureFlux.CONCRETISATIONS_MONTANT, ANNEE_TEST, m));
        }
        soft.assertAll();
    }

    @Test
    void rP11_anneeAbsente_anneeDeLHorloge() throws Exception {
        String corps = appels.corps(demande(CODE, libre, List.of(t)));

        List<String> annee = JsonPath.read(corps, "$.parametres.annee");
        assertThat(annee).containsExactly(String.valueOf(Year.now(clock).getValue()));
    }

    // Faute (a) du modèle corrigée : % réalisé du montant de concrétisation ÷ SON prévu, jamais le prévu d'encaissement
    @Test
    void rP11_pourcentageConcretisationsMontant_diviseParSonPrevu_pasParLePrevuDEncaissement() throws Exception {
        jeuDe2024();
        FluxMensuel.Cumul cumul = new FluxMensuel.Cumul();
        cumul.ajouter(MesureFlux.PREVU_CONCRETISATION_MONTANT, new CleFlux(t.getId(), ANNEE_TEST, 3), new BigDecimal("200"));
        cumul.ajouter(MesureFlux.PREVU_ENCAISSEMENT, new CleFlux(t.getId(), ANNEE_TEST, 3), new BigDecimal("50"));
        when(sourcePrevu.prevu(any())).thenReturn(cumul.flux(AnomaliesFlux.AUCUNE));
        BigDecimal reel = fluxDe2024(MesureFlux.CONCRETISATIONS_MONTANT)
                .mois(MesureFlux.CONCRETISATIONS_MONTANT, ANNEE_TEST, 3);

        Map<String, Object> mars = lignes(section(sections(ANNEE, AN), "concretisations-montant")).get(2);

        assertThat(reel).as("précondition").isPositive();
        assertThat(nombre(mars.get("prevu"))).isEqualByComparingTo("200");
        assertThat(nombre(mars.get(POURCENTAGE_REALISE))).isEqualByComparingTo(
                reel.multiply(BigDecimal.valueOf(100)).divide(new BigDecimal("200"), 2, RoundingMode.HALF_UP));
    }

    // ------------------------------------------------------------------ R-P8 (moitié n° 18)

    @Test
    void rP8_dossierConcretiseEnNovembre2023AnnuleEnMars2024_n18NovembreA1() throws Exception {
        jeu.dater(jeu.dossier(t, jeu.statuts().annulation()), LocalDate.of(2023, 11, 15), null, null,
                LocalDate.of(2024, 3, 10));
        rapports.relire();

        Map<String, Object> s = section(sections(ANNEE, "2023"), "concretisations-quantite");

        assertThat(nombre(lignes(s).get(10).get(REEL))).isEqualByComparingTo("1");
    }

    // ------------------------------------------------------------------ R-V4 droits par le socle

    @Test
    void rV4_roleRestreintSansLigne_403SurDescripteurResultatExport() throws Exception {
        RequestPostProcessor restreint = compteAvec(rapports.role("Restreint " + jeu.prefixe(), true));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(descripteur(VENTE, CODE)).with(restreint))).as("descripteur").isEqualTo(403);
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE)).param(TRANCHE_ID, t.getId().toString())
                .with(restreint))).as("resultat").isEqualTo(403);
        soft.assertThat(appels.exporter(get(export(VENTE, CODE)).param("format", "xlsx")
                .param(TRANCHE_ID, t.getId().toString()).with(restreint)).getResponse().getStatus()).as("export")
                .isEqualTo(403);
        soft.assertAll();
    }

    @Test
    void rV4_commercialChefLie_200SurDescripteurEtResultat() throws Exception {
        AdmRole chef = rapports.role("Commercial Chef " + jeu.prefixe(), true);
        rapports.lier(rapportId, chef);
        RequestPostProcessor lie = compteAvec(chef);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(descripteur(VENTE, CODE)).with(lie))).as("descripteur").isEqualTo(200);
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE)).param(TRANCHE_ID, t.getId().toString()).with(lie)))
                .as("resultat").isEqualTo(200);
        soft.assertAll();
    }

    @Test
    void rV4_administrateurSansLigneDeRole_withReportRestriction0_200() throws Exception {
        RequestPostProcessor admin = compteAvec(rapports.role("Administrateur", false));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(descripteur(VENTE, CODE)).with(admin))).as("descripteur").isEqualTo(200);
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE)).param(TRANCHE_ID, t.getId().toString()).with(admin)))
                .as("resultat").isEqualTo(200);
        soft.assertThat(appels.exporter(get(export(VENTE, CODE)).param("format", "xlsx")
                .param(TRANCHE_ID, t.getId().toString()).with(admin)).getResponse().getStatus()).as("export")
                .isEqualTo(200);
        soft.assertAll();
    }

    @Test
    void rV4_rapportVisibleDansLaListe_groupeAutresRapports_pourUnRoleLibre() throws Exception {
        String corps = appels.corps(get(AppelsRapports.liste(VENTE)).with(libre));

        List<Map<String, Object>> groupes = JsonPath.read(corps, "$[*]");
        List<Object> contenant = groupes.stream()
                .filter(g -> ((List<?>) g.get("rapports")).stream()
                        .anyMatch(r -> CODE.equals(((Map<?, ?>) r).get("code"))))
                .map(g -> g.get("nom")).toList();
        assertThat(contenant).containsExactly(FixtureRapports.AUTRES_RAPPORTS);
    }

    // ------------------------------------------------------------------ R-V5 clé d'API

    @Test
    void rV5_cleDApiAvecToutesLesPortees_403HorsPortee_surLesRoutesDuN18() throws Exception {
        AdmUser compte = dossiers.compte("N18-" + jeu.prefixe(), "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n18-" + jeu.prefixe());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        for (String url : List.of(descripteur(VENTE, CODE), resultat(VENTE, CODE), export(VENTE, CODE))) {
            mockMvc.perform(get(url).param(TRANCHE_ID, t.getId().toString())
                            .header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        }
    }
}
