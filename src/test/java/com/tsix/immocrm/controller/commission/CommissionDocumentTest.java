package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.DETAIL;
import static com.tsix.immocrm.fixtures.ApiCommissions.DOCUMENT_DE_LIGNE;
import static com.tsix.immocrm.fixtures.ApiCommissions.FACTURES;
import static com.tsix.immocrm.fixtures.ApiCommissions.document;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.ApiCommissions.nombre;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.horodatage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Document à payer — FEATURE-vente-commissions.md §2 R-G, §3.1 C6 (un commercial, aucune ligne déjà documentée, projet de
 * facturation sur chaque tranche, somme ≠ 0, tout ou rien), §4 ({@code POST /documents-a-payer} 201,
 * {@code GET …/document-a-payer}), §8.1 « Document (C6) », réponses du manager M4 (ventilation par projet dans
 * {@code BuyVentilationExpense}). Horloge fixée au 10/02/2027 : code « 27/03014 » par la séquence « Facture fournisseur ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionDocumentTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2027, 2, 10, 11, 30);
    private static final LocalDate JOUR = LocalDate.of(2024, 2, 23);
    private static final String DEUX_COMMERCIAUX = "Vous devez générer un document par commercial!";
    private static final String DEJA_DOCUMENTEE = "Vous avez des lignes affectées à un document à payé !";
    private static final String SANS_PROJET = "Il faut obligatoirement renseigner un projet de facturation pour les "
            + "dépenses des tranches concernées !";
    private static final String SOMME_NULLE = "Vous ne pouvez pas générer un document, La somme des commissions "
            + "sélectionnées égale 0 !";
    private static final String AUCUNE_LIGNE = "Aucune ligne n'est sélectionnée !";
    private static final String SANS_DOCUMENT = "Cette ligne n'a pas encore un document à payer!";
    private static final String DOCUMENTS_DU_TIERS = "SELECT COUNT(*) FROM BuyDocument WHERE CfgTierId = :id";
    private static final String DOCUMENT_DU_TIERS = "SELECT * FROM BuyDocument WHERE CfgTierId = :id";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return Clock.fixed(MAINTENANT.atZone(FuseauMetier.ZONE).toInstant(), FuseauMetier.ZONE);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;

    private Contexte c;
    private RequestPostProcessor qui;
    private UUID commercial;
    private UUID projet;
    private Dossier d1;
    private Dossier d2;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMP");
        fixture.configurationDocument();
        qui = fixture.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU).qui();
        commercial = fixture.personne("COM-" + c.jeton(), false);
        projet = fixture.chantier(c.tranche(), "CH-" + c.jeton());
        d1 = fixture.dossier(c, "D1", commercial, new BigDecimal("775000"), JOUR);
        d2 = fixture.dossier(c, "D2", commercial, new BigDecimal("775000"), JOUR);
    }

    private UUID ligne(Dossier d, BigDecimal montant) {
        return fixture.ligne(d, CONCRETISATION, commercial, montant, JOUR, null);
    }

    private ResultActions documenter(List<UUID> lignes) throws Exception {
        return mockMvc.perform(document(lignes).with(qui));
    }

    private void verifierRienEcrit(List<UUID> lignes) {
        assertAll(
                () -> assertThat(fixture.compter(DOCUMENTS_DU_TIERS, commercial)).isZero(),
                () -> assertThat(lignes).isNotEmpty()
                        .allSatisfy(l -> assertThat(fixture.ligneParId(l).get("BuyDocumentId")).isNull()),
                () -> assertThat(fixture.valeurSequence()).isEqualTo(3013));
    }

    private Tuple documentEcrit() {
        List<Tuple> documents = fixture.lignes(DOCUMENT_DU_TIERS, commercial);
        assertThat(documents).hasSize(1);
        return documents.get(0);
    }

    // ------------------------------------------------------------------ document conforme

    @Test
    void rg_document_201_enTeteConforme() throws Exception {
        List<UUID> lignes = List.of(ligne(d1, new BigDecimal("300")), ligne(d2, new BigDecimal("100")));

        ResultActions r = documenter(lignes).andExpect(status().isCreated());

        Tuple doc = documentEcrit();
        assertAll(
                () -> assertThat(doc.get("ReferenceType")).isEqualTo("SALE_COMMISSION"),
                () -> assertThat(doc.get("DocType")).isEqualTo("INVOICE"),
                () -> assertThat(decimal(doc.get("AmountTTC"))).isEqualByComparingTo("400"),
                () -> assertThat(doc.get("Code")).isEqualTo("27/03014"),
                () -> assertThat(guid(doc.get("StrEntityId"))).isEqualTo(guid(fixture.entiteDocument())),
                () -> assertThat(guid(doc.get("AdmModuleId"))).isEqualTo(guid(fixture.moduleCom())),
                () -> assertThat(guid(doc.get("CfgCompanyId"))).isEqualTo(guid(c.projet().getCfgCompany().getId())),
                () -> assertThat(horodatage(doc.get("DocDate")).toLocalDate()).isEqualTo(MAINTENANT.toLocalDate()),
                () -> assertThat(fixture.valeurSequence()).isEqualTo(3014),
                () -> assertThat((String) lire(r, "$.id")).isEqualToIgnoringCase(guid(doc.get("PKey"))),
                () -> assertThat((String) lire(r, "$.code")).isEqualTo("27/03014"),
                () -> assertThat((String) lire(r, "$.commercialId")).isEqualTo(commercial.toString()),
                () -> assertThat(nombre(r, "$.montant")).isEqualByComparingTo("400"),
                () -> assertThat((List<Object>) lire(r, "$.lignes")).hasSize(2));
    }

    /** R-G : une ligne de document par ligne de commission (libellé, U, 1, prix TTC = montant, TVA 0, note = mode). */
    @Test
    void rg_uneLigneDeDocumentParLigneDeCommission() throws Exception {
        documenter(List.of(ligne(d1, new BigDecimal("300")), ligne(d2, new BigDecimal("100"))))
                .andExpect(status().isCreated());

        UUID doc = UUID.fromString(guid(documentEcrit().get("PKey")));
        List<Tuple> lignes = fixture.lignes("SELECT * FROM BuyDocumentLine WHERE BuyDocumentId = :id", doc);
        assertThat(lignes).hasSize(2)
                .anySatisfy(l -> assertThat(l.get("Description")).isEqualTo("Concrétisation du dossier N° " + d1.code()))
                .anySatisfy(l -> assertThat(l.get("Description")).isEqualTo("Concrétisation du dossier N° " + d2.code()))
                .allSatisfy(l -> assertAll(
                        () -> assertThat(l.get("Unity")).isEqualTo("U"),
                        () -> assertThat(decimal(l.get("Quantity"))).isEqualByComparingTo("1"),
                        () -> assertThat(decimal(l.get("VatRatio"))).isEqualByComparingTo("0"),
                        () -> assertThat(l.get("Notes")).isEqualTo("Montant")))
                .extracting(l -> decimal(l.get("UnitPriceTTC")))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("300"), new BigDecimal("100"));
    }

    @Test
    void rg_lignesDeCommission_recoiventLeDocument_historiqueDeStatut() throws Exception {
        List<UUID> lignes = List.of(ligne(d1, new BigDecimal("300")), ligne(d2, new BigDecimal("100")));

        documenter(lignes).andExpect(status().isCreated());

        Tuple doc = documentEcrit();
        UUID docId = UUID.fromString(guid(doc.get("PKey")));
        assertAll(
                () -> assertThat(lignes).isNotEmpty().allSatisfy(l ->assertThat(guid(fixture.ligneParId(l).get("BuyDocumentId")))
                        .isEqualTo(guid(docId))),
                () -> assertThat(fixture.lignes("SELECT * FROM BuyDocumentStatusHistory WHERE BuyDocumentId = :id", docId))
                        .singleElement().satisfies(h -> assertThat(guid(h.get("BuyDocumentStatusId")))
                                .isEqualTo(guid(doc.get("BuyDocumentStatusId")))));
    }

    /** M4 : ventilation par projet de facturation, une ligne par projet, Σ taux = 1, taux = part du projet. */
    @Test
    void rg_ventilationParProjet_partDuMontant_sommeEgaleUn() throws Exception {
        var autre = fixture.autreTranche(c, "T2", true);
        UUID projet2 = fixture.chantier(autre, "CH2-" + c.jeton());
        Dossier ailleurs = fixture.dossier(c, autre, "D3", commercial, new BigDecimal("775000"), JOUR);

        documenter(List.of(ligne(d1, new BigDecimal("300")), ligne(ailleurs, new BigDecimal("100"))))
                .andExpect(status().isCreated());

        UUID doc = UUID.fromString(guid(documentEcrit().get("PKey")));
        List<Tuple> ventilations = fixture.lignes("SELECT * FROM BuyVentilationExpense WHERE BuyDocumentId = :id", doc);
        assertThat(ventilations).hasSize(2)
                .anySatisfy(v -> assertAll(
                        () -> assertThat(guid(v.get("PrjProjectId"))).isEqualTo(guid(projet)),
                        () -> assertThat(decimal(v.get("VentilationRate"))).isEqualByComparingTo("0.75")))
                .anySatisfy(v -> assertAll(
                        () -> assertThat(guid(v.get("PrjProjectId"))).isEqualTo(guid(projet2)),
                        () -> assertThat(decimal(v.get("VentilationRate"))).isEqualByComparingTo("0.25")));
    }

    @Test
    void rg_unSeulProjet_ventilationA100Pourcent() throws Exception {
        documenter(List.of(ligne(d1, new BigDecimal("300")))).andExpect(status().isCreated());

        UUID doc = UUID.fromString(guid(documentEcrit().get("PKey")));
        assertThat(fixture.lignes("SELECT * FROM BuyVentilationExpense WHERE BuyDocumentId = :id", doc))
                .singleElement().satisfies(v -> assertAll(
                        () -> assertThat(guid(v.get("PrjProjectId"))).isEqualTo(guid(projet)),
                        () -> assertThat(decimal(v.get("VentilationRate"))).isEqualByComparingTo("1")));
    }

    // ------------------------------------------------------------------ refus (rien n'est écrit)

    @Test
    void c6_deuxCommerciaux_409MessageExact_rienEcrit() throws Exception {
        UUID autre = fixture.personne("AUT-" + c.jeton(), false);
        List<UUID> lignes = List.of(ligne(d1, new BigDecimal("300")),
                fixture.ligne(d2, CONCRETISATION, autre, new BigDecimal("100"), JOUR, null));

        documenter(lignes).andExpect(status().isConflict()).andExpect(jsonPath(DETAIL).value(DEUX_COMMERCIAUX));

        verifierRienEcrit(lignes);
        assertThat(fixture.compter(DOCUMENTS_DU_TIERS, autre)).isZero();
    }

    /** C6 : une ligne déjà documentée — message R-G (statut 400 ou 409, non fixé par le spec). */
    @Test
    void c6_ligneDejaDocumentee_refusMessageExact_rienDePlus() throws Exception {
        UUID existant = fixture.document(commercial, "DOC-" + c.jeton(), new BigDecimal("50"));
        UUID libre = ligne(d1, new BigDecimal("300"));
        UUID documentee = fixture.ligne(d2, CONCRETISATION, commercial, new BigDecimal("50"), JOUR, existant);

        ResultActions r = documenter(List.of(libre, documentee)).andExpect(jsonPath(DETAIL).value(DEJA_DOCUMENTEE));

        assertAll(
                () -> assertThat(r.andReturn().getResponse().getStatus()).isIn(400, 409),
                () -> assertThat(fixture.compter(DOCUMENTS_DU_TIERS, commercial)).isEqualTo(1),
                () -> assertThat(fixture.ligneParId(libre).get("BuyDocumentId")).isNull(),
                () -> assertThat(fixture.valeurSequence()).isEqualTo(3013));
    }

    @Test
    void c6_trancheSansProjetDeFacturation_400MessageExact_rienEcrit() throws Exception {
        var sansChantier = fixture.autreTranche(c, "SC", true);
        Dossier d = fixture.dossier(c, sansChantier, "D9", commercial, new BigDecimal("775000"), JOUR);
        List<UUID> lignes = List.of(ligne(d1, new BigDecimal("300")), ligne(d, new BigDecimal("100")));

        documenter(lignes).andExpect(status().isBadRequest()).andExpect(jsonPath(DETAIL).value(SANS_PROJET));

        verifierRienEcrit(lignes);
    }

    @Test
    void c6_sommeNulle_400MessageExact_rienEcrit() throws Exception {
        List<UUID> lignes = List.of(ligne(d1, new BigDecimal("300")),
                fixture.ligne(d1, DESISTEMENT, commercial, new BigDecimal("-300"), JOUR, null));

        documenter(lignes).andExpect(status().isBadRequest()).andExpect(jsonPath(DETAIL).value(SOMME_NULLE));

        verifierRienEcrit(lignes);
    }

    @Test
    void rg_aucuneLigne_400MessageExact() throws Exception {
        documenter(List.of()).andExpect(status().isBadRequest()).andExpect(jsonPath(DETAIL).value(AUCUNE_LIGNE));

        assertThat(fixture.compter(DOCUMENTS_DU_TIERS, commercial)).isZero();
    }

    /** C6 « tout ou rien » : séquence absente → échec, ni document ni ligne documentée, aucun amorçage. */
    @Test
    void c6_sequenceAbsente_echec_aucunDocumentNiLigneDocumentee() throws Exception {
        List<UUID> lignes = List.of(ligne(d1, new BigDecimal("300")));
        fixture.masquerSequence();

        int statut = documenter(lignes).andReturn().getResponse().getStatus();

        assertAll(
                () -> assertThat(statut).isGreaterThanOrEqualTo(500),
                () -> assertThat(fixture.compter(DOCUMENTS_DU_TIERS, commercial)).isZero(),
                () -> assertThat(fixture.ligneParId(lignes.get(0)).get("BuyDocumentId")).isNull(),
                () -> assertThat(fixture.compter(
                        "SELECT COUNT(*) FROM AdmSequence WHERE Code = N'" + FixtureTerrains.CODE_SEQUENCE
                                + "' AND PKey <> :id", UUID.randomUUID())).isZero());
    }

    // ------------------------------------------------------------------ ouvrir le document, onglet Factures

    @Test
    void p4_ouvrirDocumentDeLaLigne_200() throws Exception {
        UUID ligne = ligne(d1, new BigDecimal("300"));
        documenter(List.of(ligne)).andExpect(status().isCreated());
        String doc = guid(documentEcrit().get("PKey"));

        ResultActions r = mockMvc.perform(get(DOCUMENT_DE_LIGNE, ligne).with(qui)).andExpect(status().isOk());

        assertAll(
                () -> assertThat((String) lire(r, "$.id")).isEqualToIgnoringCase(doc),
                () -> assertThat((String) lire(r, "$.code")).isEqualTo("27/03014"),
                () -> assertThat((List<String>) lire(r, "$.lignes[*].ligneId")).containsExactly(ligne.toString()));
    }

    @Test
    void p4_ouvrirDocument_ligneSansDocument_404MessageExact() throws Exception {
        UUID ligne = ligne(d1, new BigDecimal("300"));

        mockMvc.perform(get(DOCUMENT_DE_LIGNE, ligne).with(qui))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(DETAIL).value(containsString(SANS_DOCUMENT)));
    }

    @Test
    void p4_facturesDeLaTranche_listeLeDocument() throws Exception {
        documenter(List.of(ligne(d1, new BigDecimal("300")))).andExpect(status().isCreated());
        String doc = guid(documentEcrit().get("PKey")).toLowerCase(java.util.Locale.ROOT);

        mockMvc.perform(get(FACTURES, c.tranche().getId()).with(qui))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(doc)));
    }
}
