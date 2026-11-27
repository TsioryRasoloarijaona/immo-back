package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Compte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.LectureExport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ApiCommissions.DETAIL;
import static com.tsix.immocrm.fixtures.ApiCommissions.DOCUMENT_DE_LIGNE;
import static com.tsix.immocrm.fixtures.ApiCommissions.DOSSIERS;
import static com.tsix.immocrm.fixtures.ApiCommissions.DROITS;
import static com.tsix.immocrm.fixtures.ApiCommissions.EXPORT_LIGNES;
import static com.tsix.immocrm.fixtures.ApiCommissions.EXPORT_TRANCHES;
import static com.tsix.immocrm.fixtures.ApiCommissions.FACTURES;
import static com.tsix.immocrm.fixtures.ApiCommissions.LIGNES;
import static com.tsix.immocrm.fixtures.ApiCommissions.TRANCHES;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_DESISTEMENT;
import static com.tsix.immocrm.fixtures.ApiCommissions.creees;
import static com.tsix.immocrm.fixtures.ApiCommissions.document;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.ApiCommissions.montant;
import static com.tsix.immocrm.fixtures.ApiCommissions.raisons;
import static com.tsix.immocrm.fixtures.ApiCommissions.suppression;
import static com.tsix.immocrm.fixtures.ApiCommissions.tranche;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixtureCommissions.ECRAN;
import static com.tsix.immocrm.fixtures.FixtureCommissions.IMPRIMER;
import static com.tsix.immocrm.fixtures.FixtureCommissions.NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureCommissions.SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Commission payée à l'agence — FEATURE-agences-a4-commission.md §2 R-K1…R-K9, §3, « Réponses au test-writer ».
 * Règle par défaut : interne 100 / Extern 1 000 (montants fixes) ; règle « agence A » : interne 50 (doit être ignoré,
 * R-K3) / Extern 2 % ; pourcentages stockés en ratio. Agences sans {@code IsExtern} : l'Extern est forcé par le code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionsAgenceTest {

    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final LocalDate ANNULATION = LocalDate.of(2024, 9, 12);
    private static final BigDecimal PRIX = new BigDecimal("775000");
    private static final String LIGNES_DU_TIERS = "SELECT COUNT(*) FROM ComCommissionLine WHERE CfgEmployeeId = :id";
    private static final String DOCUMENTS_DU_TIERS = "SELECT COUNT(*) FROM BuyDocument WHERE CfgTierId = :id";
    private static final String MESSAGE_FERMEE = "Cette fonction n'est pas ouverte aux agences";
    private static final String TIERS = "CfgTierId";
    private static final String EMPLOYE = "CfgEmployeeId";
    private static final String MONTANT_LIGNE = "CommissionAmount";
    private static final String COMMISSIONS_TRANCHE = "/api/tranches/{id}/commissions";
    private static final String DEBUT = "2027-01-01T00:00:00";
    private static final String MILIEU = "2027-04-01T00:00:00";
    private static final String JUIN = "2027-06-01T00:00:00";
    private static final String FIN = "2027-12-31T00:00:00";
    private static final String BENEFICIAIRE_AGENCE = "beneficiaireAgence";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;
    @Autowired private FixtureAgences agences;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private EntityManager em;

    private Contexte c;
    private UUID trancheId;
    private RequestPostProcessor gestionnaire;
    private UUID agenceA;
    private UUID agenceB;
    private UUID vendeurA;
    private UUID vendeurB;
    private UUID commercialSiege;
    private Dossier dA;
    private Dossier dB;
    private Dossier dC;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMA");
        trancheId = c.tranche().getId();
        gestionnaire = fixture.compte(c.jeton(), ECRAN, NOUVEAU, SUPPRIMER, IMPRIMER).qui();
        agenceA = agences.agence("AGA-" + c.jeton());
        agenceB = agences.agence("AGB-" + c.jeton());
        vendeurA = agences.vendeur("VA-" + c.jeton());
        vendeurB = agences.vendeur("VB-" + c.jeton());
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);
        commercialSiege = fixture.personne("SIE-" + c.jeton(), false);

        fixture.regle(trancheId, "SaleCommissionBasedOnAmount", true, "SaleCommissionAmount", new BigDecimal("100"),
                "SaleCommissionExternBasedOnAmount", true, "SaleCommissionAmountExtern", new BigDecimal("1000"));
        fixture.regle(trancheId, TIERS, agenceA,
                "SaleCommissionBasedOnAmount", true, "SaleCommissionAmount", new BigDecimal("50"),
                "SaleCommissionExternBasedOnAmount", false, "SaleCommissionPercentExtern", new BigDecimal("0.02"));

        dA = fixture.dossier(c, "DA", vendeurA, PRIX, AVANCE);
        dB = fixture.dossier(c, "DB", vendeurB, PRIX, AVANCE);
        dC = fixture.dossier(c, "DC", commercialSiege, PRIX, AVANCE);
    }

    // ------------------------------------------------------------------ outils

    private ResultActions generer(String type, Dossier... dossiers) throws Exception {
        return mockMvc.perform(generation(trancheId, type, Stream.of(dossiers).map(Dossier::id).toList())
                .with(gestionnaire));
    }

    private Tuple seuleLigne(Dossier d, String type) {
        List<Tuple> lignes = fixture.lignesDuDossier(d.id(), type);
        assertThat(lignes).as("lignes %s du dossier %s", type, d.code()).hasSize(1);
        return lignes.get(0);
    }

    private void verifierLigne(Tuple l, UUID beneficiaire, String montant) {
        assertAll(
                () -> assertThat(guid(l.get(EMPLOYE))).as("bénéficiaire").isEqualTo(guid(beneficiaire)),
                () -> assertThat(decimal(l.get(MONTANT_LIGNE))).as("montant").isEqualByComparingTo(montant));
    }

    private List<Object> champ(ResultActions r, String cle, UUID id, String nom) throws Exception {
        return lire(r, "$[?(@." + cle + " == '" + id + "')]." + nom);
    }

    /** Libellé « agence » que la liste des dossiers de vente (A1b) rend pour le dossier, vu du siège. */
    private Object agenceSelonA1b(Dossier d) throws Exception {
        // CfgCommercialId posé en SQL natif : sans clear, la liste relit le ComFolder géré (commercial nul → « Siège »).
        fixture.relire();
        ResultActions r = mockMvc.perform(get("/api/dossiers").param("search", d.code()).param("size", "200")
                .with(connecte(patrimoine.administrateur()))).andExpect(status().isOk());
        List<Object> v = lire(r, "$.content[?(@.id == '" + d.id() + "')].agence");
        assertThat(v).as("dossier %s dans la liste A1b", d.code()).hasSize(1);
        return v.get(0);
    }

    // ------------------------------------------------------------------ R-K1 / R-K2 / R-K3 : génération

    /** R-K1, R-K2, R-K3 : vendeur d'agence → ligne au nom de A, règle « agence A », bloc Extern 2 % × 775 000. */
    @Test
    void rk1_rk2_rk3_dossierVendeurAgenceA_ligneAuNomDeA_regleAgence_blocExtern() throws Exception {
        generer(T_CONCRETISATION, dA).andExpect(status().isOk());

        verifierLigne(seuleLigne(dA, CONCRETISATION), agenceA, "15500");
    }

    /** R-K2 (repli sur la règle par défaut), R-K3 (Extern forcé : 1 000, pas 100). */
    @Test
    void rk2_rk3_dossierVendeurAgenceB_ligneAuNomDeB_regleParDefaut_blocExtern() throws Exception {
        generer(T_CONCRETISATION, dB).andExpect(status().isOk());

        verifierLigne(seuleLigne(dB, CONCRETISATION), agenceB, "1000");
    }

    /** R-K1 « sinon le commercial (inchangé) » : interne, règle par défaut, 100. */
    @Test
    void rk1_dossierCommercialSiege_ligneAuNomDuCommercial_inchange() throws Exception {
        generer(T_CONCRETISATION, dC).andExpect(status().isOk());

        verifierLigne(seuleLigne(dC, CONCRETISATION), commercialSiege, "100");
    }

    /** R-K1 : le vendeur d'agence ne reçoit aucune ligne à son nom. */
    @Test
    void rk1_aucuneLigneAuNomDesVendeursDAgence() throws Exception {
        generer(T_CONCRETISATION, dA, dB, dC).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(dA.id())).hasSize(1),
                () -> assertThat(fixture.lignesDuDossier(dB.id())).hasSize(1),
                () -> assertThat(fixture.compter(LIGNES_DU_TIERS, vendeurA)).isZero(),
                () -> assertThat(fixture.compter(LIGNES_DU_TIERS, vendeurB)).isZero());
    }

    /** R-K2 : une règle posée sur le vendeur d'agence n'est jamais retenue pour ses dossiers. */
    @Test
    void rk2_reglePoseeSurLeVendeur_ignoree() throws Exception {
        fixture.regle(trancheId, TIERS, vendeurA,
                "SaleCommissionBasedOnAmount", true, "SaleCommissionAmount", new BigDecimal("7"),
                "SaleCommissionExternBasedOnAmount", true, "SaleCommissionAmountExtern", new BigDecimal("7777"));

        generer(T_CONCRETISATION, dA).andExpect(status().isOk());

        verifierLigne(seuleLigne(dA, CONCRETISATION), agenceA, "15500");
    }

    // ------------------------------------------------------------------ R-K5 : DEJA_GENERE, désistement

    /** R-K5 : 2ᵉ génération → DEJA_GENERE, toujours une seule ligne (au nom de A). */
    @Test
    void rk5_deuxiemeGeneration_DEJA_GENERE_uneSeuleLigneDeA() throws Exception {
        generer(T_CONCRETISATION, dA).andExpect(status().isOk());

        ResultActions r = generer(T_CONCRETISATION, dA).andExpect(status().isOk());

        assertAll(
                () -> assertThat(raisons(r, dA.id())).containsExactly("DEJA_GENERE"),
                () -> verifierLigne(seuleLigne(dA, CONCRETISATION), agenceA, "15500"));
    }

    /** R-K5 « DEJA_GENERE se calcule sur le bénéficiaire (agence) » : une ligne déjà au nom de A suffit. */
    @Test
    void rk5_ligneDejaAuNomDeLAgence_DEJA_GENERE_rienAjoute() throws Exception {
        fixture.ligne(dA, CONCRETISATION, agenceA, new BigDecimal("15500"), AVANCE, null);

        ResultActions r = generer(T_CONCRETISATION, dA).andExpect(status().isOk());

        assertAll(
                () -> assertThat(raisons(r, dA.id())).containsExactly("DEJA_GENERE"),
                () -> assertThat(fixture.lignesDuDossier(dA.id())).hasSize(1),
                () -> assertThat(fixture.compter(LIGNES_DU_TIERS, vendeurA)).isZero());
    }

    /** R-K5 : concrétisation générée par l'API, puis désistement → reprise négative au nom de A. */
    @Test
    void rk5_desistementDossierVendeurAgence_repriseNegativeAuNomDeA() throws Exception {
        generer(T_CONCRETISATION, dA).andExpect(status().isOk());
        fixture.poser(dA, "StrStatusId", fixture.statutDesiste(c).getId());
        fixture.poser(dA, "CancelDate", ANNULATION);

        generer(T_DESISTEMENT, dA).andExpect(status().isOk());

        verifierLigne(seuleLigne(dA, DESISTEMENT), agenceA, "-15500");
    }

    // ------------------------------------------------------------------ R-K6 : document à payer

    /** R-K6 : document des lignes générées pour A → {@code BuyDocument.CfgTierId} = A. */
    @Test
    void rk6_documentDesLignesDeA_tiersDuDocumentEgalA() throws Exception {
        fixture.configurationDocument();
        fixture.chantier(c.tranche(), "CH-" + c.jeton());
        List<UUID> ids = creees(generer(T_CONCRETISATION, dA).andExpect(status().isOk())).stream()
                .map(UUID::fromString).toList();

        mockMvc.perform(document(ids).with(gestionnaire)).andExpect(status().isCreated());

        assertAll(
                () -> assertThat(ids).hasSize(1),
                () -> assertThat(fixture.compter(DOCUMENTS_DU_TIERS, agenceA)).isEqualTo(1),
                () -> assertThat(fixture.compter(DOCUMENTS_DU_TIERS, vendeurA)).isZero());
    }

    /** R-K6 : A + B → 409, A + C → 409, rien écrit ; témoin A seul → 201. */
    @Test
    void rk6_lignesDeDeuxBeneficiaires_409_temoinAgenceSeule201() throws Exception {
        fixture.configurationDocument();
        fixture.chantier(c.tranche(), "CH-" + c.jeton());
        UUID ligneA = fixture.ligne(dA, CONCRETISATION, agenceA, new BigDecimal("300"), AVANCE, null);
        UUID ligneB = fixture.ligne(dB, CONCRETISATION, agenceB, new BigDecimal("200"), AVANCE, null);
        UUID ligneC = fixture.ligne(dC, CONCRETISATION, commercialSiege, new BigDecimal("100"), AVANCE, null);

        mockMvc.perform(document(List.of(ligneA, ligneB)).with(gestionnaire)).andExpect(status().isConflict());
        mockMvc.perform(document(List.of(ligneA, ligneC)).with(gestionnaire)).andExpect(status().isConflict());
        assertAll(
                () -> assertThat(fixture.compter(DOCUMENTS_DU_TIERS, agenceA)).isZero(),
                () -> assertThat(fixture.ligneParId(ligneA).get("BuyDocumentId")).isNull());

        mockMvc.perform(document(List.of(ligneA)).with(gestionnaire)).andExpect(status().isCreated());
        assertThat(fixture.compter(DOCUMENTS_DU_TIERS, agenceA)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ R-K7 : écran des règles

    private ResultActions envoyerRegles(CfgTranche t, CorpsJson... lignes) throws Exception {
        return mockMvc.perform(put(COMMISSIONS_TRANCHE, t.getId()).with(connecte(patrimoine.administrateur()))
                .contentType(MediaType.APPLICATION_JSON).content(lignes(lignes)));
    }

    /** R-K7 : une règle dont le tiers est une agence est acceptée et relue. */
    @Test
    void rk7_regleTiersAgence_acceptee_relue() throws Exception {
        CfgTranche t = fixture.autreTranche(c, "R", true);

        envoyerRegles(t, corps().avec("dateDebut", DEBUT).avec("dateFin", FIN).avec("commercialId", agenceA))
                .andExpect(status().isOk());

        mockMvc.perform(get(COMMISSIONS_TRANCHE, t.getId()).with(connecte(patrimoine.administrateur())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[*].commercialId", hasItem(agenceA.toString())));
    }

    /** R-K7 : deux règles de la même agence qui se chevauchent → 400 ; témoin sans chevauchement → 200. */
    @Test
    void rk7_deuxReglesAgenceAChevauchantes_400_temoin200() throws Exception {
        CfgTranche t = fixture.autreTranche(c, "R", true);

        envoyerRegles(t, corps().avec("dateDebut", DEBUT).avec("dateFin", JUIN).avec("commercialId", agenceA),
                corps().avec("dateDebut", MILIEU).avec("dateFin", FIN).avec("commercialId", agenceA))
                .andExpect(status().isBadRequest());
        envoyerRegles(t, corps().avec("dateDebut", DEBUT).avec("dateFin", "2027-03-31T00:00:00").avec("commercialId", agenceA),
                corps().avec("dateDebut", MILIEU).avec("dateFin", FIN).avec("commercialId", agenceA))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ R-K8 : lecture

    /** R-K8 : lignes — {@code beneficiaireAgence} vrai pour A / B, faux pour C ; libellé de A = celui d'A1b. */
    @Test
    void rk8_lignes_beneficiaireAgence_etLibelleDeLAgence() throws Exception {
        UUID ligneA = fixture.ligne(dA, CONCRETISATION, agenceA, new BigDecimal("300"), AVANCE, null);
        UUID ligneB = fixture.ligne(dB, CONCRETISATION, agenceB, new BigDecimal("200"), AVANCE, null);
        UUID ligneC = fixture.ligne(dC, CONCRETISATION, commercialSiege, new BigDecimal("100"), AVANCE, null);
        Object libelleA = agenceSelonA1b(dA);

        ResultActions r = mockMvc.perform(get(LIGNES, trancheId).with(gestionnaire)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(libelleA).as("libellé A1b").isNotNull(),
                () -> assertThat(champ(r, "id", ligneA, BENEFICIAIRE_AGENCE)).containsExactly(true),
                () -> assertThat(champ(r, "id", ligneB, BENEFICIAIRE_AGENCE)).containsExactly(true),
                () -> assertThat(champ(r, "id", ligneC, BENEFICIAIRE_AGENCE)).containsExactly(false),
                () -> assertThat(champ(r, "id", ligneA, "commercial")).containsExactly(libelleA));
    }

    /** R-K8 : dossiers — {@code beneficiaireAgence} et {@code agence} (= valeur A1b pour A, null pour le siège). */
    @Test
    void rk8_dossiers_beneficiaireAgence_etAgenceCommeA1b() throws Exception {
        Object libelleA = agenceSelonA1b(dA);

        ResultActions r = mockMvc.perform(get(DOSSIERS, trancheId).with(gestionnaire)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(libelleA).as("libellé A1b").isNotNull(),
                () -> assertThat(champ(r, "dossierId", dA.id(), BENEFICIAIRE_AGENCE)).containsExactly(true),
                () -> assertThat(champ(r, "dossierId", dB.id(), BENEFICIAIRE_AGENCE)).containsExactly(true),
                () -> assertThat(champ(r, "dossierId", dC.id(), BENEFICIAIRE_AGENCE)).containsExactly(false),
                () -> assertThat(champ(r, "dossierId", dA.id(), "agence")).containsExactly(libelleA),
                () -> assertThat(champ(r, "dossierId", dC.id(), "agence")).containsExactly((Object) null));
    }

    /** R-K8 : factures — {@code beneficiaireAgence} vrai pour le document de A, faux pour celui de C. */
    @Test
    void rk8_factures_beneficiaireAgence() throws Exception {
        UUID docA = fixture.document(agenceA, "DOCA-" + c.jeton(), new BigDecimal("300"));
        UUID docC = fixture.document(commercialSiege, "DOCC-" + c.jeton(), new BigDecimal("100"));
        fixture.ligne(dA, CONCRETISATION, agenceA, new BigDecimal("300"), AVANCE, docA);
        fixture.ligne(dC, CONCRETISATION, commercialSiege, new BigDecimal("100"), AVANCE, docC);

        ResultActions r = mockMvc.perform(get(FACTURES, trancheId).with(gestionnaire)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(champ(r, "id", docA, BENEFICIAIRE_AGENCE)).containsExactly(true),
                () -> assertThat(champ(r, "id", docC, BENEFICIAIRE_AGENCE)).containsExactly(false));
    }

    // ------------------------------------------------------------------ R-K9 : autorisation

    private Map<String, MockHttpServletRequestBuilder> routes(UUID ligne) {
        Map<String, MockHttpServletRequestBuilder> m = new LinkedHashMap<>();
        m.put("liste des tranches", get(TRANCHES));
        m.put("tranche", tranche(trancheId));
        m.put("dossiers", get(DOSSIERS, trancheId));
        m.put("lignes", get(LIGNES, trancheId));
        m.put("factures", get(FACTURES, trancheId));
        m.put("document de la ligne", get(DOCUMENT_DE_LIGNE, ligne));
        m.put("export lignes", get(EXPORT_LIGNES, trancheId));
        m.put("export tranches", get(EXPORT_TRANCHES));
        m.put("droits", get(DROITS));
        m.put("generation", generation(trancheId, T_CONCRETISATION, List.of(dC.id())));
        m.put("montant", montant(ligne, new BigDecimal("250")));
        m.put("document a payer", document(List.of(ligne)));
        m.put("suppression", suppression(List.of(ligne)));
        return m;
    }

    private MockHttpServletResponse reponse(MockHttpServletRequestBuilder requete, RequestPostProcessor qui)
            throws Exception {
        return LectureExport.executer(mockMvc, requete.with(qui)).getResponse();
    }

    /** Compte portant tous les droits commission ET le rôle Agence : la liste blanche A1d seule le refuse. */
    private RequestPostProcessor compteAgenceAvecDroits() {
        Compte compte = fixture.compte(c.jeton(), ECRAN, NOUVEAU, SUPPRIMER, IMPRIMER);
        AdmUserRole lien = new AdmUserRole();
        lien.setId(UUID.randomUUID());
        lien.setAdmUser(compte.compte());
        lien.setAdmRole(em.getReference(AdmRole.class, agences.roleAgence()));
        em.persist(lien);
        em.flush();
        return FixtureDossierVente.connecte(compte.compte(), FixtureAgences.ROLE_AGENCE, FixtureCommissions.ROLE_JWT);
    }

    /** R-K9 : compte agence → 403 + message de fermeture sur chaque route ; témoin siège (mêmes droits) non 403. */
    @Test
    void rk9_compteAgence_403SurChaqueRouteCommissions_temoinSiege() throws Exception {
        UUID ligne = fixture.ligne(dA, CONCRETISATION, agenceA, new BigDecimal("300"), AVANCE, null);
        RequestPostProcessor agence = compteAgenceAvecDroits();

        SoftAssertions soft = new SoftAssertions();
        for (Map.Entry<String, MockHttpServletRequestBuilder> e : routes(ligne).entrySet()) {
            MockHttpServletResponse rep = reponse(e.getValue(), agence);
            soft.assertThat(rep.getStatus()).as("agence : %s", e.getKey()).isEqualTo(403);
            soft.assertThat(rep.getContentAsString()).as("message : %s", e.getKey()).contains(MESSAGE_FERMEE);
        }
        soft.assertThat(fixture.lignesDuDossier(dC.id())).as("rien généré par l'agence").isEmpty();
        for (Map.Entry<String, MockHttpServletRequestBuilder> e : routes(ligne).entrySet()) {
            soft.assertThat(reponse(e.getValue(), gestionnaire).getStatus()).as("siège : %s", e.getKey()).isNotEqualTo(403);
        }
        soft.assertAll();
    }

    /** R-K9 : compte siège sans droit commission → 403 (lecture et écriture). */
    @Test
    void rk9_compteSiegeSansDroit_403() throws Exception {
        RequestPostProcessor sansDroit = fixture.compte(c.jeton()).qui();

        mockMvc.perform(get(LIGNES, trancheId).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(generation(trancheId, T_CONCRETISATION, List.of(dA.id())).with(sansDroit))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(TRANCHES).with(gestionnaire)).andExpect(status().isOk());
        assertThat(fixture.lignesDuDossier(dA.id())).isEmpty();
    }

    /** Garde-fou de lecture du message : {@code $.detail} porte le texte de fermeture (A1d). */
    @Test
    void rk9_compteAgence_detailDuRefus() throws Exception {
        mockMvc.perform(get(TRANCHES).with(compteAgenceAvecDroits()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_FERMEE));
    }
}
