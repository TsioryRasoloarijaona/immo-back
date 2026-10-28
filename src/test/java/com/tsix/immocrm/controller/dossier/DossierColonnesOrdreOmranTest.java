package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Colonnes et défauts de la grille OMRAN « Dossiers de vente » (FEATURE-dossiers-colonnes-omran.md §2, §5). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierColonnesOrdreOmranTest {

    private static final String CATALOGUE = "/api/referentiels/colonnes/dossiers";
    private static final String LISTE = "/api/dossiers";
    private static final String PREMIER = "$.content[0].";
    private static final String SEARCH = "search";
    private static final String COLONNES = "colonnes";

    /** §2.1 : les 15 colonnes nouvelles. */
    private static final List<String> NOUVELLES = List.of(
            "dateRemiseContratLegalisation", "dateLegalisationContrat", "dateRemiseCertificatPropriete", "numeroTitre",
            "decisionMeilleurTaux", "datePromesseVente", "dateFicheIntervention", "dateOctroiCredit",
            "dateVisiteTechnique", "dateExpedition", "echeanceRelance1", "echeanceRelance2", "dateEnvoiMeilleurTaux",
            "delaiNotaireCredit", "delaiCreditCles");

    /** §2.4 : défaut serveur d'avant le lot, inchangé (décision manager du 28/10). */
    private static final List<String> DEFAUTS_SERVEUR = List.of(
            "id", "code", "dateDocument", "statut", "clientId", "clientNom", "commercialId", "commercialNom", "prix",
            "totalAPayer", "dateConcretisation", "prospectionId", "prospectionCode");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureDossierVente fixture;
    @Autowired
    private EntityManager em;

    private CfgTranche tranche;
    private String prefixe;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("TR-COM-" + UUID.randomUUID().toString().substring(0, 6), true);
        prefixe = "DCOM" + UUID.randomUUID().toString().substring(0, 6);
    }

    // ------------------------------------------------------------------ catalogue

    /** §2.1 : les 15 colonnes nouvelles, leur libellé OMRAN et leur type. */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "dateRemiseContratLegalisation | Date remise contrat pour légalisation | DATE",
            "dateLegalisationContrat | Date légalisation contrat | DATE",
            "dateRemiseCertificatPropriete | Date remise du certificat de propriété au client | DATE",
            "numeroTitre | N° titre | TEXTE",
            "decisionMeilleurTaux | Décision MEILLEURTAUX | TEXTE",
            "datePromesseVente | Date Promesse de Vente | DATE",
            "dateFicheIntervention | Date Fiche Intervention | DATE",
            "dateOctroiCredit | Date Attestation de Consignation / Octroi Crédit | DATE",
            "dateVisiteTechnique | Date Visite Technique / Réception | DATE",
            "dateExpedition | Date Expédition | DATE",
            "echeanceRelance1 | Échéance de la relance 1 | DATE",
            "echeanceRelance2 | Échéance de la relance 2 | DATE",
            "dateEnvoiMeilleurTaux | Date Envoi MEILLEURTAUX | DATE",
            "delaiNotaireCredit | Délais Envoi au Notaire-Crédit Accordé | NOMBRE",
            "delaiCreditCles | Délais Crédit Accordé - Remise des Clés | NOMBRE"
    })
    void r2_1_nouvelleColonne_auCatalogueAvecSonLibelleOmran(String nom, String libelle, String type) throws Exception {
        mockMvc.perform(get(CATALOGUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == '" + nom + "')].libelle").value(libelle))
                .andExpect(jsonPath("$[?(@.nom == '" + nom + "')].type").value(type));
    }

    /** §2.3 : libellés OMRAN, nom wire inchangé. */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "prix | Prix de vente",
            "dateDocument | Date Réservation",
            "origineProspection | Source de contact",
            "supportSource | Support source de contact",
            "dateLivraisonReelle | Date Remise de Clés",
            "dateMinute | Date minute / Signature contrat",
            "dateCompromis | Date Compromis / Approbation Crédit",
            "trancheLibelle | Tranche",
            "trancheCode | Code tranche",
            "projetLibelle | Projet",
            "projetCode | Code projet",
            "articleLibelle | Description article",
            "autresClients | Autre clients",
            "superficie | Sup.(m²)",
            "superficieGlobale | Sup.Globale (m²)",
            "delaiConcretisation | Délais Prospection-Concrétisation",
            "delaiNotaire | Délais Concrétisation-Envoi Notaire",
            "delaiLivraison | Délais Envoi au Notaire-Remise des Clés"
    })
    void r2_3_libelleRenomme_commeOmran(String nom, String libelle) throws Exception {
        mockMvc.perform(get(CATALOGUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == '" + nom + "')].libelle").value(libelle));
    }

    /** §2.4 / §5 : les 15 colonnes nouvelles sont hors défaut serveur. */
    @Test
    void r2_4_nouvellesColonnes_parDefautFaux() throws Exception {
        String corps = catalogue();

        List<Boolean> parDefaut = NOUVELLES.stream()
                .<Boolean>flatMap(n -> JsonPath.<List<Boolean>>read(corps, "$[?(@.nom == '" + n + "')].parDefaut")
                        .stream())
                .toList();

        assertThat(parDefaut).hasSize(NOUVELLES.size()).containsOnly(false);
    }

    /** §2.4 / §5 : les parDefaut existants ne changent pas (totalAPayer et prospectionCode compris). */
    @Test
    void r2_4_parDefautExistants_inchanges() throws Exception {
        List<String> defauts = JsonPath.read(catalogue(), "$[?(@.parDefaut == true)].nom");

        assertThat(defauts).containsExactlyInAnyOrderElementsOf(DEFAUTS_SERVEUR);
    }

    /** §2.4 : sans ?colonnes=, la liste garde son défaut — aucune colonne nouvelle, Total à payer toujours servi. */
    @Test
    void r2_4_listeSansSelection_defautInchange() throws Exception {
        ComFolder f = dossier();
        f.setTotalToPay(new BigDecimal("1200000"));
        em.flush();
        dates(f, "ReceptionDate", LocalDateTime.of(2025, 4, 3, 0, 0));
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "code").value(f.getCode()))
                .andExpect(jsonPath(PREMIER + "totalAPayer").value(1200000.0))
                .andExpect(jsonPath(PREMIER + "dateVisiteTechnique").doesNotExist());
    }

    // ------------------------------------------------------------------ liste : valeurs

    /** §5 : les cinq colonnes nommées par le spec lisent leur colonne ComFolder. */
    @Test
    void r2_1_liste_dateVisiteTechnique_octroiCredit_relance1_envoiMeilleurTaux_numeroTitre() throws Exception {
        ComFolder f = dossier();
        em.flush();
        dates(f, "ReceptionDate", LocalDateTime.of(2025, 4, 3, 0, 0));
        dates(f, "CreditPossessionDate", LocalDateTime.of(2025, 4, 11, 0, 0));
        dates(f, "AgreementDate", LocalDateTime.of(2025, 3, 2, 0, 0));
        dates(f, "DeliveryDateReal", LocalDateTime.of(2025, 5, 1, 0, 0));
        dates(f, "Revival1ValidityDate", LocalDateTime.of(2025, 6, 15, 0, 0));
        dates(f, "loanCompanySendDate", LocalDateTime.of(2025, 2, 20, 0, 0));
        em.createNativeQuery("UPDATE ComFolder SET FinalTitleNumber = :v WHERE PKey = :id")
                .setParameter("v", "TF-4521/C")
                .setParameter("id", f.getId())
                .executeUpdate();
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES,
                        "code,dateVisiteTechnique,dateOctroiCredit,echeanceRelance1,dateEnvoiMeilleurTaux,numeroTitre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "dateVisiteTechnique", startsWith("2025-04-03")))
                .andExpect(jsonPath(PREMIER + "dateOctroiCredit", startsWith("2025-04-11")))
                .andExpect(jsonPath(PREMIER + "echeanceRelance1", startsWith("2025-06-15")))
                .andExpect(jsonPath(PREMIER + "dateEnvoiMeilleurTaux", startsWith("2025-02-20")))
                .andExpect(jsonPath(PREMIER + "numeroTitre").value("TF-4521/C"));
    }

    /** §2.1 : les autres dates nouvelles lisent la colonne ComFolder du tableau. */
    @Test
    void r2_1_liste_autresDatesNouvelles_lisentLeurColonne() throws Exception {
        ComFolder f = dossier();
        em.flush();
        dates(f, "AgreementSavingDateForLegalization", LocalDateTime.of(2025, 1, 5, 0, 0));
        dates(f, "AgreementSavingDateLegalization", LocalDateTime.of(2025, 1, 12, 0, 0));
        dates(f, "FinalTitleDate", LocalDateTime.of(2025, 7, 1, 0, 0));
        dates(f, "ReservationDate", LocalDateTime.of(2024, 12, 9, 0, 0));
        dates(f, "InterventionDate", LocalDateTime.of(2025, 2, 3, 0, 0));
        dates(f, "OwnershipCertificateDate", LocalDateTime.of(2025, 8, 4, 0, 0));
        dates(f, "Revival2ValidityDate", LocalDateTime.of(2025, 9, 22, 0, 0));
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES,
                        "code,dateRemiseContratLegalisation,dateLegalisationContrat,dateRemiseCertificatPropriete,"
                                + "datePromesseVente,dateFicheIntervention,dateExpedition,echeanceRelance2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "dateRemiseContratLegalisation", startsWith("2025-01-05")))
                .andExpect(jsonPath(PREMIER + "dateLegalisationContrat", startsWith("2025-01-12")))
                .andExpect(jsonPath(PREMIER + "dateRemiseCertificatPropriete", startsWith("2025-07-01")))
                .andExpect(jsonPath(PREMIER + "datePromesseVente", startsWith("2024-12-09")))
                .andExpect(jsonPath(PREMIER + "dateFicheIntervention", startsWith("2025-02-03")))
                .andExpect(jsonPath(PREMIER + "dateExpedition", startsWith("2025-08-04")))
                .andExpect(jsonPath(PREMIER + "echeanceRelance2", startsWith("2025-09-22")));
    }

    /** §2.1 / §5 : decisionMeilleurTaux = libellé de ComLoanCompanyDecision, pas l'UUID ni le code. */
    @Test
    void r2_1_decisionMeilleurTaux_renvoieLeLibelleDeLaDecision() throws Exception {
        ComFolder f = dossier();
        UUID decision = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO ComLoanCompanyDecision (PKey, sorting, Code, Description) "
                        + "VALUES (:id, 1, 'ACC-DCOM', 'ACCEPTEE DCOM')")
                .setParameter("id", decision)
                .executeUpdate();
        em.flush();
        em.createNativeQuery("UPDATE ComFolder SET ComLoanCompanyDecisionId = :d WHERE PKey = :id")
                .setParameter("d", decision)
                .setParameter("id", f.getId())
                .executeUpdate();
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "code,decisionMeilleurTaux"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "decisionMeilleurTaux").value("ACCEPTEE DCOM"));
    }

    // ------------------------------------------------------------------ délais

    /** §2.2 : 40 j de l'envoi notaire à l'octroi, 20 j de l'octroi à la remise des clés. */
    @Test
    void r2_2_delaiNotaireCredit_etDelaiCreditCles_enJours() throws Exception {
        ComFolder f = dossier();
        em.flush();
        dates(f, "AgreementDate", LocalDateTime.of(2025, 3, 2, 0, 0));
        dates(f, "CreditPossessionDate", LocalDateTime.of(2025, 4, 11, 0, 0));
        dates(f, "DeliveryDateReal", LocalDateTime.of(2025, 5, 1, 0, 0));
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "code,delaiNotaireCredit,delaiCreditCles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "delaiNotaireCredit").value(40))
                .andExpect(jsonPath(PREMIER + "delaiCreditCles").value(20));
    }

    /** §2.2 : sans octroi de crédit, les deux délais qui en dépendent sont null (pas 0). */
    @Test
    void r2_2_octroiCreditNul_deuxDelaisNuls() throws Exception {
        ComFolder f = dossier();
        em.flush();
        dates(f, "AgreementDate", LocalDateTime.of(2025, 3, 2, 0, 0));
        dates(f, "DeliveryDateReal", LocalDateTime.of(2025, 5, 1, 0, 0));
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe)
                        .param(COLONNES, "code,delaiNotaireCredit,delaiCreditCles,delaiLivraison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "delaiLivraison").value(60))
                .andExpect(jsonPath(PREMIER + "delaiNotaireCredit").doesNotExist())
                .andExpect(jsonPath(PREMIER + "delaiCreditCles").doesNotExist());
    }

    /** §2.2 : sans envoi au notaire, delaiNotaireCredit est null ; delaiCreditCles reste calculé. */
    @Test
    void r2_2_envoiNotaireNul_delaiNotaireCreditNul_delaiCreditClesCalcule() throws Exception {
        ComFolder f = dossier();
        em.flush();
        dates(f, "CreditPossessionDate", LocalDateTime.of(2025, 4, 11, 0, 0));
        dates(f, "DeliveryDateReal", LocalDateTime.of(2025, 5, 1, 0, 0));
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "code,delaiNotaireCredit,delaiCreditCles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "delaiCreditCles").value(20))
                .andExpect(jsonPath(PREMIER + "delaiNotaireCredit").doesNotExist());
    }

    /** §2.2 : avec une prospection, delaiConcretisation part de sa date (31 j), pas du DocDate du dossier (12 j). */
    @Test
    void r2_2_delaiConcretisation_partDeLaDateDeProspection() throws Exception {
        ComFolder f = dossier();
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-" + prefixe);
        p.setDocDate(LocalDateTime.of(2025, 1, 1, 10, 0));
        em.persist(p);
        f.setComProspection(p);
        f.setDocDate(LocalDateTime.of(2025, 1, 20, 9, 0));
        f.setAdvanceDate(LocalDateTime.of(2025, 2, 1, 9, 0));
        em.flush();
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "code,delaiConcretisation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "delaiConcretisation").value(31));
    }

    /** §2.2 : sans prospection, delaiConcretisation part du DocDate du dossier. */
    @Test
    void r2_2_delaiConcretisation_sansProspection_partDuDocDate() throws Exception {
        ComFolder f = dossier();
        f.setDocDate(LocalDateTime.of(2025, 1, 20, 9, 0));
        f.setAdvanceDate(LocalDateTime.of(2025, 2, 1, 9, 0));
        em.flush();
        em.clear();

        mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(COLONNES, "code,delaiConcretisation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIER + "delaiConcretisation").value(12));
    }

    private ComFolder dossier() {
        return fixture.dossier(prefixe, tranche, StatutDossier.CONCRETISATION);
    }

    private String catalogue() throws Exception {
        return mockMvc.perform(get(CATALOGUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Colonne posée en SQL : le test ne dépend pas du mappage de l'entité, qui fait partie du lot. */
    private void dates(ComFolder f, String colonne, LocalDateTime valeur) {
        em.createNativeQuery("UPDATE ComFolder SET " + colonne + " = :v WHERE PKey = :id")
                .setParameter("v", valeur)
                .setParameter("id", f.getId())
                .executeUpdate();
    }
}
