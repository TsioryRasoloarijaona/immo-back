package com.tsix.immocrm.controller.dashboard;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dashboard/annuel} — voir {@code instructions/FEATURE-dashboard-omran.md}.
 *
 * <h2>Pourquoi une année dédiée plutôt que des deltas</h2>
 *
 * <p>La base de test partage ses tables entre classes. {@link DashboardControllerTest} contourne
 * le problème en mesurant un écart avant/après ; ici les fixtures sont datées de <b>2077</b>, ce
 * qui isole complètement l'assiette et permet d'asserter des valeurs <b>exactes</b> plutôt que des
 * variations. Un test qui affirme « le total vaut 300 000 » attrape une double-comptabilisation ;
 * un test qui affirme « le total a augmenté » ne l'attrape pas.
 *
 * <p>Seule exception : {@code dossiersSansDateAvance} n'est pas borné par l'année — une date
 * absente n'appartient à aucune année. Ce compteur est donc vérifié en écart.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SyntheseAnnuelleTest {

    /** Année sans aucune donnée réelle ni fixture d'une autre classe : l'assiette est à nous. */
    private static final int ANNEE = 2077;
    private static final String URL = "/api/dashboard/annuel";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private ComDocumentRepository documentRepository;

    @Autowired
    private ComSettlementCategoryRepository categoryRepository;

    @Autowired
    private StlSettlementRepository settlementRepository;

    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;

    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    @Autowired
    private StrStatusRepository strStatusRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    // ---------------------------------------------------------------- fixtures

    /**
     * Le seuil de concrétisation est résolu par ce statut : sans lui, le service retombe sur son
     * seuil par défaut et ne filtre plus rien. Chaque test qui parle d'assiette doit donc le poser.
     */
    private StrStatus newStatut(String code, int sorting) {
        StrStatus statut = new StrStatus();
        statut.setId(UUID.randomUUID());
        statut.setCode(code);
        statut.setSorting(sorting);
        statut.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        return strStatusRepository.save(statut);
    }

    private StrStatus concretisation() {
        return newStatut(StatutDossier.CONCRETISATION.getCode(), 2);
    }

    private CfgTier newTier() {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("SA-TIER-" + UUID.randomUUID());
        t.setSocialReason("CLIENT TEST");
        return tierRepository.save(t);
    }

    /** {@code StkVocation} est le seul {@code @NotNull} de {@code ComFolder} : sans lui, persist echoue. */
    private StkVocation newVocation() {
        StkVocation v = new StkVocation();
        v.setId(UUID.randomUUID());
        v.setCode("SA-VOC-" + UUID.randomUUID());
        return stkVocationRepository.save(v);
    }

    private ComFolder newDossier(StrStatus statut, LocalDateTime dateAvance, String prix) {
        ComFolder f = new ComFolder();
        f.setId(UUID.randomUUID());
        f.setCode("SA-DOS-" + UUID.randomUUID());
        f.setStkVocation(newVocation());
        f.setStrStatus(statut);
        f.setCfgCustomer(newTier());
        f.setAdvanceDate(dateAvance);
        f.setPrice(new BigDecimal(prix));
        return folderRepository.save(f);
    }

    /** Catégorie d'avance : celle qui entre dans la courbe et dans le montant réglé. */
    private ComSettlementCategory newCategorieAvance() {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode("SA-CAT-AV-" + UUID.randomUUID());
        c.setIsSaleAdvance(true);
        c.setIsSaleExpense(false);
        c.setIsCredit(true);
        return categoryRepository.save(c);
    }

    /** Catégorie de frais : admise par la clause OR du légataire, rejetée par l'intention. */
    private ComSettlementCategory newCategorieFrais() {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode("SA-CAT-FR-" + UUID.randomUUID());
        c.setIsSaleAdvance(true);
        c.setIsSaleExpense(true);
        c.setIsCredit(true);
        return categoryRepository.save(c);
    }

    private ComDocument newEcheance(ComFolder dossier, ComSettlementCategory categorie,
                                    LocalDateTime date, String montant) {
        ComDocument d = new ComDocument();
        d.setId(UUID.randomUUID());
        d.setComFolder(dossier);
        d.setComSettlementCategory(categorie);
        d.setDocDate(date);
        d.setAmount(new BigDecimal(montant));
        return documentRepository.save(d);
    }

    private StlSettlementType newType(boolean nonComptabilise) {
        StlSettlementType t = new StlSettlementType();
        t.setId(UUID.randomUUID());
        t.setCode("SA-TYP-" + UUID.randomUUID());
        t.setIsNotContabilized(nonComptabilise);
        return settlementTypeRepository.save(t);
    }

    private StlSettlementLine newReglement(ComDocument echeance, StlSettlementType type,
                                           boolean credit, LocalDateTime dateEcheance,
                                           LocalDateTime datePointage, String montant) {
        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("SA-REG-" + UUID.randomUUID());
        s.setStlSettlementType(type);
        s.setTier(newTier());
        s.setIsCredit(credit);
        s.setEcheanceDate(dateEcheance);
        s.setSettlementDate(datePointage);
        s.setAmount(new BigDecimal(montant));
        s = settlementRepository.save(s);

        StlSettlementLine l = new StlSettlementLine();
        l.setId(UUID.randomUUID());
        l.setStlSettlement(s);
        l.setComDocument(echeance);
        l.setAmount(new BigDecimal(montant));
        return settlementLineRepository.save(l);
    }

    private static LocalDateTime jourDe(int mois, int jour) {
        return LocalDateTime.of(ANNEE, mois, jour, 0, 0);
    }

    // ---------------------------------------------------------------- bloc A

    /**
     * Les douze points sont servis même quand un seul mois porte une échéance. C'est ce qui permet
     * au client de tracer la courbe sans savoir quels mois manquent.
     */
    @Test
    void echeancier_sertTouseLesMoisMemeVides() throws Exception {
        ComFolder dossier = newDossier(concretisation(), jourDe(3, 1), "1000000");
        newEcheance(dossier, newCategorieAvance(), jourDe(8, 15), "150000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annee").value(ANNEE))
                .andExpect(jsonPath("$.echeancier.length()").value(12))
                .andExpect(jsonPath("$.echeancier[7].mois").value(8))
                .andExpect(jsonPath("$.echeancier[7].montant").value(150000))
                .andExpect(jsonPath("$.echeancier[7].lignes").value(1))
                .andExpect(jsonPath("$.echeancier[0].mois").value(1))
                .andExpect(jsonPath("$.echeancier[0].montant").value(0))
                .andExpect(jsonPath("$.echeancier[0].lignes").value(0));
    }

    /**
     * Une échéance de frais n'entre pas dans la courbe. C'est l'intention derrière la contradiction
     * du légataire — sa clause {@code OR IsSaleExpense=1} est annulée par un
     * {@code AND IsSaleExpense=0} deux lignes plus bas.
     */
    @Test
    void echeancier_ecarteLesFrais() throws Exception {
        ComFolder dossier = newDossier(concretisation(), jourDe(3, 1), "1000000");
        newEcheance(dossier, newCategorieAvance(), jourDe(5, 10), "100000");
        newEcheance(dossier, newCategorieFrais(), jourDe(5, 20), "777000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echeancier[4].montant").value(100000))
                .andExpect(jsonPath("$.echeancier[4].lignes").value(1));
    }

    /** Sous le seuil de concrétisation, le dossier ne compte dans aucun bloc. */
    @Test
    void echeancier_ecarteUnDossierEnReservation() throws Exception {
        concretisation();
        ComFolder reserve = newDossier(newStatut("RESERVATION", 1), jourDe(3, 1), "1000000");
        newEcheance(reserve, newCategorieAvance(), jourDe(6, 10), "500000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echeancier[5].montant").value(0))
                .andExpect(jsonPath("$.echeancier[5].lignes").value(0));
    }

    /**
     * ANNULATION et DESISTEMENT sont écartés bien que leur {@code Sorting} dépasse le seuil : c'est
     * {@code StatutDossier.isSortie()} qui les retire, l'équivalent exact des drapeaux
     * {@code BoolField002}/{@code BoolField003} du légataire.
     */
    @Test
    void echeancier_ecarteLesSorties() throws Exception {
        concretisation();
        ComFolder annule = newDossier(newStatut(StatutDossier.ANNULATION.getCode(), 30), jourDe(3, 1), "900000");
        newEcheance(annule, newCategorieAvance(), jourDe(9, 5), "300000");
        ComFolder desiste = newDossier(newStatut(StatutDossier.DESISTEMENT.getCode(), 31), jourDe(3, 2), "900000");
        newEcheance(desiste, newCategorieAvance(), jourDe(9, 6), "400000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echeancier[8].montant").value(0))
                .andExpect(jsonPath("$.echeancier[8].lignes").value(0));
    }

    // ---------------------------------------------------------------- bloc B

    @Test
    void ventes_comptentLesDossiersConcretisesDeLAnnee() throws Exception {
        newDossier(concretisation(), jourDe(4, 12), "1500000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventesConcretisees.nombreDossiers").value(1))
                .andExpect(jsonPath("$.ventesConcretisees.prixTotal").value(1500000));
    }

    /**
     * Le trou d'assiette du légataire : 232 des 387 dossiers réels n'ont pas de date d'avance. Ils
     * ne sont pas comptés dans les ventes, mais le compteur dédié les rend visibles.
     */
    @Test
    void ventes_dossierSansDateAvance_estCompteAPart() throws Exception {
        String avant = mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long sansDateAvant = com.jayway.jsonpath.JsonPath.parse(avant)
                .read("$.ventesConcretisees.dossiersSansDateAvance", Integer.class);

        newDossier(concretisation(), null, "800000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventesConcretisees.nombreDossiers").value(0))
                .andExpect(jsonPath("$.ventesConcretisees.dossiersSansDateAvance").value(sansDateAvant + 1));
    }

    /** Le montant réglé ne retient qu'avances, crédits et autofinancements — pas les frais. */
    @Test
    void ventes_montantRegle_sommeLesAvances() throws Exception {
        ComFolder dossier = newDossier(concretisation(), jourDe(2, 1), "1000000");
        ComDocument echeance = newEcheance(dossier, newCategorieAvance(), jourDe(2, 10), "250000");
        newReglement(echeance, newType(false), true, jourDe(2, 10), null, "250000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventesConcretisees.montantRegle").value(250000));
    }

    // ---------------------------------------------------------------- bloc C

    /** FEATURE-lot4 §2 : un règlement annulé ne compte ni dans le réglé, ni dans les encaissements. */
    @Test
    void reglementAnnule_excluDuMontantRegleEtDesEncaissements() throws Exception {
        ComFolder dossier = newDossier(concretisation(), jourDe(2, 1), "1000000");
        ComDocument echeance = newEcheance(dossier, newCategorieAvance(), jourDe(2, 10), "400000");
        newReglement(echeance, newType(false), true, jourDe(2, 10), null, "100000");
        StlSettlement annule = newReglement(echeance, newType(false), true, jourDe(2, 10), null, "250000")
                .getStlSettlement();
        annule.setStatus("CANCELED");
        settlementRepository.save(annule);

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventesConcretisees.montantRegle").value(100000))
                .andExpect(jsonPath("$.encaissements.nombreLignes").value(1))
                .andExpect(jsonPath("$.encaissements.montantTotal").value(100000));
    }

    /**
     * Un règlement non pointé est rattaché à l'année de son <b>échéance</b> : {@code SettlementDate}
     * n'est renseignée que sur les pointés, 14 lignes réelles sur 3 244.
     */
    @Test
    void encaissements_reglementNonPointe_suitSonEcheance() throws Exception {
        ComFolder dossier = newDossier(concretisation(), jourDe(1, 5), "1000000");
        ComDocument echeance = newEcheance(dossier, newCategorieAvance(), jourDe(1, 10), "300000");
        newReglement(echeance, newType(false), true, jourDe(1, 10), null, "300000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encaissements.nombreLignes").value(1))
                .andExpect(jsonPath("$.encaissements.montantTotal").value(300000));
    }

    /** Un règlement pointé suit sa date de pointage, pas son échéance. */
    @Test
    void encaissements_reglementPointe_suitSaDateDeReglement() throws Exception {
        ComFolder dossier = newDossier(concretisation(), jourDe(1, 5), "1000000");
        ComDocument echeance = newEcheance(dossier, newCategorieAvance(), jourDe(1, 10), "300000");
        // Échéance dans l'année précédente, pointage dans l'année observée : c'est le pointage qui décide.
        newReglement(echeance, newType(false), true,
                LocalDateTime.of(ANNEE - 1, 12, 20, 0, 0), jourDe(1, 15), "300000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encaissements.nombreLignes").value(1))
                .andExpect(jsonPath("$.encaissements.montantTotal").value(300000));
    }

    /**
     * Un décaissement n'est pas un encaissement négatif : il est hors sujet. C'est le correctif du
     * lot K, où un décaissement de 470 000 DH faussait les totaux.
     */
    @Test
    void encaissements_ecarteUnDecaissement() throws Exception {
        ComFolder dossier = newDossier(concretisation(), jourDe(1, 5), "1000000");
        ComDocument echeance = newEcheance(dossier, newCategorieAvance(), jourDe(3, 10), "500000");
        newReglement(echeance, newType(false), false, jourDe(3, 10), null, "500000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encaissements.nombreLignes").value(0))
                .andExpect(jsonPath("$.encaissements.montantTotal").value(0));
    }

    /** Un type non comptabilisé n'entre pas davantage. */
    @Test
    void encaissements_ecarteUnTypeNonComptabilise() throws Exception {
        ComFolder dossier = newDossier(concretisation(), jourDe(1, 5), "1000000");
        ComDocument echeance = newEcheance(dossier, newCategorieAvance(), jourDe(4, 10), "600000");
        newReglement(echeance, newType(true), true, jourDe(4, 10), null, "600000");

        mockMvc.perform(get(URL).param("annee", String.valueOf(ANNEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encaissements.nombreLignes").value(0));
    }

    // ---------------------------------------------------------------- l'année

    /** Sans paramètre, l'année courante — et donc pas nos fixtures de 2077. */
    @Test
    void sansParametre_sertLAnneeCourante() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annee").value(java.time.LocalDate.now().getYear()))
                .andExpect(jsonPath("$.echeancier.length()").value(12));
    }
}
