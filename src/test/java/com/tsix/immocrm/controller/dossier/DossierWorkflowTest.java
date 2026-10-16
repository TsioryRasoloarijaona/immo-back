package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.str.StrField;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrStatusIntervener;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.fixtures.FixtureWorkflow;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.dossier.workflow.IntervenantCourant;
import com.tsix.immocrm.services.dossier.workflow.StatutsVisibles;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Moteur de workflow paramétré des dossiers de vente (FEATURE-vente-workflow.md, section Vérification). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierWorkflowTest {

    private static final String STATUT = "/api/dossiers/{id}/statut";
    private static final String ETAPES = "/api/dossiers/{id}/statut/etapes-suivantes";
    private static final String SUGGESTION = "/api/dossiers/{id}/statut/transition-suggeree";
    private static final String ADMIN = "Administrateur";
    private static final String CHEF = "Commercial Chef";
    private static final String COMMERCIAL = "Commercial";
    private static final String VERS_CONCRETISATION = "{\"statut\":\"CONCRETISATION\"}";
    private static final String VERS_MEILLEURTAUX = "{\"statut\":\"MEILLEURTAUX\"}";
    private static final String VERS_MEILLEURTAUX_SANS_SEUIL = "{\"statut\":\"MEILLEURTAUX\",\"ignorerSeuil\":true}";
    private static final String DETAIL = "$.detail";
    private static final String DROIT_SEUIL = "Yes.Com.Front.Sales.Monitoring.toolVerifyThreshold";
    private static final BigDecimal DOUZE_POURCENT = new BigDecimal("108000");
    private static final BigDecimal SEUIL_MEILLEURTAUX = new BigDecimal("0.125");
    /** Prix après remise du dossier ARS 2025/00321 (yesRemsProd), passé par MEILLEURTAUX. */
    private static final BigDecimal PRIX_REEL = new BigDecimal("900000");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureStatutsDossier statuts;
    @Autowired private FixtureWorkflow workflow;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StrStatusRepository statusRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComDocumentPaymentMethodRepository modaliteRepository;
    @Autowired private AdmUserRoleRepository userRoleRepository;
    @Autowired private StatutsVisibles statutsVisibles;
    @Autowired private IntervenantCourant intervenantCourant;
    @Autowired private EntityManager em;
    @Autowired private Clock clock;

    private CfgTranche tranche;
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("TST-WF", true);
        admin = fixture.compte("TST-ADM", ADMIN);
    }

    // ------------------------------------------------------------ transitions et intervenants

    @Test
    void transitionHorsParametrage_409_listeLesCiblesParametrees() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        workflow.transition(statut(StatutDossier.RESERVATION), statut(StatutDossier.CONCRETISATION), false);

        changer(dossier, "{\"statut\":\"MINUTE\"}", admin, ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.transitionsAutorisees", hasSize(1)))
                .andExpect(jsonPath("$.transitionsAutorisees[0]").value("CONCRETISATION"));
    }

    @Test
    void roleNonIntervenant_403_etAucuneEtapeProposee() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        AdmUser chef = fixture.compte("TST-CHEF", CHEF);
        AdmUser commercial = fixture.compte("TST-COM", COMMERCIAL);
        StrWorkFlow t = workflow.transition(statut(StatutDossier.RESERVATION), statut(StatutDossier.CONCRETISATION), false);
        workflow.intervenantRole(t, roleDe(chef));

        changer(dossier, VERS_CONCRETISATION, commercial, COMMERCIAL)
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ETAPES, dossier.getId()).with(FixtureDossierVente.connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get(ETAPES, dossier.getId()).with(FixtureDossierVente.connecte(chef, CHEF)))
                .andExpect(jsonPath("$[0].statut").value("CONCRETISATION"));
    }

    @Test
    void intervenantDuRole_200_etStrWorkflowIdDansLHistorique() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        AdmUser chef = fixture.compte("TST-CHEF", CHEF);
        StrWorkFlow t = workflow.transition(statut(StatutDossier.RESERVATION), statut(StatutDossier.CONCRETISATION), false);
        workflow.intervenantRole(t, roleDe(chef));

        changer(dossier, VERS_CONCRETISATION, chef, CHEF)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CONCRETISATION"));

        List<StrHistory> lignes = em.createQuery(
                        "SELECT h FROM StrHistory h WHERE h.objectId = :id AND h.strWorkflowId IS NOT NULL", StrHistory.class)
                .setParameter("id", dossier.getId()).getResultList();
        assertThat(lignes).singleElement().extracting(StrHistory::getStrWorkflowId).isEqualTo(t.getId());
    }

    @Test
    void administrateur_passeSansEtreIntervenant() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        AdmUser chef = fixture.compte("TST-CHEF", CHEF);
        StrWorkFlow t = workflow.transition(statut(StatutDossier.RESERVATION), statut(StatutDossier.CONCRETISATION), false);
        workflow.intervenantRole(t, roleDe(chef));

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN).andExpect(status().isOk());
    }

    // ------------------------------------------------------------ champs et pièces obligatoires

    @Test
    void champDuDossierManquant_400_avecLeLibelleOmran_puisAccepteAvecLaDate() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        StrStatus concretisation = statut(StatutDossier.CONCRETISATION);
        workflow.transition(statut(StatutDossier.RESERVATION), concretisation, false);
        StrField date = workflow.champ(StatutDossier.CONFIG_ENTITY_ID, "AdvanceDate", "Date concrétisation");
        workflow.champObligatoireDuStatut(concretisation, date, null);

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL, containsString("Pour exécuter la transition")))
                .andExpect(jsonPath(DETAIL, containsString("Le champ 'Date concrétisation' est obligatoire !")));
        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getStrStatus().getCode())
                .isEqualTo("RESERVATION");

        changer(dossier, "{\"statut\":\"CONCRETISATION\",\"date\":\"2026-03-02T00:00:00\"}", admin, ADMIN)
                .andExpect(status().isOk());
        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getAdvanceDate())
                .isEqualTo(LocalDateTime.of(2026, 3, 2, 0, 0));
    }

    @Test
    void champDeLaTransitionManquant_400() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        StrWorkFlow t = workflow.transition(statut(StatutDossier.RESERVATION), statut(StatutDossier.CONCRETISATION), false);
        workflow.champObligatoireDeTransition(t,
                workflow.champ(StatutDossier.CONFIG_ENTITY_ID, "CfgLawyerId", "Notaire / avocat"));

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL, containsString("Le champ 'Notaire / avocat' est obligatoire !")));
    }

    @Test
    void champDuClientManquant_400_puisAccepteUneFoisRenseigne() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        StrStatus concretisation = statut(StatutDossier.CONCRETISATION);
        workflow.transition(statut(StatutDossier.RESERVATION), concretisation, false);
        StrField cin = workflow.champ(FixtureWorkflow.ENTITE_TIERS, "IDNumber", "Pièce d'identité N°");
        workflow.champObligatoireDuStatut(concretisation, cin, workflow.lienClient());
        workflow.personnePhysique(dossier.getCfgCustomer().getId());
        em.clear();

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL, containsString("Client : ")))
                .andExpect(jsonPath(DETAIL, containsString("Le champ 'Pièce d'identité N°' est obligatoire !")));

        CfgTier client = tierRepository.findById(dossier.getCfgCustomer().getId()).orElseThrow();
        client.setIDNumber("TST123456");
        tierRepository.saveAndFlush(client);
        changer(dossier, VERS_CONCRETISATION, admin, ADMIN).andExpect(status().isOk());
    }

    @Test
    void champDuClientPersonneMorale_nonControle() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        StrStatus concretisation = statut(StatutDossier.CONCRETISATION);
        workflow.transition(statut(StatutDossier.RESERVATION), concretisation, false);
        StrField cin = workflow.champ(FixtureWorkflow.ENTITE_TIERS, "IDNumber", "Pièce d'identité N°");
        workflow.champObligatoireDuStatut(concretisation, cin, workflow.lienClient());

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN).andExpect(status().isOk());
    }

    @Test
    void pieceManquante_400_puisAccepteAvecLaPiece() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        StrStatus concretisation = statut(StatutDossier.CONCRETISATION);
        workflow.transition(statut(StatutDossier.RESERVATION), concretisation, false);
        AdmAttachedFileType fiche = workflow.typePiece("TST FICHE DE RESERVATION");
        workflow.pieceDuStatut(concretisation, fiche, true);

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL,
                        containsString("Le document de type : TST FICHE DE RESERVATION est obligatoire !")));

        workflow.pieceJointe(dossier.getId(), fiche);
        changer(dossier, VERS_CONCRETISATION, admin, ADMIN).andExpect(status().isOk());
    }

    // ------------------------------------------------------------ seuil de versement et schéma

    @Test
    void seuilMeilleurtaux_douzePourcentVerses_409() throws Exception {
        ComFolder dossier = dossierAvecVersement(StatutDossier.CONCRETISATION, DOUZE_POURCENT);
        StrStatus meilleurtaux = statut(StatutDossier.MEILLEURTAUX);
        workflow.transition(statut(StatutDossier.CONCRETISATION), meilleurtaux, false);
        workflow.seuilGlobal(meilleurtaux, 1, SEUIL_MEILLEURTAUX);

        changer(dossier, VERS_MEILLEURTAUX, admin, ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL, containsString("au moins égal à 12,50 %")))
                .andExpect(jsonPath(DETAIL, containsString("112 500,00")));
    }

    @Test
    void seuilMeilleurtaux_treizePourcentVerses_200() throws Exception {
        ComFolder dossier = dossierAvecVersement(StatutDossier.CONCRETISATION, new BigDecimal("117000"));
        StrStatus meilleurtaux = statut(StatutDossier.MEILLEURTAUX);
        workflow.transition(statut(StatutDossier.CONCRETISATION), meilleurtaux, false);
        workflow.seuilGlobal(meilleurtaux, 1, SEUIL_MEILLEURTAUX);

        changer(dossier, VERS_MEILLEURTAUX, admin, ADMIN).andExpect(status().isOk());
    }

    @Test
    void ignorerSeuil_administrateur_200_etTrace() throws Exception {
        ComFolder dossier = dossierAvecVersement(StatutDossier.CONCRETISATION, DOUZE_POURCENT);
        StrStatus meilleurtaux = statut(StatutDossier.MEILLEURTAUX);
        workflow.transition(statut(StatutDossier.CONCRETISATION), meilleurtaux, false);
        workflow.seuilGlobal(meilleurtaux, 1, SEUIL_MEILLEURTAUX);

        changer(dossier, VERS_MEILLEURTAUX_SANS_SEUIL, admin, ADMIN)
                .andExpect(status().isOk());

        List<String> descriptions = em.createQuery(
                        "SELECT h.description FROM StrHistory h WHERE h.objectId = :id", String.class)
                .setParameter("id", dossier.getId()).getResultList();
        assertThat(descriptions).anyMatch(d -> d != null && d.startsWith("Seuil de versement non vérifié"));
    }

    @Test
    void ignorerSeuil_horsAdministrateur_403() throws Exception {
        ComFolder dossier = dossierAvecVersement(StatutDossier.CONCRETISATION, DOUZE_POURCENT);
        AdmUser chef = fixture.compte("TST-CHEF", CHEF);
        StrStatus meilleurtaux = statut(StatutDossier.MEILLEURTAUX);
        StrWorkFlow t = workflow.transition(statut(StatutDossier.CONCRETISATION), meilleurtaux, false);
        workflow.intervenantRole(t, roleDe(chef));
        workflow.seuilGlobal(meilleurtaux, 1, SEUIL_MEILLEURTAUX);

        changer(dossier, VERS_MEILLEURTAUX_SANS_SEUIL, chef, CHEF)
                .andExpect(status().isForbidden());
    }

    @Test
    void seuil_commercialSansLeDroit_nEstPasControle_200() throws Exception {
        ComFolder dossier = dossierAvecVersement(StatutDossier.CONCRETISATION, DOUZE_POURCENT);
        AdmUser chef = fixture.compte("TST-CHEF", CHEF);
        AdmUser commercial = fixture.compte("TST-COM", COMMERCIAL);
        workflow.droit(roleDe(chef), DROIT_SEUIL);
        StrStatus meilleurtaux = statut(StatutDossier.MEILLEURTAUX);
        StrWorkFlow t = workflow.transition(statut(StatutDossier.CONCRETISATION), meilleurtaux, false);
        workflow.intervenantRole(t, roleDe(commercial));
        workflow.seuilGlobal(meilleurtaux, 1, SEUIL_MEILLEURTAUX);

        changer(dossier, VERS_MEILLEURTAUX, commercial, COMMERCIAL).andExpect(status().isOk());
    }

    @Test
    void seuil_chefAvecLeDroit_douzePourcent_409() throws Exception {
        ComFolder dossier = dossierAvecVersement(StatutDossier.CONCRETISATION, DOUZE_POURCENT);
        AdmUser chef = fixture.compte("TST-CHEF", CHEF);
        workflow.droit(roleDe(chef), DROIT_SEUIL);
        StrStatus meilleurtaux = statut(StatutDossier.MEILLEURTAUX);
        StrWorkFlow t = workflow.transition(statut(StatutDossier.CONCRETISATION), meilleurtaux, false);
        workflow.intervenantRole(t, roleDe(chef));
        workflow.seuilGlobal(meilleurtaux, 1, SEUIL_MEILLEURTAUX);

        changer(dossier, VERS_MEILLEURTAUX, chef, CHEF)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL, containsString("au moins égal à 12,50 %")));
    }

    @Test
    void retourAReservation_sansDate_garderDocDate() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.CONCRETISATION);
        LocalDateTime reservation = LocalDateTime.of(2025, 1, 5, 0, 0);
        dossier.setDocDate(reservation);
        folderRepository.save(dossier);
        statut(StatutDossier.RESERVATION);

        mockMvc.perform(get(ETAPES, dossier.getId()).with(FixtureDossierVente.connecte(admin, ADMIN)))
                .andExpect(jsonPath("$[?(@.statut=='RESERVATION')].dateActuelle").value("2025-01-05T00:00:00"));
        changer(dossier, "{\"statut\":\"RESERVATION\"}", admin, ADMIN).andExpect(status().isOk());

        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getDocDate()).isEqualTo(reservation);
    }

    @Test
    void schema_egalAuPrixAuCentimePres_200() throws Exception {
        // Échéancier réel de ARS 2025/00321 : 650 000 + 250 000,004999 pour 900 000.
        ComFolder dossier = dossierAvecSchema(new BigDecimal("250000.004999"));
        workflow.transition(statut(StatutDossier.RESERVATION), statutAvecSchema(), false);

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN).andExpect(status().isOk());
    }

    @Test
    void schema_differentDuPrix_409() throws Exception {
        ComFolder dossier = dossierAvecSchema(new BigDecimal("240000"));
        workflow.transition(statut(StatutDossier.RESERVATION), statutAvecSchema(), false);

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL, containsString("vous devez ajouter dans le schéma de financement "
                        + "un montant de 10 000,00 !")));
    }

    // ------------------------------------------------------------ modalité de paiement (FEATURE-vente-echeancier.md §2)

    @ParameterizedTest(name = "BoolField001 = {0} -> {1} ligne(s)")
    @CsvSource({"1, 2", "0, 0"})
    void statutEffectif_instantaneDeLaModalite_seulementSiDrapeau001(int effectif, int lignesAttendues) throws Exception {
        ComFolder dossier = dossierAvecSchema(new BigDecimal("250000"));
        StrStatus concretisation = statut(StatutDossier.CONCRETISATION);
        workflow.transition(statut(StatutDossier.RESERVATION), concretisation, false);
        em.flush();
        em.createNativeQuery("UPDATE StrStatus SET BoolField001 = :effectif WHERE PKey = :id")
                .setParameter("effectif", effectif).setParameter("id", concretisation.getId()).executeUpdate();

        changer(dossier, VERS_CONCRETISATION, admin, ADMIN).andExpect(status().isOk());

        assertThat(modaliteRepository.findByComFolderIdOrderByDocDateDesc(dossier.getId())).hasSize(lignesAttendues);
    }

    // ------------------------------------------------------------ date associée, validité, gestionnaire

    @Test
    void dateAssociee_parametree_ecritLaColonneDuStatut() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.CONCRETISATION);
        StrStatus meilleurtaux = statut(StatutDossier.MEILLEURTAUX);
        meilleurtaux.setStrFieldDateId(
                workflow.champ(StatutDossier.CONFIG_ENTITY_ID, "LoanCompanySendDate", "Date d'envoi MEILLEURTAUX").getId());
        statusRepository.save(meilleurtaux);
        workflow.transition(statut(StatutDossier.CONCRETISATION), meilleurtaux, false);

        mockMvc.perform(get(ETAPES, dossier.getId()).with(FixtureDossierVente.connecte(admin, ADMIN)))
                .andExpect(jsonPath("$[0].champDate").value("loanCompanySendDate"));
        changer(dossier, "{\"statut\":\"MEILLEURTAUX\",\"date\":\"2026-04-10T00:00:00\"}", admin, ADMIN)
                .andExpect(status().isOk());

        ComFolder relu = folderRepository.findById(dossier.getId()).orElseThrow();
        assertThat(relu.getLoanCompanySendDate()).isEqualTo(LocalDateTime.of(2026, 4, 10, 0, 0));
        assertThat(relu.getLoanCompanyDecisionDate()).isNull();
    }

    @Test
    void dateAssociee_repli_formaliseEcritOwnershipCertificateDate() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.REMISE_DES_CLEFS);
        statut(StatutDossier.FORMALISE);

        changer(dossier, "{\"statut\":\"FORMALISE\",\"date\":\"2026-05-01T00:00:00\"}", admin, ADMIN)
                .andExpect(status().isOk());

        ComFolder relu = folderRepository.findById(dossier.getId()).orElseThrow();
        assertThat(relu.getOwnershipCertificateDate()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        assertThat(relu.getAgreementRegistryDate()).isNull();
    }

    @Test
    void validiteReservation_septJours_etAlerteExpire() throws Exception {
        fixture.categoriesAction();
        ComFolder dossier = dossierAuStatut(StatutDossier.CONCRETISATION);
        statut(StatutDossier.RESERVATION);

        changer(dossier, "{\"statut\":\"RESERVATION\"}", admin, ADMIN).andExpect(status().isOk());

        LocalDateTime attendue = LocalDate.now(clock).plusDays(FixtureStatutsDossier.DELAI_RESERVATION).atStartOfDay();
        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getStatusValidityDate()).isEqualTo(attendue);
        List<LocalDateTime> alertes = em.createQuery("SELECT m.alertValidityDate FROM ComActionMessage m "
                        + "WHERE m.comFolder.id = :id AND m.isSystem = true AND m.alertMessage LIKE '%est expiré'",
                        LocalDateTime.class)
                .setParameter("id", dossier.getId()).getResultList();
        assertThat(alertes).containsExactly(attendue);
    }

    @Test
    void statutAvecAffectationDeGestionnaire_poseLePersonnelDeLAppelant() throws Exception {
        ComFolder dossier = dossierAuStatut(StatutDossier.ENVOI_AU_NOTAIRE);
        StrStatus compromis = statut(StatutDossier.COMPROMIS);
        compromis.setAffectationGestionnaire(true);
        statusRepository.save(compromis);

        changer(dossier, "{\"statut\":\"COMPROMIS\"}", admin, ADMIN).andExpect(status().isOk());

        ComFolder relu = folderRepository.findById(dossier.getId()).orElseThrow();
        assertThat(relu.getCfgSaleManager()).isNotNull()
                .extracting(CfgTier::getId).isEqualTo(admin.getCfgEmployee().getId());
    }

    // ------------------------------------------------------------ passage automatique

    @Test
    void suggestion_creditDebloqueVersMinute_quandLeSeuilEstAtteint() throws Exception {
        ComFolder dossier = dossierAvecVersement(StatutDossier.CREDIT_DEBLOQUE, PRIX_REEL);
        StrStatus minute = statut(StatutDossier.MINUTE);
        workflow.transition(statut(StatutDossier.CREDIT_DEBLOQUE), minute, true);
        workflow.transition(statut(StatutDossier.CREDIT_DEBLOQUE), statut(StatutDossier.DESISTEMENT), false);
        workflow.seuilGlobal(minute, 1, BigDecimal.ONE);

        mockMvc.perform(get(SUGGESTION, dossier.getId()).with(FixtureDossierVente.connecte(admin, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("MINUTE"))
                .andExpect(jsonPath("$.date").value("2026-02-15T00:00:00"));
    }

    @Test
    void suggestion_seuilNonAtteint_aucune() throws Exception {
        ComFolder dossier = dossierAvecVersement(StatutDossier.CREDIT_DEBLOQUE, new BigDecimal("500000"));
        StrStatus minute = statut(StatutDossier.MINUTE);
        workflow.transition(statut(StatutDossier.CREDIT_DEBLOQUE), minute, true);
        workflow.seuilGlobal(minute, 1, BigDecimal.ONE);

        mockMvc.perform(get(SUGGESTION, dossier.getId()).with(FixtureDossierVente.connecte(admin, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").doesNotExist());
    }

    @Test
    void suggestion_dossierInconnu_404() throws Exception {
        mockMvc.perform(get(SUGGESTION, UUID.randomUUID()).with(FixtureDossierVente.connecte(admin, ADMIN)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ statuts visibles (pour la liste)

    @Test
    void statutsVisibles_parRole_etSansRestrictionPourLAdministrateur() {
        AdmUser chef = fixture.compte("TST-CHEF", CHEF);
        AdmUser commercial = fixture.compte("TST-COM", COMMERCIAL);
        StrStatus reservation = statut(StatutDossier.RESERVATION);
        visiblePar(reservation, roleDe(chef));

        Optional<Set<UUID>> pourChef = visiblesPour(chef, CHEF);
        Optional<Set<UUID>> pourCommercial = visiblesPour(commercial, COMMERCIAL);
        Optional<Set<UUID>> pourAdmin = visiblesPour(admin, ADMIN);

        assertThat(pourChef).hasValue(Set.of(reservation.getId()));
        assertThat(pourCommercial).hasValue(Set.of());
        assertThat(pourAdmin).isEmpty();
    }

    // ------------------------------------------------------------ outils

    private ResultActions changer(ComFolder dossier, String corps, AdmUser qui, String role) throws Exception {
        return mockMvc.perform(patch(STATUT, dossier.getId()).with(FixtureDossierVente.connecte(qui, role))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private StrStatus statut(StatutDossier code) {
        return statuts.statut(code);
    }

    private StrStatus statutAvecSchema() {
        StrStatus concretisation = statut(StatutDossier.CONCRETISATION);
        concretisation.setVerificationSchema(true);
        return statusRepository.save(concretisation);
    }

    private ComFolder dossierAuStatut(StatutDossier code) {
        ComFolder dossier = fixture.dossier("TST-WF", tranche, null);
        dossier.setStrStatus(statut(code));
        dossier.setPriceAfterDiscount(PRIX_REEL);
        dossier.setPrice(PRIX_REEL);
        return folderRepository.save(dossier);
    }

    /** Dossier au prix réel, une échéance AVANCE de tout le prix, réglée de {@code verse} le 15/02/2026. */
    private ComFolder dossierAvecVersement(StatutDossier code, BigDecimal verse) {
        ComFolder dossier = dossierAuStatut(code);
        ComSettlementCategory avance = fixture.categorieEcheance("TST-AVANCE", true, false, false, null);
        ComDocument echeance = fixture.echeance(dossier, avance, PRIX_REEL, LocalDateTime.of(2026, 1, 1, 0, 0));
        regler(echeance, verse, LocalDateTime.of(2026, 2, 15, 0, 0));
        return dossier;
    }

    private ComFolder dossierAvecSchema(BigDecimal autoFinancement) {
        ComFolder dossier = dossierAuStatut(StatutDossier.RESERVATION);
        ComSettlementCategory avance = fixture.categorieEcheance("TST-AVANCE", true, false, false, null);
        ComSettlementCategory auto = fixture.categorieEcheance("TST-AUTOFIN", false, true, false, null);
        fixture.echeance(dossier, avance, new BigDecimal("650000"), LocalDateTime.of(2026, 1, 1, 0, 0));
        fixture.echeance(dossier, auto, autoFinancement, LocalDateTime.of(2026, 6, 1, 0, 0));
        return dossier;
    }

    private void regler(ComDocument echeance, BigDecimal montant, LocalDateTime date) {
        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setAmount(montant);
        reglement.setIsCredit(true);
        reglement.setDocDate(date);
        em.persist(reglement);
        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(montant);
        em.persist(ligne);
    }

    private UUID roleDe(AdmUser compte) {
        return userRoleRepository.findByAdmUserId(compte.getId()).get(0).getAdmRole().getId();
    }

    private void visiblePar(StrStatus statut, UUID roleId) {
        StrIntervener i = new StrIntervener();
        i.setId(UUID.randomUUID());
        i.setAdmRoleId(roleId);
        i.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        em.persist(i);
        StrStatusIntervener si = new StrStatusIntervener();
        si.setId(UUID.randomUUID());
        si.setStrIntervenerId(i.getId());
        si.setStrStatusId(statut.getId());
        em.persist(si);
    }

    private Optional<Set<UUID>> visiblesPour(AdmUser compte, String role) {
        AuthenticatedUser principal = new AuthenticatedUser(compte.getId(), compte.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        try {
            return statutsVisibles.pour(intervenantCourant.courant());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
