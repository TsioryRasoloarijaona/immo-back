package com.tsix.immocrm.controller.encaissement;

import com.tsix.immocrm.dto.encaissement.EncaissementDetailDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDto;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/encaissements/{id}} — la fiche d'un règlement et ses affectations.
 *
 * <p>Relevé dans la trace d'audit du 14/09/2026 : à l'ouverture d'une fiche règlement, l'écran
 * légataire charge les lignes d'affectation (`ORDER BY Amount ASC`), puis `StlDeduction`,
 * `StlSettlementDocumentToAttach` et `StlSettlementHistory`. Les deux premières sont vides sur la
 * totalité du périmètre Vente, la troisième n'est informative que sur 65 règlements sur 643 : seules
 * les affectations sont reprises. Voir
 * {@code instructions/FEATURE-affectations-encaissement.md}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EncaissementFicheAffectationsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private ComSettlementCategoryRepository categoryRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private EntityManager entityManager;

    /**
     * Vide le contexte de persistance avant l'appel HTTP — {@code montantBrutRestant} et
     * {@code montantNet} ne sont peuplés qu'au chargement de l'entité.
     */
    private void relireDepuisLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private ComFolder newDossier(String suffixe) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-AFF-" + suffixe);
        vocationRepository.save(vocation);

        ComFolder dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode("DOS-AFF-" + suffixe);
        dossier.setStkVocation(vocation);
        return folderRepository.save(dossier);
    }

    private ComDocument newEcheance(ComFolder dossier, String code, BigDecimal montant,
                                    ComSettlementCategory categorie) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(dossier);
        echeance.setCode(code);
        echeance.setDescription("Échéance " + code);
        echeance.setDocDate(LocalDateTime.of(2025, 6, 15, 0, 0));
        echeance.setAmount(montant);
        echeance.setComSettlementCategory(categorie);
        return documentRepository.save(echeance);
    }

    private ComSettlementCategory newCategorie(String code) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription("Catégorie " + code);
        return categoryRepository.save(categorie);
    }

    private StlSettlement newReglement(String reference, BigDecimal montant) {
        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setCode(reference);
        reglement.setDocDate(LocalDateTime.now());
        reglement.setAmount(montant);
        reglement.setStatus("IN_WORK");
        reglement.setIsCredit(true);
        return settlementRepository.save(reglement);
    }

    private void affecter(StlSettlement reglement, ComDocument echeance, BigDecimal montant) {
        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(montant);
        settlementLineRepository.save(ligne);
    }

    // ------------------------------------------------------------------
    // Les affectations
    // ------------------------------------------------------------------

    /**
     * Trois échéances soldées par un règlement, dans l'ordre de l'écran légataire : montant
     * affecté croissant.
     */
    @Test
    void fiche_listeLesEcheancesSoldees_montantCroissant() throws Exception {
        ComFolder dossier = newDossier("A");
        ComSettlementCategory avance = newCategorie("AVANCE-AFF");
        ComDocument grosse = newEcheance(dossier, "ECH-AFF-A1", new BigDecimal("5000.00"), avance);
        ComDocument moyenne = newEcheance(dossier, "ECH-AFF-A2", new BigDecimal("3000.00"), avance);
        ComDocument petite = newEcheance(dossier, "ECH-AFF-A3", new BigDecimal("1000.00"), avance);

        StlSettlement reglement = newReglement("REF-AFF-A", new BigDecimal("9000.00"));
        // Insérées dans le désordre : c'est la requête qui doit trier, pas l'ordre d'insertion.
        affecter(reglement, grosse, new BigDecimal("5000.00"));
        affecter(reglement, petite, new BigDecimal("1000.00"));
        affecter(reglement, moyenne, new BigDecimal("3000.00"));
        relireDepuisLaBase();

        mockMvc.perform(get("/api/encaissements/" + reglement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectations.length()").value(3))
                .andExpect(jsonPath("$.affectations[0].montantAffecte").value(1000.00))
                .andExpect(jsonPath("$.affectations[1].montantAffecte").value(3000.00))
                .andExpect(jsonPath("$.affectations[2].montantAffecte").value(5000.00))
                .andExpect(jsonPath("$.affectations[0].echeanceCode").value("ECH-AFF-A3"))
                .andExpect(jsonPath("$.affectations[0].echeanceDescription").value("Échéance ECH-AFF-A3"))
                .andExpect(jsonPath("$.affectations[0].montantEcheance").value(1000.00))
                .andExpect(jsonPath("$.affectations[0].categorie").value("AVANCE-AFF"))
                .andExpect(jsonPath("$.affectations[0].dossierCode").value("DOS-AFF-A"));
    }

    /**
     * <strong>Le défaut que cette ressource existe pour corriger.</strong>
     *
     * <p>Un règlement qui solde des échéances de deux dossiers n'expose qu'un seul
     * {@code dossierCode} dans la forme de liste — celui de la première ligne. Mesuré sur les
     * données réelles : 8 règlements sur 643 sont dans ce cas, et pour eux la liste affiche un
     * dossier et en cache un autre. La fiche, elle, les montre tous.
     */
    @Test
    void fiche_reveleLesDossiersQueLaListeMasque() throws Exception {
        ComFolder premier = newDossier("B1");
        ComFolder second = newDossier("B2");
        ComDocument echeancePremier = newEcheance(premier, "ECH-AFF-B1", new BigDecimal("400.00"), null);
        ComDocument echeanceSecond = newEcheance(second, "ECH-AFF-B2", new BigDecimal("600.00"), null);

        StlSettlement reglement = newReglement("REF-AFF-B", new BigDecimal("1000.00"));
        affecter(reglement, echeancePremier, new BigDecimal("400.00"));
        affecter(reglement, echeanceSecond, new BigDecimal("600.00"));
        relireDepuisLaBase();

        // La liste ne peut nommer qu'un dossier — c'est sa limite, pas un bug à corriger là.
        mockMvc.perform(get("/api/encaissements").param("reference", "REF-AFF-B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dossierCode").isString());

        // La fiche les porte tous les deux.
        mockMvc.perform(get("/api/encaissements/" + reglement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectations.length()").value(2))
                .andExpect(jsonPath("$.affectations[*].dossierCode",
                        org.hamcrest.Matchers.containsInAnyOrder("DOS-AFF-B1", "DOS-AFF-B2")));
    }

    /**
     * La somme des affectations doit retomber sur la part affectée du règlement.
     *
     * <p>{@code montantBrutRestant} est calculé par une pré-agrégation jointe, les affectations par
     * une requête distincte. Les deux doivent dire la même chose — sinon la fiche s'auto-contredit,
     * ce qui est pire que de ne rien afficher.
     */
    @Test
    void sommeDesAffectations_egaleLaPartAffectee() throws Exception {
        ComFolder dossier = newDossier("C");
        ComDocument echeance1 = newEcheance(dossier, "ECH-AFF-C1", new BigDecimal("1000.00"), null);
        ComDocument echeance2 = newEcheance(dossier, "ECH-AFF-C2", new BigDecimal("1000.00"), null);

        // 1 000 encaissés, 700 affectés : il reste 300.
        StlSettlement reglement = newReglement("REF-AFF-C", new BigDecimal("1000.00"));
        affecter(reglement, echeance1, new BigDecimal("250.00"));
        affecter(reglement, echeance2, new BigDecimal("450.00"));
        relireDepuisLaBase();

        mockMvc.perform(get("/api/encaissements/" + reglement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantBrut").value(1000.00))
                .andExpect(jsonPath("$.montantBrutRestant").value(300.00))
                .andExpect(jsonPath("$.affectations[0].montantAffecte").value(250.00))
                .andExpect(jsonPath("$.affectations[1].montantAffecte").value(450.00));
    }

    /**
     * Un règlement encaissé mais pas encore imputé rend un tableau <strong>vide</strong>, jamais
     * {@code null} : le front itère dessus sans garde.
     *
     * <p><strong>Ce test est aussi la non-régression d'un 500.</strong> L'association vers la
     * pré-agrégation des affectations a pour clé étrangère le {@code PKey} du règlement lui-même,
     * toujours renseigné : Hibernate fabrique donc toujours un proxy. Tant que le sous-select ne
     * portait que les règlements <em>ayant</em> des lignes, déréférencer ce proxy sur un règlement
     * sans ligne levait {@code ObjectNotFoundException} — <strong>130 règlements sur 3 243</strong>
     * (4 %) sont dans ce cas sur les données réelles. Aucun test ne l'avait vu, la liste étant
     * restreinte aux règlements affectés ; la fiche, elle, ne l'est pas. Ne pas affaiblir ce cas.
     */
    @Test
    void reglementNonAffecte_rendUnTableauVide() throws Exception {
        StlSettlement reglement = newReglement("REF-AFF-D", new BigDecimal("2500.00"));
        relireDepuisLaBase();

        mockMvc.perform(get("/api/encaissements/" + reglement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectations").isArray())
                .andExpect(jsonPath("$.affectations.length()").value(0))
                // Rien n'est affecté, donc tout reste.
                .andExpect(jsonPath("$.montantBrutRestant").value(2500.00));
    }

    // ------------------------------------------------------------------
    // La forme de la fiche
    // ------------------------------------------------------------------

    /**
     * Une fiche dit tout : elle n'est pas filtrée par la sélection de colonnes, et
     * {@code ?colonnes=} n'a plus d'effet sur elle.
     */
    @Test
    void fiche_nEstPasFiltreeParLaSelectionDeColonnes() throws Exception {
        StlSettlement reglement = newReglement("REF-AFF-E", new BigDecimal("100.00"));
        reglement.setDescription("Libellé hors sélection par défaut");
        reglement.setUserCreate("s.amrani");
        settlementRepository.save(reglement);
        relireDepuisLaBase();

        // Sans ?colonnes= : les champs hors défaut sortent quand même.
        mockMvc.perform(get("/api/encaissements/" + reglement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Libellé hors sélection par défaut"))
                .andExpect(jsonPath("$.creePar").value("s.amrani"));

        // Avec un ?colonnes= restrictif : sans effet, la fiche reste complète.
        mockMvc.perform(get("/api/encaissements/" + reglement.getId()).param("colonnes", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("REF-AFF-E"))
                .andExpect(jsonPath("$.description").value("Libellé hors sélection par défaut"));
    }

    /**
     * La garde contre la dérive des deux formes.
     *
     * <p>{@link EncaissementDetailDto} recopie les composants de {@link EncaissementDto} plutôt que
     * de les imbriquer — imbriquer aurait changé la forme de la réponse, et un
     * {@code @JsonUnwrapped} aurait réintroduit le filtre de colonnes de la liste. Le risque est
     * donc qu'un champ ajouté à la liste soit oublié sur la fiche. Ce test le transforme en échec
     * de compilation logique plutôt qu'en absence silencieuse.
     */
    @Test
    void ficheEtListe_exposentLesMemesChamps() {
        List<String> champsListe = Arrays.stream(EncaissementDto.class.getRecordComponents())
                .map(RecordComponent::getName).toList();
        List<String> champsFiche = Arrays.stream(EncaissementDetailDto.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertThat(champsFiche)
                .as("La fiche doit porter tous les champs de la liste, dans le même ordre, "
                        + "plus « affectations » — sans quoi un champ ajouté à la liste "
                        + "disparaîtrait silencieusement de la fiche.")
                .containsExactlyElementsOf(
                        java.util.stream.Stream.concat(champsListe.stream(), java.util.stream.Stream.of("affectations"))
                                .toList());
    }
}
