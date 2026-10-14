package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.PenaliteRetard;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Pénalité de retard d'une échéance, à l'ajout d'un règlement ({@code ComDocument.cs:ManagePenalty},
 * {@code FEATURE-impayes.md} P2-P3). Rejoint la transaction de l'appelant ; ne bloque jamais le règlement.
 */
@Component
public class PenaliteRetardService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int ECHELLE = 2;

    private final ParametresVente parametres;
    private final ComDocumentRepository documentRepository;
    private final ComSettlementCategoryRepository categoryRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final LigneEcheancierOmran ligneOmran;
    private final TotauxDossierEcheancier totaux;
    private final Clock clock;

    public PenaliteRetardService(ParametresVente parametres, ComDocumentRepository documentRepository,
                                 ComSettlementCategoryRepository categoryRepository,
                                 StlSettlementLineRepository settlementLineRepository,
                                 LigneEcheancierOmran ligneOmran, TotauxDossierEcheancier totaux, Clock clock) {
        this.parametres = parametres;
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.ligneOmran = ligneOmran;
        this.totaux = totaux;
        this.clock = clock;
    }

    /**
     * À appeler avant d'enregistrer le règlement : {@code k} se calcule sur le reste d'avant. Une pénalité
     * existante est réutilisée telle quelle ; un montant nul ne crée rien.
     *
     * @return la pénalité de l'échéance, vide si elle n'est pas éligible
     */
    public Optional<ComDocument> genererPenalite(ComDocument echeance) {
        BigDecimal taux = parametres.tauxPenalite();
        Optional<UUID> categoriePenalite = parametres.categoriePenalite();
        if (!eligible(echeance, taux, categoriePenalite.orElse(null))) {
            return Optional.empty();
        }
        if (echeance.getComPenaltyDocument() != null) {
            return Optional.of(echeance.getComPenaltyDocument());
        }
        Optional<ComSettlementCategory> categorie = categoriePenalite.flatMap(categoryRepository::findById);
        if (categorie.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal regle = requireNonNullElse(
                settlementLineRepository.sumAmountByComDocumentId(echeance.getId()), BigDecimal.ZERO);
        BigDecimal montantHT = PenaliteRetard.montantHT(taux, echeance.getAmount(), echeance.getAmountHT(), regle);
        if (montantHT.signum() == 0) {
            return Optional.empty();
        }
        ComDocument penalite = documentRepository.save(nouvellePenalite(echeance, categorie.get(), montantHT));
        echeance.setComPenaltyDocument(penalite);
        documentRepository.save(echeance);
        // OMRAN réenregistre le dossier (ComFolder.Commit) : le net à payer compte les pénalités.
        totaux.rafraichir(echeance.getComFolder());
        return Optional.of(penalite);
    }

    /**
     * Toutes les conditions de P2. {@code DocDate} strictement avant aujourd'hui (comme OMRAN), à la différence
     * de l'impayé, qui compte le jour même ; sans catégorie pénalité paramétrée, rien n'est éligible.
     */
    boolean eligible(ComDocument echeance, BigDecimal taux, UUID categoriePenalite) {
        ComSettlementCategory categorie = echeance.getComSettlementCategory();
        ComFolder dossier = echeance.getComFolder();
        LocalDate aujourdhui = LocalDate.now(clock);
        return echeance.getDocDate() != null
                && echeance.getDocDate().toLocalDate().isBefore(aujourdhui)
                && taux != null && taux.signum() > 0
                && categoriePenalite != null
                && categorie != null && Boolean.TRUE.equals(categorie.getIsWithPenalty())
                && !categoriePenalite.equals(categorie.getId())
                && dossier != null && !Boolean.TRUE.equals(dossier.getIsPenaltyDisabled());
    }

    private ComDocument nouvellePenalite(ComDocument echeance, ComSettlementCategory categorie, BigDecimal montantHT) {
        BigDecimal tva = requireNonNullElse(echeance.getVATRatio(), LigneEcheancierOmran.TVA_PAR_DEFAUT);
        BigDecimal montant = montantHT.multiply(BigDecimal.ONE.add(tva)).setScale(ECHELLE, RoundingMode.HALF_UP);
        ComSettlementCategory categorieEcheance = echeance.getComSettlementCategory();

        ComDocument penalite = new ComDocument();
        penalite.setId(UUID.randomUUID());
        ligneOmran.signerCreation(penalite);
        penalite.setComFolder(echeance.getComFolder());
        penalite.setCfgCompany(echeance.getCfgCompany());
        penalite.setAdmModuleId(echeance.getAdmModuleId());
        penalite.setDocDate(LocalDate.now(clock).atStartOfDay());
        penalite.setComSettlementCategory(categorie);
        penalite.setDescription(categorie.getDescription());
        penalite.setVATRatio(tva);
        penalite.setAmountHT(montantHT);
        penalite.setAmountTTC(montant);
        penalite.setAmount(montant);
        penalite.setDiscountRatio(BigDecimal.ZERO);
        penalite.setExcludeFromPrice(Boolean.TRUE.equals(categorie.getExcludeFromPrice()));
        penalite.setCode("");
        penalite.setQuittanceCode("");
        penalite.setNotes("Pénalité sur : " + requireNonNullElse(
                Champs.de(categorieEcheance, ComSettlementCategory::getDescription), "")
                + " du " + echeance.getDocDate().format(JOUR));
        return penalite;
    }
}
