package com.tsix.immocrm.controller.impaye;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/impayes} ({@code FEATURE-impayes.md} §3). Chaque test isole ses lignes par {@code q} = un jeton
 * propre au test, porté par le code de ses dossiers : la base de test a ses propres échéances.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ImpayeControllerTest {

    private static final String URL = "/api/impayes";
    private static final String LIGNES = "$.lignes";
    private static final String PAR_ID = "$.lignes[?(@.echeanceId == '%s')]";
    private static final String TOTAL = "$.total";
    private static final String Q = "q";
    private static final String PREMIERE_ECHEANCE = "$.lignes[0].echeanceId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;
    @Autowired private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private Clock clock;

    private String jeton;
    private CfgTranche tranche;
    private ComSettlementCategory avance;
    private LocalDateTime hier;

    @BeforeEach
    void setUp() {
        jeton = "IMP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        tranche = fixture.tranche(jeton, true);
        avance = fixture.categorieEcheance("AVANCE-" + jeton, true, false, false, null);
        hier = LocalDate.now(clock).minusDays(1).atStartOfDay();
    }

    private ComFolder dossier(String suffixe, StatutDossier statut, boolean venteEffective) {
        ComFolder dossier = fixture.dossier(jeton + "-" + suffixe, tranche, statut);
        dossier.getStrStatus().setVenteEffective(venteEffective);
        return dossier;
    }

    private ComFolder dossierActif(String suffixe) {
        return dossier(suffixe, StatutDossier.FORMALISE, true);
    }

    private ComDocument echeance(ComFolder dossier, String montant, LocalDateTime date) {
        return fixture.echeance(dossier, avance, new BigDecimal(montant), date);
    }

    private void regler(ComDocument echeance, String montant, String statut, boolean encaissement) {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIR-" + jeton);
        settlementTypeRepository.save(type);

        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setAmount(new BigDecimal(montant));
        reglement.setStatus(statut);
        reglement.setIsCredit(encaissement);
        reglement.setDocDate(hier);
        reglement.setStlSettlementType(type);
        settlementRepository.save(reglement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal(montant));
        settlementLineRepository.save(ligne);
    }

    // ------------------------------------------------------------------ R1

    @Test
    void r1_reservationAnnulationDesistementEtStatutNulExclus() throws Exception {
        ComDocument active = echeance(dossierActif("ACT"), "1000", hier);
        ComDocument reservation = echeance(dossier("RES", StatutDossier.RESERVATION, false), "1000", hier);
        ComDocument annule = echeance(dossier("ANN", StatutDossier.ANNULATION, false), "1000", hier);
        ComDocument desiste = echeance(dossier("DES", StatutDossier.DESISTEMENT, false), "1000", hier);
        ComDocument sansStatut = echeance(fixture.dossier(jeton + "-NUL", tranche, null), "1000", hier);

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PAR_ID, active.getId()).isNotEmpty())
                .andExpect(jsonPath(PAR_ID, reservation.getId()).isEmpty())
                .andExpect(jsonPath(PAR_ID, annule.getId()).isEmpty())
                .andExpect(jsonPath(PAR_ID, desiste.getId()).isEmpty())
                .andExpect(jsonPath(PAR_ID, sansStatut.getId()).isEmpty());
    }

    @Test
    void r1_categorieQuiNEstPasUnEncaissement_exclue() throws Exception {
        ComFolder dossier = dossierActif("DEB");
        ComSettlementCategory debit = fixture.categorieEcheance("REMB-" + jeton, false, false, false, null);
        debit.setIsCredit(false);
        fixture.echeance(dossier, debit, new BigDecimal("500"), hier);

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(0));
    }

    // ------------------------------------------------------------------ R2

    /**
     * Annulé exclu, décaissement soustrait : réglé = 3 000,25 − 500 = 2 500,25, comme le {@code montantRegle}
     * de l'échéancier de la fiche, qui marque la même échéance EN_RETARD.
     */
    @Test
    void r2_resteIdentiqueALEcheancierDeLaFiche() throws Exception {
        ComFolder dossier = dossierActif("R2");
        ComDocument echeance = echeance(dossier, "10000.50", hier);
        regler(echeance, "3000.25", "IN_WORK", true);
        regler(echeance, "1000", "CANCELED", true);
        regler(echeance, "500", "POINTED", false);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier", dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].montantRegle").value(2500.25))
                .andExpect(jsonPath("$.content[0].reste").value(7500.25))
                .andExpect(jsonPath("$.content[0].statutCalcule").value("EN_RETARD"));

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES, hasSize(1)))
                .andExpect(jsonPath(PREMIERE_ECHEANCE).value(echeance.getId().toString()))
                .andExpect(jsonPath("$.lignes[0].montantAPayer").value(10000.50))
                .andExpect(jsonPath("$.lignes[0].montantRegle").value(2500.25))
                .andExpect(jsonPath("$.lignes[0].montantReste").value(7500.25));
    }

    // ------------------------------------------------------------------ R3

    @Test
    void r3_echeanceSoldeeExclue() throws Exception {
        ComFolder dossier = dossierActif("R3");
        ComDocument soldee = echeance(dossier, "2000", hier);
        regler(soldee, "2000", "POINTED", true);
        ComDocument partielle = echeance(dossier, "2000", hier);
        regler(partielle, "1999.99", "POINTED", true);

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES, hasSize(1)))
                .andExpect(jsonPath(PREMIERE_ECHEANCE).value(partielle.getId().toString()))
                .andExpect(jsonPath("$.lignes[0].montantReste").value(0.01));
    }

    // ------------------------------------------------------------------ R4

    @Test
    void r4_echeanceDuJourIncluse_demainExclue() throws Exception {
        ComFolder dossier = dossierActif("R4");
        LocalDate aujourdhui = LocalDate.now(clock);
        ComDocument duJour = echeance(dossier, "100", aujourdhui.atTime(18, 30));
        ComDocument demain = echeance(dossier, "100", aujourdhui.plusDays(1).atStartOfDay());

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PAR_ID, duJour.getId()).isNotEmpty())
                .andExpect(jsonPath(PAR_ID, demain.getId()).isEmpty())
                .andExpect(jsonPath("$.lignes[0].joursRetard").value(0));

        mockMvc.perform(get(URL).param(Q, jeton).param("echu", "TOUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PAR_ID, demain.getId()).isNotEmpty())
                .andExpect(jsonPath(TOTAL).value(2));
    }

    /** {@code RecoveryAge} absent de la base de test : 90 jours. */
    @Test
    void r4_modeRecouvrement_echeancePlus90JoursAuPlusAujourdhui() throws Exception {
        ComFolder dossier = dossierActif("REC");
        LocalDate aujourdhui = LocalDate.now(clock);
        ComDocument recouvrable = echeance(dossier, "100", aujourdhui.minusDays(90).atStartOfDay());
        ComDocument tropRecente = echeance(dossier, "100", aujourdhui.minusDays(89).atStartOfDay());

        mockMvc.perform(get(URL).param(Q, jeton).param("echu", "RECOUVREMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PAR_ID, recouvrable.getId()).isNotEmpty())
                .andExpect(jsonPath(PAR_ID, tropRecente.getId()).isEmpty())
                .andExpect(jsonPath("$.lignes[0].dateRecouvrement")
                        .value(aujourdhui.atStartOfDay().toString() + ":00"))
                .andExpect(jsonPath("$.lignes[0].joursRetard").value(90));
    }

    // ------------------------------------------------------------------ R5

    @Test
    void r5_dossierSansArticle_resteListeSansSociete() throws Exception {
        ComFolder dossier = dossierActif("R5");
        dossier.setStkItem(null);
        folderRepository.save(dossier);
        ComDocument echeance = echeance(dossier, "100", hier);

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES, hasSize(1)))
                .andExpect(jsonPath(PREMIERE_ECHEANCE).value(echeance.getId().toString()))
                .andExpect(jsonPath("$.lignes[0].societe").value(nullValue()))
                .andExpect(jsonPath("$.lignes[0].projet").value(nullValue()));
    }

    @Test
    void r5_societeParLeProjet_pasParLEcheance() throws Exception {
        ComDocument echeance = echeance(dossierActif("SOC"), "100", hier);

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE_ECHEANCE).value(echeance.getId().toString()))
                .andExpect(jsonPath("$.lignes[0].societe").value("Société " + jeton))
                .andExpect(jsonPath("$.lignes[0].projet").value("Projet " + jeton))
                .andExpect(jsonPath("$.lignes[0].tranche").value("Tranche " + jeton));
    }

    // ------------------------------------------------------------------ R6

    @Test
    void r6_commercialHorsPerimetre_neVoitPasLesImpayesDUnProjetQuIlNeVoitPasDansLesDossiers() throws Exception {
        ComDocument dansPerimetre = echeance(dossierActif("A"), "100", hier);
        CfgTranche autreTranche = fixture.tranche(jeton + "B", true);
        ComFolder dossierB = fixture.dossier(jeton + "-B", autreTranche, StatutDossier.FORMALISE);
        dossierB.getStrStatus().setVenteEffective(true);
        ComDocument horsPerimetre = echeance(dossierB, "100", hier);

        AdmUser commercial = fixture.compte("COM-" + jeton, "Commercial");
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(commercial);
        habilitation.setCfgProject(tranche.getCfgProject());
        habilitationRepository.save(habilitation);

        mockMvc.perform(get("/api/dossiers/{id}", dossierB.getId()).with(connecte(commercial, "Commercial")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(URL).param(Q, jeton).with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PAR_ID, dansPerimetre.getId()).isNotEmpty())
                .andExpect(jsonPath(PAR_ID, horsPerimetre.getId()).isEmpty());

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(2));
    }

    @Test
    void r6_societeInactive_masqueAussiPourUnNonRestreint() throws Exception {
        CfgTranche trancheInactive = fixture.tranche(jeton + "I", false);
        ComFolder dossier = fixture.dossier(jeton + "-I", trancheInactive, StatutDossier.FORMALISE);
        dossier.getStrStatus().setVenteEffective(true);
        echeance(dossier, "100", hier);

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(0));
    }

    // ------------------------------------------------------------------ R7

    @Test
    void r7_totauxSurToutLeFiltre_pasSurLaPage() throws Exception {
        ComFolder premier = dossierActif("T1");
        echeance(premier, "100", hier.minusDays(2));
        echeance(premier, "200", hier.minusDays(1));
        echeance(dossierActif("T2"), "400", hier);

        mockMvc.perform(get(URL).param(Q, jeton).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES, hasSize(2)))
                .andExpect(jsonPath(TOTAL).value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totaux.montantAPayer").value(700))
                .andExpect(jsonPath("$.totaux.montantRegle").value(0))
                .andExpect(jsonPath("$.totaux.montantReste").value(700))
                .andExpect(jsonPath("$.totaux.nombreDossiers").value(2));
    }

    // ------------------------------------------------------------------ filtres et tri

    @Test
    void triParDefaut_dateEcheanceDecroissante_etTriDemande() throws Exception {
        ComFolder dossier = dossierActif("TRI");
        ComDocument ancienne = echeance(dossier, "300", hier.minusDays(10));
        ComDocument recente = echeance(dossier, "100", hier);
        ComDocument milieu = echeance(dossier, "200", hier.minusDays(5));

        mockMvc.perform(get(URL).param(Q, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[*].echeanceId", contains(recente.getId().toString(),
                        milieu.getId().toString(), ancienne.getId().toString())));

        mockMvc.perform(get(URL).param(Q, jeton).param("sort", "montantReste,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[*].montantReste", contains(100.0, 200.0, 300.0)));
    }

    @Test
    void filtres_commercialCategorieClientArticleEtPeriode() throws Exception {
        ComFolder vise = dossierActif("VISE");
        CfgTier commercial = fixture.tier("VEND-" + jeton);
        vise.setCfgCommercial(commercial);
        folderRepository.save(vise);
        ComDocument cible = echeance(vise, "100", hier);
        ComSettlementCategory autre = fixture.categorieEcheance("FRAIS-" + jeton, false, false, false, null);
        ComDocument autreCategorie = fixture.echeance(dossierActif("AUTRE"), autre, new BigDecimal("50"),
                hier.minusDays(30));

        mockMvc.perform(get(URL).param(Q, jeton).param("commercialId", commercial.getId().toString()))
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PAR_ID, cible.getId()).isNotEmpty());
        mockMvc.perform(get(URL).param(Q, jeton).param("categorieId", autre.getId().toString()))
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PAR_ID, autreCategorie.getId()).isNotEmpty());
        mockMvc.perform(get(URL).param(Q, jeton).param("client", ("cli-" + jeton + "-vise").toLowerCase()))
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PAR_ID, cible.getId()).isNotEmpty());
        mockMvc.perform(get(URL).param(Q, jeton).param("article", "LOT-" + jeton + "-VISE"))
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PAR_ID, cible.getId()).isNotEmpty());
        mockMvc.perform(get(URL).param(Q, jeton).param("article", "LOT-" + jeton))
                .andExpect(jsonPath(TOTAL).value(0));
        mockMvc.perform(get(URL).param(Q, jeton).param("champDate", "ECHEANCE")
                        .param("du", hier.toLocalDate().toString()).param("au", hier.toLocalDate().toString()))
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PAR_ID, cible.getId()).isNotEmpty());
    }

    @Test
    void filtreProjet_restreintAuxDossiersDuProjet() throws Exception {
        ComDocument cible = echeance(dossierActif("PRJ"), "100", hier);

        mockMvc.perform(get(URL).param("projetId", tranche.getCfgProject().getId().toString())
                        .param("trancheId", tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PREMIERE_ECHEANCE).value(cible.getId().toString()))
                .andExpect(jsonPath("$.lignes[0].article").value("LOT-" + jeton + "-PRJ"));
    }

    // ------------------------------------------------------------------ 400, export, catalogue

    @Test
    void echuInconnu_renvoie400() throws Exception {
        mockMvc.perform(get(URL).param("echu", "HIER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void triSurColonneInconnue_renvoie400() throws Exception {
        mockMvc.perform(get(URL).param("sort", "inconnue,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void export_classeurXlsx() throws Exception {
        echeance(dossierActif("XLS"), "100", hier);

        mockMvc.perform(get(URL + "/export").param(Q, jeton).param("colonnes", "dateEcheance,montantReste"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void export_colonneInconnue_renvoie400() throws Exception {
        mockMvc.perform(get(URL + "/export").param("colonnes", "inconnue"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogueDesColonnes_ordreOmranSansGroupement() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/impayes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("dateEcheance"))
                .andExpect(jsonPath("$[?(@.nom == 'montantReste')].parDefaut").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'montantRegle')].parDefaut").value(false))
                .andExpect(jsonPath("$[?(@.groupable == true)]").isEmpty());
    }
}
